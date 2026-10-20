# Kleines Sonar-Wartbarkeitspaket vom 20.10.2026

Ausgangspunkt: main, Commit `b7ae44d`, sauberer Arbeitsbaum. Der erneuerte Export enthält 79 eindeutige offene Findings; die vorher freigegebenen elf Accepted- und sechs False-Positive-Entscheidungen sind darin nicht mehr enthalten.

## Ergebnis und Zuordnung

17 kleinere Hinweise aus acht Regeln in neun Java-Dateien gezielt bearbeitet (sieben Produktionsdateien, zwei vorhandene Testdateien). Neun weitere kleine Hinweise mit ihren Verträgen abgeglichen und zurückgestellt. Die übrigen 53 Findings, insbesondere große Komplexität, Session-/UI-Serialisierung, Konstruktorbreite und Testassertionsaufteilung, gehören nicht zu diesem Paket.

Die 26 geprüften IDs mit ursprünglichen Exportzeilen und Einzelbegründungen stehen in [sonar-wartbarkeit-findings-2026-10-20.csv](sonar-wartbarkeit-findings-2026-10-20.csv). Keine Suppressions oder Scans. In der Codephase keine Statusänderungen; vier nachfolgend ausdrücklich freigegebene Accepted-Entscheidungen sind unten dokumentiert. Die tatsächliche Reduktion und mögliche Folgehinweise sind erst nach dem nächsten Benutzer-Scan bekannt; 17 Codebearbeitungen sind kein bereits bestätigter Serverabschluss.

## Änderungen

| Regel | Anzahl | Umsetzung und erhaltener Vertrag |
| --- | ---: | --- |
| S8694 | 2 | Month.JANUARY für Bootstrap-Geburtsdatum und DatePicker-Referenzdatum; dieselben Kalenderdaten und Eingabeformate. |
| S131 | 2 | Test-Switches weisen unbekannte Parameter explizit ab. Der bisher implizite anonyme Rollenfall bleibt als eigener Fall erhalten; Assertions unverändert. |
| S1192 | 1 | Gemeinsamen Hintergrundwert aller UserStatus-Konstanten einmal als private Klassenkonstante definiert. Getter, Farben und Theme-Token unverändert. |
| S1121 | 1 | Bytezähler und Grenzvergleich im Avatar-Ausgabestream getrennt; dieselbe Reihenfolge, Grenze und InvalidImage-Ausnahme. |
| S9395 | 8 | AffineTransform verwendet durch ein double-Literal ausdrücklich den double-Konstruktor statt des sonst gewählten float-Konstruktors. Keine zusätzlichen float-Casts. Die bestehenden ganzzahligen Matrixwerte und begrenzten Bildmaße bleiben exakt darstellbar. Alle sieben bestehenden EXIF-Ausrichtungstests erfolgreich. |
| S6204 | 1 | Interne Benutzer-Filterliste mit Stream.toList erzeugt. Alle Leser auf Größenabfrage, Paging und separate Sortierung geprüft; keine Mutation der Ergebnisliste. |
| S9391 | 1 | Sichtbare Konfigurationseinträge über Arrays.stream/filter/toList gebildet. Enumreihenfolge, leere Gruppen und Leser bleiben erhalten. |
| S4449 | 1 | HTML-Nichtnull-Vertrag unmittelbar vor MimeMessageHelper.setText explizit mit Assert.notNull geprüft. Reihenfolge nach Empfänger und Betreff sowie IllegalArgumentException erhalten. Leerer Text bleibt erlaubt, vor dem Versand wird keine Konfiguration für ungültige Argumente gelesen. Bestehende MIME-/Nullargumenttests erfolgreich. |

Keine Änderung von Layout, Icons, Theme, Rollenregeln, Transaktionsgrenzen, Admission, Login/Logout oder Datenformaten. Keine neuen Tests nur zur Abbildung der Implementierung ergänzt: Die bestehenden fachlichen Regressionen decken die berührten Verträge ab.

## Neun bewusst zurückgestellte Hinweise

- **S2143, DeviceRememberMeServices (1):** Spring Security 7.1.1 verlangt für PersistentRememberMeToken weiterhin java.util.Date. Diese notwendige Adaptergrenze bleibt; keine künstliche Zeitumstellung oder API-Änderung.
- **S1172, IsAuthenticatedBaseView (1):** hasRequiredAccess ist ein überschreibbarer Rechte-Hook. AdminView verwendet den Principal tatsächlich. Das Entfernen des Parameters würde die Architektur des Hooks und den geprüften Principal-Snapshot verändern.
- **S1068, zwei UI-Testfelder (2):** Die Layout-Fixtures werden bis zum Testende stark referenziert gehalten und im Cleanup freigegeben. Diese Lebensdauer bleibt gemäß Projektregeln erhalten; keine Feldentfernung nur für die Warnungszahl.
- **S127/S135, Avatar-Binärparser (3):** Chunk-/Markergrenzen und Abbruchbedingungen sollten zusammen mit dem Parser geprüft werden. Die bloße Umformung von for zu while beziehungsweise eine mechanische Änderung der Abbrüche bringt hier keinen fachlichen Nutzen. Keine neue Parserfunktionalität in diesem kleinen Paket.
- **S1141, SessionAdmissionService und AdminUserManagementSection (2):** Die verschachtelten Fehlergrenzen betreffen Admission/Rollback beziehungsweise Binderfehler. Keine reine Verschiebung in Hilfsmethoden zur Warnungsberuhigung. Ein gezieltes Folgepaket kann tatsächliche Verantwortlichkeiten prüfen.

