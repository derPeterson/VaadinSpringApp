# Arbeitsanweisungen für Coding-Agenten

## 1. Orientierung

- Lies zuerst `README.md`, danach nur Dateien, die für die Aufgabe relevant sind.
- Prüfe README-Aussagen anhand des aktuellen Codes, wenn die Aufgabe davon abhängt.
- Suche gezielt nach Klassen, Aufrufern und Konfiguration; lies nicht das ganze Projekt.
- Überspringe Abhängigkeiten (`node_modules/`), generierte Dateien, `data/` und `logs/`.
- Lies keine `.env`-Dateien, Datenbanken oder Dateien mit Zugangsdaten.

## 2. Projektkonventionen

- Behalte Java 25 und den Spring-Boot-/Vaadin-Flow-Stack aus `pom.xml` bei.
- Baue Views mit den bestehenden Vaadin-Java-Komponenten. Ersetze sie nicht durch
  HTML-Templates, React oder ein anderes Frontend.
- Java-Code liegt unter `src/main/java/de/derpeterson/app/`.
- Nutze die vorhandene Aufteilung: `views/` und `ui/` für Oberfläche,
  `service/` für Anwendungslogik, `repository/` für JPA-Zugriffe, `model/` für Daten.
- Folge den benachbarten Dateien: Namen wie `*View`, `*Service`, `*Repository`,
  `*Entity`; vorhandene Konstruktor-Injektion und Lombok-Muster beibehalten.
- Mache kleine, gezielte Änderungen. Keine ungefragten Refactorings.
- Ändere Dependencies oder Versionen nur, wenn die Aufgabe es erfordert.
- Styles und UI-Bilder: `src/main/resources/META-INF/resources/custom-theme/`.
  Styles werden in `Application.java` eingebunden; weitere Icons liegen unter
  `src/main/resources/META-INF/resources/icons/`.
- Übersetzungen: `src/main/resources/i18n/`; Mailvorlagen: `src/main/resources/email/`.
- Lade serverseitige Ressourcen über Classpath-Streams, nicht über `src/`-Dateipfade.
  Das muss auch im JAR funktionieren.
- Bearbeite `src/main/frontend/generated/`, `vite.generated.ts` und `target/`
  nicht manuell. Eigene Vite-Anpassungen gehören in `vite.config.ts`.
- Behalte den lokalen Code-Stil bei. Spotless ist mit `eclipse-formatter.xml`
  konfiguriert; formatiere nicht ungefragt das gesamte Projekt.

## 3. Ausführung und Fehlerdiagnose

- Beachte die tatsächliche Shell: unter Windows PowerShell-Befehle verwenden.
- Verwende im Projektstamm den Maven Wrapper `.\mvnw.cmd`, nicht globales Maven.
  Unter Linux/macOS ist das Gegenstück `./mvnw`.
- Prüfe vor Builds mit `.\mvnw.cmd --version`, dass tatsächlich JDK 25 verwendet wird.
  Bei einer anderen Version nicht einfach weiterbauen oder Systemsoftware ändern.
- Gib Maven-Ziele ausdrücklich an: Ein Wrapper-Aufruf ohne Ziel startet laut
  `pom.xml` standardmäßig die Anwendung.
- Bei Buildfehlern prüfe zuerst deinen Diff und die erste relevante Fehlerursache,
  nicht nur nachfolgende Fehlermeldungen. Berichte externe Blocker klar.
- Setze Java nicht herab und entferne keine Funktionalität, um Fehler zu umgehen.
- Prüfe bei API-Änderungen die offizielle Dokumentation der verwendeten Version.
  Kennzeichne Vermutungen und ungeprüftes Verhalten, statt sie als Fakten auszugeben.

## 4. Validierung

- Führe passende vorhandene Tests aus, sofern die Aufgabe Ausführung erlaubt.
- Ergänze bei Verhaltensänderungen gezielte Tests für das erwartete Verhalten.
- Tests liegen unter `src/test/java/`; nutze bestehende JUnit-Muster.
  `ImageHelperTest` ist ein Beispiel ohne Anwendungsstart oder Datenbank.
- Unterscheide Build-Erfolg, tatsächlich ausgeführte Tests und manuelle
  Funktionsprüfung. Ein Build ohne Testklassen ist kein bestandener Funktionstest.
- Beachte: Die Compile-Phase kann bereits Frontend-Dateien generieren.
- Das Maven-Profil `it` startet und stoppt die Anwendung; nicht ohne Auftrag nutzen.
- Ändere keine Datenbankdaten, starte keine Anwendung und versende keine E-Mails,
  sofern die Aufgabe dies nicht umfasst.
- Prüfe zum Abschluss `git diff --check` und den Umfang der Änderungen.
- Falls ein Build generierte, bereits versionierte Dateien verändert:
  Prüfe diese Änderungen und berichte sie. Nimm sie nur auf, wenn sie zur
  Aufgabe gehören, Entferne sie nicht pauschal.

## 5. Git

- Prüfe vor Änderungen Branch und Arbeitsstand mit `git status --short --branch`.
- Bewahre vorhandene Änderungen des Nutzers. Überschreibe oder bereinige sie nicht.
- Befolge den Branch-, Commit- und Rückwechselablauf eines ausdrücklich verwendeten
  OpenCode-Kommandos. Erfinde keinen eigenen Ablauf; kläre Unklarheiten.
- Ohne entsprechenden Auftrag keinen Commit, Merge oder Push durchführen.
- Keine destruktiven Git-Befehle wie `git reset --hard`, `git checkout --` oder
  `git clean`, um fremde Änderungen zu entfernen.

## 6. Ehrlicher Abschlussbericht

- Nenne geänderte Dateien und das Ergebnis knapp auf Deutsch.
- Berichte nur tatsächlich ausgeführte Befehle und beobachtete Ergebnisse.
- Nenne die Anzahl ausgeführter Tests, Fehler/Fehlschläge und offene Prüfungen.
  Wenn nichts ausgeführt wurde, sage das ausdrücklich.
- Übernimm Commit-Kennungen nur aus tatsächlicher Git-Ausgabe.
- Melde eine nicht abgeschlossene Aufgabe ausdrücklich als unvollständig.
- Falls du die Aufgabendauer berichtest: Erfasse Start und Ende per Systemzeit.
  Schätze oder erfinde niemals eine Dauer; ohne Messung keine Zeitangabe.
