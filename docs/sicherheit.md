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

Fehlversuche werden je kanonisierter Identität unter einem Hash gespeichert. Das
Zeitfenster und eine Sperre dauern jeweils 15 Minuten; das Versuchslimit ist konfigurierbar.
Ein erfolgreicher Passwortreset setzt den zugehörigen Schutz kontrolliert zurück.

## Verifikation, Passwortreset und E-Mail-Änderung

Nur eine deaktivierte, noch ausdrücklich ausstehende Registrierung darf per
Verifikationslink aktiviert werden. Eine administrative Freigabe oder Sperre beendet
diesen ausstehenden Zustand. Ein alter Link darf keine Adminentscheidung aufheben.

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
E-Mail-Änderung bindet den Nachweis an Konto, Zieladresse und aktuelle Passwortversion;
die ausdrückliche Bestätigung widerruft Zugänge. Sicherheitsmails fixieren ihre
Zieladresse; gewöhnliche Queueaufträge verwenden die aktuelle Kontoadresse.

## Sitzungslimit und Remember-me

Eine zugelassene HTTP-Sitzung belegt einen Platz. Zusätzliche Tabs teilen diesen Platz.
Gespeicherte Remember-me-Zugänge ohne aktive Sitzung belegen keinen Platz. Logout,
Widerruf und Inaktivitätsablauf geben Plätze frei. Browserfenster schließen ist kein Logout.
Eine Limitreduzierung verdrängt bereits zugelassene Geräte nicht automatisch.

Remember-me verwendet rotierende Tokenserien und gespeicherte Gerätemetadaten.
Neue Serie und Metadaten werden atomar erzeugt. Passwortlogin ersetzt die gespeicherte
Anmeldung dieses Browsers. Logout widerruft dessen Serie; eigene Verwaltung erlaubt
Einzel- und Sammelwiderruf. Ablaufräumung ergänzt keine erfundenen Geräteinformationen.

Bei vollem Limit ermöglicht ein kurzlebiger, nach Passwortprüfung erworbener Nachweis
nur die eigene Sitzungsverwaltung. Er ist keine vollständige Anmeldung und gewährt
keinen Zugriff auf Profil, Administration oder Inbox.

## Serverneustart und Oberfläche

Spring Session JDBC erhält gültige Anmeldung, Sitzungsplatz, ursprüngliche
Authentifizierungsfaktoren und Inaktivitätsfrist in derselben Datenbank. Kontostatus,
Rollen und Passwortversion werden beim Wiederherstellen frisch geprüft. Ein Neustart
verlängert keine Gültigkeit; Logout und Widerruf bleiben wirksam. Standardtimeout:
1800 Sekunden, über `app.sessions.timeout-seconds` für neue Sitzungen einstellbar.

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
