# Abschließende Prüfung der vier Sonar-Kontextmeldungen

Stand: 20.10.2026, auf `main`, Ausgangspunkt `c99d778`.
**Die fachliche Prüfung und ihre Nachweise sind abgeschlossen. Die vorgeschlagenen
Sonar-Statusänderungen wurden nicht ausgeführt.** Kein Commit, Push, Scan,
Quality-Gate-Wechsel oder Quellcode-Suppression durch diese Arbeit.

## Konkrete Entscheidungsvorschläge

| Finding-ID | Stelle / Regel | Empfehlung | Tragender Nachweis |
| --- | --- | --- | --- |
| `685055b4-02cc-4dd8-8181-6609338fcfd9` | `AdminUserActivityFilter.clock` / S1948 | **False positive im geprüften Produktionspfad** | Tatsächlicher System-Clock-Feldwert serialisiert und zurückgelesen; feste Clock bewahrt Zeit/Zone und bleibt im Filter verwendbar. |
| `6aebf5ce-9e84-45c8-9c71-f7696ba79a7c` | `AdminUserManagementSection.clock` / S1948 | **False positive im geprüften Produktionspfad** | Tatsächlicher System-Clock-Feldwert serialisiert und zurückgelesen; feste Clock bleibt nach dem Roundtrip für relative Aktivitätstexte und absolute Berliner Offsets verwendbar. |
| `67c5a51d-9895-41e9-8211-2ae3519813dd` | Exakter H2-Klassenvergleich / S1872 | **Accepted – bewusste, begründete Abweichung** | Echter nativer Servlet wird erkannt; eine Unterklasse mit passendem Namen/Mapping und vertrauenswürdigem Origin bekommt die Ausnahme ausdrücklich nicht. |
| `aa8853f3-b91a-4fdd-bd8f-fcc970fe10cb` | CSRF-RequestMatcher / S4502 | **Accepted – geprüfte Schutzabgrenzung** | Bestehende HTTP-/Flow-/H2-Angriffstests erneut erfolgreich; klassische App-Mutationen bleiben Spring-CSRF-geschützt. |

Die zugehörigen vollständigen Begründungen zum Übernehmen in Sonar stehen in
[sonar-kontextentscheidungen-2026-10-20.csv](sonar-kontextentscheidungen-2026-10-20.csv).
Sie sind Vorschläge, keine bereits gesetzten Serverentscheidungen.

## 1. Tatsächliche Clock-Felder: serialisierbarer Standardwert

Der produktive Aufruf in `AdminView` verwendet den öffentlichen Konstruktor von
`AdminUserManagementSection`. Dieser wählt `Clock.systemUTC()` und reicht dieselbe
Clock an den Aktivitätsfilter weiter. Der eigenständig nutzbare Standardkonstruktor
des Filters verwendet ebenfalls `Clock.systemUTC()`. Es wurde kein anderer
produktiver Aufruf der eingeschränkten Clock-Testkonstruktoren gefunden.

