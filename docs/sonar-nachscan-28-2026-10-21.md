# Sonar-Nachscan: Paket mit 28 Hinweisen – 21.10.2026

## Ergebnis und Ausgangspunkt

Ausgangspunkt: `main`, Commit `f6c45ba8e6d2a2d80ba997fffe28a2df783f8e1d`, sauberer Arbeitsstand.
Der aktuelle Export enthielt 36 offene Findings. Beauftragt waren 28; die fünf S107-
Konstruktorhinweise und drei S110-Vererbungshinweise sind unverändert außerhalb dieses Pakets.

Alle 28 Hinweise wurden an ihren tatsächlichen Codestellen geprüft: **24 im Code adressiert,
vier begründet beibehalten**. Das ist eine lokale Bewertung, kein neuer Sonar-Scan und keine
Statusänderung am Server. Der nächste Benutzer-Scan muss bestätigen, welche IDs entfallen
und ob Folgehinweise entstehen. Der Export selbst wurde nicht verändert.

Die [Zuordnung aller 28 IDs](sonar-nachscan-28-findings-2026-10-21.csv) enthält Originalpfad,
Exportzeile, Maßnahme und lokale Bewertung. Die Zeilen beziehen sich bewusst auf den Export.

## Änderungen

| Bereich | Findings | Änderung und fachlicher Vertrag |
| --- | ---: | --- |
| Null-Sicherheit | 4 × S2259 | In FlowUiRecoveryFilter und DurableAuthentication Authentication/Principal/Passwortwerte konsistent einmal lesen. Fehlende oder leere aktuelle gespeicherte Passwortversion kontrolliert ablehnen. Bereits von Spring gelöschte Credentials des erfolgreich angemeldeten Principals bleiben zulässig; eine abweichende noch vorhandene Version bleibt abgewiesen. |
| Sessionattribute | 2 × S4449 | Explizite Backend-Löschung statt nullable setAttribute-Aufruf; eigener Adapter mit JSpecify Nullable. Leerer SecurityContext, explizites removeAttribute und null-Wert behalten die bisherige Löschsemantik. Auch im Adapter Authentication nur einmal lesen. |
| UI-Serialisierung | 3 × S1948 | Bestehende Ablehnung der Entity-/Formular-/Uploadgraphen erhalten; kanonische Serialisierungssignaturen verwenden. Graph-Ablehnung und eigene Schreib-/Leseschutzmethoden mit genauer Exception und Meldung getrennt geprüft. |
| Paging | 4 × S2184 | long-Operand vor Addition/Subtraktion in Inbox und Benutzerverwaltung; keine geänderten Grenzen oder Layouts. Extreme positive/negative int-Werte und maximale Seitenzahl zusätzlich geprüft. |
| Editorzuständigkeit | 1 × S3398 | Rollenauflösung in den einzigen Nutzer UserEditor verschoben. Keine Änderung an Rollenrechten, Service- oder Transaktionsgrenzen. |
| Testlesbarkeit | 10 | 5 × S5778, S1130, S7467, S8924, S9016 und S5785: Fehlerauslöser isoliert, unbenutzte Exception unbenannt, Import/Assertion/Mockvorbereitung bereinigt. Keine Assertions abgeschwächt. |

