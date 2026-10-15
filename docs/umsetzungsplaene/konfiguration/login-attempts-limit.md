# Anmeldeversuche begrenzen

Stand: 15.10.2026; Ausgangscommit `1d57a5e`. **Umsetzungsplan, noch nicht implementiert.**

| Eigenschaft | Wert |
| --- | --- |
| Konfiguration | `login.attempts.limit` / `ConfigEntry.LOGIN_ATTEMPTS_LIMIT` |
| Enum-Standard | `10` |
| Gruppe | Sicherheit |
| Vorgeschlagene Aktivierung | Ab nächstem Anmeldeversuch, ohne Neustart |
| Priorität | Hoch; gemeinsamer Einstieg für weitere Anmeldepläne |

## Heutiger Stand

Der Wert wird als positive Ganzzahl validiert und gespeichert. `LoginView` ruft
unmittelbar `AuthenticationManager.authenticate` auf und speichert bei Erfolg den
SecurityContext über `SecurityService`. Daneben existiert der Spring-Formlogin in
`SecurityConfig`. Kein Weg liest das Versuchslimit. Zähler, Zeitfenster und zeitweise
Anmeldesperre fehlen. Bestehende Kontosperren über `enabled=false` bleiben eine eigene Funktion.

## Empfohlener erster Umfang

**Vorschlag:** Pro normalisierter Anmeldekennung höchstens N falsche Passwortversuche
innerhalb von 15 Minuten; der N-te Fehler sperrt weitere Passwortprüfungen für 15 Minuten.
N kommt aus der Konfiguration. Fenster und Sperrdauer werden zunächst als benannte
zentrale Regeln mit injizierbarer `Clock` geführt. Beide 15-Minuten-Werte sind ein
Produktvorschlag, keine vorhandenen Einstellungen.

- Bei N=10 scheitern zehn Versuche regulär; Versuch 11 wird während der Sperre auch
  mit richtigem Passwort nicht zugelassen. Nach Ablauf beginnt ein neues Fenster.
- Ein erfolgreicher Passwortnachweis vor einer Sperre setzt den Zähler zurück.
  Eine anschließend wegen Sitzungslimit abgewiesene Anmeldung zählt nicht als Passwortfehler.
- Geblockte Wiederholungen verlängern die Sperre nicht unbegrenzt.
- Admin, Superadmin, Benutzer und Gast unterliegen derselben Regel.
- Bestehende Sitzungen bleiben erhalten. Remember-me-Anmeldungen und ungültige
  Remember-me-Cookies zählen nicht als Passwortversuche.
- Eine angefragte Resetmail hebt die Sperre nicht auf. Nach einem mit gültigem Token
  abgeschlossenen Passwortwechsel wird der Zähler für dieses Konto geleert.

Das Minimum bleibt 1; 0 erhält keine neue Bedeutung „ausgeschaltet“. Ein reduziertes
Limit gilt beim nächsten Prüfzugriff auf einen laufenden Zähler. Eine bereits gesetzte
Sperre läuft zu ihrem gespeicherten Endzeitpunkt ab.

## Sichtbare Änderung

Das Loginformular behält seine Gestaltung. Vorübergehende Drosselung erhält eine
verständliche DE/EN-Meldung zum späteren Versuch. Sie verrät weder, ob die Adresse
existiert, noch Rollen oder kontospezifische Zählerstände. Für bekannte und unbekannte
Kennungen gilt derselbe äußere Ablauf. Die Feldhilfe erklärt Fenster, Zählweise und Wirkung.

„Passwort vergessen“ bleibt erreichbar. Ein Entsperrpanel, CAPTCHA oder automatische
dauerhafte Kontodeaktivierung gehört nicht zum ersten Umfang.

## Technischer Umsetzungsweg

1. **`LoginAttemptService` und persistenter Zähler neu vorgeschlagen:** Schlüssel aus
   `EmailIdentity.canonicalize`, Fensterbeginn, Fehlversuche und `blockedUntil` speichern.
   Unbekannte Kennungen ebenfalls begrenzen, ohne Benutzerkonten anzulegen. Keine
   Passwörter, Tokens oder vollständigen Requestdaten im Zähler speichern.
