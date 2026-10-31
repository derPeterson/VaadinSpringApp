# Anmeldung mit Google und Microsoft

Die lokale E-Mail-/Passwortanmeldung bleibt erhalten. Vollständig konfigurierte
Anbieter erscheinen zusätzlich bei Anmeldung und Registrierung. Google sowie
private Microsoft-Konten und Firmen-/Schulkonten werden unterstützt.

## Einrichtung

Unter **Administration → Konfiguration → Anmeldeanbieter** können Administratoren
Google und Microsoft getrennt einrichten und aktivieren. Die Oberfläche führt zur
jeweiligen Anbieterverwaltung, zeigt die aus `BASE_URL` abgeleitete Rücksprungadresse
zum Kopieren und bietet Client-ID, maskiertes Client-Secret und die Freigabe an.
Die Anbieteranwendung muss einmalig bei Google beziehungsweise Microsoft angelegt werden.
Microsoft unterstützt weiterhin private sowie Firmen- und Schulkonten.

Speichern übernimmt die Einstellungen für neue Anmeldungen ohne Neustart. Ein
gespeichertes Geheimnis wird niemals in das Formular zurückgeladen. Ein leeres
Geheimnisfeld behält den bisherigen Wert bei. Die Zugangsdaten werden erst beim
tatsächlichen Anmeldeversuch vom Anbieter geprüft. Der Status „Aktiv“ bezeichnet
die lokale Freigabe, keine vorab bestätigte Verbindung.

Bestehende Portalsitzungen bleiben erhalten. Nach Deaktivierung benötigen betroffene
Benutzer einen anderen Anmeldeweg. Laufende Anbieteranfragen mit zwischenzeitlich
geänderten Einstellungen müssen neu gestartet werden. Veraltete parallele
Adminformulare dürfen neuere Einstellungen nicht überschreiben.
Die Zugangsdaten werden wie die vorhandene SMTP-Konfiguration serverseitig in
der lokalen Datenbank gespeichert. Die Administration prüft vor Lesen und Speichern
frische Administratorrechte. Endpunktadressen sind nicht über die Oberfläche änderbar.

Die folgenden Startvorgaben sind optional. Solange für einen Anbieter noch keine
Einstellung gespeichert wurde, gelten die Werte aus der Serverkonfiguration.
Gespeicherte Adminwerte haben danach Vorrang, einschließlich „Deaktiviert“.

Für jeden Anbieter eine eigene Webanwendung registrieren. Bei Microsoft als
unterstützte Kontotypen Organisationsverzeichnisse **und persönliche Microsoft-Konten**
wählen. Benötigt werden ausschließlich `openid`, `profile` und `email`.
Keine Mailbox-, Kalender-, Laufwerks- oder Graph-Berechtigungen hinzufügen.

| Einstellung | Google | Microsoft |
| --- | --- | --- |
| Client-ID | `app.external-login.google.client-id` | `app.external-login.microsoft.client-id` |
| Client-Secret | `app.external-login.google.client-secret` | `app.external-login.microsoft.client-secret` |
| Rücksprung | `https://localhost:8443/login/oauth2/code/google` | `https://localhost:8443/login/oauth2/code/microsoft` |

Den jeweiligen Rücksprung mit der tatsächlich verwendeten HTTPS-Adresse exakt beim
Anbieter eintragen. Hinter einem Proxy müssen Ursprung und Weiterleitungsheader
gemäß der HTTPS-Konfiguration stimmen. Zugangsdaten über den Adminbereich oder die
lokalen Startvorgaben setzen, niemals einchecken.
Die [`.env.example`](../.env.example) enthält diese vier Einstellungen für die lokale
`.env` oder echte Umgebungsvariablen:

```properties
APP_EXTERNAL_LOGIN_GOOGLE_CLIENT_ID=
APP_EXTERNAL_LOGIN_GOOGLE_CLIENT_SECRET=
APP_EXTERNAL_LOGIN_MICROSOFT_CLIENT_ID=
APP_EXTERNAL_LOGIN_MICROSOFT_CLIENT_SECRET=
```

