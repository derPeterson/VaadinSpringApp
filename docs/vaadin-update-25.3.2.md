# Vaadin-Update auf 25.3.2

Stand: 20.10.2026, Branch `feature/user-messages`, Ausgangspunkt
`97c6aad7151deaf1c05f86ef8dc8b32a01f022af`.

## Änderung

Die zentrale `vaadin.version` in `pom.xml` wurde von 25.3.0 auf 25.3.2 gesetzt.
BOM und Vaadin-Maven-Plugin verwenden dieselbe Eigenschaft. 25.3.2 ist laut den
[offiziellen Release Notes](https://github.com/vaadin/platform/releases/tag/25.3.2)
die aktuelle stabile Wartungsversion; die zugehörigen Flow- und Web-Component-
Versionen sind 25.3.3. Die Java-Komponenten verwenden 25.3.2.

`package.json`, `package-lock.json` und das bereits versionierte
`src/main/bundles/prod.bundle` wurden durch den Vaadin-Build erzeugt und aus der
geprüften Quellkopie übernommen. Paketversionen und Bundleinhalt wurden nicht
von Hand ersetzt. Zum generierten Paketstand gehören auch aktualisierte
Buildwerkzeuge und die vom Plugin vorgegebenen TypeScript-Paketaliase.

Das versionierte Entwicklungsbundle bleibt unverändert. Die Anwendung verwendet
bereits `vaadin.frontend.hotdeploy=true` und damit den Frontend-Entwicklungsserver
anstelle dieses vorbereiteten Bundles. Der Produktionsbuild hat das tatsächlich
verwendete Produktionsbundle mit den neuen Abhängigkeiten vollständig erneuert.
Grundlage: [Vaadin-Entwicklungsmodi](https://vaadin.com/docs/latest/flow/configuration/development-mode).

Anwendungs-Java, eigene JavaScript-Bausteine, Übersetzungen und Theme-Quelldateien
wurden nicht geändert. Java 25 und Spring Boot 4.1.1 bleiben erhalten.

## Beobachtete Prüfung

Die Prüfung lief in einer eigenen temporären Quellkopie ohne echte Daten,
Logdateien oder lokale `.env`-Dateien. JDK 25.0.4.1 und Maven Wrapper 3.9.16
wurden geprüft; der Frontend-Build verwendete Node.js 24.21.0.

- Shared-Modul frisch gebaut und installiert.
- Vollständiger Produktionsbuild mit Clean/Verify einschließlich Frontend-Neubau
  und Spring-Boot-JAR erfolgreich.
- Alle **1843 Anwendungs-JUnit-Tests** erfolgreich, keine Fehler/Fehlschläge/Skips.
- **48 Browser-Helfertests** erfolgreich, keine Fehler/Fehlschläge/Skips.
- **Zwölf Frontend-JavaScript-Tests** erfolgreich, keine Fehler/Skips.
- Spotless-/Checkstyle-Prüfung erfolgreich; kein Java-Code geändert.
- Das erzeugte JAR enthält unter anderem `flow-server-25.3.3.jar`,
  `vaadin-button-flow-25.3.2.jar` und `vaadin-core-25.3.2.jar`.
- Die übernommenen Paketdateien und das Produktionsbundle stimmen mit den
  automatisch erzeugten Dateien der geprüften Kopie überein.
- `git diff --check` erfolgreich.

Für dieses Update wurde kein Browser-UI-Lauf ausgeführt. Die früheren
DE-/EN-Gesamtrunden dokumentieren den Stand mit Vaadin 25.3.0 und werden nicht als
Browsernachweis für 25.3.2 ausgegeben. Die visuelle Abnahme des aktualisierten
Frameworks bleibt damit offen.

## Übernahme

Maven-Projekt in der IDE neu laden, Anwendung neu bauen und vollständig neu starten.
Ein Browser-Refresh allein übernimmt die geänderten Java-Abhängigkeiten nicht.
Die bereits laufende Entwicklungsanwendung wurde während der Prüfung nicht beendet
oder neu gestartet. Keine echten Daten verändert; kein Commit oder Push.
