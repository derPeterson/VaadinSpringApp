# Dauerhafte Anmeldung und neu aufgebaute Vaadin-Oberfläche

Stand: 21.10.2026. Auf ausdrücklichen Benutzerwunsch zusätzlich zum Sonar-Paket.

## Sichtbarer Vertrag

Ein Serverneustart erhält eine noch gültige Anmeldung und ihren bisherigen
Sitzungsplatz, mit und ohne Remember-me. Voraussetzung sind dieselbe Datenbank,
gültige Cookiekennung und unveränderte relevante Sicherheitskonfiguration.
Neustart verlängert weder Inaktivitätsfrist noch Authentifizierungsfaktoren.
Logout und Widerruf bleiben über Neustarts wirksam. Aktuelle Rollen und Kontostatus
werden beim Lesen erneut aus dem Kontobestand geladen; gesperrte, gelöschte oder
seitdem mit einem anderen Passwort ausgestattete Konten werden zurückgewiesen.
Der eigene bestätigte Passwortwechsel aktualisiert nach Commit ausdrücklich nur
die beibehaltene aktuelle Sitzung; andere Sitzungen bleiben ungültig.

Die Vaadin-Oberfläche wird nach Neustart frisch aufgebaut. Offene Formulare,
Auswahlzustände, Uploadentwürfe und Callbacks werden nicht gespeichert oder
nachträglich ausgeführt. Ein alter UIDL-/Push-Transport fordert ausschließlich
ein neues Dokument an. Wartung führt dabei weiterhin zur Wartungsseite.

Ein Browserprozess-Neustart ist etwas anderes: JSESSIONID bleibt ein HttpOnly-
Sitzungscookie, SameSite=Lax, Secure abhängig von HTTPS. Ohne Remember-me besteht
keine Zusage über Browserschließen hinweg. Remember-me behält seine bestehende
Tokenrotation, Laufzeit und gesonderte Widerrufsregeln. Ein geschlossenes Fenster
gibt den serverseitigen Platz erst bei Timeout oder ausdrücklichem Widerruf frei.

## Speicherung und Grenzen

Spring Session JDBC 4.1.1 verwendet in der bestehenden H2-Datenbank die zusätzlichen
Tabellen SPRING_SESSION und SPRING_SESSION_ATTRIBUTES. Feste idempotente DDL legt
Tabellen und Indizes beim Start an. Bestehende Tabelleninhalte werden nicht
zurückgesetzt. Attribute sind versionierte JSON-Werte mit einer festen Typauswahl
und Größenbegrenzung: Authentifizierungsmemento, CSRF-Token, Admission-Metadaten,
Principalindex, Widerrufsflag und zugehörige Remember-me-Serie. Der Bestand enthält
dadurch sicherheitsrelevante Token-/Sitzungsdaten und gehört zum geschützten
Datenbankbestand. Er wird niemals ins Log ausgegeben.

Passwort und Spring-Servicegraph werden nicht serialisiert. Gespeichert wird ein
Fingerabdruck des vorhandenen kodierten Passworts zur Erkennung von Änderungen.
Kontorollen werden frisch gelesen. Echte FactorGrantedAuthority-Werte behalten ihre
ursprüngliche Zeit; ein Neustart erzeugt keine frische Passwortbestätigung.
Kein Java-ObjectInputStream, Jackson-DefaultTyping oder beliebige Klassen aus der
Datenbank. Unzulässige gespeicherte Authentifizierung wird entfernt; ein abgelaufener
Registrierungseintrag bleibt bis zur HTTP-Invalidierung/Timeout als Widerrufsnachweis
erhalten. Er zählt nicht als aktiver Platz. Die nächste alte Fachanfrage wird
ausdrücklich abgewiesen und
erfordert eine neue Anmeldung. Ein Datenbankausfall schaltet nicht auf ein
unbegrenztes lokales Register um.

Nicht freigegebene Attribute einschließlich VaadinSession, Locks, Services,
Komponenten und befristeter Login-Recovery-Proof bleiben ausschließlich im
Prozess. Der Proof muss nach Neustart durch erneute Passwortprüfung erworben
werden. UI-Werte und echte serialisierbare Callbacks bleiben lesbar; verwaltete
Services werden bei einem erlaubten Komponenten-Roundtrip aus dem aktuellen
Servlet-Spring-Kontext aufgelöst. Entity-basierte Editoren lehnen Java-Passivierung
ausdrücklich ab. Vollständige UI-Passivierung ist kein Teil dieses Vertrags.

Die Zulassung bleibt auf **eine Anwendungsinstanz** begrenzt. Datenbankpersistenz
macht die bestehenden lokalen Kontosperren und Vaadin-UIs nicht clusterfähig.
Mehrere App-Instanzen oder Rolling Deployments erfordern ein eigenes Paket.
Registrierungen zählen bereits vor Veröffentlichung des SecurityContext und
werden bei Fehlern zurückgenommen. ID-Wechsel hält den gemeinsamen Laufzeitmutex
und existierende Wrapper zusammen; alte Cookiekennungen bleiben ungültig.

## Einführung

Shared-Modul und Anwendung bauen, anschließend Anwendung neu starten. Beim ersten
Upgrade sind bisherige rein lokale Sitzungen nicht in den neuen Tabellen: erneute
Anmeldung erforderlich; bestehende Remember-me-Einträge behalten ihren Vertrag.
Ein Refresh allein lädt die neuen Java-Komponenten nicht.

Der Standard für Inaktivität beträgt 1800 Sekunden. Optionaler Startwert:
`app.sessions.timeout-seconds` beziehungsweise `APP_SESSIONS_TIMEOUT_SECONDS`.
Nur positive Werte; Änderung gilt für neu angelegte Sitzungen. Es gibt kein neues
UI-Konfigurationsfeld. Die bisherigen Standard-Cookiepfad-/HTTPS-Regeln bleiben
beim Spring-Cookie-Serializer. Ablaufbereinigung für JDBC und lokalen UI-Cache
läuft minütlich. Für Neustarterhalt müssen Datenbankvolume und Remember-me-Key
erhalten bleiben; ein frischer Testdatenbankbestand kann keine alten Sitzungen
wiederherstellen. Ein Schemawechsel zu einer anderen Datenbank ist nicht enthalten.

Es wurden ausschließlich eigene isolierte H2-Testdaten und eigene Docker-
Testumgebungen verwendet. Eine Bestandsdatenbank oder laufende Entwicklungs-
anwendung wurde für dieses Paket nicht geöffnet oder neu gestartet.

## Nachweise

Konkrete Testergebnisse und verbleibende Lücken stehen im
[Paketbericht](sonar-ui-sessions-2026-10-21.md). Die Browserfreigabe umfasst die
gezielte DE-/EN-Abnahme, keine vollständige Gesamtrunde.

Frameworkgrundlagen: [Spring Session JDBC](https://docs.spring.io/spring-session/reference/configuration/jdbc.html),
[Spring Session mit Boot](https://docs.spring.io/spring-session/reference/guides/boot-jdbc.html),
[Vaadin Spring Scopes](https://vaadin.com/docs/latest/flow/integrations/spring/scopes).
