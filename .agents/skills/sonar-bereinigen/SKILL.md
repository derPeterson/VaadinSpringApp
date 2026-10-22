---
name: sonar-bereinigen
description: "SonarQube für VaadinSpringApp analysieren und planen, ein benanntes Finding-Paket umsetzen oder komplett bereinigen. Aktuelle Scans, fachliche Triage, Paket-Branches, Regressionen und Nachscans verbinden. Für ausdrückliche Sonar-Aufträge; Skill-Erstellung und Trockenprüfung starten keine Bereinigung."
---

# Sonar Bereinigen

Lies im tatsächlichen Repository `README.md`, `AGENTS.md`, relevante Test-/
Sicherheitsdokumentation und `docs/testbedarf.md`. OpenCode bleibt unverändert.
Die allgemeinen Projektregeln gelten; eigene Änderungen von fremden unterscheiden.

## Drei Aufträge, unterschiedliche Befugnisse

| Auftrag | Umfang und Abschluss |
| --- | --- |
| Sonar analysieren und planen | Aktuellen Scan durchführen, Serverabschluss abwarten, Findings fachlich bewerten und befristeten Paketplan erstellen; danach anhalten. Keine Codeänderung, Branchanlage, Commits, Merges oder Issue-Statusänderung. |
| Sonar-Paket X umsetzen | Genau das benannte Paket aus dem aktuellen Plan prüfen und umsetzen; eigener Branch, gezielte Tests, eigener lokaler Commit und Merge nach main sowie bestätigender Nachscan. Danach anhalten, kein nächstes Paket automatisch. |
| Sonar komplett bereinigen | Aktuellen Stand analysieren und planen, geeignete Pakete nacheinander umsetzen, lokal integrieren und nachscannen; kleine Folgefehler beseitigen. Ziel sind möglichst null berechtigte offene Issues. |

Die ausdrückliche Paket-/Komplettbeauftragung umfasst die beschriebenen eigenen
lokalen Commits/Merges und begründeten Issue-Entscheidungen. Keine Pushes.
Fehlt der Modus und ist er nicht aus dem bestehenden Auftrag ersichtlich, vor
verändernden Aktionen gezielt klären. Ein ausdrücklicher Nur-Analyse-/Trockenauftrag
hat Vorrang. Das Anlegen oder Prüfen dieses Skills ist kein Sonar-Auftrag.

## 1. Aktuelle, zuordenbare Analyse beschaffen

- Repository, Branch, Quellstand einschließlich uncommittierter Änderungen sowie
  laufende eigene Builds/Scans prüfen. Keine doppelten Analysen oder fremden Eingriffe.
- Verfügbare konfigurierte MCP-/Scanner-Werkzeuge und Berechtigungen prüfen.
  Fehlt die Verbindung, Authentifizierung oder Scanmöglichkeit, den betroffenen
  Schritt als blockiert melden. Keine Tokens aus Dateien/Chat beschaffen, ausgeben
  oder unbesehen credentialhaltige Hilfsskripte ausführen. Keine Integration installieren.
- Der Hauptprojekt-Scan muss zum tatsächlichen main-Stand gehören. Community Build
  bietet dort keine getrennte Branchanalyse: Paket-Branches nicht als main ausgeben.
  Der Scan selbst braucht keinen Git-Push. Edition und Möglichkeiten bei Bedarf prüfen.
- Den geschützten Einstieg `scripts/sonar/sonar.py scan` verwenden: JDK 25,
  zentraler Stil mit `-N`, vollständiger optimierter Reactor-Install mit Profilen
  `production,coverage`, wirksames Maven-Modell und gemeinsamer Coverage-Prüfer vor
  einem einzigen Upload unter `vaadin-spring-app`. Alle drei Java-Module gehören zur
  Analyse; App/Shared sind Anwendungscode, Browserquellen bleiben Testcode. Keine
  Einzeluploads pro Modul, Ersatzkeys oder manuellen Scope-/Coverage-Ausschlüsse.
  Fremde/alte Buildartefakte nicht als aktuell übernehmen. Quellstand, Reports und
  Modell müssen zusammenpassen. Reine Planung benötigt keine vollständige Browserrunde.
- Scannerbericht hochladen lassen und den konkreten Serverauftrag bis zum Abschluss
  verfolgen. Danach Analyse-ID, Zeitpunkt, Projekt, Quellstand und Quality Gate zuordnen.
  Scanner-Erfolg allein oder eine fremde neuere Analyse ist kein passender Nachweis.
- Alle offenen Issues des Gesamtcodes vollständig/paginiert abrufen, nicht nur New Code.
  IDs, Regeln, Schwere, Codestellen und relevante Flows erfassen; bestehende Ausnahmen
  nicht pauschal wieder öffnen. Hotspots und Gate-Ursachen gesondert ausweisen.
  Alte Exporte sind nur ausdrücklich gekennzeichnete Eingaben, kein frischer Scan.

## 2. Fachlich bewerten und Pakete bilden

