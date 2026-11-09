# Anmeldung mit Google und Microsoft

Die lokale E-Mail-/Passwortanmeldung bleibt erhalten. Vollständig konfigurierte
Anbieter erscheinen zusätzlich bei Anmeldung und Registrierung. Google sowie
private Microsoft-Konten und Firmen-/Schulkonten werden unterstützt.

## Einrichtung

Unter **Administration → Konfiguration → Anmeldeanbieter** können ausschließlich Superadmins
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
Client-Secrets werden mit AES-256-GCM und zufälliger Nonce in
`external_providers.encrypted_client_secret` gespeichert. Die Verschlüsselung ist
an den jeweiligen Anbieter gebunden. Client-IDs bleiben lesbar. Die Administration
prüft vor Lesen und Speichern frische Superadminrechte. Endpunktadressen sind nicht
über die Oberfläche änderbar. Entschlüsselte Secrets bleiben serverseitig und werden
nicht ins Formular, in UI-Sitzungen oder in Fehlermeldungen übernommen.

Vor dem Hinterlegen von Secrets den eigenen Startschlüssel
`APP_SECURITY_PROVIDER_SECRETS_ENCRYPTION_KEY` in der `.env` setzen.
Er besteht aus 32 zufälligen Bytes als Base64. In PowerShell 7 einmalig erzeugen:

```powershell
[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

Einen anderen Schlüssel als für TOTP verwenden. Er bleibt dauerhaft gleich und
wird getrennt von Datenbank und Repository gesichert. Ohne Schlüssel sind nur
Anbieter ohne hinterlegtes Secret möglich. Fehlender/falscher Schlüssel oder
beschädigte gespeicherte Secrets führen zu einem Fehler, beim Start zum Abbruch.
Es gibt keinen Klartext-Fallback und keinen Ersatz durch Startvorgaben. Ohne den
ursprünglichen Schlüssel sind gespeicherte Secrets nicht wiederherstellbar.

Das aktuelle Entwicklungsschema enthält ausschließlich die verschlüsselte Spalte.
Eine ältere lokale Datenbank muss zurückgesetzt und anschließend neu eingerichtet
werden. Es gibt keinen automatischen Umbau alter Klartextbestände. Vorhandene
Klartextkopien und alte Datenbankexporte werden durch diese Änderung nicht verändert.

Die folgenden Startvorgaben sind optional. Beim Start legt die Anwendung fehlende
Anbieterzeilen in `external_providers` mit diesen Werten an. Vorhandene Zeilen
bleiben unverändert. Gespeicherte Adminwerte haben Vorrang, einschließlich „Deaktiviert“.
Client-ID und Secret allein aktivieren keinen Anbieter. Dazu ist der eigene
Aktivierungsschalter erforderlich. Beide Anbieter sind standardmäßig deaktiviert.

Für jeden Anbieter eine eigene Webanwendung registrieren. Bei Microsoft als
unterstützte Kontotypen Organisationsverzeichnisse **und persönliche Microsoft-Konten**
wählen. Benötigt werden ausschließlich `openid`, `profile` und `email`.
Keine Mailbox-, Kalender-, Laufwerks- oder Graph-Berechtigungen hinzufügen.

| Einstellung | Google | Microsoft |
| --- | --- | --- |
| Client-ID | `app.external-login.google.client-id` | `app.external-login.microsoft.client-id` |
| Client-Secret | `app.external-login.google.client-secret` | `app.external-login.microsoft.client-secret` |
| Aktivierung | `app.external-login.google.enabled` | `app.external-login.microsoft.enabled` |
| Rücksprung | `https://localhost:8443/login/oauth2/code/google` | `https://localhost:8443/login/oauth2/code/microsoft` |

Den jeweiligen Rücksprung mit der tatsächlich verwendeten HTTPS-Adresse exakt beim
Anbieter eintragen. Hinter einem Proxy müssen Ursprung und Weiterleitungsheader
gemäß der HTTPS-Konfiguration stimmen. Zugangsdaten über den Adminbereich oder die
lokalen Startvorgaben setzen, niemals einchecken.
Die [`.env.example`](../.env.example) enthält diese sechs Einstellungen für die lokale
`.env` oder echte Umgebungsvariablen:

```properties
APP_EXTERNAL_LOGIN_GOOGLE_CLIENT_ID=
APP_EXTERNAL_LOGIN_GOOGLE_CLIENT_SECRET=
APP_EXTERNAL_LOGIN_GOOGLE_ENABLED=false
APP_EXTERNAL_LOGIN_MICROSOFT_CLIENT_ID=
APP_EXTERNAL_LOGIN_MICROSOFT_CLIENT_SECRET=
APP_EXTERNAL_LOGIN_MICROSOFT_ENABLED=false
```

