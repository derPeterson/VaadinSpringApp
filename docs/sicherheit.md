# Sicherheit und Datenverträge

## Anmeldung und Rollen

E-Mail-Identitäten werden zentral getrimmt und kleingeschrieben. Die Datenbank
erzwingt Eindeutigkeit und die kanonische Form. Passwörter werden mit BCrypt gespeichert;
Eingaben werden durch gemeinsame begrenzte Regeln statt teurer unbeschränkter Regex geprüft.
Anmeldefehler und Resetanfragen geben keine zuverlässige Auskunft über vorhandene Konten.

Die Rollen sind USER, GUEST, ADMIN und SUPERADMIN. Administrative Dienste prüfen die
aktuellen Rechte; ein UI-Button allein ist keine Zugriffskontrolle. Kontosperre,
Löschung, Rollenänderung und Sessionwiderruf werden auch bei bereits geöffneten
Ansichten wirksam. Der letzte aktive Superadmin ist durch eine gemeinsame
Datenbanksperre geschützt, auch bei konkurrierenden Änderungen verschiedener Konten.

Fehlversuche werden je kanonisierter Identität unter einem Hash gespeichert.
Versuchslimit und Zeitfenster sind konfigurierbar und gelten ab dem nächsten
Versuch. Das Fenster beträgt standardmäßig 15 Minuten und kann bis zu 24 Stunden
betragen. Die Sperre dauert 15 Minuten. Bereits verhängte Sperren behalten ihr Ende.
Ein erfolgreicher Passwortreset setzt den zugehörigen Schutz kontrolliert zurück.

## Eigenes Konto deaktivieren und löschen

Unter „Mein Konto → Sicherheit → Konto verwalten“ kann der Kontoinhaber das Konto
vorübergehend deaktivieren oder endgültig löschen. Ein eingerichteter Authenticator
hat Vorrang. Ohne Authenticator bestätigt das aktuelle Portalpasswort die Aktion.
Ein ausschließlich extern angemeldetes Konto kann zuvor ein Portalpasswort oder
einen Authenticator einrichten. Die Aktion ist an die aktuelle Sitzung und den
nachgewiesenen Kontostand gebunden. Der letzte aktive Superadmin bleibt geschützt.

Die Selbstdeaktivierung widerruft Sitzungen und gespeicherte Anmeldungen. Sie
reserviert ein Löschdatum und verschickt einen Reaktivierungslink an die
Kontoadresse. Unter „Konfiguration → Sicherheit & Sitzungen“ werden die
Aufbewahrung und der Vorlauf der Erinnerung in ganzen Tagen eingestellt. Die
Standardwerte betragen 30 und 7 Tage. Ein Vorlauf von null deaktiviert die
Erinnerung. Der tatsächliche Vorlauf lässt mindestens einen Tag nach der
Deaktivierung frei. Bereits festgelegte Termine ändern sich durch spätere
Konfigurationsänderungen nicht.

Der Link aktiviert das Konto erst nach einer ausdrücklichen Bestätigung und ist
bis zum Löschdatum gültig. Eine erfolgreiche Aktivierung macht alle bisherigen
Links dieses Vorgangs ungültig. Über die Anmeldeseite lässt sich bei Bedarf ein
weiterer Link anfordern. Die Oberfläche meldet denselben Ausgang unabhängig
vom Kontostatus. Eine durch einen Admin veranlasste Kontosperre kann über diesen
Weg nicht aufgehoben werden.

Die Löschung wird direkt bestätigt oder nach Ablauf der Deaktivierungsfrist
automatisch ausgeführt. Sie entfernt das Konto, Anmeldeverknüpfungen,
gespeicherte Nachweise, Profilbild, eigene Vorgänge und verfasste Inhalte.
Fremde Vorgänge bleiben erhalten. Noch laufende Zuweisungen werden gelöst.
Die Bestätigungsmail entsteht in derselben Transaktion wie die Löschung. Nur
der Mailauftrag hält die Zieladresse bis zum Versand oder endgültigen
Versandfehler. Bei unklarem SMTP-Ausgang bleibt der Auftrag entsprechend dem
Mailqueue-Vertrag zur Klärung erhalten. Bereits zugestellte externe E-Mails
können nicht zurückgerufen werden.

## Externe Identitäten

