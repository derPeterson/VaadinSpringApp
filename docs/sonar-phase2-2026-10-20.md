# SonarQube – Phase 2, 20.10.2026

**Aktualisierung zu späteren Folgepaketen:**
[CSRF einschließlich lokaler H2-Konsole](sonar-csrf-umsetzung-2026-10-20.md),
[P2-TIME für den vereinbarten Entwicklungs-Neuaufbau](sonar-time-umsetzung-2026-10-20.md)
und der unten zunächst offen gebliebene
[Worker-Abbruchbefund](email-worker-interrupt-2026-10-20.md) sind inzwischen
umgesetzt und geprüft. Der Worker-Nachtrag besteht mit 1977 Anwendungs-JUnits und
48 Helfern sowie Produktionsbuild. Die folgenden Angaben sind der historische
Phase-2-Abschluss vor diesen Folgepaketen; keine Sonar-Serverstatusänderung.

## Ergebnis und Umfang

Branch **fix/sonar-validation-session**, Ausgangscommit `5fbfee531724035f89e68fe2a552b01a9fe5b63c`. Keine Commits oder Pushes erstellt. Die drei beauftragten Korrekturpakete sind umgesetzt. Die zusätzliche Absicherung der betroffenen bestehenden E-Mail-Felder wurde vom Benutzer ausdrücklich als Teil von P1-REGEX freigegeben. Layout und Komponentenarten sind unverändert.

| Paket | Umsetzung / Ergebnis |
|---|---|
| P1-REGEX · S5998 · 49894819-377a-4697-805e-461b9fb69540 | Zentralen E-Mail-Validator auf eine nicht rekursiv gruppierte, vorab kompilierte Regel umgestellt. Gleiche ASCII-Adresspolicy einschließlich Domainsegmenten und TLD-Mindestlänge; trim()/Blank-Verhalten erhalten. Keine neue Längenbeschränkung. |
| P1-REGEX, zugehörige Feldvalidierung | Vaadins EmailField führte die frühere problematische RegEx bereits bei einer Wertänderung aus. Isoliert mit 8.192 Domainsegmenten als StackOverflowError reproduziert. Deshalb dieselbe sichere, Java-/JavaScript-kompatible Regel an die vorhandenen E-Mail-Felder in Registrierung, Login, Passwortanfrage, Verifikation, Konto/E-Mail-Änderung, Benutzerverwaltung und Mail-Test angebunden. Kein Austausch von Vaadin-Komponenten. |
| P2-SESSION · S2259 · 15e7412a-f7a7-47ad-b360-01bd0db31f9f | Authentication für die initiale Principalprüfung genau einmal aus der Session lesen. Nachfolgender Rechte-Refresh und Ablehnung verschwundener Anmeldungen bleiben bestehen. |
| P2-REGEX · S8786 · 008a0286-0e06-4cd7-94de-5b1131e291c6 | SMTP-Konfigurationsprüfung mit flachen, eindeutig getrennten Zeichenbereichen linear formuliert und vorab kompiliert. Dieselbe Regel auch im betreffenden EmailField. Die eigenständige SMTP-Policy erhalten, einschließlich zuvor erlaubter Unicode-Zeichen und besonderer Punktfolgen. ASCII-Leerraum ausdrücklich als Hexbereich formuliert, weil Java- und JavaScript-\s sonst verschieden wären. |
| P1-CSRF | Gesondert geprüft; [konkreter Vorschlag mit Regressionstestplan](sonar-csrf-vorschlag-2026-10-20.md), **nicht umgesetzt**. |
| P2-TIME | [Speicher-/Zeitzonensemantik dokumentiert](sonar-time-semantik-2026-10-20.md); **keine Zeit-/Datenmigration umgesetzt**. |

Die beiden E-Mail-Regeln bleiben fachlich bewusst getrennt: SMTP erlaubt zum Beispiel a@b.c und bestimmte Punktfolgen; die Benutzerregel verlangt einen mindestens zweistelligen letzten Domainteil und schließt leere Domainteile aus. Ein pauschales Ersetzen einer Regel durch die andere wäre eine Verhaltensänderung gewesen.

## Regressionen und Prüfungen

**18 zusätzliche JUnit-Ausprägungen gegenüber dem Ausgangsstand mit 1.843 Anwendungstests.** Eine bestehende Prüfung der exakten alten Flow-RegEx wurde durch Verhaltensabgleich ersetzt; Passwort-/Datumsprüfungen bleiben erhalten.