Die Anwendung ordnet diese Namen ausdrücklich den typisierten Spring-Properties
zu. Die `.env` wird als Properties-Datei importiert. Die Einträge dort jeweils
ohne Leerzeichen vor oder nach dem eigenen Wert ausfüllen.
Zur erstmaligen Aktivierung `ENABLED=true` und beide Zugangsdaten setzen. Eine
angeforderte Aktivierung ohne vollständige oder mit formal ungültigen Zugangsdaten
führt zu einem Startfehler. Ob die Zugangsdaten beim Anbieter gültig sind, wird
erst beim tatsächlichen Anmeldeversuch geprüft. Ein deaktivierter Anbieter bleibt auf Anmeldung und
Registrierung unsichtbar. Änderungen an Startvorgaben benötigen einen Neustart
und wirken nur auf fehlende Anbieterzeilen. Bereits gespeicherte Anbieter über
die Administration ändern. Dort genügt Speichern und ein neuer Aufruf der Anmeldeseite.
Die Datenbank muss dem aktuellen H2-Schema entsprechen. Bestehende Tabellen werden
nicht automatisch umgebaut. Die Anbieterinitialisierung selbst benötigt keine
zusätzliche Schemaänderung und überschreibt keine vorhandenen Einstellungen.

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
6. **Anmeldung erlauben** aktivieren und **Konfiguration speichern** wählen.
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
6. **Anmeldung erlauben** aktivieren und **Konfiguration speichern** wählen.
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
im bestehenden Konto anmelden und unter **Sicherheit → Anmeldeanbieter**
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

Das Sicherheitspanel trennt „Portalzugang“ mit Passwort, E-Mail und Zugangsdatenhinweis
vom Tab „Anmeldeanbieter“. Dieser zeigt je Anbieter Verbindung und passende
Aktion. „Verbinden“ oder „Trennen“ öffnet die dafür nötige Identitätsbestätigung.
Passwort-/E-Mail-Änderungen behalten ihre vorhandenen Schaltflächen. Ein bereits
verbundener Anbieter kann auch diese Aktionen bestätigen; lokale Konten ohne
Anbieterverbindung verwenden weiterhin die Passwortprüfung im Änderungsdialog.
Eine allgemeine Sicherheitsaktionsauswahl ist nicht erforderlich. Nach Rückkehr
vom Anbieter wird die gewählte Aktion wieder aufgenommen, eine Trennung aber nie
automatisch ausgeführt. Der Nachweis ist fünf Minuten gültig, an
Konto, Sitzung, Zugangsversion und Zweck gebunden und wird bei Verwendung verbraucht.
Microsoft muss einen frischen `auth_time`-Nachweis liefern. Google allein bestätigt
im normalen Betrieb keine sensible Kontoaktion. Bei geltender 2FA-Regel sind TOTP
oder ein einmaliger Ersatzcode notwendig, unabhängig vom ursprünglich verwendeten
Anmeldeweg. Ein eigenes Portalpasswort bleibt freiwillig.

Auch bei vollständig deaktivierten Anbietern bleiben vorhandene Verbindungen
sichtbar. Portalpasswort und eingerichtete 2FA bleiben unabhängig nutzbar. Die letzte
nutzbare Anmeldung darf nicht getrennt werden. Anbieter-Deaktivierung löscht keine
Zuordnung und beendet keine bereits bestehende Sitzung.

### Google-Bestätigung und geschützte Ersteinrichtung

Ein neuer Google-OIDC-Durchlauf kann eine bestehende Google-Sitzung verwenden und
beweist keine erneute aktive Passwort- oder MFA-Eingabe. Deshalb wird er im normalen
Betrieb nicht als Bestätigung einer sensiblen Kontoaktion akzeptiert. Die erneute
Microsoft-Bestätigung verlangt weiterhin `prompt=login`, `max_age=0` und einen
frischen `auth_time`. Ein fehlender oder unzulässig alter Zeitpunkt wird abgewiesen.

Die echte erstmalige Google-2FA-Einrichtung verwendet den erneuten kontogebundenen
OIDC-Durchlauf, einen begrenzten Einmalcode an die bereits bestätigte Portaladresse
und den ersten TOTP. Das ausdrücklich akzeptierte Restrisiko gemeinsam übernommener
Google-/Mailkonten bleibt bestehen. Dieser Weg ersetzt niemals einen bereits
eingerichteten, entfernten oder verlorenen Faktor. Faktorwechsel und neue Ersatzcodes
verlangen immer den vorhandenen Faktor oder einen gültigen Ersatzcode.

