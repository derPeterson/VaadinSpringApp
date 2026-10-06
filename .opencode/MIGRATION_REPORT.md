# Migrationsbericht – 6. Oktober 2026

## Ergebnis

Der Stand aus der gelieferten `.opencode.zip` wurde in einer separaten
Arbeitskopie nach Python migriert. Das ursprüngliche Vaadin-Projekt und die
Eingangs-ZIP wurden nicht überschrieben. Das Ergebnis liegt als
`opencode-python-migration.zip` mit einem `.opencode/`-Ordner vor.

Umgesetzt sind ausschließlich die vier Module `branch`, `prompt`, `benchmark`
und `usage`; `complete` expandiert auf alle vier. Die 22 Modellzuordnungen
bleiben erhalten. Ihre alten `bench-*.md`-Commands wurden durch `start-*.md`
ersetzt, einschließlich der vier bisherigen forced-Modell-IDs. Zusätzlich
gibt es den generischen `/start`-Command.

Der separate `/feature`-Command wurde anschließend ebenfalls entfernt.
Seine Implementierungs-, Java-25-, Test-, Diff- und Commit-Regeln stehen
vollständig im generischen `commands/start.md`. Die Modellvarianten verwenden
weiterhin diesen gemeinsamen Ablauf; eine zweite Workflow-Datei ist nicht nötig.

Die lokalen Anzeigenamen und Command-Schlüssel entsprechen jetzt dem
Ollama-Namen vor dem Doppelpunkt: `qwen3-coder`, `qwen3-coder-q3-tools`,
`devstral-small-2` und `qwen3.8`. Die vollständigen Modell-IDs inklusive Tags
bleiben unverändert. Die lokale Ollama-Liste wurde gelesen: `qwen3-coder:latest`
und `qwen3-coder:30b` haben beide die ID `06c1097efce0`; im Workflow gibt es
dafür weiterhin genau einen Eintrag. Ollama-Tags wurden nicht gelöscht.

Der Benchmark behält Baseline/Final-Builds, Klassen-/Package-Auflösung,
JaCoCo/Surefire-Metriken, Testzeiten, Korrekturrunden, menschliche Eingriffe,
Commit-/Diff-Metriken, externe CSV/Reports und Findings bei. `diff.patch`
enthält direkt die vollständigen Git-Bytes samt Binary-Patches. Usage wird
als Delta derselben OpenCode-Sitzung erfasst und im Benchmark gespeichert.

Die Prompt-Integration verwendet dieselbe Factory, Config, fachlichen Regeln,
Structured Outputs und Artefakt-Schreiber. Der Kern und sämtliche Provider-/
OAuth-Implementierungen wurden anhand der Eingangs-ZIP bytegenau auf Erhalt
geprüft. Änderungen im Prompt-Verzeichnis beschränken sich auf README,
CLI-Hilfetext und die zwei beschriebenen Smoke-Test-Korrekturen.

## Tatsächlich ausgeführte Prüfungen

| Prüfung | Ergebnis |
|---|---|
| Automatische Python-Suite | **88 Tests, 0 Fehler, 0 Failures, 0 Skips** nach Fehler-/Abbrucharchivierung |
| Echte temporäre Git-Repositories | Branch-/Status-Prüfungen, Commits, Findings, Text-/Binary-Diff und sauberer Endzustand erfolgreich |
| CLI als eigener Prozess | Begin/Finish, sichere JSON-Task-Übergabe und UTF-8-Umsetzungsbericht mit Quotes/Zeilenumbrüchen/Shell-Sonderzeichen erfolgreich |
| Fehlerpfade | Dirty Tree, vorhandene lokale/remote Branches, falscher Branch, tracked Findings, fehlende Usage-Session, Build-/CSV-Fehler korrekt abgewiesen |
| Wiederaufnahme und Locking | Retained State/Findings, Retry, idempotentes Finish und konkurrierender Store-Lock geprüft |
| CSV | Einheitliches Schema; FirstRun-Schema gezielt erweitert und historische Zeile erhalten; fremde Schemas abgewiesen; keine doppelte ID; kulturunabhängige Dezimalzahlen |
| Modellgenerator | 22 Zuordnungen; selected/forced erhalten; manuelle Commands geschützt; Konfigurationsfehler vor Mutation abgefangen |
| Usage-Parser mit Offline-Fixtures | Delta, aktive Messages, Teilintervalle, Retries, mehrere Steps, fehlende Werte, Modell-IDs und Vermeidung von Doppelzählung geprüft |
| Installiertes OpenCode **1.18.32** | Bereinigter echter Export erfolgreich mit demselben Usage-Parser ausgewertet; nur Schema/Metriken gespeichert |
| Live-Prompt-Improver mit ChatGPT | CLI → Config → Factory → Client → Structured Output → Pydantic → Original-/Improved-Artefakte erfolgreich |
| Echte Maven-/JaCoCo-Integration | Zweimal vollständig erfolgreich, zuletzt nach Abschluss der Codeänderungen; Java **25.0.4.1**, Maven Wrapper **3.9.16**, JaCoCo **0.8.15** |
| Syntax und Paketprüfung | Alle Python-Quellen geparst; Prompt-/Katalog-Erhalt, entfernte Legacy-Dateien, ZIP-Integrität und Bytecodefreiheit geprüft |