Die Anwendung ordnet diese Namen ausdrücklich den typisierten Spring-Properties
zu. Die `.env` wird als Properties-Datei importiert. Die Einträge dort jeweils
ohne Leerzeichen vor oder nach dem eigenen Wert ausfüllen.
Ein Anbieter ohne beide Angaben bleibt unsichtbar. Nur Änderungen an Startvorgaben
erfordern einen Neustart. Für Änderungen über die Administration genügt Speichern
und ein neuer Aufruf der Anmeldeseite.
Dieses Paket ändert auch das aktuelle H2-Schema. Zum Starten eine frische eigene
Testdatenbank verwenden. Bestehende Entwicklungsdaten werden nicht automatisch umgebaut.

### Google: Client-ID und Client-Secret erstellen

Diese Zugangsdaten werden einmal für das Portal angelegt. Die Registrierung erfolgt
beim Anbieter, anschließend werden die Werte in unserer Adminoberfläche gespeichert.

1. Die [Google Cloud Console](https://console.cloud.google.com/) öffnen und ein eigenes
   Projekt erstellen oder ein vorhandenes auswählen.
2. Unter **Google Auth Platform** bei der ersten Einrichtung den Anwendungsnamen,
   die Kontaktadresse und die Zielgruppe einrichten.
3. Unter **Clients → Client erstellen** den Anwendungstyp **Webanwendung** wählen.
4. Unter **Autorisierte Weiterleitungs-URIs** die Rücksprungadresse aus unserer
   Oberfläche **Administration → Konfiguration → Anmeldeanbieter → Google** eintragen.
   Die Adresse exakt übernehmen. Das lokale Beispiel ist
   `https://localhost:8443/login/oauth2/code/google`.
5. Den Client erstellen. Die erhaltene **Client-ID** und das **Client-Secret** in
   die entsprechenden Google-Felder unserer Adminoberfläche übertragen.
6. **Anmeldung erlauben** aktivieren und **Einstellungen speichern** wählen.
   Anschließend die Anmeldeseite neu laden. Dort erscheint die Google-Anmeldung.

Offizielle Anleitung: [Google OpenID Connect – Einrichtung](https://developers.google.com/identity/openid-connect/openid-connect#settingup).

### Microsoft: Client-ID und Client-Secret erstellen

1. Das [Microsoft Entra Admin Center](https://entra.microsoft.com/) öffnen.
2. Unter **Entra ID → App-Registrierungen → Neue Registrierung** eine eigene
   Anwendung registrieren. Als unterstützte Kontotypen **Konten aus beliebigen
   Organisationsverzeichnissen und persönliche Microsoft-Konten** wählen.
   Damit werden private sowie Firmen-/Schulkonten unterstützt.
3. Die Plattform **Web** einrichten und die Rücksprungadresse aus unserer Oberfläche
   **Administration → Konfiguration → Anmeldeanbieter → Microsoft** hinterlegen.
   Die Adresse exakt übernehmen. Das lokale Beispiel ist
   `https://localhost:8443/login/oauth2/code/microsoft`.
4. Unter **Übersicht** die **Anwendungs-ID (Client)** kopieren. Das ist die Client-ID
   für das entsprechende Microsoft-Feld unserer Adminoberfläche.
5. Unter **Zertifikate und Geheimnisse → Geheime Clientschlüssel → Neuer geheimer
   Clientschlüssel** ein Secret anlegen. Seinen **Wert**, nicht die Geheimnis-ID,
   in das Client-Secret-Feld unserer Adminoberfläche übernehmen.
   Der Wert wird nur beim Anlegen vollständig angezeigt. Vor seinem Ablauf einen
   neuen Schlüssel konfigurieren.
6. **Anmeldung erlauben** aktivieren und **Einstellungen speichern** wählen.
   Anschließend die Anmeldeseite neu laden. Dort erscheint die Microsoft-Anmeldung.
   Für den gemeinsamen Microsoft-Einstieg ist keine zusätzliche Tenant-ID nötig.

Offizielle Anleitungen: [Microsoft-App registrieren](https://learn.microsoft.com/en-us/entra/identity-platform/quickstart-register-app),
[Redirect-URI einrichten](https://learn.microsoft.com/en-us/entra/identity-platform/how-to-add-redirect-uri),
[Client-Secret anlegen](https://learn.microsoft.com/en-us/entra/identity-platform/how-to-add-credentials).

Client-Secrets ausschließlich in der eigenen Adminoberfläche oder lokalen
Startkonfiguration hinterlegen. Nicht in Chats posten oder in Dokumentation und Git speichern.

Die optionalen Einstellungen `authorization-uri`, `token-uri` und `jwk-set-uri`
dienen den eigenen isolierten Testanbietern. Im normalen Betrieb nicht überschreiben.
Die Issuerprüfung bleibt auch bei Testendpunkten aktiv. Microsofts gemeinsamer
Einstieg wird auf einen konkreten UUID-Mandanten mit passendem Issuer aufgelöst.

## Konten und Nachweise

Anbieter, Issuer und Subject identifizieren eine externe Anmeldung. Eine gleiche
E-Mail-Adresse verknüpft niemals automatisch ein bestehendes Konto. Dazu zuerst
im bestehenden Konto anmelden und unter **Sicherheit → Verbundene Anmeldungen**
den zusätzlichen Anbieter bewusst verbinden.

Bei einer neuen externen Registrierung werden Namen und Kontaktadresse bestätigt
oder ergänzt. Die im Abschlussformular gewählte Oberflächensprache wird als
Kontosprache gespeichert. Ein Portalpasswort ist nicht erforderlich. Neue Konten erhalten
ausschließlich die Benutzerrolle. Geschlecht und Geburtsdatum sind freiwillig und
werden ohne Angabe als leer gespeichert. Ausgefüllte Geburtstage müssen vergangen sein.

Google bestätigt eine Kontaktadresse unmittelbar nur bei `email_verified` und einer
Gmail-Adresse oder passendem bestätigten Workspace-Domainclaim. Andere Google-Adressen,
Microsoft-Adressen und vom Benutzer geänderte Kontaktadressen benötigen den eigenen
Portal-Verifikationslink. Vorher entsteht kein uneingeschränkt nutzbarer Portalzugang.
Anbieterzugriffs- und Refresh-Tokens werden nicht aufbewahrt. Die Portalabmeldung
beendet keine Sitzung beim Anbieter. Eine spätere Sperre beim Anbieter wird beim nächsten
Anbieternachweis geprüft, nicht fortlaufend während einer bestehenden Portalsitzung.

Für Passwort-/E-Mail-Änderungen und Verknüpfungen zuerst die konkrete Sicherheitsaktion
auswählen und die Identität mit dem vorhandenen Portalpasswort oder einem bereits
verbundenen Anbieter erneut bestätigen. Der Nachweis ist fünf Minuten gültig, an
Konto, Sitzung, Zugangsversion und Zweck gebunden und wird bei Verwendung verbraucht.
Eine alte Anbietersitzung allein genügt nicht: Ein frischer `auth_time`-Nachweis ist
erforderlich. Liefert ein Anbieter diesen nicht, bleibt die Änderung gesperrt.
Die vorhandene Wiederherstellung über die bestätigte Portaladresse bleibt nutzbar.
Ein eigenes Portalpasswort kann freiwillig eingerichtet werden.

Die letzte Anmeldemöglichkeit kann nicht getrennt werden. Trennen eines Anbieters
widerruft bestehende Portalsitzungen und gespeicherte Anmeldungen. Kontosperre, lokale
Rollen, Wartung, Sitzungslimit und Gerätewiderruf gelten auch für externe Anmeldungen.
„Login speichern“ muss weiterhin ausdrücklich beim betreffenden Login gewählt werden.

## Prüfung und Grenzen

Die isolierten Tests verwenden einen eigenen OIDC-Dienst mit signierten Antworten,
Nonce, State und PKCE. Er ersetzt keine Prüfung mit den tatsächlich eingerichteten
Google-/Microsoft-Anwendungen. Dieser letzte Durchlauf benötigt deren Client-Konfiguration
und eine interaktive Benutzeranmeldung. Insbesondere erneute Anbieterbestätigung und
Organisationsrichtlinien sind dabei zu kontrollieren.

Grundlagen: [Google OpenID Connect](https://developers.google.com/identity/openid-connect/openid-connect),
[Google-Kontaktvertrauen](https://developers.google.com/identity/sign-in/android/backend-auth),
[Microsoft-ID-Token](https://learn.microsoft.com/en-us/entra/identity-platform/id-token-claims-reference),
[Spring Security OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/index.html).
