# Nachtauftrag für Release 0.2.0

Der Vaadin-Komponentenumbau ist abgeschlossen. Ausgangspunkt ist der vorbereitete
Release-Kandidat `04f84c36` auf `codex/vaadin-komponenten-folgearbeiten-6`.
Die Ausführung beginnt mit dem Auftrag für die Nacht. Ziel ist ein lokal
getaggter Release auf `main` mit belegter Prüfung derselben Revision.

## Zwischencommits

Jeden abgeschlossenen Arbeitsschritt mit Änderungen kurz reviewen und lokal
separat committen. Die deutsche Commit-Message beschreibt, was sich konkret
geändert hat. Das gilt auch für Anpassungen an Browsertests und Korrekturen aus
dem Sonar-Scan. Reine Prüfläufe liefern Belege zur geprüften Revision. Ihre
Ergebnisse gehören in den Prüfstand. Bei erfolgreichem Release folgt der
Dokumentationscommit nach dem Tag.

## 1. Testbedarf abarbeiten

- Den aktuellen Quellstand und die letzte erfolgreiche QA prüfen. Die offenen
  Einträge in [Testbedarf](../testbedarf.md) vollständig mit vorhandenen Tests
  und Ergebnissen abgleichen. Den geänderten Code zusätzlich auf fehlende
  Regressionstests prüfen.
- Die verbliebenen Fehlschläge aus dem [Prüfstand](../status.md#offene-nachweise)
  gezielt nachstellen. Bei der Textarea mit `MAIL-02`, `MAIL-03` und `NOTIFY-01`
  anfangen und die dort bislang blockierten Folgeschritte mitprüfen.
- Fehlende Abdeckung ergänzen, die Ursachen von Anwendungs- und Testfehlern
  beheben und gezielt nachtesten. Punkte erst streichen, wenn die passende
  Abdeckung tatsächlich bestanden hat. Bei Browserfällen gilt das für DE und EN.
  Wartende Einträge mit Grund erhalten. Die Buttonlayouts sind manuell abgenommen.
- Korrekturen und Testergänzungen in fachlich zusammenhängenden Zwischencommits
  auf dem Release-Branch festhalten. Für die abschließende Prüfung einen
  sauberen Kandidatenstand herstellen.

## 2. Kandidaten auf `main` prüfen

- Den Kandidaten lokal per Fast-forward auf `main` übernehmen. So bleibt der
  zu prüfende Commit identisch. Danach auf sauberem `main` den geschützten
  [Sonar-Scan](../entwicklung.md#vollständiger-scan) ausführen und das Quality Gate
  der konkreten Revision abwarten. Der Scan deckt den Reactor-Build samt JUnit,
  Browser-Helfertests und gemeinsamer Java-Coverage ab. Die Frontend- und
  Offline-Prüfungen gemäß [Testanleitung](../tests.md) ergänzen.
- Falls der Sonar-Scan mehrere Korrekturen erfordert, zusammengehörige Befunde
  zu kleinen fachlichen Arbeitspaketen bündeln. Jedes Paket gezielt prüfen und
  einzeln committen. Nach den Korrekturen den sauberen Kandidaten erneut scannen.
- Erst danach die vollständige Browsersuite in DE und EN auf demselben
  unveränderten Anwendungsimage ausführen. Die vorhandene isolierte
  [Browser-Testumgebung](../../browser-tests/README.md) verwenden. Korrekturen
  aus den Browserläufen nach dem gezielten Nachtest separat committen.
- Wenn eine Korrektur den Kandidaten verändert, prüfen, welche Nachweise neu
  erstellt werden müssen. Das Sonar Quality Gate muss für die finale Revision
  `OK` melden. Beide erfolgreichen Browser-Gesamtläufe und alle übrigen
  erforderlichen Prüfungen müssen ebenfalls zum finalen Kandidaten passen.

## 3. Release erstellen und dokumentieren

- Das Kandidaten-JAR nach [Release-Anleitung](../releases.md#qa-freigabe)
  verifizieren. Die tatsächlichen Prüfergebnisse, Kandidatenrevision,
  JAR-Prüfsumme und Browser-Image-ID als Releasequalifikation festhalten.
- Bei vollständigem Nachweis den annotierten Tag `v0.2.0` auf dem geprüften
  Commit erstellen, JAR und Manifest lokal ablegen und `main` separat auf
  `0.2.1-SNAPSHOT` weiterstellen. Bleibt eine erforderliche Prüfung offen oder
  fehlerhaft, bleibt der Release ungetaggt.
- Den [Prüfstand](../status.md) und [Testbedarf](../testbedarf.md) mit den
  belegten Ergebnissen aktualisieren und offene Blocker festhalten. Beim
  erfolgreichen Release die Abschlussdokumentation nach dem Tag committen,
  damit der geprüfte Kandidatencommit unverändert bleibt. Den erledigten
  Releaseplan dabei entfernen.

## 4. Neuigkeiten-Branch auf den neuen `main` bringen

- Nach erfolgreichem Release den Branch `feature/neuigkeiten` in seinem
  separaten Worktree auf den dokumentierten `main` mit `0.2.1-SNAPSHOT` bringen.
  Damit übernimmt er die Korrekturen aus den Browserprüfungen und dem Sonar-Scan.
- Bei sauberem Arbeitsstand `git rebase main` im Neuigkeiten-Worktree ausführen.
  Git setzt eigene Feature-Commits auf den neuen Stand. Ohne eigene Commits
  rückt der Branch direkt auf `main` vor. Uncommittierte Änderungen erhalten
  und bei ungelösten Konflikten den Blocker benennen.
- Anschließend prüfen, dass `main` in der Historie des Neuigkeiten-Branch liegt,
  die Projektversion stimmt und die geplante Neuigkeiten-Umsetzung samt eigenen
  Änderungen erhalten ist.
