# Entwicklung

## Voraussetzungen und Aufbau

JDK 25 und der Maven Wrapper sind verbindlich. Die tatsächlichen Bibliotheksversionen
stehen in `pom.xml`; Node-Abhängigkeiten in `package-lock.json`. Nicht neben diesen
Dateien eine zweite Versionsliste pflegen. Docker Desktop verwendet Linux-Container.

Die Paketnamen beziehen sich auf `src/main/java/de/derpeterson/app/`:

| Paket | Zuständigkeit |
| --- | --- |
| `Application` (Basispaket) | Spring-Boot-Einstieg, App-Shell und zentrale Stylesheets |
| `config` | Spring-/Vaadin-Startkonfiguration, Bootstrap, Schemaanlage, Mail und Threadpools |
| `views`, `views/admin` | Navigierbare Seiten und Administrationsbereiche |
| `ui/base`, `ui/components` | Gemeinsame View-Basen und wiederverwendbare Komponenten/Dialoge |
| `service` | Fachaktionen, Transaktionen, Konto-/Bild-/Benachrichtigungslogik und Mailqueue |
| `repository` | JPA-Abfragen, Datenzugriff und Sperren |
| `model`, `model/enums` | Persistente Datenmodelle und fachliche Typen |
| `security`, `security/session` | Login, Rechte, Wartungsschutz, Geräte und dauerhafte Sitzungen |
| `scheduler` | Zeitgesteuerte Verfügbarkeitsprüfung und Benachrichtigungsbereinigung |
| `i18n` | Sitzungsabhängige Übersetzungen und Sprachanbindung |
| `validation` | Gemeinsame Eingaberegeln und fachliche Validierungsfehler |
| `events`, `websocket` | Sprach-/Rollenereignisse und Live-Aktualisierung angebundener UIs |
| `helper/*` (email, image, logging, text, time, ui) | Begrenzte Hilfsfunktionen für Ressourcen, Diagnose, Zeit und Komponenten |

| Weiterer Bereich | Zuständigkeit |
| --- | --- |
| `shared/src/main/java` | Eigenes Maven-Modul mit typisiertem Textkatalog, Konfigurations-/Routenkonstanten und Token-Zeitregeln |
| `src/main/resources/i18n`, `email` | DE-/EN-Texte und Mailvorlagen |
| `src/main/resources/db` | Aktuelles H2-Anwendungs- und Session-Schema |
| `src/main/resources/META-INF/resources/{custom-theme,icons}` | Gemeinsames Theme und zusätzliche Icons |
| `src/test`, `browser-tests` | Technische Tests, echte Browserjourneys und deren Runner |
| `scripts/sonar` | Lokale Codex-/Sonar-Anbindung, geschützter Zugang und main-Scanner; keine Anwendungslogik |
| `.agents/skills` | Codex-Projektabläufe; Bedienung unter [Arbeiten mit Codex](codex-skills.md) |

Shared-Quellen nicht als Quellordner in andere Module einhängen. Vor einem frischen
Anwendungsbuild oder nach Shared-Änderungen das Modul installieren:

```powershell
.\mvnw.cmd --version
.\mvnw.cmd -f shared/pom.xml clean install
.\mvnw.cmd spring-boot:run
```

## Konfiguration und Datenbank

Spring lädt `application.properties` sowie optional die lokale, nicht versionierte
`.env`. `app.config.defaults.*` bzw. die vorgesehenen `APP_CFG_*`-Variablen liefern
Vorgaben für fehlende Datenbankeinträge. Gespeicherte Adminwerte haben Vorrang.
`ConfigService` bewahrt Strings unverändert; typisierte Getter lehnen ungültige
Zahlen/Booleans ab. SMTP, Remember-me und Workerzahl werden beim Start gebunden.

Bootstrap ist standardmäßig aus. Die optionalen Listen `app.bootstrap.superadmins`,
`admins`, `users` und `guests` legen explizite lokale Testkonten mit genau ihrer
vorgesehenen Rolle an. Alle Einträge werden vor der ersten Anlage geprüft; die
Anlage ist transaktional und überschreibt keine schon vorhandenen Konten.
[Beispiel](bootstrap-accounts.example.properties).