Der echte Maven-Test verwendete ein isoliertes minimales Java-Projekt, keine
Vaadin-Anwendung, keine Datenbank und keinen Applikationsstart. Der Lauf
ging über die tatsächliche Python-CLI, nicht über gemockte Build-Ergebnisse.

| Maven-Testmetrik | Baseline | Final |
|---|---:|---:|
| Tests | 1 | 2 |
| Failures | 0 | 0 |
| Errors | 0 | 0 |
| Skips | 0 | 0 |
| Line Coverage | 75 % | 100 % |
| Branch Coverage | 50 % | 100 % |

Danach lag der Lauf auf `feature/negative_test` mit sauberem Working Tree.
`report.md`, `result.json`, `results.csv`, archivierte `findings.md` und
`diff.patch` waren vorhanden. `git apply --reverse --check` für den Patch
war erfolgreich. Build-Ausgaben laufen auf stderr, damit CLI-stdout ein
gültiges JSON-Dokument bleibt.

Die ersten Live-Prüfungen wurden durch die eingeschränkte Ausführungsumgebung
am Maven-Cache bzw. Windows Credential Manager blockiert. Nach Freigabe wurden
dieselben Prüfungen erfolgreich ausgeführt. Das waren Umgebungszugriffsfehler,
keine als erfolgreich ausgegebenen fehlgeschlagenen Tests.

## Grenzen und verbleibende Punkte

- Der gesamte Vaadin-/Spring-Anwendungsbuild wurde nicht ausgeführt, da seine
  Quelldateien nicht Teil der zu migrierenden ZIP sind und der Umbau ausschließlich
  die Workflow-Infrastruktur betrifft. Der Maven-Aufruf und seine Metriken wurden
  stattdessen im echten isolierten Java-Projekt geprüft.
- Ein vollständiger `/start complete`-Coding-Lauf in der OpenCode-TUI wurde
  nicht ausgeführt. Command-Dateien/Generator, Python-Lifecycle, echter
  Session-Export, echter Maven-Benchmark und Live-Improver wurden einzeln und
  über die dokumentierten Integrationstests geprüft. Die TUI-Modellauswahl und
  End-to-End-Coding wurde inzwischen im ersten ConfigService-Run vom Nutzer ausgeführt.
  Die fünf FirstRun-Korrekturen wurden inzwischen anhand der Artefakte des
  zweiten ConfigService-Laufs geprüft. Der neue Bericht-/Findings-Fix wurde
  lokal über die automatische Suite und echte CLI-Prozesse geprüft.
  Plugin-Ladung und Tool-Ausführung sind inzwischen im echten Server 2.0.19
  geprüft; die aktive ID wird automatisch übergeben.
- Usage endet am Finish-Snapshot. Laufende Inference-Schritte und die danach
  gesendete Abschlussantwort sind nicht vollständig enthalten. Requests zählt
  abgeschlossene Inference-Schritte, nicht garantiert alle HTTP-Versuche.
- Token- und Kostenwerte stammen aus OpenCode, fehlende Werte bleiben
  unbekannt. `cost: 0` wird als gemeldete Schätzung erhalten und ist kein
  Beleg für reale kostenlose Nutzung. Externe Improver-Aufrufe sind keine
  OpenCode-Session-Nutzung.