Nur eine gesondert beim Start freigegebene, ausdrücklich eingeschaltete
Entwicklungsoption erlaubt den früheren schwachen Google-Nachweis für Aktionen ohne
geltende 2FA-Anforderung. Sie erzeugt keinen zweiten Faktor. Ohne Startfreigabe sind
auch gespeicherte Ausnahmen wirkungslos. Alle Konto-, Sitzungs-, Zweck-, Versions-,
Signatur-, State-, Nonce- und PKCE-Prüfungen bleiben erhalten.

Details zu Einrichtung, Regeln, Wiederherstellung und Grenzen stehen unter
[Zwei-Faktor-Authentifizierung](zwei-faktor-authentifizierung.md).
Fehlgeschlagene Bestätigungen kehren zur Kontosicherheit mit passender Meldung zurück,
sie beginnen keine neue Registrierung.
### Welche Kontoaktionen möglich sind

Die folgende Übersicht ergänzt die jeweils geltende 2FA-Regel. Ein eingerichteter Faktor
und dessen erforderliche Aktionsbestätigung werden durch keinen anderen Anmeldeweg umgangen.

Die ursprüngliche Registrierung bestimmt die Anmeldewege nicht dauerhaft.
Maßgeblich sind die aktuell gespeicherten und freigegebenen Zugänge. Das Konto
speichert den Kanal seiner Selbstregistrierung (Portal, Google oder Microsoft).
Bei administrativ oder beim Start angelegten Konten bleibt dieses Feld leer.
Der tatsächliche Anmeldeweg der Sitzung ist separat in ihrer dauerhaften
Authentifizierung hinterlegt. Die heute nutzbaren Wege ergeben sich aus
Portalpasswort, Verknüpfungen und freigegebenen Anbietern.
In der Benutzerverwaltung stehen „Registriert über“ und „Verfügbare Anmeldewege“
im Abschnitt „Anmeldung“ der Benutzerdetails. „Geräte & Sitzungen“ nennt bei
jeder aktiven Sitzung ihren eigenen Anmeldeweg.

| Aktueller Zugang | Portalpasswort | Weitere Anbieter | Anbieter trennen |
| --- | --- | --- | --- |
| Nur Portalpasswort | Mit aktuellem Passwort ändern. | Google und Microsoft nach Passwortbestätigung verbinden. | Erst nach einer Verbindung relevant. |
| Nur Google oder nur Microsoft | Freiwillig nach Anbieterbestätigung einrichten. Für Google gilt die geschützte 2FA-Ersteinrichtung, für Microsoft der frische Nachweis. | Den anderen Anbieter nach Bestätigung des bestehenden Zugangs verbinden. | Die letzte nutzbare Anmeldung bleibt geschützt. |
| Google und Microsoft ohne Portalpasswort | Nach Bestätigung eines verbundenen Anbieters gemäß dessen Nachweisvertrag einrichten. | Beide Verbindungen sind bereits vorhanden. | Möglich, wenn der andere Anbieter weiterhin freigegeben ist. |
| Portalpasswort und mindestens ein Anbieter | Mit aktuellem Passwort oder Anbieterbestätigung gemäß dessen Nachweisvertrag ändern. | Den noch fehlenden Anbieter bewusst verbinden. | Nach Identitätsbestätigung möglich, das Portalpasswort bleibt als Zugang erhalten. |

Die Portal-E-Mail-Adresse kann in allen Fällen nach Identitätsbestätigung geändert
werden. Erst der Link an die neue Adresse übernimmt die Änderung. Das ändert weder
die E-Mail-Adresse beim Anbieter noch dessen gespeicherte Identitätszuordnung.
Nach bestätigter E-Mail-Änderung werden Portalsitzungen und gespeicherte
Anmeldungen widerrufen. Ein Passwortwechsel behält die aktuelle Sitzung und
widerruft die übrigen Zugänge sowie gespeicherte Anmeldungen.
Eine Anbieteridentität darf nur einem Portalkonto zugeordnet sein. Gleiche
E-Mail-Adressen führen weder zur Zusammenführung noch zu zusätzlichen Rechten.

Die letzte Anmeldemöglichkeit kann nicht getrennt werden. Beim Trennen eines
Anbieters werden andere Portalsitzungen und gespeicherte Anmeldungen widerrufen.
Die aktuelle Sitzung bleibt bei bestätigtem Portalpasswort oder einem weiterhin
nutzbaren anderen Anbieter bestehen. Eine Sitzung des getrennten Anbieters oder
eine per gespeichertem Login wiederaufgenommene Sitzung endet mit einem Hinweis
zur erneuten Anmeldung. Kontosperre, lokale
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