`db/application-schema-h2.sql` beschreibt das aktuelle Anwendungsschema.
Beim Start werden fehlende Tabellen/Indizes angelegt; Hibernate validiert die
Übereinstimmung mit den JPA-Mappings. Bestehende Tabellen werden nicht umgebaut.
Schema- und Modelländerungen gehören zusammen in denselben Arbeitsstand und werden
mit einer frischen isolierten Testdatenbank geprüft. Lokale Testdaten sind austauschbar.
Remember-me legt seine Tabellen in `DeviceTokenRepository` an; Spring Session
verwendet `db/durable-sessions-h2.sql`. Beide Initialisierungen sind idempotent.
Kontoadresse, Remember-me und dauerhafte Sessionidentität unterstützen 255 Zeichen.

Die normale lokale H2-Datei liegt unter `data/`; Docker und der Browserrunner haben
getrennte Datenbanken. Dateien aus `data/`, `logs/`, `.env` und Browserläufen nicht
als Build-Eingaben oder Dokumentationsarchiv übernehmen.

## Build und Änderungen übernehmen

```powershell
.\mvnw.cmd clean verify -Pproduction -Dvaadin.productionMode=true
```

Java-/Konfigurationsänderungen benötigen Build und Neustart. Im Entwicklungsmodus
lädt Vaadin die Quellstyles; bei optimiertem Frontend sind Neubau und anschließendes
Browser-Neuladen erforderlich. `vite.config.ts` ist für eigene Vite-Anpassungen;
generierte Frontenddateien und `target/` nicht von Hand bearbeiten.
Classpathressourcen als Streams laden, damit auch das gepackte JAR funktioniert.

## SonarQube und Codex

Die lokale Anbindung liegt unter `scripts/sonar/`. Sie benötigt die virtuelle
Python-Projektumgebung (Python 3.11 oder neuer), Docker Desktop und den vorhandenen
SonarQube Community Build unter `http://localhost:9000`. Die Versionen von MCP-Image
und Maven-Scanner sind im Helfer festgelegt; das Image ist zusätzlich per Digest gebunden.

Einmalige Einrichtung im Projektroot:

```powershell
# Offizielles Image in der im Helfer festgelegten Version bereitstellen:
docker pull sonarsource/sonarqube-mcp:1.29.0.4488
# Verdecktes lokales Eingabefenster öffnen:
.\.venv\Scripts\python.exe scripts/sonar/sonar.py token
# Projektbezogene Codex-Konfiguration erzeugen:
.\.venv\Scripts\python.exe scripts/sonar/sonar.py configure
```

Unter SonarQube „My Account → Security“ einen **Benutzertoken** erstellen.
Das Konto benötigt Browse für `vaadin-spring-app`, für Scans zusätzlich Execute
Analysis und für ausdrücklich beauftragte Statusentscheidungen die entsprechenden
Issue-/Hotspot-Rechte. Ein Projekt-/globaler Analysetoken ersetzt den MCP-Benutzertoken nicht.
Token ausschließlich im lokalen Fenster eingeben. Die Datei
`%LOCALAPPDATA%/VaadinSpringApp/Sonar/token.dpapi` ist mit Windows DPAPI an den
Windows-Benutzer gebunden und liegt außerhalb des Repositorys. Zum Ersetzen das
Fenster erneut aufrufen. Alternativ ist `SONARQUBE_TOKEN` im aufrufenden Prozess
möglich. Keine Werte in Chat, Skript, Konfiguration oder Befehlsargumente kopieren.