- OAuth-Refresh-Lock über mehrere Prozesse/Projekte, automatisches Re-Login,
  spezifisches `response.incomplete`-Handling und alte Keyring-Chunks wurden
  wie gewünscht nicht in den unveränderten Improver eingebaut. Der neue
  Workflow-Store-Lock schützt nur Run-Zustand und CSV.
- Frühere PowerShell-CSV-Schemas werden nicht übernommen. Vorhandene Python-
  Runs einschließlich FirstRun bleiben erhalten; dessen CSV-Schema wird beim
  nächsten Eintrag gezielt erweitert. Historische Run-Dateien werden nicht verändert.
  Es gibt
  keine Bereinigung alter Commands durch den Generator. Er verwaltet allein
  seine eigenen `start-*.md`-Dateien mit dem aktuellen `MARKER`.

## Einspielen und erster Auftrag

### Nachtrag: aktive Sitzung unabhängig von der Oberfläche

Der erste Desktop-Probelauf stoppte vor `begin`, weil die separate Windows-CLI
die aktuelle Desktop-Sitzung nicht kannte. Die Sitzungs-ID war vorhanden,
`opencode.cmd export` lieferte dennoch „Session not found“. Das saubere
Arbeitsverzeichnis ist eine separate Voraussetzung.

`plugins/workflow-usage.js` stellt jetzt `workflow_usage_snapshot` bereit.
Das Tool erhält die tatsächliche Session-ID aus OpenCodes Tool-Kontext und
verwendet in V1 den bereitgestellten SDK-Client. In V2 registriert der
Default-Export mit `id/setup` das Tool über `ctx.tool.transform`; die öffentliche
Client-Abhängigkeit 2.0.19 liest den vollständigen Export. Dienstregistrierung,
Version und Prozess-ID bestätigen, dass genau der aktive Server angesprochen
wird. `session.context` wird wegen fehlender Nachrichten vor Komprimierungen
nicht als Export-Ersatz verwendet. Vor Begin und Finish werden
getrennte bereinigte Exporte geschrieben; das bestehende Python-Usage-Modul
validiert Sitzung, Projekt, Frische und Messzeitpunkt. Es gibt keine Suche nach
der neuesten Sitzung, keine öffentliche Freigabe und kein zusätzliches
fachliches Modul. Die Sperrdatei enthält den zusätzlich benötigten V2-Client.

Die zusätzlichen Prüfungen verwenden das echte Plugin und die Clients
**1.18.32 / 2.0.19** mit einem kontrollierten HTTP-Transport, keine Modellanfragen:
**11 Node-Tests erfolgreich**, einschließlich Server-/Auth-Erhalt, 151 Messages
ohne künstliches Limit, konkurrierenden Sessions, Temp-Dateien, fehlenden
Sitzungen, falschen Serverprozessen, V2-Komprimierungen und Entfernung von
Text-/Credential-Daten. **54 Python-Tests erfolgreich**. Die Python-Suite prüft
auch einen vollständigen Complete-Lifecycle mit zwei SDK-Snapshots, einem
echten temporären Git-Repository und gemockten Maven-/Prompt-Aufrufen.
Ein isolierter echter Server **2.0.19** hat das lokale `.js`-Plugin geladen,
das registrierte Tool ausgeführt und dessen Session über den nativen Client
exportiert. Die Session-ID stammt aus dem Command-/Tool-Aufrufkontext. Zusätzlich
wurde der reale Desktop-Export mit 150 Assistant-Messages und einer Komprimierung
durch den Adapter und den Python-Parser geprüft. Eine alte Nachricht hat keine
Token-/Kostenwerte; solche Lücken bleiben unbekannt und werden nicht als 0 ergänzt.
Kein
Modell wurde angefragt; der produktive Desktop-Server wurde nicht verändert.
Vier verschiedene Oberflächen und ein kompletter Coding-Lauf wurden damit
nicht live getestet. Fehlende Komprimierungszeiten und V2-Retry-Verläufe bleiben
unbekannt. Die gesamte Workflow-Dauer wird weiterhin separat gemessen.