2. Neue Tabelle mit eindeutigem Schlüssel samt Bestandsupgrade liefern. Benutzer
   bleiben unverändert. Abgelaufene, nicht mehr gesperrte Einträge bereinigen;
   Wachstum durch erfundene Adressen begrenzen, ohne bei einer Grenze still den Schutz
   auszuschalten. Bereinigungsfrist und Größenregel vor Implementierung konkretisieren.
3. **`LoginAuthenticationService` neu vorgeschlagen:** Zulassung, Passwortprüfung und
   Verbuchung gemeinsam kapseln. Vaadin-Button und erreichbaren Spring-Formlogin
   anbinden; direkter HTTP-Login darf den Zähler nicht umgehen. Technische Fehler und
   Wartungs-/Sitzungsablehnungen von falschen Zugangsdaten unterscheiden. Mehrere
   Listener dürfen einen Fehler nicht doppelt zählen.
4. Prüfung und Zählerfortschreibung pro Kennung gegen Konkurrenz absichern,
   einschließlich gleichzeitiger Erstanlage. Lock-/Transaktionsablauf vor Umsetzung
   festlegen; ungesichertes „lesen, prüfen, schreiben“ reicht nicht. Keine globale
   Sperre für sämtliche Benutzer verwenden.
5. Erst nach zugelassener erfolgreicher Anmeldung Context, Vaadin-Principal,
   Remember-me-Cookie und Status wie bisher speichern. Bei Drosselung unterbleibt dies.
6. Den erfolgreichen Passwortreset anschließen; DE/EN-Hinweise, Login-/Resetvertrag
   und Testdokumentation auf das tatsächlich implementierte Verhalten aktualisieren.

Persistenz verhindert, dass ein Neustart alle Zähler löscht oder mehrere Instanzen
getrennt zählen. Zusätzliche IP-/Ingress-Begrenzung kann später hinzukommen; dieses
Paket liefert keinen vollständigen Schutz gegen verteilte Angriffe oder massenhaft
neue Kennungen. Zeitweise Kontodrosselung kann von Dritten ausgelöst werden;
Sperrdauer und Meldungen deshalb vor Aktivierung bewusst bestätigen.

## Tests und Abnahme

- **JUnit:** N−1/N/N+1, N=1, exakte Zeitgrenzen, erfolgreicher Login, abgelaufenes
  Fenster, Passwortreset, geändertes Limit und ungültiger gespeicherter Wert.
- **Persistenz/Konkurrenz:** gleichzeitige Erstanlage und Grenzübertritt, Erfolg und
  Fehler gleichzeitig, normalisierte E-Mail-Varianten, neuer Kontext mit erhaltenem
  Zähler und Bereinigung ohne Löschen aktiver Sperren.
- **Integration:** Vaadin-/HTTP-Login zählen genau einmal; technische Fehler erzeugen
  weder Erfolg noch Remember-me-Cookie. Sitzungslimit und Wartung erhöhen den Zähler nicht.
- **Browser DE/EN:** mit kleinem Testlimit sperren, korrekten Login während der Sperre
  ablehnen und nach kontrolliertem Ablauf zulassen; bestehende Sitzung bleibt erhalten.
  JUnit-Zeitsteuerung über Clock, nicht minutenlange Sleeps; nur eigene Testdaten.

Fertig ist das Paket, wenn die serverseitige Regel auf allen Passwortwegen gilt,
keine Konten dauerhaft deaktiviert und die Grenzfälle nachgewiesen sind.
Die hier beschriebenen Prüfungen wurden noch nicht ausgeführt.

## Grundlage und Anschlussstellen

[ConfigEntry](../../../src/main/java/de/derpeterson/app/model/enums/ConfigEntry.java),
[LoginView](../../../src/main/java/de/derpeterson/app/views/LoginView.java),
[SecurityService](../../../src/main/java/de/derpeterson/app/security/SecurityService.java),
[SecurityConfig](../../../src/main/java/de/derpeterson/app/security/SecurityConfig.java),
[EmailIdentity](../../../src/main/java/de/derpeterson/app/model/EmailIdentity.java),
[Passwortreset-Vertrag](../../password-reset-contract.md).

OWASP beschreibt Schwellwert, Beobachtungsfenster und Sperrdauer sowie die Gefahr
fremdausgelöster Sperren. Die 15 Minuten sind mein Vorschlag:
[Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html#login-throttling).
Abstimmen mit [Sitzungslimit](max-sessions-per-user.md) und [Wartungsmodus](maintenance-mode.md).
