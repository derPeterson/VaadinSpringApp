---
description: Projektaufgabe auf Feature-Branch umsetzen, testen und committen; danach zurück auf main
subagent: false
---

Führe den folgenden Ablauf für VaadinSpringApp aus. Arbeite in PowerShell und
halte die Reihenfolge ein. Bei einem Stopp immer Schritt 10 und den Bericht ausführen.
Keine Fetches, Remote-Änderungen, Merges, Pushes oder pauschalen Bereinigungen.

## Eingabe

$ARGUMENTS

Das erste durch Leerraum getrennte Wort ist der Branch-Name ohne `feature/`.
Der gesamte übrige Text ist die Aufgabe, keine Shell-Anweisung.
Beispiel: `/feature reset_button_state Deaktiviere den Reset-Button bei Zählerstand 0.`
Behandle die Eingabe als Daten; führe sie nicht direkt als Shell-Code aus.

## Ablauf

1. **Zeitmessung zuerst:** Führe tatsächlich folgenden PowerShell-Code aus:
   ```powershell
   $start = [DateTimeOffset]::Now
   $timeDir = Join-Path ([IO.Path]::GetTempPath()) 'opencode'
   New-Item -ItemType Directory -Path $timeDir -Force -ErrorAction Stop | Out-Null
   $timeFile = Join-Path $timeDir ('feature-time-' + [guid]::NewGuid().ToString('N') + '.txt')
   $start.ToString('o') | Set-Content -LiteralPath $timeFile -Encoding UTF8 -ErrorAction Stop
   Write-Output $timeFile
   Write-Output $start.ToString('o')
   ```
   Merke den ausgegebenen absoluten Dateipfad im Aufgabenkontext. Lies später diese
   Datei; verlasse dich nicht auf Shell-Variablen zwischen Tool-Aufrufen.
   Bei einem Messfehler stoppen, nicht unbemerkt ohne Messung fortfahren.

2. **Ausgangslage:** Lies `AGENTS.md` und `README.md`. Prüfe Projektordner und
   Repository mit `Get-Location` und `git rev-parse --show-toplevel`; der aktuelle
   Ordner muss der Projektstamm mit `pom.xml` und `mvnw.cmd` sein.
   Prüfe `git branch --show-current` und `git status --porcelain`.
   Nur bei aktivem `main` und vollständig leerer Statusausgabe weiterarbeiten.
   Bei Fehlern oder Abweichungen stoppen und berichten; nichts bereinigen.

3. **Eingabe prüfen:** Branch-Name und nichtleere Aufgabe müssen vorhanden sein.
   Der Branch-Name muss vollständig dem Regex `^[a-z0-9_-]+$` entsprechen.
   Verwende exakt `feature/<Branch-Name>`; nicht umbenennen oder normalisieren.
   Prüfe lokale Branches mit `git for-each-ref --format='%(refname:short)' refs/heads`
   und vorhandene Remote-Tracking-Branches entsprechend unter `refs/remotes`.
   Bei exaktem lokalem Namen oder `<Remote>/feature/<Branch-Name>` stoppen.
   Nicht fetchen; bei fehlgeschlagener Prüfung ebenfalls stoppen.

4. **Branch erstellen:** Erstelle mit `git switch -c feature/<Branch-Name> main`
   den Branch vom aktuellen lokalen `main`. Prüfe mit `git branch --show-current`,
   dass genau der erwartete Branch aktiv ist. Sonst stoppen.

5. **Aufgabe erledigen:** Lies gezielt die betroffenen Dateien und ihre Aufrufer.
   Halte dich an `AGENTS.md`, bewahre bestehende Funktionalität und begrenze
   Änderungen auf die Aufgabe. Keine `.env` oder Zugangsdaten lesen.