Zum Nachrüsten des bereits installierten Stands: README, `commands/start.md`,
`.gitignore`, `scripts/workflow/usage.py`, `scripts/workflow/cli.py` und die
Tests und beide NPM-Dateien aktualisieren sowie `plugins/workflow-usage.js` und
`scripts/workflow_usage_bridge.mjs` hinzufügen.
`npm --prefix .opencode ci` aus dem Projektroot ausführen, den tatsächlichen
Desktop-Dienst mit dessen 2.0.19-Binary neu starten, Änderungen vor dem
Benchmark auf `main` committen und dann denselben
ConfigService-Auftrag neu senden. Die 22 Modell-Commands und der Katalog
haben sich durch diesen Fix nicht verändert.

Vorhandene `.opencode` sichern und durch den Ordner aus der ZIP ersetzen.
Nicht nur darüberkopieren: sonst bleiben die abgelösten Commands aktiv.
Lokale Zusatzdateien aus der Sicherung gezielt übernehmen, Dependencies mit
`requirements.txt` bereitstellen. Die ausführliche Anleitung steht in `README.md`.

Beispiel bei sichtbar ausgewähltem GPT-6.1 Sol:

```text
/start complete userservice_tests UserService gpt61-sol Create complete tests for UserService.
```

Keine Reviewer-, RAG- oder sonstigen fachlichen Zusatzmodule wurden gebaut.

## FirstRun: fünf Korrekturen nach dem ersten echten Auftrag

1. `prepare` prüft Usage und misst die Baseline, bevor `begin --id` den Run
   startet. Nach prepare ist ein neuer SDK-Snapshot erforderlich. Start und
   Ende von Duration und Usage entsprechen exakt den SDK-Capture-Zeitpunkten.
   Message-/Reasoning-Intervalle werden an beiden Grenzen abgeschnitten.
   `MessageElapsedSeconds` ersetzt die irreführende Bezeichnung
   `InferenceSeconds`: Tool-/Wartezeiten bleiben enthalten, überlappende
   Messages dürfen zusammen länger als die Laufzeit sein. Tokenwerte eines
   erst später abgeschlossenen Requests werden nicht künstlich zeitanteilig
   verteilt; diese Grenze ist in der README erklärt.
2. Die README empfiehlt frische Sessions, denselben Ausgangscommit und
   identische Module/Improver-Einstellungen für Modellvergleiche. Der
   Workflow erstellt oder wechselt Sessions nicht automatisch.
3. Kosten erhalten einen expliziten Status. Bei gemeldeter 0 steht im Report
   „OpenCode meldet 0 USD; tatsächliche Kosten unbekannt.“ Keine erfundenen
   Preise oder Abrechnungsaussagen.
4. Der Workflow-Adapter speichert die tatsächlich an die bestehende Factory
   übergebenen Improver-Provider-/Modellwerte und Dauer in
   `prompt-metadata.json`, Report und CSV. Config, Kern, OAuth und Provider
   bleiben unverändert. Die Metadaten belegen die lokalen Einstellungen,
   nicht eine unabhängig bestätigte serverseitige Modellzuordnung.
5. `benchmark-before.json` und `benchmark-after.json` archivieren die
   JaCoCo-Zeile der Zielklasse, Surefire-Suitenzähler/-zeiten und erfolgreiche
   Maven-Aufrufe mit Exitcodes/Java-Major. Counts und Coverage lassen sich
   unabhängig aus diesen Run-Dateien nachrechnen. XML-Properties und
   System-/Console-Inhalte werden nicht übernommen; vollständige Build-Logs
   und ein kryptografischer Nachweis sind damit nicht verbunden.

Der neue Complete-Integrationstest lief über die echte Python-CLI in einem
isolierten Java-25-/Maven-/JaCoCo-Projekt: 1 → 2 Tests, 75 → 100 % Zeilen,
50 → 100 % Branches, keine Fehler oder Skips. Die bestehenden Improver-
Factory-, Structured-Output- und Artefaktpfade wurden mit einer kontrollierten
Offline-Antwort geprüft; Usage-Snapshots waren Fixtures, keine bezahlten
Modellanfragen. Messgrenzen, Prompt-Metadaten und beide Benchmark-Belege
wurden aus den erzeugten Dateien geprüft. Der vollständige FirstRun-CSV-
Datensatz blieb beim Anhängen der neuen Ergebniszeile erhalten. Der Patch
bestand `git apply --reverse --check`, der Feature-Branch war abschließend
sauber. Zusätzlich bestanden 60 Python- und 11 Node-Tests.