- Findings im tatsächlichen Code samt Aufrufern, Sicherheits-/Transaktionsverträgen
  und Tests prüfen. Meldung und vorgeschlagene Lösung sind nicht automatisch richtig.
- Echte Sicherheitsprobleme, Bugs, Laufzeit- und Transaktionsrisiken priorisieren.
  Gleichartige sichere Kosmetik gesammelt mitnehmen, ohne dringende Fehler zu verdrängen.
- Zusammengehörige Hinweise zu fachlichen Paketen bündeln: Ziel, Finding-IDs/Anzahlen,
  Priorität, Änderung, Tests, Aufwand/Risiken, Abhängigkeiten und Entscheidungsbedarf.
  Keine künstlichen Pakete pro Einzelzeile und keine unnötige Architekturaufteilung.
- Plan unter `docs/planung/sonar-pakete.md` aktuell halten. Er enthält nur offene oder
  wartende Arbeiten; erledigte Schritte nach belegter Abnahme entfernen. Keinen zweiten
  aktiven Plan unbesehen überschreiben. Nur-Analyse endet hier mit kurzem Bericht.

## 3. Ausführen oder Entscheidungen zurückstellen

Für Paket-/Komplettmodus die [Ausführungsregeln](references/ausfuehrung.md) lesen.
Keine vollständige große Testrunde automatisch starten. Der Fokus ist Sonar-Bereinigung
mit passenden Regressionen, nicht das Hinzufügen neuer Fachfunktionen.

- Größere interne Refactorings innerhalb bestehender Verträge sind erlaubt. Neue
  Fähigkeiten oder grundlegende Verhaltens-/Architekturänderungen gezielt abstimmen.
- Bei benötigter Entscheidung asynchron fragen und unabhängige Pakete weiterbearbeiten.
  Bei ausbleibender Antwort nach etwa zehn Minuten den Teil mit Grund wartend belassen;
  weder aus Schweigen Zustimmung ableiten noch den gesamten Lauf unnötig blockieren.
  Ist nichts Unabhängiges mehr möglich, mit offenem Punkt abschließen statt lange warten.
- Sinnvolle JUnit-Regressionen ergänzen. Nicht bereits im Auftrag erfolgreich geprüften
  Browser-/anderen Testbedarf in `docs/testbedarf.md` zusammenführen: Funktion, Szenario,
  Erwartung, Testart und gegebenenfalls Wartegrund. Nicht nur im temporären Bericht nennen.
- Die vereinbarte Kontingent-/Fortsetzungsregel steht unter „Kontingent und Wiederaufnahme“
  im [QA-Skill](../grosse-testrunde/SKILL.md). Nur diesen Abschnitt dafür lesen: gelegentlich
  prüfen, bei Erschöpfung sichern/stoppen und nötigen Folgeauftrag nach Reset vorbereiten.
  Dadurch wird weder der QA-Skill ausgeführt noch dessen Testumfang zusätzlich beauftragt.

## 4. Kurzer Abschluss, keine Berichtssammlung

- Genau einen aktuellen realen Bericht unter `.codex/sonar/bericht.md` aktualisieren.
  Er ist lokal ignoriert, bleibt zum Nachlesen liegen und wird beim nächsten Sonar-
  Auftrag ersetzt. Datum/Modus stehen im Inhalt, keine Serie datierter Dateien anlegen.
- Vor dem Ersetzen alle offenen Entscheidungen/Umsetzungspakete im aktiven Plan und
  Testlücken in `docs/testbedarf.md` erhalten. Ein temporärer Bericht ist nie die einzige
  Ablage wichtiger offener Arbeit. Sonar-Ausnahmebegründungen stehen direkt am Finding.
- Kurz berichten: Ausgangs-/Endanalyse, Pakete erledigt/wartend, tatsächlich durch Code
  behobene IDs, Accepted, False positives, neue/wiedereröffnete/verbleibende Issues,
  echte Tests, Gate und offene Browserabnahme. Gesamt- und New-Code-Sicht unterscheiden.
- Behebungen nur durch passenden Nachscan bestätigen. Änderungen an Profil/Scope oder
  Status nicht als Codebehebung zählen; nichts für eine schöne Null unterdrücken.
- Bei vollständigem Abschluss erledigten Paketplan samt Verweisen entfernen, gültige
  Bedienung dokumentieren und nur die aktuelle Zusammenfassung behalten. Wartende
  Umsetzungspakete bleiben erhalten; Testlücken verbleiben unabhängig im Testbedarf.
- Eigene erledigte Folgeaufträge entfernen, Diff prüfen, auf Deutsch berichten und
  auf den tatsächlichen Bericht/Testbedarf verlinken. Fehlende Nachweise offen nennen.
- Vorschau-/Beispielberichte liegen getrennt im temporären Chat-Ausgabeordner unter
  festem Namen und sind fiktiv gekennzeichnet. Alte Beispiele beim nächsten Beispiel
  überschreiben; keine Datumsarchive und keine fiktiven Zahlen im echten Prüfstand.
