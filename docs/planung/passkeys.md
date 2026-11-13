# Passkeys als spätere Anmeldealternative

## Entscheidungsvorlage

Analyseauftrag aus [Nachtpunkt 3](nachtaufgaben.md). **Noch keine Umsetzung
freigegeben.** Empfehlung: vorerst zurückstellen. Die vorhandene Anmeldung ist
bereits nutzbar; für den geplanten öffentlichen Testbetrieb bringen zuverlässiger
Mailversand und Betriebssicherheit zunächst mehr als ein zusätzlicher Anmeldeweg.

Passkeys bleiben ein sinnvolles späteres Paket: kein abzutippendes Passwort oder
OTP, besserer Schutz vor nachgebauten Anmeldeseiten und bequemere Anmeldung am
Smartphone. Sie sollten einen vorhandenen Ablauf vereinfachen. Eine zusätzliche
Pflichtfolge aus Passwort, Passkey und Authenticator wäre hier kein gutes Ziel.

## Was ein Passkey bedeutet

Ein Passkey ist ein kryptografisches Zugangsmittel. Das Gerät beziehungsweise der
Passkey-Anbieter verwahrt den privaten Schlüssel, das Portal den öffentlichen.
Fingerabdruck, Gesichtserkennung oder Geräte-PIN entsperren das Zugangsmittel lokal;
das Portal erhält diese Merkmale nicht. Passkeys können synchronisiert oder an
ein einzelnes Gerät gebunden sein. Dadurch unterscheiden sich ihre Verlust- und
Wiederherstellungsszenarien. [FIDO-Grundlagen](https://fidoalliance.org/passkeys/)

Die Bindung an die Website erschwert Phishing. Eine gültige Signatur allein darf
aber keine beliebige geschützte Aktion freigeben. Domain, Ursprung, Challenge,
Kontozuordnung und gegebenenfalls bestätigte lokale Benutzerprüfung müssen zum
jeweiligen Vorgang passen. Dies ist ein anderer Mechanismus als der zeitbasierte
sechsstellige Code unserer Authenticator-App.
[WebAuthn-Spezifikation](https://www.w3.org/TR/webauthn-3/)

## Ausgangslage im Projekt

| Bestehender Bereich | Berührungspunkt einer späteren Umsetzung |
| --- | --- |
| `SecurityConfig`, `LoginAuthenticationService`, `ExternalLoginConfiguration` | Passwort-, gespeicherte und externe Anmeldung; ein neuer Erfolgspfad muss dieselben Kontoprüfungen auslösen. |
| `SignInMethod`, `SessionAdmissionService`, `DurableAuthentication` und Sitzungsansicht | Anmeldeweg, Sitzungsaufnahme, Limit und dauerhafte Sicherheitsinformationen um Passkey ergänzen. |
| `TwoFactorAuthorization`, `TwoFactorAction`, `TwoFactorPolicies` | Zweckgebundene Freigaben für Anmeldung, Administration und einzelne Kontoänderungen. Ein Login bestätigt ausdrücklich keine spätere Änderung. |
| `TwoFactorFlow`, HTTP-/UI-Guards | Gesperrte Einrichtung, Wiederherstellung und Hilfe dürfen nicht umgangen werden. |
| `PendingEmailVerificationFlow`, `PendingAccountReactivationFlow` | E-Mail-Nachweis, Selbstdeaktivierung und administrative Sperre bleiben wirksam. |
| Kontoverwaltung, Löschdienst und aktuelles H2-Schema | Passkeys verwalten, widerrufen und beim endgültigen Löschen restlos entfernen. |

Es gibt derzeit kein WebAuthn-Datenmodell und keinen Passkey-Endpunkt. Die
vorhandenen TOTP-Nachweise lassen sich nicht einfach in Passkey-Nachweise umbenennen.
Die [geschützte Google-Ersteinrichtung](../externe-anmeldung.md#google-bestätigung-und-geschützte-ersteinrichtung)
ist ein bewusst vereinbarter Sonderweg und kein allgemeiner Wiederherstellungsnachweis.

## Vorgeschlagener fachlicher Ablauf

- Bestehende Konten und ihre Authenticator-App bleiben unverändert. Passkeys sind
  zunächst freiwillig und nur nach erneuter sicherer Identitätsbestätigung anlegbar.
  Keine Verknüpfung allein anhand einer übereinstimmenden E-Mail-Adresse.
- Mehrere Passkeys pro Konto erlauben, mit verständlichem Namen, Anlagedatum,
  letzter Verwendung und einzelnem Widerruf. Einen zweiten Zugang vor dem Entfernen
  des letzten verbleibenden Anmeldewegs sicherstellen.
- Bei der Anmeldung einen klaren alternativen Passkey-Einstieg anbieten. Passwort
  und Google/Microsoft bleiben erreichbar; die Oberfläche muss nicht vier gleich
  gewichtete Hauptschaltflächen bekommen. Gestaltung gemeinsam abstimmen.
- Zunächst bestehende 2FA-Regeln unverändert nach erfolgreicher Passkey-Anmeldung
  anwenden. Erst ein separat beschlossener Vertrag darf einen serverseitig mit
  Benutzerprüfung bestätigten Passkey als gleichwertigen starken Nachweis anerkennen.
  Das wäre ein Austausch des Nachweises, kein pauschales Abschalten der 2FA-Regeln.
- Für sensible Aktionen eine frische, an Zweck, Sitzung und Konto gebundene
  Bestätigung verlangen. Alte Anmeldeassertions nicht wiederverwenden. Auch Löschen,
  Deaktivieren, Passwort-/E-Mail-Änderung und Anbieterverknüpfung berücksichtigen.
- Geräteverlust darf nicht zu einer unsicheren E-Mail-Abkürzung werden. Bestehende
  Authenticator-/Ersatzcode-Wege erhalten; eine erfolgreiche Wiederherstellung muss
  alte Freigaben und kompromittierte Zugangsmittel gezielt widerrufen können.

Die Empfehlung zum späteren Ziel ist eine **Passkey-Anmeldung mit verbindlicher
lokaler Benutzerprüfung als Alternative**, nicht dauerhaft Passkey plus OTP für
jeden Benutzer. Welche Rollen beziehungsweise Aktionen diesen Nachweis akzeptieren,
ist vor Implementierung ausdrücklich festzulegen. Bis dahin gilt die bestehende
2FA-Policy ohne Ausnahme.

## Technische Arbeitspakete einer späteren Freigabe

1. **Verträge festlegen:** erlaubte Domains/Relying-Party-ID, Herkunftsnachweis,
   akzeptierte Nachweisstärke, Ersteinrichtung und Verlustfälle. Eine stabile
   öffentliche Domain zuerst wählen; lokale `localhost`-Passkeys sind nicht einfach
   Zugangsmittel für eine später andere Domain.
2. **Protokoll integrieren:** die dann eingesetzte Spring-Security-Version prüfen
   und deren WebAuthn-Unterstützung verwenden. Die Dokumentation beschreibt
   Registrierung, Authentifizierung und Repository-Schnittstellen; Browseraufrufe
   benötigen `navigator.credentials`. Das ist keine reine Java-Feldkomponente.
   Einen kleinen Browseradapter mit Flow-Oberfläche gezielt entwerfen, keine
   eigene Kryptografie oder neue parallele Loginseite einführen.
   [Spring-Security-Passkeys](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/passkeys.html)
3. **Persistenz und Sicherheit:** separate Credential-Datensätze mit stabilem
   Benutzerbezug, eindeutigem Credential-Identifier, öffentlichem Schlüssel und
   nötigen Authenticator-Metadaten. Challenges kurzlebig und einmalig, CSRF-Schutz,
   Missbrauchsbegrenzung, Origin-/RP-Prüfung und transaktionale Konsistenz.
   Synchronisierte Passkeys und deren Zählerverhalten explizit berücksichtigen.
4. **Bestehende Fachgrenzen anbinden:** Kontosperren, Zugangsversionen,
   Sitzungswechsel/-limit, Remember-me, gesperrte Abläufe und zweckgebundene
   Bestätigungen; keine generelle Freigabe allein durch neuen Authentifizierungstyp.
5. **Oberfläche und Texte:** native Vaadin-/Cosmos-Bausteine, DE/EN, Verwaltung unter
   Sicherheit, eindeutige Hinweise auf lokal entsperrte Geräte. Sicherheitsmails
   über vorhandene MJML-/HTML-Vorlagen; Entwicklerkonten bleiben mailfrei.
6. **Prüfen:** echte Registrierung/Anmeldung/Widerruf mit virtuellen WebAuthn-
   Authenticatoren; negative Signatur-, Origin-, Challenge-, Replay- und
   Benutzerprüfungsfälle; parallel vorhandene TOTP-/Recovery-Regressionen.
   Anschließend echte Geräte/Browser und Smartphone-Übergaben manuell abnehmen.

## Noch gemeinsam zu entscheiden

- [ ] Passkeys jetzt weiterhin zurückstellen oder ein eigenes Feature freigeben.
- [ ] Für welche Rollen und Aktionen ersetzt ein verifizierter Passkey den OTP-Nachweis?
- [ ] Welche Verlust-/Wiederherstellungswege und mehrere Geräte sollen unterstützt werden?
- [ ] Domain und gewünschte Darstellung der Anmeldung festlegen.

Vor einer späteren Umsetzung diese Analyse gegen aktuellen Code und offizielle APIs
erneut prüfen. Es wurden keine Abhängigkeiten, Sicherheitsregeln oder Anmeldewege
für diese Planung verändert.