Google und Microsoft ergänzen die lokale Anmeldung. Anbieter, Issuer und Subject
bilden die eindeutige Identität. Keine automatische Verknüpfung über E-Mail und
keine Übernahme von Anbieterrollen. Neue externe Konten können ohne Portalpasswort
angelegt werden. Eine zufällige Zugangsversion bindet Sitzungen und Sicherheitslinks
auch für diese Konten. State, Nonce, PKCE und OIDC-Tokenvalidierung übernimmt Spring.
Anbieterbeweise und Registrierungsentwürfe sind kurzlebig und werden nicht dauerhaft
als OIDC-Objektgraphen gespeichert. Ein externer Login ist kein Passwort-/MFA-Faktor.
Kontaktvertrauen, erneute Sicherheitsbestätigung und Grenzen stehen unter
[Externe Anmeldung](externe-anmeldung.md).

Ein neuer Google-Durchlauf ist kein frischer unabhängiger Sicherheitsnachweis.
Im normalen Betrieb bestätigt er keine sensible Kontoaktion. Die rollen- und
aktionsbezogene [Zwei-Faktor-Authentifizierung](zwei-faktor-authentifizierung.md)
verwendet TOTP oder einmalige Ersatzcodes. Remember-me ersetzt nur den ersten Faktor.
HTTP, alte Vaadin-Ansichten und Push-Verbindungen bleiben bis zur geforderten
Bestätigung eingeschränkt. Die Superadmin-Konfiguration entscheidet nach der bisher
geltenden Regel über ihre eigene Änderung. Entwicklungs-Ausnahmen benötigen eine
separate Startfreigabe und erzeugen keinen echten MFA-Nachweis.

## Verifikation, Passwortreset und E-Mail-Änderung

Anlageweg, administrative Kontofreigabe und Nachweis der aktuellen E-Mail-Adresse
sind getrennte Eigenschaften. Der Nachweis ist `VERIFIED`, `PENDING` oder
`NOT_CHECKED`. Selbstregistrierung und normale administrative Anlage erfordern
Bestätigung. Superadmins können neue Konten ausdrücklich ohne Nachweis bereitstellen.
Startkonten sind standardmäßig ungeprüft nutzbar. Die Option
`email-verification-required=true` verlangt auch dort den normalen Nachweis.

Eine administrative Freigabe ersetzt keinen E-Mail-Nachweis. Eine administrative
Sperre oder Selbstdeaktivierung verhindert dagegen die Bestätigung und den erneuten
Versand. Alte Links dürfen diese Entscheidungen nicht aufheben. Ein erfolgreicher
Link setzt den Nachweis für die aktuelle Adresse auf `VERIFIED`.

Erst nach korrektem Passwort beziehungsweise einem gültigen verbundenen Anbieterbeweis
zeigt `/verify-email` die ausstehende Bestätigung. Der zehn Minuten gültige Nachweis
enthält weder Rollen noch einen authentifizierten SecurityContext. Er ist an Konto,
Adresse und Zugangsversion gebunden. Portal, Administration, Remember-me und der zweite
Faktor werden erst nach dem E-Mail-Nachweis zugänglich. Falsche Zugangsdaten bleiben
allgemein. Die Seite erlaubt nur Hinweise, erneuten Versand und Rückkehr zur Anmeldung.

Administrativer Mailversand prüft aktuelle Rechte und den gespeicherten Kontostand
unter Kontosperre. Ein Formularentwurf wird niemals zur Zieladresse. Passwortreset
wird dort nur für freigegebene Konten mit Portalpasswort und ohne ausstehende
E-Mail-Bestätigung angeboten. Der Versand ändert das Passwort nicht. Neue
Bestätigungsmails und administrative Resetanfragen sind pro Konto auf eine Minute
begrenzt. Bereits offene Bestätigungsmails werden dedupliziert.

Eine administrative Adressänderung verlangt einen neuen Nachweis, widerruft aktive
Bestätigungs-/Resetlinks und die Zugangsversion und entfernt noch wartende
Bestätigungs-/Resetmails. Bereits laufende Mailaufträge werden nicht erneut gestartet.
Eine bestätigte eigene Adressänderung trägt ihren Nachweis dagegen auf die neue
Adresse ein. Der Schutz des letzten nutzbaren Superadmins bleibt bestehen.

Tokens sind begrenzt gültig und werden bei Verwendung verbraucht. Statuswechsel
verlaufen aus ACTIVE in einen terminalen Zustand; verbrauchte/abgelaufene Tokens
werden nicht wieder aktiviert. Gültigkeit verlangt einen Zeitpunkt vor dem Ablauf.
Token-Gültigkeit beträgt mindestens eine Minute, anschließende Aufbewahrung mindestens
zwei Minuten. Fehlerhafte Konfiguration erzeugt keinen neuen Token und keinen Mailauftrag.

Konto, Tokenrotation und zugehöriger Mailqueue-Eintrag werden unter derselben
Kontosperre und Transaktion verarbeitet. Ein Fehler beim Pflicht-Mailauftrag oder
einem verpflichtenden internen Sicherheitshinweis rollt die Fachaktion zurück.
Nicht jeder technische Fehler wird als ungültiger Link verschluckt.