- UserInputRulesTest: 150 kurze Kombinationen mit der vorhandenen Flow-Regel verglichen, zusätzlich Trim-Verhalten. Lange gültige und ungültige Domainketten mit 2.048 / 8.192 / 32.768 Segmenten, Localparts mit 100.000 Zeichen, fehlende Trenner und zusätzliche @-/Punkt-/Leerzeichenfälle. Eigener EmailField-Nachweis vor dem Formularcallback.
- SecurityServiceSessionStateTest: SecurityContext verschwindet zwischen Lesezugriffen oder seine Authentication wird geleert; jeweils kontrollierte Sequenz ohne Sleeps. Request wird ohne NPE abgelehnt, Session invalidiert und kein Principal veröffentlicht. Stabile Passwort-/Remember-me-Sitzungen und anonyme Sitzung behalten ihre bisherigen Verträge.
- AdminEmailConfigurationValidationTest: beide SMTP-Einträge EMAIL_FROM und EMAIL_ADMIN; gültige/ungültige Zeichen, bestehende besondere Punkt-/Unicode-Regeln, Trim-Verhalten und lange Werte bis 50.000 Domainsegmente. Tatsächlicher Konfigurationsvalidator und automatische Feldvalidierung geprüft. Keine Mail versendet und keine Konfiguration gespeichert.
- RegistrationViewTest: echte Registrierungs-/Bestätigungsfelder nehmen lange gültige Werte ohne Stackoverflow an und erkennen lange ungültige Werte vor dem Buttoncallback. Kein User-/Verifikationsservice aufgerufen.
- Offline JavaScript (Node 24): neue Patterns mit RegExp-Flag v kompiliert, **106 kurze Policyprüfungen und drei lange Eingaben erfolgreich**. Dies prüft Syntax und RegEx-Verhalten, keine vollständige Browseroberfläche.

| Tatsächlich ausgeführte Prüfung | Ergebnis |
|---|---|
| Maven Wrapper / JDK | Maven 3.9.16, Java 25.0.4.1 |
| Shared clean install | Erfolgreich; keine Shared-Quelländerung |
| Spotless apply und check, nach letzter Java-Korrektur wiederholt | Erfolgreich |
| Checkstyle, nach letzter Java-Korrektur wiederholt | 0 Verstöße |
| Erste gezielte Anwendungs-/Securityrunde | 146 Tests, 0 Failures/Errors/Skips |
| Erster vollständiger Lauf vor der zusätzlichen Feldabsicherung | 1.857 Tests, **1 Failure**, 0 Errors/Skips; bestehender Worker-Abbruchfall, siehe unten |
| Isolierter Mail-Worker-Nachtest | 12 Tests, 0 Failures/Errors/Skips |
| Erweiterte Validator-/Feld-/Viewrunde | 109 Tests, 0 Failures/Errors/Skips |
| **Finaler vollständiger Maven-Testlauf plus jacoco:report** | **1.861 Tests, 0 Failures, 0 Errors, 0 Skips** |
| Abschließender fokussierter Nachtest nach Lesbarkeits-/Formatkorrektur der eigenen Änderungen | **44 Tests, 0 Failures/Errors/Skips** |
| Browsermodul: Maven-Helfertests | **48 Tests, 0 Failures/Errors/Skips**; Browser-Tag wie konfiguriert ausgeschlossen |

Kein neuer Anwendungs-Browserlauf, kein Produktionsimage gebaut, keine laufende Anwendung gestartet und keine bestehenden Datenbanken verwendet. Die SonarQube-Seite wurde nur zum Finding-Abgleich gelesen; das war keine Browserabnahme der Anwendung.

JaCoCo hat den lokalen Bericht mit 214 Klassen erzeugt: **10.017 / 11.452 Zeilen (87,5 %)** und **2.815 / 3.704 Zweige (76,0 %)**. UserInputRules dort 7/7 Zeilen und 14/14 Zweige. Grenzen: Das vorhandene jacoco.exec wird angehängt; keine isolierte neue Delta-Coveragemessung und keine neue Browsercoverage. Diese Zahlen sind keine Zusicherung vollständiger Sicherheitsabdeckung.

## Offener Befund aus dem vollständigen Testlauf

`EmailQueueWorkerPoolTest.interruptedCoordinatorDrainsAcceptedWorkAndRestoresItsInterruptFlag` schlug im ersten Lauf mit „erwartet 1, tatsächlich 2 Aufrufe“ fehl. Im isolierten Nachtest und im finalen vollständigen Lauf war er erfolgreich. Test und Produktionsworker sind gegenüber HEAD unverändert.

Statisch nachvollziehbarer Kandidat: Wenn completed.take() kurz vor dem Interrupt bereits zurückgekehrt ist und finished.get() für ein abgeschlossenes Future sofort zurückkehrt, kann refill noch true sein und eine zweite Aufgabe vor der nächsten unterbrechbaren Warteoperation angenommen werden. Das erklärt eine mögliche reihenfolgeabhängige Abweichung; der genaue Ablauf des fehlgeschlagenen Laufs wurde nicht instrumentiert. Den Befund deshalb **nicht als bloßen folgenlosen Testfehler oder sicher reproduzierten Ablauf ausgeben**.

Als separates Folgepaket eine explizite Interruptprüfung vor dem Nachfüllen und einen gezielt gesteuerten Regressionstest prüfen. In dieser Aufgabe keine Worker-Änderung und keine Abschwächung der Assertion vorgenommen. Die grüne Abschlussrunde beseitigt diesen offenen Zuverlässigkeitsbefund nicht.

## 411 eindeutige Findings und 423 Qualitätszuordnungen

