---
name: branch-lokal-mergen
description: "Einen ausdrücklich gewünschten lokalen Branch-Merge in VaadinSpringApp mit Abschlussbereinigung und Build-/JUnit-/Startprüfung vorbereiten und durchführen. Bei Mergeaufträgen verwenden; Quelle und Ziel aus dem Auftrag bestimmen. Manuelle Benutzermerges, Beispiele und das Anlegen/Prüfen dieses Skills lösen keinen Merge aus."
---

# Branch Lokal Mergen

Lies `README.md`, `AGENTS.md`, relevante aktive Feature-Pläne und `docs/testbedarf.md`.
Ein ausdrücklicher Mergeauftrag ist Voraussetzung. Keine Hooks, Überwachung oder
automatischen Abläufe bei einem vom Benutzer selbst ausgeführten Merge einrichten.
Ein Trocken-/Nur-Prüfauftrag und die Erstellung dieses Skills verändern keine Branches.

## 1. Quelle, Ziel und sauberen Startstand bestimmen

- Quelle und Ziel aus Auftrag/Gespräch eindeutig bestimmen und lokal auflösen.
  Sie sind nicht zwingend aktueller Branch bzw. main. Nur wenn unklar gezielt fragen.
  Nicht eigenmächtig fetchen/pullen, neue Ersatzbranches erzeugen oder Ref-Namen raten.
- Relevante Checkouts/Worktrees und laufende eigene oder fremde Vorgänge prüfen.
  Bei bestehenden uncommittierten Änderungen im betroffenen Arbeitsstand sofort
  stoppen: gestagte, ungestagte und nicht ignorierte neue Dateien berücksichtigen.
  Auch Ziel-/Quellcheckout beachten; keine Änderungen beim Checkout mitnehmen.
- In diesem Fall nur die betroffenen Pfade und das Hindernis nennen, keine Inhalte
  aus Credentialdateien lesen. Nicht bereinigen, stagen, committen, stashen oder
  verwerfen. Der Benutzer prüft/committet zuerst. Kein Merge trotz sauberem Teilbereich.
- Bereits laufenden Merge/Rebase oder eine anderweitig belegte Zielumgebung nicht
  überschreiben. Bei identischen Branches oder bereits enthaltener Quelle keinen
  neuen Merge erfinden; offen nennen, welche Prüfung noch beauftragt/offen ist.

## 2. Featurebezogene Abschlussbereinigung

- Vereinbarte Anforderungen, Branch-Diff und aktive Pläne/Verweise prüfen. Nach main
  bedeutet der Mergeauftrag zugleich Feature-Abnahme und Abschlussauftrag; keine
  zusätzliche Formel „Umsetzung abgeschlossen“ verlangen. Andere Ziele schließen
  das Feature nur bei entsprechend eindeutigem Auftrag ab.
- Nachweislich überflüssige Klassen, Methoden, Variablen, Imports und Ressourcen
  im Featureumfang bereinigen. Dynamische Verwendung/Framework-Einbindung prüfen,
  nicht nur Textreferenzen zählen. Keine globale Refaktorierung oder fremde Bereinigung.
- Erledigte Feature-Pläne und Provisorien entfernen, gültige Bedienungs-/Fachdokumentation
  und Links aktualisieren. Offene Folgefunktionen nach TODO, unbestätigte Testaufgaben
  in `docs/testbedarf.md` erhalten. Unerledigte notwendige Anforderungen nicht durch
  Löschen des Plans verdecken. Bei bloßer Zwischenintegration aktive Pläne erhalten.
- Entstehen Bereinigungsänderungen, diese auf der Quelle belassen, vorgeschriebene
  Stilprüfungen und passende gezielte Tests ausführen. Zusammenfassung und Commit-
  Message liefern, dann anhalten. Kein automatischer Bereinigungscommit und kein Merge.
  Nach Benutzercommit und entsprechendem Weiterauftrag erneut den sauberen Stand prüfen.
- Größere fachliche Änderungen abstimmen. Erfolgreiche unveränderte Belege berücksichtigen;
  die vor dem Merge nötige Prüfung nicht zu einer ungefragten großen QA ausweiten.

## 3. Lokal integrieren

- Nur bei sauberem geprüften Stand den Quellbranch in den vereinbarten Zielbranch
  übernehmen. Bestehende Commits erhalten: regulärer Merge/Fast-forward, kein Squash,
  Rebase, Amend, Hard-Reset oder erzwungenes Umschreiben von Branches.
- Der für die Integration notwendige Merge-Commit ist vom Mergeauftrag umfasst.
  Überschaubare Konflikte nach beiden fachlichen Verträgen lösen; keine Seite blind
  überschreiben. Bei unklarer Fachentscheidung anhalten und den tatsächlichen Zustand
  samt nötigem nächsten Schritt melden. Fremde Prozesse/Arbeiten erhalten.
- Quellbranch immer behalten, auch nach erfolgreichem Merge. Keine lokalen/Remote-
  Branches, Tags oder Worktrees zur Aufräumung löschen. Kein Push.

## 4. Auf dem tatsächlichen Ziel nachprüfen

- Die [Nachprüfung](references/nachpruefung.md) anwenden: Java 25, aktueller Build,
  vollständige App-/Shared- und Browser-Helfertests samt gemeinsamer Java-Coverage
  im Root-Reactor und isolierter Start des frischen `app/target/`-JARs bis zur
  Betriebsbereitschaft. Der technische Bericht liegt in `coverage-report/target/`.
  Zielbranch nennen, nicht immer main behaupten. Build und JUnit möglichst in einem
  Lauf kombinieren; nicht dieselben Anwendungstests mehrfach unnötig ausführen.
- Keine automatische Browserrunde, große QA oder Sonar-Bereinigung. Zusätzliche
  umfangreiche Prüfungen bei fachlichem Bedarf begründen und vorher freigeben lassen.
- Bei Nachprüfungsfehlern Ursache untersuchen; kleine klare Fehler ggf. mit passenden
  Regressionen korrigieren. Neue Korrekturen uncommittiert zur Benutzerprüfung mit
  Commit-Message zurückgeben; keine alten Commits umschreiben oder Nachkorrekturen
  eigenmächtig committen. Den Merge dann nicht als vollständig abgenommen melden.
- Ein geglückter Git-Merge ist nicht automatisch ein erfolgreicher Build/Start.
  Bei fehlenden Voraussetzungen tatsächlichen Integrationsstand und offenen Nachweis
  trennen; keine stillen Rollbacks oder Erfolgsmeldung trotz Fehler.

## 5. Kurzer Abschluss im Chat

- Quelle/Ziel, tatsächlichen Merge-/Commitstand, Bereinigung, ausgeführte JUnit-Zahlen,
  Build und Startprüfung sowie offene Punkte knapp auf Deutsch berichten. Hashes nur
  aus Git beziehen. Quellbranch bleibt erhalten; Push erfolgt durch den Benutzer.
- Standardmäßig keine Berichtsdatei. Falls ausdrücklich gewünscht, einen aktuellen
  Bericht unter festem Namen ersetzen, keine datierte Sammlung erzeugen.
- Offenen Testbedarf erhalten und nennen. Die letzte vollständige QA-Baseline bzw.
  `docs/status.md` nicht durch eine bloße Mergeprüfung als vollständig neu geprüft ausgeben.
- Nur bei erfolgreicher Integration und erforderlichen Nachweisen den Featureabschluss
  bestätigen. Andernfalls exakt den noch offenen Schritt nennen.
