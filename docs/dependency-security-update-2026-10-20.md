# Gezielte Sicherheitsupdates der Java-Abhängigkeiten

Stand: 20.10.2026, `main`, geprüfter Quellstand einschließlich der zuvor
umgestellten Info-/Abbrechen-Icons. Die Ausgangsquellen entsprechen Commit
`bba54880e60c716aa9b74e2ac57855da389dc896`.

## Umfang und Versionsverwaltung

| Bibliotheksfamilie | Bisher | Jetzt | Verwendung |
| --- | --- | --- | --- |
| Tomcat Embed Core, WebSocket und EL | 11.0.24 | 11.0.27 | Laufzeit |
| Jackson Core und Databind, zusätzlich YAML im Entwicklungswerkzeug | 3.1.5 | 3.1.8 | Laufzeit / Entwicklungswerkzeug |
| Logback Classic und Core | 1.5.38 | 1.6.5 | Laufzeit |
| ASM | 9.10.1 | 9.11.1 | Ausschließlich Testabhängigkeit im aufgelösten Anwendungsbaum |

Spring Boot 4.1.1, Vaadin 25.3.2 und Java 25 bleiben erhalten. Maven Central
führt zum Prüfzeitpunkt keine neuere stabile Spring-Boot-Version; 4.2-Milestones
sind keine geeigneten Wartungsupdates. Deshalb überschreiben die zentralen
Eigenschaften `tomcat.version`, `jackson-bom.version` und `logback.version` in
`pom.xml` gezielt die Vorgaben des Boot-Parents. Die vorhandene `asm.version`
wurde aktualisiert. Zusammengehörige Module bleiben dadurch aufeinander abgestimmt.
Jackson wird über seine BOM aktualisiert, nicht durch getrennte Core-/Databind-Pins.
Die reguläre gemeinsame Jackson-Annotations-Abhängigkeit bleibt bei 2.21.

Bei einem späteren Boot-Update die vier Eigenschaften mit dessen verwalteten
Versionen vergleichen und überflüssige Overrides entfernen. Die Overrides sind
lokale Abweichungen von Boots Standardkombination; die unten dokumentierten
Prüfungen belegen den hier getesteten Umfang.

## Abgleich der gemeldeten CVEs

Doppelte Scannerzeilen wurden zusammengefasst. Die gewählten Versionen liegen
außerhalb der veröffentlichten betroffenen Bereiche aller 29 eindeutig genannten
CVEs. Quellen sind Herstellerhinweise und die jeweiligen CNA-Datensätze im
[offiziellen CVE-Katalog](https://github.com/CVEProject/cvelistV5/tree/main/cves/2026).
Dies ist ein Versionsabgleich, kein Nachweis, dass zuvor jeder Angriffsweg in
unserer Konfiguration ausnutzbar war.

- Tomcat: CVE-2026-76183, -77791, -87022, -79677, -73180, -65637, -65905,
  -65182, -68525, -86350, -68569, -65183, -66422, -77762 und -68763.
  Die betreffenden Tomcat-11-Korrekturen sind in 11.0.25 bzw. 11.0.26 enthalten;
  eingesetzt wird die veröffentlichte Wartungsversion 11.0.27.
  [Apache-Sicherheitshinweise](https://tomcat.apache.org/security-11.html),
  [Changelog](https://tomcat.apache.org/tomcat-11.0-doc/changelog.html).
- Jackson: CVE-2026-89407, -89425, -68497, -91777, -91776, -83557 und -19032.
  Die Korrekturen kamen in 3.1.6 und 3.1.7; 3.1.8 enthält zusätzlich spätere
  Korrekturen innerhalb derselben Wartungsreihe.
  [3.1.6](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.1.6),
  [3.1.7](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.1.7),
  [3.1.8](https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.1.8).
- Logback: CVE-2026-19880. Unsere Konfiguration verwendet keinen SiftingAppender
  mit MDC-basierten Dateipfaden; der beschriebene Angriffsweg war hier nicht
  erkennbar. Trotzdem erfolgt das Update. 1.6.5 enthält auch die Nachbesserung
  CVE-2026-104721 zum unvollständigen Fix aus 1.6.3. Die vorhandenen normalen
  Console-/RollingFileAppender und INFO-Schutzregeln bleiben erhalten.
  [Logback-Änderungen und Migrationshinweise](https://logback.qos.ch/news.html).
- ASM: CVE-2026-110260, -110258, -110263, -110257, -110261 und -110591.
  Die Sicherheitskorrekturen aus 9.11 sind enthalten; 9.11.1 behebt zusätzlich
  eine dort eingeführte Regression. Der aufgelöste Pfad läuft über
  `spring-boot-starter-test` → `json-smart` → `accessors-smart` → `asm`.
  Im gepackten Produktions-JAR liegt kein eigenständiges ASM-JAR.
  [ASM-Änderungen](https://asm.ow2.io/versions.html).

## Tatsächlich ausgeführte Prüfungen

Alle Builds und Tests liefen in einer eigenen temporären Quellkopie ohne echte
Datenbanken, Logdateien oder lokale `.env`-Dateien. Die Quellen wurden vor der
Übernahme per SHA-256 mit dem unveränderten Arbeitsstand abgeglichen.

- Maven Wrapper 3.9.16 und tatsächliches JDK 25.0.4.1 geprüft.
- Shared-Modul frisch gebaut und installiert; dort existieren keine eigenen Tests.
- `mvnw -Pproduction -Dvaadin.productionMode=true clean verify`: erfolgreich,
  einschließlich Kompilierung, Frontend-Prüfung und gepacktem Spring-Boot-JAR.
  Das vorhandene Produktionsbundle konnte wiederverwendet werden; keine
  versionierten Paketdateien oder Bundles wurden durch den Build geändert.
- **1843 Anwendungs-JUnit-Tests**, **0 Fehler, 0 Fehlschläge, 0 Skips**;
  anhand der XML-Berichte zusätzlich kontrolliert.
- **48 Browser-Helfertests**, **0 Fehler, 0 Fehlschläge, 0 Skips**.
  Dies sind keine Browser-UI-Tests.
- Abhängigkeitsbaum kontrolliert: alle Module entsprechen der obigen Tabelle.
- Produktions-JAR geprüft: Jackson Core/Databind 3.1.8, Tomcat Core/WebSocket/EL
  11.0.27 sowie Logback Classic/Core 1.6.5 tatsächlich enthalten.
- Spotless-/Checkstyle-Prüfung erfolgreich; 0 Checkstyle-Verstöße.
- `git diff --check` erfolgreich.

Keine neue Browser-UI-Runde, kein externer Mend-Neuscan und keine Exploitversuche.
Die bisherigen Browsernachweise sind kein Browsernachweis für diese neuen
Bibliotheksversionen. Keine Aussage über vollständige Schwachstellenfreiheit
aller Abhängigkeiten oder eingebetteter/shaded Werkzeugkopien. Der lokale
IDE-Scanner muss nach Maven-Neuladen mit seinen aktuellen Daten erneut prüfen.

## Einführung

Maven-Projekt neu laden, Anwendung neu bauen und vollständig neu starten.
Ein Browser-Refresh allein übernimmt die Bibliotheken nicht. Die laufende
Entwicklungsanwendung und reale Daten wurden nicht verändert; kein Commit/Push.