Das Änderungspaket `opencode-benchmark-quality-fix.zip` enthält ausschließlich
die geänderten Dateien. Auf den installierten V2-Fix darüberkopieren. Keine
neuen Dependencies, Katalogänderungen oder Änderungen am Plugin; für diesen
Patch ist kein erneutes `npm ci` erforderlich. Infrastruktur auf main
committen und für den nächsten vergleichbaren Lauf eine frische Sitzung
verwenden. Der erste Run und seine fachlichen Findings bleiben erhalten;
ConfigService wurde in diesem Schritt nicht verändert.

## Umsetzung und offene Findings getrennt

`report.md` enthält jetzt einen eigenen Abschnitt **Umsetzung und
Verhaltensänderungen**. Dort erklärt OpenCode abgeschlossene Arbeiten,
geänderte Verträge, Entscheidungen sowie Prüfungen und Grenzen. Die Behebung
mitgegebener Findings wird als Umsetzung dokumentiert. `findings.md` ist für
neu beobachtete oder weiterhin offene Probleme reserviert. Wurden keine
weiteren offenen Probleme beobachtet, steht dort ausdrücklich:
**„Keine weiteren offenen Findings festgestellt.“**

Der gemeinsame `/start`-Ablauf schreibt die Erklärung als temporäre UTF-8
Markdown-Datei außerhalb Git und übergibt sie mit `finish --summary-file`.
Python behält den Text im vorhandenen Run-Zustand und übernimmt ihn in den
generierten Report. Ein zusätzliches dauerhaftes Run-Dokument oder Modul
wurde nicht eingeführt. Die temporäre Übergabedatei bleibt Eigentum des
Aufrufers. Die Angaben entstehen vor dem abschließenden Usage-Snapshot.

Fehlende Findings-Angaben bedeuten unbekannten Status; Python erzeugt dann
keine unbelegte Aussage über Fehlerfreiheit. Ein bei direkten CLI-Aufrufen
nicht übergebener Umsetzungsbericht wird ebenfalls als fehlend ausgewiesen.
Leere, fehlende, ungültig kodierte oder innerhalb Git liegende Übergabedateien
werden vor dem finalen Build und einer Zustandsänderung abgewiesen.

Bei Build-/CSV-Fehlern bleibt die Erklärung für einen Retry erhalten. Nach
weiteren Task-Commits wird eine überholte Erklärung verworfen, wenn kein
aktueller Text übergeben wird. Abgeschlossene Runs werden bei wiederholtem
Finish nicht umgeschrieben. Vorhandene Findings bleiben bytegetreu archiviert;
historische Benchmark-Läufe und das CSV-Schema bleiben unverändert.

**63 Python-Tests und 11 Node-Tests erfolgreich.** Geprüft wurden insbesondere
offene Findings getrennt vom Report, ausdrücklich keine weiteren Findings,
fehlende Angaben, UTF-8 mit BOM, Pfade mit Leerzeichen, ungültige Übergaben,
Berichtserhalt bei CSV-Fehlern, überholte Berichte nach neuen Commits und ein
idempotenter Abschluss. Begin/Finish liefen auch als echte CLI-Prozesse in
temporären Git-Repositories. Der Maven-Aufruf wurde für diese reine
Berichtsänderung nicht erneut live ausgeführt; die bestehende Build-/Usage-
Logik bleibt erhalten und wird von der Regression-Suite mitgeprüft.

Das Änderungspaket `opencode-report-findings-fix.zip` enthält die sechs
geänderten Dateien für den bisherigen `opencode-benchmark-quality-fix`-Stand.
In `.opencode` darüberkopieren. Keine neuen Dependencies, Katalog- oder
Pluginänderungen; kein erneutes `npm ci` erforderlich. Der vollständige Stand
in `opencode-python-migration.zip` wurde ebenfalls aktualisiert.

## Analyse-Ablauf und lesende Statusabfrage

Als nächstes Paket wurden die Punkte 1 und 4 der Workflow-Verbesserungen
umgesetzt. Änderungen wurden ausschließlich in der separaten Arbeitskopie
vorbereitet. Der parallel aktive UserService-Auftrag im Vaadin-Projekt wurde
nicht verändert oder unterbrochen. Die neue Statusabfrage wurde lesend am
echten Store geprüft und erkannte diesen Run als aktiv registriert.

