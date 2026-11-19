# Nachtauftrag für Release 0.2.0

Der Vaadin-Komponentenumbau ist abgeschlossen. Ausgangspunkt ist der vorbereitete
Release-Kandidat `04f84c36` auf `codex/vaadin-komponenten-folgearbeiten-6`.
Die Ausführung beginnt mit dem Auftrag für die Nacht. Ziel ist ein lokal
getaggter Release auf `main` mit belegter Prüfung derselben Revision.

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
- Die Korrekturen prüfen und den Stand auf dem Feature-Branch lokal committen.
  Für die abschließende Prüfung einen sauberen Kandidatenstand herstellen.

## 2. Kandidaten auf `main` prüfen

- Den Kandidaten lokal per Fast-forward auf `main` übernehmen. So bleibt der
  zu prüfende Commit identisch. Danach auf sauberem `main` den geschützten
  [Sonar-Scan](../entwicklung.md#vollständiger-scan) ausführen und das Quality Gate
  der konkreten Revision abwarten. Der Scan deckt den Reactor-Build samt JUnit,
  Browser-Helfertests und gemeinsamer Java-Coverage ab. Die Frontend- und
  Offline-Prüfungen gemäß [Testanleitung](../tests.md) ergänzen.
- Erst danach die vollständige Browsersuite in DE und EN auf demselben
  unveränderten Anwendungsimage ausführen. Die vorhandene isolierte
  [Browser-Testumgebung](../../browser-tests/README.md) verwenden.
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
  damit der geprüfte Kandidatencommit unverändert bleibt. Nach Abschluss
  diesen Plan entfernen.
