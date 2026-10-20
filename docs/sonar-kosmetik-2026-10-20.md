# Kosmetische Sonar-Bereinigung vom 20.10.2026

Ausgangspunkt: `main`, Commit `2dea171`, sauberer Arbeitsbaum. Aktueller offener Export: 398 eindeutige Findings (SHA-256 `57ec2fbe5f588b895a11c73d5b76af7119ea8c9305413fa386a92bfe0101a2b6`).

## Ergebnis und Umfang

**275 der 398 exportierten Findings (rund 69 %) wurden gezielt im Code bearbeitet.** Das ist die Zahl zugeordneter IDs, keine vorweggenommene Sonar-Erfolgsmeldung. Mehrere Findings betreffen dieselbe Codezeile. Ein neuer Scan muss die tatsächlich entfallenen beziehungsweise neu gemeldeten Issues bestätigen.

84 Java-Dateien geändert: 38 Produktionsdateien und 46 Testdateien. Keine neue Architektur, Abhängigkeiten, öffentliche API oder fachliche Funktion. Konstanten bleiben lokal in ihren Klassen; kein zusätzlicher globaler Katalog. Keine pauschalen Transaktions-, Serialisierungs- oder Vaadin-Komponentenänderungen. Alle neuen lokalen Testwerte haben explizite Typen; keine neuen var-Deklarationen.

Die vollständige ID-Liste mit Ausgangsposition und Maßnahme steht in [sonar-kosmetik-findings-2026-10-20.csv](sonar-kosmetik-findings-2026-10-20.csv). Die dortigen Zeilen stammen ausdrücklich aus dem Export vor dieser Änderung und sind keine Positionsangaben für den nachfolgenden Quellstand.

| Regel | Zugeordnete Findings | Änderung |
| --- | ---: | --- |
| java:S5778 | 113 | Testvorbereitung aus der Ausnahmeprüfung herausgezogen; erwarteter Zielaufruf und Fehlervertrag erhalten |
| java:S1192 | 53 | Wiederholtes Literal als private Klassenkonstante benannt; Werte unverändert |
| java:S7467 | 42 | Unbenutzte Catch-Bindung als unnamed variable geschrieben; Behandlung erhalten |
| java:S1128 | 22 | Unbenutzten beziehungsweise doppelten statischen Import entfernt |
| java:S1130 | 9 | Nicht benötigte throws-Exception-Deklaration entfernt |
| java:S1612 | 9 | Gleichwertige Methodenreferenz für vorhandenen stabilen Empfänger verwendet |
| java:S1117 | 6 | Lokalen Namen präzisiert; Feldüberschattung beseitigt |
| java:S1481 | 6 | Unbenutzte Bindung entfernt beziehungsweise Ressource unbenannt; Ausführung und Lebensdauer erhalten |
| java:S9016 | 3 | Mockito-Mock vor dem Stubbing explizit vorbereitet |
| java:S1186 | 2 | Beabsichtigte leere Teststub-Callbacks begründet |
| java:S8924 | 2 | Vorhandenen Mockito-Aufruf statisch importiert |
| java:S6068 | 2 | Überflüssige eq-Matcher bei vollständig konkreten Argumenten entfernt |
| java:S1659 | 2 | Lokale Zahlenvariablen getrennt deklariert |
| java:S1854 | 1 | Unbenutzte Zuweisung entfernt; Login-Aufruf erhalten |
| java:S5786 | 1 | Redundante Sichtbarkeit von JUnit-Testklasse und Testmethoden entfernt |
| java:S6203 | 1 | Unveränderten JavaScript-Textblock als private Konstante benannt |
| java:S1172 | 1 | Überflüssigen Testhelferparameter und zugehörige veraltete Zeitaufnahmen entfernt |

Zusätzlich entfallen drei Folgedeklarationen `throws Exception` bei Aufrufern des bereinigten `SessionManagementTest.restart`-Helfers. Sie sind keine zusätzlichen IDs im Ausgangsexport und wurden nicht in die 275 eingerechnet.

## Fachliche Verträge erhalten

Bei 113 Ausnahmeprüfungen stehen Konstruktion, Getter, Kennungen und Eingabewerte nun vor `assertThrows`. Die Prüfung umfasst den fachlichen Aufruf, der tatsächlich scheitern soll. Damit kann beispielsweise ein Fehler beim Erzeugen eines Testobjekts nicht mehr versehentlich als erwartete Ablehnung des Services gelten. Vorbereitung bleibt im selben Test, Try-/Lock-/Transaktionskontext; Parameterfälle, erwartete Ausnahmetypen und bestehende Zustandsprüfungen bleiben erhalten. Bei verzweigten Parameterfällen wird die Testoperation vor der Ausnahmeprüfung ausgewählt.

Unbenutzte Catch-Namen wurden durch `_` ersetzt; Catch-Typen, Rückgaben, Unterbrechungsbehandlung und Fehlergrenzen bleiben identisch. Technische Attributnamen, CSS-Klassen, Login-Fehlercodes, Fehlermeldungen und Routen behalten ihre Werte. Die lokale Home-Pfadkonstante verwendet die bereits vorhandene gemeinsame HOME_ROUTE-Konstante. Keine Theme-, Sprach- oder Layoutänderung.

