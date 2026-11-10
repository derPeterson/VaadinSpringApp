# Java-Code projektweit lesbarer machen

Branch `codex/java-lesbarkeit`, angelegt vom lokalen `main` bei `84ee175e`.
Freigegeben sind alle Arbeitspakete mit Zwischencommits, das Gesamtreview,
der lokale Merge nach `main`, Sonar mit Behebung und Nachscan sowie die große
Testrunde auf dem bereinigten Stand.

## Ziel und Umfang

Den versionierten Java-Code in `app`, `shared` und `browser-tests` einschließlich
Testcode vollständig sichten. Auf dem Ausgangsstand sind das 522 Java-Dateien.
Die App enthält 283 Haupt- und 149 Testdateien, Shared enthält 5 Dateien und das
Browser-Testmodul 85. Der [Codestil](../entwicklung.md#codestil) ist der Maßstab.

Klassen, Methoden und Variablen so benennen, dass ihre Aufgabe auch an der
Verwendungsstelle erkennbar ist. Zusammengehörige Felder und Schritte in Blöcken
anordnen. Lange Abläufe dort gliedern, wo die Aufgabe wechselt oder eine benannte
Teilmethode einen fachlichen Fall klarer macht. Kommentare und Sichtbarkeit
gezielt einsetzen. Jede Datei sichten und konkrete Lesbarkeitshürden beheben.

## Offene Arbeitspakete

- [ ] **App-Oberfläche:** Nach den Kontoeinstellungen und der 2FA-Kontokomponente
  die übrigen Views, UI-Komponenten, zugehörige
  UI-Helfer und Texte paketweise sichten. Vaadin-Aufbau, Sprachwechsel und
  Klickabläufe müssen sich in der Klasse leicht finden und nachvollziehen lassen.
- [ ] **App-Fachlogik und Infrastruktur:** Anmeldung, Berechtigungen, Sitzungen,
  Dienste, Datenzugriff und die übrigen Hauptquellen sichten. Bei Änderungen die
  Aufrufreihenfolge, Fehlerfälle sowie Transaktions- und Sperrgrenzen prüfen.
- [ ] **App-Tests:** Alle Testquellen paketweise sichten. Testnamen, Vorbereitung,
  Ausführung und Erwartungen sollen das jeweilige Szenario erkennen lassen.
- [ ] **Browser-Tests:** Java-Tests, Page Objects und Hilfscode sichten. Szenarien,
  Selektoren, Wartebedingungen und Assertions bei der Überarbeitung erhalten.
- [ ] **Gesamtreview:** Abdeckung aller drei Module mit der Liste der versionierten
  Java-Dateien abgleichen und aussagekräftige Vorher-nachher-Stellen vorlegen.
- [ ] **Integration:** Den vollständig geprüften Branch lokal nach `main` mergen.
- [ ] **Sonar:** Auf `main` analysieren, berechtigte Findings beheben und committen.
  Einen vollständigen Nachscan auswerten.
- [ ] **Große Testrunde:** Den bereinigten Stand vollständig prüfen, notwendige
  Korrekturen committen und die betroffenen Prüfungen wiederholen.

## Prüfung und Abnahme

Pro Paket den Diff auf Aufrufer, Framework-Einstiege, Fallunterscheidungen,
Ausführungsreihenfolge und Seiteneffekte prüfen. Die vorgeschriebenen Java-Stilchecks
zentral ausführen. Umgegliederte Abläufe mit passenden vorhandenen Tests absichern.
Für reine Namens- und Blockänderungen sind Diff und Stilchecks der Nachweis.
Browserläufe, Sonar-Scans und Commits richten sich nach dem jeweiligen Auftrag.

Die Modulgrenzen und das fachliche Verhalten bleiben Maßstab für jede Änderung.
Grundlegende Architektur- oder Verhaltensänderungen separat zur Entscheidung
vorlegen und unabhängige Lesbarkeitsarbeiten fortsetzen. Den Plan während der
Umsetzung auf die jeweils offenen Pakete reduzieren.
