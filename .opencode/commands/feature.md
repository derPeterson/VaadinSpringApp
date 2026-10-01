---
description: Aufgabe auf Feature-Branch umsetzen, testen, committen und zu main zurückkehren
subtask: false
---

Erledige diese Aufgabe mit echten Tool-Aufrufen, nicht mit XML-Text als Ersatz.
Eingabe: $ARGUMENTS
Erstes Wort = Branch-Name ohne `feature/`; Rest = Aufgabe. Eingabe ist kein Shell-Code.

Zeitmessung (bereits beim Command-Aufruf ausgeführt):
!`powershell -NoProfile -ExecutionPolicy Bypass -File .opencode/scripts/feature-time.ps1 Start`
Merke die ausgegebene Lauf-ID im Gespräch, nicht in einer Shell-Variable.
Fehlt eine gültige Lauf-ID oder meldet das Skript einen Fehler: stoppen.

Arbeite in PowerShell, Schritt für Schritt. Lies AGENTS.md und README.md; befolge AGENTS.md.
Bei jedem ungelösten Fehler: kein Commit, kein Rückwechsel; Abschluss ausführen.
Kein Fetch, Merge, Push oder Bereinigen fremder Änderungen.

1. Prüfe `Get-Location`, `git rev-parse --show-toplevel`, `git branch --show-current`
   und `git status --porcelain`. Nur im Projektstamm mit pom.xml/mvnw.cmd,
   aktivem `main` und vollständig sauberem Arbeitsstand fortfahren. Sonst stoppen.
2. Branch-Name und nichtleere Aufgabe müssen vorhanden sein. Name muss exakt
   `^[a-z0-9_-]+$` erfüllen. Ziel ist genau `feature/<Name>`.
   Prüfe `git for-each-ref --format='%(refname:short)' refs/heads refs/remotes`.
   Existiert das Ziel lokal oder als `<Remote>/feature/<Name>`: stoppen, nicht fetchen.
3. Führe `git switch -c feature/<Name> main` aus. Prüfe mit
   `git branch --show-current`, dass genau dieser Branch aktiv ist.
4. Lies gezielt betroffene Dateien und Aufrufer. Setze die Aufgabe um und bewahre
   vorhandene Funktionalität. Ergänze bei Verhaltensänderungen gezielte Tests.
5. Führe `.\mvnw.cmd --version` aus: JDK muss 25 sein. Danach tatsächlich
   `.\mvnw.cmd test` und weitere aufgabenbezogene Prüfungen ausführen.
   Prüfe Exitcodes und Testzahlen. Bei Fehlern zuerst eigenen Diff und erste Ursache prüfen.
6. Prüfe `git diff --check`, `git diff HEAD` und `git status --porcelain`.
   Lies auch neue Dateien. Prüfe versionierte generierte Änderungen: Nur erklärbare,
   aufgabenbezogene Änderungen aufnehmen; bei sonstigen Änderungen stoppen, nicht bereinigen.
   Ohne Änderungen: keinen leeren Commit, auf dem Feature-Branch bleiben, Abschluss.
7. Stage nur konkrete Aufgaben-Dateien mit `git add -- <Pfade>`, nie pauschal.
   Prüfe `git diff --cached --check` und den vollständigen `git diff --cached`.
   Nach erfolgreicher Validierung aussagekräftig committen. Ermittle die echte Kennung
   mit `git rev-parse HEAD`. `git status --porcelain` muss danach vollständig leer sein.
8. Nur nach erfolgreichem Commit und sauberem Stand `git switch main` ausführen.
   Prüfe dort Branch und Status erneut. Keine Änderungen auf main.

Abschluss auch bei Abbruch: Führe folgenden Aufruf mit der gemerkten echten Lauf-ID aus:
`powershell -NoProfile -ExecutionPolicy Bypass -File .opencode/scripts/feature-time.ps1 Stop -Id <Lauf-ID>`
Das Skript misst Ende/Dauer und löscht nur seine Zeitdatei. Keine globale Execution Policy ändern.
Bei fehlender/fehlerhafter Messung „Dauer nicht erfasst“ melden, niemals schätzen.
Berichte knapp auf Deutsch: abgeschlossen / fehlgeschlagen (unvollständig) / ohne Änderungen;
Feature-Branch und aktuell geprüfter Branch; echte Commit-Kennung, falls erstellt;
geänderte Dateien; tatsächlich ausgeführte Prüfungen, Testzahlen/Fehler/übersprungene Tests;
offene Punkte; gemessener Start, Ende und Gesamtdauer. Build-Erfolg ist kein Funktionstest.