Der Request kennt jetzt `task_mode`: `analysis` für reine Analysen,
`implementation` für Änderungsaufträge. Der gemeinsame Slash-Command setzt
diesen Wert anhand des ursprünglichen Auftrags, nicht anhand zusätzlicher
Improver-Vorschläge. Die Slash-Syntax und die vier Module bleiben erhalten.
Direkte Requests ohne Feld verwenden das bisherige Implementierungsverhalten.

Bei einer Analyse sind Task-Dateiänderungen, Fixes und Task-/Leercommits
ausdrücklich ausgeschlossen. Die ausgewählten Branch- und Benchmark-Module
dürfen weiterhin ihre Arbeit durchführen. Python weist Finish ab, wenn HEAD
vom Start-Commit abweicht; uncommittete Änderungen scheitern an der bestehenden
Clean-Tree-Prüfung. Es findet kein automatischer Reset statt. Erfolgreiche
Analysen archivieren Findings und Arbeitsbericht samt Benchmark/Usage und
leerem `diff.patch`. Der Reportabschnitt heißt jetzt **Durchgeführte Arbeit
und Verhaltensänderungen** und nennt die gespeicherte Auftragsart. Bei älteren
Runs ist eine fehlende Auftragsart als nicht erfasst erkennbar. Das CSV-Schema
wurde nicht erweitert; die Auftragsart liegt in Zustand, Begin-/Statusausgabe
und Report vor. Bereits abgeschlossene Runs bleiben unverändert.

`status` zeigt ohne ID ausschließlich den aktiven Run, sonst `idle`.
`status --id <Run-ID>` liest gezielt einen vorhandenen historischen Run.
Die Ausgabe enthält gespeicherte Phase, Auftragsart, Module, Branch, Pfade,
vorhandene Hauptartefakte und Hinweise zu nächsten Schritten. Die Abfrage
läuft ohne Git-Befehle, Provider-/Build-Aufrufe, Dateischreibzugriffe oder
Store-Lock. Sie bestätigt keinen aktuellen Git-Zustand oder laufenden Prozess;
bei vorbereitendem/abschließendem Zustand muss ein noch laufender Prozess
zunächst abgewartet werden. Eine während des Lesens wechselnde aktive Run-ID
wird durch erneutes Lesen berücksichtigt. Fremde Projekte, ungültige IDs und
fremde Artefaktpfade werden abgewiesen. Fehlerdetails werden nicht ungefiltert
in die Statusausgabe übernommen.

Der CLI-Ausgabekanal wurde ausdrücklich auf UTF-8 gesetzt, damit die neuen
deutschen JSON-Statushinweise auch bei Windows-Ausgabeumleitung korrekt
ankommen. Dies wurde im echten CLI-Prozess geprüft.

**72 Python-Tests und 11 Node-Tests erfolgreich.** Ergänzt/geprüft wurden
insbesondere vollständige Analyse mit allen vier Modulen und kontrollierten
Improver-/Usage-Fixtures, unveränderte Commits und leerer Diff, Abweisung
unerwarteter Analyse-Commits ohne Verwerfen der Arbeit, Status ohne Store,
Status während eines gehaltenen Writer-Locks, historische/abgebrochene Runs,
ältere Zustände ohne Auftragsart, Fremd-/Fehlzustände und ein paralleler
Zustandswechsel. Echte temporäre Git-Repositories und separate CLI-Prozesse
wurden verwendet; für dieses Paket wurden keine Live-Modellanfragen oder
erneuten Maven-Builds gestartet. Die Build-/Usage-Regressionstests bestanden.

`opencode-analysis-status-fix.zip` enthält die sechs geänderten Dateien auf
Basis von `opencode-report-findings-fix.zip`. Erst nach Abschluss des parallel
laufenden Auftrags im Projektroot entpacken und in `.opencode` darüberkopieren;
Infrastruktur anschließend auf main committen. Kein erneutes `npm ci`
erforderlich. Die vollständige Migrations-ZIP ist ebenfalls aktualisiert.
Die weiteren Punkte (abgebrochene Benchmarks auswerten, Improver-Fehlerfälle
und zusätzliche Improver-Usage) sind in diesem Paket noch nicht umgesetzt.