SonarSource prüft die Serialisierungshooks anhand exakter deklarierter Exceptiontypen.
Die bisherigen engeren Subtypen wurden deshalb nicht als besondere Behandlung erkannt.
Die neuen Signaturen ändern weder die tatsächlich geworfenen Exceptions noch die Ablehnung
der Passivierung. Sie entsprechen der
[Erkennung in SonarSource SerializableContract](https://github.com/SonarSource/sonar-java/blob/master/java-checks/src/main/java/org/sonar/java/checks/serialization/SerializableContract.java).
Das Verhalten des konkret installierten Analysers bestätigt erst der Neuscan.

## Vier begründete Ausnahmen

| Regel / ID | Bewertung und Begründung |
| --- | --- |
| S2143 / `5dc6d8a7-3c88-4ea4-b330-64bfde736cf5` | Notwendiger Date-Adapter für Spring Security SessionInformation. Keine Zeitrechnung und keine lokale Zeitzonenumwandlung an dieser Stelle. Gespeicherte Zeiten bleiben unverändert. |
| S1452 / `20b98fff-454d-411e-befa-ce7a5bd59e0a` | Akzeptable Framework-Grenze: JdbcIndexedSessionRepository.JdbcSession ist paketprivat. Den konkreten Typ kann die Anwendung nicht benennen. Der Wildcard bewahrt Typsicherheit ohne Rawtype/Cast oder künstliche neue Repositoryarchitektur. |
| S3077 / `165018fb-c05b-4981-a697-a058a6a38607` | False-Positive-Kandidat: ServiceReference publiziert/cacht eine Beanreferenz; alle aktuellen produktiven Aufrufer geben verwaltete Services bzw. Broadcaster weiter. Die Klasse verändert deren internen Zustand nicht. volatile schützt die Referenzveröffentlichung, die Services verantworten ihre eigene Nebenläufigkeit. AtomicReference würde die angemahnte Zustandsfrage nicht zusätzlich lösen. |
| S2142 / `1d9d9d87-0613-43e5-a4aa-806fa094466a` | False-Positive-Kandidat für die methodenübergreifende Behandlung: awaitCompletion merkt den Interrupt, unterbindet weitere Annahmen und wartet die bereits angenommenen Tasks ab. process stellt das Flag garantiert im finally wieder her. Sofortiges Reinterrupt würde das blockierende Drain wiederholt unterbrechen. Bestehender kontrollierter Regressionstest und weitere Interruptfälle sind erfolgreich nachgelaufen. |

Die betreffenden Gründe stehen zusätzlich direkt an den Codegrenzen. Eine Sonar-Accepted-
oder False-Positive-Entscheidung wurde **nicht** vorgenommen. Für S1452 wurden die tatsächliche
[JdbcSession-Sichtbarkeit in Spring Session 4.1.1](https://github.com/spring-projects/spring-session/blob/4.1.1/spring-session-jdbc/src/main/java/org/springframework/session/jdbc/JdbcIndexedSessionRepository.java)
und für S4449 der dokumentierte
[Session-Löschvertrag derselben Version](https://github.com/spring-projects/spring-session/blob/4.1.1/spring-session-core/src/main/java/org/springframework/session/Session.java)
geprüft. Keine zusätzliche Nullable-Bibliothek eingeführt.

## Tatsächliche Prüfungen

- JDK **25.0.4.1**, Maven Wrapper **3.9.16** geprüft.
- Nach den Java-Änderungen im echten Projektroot Spotless apply, Spotless check und
  Checkstyle erfolgreich; **0 Checkstyle-Verstöße**.
- Gemeinsame Bibliothek in isolierter Buildkopie mit `-f shared/pom.xml clean install` gebaut.
- Gezieltes Paket: **230 Tests**, 0 Fehler, 0 Fehlschläge, 0 Skips.
- Nach Präzisierung des nullable Testtyps und der Serialisierungsassertionen:
  **37 gezielte Nachtests**, 0 Fehler, 0 Fehlschläge, 0 Skips.
- Beim Verschärfen des Serialisierungstests gab es zuvor einen Fehlschlag wegen einer
  falschen Testannahme: Vaadin kann bereits im Frameworkgraphen mit
  `ImmutableCollections$SubList` ablehnen, bevor unser writeObject erreicht wird.
  Der ursprüngliche Graph-Ablehnungscheck bleibt erhalten; die eigenen writeObject-
  und readObject-Hooks werden zusätzlich direkt mit eigenen Streams geprüft.
  Das ist kein Nachweis vollständiger UI-Passivierung; diese bleibt bewusst ausgeschlossen.
- Abschließend `clean verify -Dvaadin.productionMode=true` am finalen Quellenstand:
  **2.205 Anwendungstests**, 0 Fehler, 0 Fehlschläge, 0 Skips; Produktionsfrontend,
  ausführbares Spring-Boot-JAR und frischer JaCoCo-Bericht erfolgreich.
- **20 zusätzliche Testausprägungen** gegenüber den bisherigen 2.185 Tests;
  die genannten Teil- und Gesamtläufe überlappen und dürfen nicht addiert werden.
- **372 Build-Eingaben** aus dem Arbeitsstand und der isolierten Kopie per SHA-256
  abgeglichen. Keine .env-, Datenbank-, Log- oder fremden Laufzeitdateien kopiert.

Frische JaCoCo-Messung ausschließlich aus dem abschließenden clean-Gesamtlauf:

| Zähler | Ergebnis |
| --- | ---: |
| Zeilen | 10.777/12.205 (88,30 %) |
| Verzweigungen | 3.195/4.100 (77,93 %) |
| Klassen im Messumfang | 236 |

Die Coverage erfasst die instrumentierten Anwendungsklassen und JUnit-Ausführung; sie ist
keine Browser-, Frontend-JavaScript- oder vollständige Sicherheitsabdeckung. Die Ausnahmen
sind nicht über Suppressions oder Coverageausschlüsse verborgen. Keine neue Browserrunde
ausgeführt: Für dieses Nachscan-Paket liegt keine eigene Browserfreigabe vor.

## Einführung und verbleibende Punkte

Die Java-Änderungen werden nach Build und Anwendungsneustart wirksam. Kein Schemawechsel,
keine Datenmigration und keine Änderung des dauerhaften Sessiondatenformats. Keine
Designänderung. Rechte, Admission, Wartung, Remember-me, Logout, Sessionlimit und die
bewusst neu aufgebaute Vaadin-UI behalten ihre bestehenden Verträge.

Eine fehlende aktuelle Passwortversion führt nun absichtlich zu einem behandelten
Authentifizierungsfehler; sie wird weder als gültige Sitzung übernommen noch als NPE
weitergereicht. Normale von Spring gelöschte Login-Credentials bleiben davon unterschieden.

Offen bleiben der nächste Sonar-Scan/Export und die acht ausgeschlossenen Konstruktor-/
Vererbungshinweise. Zusätzlich können die vier dokumentierten Ausnahmen nach ausdrücklicher
Freigabe am Sonar-Server eingeordnet werden. Keine Commits, Pushes oder Sonar-Statusänderungen;
keine bestehenden Container oder realen Datenbanken verändert und keine realen E-Mails versandt.