Beim Passwortwechsel bleibt nur die bestätigte eigene Sitzung erhalten. Die
E-Mail-Änderung bindet den Nachweis an Konto, Zieladresse und aktuelle Zugangsversion;
die ausdrückliche Bestätigung widerruft Zugänge. Sicherheitsmails fixieren ihre
Zieladresse; gewöhnliche Queueaufträge verwenden die aktuelle Kontoadresse.

## Sitzungslimit und Remember-me

„Login speichern“ ist ein Switch. Ein separates Cookie `login-remember-choice`
merkt seine letzte ausdrücklich gewählte Stellung für 365 Tage, auch nach dem
Abmelden. Es enthält ausschließlich `true` oder `false`, keine Zugangsdaten und
keinen Anmeldenachweis. Ohne gültige Auswahl bleibt der Switch ausgeschaltet.
Das Cookie ist hostgebunden, HttpOnly und SameSite=Lax sowie bei HTTPS Secure.
Das Umschalten allein meldet niemanden an und erzeugt keinen Remember-me-Token.
Eine vom Browser vorgeschlagene E-Mail-Adresse bleibt dessen Autovervollständigung.

Die öffentliche Sprachwahl speichert `de` oder `en` für ein Jahr im gleich
geschützten Cookie `public-language`. Eine neue Vaadin-Sitzung übernimmt die
gespeicherte Sprache vor der Anmeldung. Ohne gültiges Cookie greift die bisherige
Spracherkennung anhand der Browsereinstellungen. Bei der Anmeldung hat die
Kontosprache Vorrang.

Eine zugelassene HTTP-Sitzung belegt einen Platz. Zusätzliche Tabs teilen diesen Platz.
Gespeicherte Remember-me-Zugänge ohne aktive Sitzung belegen keinen Platz. Logout,
Widerruf und Inaktivitätsablauf geben Plätze frei. Browserfenster schließen ist kein Logout.
Eine Limitreduzierung verdrängt bereits zugelassene Geräte nicht automatisch.

Remember-me verwendet rotierende Tokenserien und gespeicherte Gerätemetadaten.
Neue Serie und Metadaten werden atomar erzeugt. Eine neue geprüfte Anmeldung ersetzt die gespeicherte
Anmeldung dieses Browsers. Logout widerruft dessen Serie; eigene Verwaltung erlaubt
Einzel- und Sammelwiderruf. Ablaufräumung ergänzt keine erfundenen Geräteinformationen.
Die Gültigkeit wird beim Speichern und beim Wiederverwenden einer Anmeldung gelesen.
Bei kürzerer Gültigkeit kann ein vorhandener Zugang früher abgelehnt werden. Der
Browser behält das Ablaufdatum eines bereits ausgestellten Cookies bis zur Erneuerung.
Der Schlüssel für gespeicherte Anmeldungen wird beim Anwendungsstart geladen.

Bei vollem Limit ermöglicht ein kurzlebiger, nach Passwort- oder externer Identitätsprüfung erworbener Nachweis
nur die eigene Sitzungsverwaltung. Er ist keine vollständige Anmeldung und gewährt
keinen Zugriff auf Profil, Administration oder Inbox.

## Serverneustart und Oberfläche

Spring Session JDBC erhält gültige Anmeldung, Sitzungsplatz, ursprüngliche
Authentifizierungsfaktoren und Inaktivitätsfrist in derselben Datenbank. Kontostatus,
Rollen und Zugangsversion werden beim Wiederherstellen frisch geprüft. Ein Neustart
verlängert keine Gültigkeit. Logout und Widerruf bleiben wirksam.
Unter **Konfiguration → Sicherheit → Sitzungs-Timeout** lässt sich die Inaktivitätsfrist
einstellen. Standard sind 30 Minuten. Nach dem Speichern gilt der neue Wert für
neu angelegte HTTP-Sitzungen. Bestehende Sitzungen behalten ihre bisherige Frist.
Dazu zählen auch Sitzungen, die der Aufruf der Anmeldeseite bereits vor dem
Speichern angelegt hat. `APP_CFG_SESSION_TIMEOUT_SECONDS`
setzt den Standard in Sekunden.
Ohne diese Variable kann `app.sessions.timeout-seconds` weiterhin den Standard vorgeben.