Die [Java-25-Dokumentation](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/time/Clock.html#systemUTC())
sichert für die konkrete System-Implementierung Serialisierbarkeit zu; dasselbe
gilt für `Clock.fixed()`. Die abstrakte Basisklasse `Clock` schreibt diese
Eigenschaft dagegen nicht jeder denkbaren Implementierung vor.

Vier neue Ausprägungen in `AdminUserActivityClockSerializationTest` prüfen:

1. Den tatsächlichen Clock-Feldwert einer normal erzeugten Filterkomponente durch
   `ObjectOutputStream` und `ObjectInputStream` übertragen; Gleichheit, UTC-Zone
   und weitere Nutzbarkeit prüfen.
2. Dasselbe für die normal erzeugte Benutzerverwaltung.
3. Feste Clock aus dem tatsächlichen Filterfeld übertragen, Instant und abweichende
   Clock-Zone erhalten und die zurückgelesene Clock in einem neuen Filter verwenden.
   Am Herbstwechsel ergeben die Platzhalter weiterhin das Berliner Datum und 02:45.
4. Dasselbe für die Benutzerverwaltung: relative 15 Minuten und der eindeutige
   Berliner Offset +01:00 bleiben nach dem Roundtrip korrekt.

**Prüfgrenze:** Es wird nur die Clock selbst aus dem tatsächlichen Feld übertragen.
Der vollständige Komponenten-/UI-/Sitzungsgraph wurde nicht serialisiert. Die
Komponenten für die anschließende Verwendung werden regulär neu konstruiert.
Dies ist weder eine Freigabe für beliebige nicht serialisierbare Custom-Clocks
noch ein Nachweis, dass alle früheren 17 Session-Serialisierungsrisiken erledigt
wären. Bei neuen produktiven Clock-Implementierungen oder einer geforderten
UI-Passivierung muss der Vertrag erneut geprüft werden. Dafür wurde keine
zusätzliche Serialisierungsarchitektur eingeführt und kein Feld blind transient.

## 2. Exakter H2-Typ ist eine beabsichtigte Einschränkung

Die Ausnahme ist an die tatsächliche H2-Registration gebunden, nicht an eine
allein vom Client gewählte URL. Neben Null- und Freigabeprüfung verlangt der
Filter den exakten nativen Klassennamen sowie den echten Servletnamen und das
Mapping aus dem Container. H2 ist im POM ausdrücklich eine Runtime-Abhängigkeit.

`instanceof JakartaWebServlet` wäre unter diesem Compile-Vertrag kein direkter
Ersatz und würde außerdem jede Unterklasse einschließen. Der neue Test
`nativeConsoleSubclassDoesNotReceiveTheExactServletExemption` belegt den
bewussten Unterschied: Eine Unterklasse bekommt selbst bei korrektem Mapping
und lokalem Origin weder die native Zuordnung noch die Ausnahme. Die bestehenden
Positivtests mit dem echten nativen Servlet bleiben erfolgreich.

Die Regel beschreibt eine übliche Typprüfungsempfehlung, trifft aber nicht die
hier verlangte Gleichheitspolitik. Deshalb ist Accepted als begründete
Abweichung angemessener als eine Aussage, die Regel sei generell falsch.
Bei einem geänderten H2-Servlet oder bewusst anderer Verdrahtung neu prüfen.

## 3. CSRF bleibt aktiv; die Ausnahmen haben einen eigenen Schutz

`SecurityConfig` ruft kein globales `csrf.disable()` auf. Der eigene Matcher
schützt klassische Mutationen mit Spring-Tokens. Die geprüften, tatsächlich
gemappten Flow-Transporte nutzen ihre Framework-Prüfungen; die ausdrücklich
freigeschaltete lokale H2-Konsole muss durch den vorgelagerten Herkunftsfilter.

[Spring Boot](https://docs.spring.io/spring-boot/reference/data/sql.html#data.sql.h2-web-console)
weist darauf hin, dass die native H2-Entwicklungskonsole Spring-CSRF nicht selbst
unterstützt. Die eigene Ausnahme ist deshalb auf ihren dokumentierten lokalen
Host-/Servletvertrag begrenzt. Der Matcher erlaubt keine pauschale Umgehung für
Login/Logout oder einen beliebigen VAADIN-/H2-URL-Präfix.

Erneut ausgeführte technische Nachweise:

- `CsrfSecurityChainTest`: fehlende, falsche und fremde Tokens, gültiger HTTP-Login,
  Tokenrotation, POST-Logout, Remember-me, manueller Flow-Login, Bootstrap und
  gefälschte Transportmarker.
- `FlowCsrfRequestMatcherTest` / `PushRpcSecurityPersistenceTest`: echte Root-/Push-
  und Ressourcenpfade, falscher Flow-Token, tatsächliche Handler-/RPC-Grenzen und
  fehlende oder veraltete Berechtigungen.
- `H2ConsoleOriginFilterTest` / `H2ConsoleHttpTest`: echte native Anmeldung,
  SQL-Abfrage und Logout durch die SecurityFilterChain; fremde/fehlende Quellen
  für GET/POST-Mutationen, widersprüchliche Header und gescheiterte Versuche,
  mit Konsolenheadern die Spring-CSRF-Prüfung der Anwendung zu umgehen.
- `CsrfAdmissionTest` / `SecurityChainRegressionTest`: Session-/Login- und
  Berechtigungsverträge bleiben erhalten.

Daher Empfehlung Accepted für die bewusste Schutzabgrenzung. Das ist keine
Garantie gegen jeden möglichen Angriff oder für andere Proxy-/Hostumgebungen.
Für neue Transporte, externe H2-Freigaben oder geänderte Browser-/Proxyverträge
ist ein neues Review erforderlich. Frühere Browserabnahmen bleiben Nachweise
ihrer jeweiligen damaligen Stände; hier wurde kein neuer Browserlauf gestartet.

## Tatsächliche Prüfungen

| Prüfung | Ergebnis |
| --- | --- |
| Neue Clock-Roundtrips | **4/4 erfolgreich** |
| Neuer H2-Unterklassentest | **1/1 erfolgreich** |
| Gezielter Kontext-/H2-/CSRF-/Komponentenlauf | **179/179 erfolgreich**, keine Fehler oder Skips |
| Vollständiger Maven-Lauf mit Produktionsprofil und `verify` | **1983/1983 erfolgreich**, keine Fehler oder Skips; Produktions-JAR gebaut |
| Browser-Helfer ohne Browser-/Anwendungsstart | **48/48 erfolgreich**, keine Fehler oder Skips |
| Spotless apply/check und Checkstyle nach letzter Java-Änderung | Erfolgreich, keine Stilabweichungen |
| Abschließende Diff-/Whitespaceprüfung | Erfolgreich |

Fünf zusätzliche Testausprägungen gegenüber dem Ausgangsstand mit 1978. Die
gezielten Fälle und neuen Tests sind Teil der 1983 und werden nicht zusätzlich
als eigenständige Gesamtmenge gezählt. JDK 25.0.4.1, Maven Wrapper 3.9.16.
Frische JaCoCo-Messung nur aus diesem vollständigen Lauf: **10209/11636 Zeilen
(87,74 %)** und **2964/3854 Zweige (76,91 %)**, 219 analysierte Anwendungsklassen.

Geändert wurden ausschließlich Nachweistests und Dokumentation. Keine
Produktivlogik, Dependencies, Konfiguration oder Quellcode-Unterdrückung;
kein App-/Containerneustart, keine reale Datenbank und kein realer Mailprovider.
Die neuen Dateien ändern das laufende Anwendungsverhalten nicht; dafür ist
kein Anwendungsneustart erforderlich.

## Sonar-Status und anschließender Scan

Nach [Sonars Statusvertrag](https://docs.sonarsource.com/sonarqube-community-build/user-guide/issues/solution-overview)
bezeichnet False positive eine im Kontext unzutreffende Analyse und Accepted
eine bewusste Entscheidung, einen Hinweis vorerst oder dauerhaft nicht durch
Codeänderung zu beheben. Die vier Vorschläge sind keine Lockerung des Quality
Gates und kein Abschalten der jeweiligen Regeln. Ein tatsächlicher Issue-
Statuswechsel bleibt ausdrücklich freizugeben.

Der lokale SonarQube-Server ist erreichbar und meldet Version 26.10.0.133078,
Status UP. Der anonyme Terminalzugang ist nicht authentifiziert; SONAR_TOKEN
ist dort nicht gesetzt. Es wurde kein Token erzeugt, aus Dateien gelesen oder
in einem Artefakt gespeichert. Die zuvor vorhandene lokale Analyse-Metadatei
wurde nur gelesen. **Ein neuer Sonar-Scan/Export wurde in diesem Paket nicht
ausgeführt.** Die Rückfrage, ob der Benutzer wie bisher scannt oder ein lokales
Token bereitstellt, ist noch offen. Ein grünes Quality Gate wird nicht behauptet.

Nächster Schritt: dokumentierte Statusentscheidungen ausdrücklich freigeben und
anwenden; danach den aktuellen Stand erneut analysieren und vollständig exportieren.
Dabei anhand der IDs prüfen, ob die sieben vorherigen Codekorrekturen erkannt
wurden und ob die fünf neuen Nachweistests weitere relevante Meldungen erhalten.