Die neun Stellen wurden zunächst lokal zurückgestellt. Nach der nachfolgenden Benutzerfreigabe wurden vier davon auf dem Server akzeptiert; die übrigen fünf bleiben offen (siehe folgender Abschnitt).

## Nachfolgende freigegebene Sonar-Entscheidungen

Auf ausdrücklichen Benutzerauftrag am 20.10.2026 vier Findings einzeln als **Accepted** gespeichert, jeweils mit fachlicher Begründung und anschließendem Nachlesen von Status und Kommentar:

| Finding-ID | Stelle | Begründung |
| --- | --- | --- |
| 745e1826-ed23-489d-88c2-739f2d00fedd | DeviceRememberMeServices / S2143 | Notwendige Date-Adaptergrenze zum PersistentRememberMeToken-Konstruktor. |
| e5525b01-48fe-4847-b0a0-db159769f2b9 | IsAuthenticatedBaseView / S1172 | Überschreibbarer Rechte-Hook; AdminView verwendet den übergebenen Principal tatsächlich. |
| b457c49b-3211-4689-be2e-ef4fd9c12331 | AdminUserErrorNotificationTest / S1068 | Layout-Fixture bewusst bis Cleanup stark referenziert gehalten. |
| 8f4dedef-29f2-4cdb-b22d-287af50e5f5b | UserStatusNotificationTest / S1068 | Layout-Fixture bewusst bis Cleanup stark referenziert gehalten. |

Die fünf S127-/S135-/S1141-Hinweise im Parser und an den Fehlergrenzen bleiben für ein eigenes Codepaket offen. Sie wurden nicht pauschal akzeptiert. Die Sonar-Übersicht zeigte danach **75 offene Findings**. Diese Zahl liegt vor dem nächsten Scan der 17 Codebearbeitungen; deren tatsächliches Ergebnis ist weiterhin nicht behauptet. Kein weiterer Code geändert oder neuer Scan gestartet. CSV entsprechend aktualisiert.

## Tatsächlich ausgeführte Prüfungen

JDK 25.0.4.1 und Maven Wrapper 3.9.16 vor dem Build geprüft. Nach der Java-Änderungsrunde im Projektroot Spotless apply, Spotless check und Checkstyle ausgeführt, alle erfolgreich, null Checkstyle-Verstöße. Diff-/Whitespaceprüfung erfolgreich.

- **326/326 gezielte Tests**, null Failures, Errors oder Skips: Bildnormalisierung/EXIF und Draftwerte, kompakte Datumseingaben, Bootstrap- und Rollenpersistenz, MIME-/Mailverträge, Statussymbole, Konfigurationsgruppen, Admin-Mailvalidierung, Benutzerfilter/Paging/Dialogvalidierung sowie UI-/Sprach-/Navigationsfälle.
- **2.000/2.000 Anwendungstests** im vollständigen Produktions-verify, null Failures, Errors oder Skips. Surefire-XML unabhängig aufsummiert und mit der Maven-Zusammenfassung abgeglichen. Die gezielten 326 Fälle sind darin enthalten und werden nicht doppelt gezählt.
- Produktions-JAR erfolgreich gebaut und repackaged. Vaadins letzter Produktionsbuild schrieb die anfangs saubere package-lock.json automatisch um (unter anderem Workbox-Einträge). Diese für das Java-Paket sachfremde Build-Nebenwirkung wurde geprüft und ausschließlich diese eigene Lockdateiänderung bytegleich auf HEAD zurückgeführt; keine beabsichtigte Dependency-Änderung bleibt im Diff. Der erfolgreiche Build beschreibt den tatsächlich von Vaadin aufbereiteten Frontendstand, keine zusätzliche unabhängige npm-ci-Prüfung. Nach der letzten Java-Anpassung wurden Stilprüfungen und vollständiger Produktionslauf erneut erfolgreich ausgeführt. Finaler Lauf beendet am 20.10.2026 um 22:34:29 +02:00.
- Frische JaCoCo-Daten ausschließlich dieses vollständigen Laufs: **10.229/11.652 Zeilen = 87,79 %**, **2.975/3.862 Zweige = 77,03 %**, 219 analysierte Anwendungsklassen. Das ist JUnit-Coverage, keine Browsercoverage und keine Sonar-New-Code-Coverage.

Die Tests nutzen eigene isolierte Fixtures. Kein Browser-UI-Lauf, Dockerimage oder Containerstart, keine bestehende Datenbank verändert, keine realen E-Mails versendet. Kein eigenständiger neuer Browser-Helfer-/Frontendtestlauf. Vorhandene deutsche und englische Komponentenfälle sind enthalten.

## Einführung und nächste Schritte

Änderungen liegen uncommittet auf main. Keine Commits, Pushes oder Merges. Der Export bleibt unverändert. Zur Übernahme das neu gebaute Produktions-JAR verwenden und die Anwendung neu starten; Browser-Refresh allein übernimmt Java-Code nicht. Es ist keine sichtbare Designänderung beabsichtigt.

Nach dem Benutzer-Commit einen neuen Scan und Export abgleichen. Größere Komplexitäts- sowie Sessionserialisierungspakete bleiben separat zu planen. Keine pauschale Empfehlung, die restlichen Findings zu akzeptieren.

Technische Grundlagen: [AffineTransform-Konstruktoren in Java 25](https://docs.oracle.com/en/java/javase/25/docs/api/java.desktop/java/awt/geom/AffineTransform.html), [PersistentRememberMeToken in Spring Security 7.1.1](https://docs.spring.io/spring-security/reference/api/java/org/springframework/security/web/authentication/rememberme/PersistentRememberMeToken.html). Die konkreten Verträge wurden am Anwendungscode und den genannten Tests geprüft.