`configure` erzeugt die ignorierte `.codex/config.toml` mit lokalen absoluten Pfaden.
Nach einem Umzug die vorhandene lokale Konfiguration bewusst abgleichen; fremde
Konfiguration wird nicht überschrieben. In Codex das tatsächliche Repository als
vertrauenswürdiges Projekt öffnen und MCP neu laden. Lokale Clients verwenden die
[Codex-MCP-Konfiguration](https://learn.chatgpt.com/docs/extend/mcp?surface=cli);
Werkzeuge in einem ChatGPT-Chat mit lokalem Ausführer sind dadurch nicht automatisch
verfügbar. Im jeweiligen Client einen echten Leseaufruf prüfen. `codex mcp get/list`
verwaltet globale Einträge und ist keine Abnahme dieser Projektkonfiguration.

Der MCP-Start öffnet einen eigenen kurzlebigen Container ohne Arbeitsverzeichnismount,
Docker-Socket oder veröffentlichten Port. Er verbindet sich über
`host.docker.internal:9000` mit dem vorhandenen Sonar-Dienst. Aktiviert sind die für
Projekt, Findings, Regeln, Gate, Metriken und Hotspots benötigten Werkzeuggruppen.
Schreibende Werkzeuge ausschließlich im freigegebenen Sonar-Ausführungsmodus nutzen;
Einrichtung, Analyseplanung und große QA verändern keine Issue-Status.

Zusätzliche technische Leseprüfung, ohne Scan oder Statusänderung:

```powershell
.\.venv\Scripts\python.exe scripts/sonar/sonar.py read
```

Sie prüft Authentifizierung, Hauptbranch, vollständige Findings/Hotspots, Gate,
Metriken und Analyseidentität. Die Ausgabe enthält getrennte Anzahlen sowie die
Quellzuordnung zu lokalem main. `sourceMatchesLocalMain: false` bedeutet abweichenden
Stand; `null` bedeutet fehlende Revision. Fehlende Coverage bleibt fehlend. Ein
Verbindungs-/Seitenfehler ist kein erfolgreicher Abruf mit null Findings.
Im normalen Skill-Ablauf werden die einzelnen Codestellen über MCP abgerufen.

Vollständigen Scan nur als beauftragten Analyseschritt auf sauberem **main** starten:

```powershell
.\.venv\Scripts\python.exe scripts/sonar/sonar.py scan
```

Der Ablauf prüft JDK 25, installiert Shared, prüft den Stil, baut optimiert samt
Anwendungstests und lädt danach den Scan mit fester Quellrevision hoch. Kein Browserlauf.
Feature-Branches und uncommittierte Änderungen werden abgewiesen. Eine lokale
`.codex/sonar/scan.lock` verhindert doppelte Helferläufe; nach einem harten Abbruch
vor Entfernen einer verbliebenen Sperre den tatsächlichen Prozessstand prüfen.
Serverwarteschlange und Quellstand werden vor Upload erneut geprüft. Andere externe
Scanner müssen ebenfalls koordiniert werden; die Sperre erfasst nur diesen Helfer.

Der konkrete Serverauftrag muss erfolgreich enden und zur erwarteten Analyse-ID
und Revision gehören. Ein rotes Gate liefert trotz abgeschlossener Verarbeitung
einen Fehlerstatus. Task-Metadaten stehen nur aktuell unter `.codex/sonar/`;
kein Berichtarchiv und kein manueller JSON-Export sind nötig. Eine gelesene frühere
Analyse ersetzt keinen neuen Scan. Die bestehenden lokalen Scanner-Batchdateien
sind kein Bestandteil dieses zugangsdatenfreien Ablaufs.

Offizielle Grundlagen: [Sonar-MCP-Einrichtung](https://docs.sonarsource.com/sonarqube-mcp-server/setup/quickstart-guides/codex-cli),
[Maven-Scanner](https://docs.sonarsource.com/sonarqube-server/analyzing-source-code/scanners/sonarscanner-for-maven).

## Arbeiten mit Codex

Die [Skill-Anleitung](codex-skills.md) beschreibt alle fünf Abläufe: große Testrunde,
Sonar-Bereinigung, Umsetzung starten, Änderungen prüfen und lokal mergen.
Testbefehle stehen in der [Testanleitung](tests.md).

## Codestil

Bei Codearbeiten diesen Abschnitt lesen und die Regeln bei Umsetzung und Review
anwenden. Die Beispiele illustrieren Schreibweise und Lesbarkeit; sie sind keine
fertigen Implementierungen oder zusätzlichen fachlichen Anforderungen.

### Namen und Methodenaufgaben

Für Menschen lesbare Namen beschreiben die fachliche Bedeutung. Listen heißen im
Plural, einzelne Objekte im Singular; Booleans benennen eine erkennbare Bedingung.
Übliche kleine Schleifenindizes sind in ihrem unmittelbaren Kontext in Ordnung.
Keine kryptischen Kürzel, unnötigen Typpräfixe oder künstlich langen Namen.

| Wenig aussagekräftig | Verständlicher im passenden Kontext |
| --- | --- |
| `list`, `data` | `unreadNotifications` |
| `u` in einer längeren Verarbeitung | `account` |
| `flag` | `hasUnreadNotifications` |
| `process()` | `markNotificationsAsRead()` |

Eine Methode hat eine klar erkennbare Aufgabe; ihr Name passt zu Ergebnis und
Seiteneffekten. Laden, Validieren und Speichern nicht hinter einem vagen Namen
verstecken. Sinnvoll benannte Teilaufgaben auslagern, ohne Transaktions-, Sperr-
oder Fehlergrenzen zu verschieben. Keine starren Zeilenlimits und keine neue
Hilfsmethode für jede einzelne Anweisung.

### Klammern und Kontrollfluss

`if`, `else`, `for`, erweitertes `for`, `while` und `do/while` haben auch bei einer
Anweisung geschweifte Klammern. `else if` darf als Kette stehen. `try`, `catch` und
`finally` übersichtlich gliedern; Fehlerbehandlung nicht mit anderen Aktionen in
eine Zeile pressen. Frühe Rückgaben oder `continue` können Verschachtelung reduzieren,
sofern sie den Ablauf und bestehende Aufräum-/Fehlerverträge erhalten.

Beispiel für einen gut überblickbaren Filter mit bewusstem Umgang mit leeren Namen:

```java
private List<String> collectNonBlankNames(List<String> candidateNames) {
  List<String> names = new ArrayList<>();
  for (String candidateName : candidateNames) {
    if (candidateName == null || candidateName.isBlank()) {
      continue;
    }
    names.add(candidateName);
  }
  return names;
}
```

Einfache kurze Bedingungen dürfen ternär bleiben. Verschachtelte ternäre Ausdrücke
in verständliche Schritte oder fachlich benannte Methoden aufteilen. Eine Schleife
ist oft verständlicher als eine lange Stream-Kette; kurze klare Streams sind ebenfalls
in Ordnung. Die Aufgabe entscheidet, nicht eine bevorzugte Syntax.

### Explizite Typen und sinnvoller Einsatz von var

Explizite lokale Typen sind Standard, besonders bei Service- und Factory-Rückgaben.
Normale und statische Einzelimporte verwenden; keine Sternchenimporte.

```java
List<String> supportedLanguages = List.of("de", "en");
Instant notificationTime = clock.instant();
```

`var` nur verwenden, wenn es einen konkreten Lesbarkeitsvorteil hat: bei anonymen
oder nicht direkt benennbaren Typen sowie bei sehr langen generischen Typen,
die unmittelbar im Konstruktor sichtbar sind. Beispielsweise entfällt hier eine
lange doppelte Typangabe:

```java
var translationsByAccount = new LinkedHashMap<Long, Map<Locale, List<String>>>();
```

`var` ist weder dynamische Typisierung noch ein Laufzeitvorteil. Bei einer unklaren
Rückgabe wie `service.load(...)` den Typ ausschreiben.

### Kommentare und Javadoc

Verträge, notwendige Voraussetzungen, wichtige Seiteneffekte und ungewöhnliche
Fehlerfälle erklären, soweit sie aus Namen und Code nicht klar hervorgehen.
Selbsterklärende Getter oder offensichtliche Schritte brauchen keinen Kommentar.

```java
// Der Grenzzeitpunkt ist bereits fällig; Gleichheit darf keinen Retry verzögern.
boolean retryIsDue = !nextAttemptAt.isAfter(now);
```

Eine Methodendokumentation kann knapp bleiben, muss aber den wichtigen Vertrag nennen:

```java
/**
 * Prüft die Retry-Fälligkeit einschließlich des Grenzzeitpunkts; beide Werte sind nicht null.
 */
private boolean isRetryDue(Instant nextAttemptAt, Instant now) {
  return !nextAttemptAt.isAfter(now);
}
```

Regex direkt daneben auf Deutsch erläutern: Zweck, erlaubte Zeichen, Anker,
Gruppen und bei Ersetzungen die verwendeten Gruppenreferenzen, soweit vorhanden.
Vor einer neuen Regel vorhandene zentrale Validierung prüfen und wiederverwenden.

### Java formatieren und prüfen

Google Java Format (`GOOGLE`, zwei Leerzeichen) ist über Spotless im Root-`pom.xml`
konfiguriert und umfasst Anwendung, Shared und Browsertests. Nach jeder eigenen
Java-Änderungsrunde vor Tests und Abschluss im Projektroot ausführen, auch ohne
weiteren Build/Testlauf; nach späteren Korrekturen wiederholen:

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd spotless:check
.\mvnw.cmd checkstyle:check
```

Spotless ordnet Imports und entfernt unbenutzte Imports. `ratchetFrom=HEAD`
begrenzt die Bearbeitung auf geänderte/neue Java-Dateien. Bei fremden Java-Änderungen
beide Spotless-Aufrufe zusätzlich mit `-DspotlessFiles=...` auf die eigenen Dateien
begrenzen; der Wert ist ein regulärer Ausdruck für absolute Dateipfade.
Beispiel ausschließlich für eine selbst bearbeitete `views/AccountView.java`:

```powershell
.\mvnw.cmd '-DspotlessFiles=.*[\\/]views[\\/]AccountView\.java' spotless:apply
.\mvnw.cmd '-DspotlessFiles=.*[\\/]views[\\/]AccountView\.java' spotless:check
.\mvnw.cmd checkstyle:check
```

Den Filter passend zu den tatsächlich eigenen Dateien wählen; den Ratchet nicht
abschalten. Keine ungefragte projektweite Umformatierung. Checkstyle prüft Klammern
und Sternchenimporte; gute Namen, sinnvolle Typwahl und Methodengliederung bleiben
Teil des Reviews.

### Python formatieren

Ruff-Version und Regeln stehen in `requirements-dev.txt` und `ruff.toml`.
Nur in der virtuellen Projektumgebung installieren:

```powershell
.\.venv\Scripts\python.exe -m pip install -r requirements-dev.txt
```

Nach jeder eigenen Python-Änderungsrunde vor Tests und Abschluss die eigenen
Dateien formatieren und prüfen; nach Korrekturen wiederholen:

```powershell
.\.venv\Scripts\python.exe -m ruff format PFAD
.\.venv\Scripts\python.exe -m ruff format --check PFAD
```

`PFAD` durch explizite eigene Dateipfade ersetzen und bei Leerzeichen zitieren.
Ruff hat hier keinen Git-Ratchet; fremde Dateien nicht mitformatieren.
`ruff check --fix` ersetzt die Formatierung nicht. OpenCode bleibt eingefroren.

Verbindliche Agentenregeln: [AGENTS.md](../AGENTS.md).
Für sinnvolle Ereignisse, Logpegel und sichere Diagnose gilt der [Logging-Vertrag](logging.md).

## Übersetzungen und Ressourcen

Bei Arbeiten an diesen Bereichen die folgenden Hinweise lesen und anwenden.

### Gemeinsamer Textkatalog und Sprachwechsel

- Übersetzungsdateien liegen unter `src/main/resources/i18n/`, Mailvorlagen unter
  `src/main/resources/email/`. Feste Schlüssel und typisierte Getter sind einmalig in
  Shared-`MessageTexts` definiert; keine zweite Liste im Anwendungs-/Testmodul pflegen.
- In Anwendungskomponenten die vorhandenen `MessageProperties`-Getter verwenden.
  Direkte rohe Übersetzungsabfragen bleiben innerhalb der i18n-Schicht.
- Normales `MessageProperties` folgt bei jedem Zugriff der aktuellen Sitzungssprache.
  `MessageProperties.forLocale(provider, locale)` bindet ausdrücklich eine feste Sprache,
  ohne die Sitzung zu lesen oder zu ändern; nur für entsprechend feste Übersetzungen verwenden.
- Für Konfigurationsgruppen `AdminConfigGroup` als Identität verwenden, nicht Properties-
  Schlüssel oder übersetzte Labels. Vorhandene typisierte Methoden für Konfigurations-
  und Statusbeschriftungen nutzen.
- Texte sichtbarer Komponenten über den vorhandenen Sprachwechselmechanismus aktualisieren.
  Dynamische Benachrichtigungen erhalten Titel und Inhalt als `Supplier<String>` über
  `NotificationHelper`; seine Textaktualisierung wertet die Supplier erneut aus.
  Bereits übersetzte Strings nicht vorab festhalten, wenn die Meldung die Sprache wechseln soll.
- Browsertests verwenden die gemeinsamen Getter über `UiTexts` mit fester Testsprache;
  keine eigene DE-/EN-Textliste und keine Erwartung „eine von beiden Sprachen“.
  Details: [Browser-Testanleitung](../browser-tests/README.md).

### Ressourcen im gepackten JAR

Serverseitige Dateien über Classpath-Streams laden, beispielsweise mit
`ClassPathResource.getInputStream()` oder `getResourceAsStream(...)`. Eigene Streams
zuverlässig schließen, üblicherweise mit `try`-with-resources. Ressourcen nicht über
`new File("src/main/resources/...")` oder die Annahme einer normalen Datei im JAR öffnen.
Öffentliche Theme-/Iconressourcen liegen unter `META-INF/resources/`.

`src/main/frontend/generated/`, `vite.generated.ts` und `target/` werden von Werkzeugen
erzeugt und nicht von Hand bearbeitet. Eigene Vite-Konfiguration gehört in
`vite.config.ts`. Die [Theme-Anleitung](theme.md) beschreibt Stylesheets und Ladereihenfolge.