Ein abschließender HTTP-Commit einer inzwischen gelöschten Sitzung schreibt nichts
mehr zurück. Sicherheitsattribute einer gelöschten Sitzung bleiben unzugänglich.
Trifft die Löschung einen bereits geladenen Anmeldekontext, wird dieser verworfen.
Alte geschützte UI-/Socket-Aufrufe werden abgewiesen, die Anmeldeseite bleibt erreichbar.
Bei einer Remember-me-Tokenabweichung bleiben Springs vollständiger Widerruf der
gespeicherten Anmeldungen und die Cookieentfernung erhalten. Es wird keine Anmeldung
wiederhergestellt. Die normale Zugriffskontrolle übernimmt den erneuten Anmeldeweg,
statt die Abweichung als unbehandelten Serverfehler weiterzureichen.
Runtime-UIs werden über Vaadins Binding-Lifecycle entfernt, damit eine bereits
laufende Antwort vor dem endgültigen Ablösen ihrer UI abgeschlossen werden kann.

Die Vaadin-Oberfläche wird nach Neustart neu aufgebaut. Formulare, Uploadentwürfe,
UI-Callbacks und Services werden nicht als Sitzungsgesamtgraph gespeichert.
Veraltete UIDL-/Push-Anfragen führen keine Fachaktion nachträglich aus.
Erlaubte dauerhafte Attribute sind begrenzte JSON-Werte mit fester Typauswahl.
Kein Passwort, beliebiger Java-Objektgraph oder Jackson-DefaultTyping im Sessionstore.

Die Anwendung verwendet eine Instanz. Dauerhafte Sessiontabellen machen lokale
Kontosperren und Vaadin-UI-Zustände nicht clusterfähig.

## CSRF, Cookies und Entwicklungswerkzeuge

Spring-CSRF schützt zustandsändernde HTTP-Aktionen einschließlich Login/Logout;
Vaadin Flow hat zusätzlich seinen Transportvertrag. Ausnahmen sind auf tatsächlich
erkannte Transporte begrenzt, keine pauschale Freigabe von `/VAADIN/**` oder allen POSTs.
JSESSIONID ist HttpOnly und SameSite=Lax; Secure richtet sich nach HTTPS.
Remember-me setzt ebenfalls SameSite=Lax und bei HTTPS Secure/HttpOnly.
Im Profil `https` akzeptiert die Anwendung nur die festgelegte äußere HTTPS-Origin;
der interne harmlose Livenessabruf ist die einzige HTTP-Ausnahme. Native
Proxy-Header gelten ausschließlich von den konkret konfigurierten Proxyadressen.
Caddy ersetzt eingehende Forwarded-Header, begrenzt Host/Port und veröffentlicht
in Docker ausschließlich seine eigenen Zugänge. Lokale Prozesse auf derselben
vertrauten IDE-Loopbackadresse sind damit nicht gegeneinander isoliert.
Zertifikatsvertrauen und Rückbau: [Lokales HTTPS](entwicklung.md#lokales-https).

Die H2-Konsole ist standardmäßig abgeschaltet. Bei ausdrücklicher lokaler Freigabe
ist sie auf Loopbackhosts und ihren eigenen Ursprungsschutz begrenzt. Sie ist kein
öffentlicher Administrationsendpunkt. [Lokale Docker-Nutzung](docker/README.md).

## Wartung

`maintenance.mode` pausiert normale Fachaktionen und öffentliche Registrierungs-/
Tokenabläufe. Admins und Superadmins können weiterhin anmelden und wieder freigeben;
sie umgehen weder Passwortschutz noch Sitzungslimit. Vorhandene Sitzungen bleiben
gespeichert. Logout und benötigte UI-Ressourcen bleiben erreichbar. Worker und
Bereinigungsaufgaben laufen weiter. Unlesbare Wartungskonfiguration öffnet den Zugang nicht.

## Daten, Bilder und Zeit

JPA-Versionen erkennen konkurrierende Formularänderungen. Aktivitätsupdates ändern
keine Profilversion. Ein Formular darf keine fehlende Version durch einen aktuellen
Wert ersetzen. Benutzerlöschung entfernt abhängige persönliche Tokens, Bilder und
Empfangseinträge; gemeinsame Mitteilungsinhalte anderer Empfänger bleiben erhalten.

Bildabrufe sind an die aktuelle Anmeldung und Revision gebunden. Der Upload prüft
Format, tatsächliche Bytezahl, Pixelgrenzen und Ausschnitt; Originale und EXIF/GPS
werden nicht gespeichert. Antworten liefern keine fremden Bilder bei fehlenden Rechten.

Aktivität, Anmeldeversuche, Sitzungen und Benachrichtigungen verwenden Instant-Werte.
Geburtstage sind LocalDate. Token- und Mailqueue-Zeiten verwenden lokale Serverzeit;
die konfigurierte Zeitzone muss während eines Laufs konsistent sein.
Sensible Daten gehören nicht in Logs: [Logging](logging.md).
