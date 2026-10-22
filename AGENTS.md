# Projektregeln für Coding-Agenten

## 1. Orientierung und Arbeitsweise

- Zuerst `README.md` lesen, danach gezielt die für die Aufgabe relevanten Dateien,
  Aufrufer und Abhängigkeiten untersuchen. Eine umfassende Projektanalyse nur
  durchführen, wenn die Aufgabe und das Verständnis ihrer Zusammenhänge sie erfordern.
- Aussagen der Dokumentation mit dem aktuellen Code abgleichen, soweit die Aufgabe
  davon abhängt. Abhängigkeiten (`node_modules/`), generierte Dateien, `data/` und
  `logs/` nicht pauschal durchsuchen.
- Zugangsdaten, `.env`, Datenbanken und Logs nicht ungefragt lesen.
- Auf Deutsch kommunizieren.

## 2. Entwicklungsstand und Daten

- Ausschließlich lokale Entwicklung mit austauschbaren Testdaten, kein produktiver
  Einsatz. Der gemeinsam beschlossene aktuelle Stand gilt; historische Schemata,
  Migrationen, Backfills und vorsorgliche Kompatibilitätswege nicht unterstützen.
- Datenmodell und aktuelles H2-Schema gemeinsam ändern; JPA validiert das Schema aus
  `app/src/main/resources/db/application-schema-h2.sql`. Änderungen mit frischen isolierten
  Testdaten prüfen. Alte Mechaniken nicht zur Unterstützung überholter Stände behalten.
- Aktuelle Sicherheits-, Eingabe-, Rechte-, Widerrufs- und Konsistenzprüfungen bleiben
  erhalten und sind keine Altlasten.
- Mit einer stabilen Version und tatsächlichem Livebetrieb Datenerhalt und Migrationen
  ausdrücklich neu bewerten; sie jetzt nicht vorsorglich einbauen.
- Abgestimmte Entscheidungen und geltende Funktionsverträge gelten, bis sie bewusst
  geändert werden. Nur konkrete neue Widersprüche erneut ansprechen.

## 3. Dokumentation und Aufgabenlisten

- Dokumentation beschreibt den aktuell gültigen Zustand und wesentliche Entscheidungen
  mit Vorteilen/Grenzen. Keine Triage-CSV, Nachscan-/Updatechroniken oder QA-Berichtsserien.
- `TODO.md` enthält nur offene Arbeiten; erledigte Aufgaben entfernen.
- Vor der Planung neuer Arbeiten `BUGS.md` lesen. Die Datei enthält bestätigte offene
  Anwendungsfehler, kurz nach Dringlichkeit sortiert
  mit Verweis auf Fehlerdetails. Diese Bugs haben Vorrang vor allen anderen Aufgaben.
  Erst nach Behebung und passendem erfolgreichen Nachtest entfernen.
- `docs/testbedarf.md` sammelt offene automatisierte Prüfungen, getrennt nach Browser-
  und JUnit-Tests: Szenario, Erwartung und Zustand offen oder wartend mit Grund,
  beispielsweise einer ausstehenden Benutzerentscheidung. Auch größere zurückgestellte
  Testaufgaben erhalten. Erst nach passender Abdeckung und erfolgreicher Prüfung entfernen;
  keine parallelen Übergabelisten. Menschliche Abnahmen stehen im manuellen Katalog.
- `docs/status.md` beschreibt die letzte große Testrunde samt Ergebnissen und offenen
  Fehlerdetails. Einzelprüfungen ersetzen diesen Gesamtstand nicht. Ungelöste Fehler
  beim Ersetzen übernehmen, auch wenn sie nicht erneut aufgetreten oder geprüft worden
  sind; Reproduktion, Erwartung/Ist und nötigen Nachtest erhalten. Kein Berichtsbackup
  oder automatisches Archiv erforderlich; Git enthält die Entwicklungsgeschichte.
- `docs/planung/` enthält nur offene vereinbarte Arbeiten. Bei Abschluss aktuelle
  Fachdokumentation pflegen und erledigten Plan samt TODO-/Dokumentationsverweisen
  entfernen, außer der Benutzer wünscht ausdrücklich etwas anderes. Bei Teilumsetzung
  offene Schritte erhalten; danach leere Planordner entfernen. Im Entwicklungsstadium
  kein automatisches Planarchiv anlegen.

## 4. Architektur und technische Grundlage

- Java 25, Spring Boot und Vaadin mit Vaadin Flow verwenden. Maßgeblich für die
  konkreten Bibliotheksversionen ist `pom.xml`; keine zweite Versionsliste pflegen.