## Fehlgeschlagene und abgebrochene Benchmark-Läufe

Punkt 2 wurde anschließend als eigenes Paket umgesetzt. Workflow-Fehler nach
Run-Anlage erscheinen als vorläufiges `failed`-Resultat in Report, JSON und
Benchmark-CSV; der Lifecycle bleibt wiederaufnehmbar, wo dies bisher möglich
war. `failures.json` und Zustand enthalten den Fehlerverlauf mit Zeitpunkt,
Phase und Exception-Typ. Die CSV enthält einen Datensatz pro Run-ID. Ein
erfolgreicher Retry ersetzt den vorläufigen Fehlereintrag, ohne andere Läufe
zu verändern oder die Fehlerhistorie zu verlieren.

`abort` archiviert mit `--reason` einen bewussten Abbruch oder mit `--failed`
einen endgültigen Fehlschlag. Bekannte Eingriffs-/Korrekturzähler sind optional
übergebbar; unbekannte Werte werden nicht als 0 ausgegeben. Vor einer erfolgreichen
Archivierung wird der aktive Lauf nicht freigegeben. Bei einem Schreibfehler
bleibt `stopping` für einen erneuten Abort erhalten. Entscheidung, Zeit und
Zähler des ersten Abort-Versuchs bleiben dabei gleich. Ein erfolgreicher Stop
ist idempotent; erfolgreich abgeschlossene Runs können nicht abgebrochen werden.

Unvalidierte Endcommit-, Diff- und Nachher-Werte bleiben unbekannt. Vorhandene
Artefakte aus früheren Abschlussversuchen werden erhalten, aber nicht als
geprüfter Abschluss ausgegeben. Ohne Begin bleibt die Dauer unbekannt. Ein
Abort führt keine Git-, Build-, Provider- oder Usage-Capture-Aufrufe aus und
verändert weder Task-Dateien noch Findings. Bereits validierbare Usage-Teilmessungen
behalten ihre echten Zeitgrenzen; ohne Nachher-Snapshot bleibt Usage unbekannt.
Die ursprüngliche Exception bleibt bei einer zusätzlichen Archivierungsstörung
sichtbar, zusammen mit dem Hinweis auf die unvollständige Archivierung.

Das CSV-Schema wurde um `Outcome`, `Terminal`, `FailureCount`,
`LastFailurePhase`, `LastFailureType` und `StopReason` ergänzt. Bisherige
Python-CSV-Zeilen werden samt vorhandenen Werten erhalten und als erfolgreich
abgeschlossen markiert: Die bisherigen Versionen schrieben ausschließlich
erfolgreiche Abschlüsse in diese Datei. Neue Fehlerdetails historischer Läufe
bleiben unbekannt; historische Run-Dateien werden nicht umgeschrieben.

**88 Python-Tests und 11 Node-Tests erfolgreich.** Neue Prüfungen decken
fehlgeschlagene Baseline/Improver/Final-Builds, Ersetzen eines Fehlereintrags
beim Retry, Fehlerhistorie, schmutzige Task-Dateien beim Abort, Stop vor Begin,
fehlende und bekannte Usage-Teilmessungen, Austausch eines ungültigen Finish-
Snapshots, gesperrte CSV, idempotente und endgültige Stops, historische CSV-
Werte, Erhalt anderer Runs und einen echten CLI-Prozess mit UTF-8-Grund ab.
Echte temporäre Git-Repositories wurden verwendet. Builds und Provider waren
kontrollierte Offline-Fixtures; keine Live-Modellanfragen, Anwendung oder
neuen Maven-Projektbuilds wurden gestartet. Prompt-Kern/Provider, Modellkatalog,
Wrapper, NPM-Dependencies und Plugin blieben unverändert.

`opencode-benchmark-outcomes-fix.zip` enthält die sieben geänderten Dateien
auf Basis des zuletzt gelieferten Analyse-/Status-Pakets. Erst nach Abschluss
des laufenden OpenCode-Auftrags im Projektroot in `.opencode` darüberkopieren.
Kein erneutes `npm ci` erforderlich. Die komplette Migrations-ZIP wurde ebenfalls
aktualisiert. Die beiden verbleibenden Punkte Improver-Fehlerbehandlung und
separate Improver-Usage sind für ein gemeinsames Folgepaket vorgesehen.
