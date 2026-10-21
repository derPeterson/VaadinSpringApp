# Vergleichsstand und Unterbrechung

Dieser Vertrag beschreibt die Datenpflege durch den Agenten. Er ist kein eigener
Runner und prüft keine Anwendung. Technische Belege liefert der vorhandene Testaufbau.

## Ablage

- `docs/status.md`: ein aktueller menschenlesbarer Bericht, keine Berichtskette.
- `.codex/qa/state.json`: ein lokaler, ignorierter Arbeitszustand mit den zwei
  getrennten Bereichen `last_success` und `current`. Beim ersten tatsächlichen
  QA-Auftrag anlegen, nicht beim Installieren oder Trockenprüfen dieses Skills.
- Lokale Browserbelege bleiben in der vorhandenen Ablage. Konkrete alte Runpfade
  gehören nur in den lokalen Zwischenstand, nicht in die dauerhafte Fachdokumentation.

## Inhalt des Zustands

Für beide Bereiche den Repositorypfad, Branch, vollständigen Commit, Zeitpunkt und
geprüften Quellstand festhalten. Der Commit allein beschreibt keinen schmutzigen
Arbeitsbaum: geänderte/neue relevante Quell-, Test-, Ressourcen- und Builddateien
mit Status und SHA-256 des aktuellen Inhalts erfassen, gelöschte Pfade ausdrücklich
kennzeichnen. Für unveränderte versionierte Dateien gilt der Commitinhalt.
Renames und Änderungen an Tests gehören zum Vergleich. Dateinamen allein reichen nicht.

Keine Geheimnisse, Datenbanken, Logs, generierten Quellen, Buildausgaben oder
Browserartefakte lesen/haschen. Potenziell geheime lokale Konfigurationen auslassen;
relevante ungeprüfte Eingaben als Grenze nennen. Die Zustandsdatei selbst und der
Ergebnisbericht gehören nicht in ihren eigenen Quellfingerabdruck.

`current` enthält zusätzlich:

- freigegebenen Umfang und ausdrücklich abgebrochene/fortsetzbare Arbeit;
- aktuelle Phase und nächste konkrete Schritte;
- relevante Änderungen mit passendem vorhandenen/ergänzten Testfall;
- Prüfungen mit Zustand offen/läuft/bestanden/fehlgeschlagen/blockiert, Zählwerten,
  zugehörigem Quellstand/Image und Belegort;
- eigene laufende Prozesse und Docker-Projekte/Image-IDs zur Wiederaufnahme;
- Zeitpunkt/Ergebnis der letzten Kontingentprüfung, Reset und eigenen Folgeauftrag;
- Sonar-Projekt, Analysestand, Gate, offene größere Finding-Pakete und fehlende Nachweise.

Nur benötigte Metadaten speichern, keine freien Mail-/Formular-/Credentialinhalte.
Den Zustand nach größeren Phasen und vor einer Unterbrechung per temporärer Datei
und atomarem Austausch sichern. Bei kaputtem Zustand nicht still eine leere
erfolgreiche Baseline annehmen.

## Entscheidungen bei Start und Wiederaufnahme

| Situation | Vorgehen |
| --- | --- |
| Keine sichere Baseline | Im Bericht offenlegen; vorhandenen Prüfstand und Git vorsichtig als Einstieg nutzen, aktuellen ersten Stand aufbauen |
| Neue Commits seit erfolgreicher QA | Tatsächliche Diffs und bereits vorhandene Tests beurteilen; Testlücken schließen |
| Gleicher Commit, geänderte Arbeitsdatei | Als Änderung behandeln; erfolgreiche Belege für einen anderen Inhalt nicht übernehmen |
| Baseline-Commit nicht erreichbar oder anderer Projektpfad | Vergleich nicht erzwingen; Herkunft klären bzw. neue Baseline offen aufbauen |
| Unterbrochen, eigener Runner lebt noch | Prozess/Metadaten prüfen und weiter beobachten; keinen zweiten starten |
| Unterbrochen, erfolgreiche Phase unverändert | Beleg und Quell-/Imagebezug prüfen; Phase nicht unnötig erneut ausführen |
| Nach erfolgreichem DE-Lauf Anwendung geändert | Beide Sprachen am neuen finalen Image absichern; altes DE-Ergebnis nicht als aktuelles ausgeben |
| Benutzer hat gestoppt | Keine automatische Fortsetzung; eigenen Folgeauftrag entfernen bzw. aufheben |
| Tests vollständig erfolgreich, Sonar aktuell geprüft, größere Findings/Gatefehler offen | Test-Baseline darf ersetzt werden; Sonar-Befund und Folgepaket erhalten, Qualitätsstand ausdrücklich nicht insgesamt grün nennen |
| Vollständig abgeschlossen | `last_success` ersetzen, `current` leeren und eigenen Folgeauftrag entfernen |
| Nur teilweise geprüft oder Sonar-Nachweis offen | `current` erhalten; `last_success` unverändert lassen; Bericht als unvollständig kennzeichnen |

Die erfolgreiche Baseline belegt bestandene Tests und die ausgeführte Sonar-Prüfung,
nicht die Abwesenheit aller Sonar-Issues. Offene größere Findings werden im aktuellen
Bericht als separates Folgepaket dokumentiert; nach ihnen muss nicht automatisch
die Testrunde weiterlaufen. Fehlende Prüfungen bleiben dagegen unvollständige Nachweise.

## Verlässliche Zählung und Bericht

Testzahlen aus tatsächlichen Reports beziehen, nicht aus der vorherigen Dokumentation.
JUnit-Anwendung und JUnit-basierte Browser-/Helfertests getrennt halten, damit keine
doppelte Gesamtzahl entsteht. Bestandene, fehlgeschlagene, abgebrochene und übersprungene
Fälle unterscheiden. DE und EN getrennt aufführen. Bewusste manuelle Opt-in-Ausschlüsse
sind keine unerklärten Skips der regulären Suite. Überarbeitete Tests und Nachtestläufe
nicht als neue Testfälle mitzählen.

Sonar-Behebungen anhand gleicher Finding-IDs und passender Vorher-/Nachheranalysen
belegen. Keine Behebung durch Statuswechsel vortäuschen. Bei fehlender aktueller Analyse
„nicht bestätigt“ schreiben. Coverage nur verlinken/kurz übernehmen, wenn aktuell vorhanden;
keine zusätzliche Messrunde auslösen.