- Oberflächen mit den vorhandenen Vaadin-Java-Komponenten aufbauen; keinen Wechsel
  zu HTML-Templates, React oder einem anderen Frontend vornehmen.
- Bestehende Paketaufteilung, Konstruktorinjektion, Lombok- und Namenskonventionen
  beachten. Anwendungsquellen: `app/src/main/java/de/derpeterson/app/`.
  Paketübersicht und Zuständigkeiten: [Entwicklung](docs/entwicklung.md).
- Fachlogik und Berechtigungsprüfungen gehören in die zuständigen Dienste; sichtbare
  oder ausgeblendete UI-Aktionen ersetzen keine fachliche Zugriffskontrolle.
- Shared bleibt ein unabhängiges Maven-Modul (`shared/pom.xml`) für gemeinsame Texte,
  Konstanten und Regeln ohne Spring-/Vaadin-/Anwendungsabhängigkeit. Anwendung und
  Browsertests verwenden sein JAR; Shared-Quellen nicht als weiteren Quellordner einhängen.
- Änderungen auf den beauftragten Umfang begrenzen. Keine ungefragten Architekturwechsel,
  Refactorings oder Abhängigkeits-/Versionsänderungen.

## 5. Lesbarkeit und Codestil

- Vor Codearbeiten den Abschnitt [Codestil samt Beispielen](docs/entwicklung.md#codestil)
  lesen und bei Umsetzung sowie Review anwenden. Die Beispiele zeigen Schreibprinzipien,
  keine neuen Fachverträge; sie passend zur Aufgabe verwenden, nicht blind kopieren.
- Für Menschen schreiben: verständliche Namen, klar abgegrenzte Methodenaufgaben,
  wenig Verschachtelung und keine bedeutungslosen Hilfsmethoden oder starren Zeilenlimits.
- Kontrollblöcke immer mit geschweiften Klammern; `try`/`catch`/`finally` übersichtlich
  gliedern. Explizite lokale Typen sowie normale/statische Einzelimporte sind Standard.
  `var` nur mit konkretem Lesbarkeitsvorteil; bei Service-/Factory-Rückgaben Typ ausschreiben.
- Kommentare/Javadoc erklären nötige Verträge, Gründe und Besonderheiten. Regex direkt
  auf Deutsch erläutern; vorhandene zentrale Validierungsregeln wiederverwenden.
- Bestehende Transaktions-, Sperr- und Fehlergrenzen beim Gliedern erhalten.
- Java: Google Java Format mit zwei Leerzeichen. Nach jeder eigenen Java-Änderungsrunde
  vor Tests und Abschluss verbindlich `spotless:apply`, `spotless:check` und
  `checkstyle:check` über den Maven Wrapper mit `-N` im Projektroot ausführen;
  die Root-Konfiguration erfasst alle Module einmal. Nach Korrekturen wiederholen.
- Python: Ruff aus der Projektumgebung. Nach jeder eigenen Python-Änderungsrunde vor
  Tests und Abschluss `ruff format` und `ruff format --check` für die eigenen Dateien
  ausführen; nach Korrekturen wiederholen. Befehle und Grenzen stehen in der Anleitung.
- Fremde Änderungen erhalten. Formatierung auf eigene Dateien begrenzen, wenn fremde
  Änderungen vorliegen; kein Abschalten des Spotless-Ratchets und keine ungefragte
  projektweite Umformatierung.

## 6. Übersetzungen und Ressourcen

- Bei Arbeiten an Übersetzungen oder Ressourcen den Abschnitt
  [Übersetzungen und Ressourcen](docs/entwicklung.md#übersetzungen-und-ressourcen)
  lesen und anwenden.
- Den gemeinsamen typisierten Textkatalog verwenden; keine parallelen Schlüssel-/
  Getterkataloge anlegen. Sprachwechsel auch bei dynamischen UI-Meldungen berücksichtigen.
- Gemeinsame typisierte Konfigurations-/Statusbezeichner verwenden; übersetzte Texte
  nicht als fachliche Identität benutzen.
- Serverseitige Ressourcen über Classpath-Streams laden. Sie müssen auch im gepackten
  JAR funktionieren; keine Abhängigkeit von lokalen `src/`-Dateipfaden einführen.
- Generierte Frontenddateien, `vite.generated.ts` und Buildausgaben nicht manuell
  bearbeiten. Eigene Vite-Anpassungen gehören in `vite.config.ts`.

## 7. Logging

- Bei Loggingarbeiten den [Logging-Vertrag](docs/logging.md) einschließlich der
  Beispiele lesen und anwenden.
- Eigene Logmeldungen einheitlich auf Englisch schreiben; Dokumentation und
  Kommunikation bleiben Deutsch. Logs nicht über den UI-Textkatalog übersetzen.
- Normalbetrieb INFO, DEBUG gezielt für hilfreiche Diagnose. Nur wichtige fachliche
  Ereignisse, behandelbare Störungen und Fehler protokollieren; keine Methoden-,
  Zweig-, Polling- oder Aktivitätsflut und keine mehrfachen Logs desselben Fehlers.
- Nur freigegebene Metadaten verwenden, keine sensiblen Inhalte. Fehler über
  `LogSafety.safeFailure` protokollieren; die Originalexception unverändert behandeln.
- Transaktionalen Erfolg erst nach Commit über `AfterCommitLog` melden. Queueaufnahme,
  SMTP-Annahme und bestätigtes Ergebnis unterscheiden; unklaren Ausgang nicht als
  Erfolg oder sicheren Fehlschlag ausgeben. Security-Logger gemäß Vertrag auf INFO lassen.

## 8. Build und Fehlerbehandlung

- Bei Buildarbeiten die [Entwicklungsanleitung](docs/entwicklung.md) beachten.
  Unter Windows PowerShell und den Maven Wrapper aus dem Projektroot verwenden
  (`.\mvnw.cmd`, unter Linux/macOS `./mvnw`), kein globales Maven.
- Vor Builds mit `.\mvnw.cmd --version` prüfen, dass tatsächlich JDK 25 verwendet wird.
  Bei Abweichungen nicht weiterbauen oder stillschweigend Systemsoftware ändern.
- Anwendungsbuilds über den Reactor mit `-pl app -am` oder als vollständigen
  Root-Build ausführen; Shared wird vor seinen Verbrauchern gebaut. Direkte
  Modulaufrufe ohne Reactor benötigen zuvor installierte Abhängigkeiten.
- Maven immer mit ausdrücklichem Ziel aufrufen. Root und Anwendungsmodul haben
  keinen automatischen Anwendungsstart als Defaultziel.
- Bei Fehlern den eigenen Diff und die erste relevante Ursache untersuchen, statt
  nur Folgefehler zu behandeln. Externe Blocker klar benennen.
- Keine Funktionen entfernen und Java nicht herunterstufen, um Fehler zu umgehen.
- Bei API-Änderungen die offizielle Dokumentation der verwendeten Version prüfen.
  Annahmen und ungeprüftes Verhalten ausdrücklich als solche kennzeichnen.

## 9. Tests und Ausführungsfreigaben

- Vor Testarbeiten die [Testanleitung](docs/tests.md) lesen und anwenden; beim Schreiben,
  Ändern oder Ausführen von Browserfällen zusätzlich die [Browser-Testanleitung](browser-tests/README.md).
- Bei Verhaltensänderungen sinnvolle gezielte Regressionen ergänzen. Bestehende Tests
  wiederverwenden; keine doppelten Implementierungen oder abgeschwächten Assertions.
- Gezielte JUnit-/Helpertests und Stil-/Ressourcenprüfungen gehören zum passenden
  Änderungsauftrag. Browserläufe benötigen eine ausdrückliche Freigabe pro Auftrag;
  frühere Freigaben gelten nicht pauschal für spätere Aufgaben.
- Der ausdrückliche Auftrag „große Testrunde“ umfasst Browser DE/EN und die nötigen
  Testergänzungen gemäß dem Skill; dafür nicht erneut fragen. Automatische Skillwahl,
  Skill-Erstellung oder Trockenprüfung allein erlaubt keinen Browserlauf.
- Nur eigene isolierte Testdatenbanken und Testdienste verwenden; keine bestehenden
  Anwendungsdaten oder fremden Prozesse als Testfixture benutzen.
- Browser DE und EN am selben unveränderten aktuellen Image prüfen. Die bestehende
  klassenweise Worker-Parallelisierung verwenden; jeder Worker hat seine eigene Umgebung,
  innerhalb eines Workers bleibt JUnit sequenziell. Keine Parallelität mit gemeinsamen
  Fixtures und keine konkurrierenden Builds oder Imagewechsel während eines Laufs.
- Keine Anwendung starten, vorhandene Daten verändern oder echten Mailversand auslösen,
  sofern dies nicht vom Auftrag umfasst ist. Maven-Profil `it` startet/stoppt die
  Anwendung und benötigt eine entsprechende Freigabe.
- Offenen automatisierten Testbedarf gemäß Punkt 3 zentral erhalten. Menschliche
  Sonderprüfungen bleiben im manuellen Katalog; Einzelheiten stehen in der Testanleitung.
- Testergebnisse nur für tatsächlich ausgeführte Prüfungen melden. Browserbelege sind
  temporäre Diagnosedaten, kein Dokumentationsarchiv; bestehende Runs nur auf Auftrag löschen.
- Vor Abschluss `git diff --check` ausführen und den Änderungsumfang prüfen.
  Durch Builds geänderte versionierte generierte Dateien prüfen und benennen;
  nur bei Aufgabenbezug übernehmen, nicht pauschal verwerfen.

## 10. Git und fremde Änderungen

- Vor Änderungen Branch und Arbeitsstand mit `git status --short --branch` prüfen.
  Vorhandene Änderungen erhalten; fremde Arbeit nicht überschreiben oder verwerfen.
- Commit, Merge und Push benötigen einen entsprechenden Benutzerauftrag. Ein
  ausdrücklich beauftragter Skill kann diese Freigabe für seine definierten Schritte
  bereits enthalten; dafür nicht erneut fragen. Automatische Skillwahl allein oder
  die Bitte um eine Commit-Message erteilt keine solche Freigabe.
- Den Branch-, Commit- und Abschlussablauf des beauftragten Skills oder Befehls
  beachten. Keine zusätzlichen Git-Aktionen außerhalb seines Umfangs erfinden.
- Aufräumen und Löschen nur, soweit der Benutzerauftrag oder der ausdrücklich
  beauftragte Skill dies umfasst. Keine eigenmächtigen destruktiven Aktionen wie
  `git reset --hard`, `git checkout --` oder `git clean`; fremde Änderungen erhalten.
- Bei beauftragten Commits die abgeschlossene Änderung auf Deutsch in Vergangenheitsform
  beschreiben, keine erst geplanten Arbeiten.

## 11. Ergebnisberichte

- Ergebnis und wesentliche Änderungen kurz und ehrlich auf Deutsch berichten.
- Prüfungen nur mit tatsächlich ausgeführten und beobachteten Ergebnissen als bestanden
  melden; relevante Testzahlen nennen. Fehler, offene Arbeiten und nicht ausgeführte
  Prüfungen sichtbar machen. Ohne Ausführung ausdrücklich darauf hinweisen.
- Commit-Hashes nur aus tatsächlicher Git-Ausgabe übernehmen. Laufzeiten nur bei
  gemessenem Beginn und Ende nennen; keine Angaben erfinden oder ungeprüft behaupten.

## 12. Design und gemeinsame Theme-Bausteine

- Vor UI-/Designarbeiten die [Theme-Anleitung](docs/theme.md) lesen und anwenden.
  Dort stehen die bestehenden Bausteine, festen Gestaltungswerte und CSS-Zuständigkeiten.
- Neue Oberflächen auf vorhandenen Vaadin-/Cosmos-Komponenten, Klassen und Tokens
  aufbauen. Eingaben, Panels, Tabellen, Buttons und Dialoge übernehmen den bestehenden
  Standard; keine eigene parallele Gestaltung für bereits vorhandene Bausteine erfinden.
- Abgestimmtes gemeinsames Design nur mit Zustimmung des Benutzers ändern. Eine lokale
  Anpassung erlaubt keine globalen Änderungen oder unbeabsichtigte Änderungen an Icons,
  Größen und anderen Ansichten.
- Neue oder abweichende Gestaltung zunächst in der zuständigen lokalen Style-CSS
  eingrenzen. Erst nach ausdrücklicher Freigabe in gemeinsame Theme-Regeln/Tokens
  übernehmen, Provisorien bereinigen und die Theme-Dokumentation im selben Stand ergänzen.
  Vorhandene gemeinsame CSS-Quellen bilden das Template; Details stehen in der Anleitung.
- Die Theme-Anleitung aktuell halten: freigegebene neue Bausteine mit Einsatz,
  Klassen/Tokens und zuständiger Datei beschreiben, damit spätere UIs sie wiederverwenden.
  Feste Pixel-/Farbwerte nur dort und in ihren CSS-Quellen pflegen.
- Nach UI-/Designänderungen erklären, ob Browser-Refresh, Build und/oder Neustart nötig
  sind. Keine sichtbare Übernahme behaupten, solange nur Quellen geändert wurden.

## Projekt-Skills

Die konkreten Arbeitsabläufe liegen unter `.agents/skills/`.
Bedienung und Umfang: [Arbeiten mit Codex](docs/codex-skills.md).