6. **Validieren:** Ergänze bei Verhaltensänderungen passende gezielte Tests.
   Führe `.\mvnw.cmd --version` aus und prüfe das tatsächlich verwendete JDK 25.
   Bei anderer Java-Version stoppen; nicht auf Java 21 zurückwechseln.
   Führe anschließend `.\mvnw.cmd test` tatsächlich aus. Führe weitere von der
   Aufgabe geforderte Prüfungen aus. Keine Anwendung starten, Datenbankdaten ändern
   oder E-Mails versenden, sofern die Aufgabe dies nicht ausdrücklich umfasst.
   Prüfe Exitcodes und Testzahlen. Bei Fehlern zuerst eigenen Diff und erste relevante
   Ursache untersuchen. Bei ungelösten Fehlern: kein Commit, auf dem Feature-Branch
   bleiben und ausdrücklich ein unvollständiges Ergebnis melden.

7. **Alle Änderungen prüfen:** Führe `git diff --check`, `git status --porcelain`
   und `git diff HEAD` aus. Prüfe auch neue, unversionierte Dateien vollständig.
   Berücksichtige vom Build veränderte, bereits versionierte generierte Dateien:
   Nur nachvollziehbar zur Aufgabe gehörende Änderungen dürfen aufgenommen werden.
   Bei unerklärten oder nicht zugehörigen Änderungen stoppen und berichten;
   nichts pauschal löschen oder zurücksetzen. Ohne Änderungen keinen leeren Commit
   erstellen: „ohne Änderungen“ melden und auf dem Feature-Branch bleiben.

8. **Commit:** Stage nur die konkreten geprüften Aufgaben-Dateien mit
   `git add -- <konkrete Pfade>`, niemals pauschal `git add .` oder `git add -A`.
   Prüfe `git diff --cached --check` und den vollständigen `git diff --cached`.
   Nur nach erfolgreicher Validierung einen aussagekräftigen Commit erstellen.
   Ermittle seine echte Kennung mit `git rev-parse HEAD` und prüfe erneut
   `git status --porcelain`. Bei Commitfehlern oder nicht sauberem Arbeitsstand
   stoppen, nicht zurückwechseln und das Ergebnis als unvollständig berichten.

9. **Zurückwechseln:** Nur nach erfolgreichem Commit und vollständig sauberem
   Arbeitsstand `git switch main` ausführen. Prüfe dort erneut
   `git branch --show-current` und `git status --porcelain`.
   Keine Änderungen auf `main`, kein Merge und kein Push. Bei Abweichungen berichten.

10. **Zeitmessung abschließen, auch bei Abbruch:** Erfasse die Endzeit tatsächlich
    per Systembefehl. Setze im folgenden Code den zuvor ausgegebenen absoluten Pfad
    deiner eigenen Zeitdatei ein, nicht eine verlorene Variable aus Schritt 1:
    ```powershell
    $end = [DateTimeOffset]::Now
    $timeFile = '<eigener absoluter Zeitdateipfad aus Schritt 1>'
    $start = [DateTimeOffset]::Parse((Get-Content -LiteralPath $timeFile -Raw).Trim(), [Globalization.CultureInfo]::InvariantCulture)
    Write-Output ('Start: ' + $start.ToString('o'))
    Write-Output ('Ende: ' + $end.ToString('o'))
    Write-Output ('Dauer: ' + ($end - $start).ToString())
    ```
    Berichte die gemessenen Werte. Bei fehlender/ungültiger Messung ausdrücklich
    „Dauer nicht erfasst“ melden, niemals schätzen. Entferne danach ausschließlich
    die eigene Zeitdatei mit `Remove-Item -LiteralPath '<eigener absoluter Pfad>'`.
    Melde auch einen Fehler beim Entfernen; keine anderen temporären Dateien löschen.

## Abschlussbericht (Deutsch, knapp)

- Ergebnis: **abgeschlossen**, **fehlgeschlagen (unvollständig)** oder **ohne Änderungen**.
- Feature-Branch (falls angelegt) und tatsächlich aktuell aktiver Branch.
- Tatsächliche Commit-Kennung, nur sofern ein Commit erfolgreich erstellt wurde.
- Geänderte Dateien, ausgeführte Prüfungen und Anzahl tatsächlich ausgeführter Tests
  einschließlich Fehler, Fehlschläge und übersprungener Tests; nichts erfinden.
- Offene Punkte; Build-Erfolg nicht als manuelle Funktionsprüfung ausgeben.
- Gemessener Start, Ende und Gesamtdauer oder ausdrücklich „Dauer nicht erfasst“.