Der bisherige Tooltip-JavaScript-Textblock in ConfigInfoButton wurde nur als private Konstante benannt. Sein vollständiger Text inklusive Zeilenumbrüchen ist exakt identisch; der Attach-Zeitpunkt des Aufrufs bleibt erhalten.

Ein zusätzlicher Tokenabgleich aller 38 Produktionsdateien gegen HEAD bestätigt: Nach Auflösung ausschließlich der neuen Konstanten, Importänderungen, beabsichtigten lokalen Namen, unbenutzten Catch-Bindungen, gleichwertigen Methodenreferenzen, getrennten Deklarationen und überflüssigen throws-Deklaration bleiben keine weiteren Tokenänderungen. Dies ergänzt den Review und Testlauf; es ersetzt keine allgemeine Verhaltens- oder Sicherheitsgarantie.

## Bewusst ausgelassen

- Die beiden als unbenutzt gemeldeten TrackedUserAppLayout-Felder in UI-Tests bleiben als starke Fixture-Referenzen erhalten. Nicht entfernt und kein Sonar-Status verändert.
- H2-Ankerverbindungen und MockedConstruction-Scopes bleiben geöffnet beziehungsweise geschlossen wie zuvor. Nur die unbenutzte Bindung ist jetzt `_`; keine verkürzte Ressourcenlebensdauer.
- Der unbenutzte Parameter des geschützten hasRequiredAccess-Hooks bleibt Teil des Override-Vertrags.
- Die wiederholte Basisfarbe in den UserStatus-Enum-Konstruktoren bleibt unverändert. Keine zusätzliche Holder-Klasse oder Änderung der Enum-Initialisierung allein für einen kosmetischen Hinweis.
- Komplexität, breite Konstruktoren, Vererbung, Transaktionsaufrufe/-grenzen, nullable Verträge, Sessionserialisierung, Zeitsemantik, SQL-/Security-Bewertungen und numerische Randfallsemantik gehören nicht in dieses Paket.
- Keine Regeln deaktiviert, keine Suppressions und keine weiteren Sonar-Statusänderungen. Die zuvor ausdrücklich freigegebenen zwei False-Positive- und zwei Accepted-Entscheidungen bleiben bestehen.

## Tatsächlich ausgeführte Prüfung

| Prüfung am finalen Stand | Ergebnis |
| --- | --- |
| Maven Wrapper / JDK | Maven 3.9.16, JDK 25.0.4.1 |
| Root Spotless apply, Spotless check, Checkstyle nach letzter Java-Änderung | Erfolgreich, keine Stilabweichungen |
| Vollständiger Lauf mit Produktionsprofil und verify | **1983 Tests**, **0 Fehler**, **0 Failures**, **0 Skips** |
| Produktionsartefakt | Ausführbares JAR erfolgreich gebaut |
| JavaScript-Textblockvergleich | Exakt identisch |
| Eingeschränkter Produktions-Tokenabgleich | 38/38 Dateien erfüllen die oben benannten kosmetischen Transformationen |
| Diff-/Whitespaceprüfung | Erfolgreich |

Finaler Produktionslauf beendet am 20.10.2026 um 18:58:55 +02:00. Surefire-XML-Summen stimmen mit der Maven-Zusammenfassung überein. Eine während der ersten Kompilierung aufgefallene übergreifende Namensersetzung wurde vor den erfolgreichen vollständigen Runden korrigiert. Nach anschließender Entfernung der drei Folgedeklarationen wurden die vorgeschriebenen Stilprüfungen und der vollständige Produktionslauf erneut ausgeführt.

Frische JaCoCo-Messung ausschließlich aus dem finalen Lauf: **10207/11634 Zeilen (87,73 %)**, **2964/3854 Zweige (76,91 %)**, **219 analysierte Klassen**. Dies sind JUnit-Zeilen-/Zweigquoten, nicht die anders aggregierte Sonar-Coverage. Kein neuer Browser-UI-Lauf, kein Dockerimage, keine erneut gestarteten Browser-Helfertests oder eigenständigen Frontend-Tests. Der vollständige Anwendungslauf enthält die vorhandenen DE-/EN-Komponententests.

## Sonar-Abgleich und Einführung

Für diesen Quellstand wurde noch kein Sonar-Scan gestartet und kein neuer Quality-Gate-Zustand behauptet. Das zuvor beobachtete grüne Gate betraf den Ausgangsstand nach den vier freigegebenen Kontextentscheidungen. Als Nächstes den aktuellen Stand erneut scannen und die offenen Findings exportieren; anhand der CSV-IDs entfallene, verbliebene und neue Hinweise getrennt abgleichen.

Für die laufende Anwendung sind keine sichtbaren Änderungen beabsichtigt. Um die Java-Änderungen zu übernehmen, das gebaute Produktions-JAR verwenden und die Anwendung neu starten; ein Browser-Refresh übernimmt keine Java-Klassen. Kein bestehender App-/Containerprozess wurde dafür verändert. Keine realen Datenbanken zurückgesetzt, keine realen Mails versendet. Alle Testdatenbanken/SMTP-Gegenstellen waren Testfixtures.

Keine Commits, Pushes oder Dependency-/Lockdateiänderungen. Die Änderungen liegen zur Prüfung auf main.