Die Differenz ist geklärt. SonarQube Community Build 26.10 im MQR-Modus zeigt auf demselben main-Analysestand **6 Security-, 21 Reliability- und 396 Maintainability-Zuordnungen**. Deren Summe ist 423; die Kategorien sind jedoch nicht disjunkt. Die angemeldete Issue-Liste zeigt **411 issues**, passend zu den 411 eindeutigen IDs im vorhandenen Export.

Das impacts-Feld im Export enthält genau dieselben Kategorienzahlen. **Zwölf Findings haben je zwei Qualitätszuordnungen**: zwei S2077 mit Security + Maintainability und zehn S8688 mit Reliability + Maintainability. Es fehlen daher keine zwölf Findings. Die gegenteilige Vermutung im Erstbericht ist damit korrigiert. Kein neuer Scan oder Serverstatus verändert; der Dashboardstand enthält diese uncommitteten Korrekturen noch nicht.

| Sonar-ID, doppelt gezählt | Regel | Kategorien |
|---|---|---|
| 02920ba9-80c6-448e-b46c-0ac795268e20 | java:S2077 | SECURITY, MAINTAINABILITY |
| 07df6dac-d06e-4043-bccf-fb50a16435e2 | java:S8688 | RELIABILITY, MAINTAINABILITY |
| e0a00977-45c2-4156-b95b-a44fe91b898d | java:S2077 | SECURITY, MAINTAINABILITY |
| 2d5c6845-122c-4287-b99e-bfc5f1349342 | java:S8688 | RELIABILITY, MAINTAINABILITY |
| e4865373-e6d3-498c-ba15-9a8b3dadd949 | java:S8688 | RELIABILITY, MAINTAINABILITY |
| 01d7d3a7-72dd-4c06-85ce-0ed208b5b74e | java:S8688 | RELIABILITY, MAINTAINABILITY |
| 9c9b44fd-73b0-4868-8895-63b75e0397e2 | java:S8688 | RELIABILITY, MAINTAINABILITY |
| fd97e3e3-abcf-4e94-a8f1-adf60342a70c | java:S8688 | RELIABILITY, MAINTAINABILITY |
| af2a5dec-1076-4abf-8988-652dc15f8758 | java:S8688 | RELIABILITY, MAINTAINABILITY |
| cf7fb75c-7b52-452c-a30c-2c2a6dce835f | java:S8688 | RELIABILITY, MAINTAINABILITY |
| 7812e423-03d5-4803-9b80-e45141b28db7 | java:S8688 | RELIABILITY, MAINTAINABILITY |
| 76cf0da1-fe33-4949-9c7f-9a9c240bcfd9 | java:S8688 | RELIABILITY, MAINTAINABILITY |

## False Positives und begründetes Zurückstellen

[Nachvollziehbare Einzelentscheidungen als CSV](sonar-triage-2026-10-20.csv): **16 False Positives und 47 bewusst akzeptierbare Abweichungen**, jeweils mit ursprünglicher Sonar-ID, Regel, Paket, Quelle, Begründung und Empfehlung. Die Zeile reportedLine bezieht sich auf den ursprünglichen Scan, nicht auf durch diese Änderungen verschobene aktuelle Zeilen. Keine Statusänderung in Sonar vorgenommen.

- False Positives insbesondere: konstantes Enum-DDL ohne Benutzereingabe, Passwort-Validierungsregeln statt Credentials, intern korrekt verborgene UsernameNotFoundException, zwei nicht erreichbare Nullpfade, notwendige Queue-Nullprüfung, ungeeignete AutoCloseable-Vorschläge, überlauffreies Upload-Limit und drei unzutreffende Transaktionsmeldungen.
- Akzeptierte Abweichungen insbesondere: vorhandene REQUIRED-Transaktionskontexte, Framework-Vererbung und Injektion, lebensdauerrelevante Testressourcen, bewusstes API-/Kalenderverhalten und vollständige zusammenhängende UI-Assertions. Nicht allein für die Metrik umbauen.
- 161 Wartbarkeits- und 163 kosmetische Findings aus Phase 1 bleiben außerhalb der Umsetzung; erforderliche Formatierung der eigenen Änderungen ist erfolgt. Keine kosmetische Projektbereinigung.
- Avatar-Transaktionsvertrag und 17 Serialisierungsrisiken bleiben fachlich zur Entscheidung zurückgestellt. Keine pauschale Änderung an Transaktionen oder Sessionserialisierung.

## Einführung und verbleibende Risiken

Für die sichtbare Wirkung der Java-Änderungen die Anwendung **neu bauen und neu starten**; ein Browser-Refresh allein übernimmt sie nicht. Bei Docker ein neues App-Image bauen und die App später gezielt daraus starten. Hier wurde weder eine bestehende App noch ein Container neu gestartet.

Offen bleiben P1-CSRF, P2-TIME, der beschriebene Worker-Abbruchbefund und die ausdrücklich zurückgestellten weiteren Findings. Die RegEx-Timeouts sind großzügige Regressiongrenzen, keine Lasttests oder Leistungszusagen. Ein späterer Sonar-Scan muss die Regelbefunde auf dem geänderten Stand neu bewerten; eine Behebung im Server wurde nicht behauptet oder manuell markiert.
