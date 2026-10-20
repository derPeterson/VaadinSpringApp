# Komplexität und verschachtelte Fehlerbehandlung vom 20.10.2026

Ausgangspunkt: `main`, Commit `6a638c6` (`refactor: Paging und umfangreiche UI-Tests bereinigt`), zu Beginn sauberer Arbeitsstand. Der vorliegende Export enthält 54 offene Findings. In diesem Paket wurden seine **17 S3776 und zwei S1141**, insgesamt **19 Finding-IDs**, gezielt bearbeitet. Alle sind nachvollziehbare Strukturhinweise; keine neue fachliche Fehlerkorrektur oder Sicherheitslückenbehebung wird daraus abgeleitet.

[Zuordnung aller 19 IDs zu Exportzeile, Quelldatei und Maßnahme](sonar-komplexitaet-findings-2026-10-20.csv). Exportzeilen beziehen sich auf den Ausgangsstand. Der tatsächliche Entfall und mögliche Folgehinweise sind erst nach einem neuen Benutzer-Scan bekannt. Keine Suppressions, Regelabschaltungen oder Sonar-Statusänderungen.

## Fachliche Aufteilung

| Bereich | Hinweise | Umsetzung und erhaltene Grenzen |
| --- | ---: | --- |
| Sicherheit und Wartung | 8 | HTTP-Flow-Nachrichten, erlaubte Dokumente, Bootstrap-Pfad, ErrorHandler, Push-Rechte, Form-Login und Session-Recovery nach ihren Aufgaben gegliedert. Authentifizierungs-Fallbacks und Veröffentlichung des angemeldeten Benutzers getrennt. Sessionlimit-Lesen und Registrierung aus dem gesperrten Admission-Ablauf heraus benannt. |
| Benutzer- und Konfigurationsverwaltung | 3 | Privater Zustand je Benutzerformular; Aufbau, Binder, Passwortvalidierung, Fokus-/Blur-Rückmeldung, Rechteprüfung, Speichern und Sprachwechsel getrennt. Binder-Validierungsfehler werden in einem eigenen Schritt behandelt, weiterhin innerhalb der bestehenden äußeren Behandlung technischer Fehler. Dienstname, Queue-Aufbewahrung und Token-Dauern als benannte Validierungsregeln gegliedert. |
| Mailqueue | 2 | Privater Koordinatorzustand je Batch; Annahme, Abschluss und Fehlerbehandlung getrennt. Prüfung und Statuswechsel eines Claims in einem privaten Helfer innerhalb desselben TransactionTemplate-Aufrufs. |
| Avatar | 2 | Bildänderung und gesperrtes Speichern getrennt; Uploadempfang und Übernahme in den geöffneten Dialog getrennt. |
| Weitere UI-Abläufe | 4 | Profilregistrierung und Profilaktualisierung, Sitzungszeile und Aktionen, Inbox-Aktualisierung und offenes Detail sowie Anmeldung und Session-Recovery vom jeweiligen Aufbau getrennt. |

Geändert wurden **16 Produktionsdateien und drei Testdateien**. Keine Änderungen an öffentlichen Service-APIs, Abhängigkeiten, Datenformaten, Zeitzonen, i18n-Katalogen oder CSS. Die privaten Helfer und die beiden privaten Zustandsklassen sind keine neuen Spring-Komponenten oder Transaktions-Proxys.

## Erhaltene Verträge und Review

- **Admission:** Der HTTP-Session-Mutex und die Identitätssperre bleiben um denselben Ablauf. Geräteprüfung, Wiederverwendung einer bestehenden Reservierung, Wartungsprüfung, Limitprüfung, Fixation-Schutz, neue Registrierung und Completion behalten ihre Reihenfolge. Fehlerzuordnung und Rollback liegen weiter innerhalb der Identitätssperre. Rücknahme betrifft weiterhin nur die requesteigene neue Reservierung. Keine Self-Injection oder neue Transaktionsannotation.
- **Login und Rechte:** Reihenfolge der Security-Filter, CSRF-Prüfung und CSRF-Fehlerantwort, Login-/Logout-/Remember-me-Verträge und begrenzte Session-Recovery erhalten. Die Fallback-Reihenfolge bei der Benutzerermittlung bleibt HTTP-SecurityContext, gespeicherter HTTP-Benutzer, Thread-Kontext, dann Vaadin-Benutzer ausschließlich unter Vaadin-Session-Lock. Eine ungültige Identität wird weiterhin vor Veröffentlichung verworfen. Cleanup und unterdrückte Cleanup-Exceptions bleiben an der bisherigen Fehlergrenze.
- **Wartung und Push:** HTTP 503 und Forward für Dokumente bleiben von Vaadin-Protokollantworten getrennt. Gefälschte Protokollparameter geben keine fremden Servlets frei. Ein vorhandener ErrorHandler erhält nicht behandelte Fehler; pro Sitzung wird weiter nur einmal installiert. Vor abgewiesenen Push-RPCs werden die bestehenden Kontexte bereinigt; Dokumentanfragen werden nicht in die WebSocket-Behandlung gezogen.
- **Queue:** Höchstens ein Batch je Instanz und höchstens die konfigurierte Anzahl angenommener Aufgaben. Ablehnung stoppt Nachfüllen; bereits angenommene Aufgaben werden abgewartet. Interrupts werden vor jeder Annahme geprüft und nach Freigabe der Batch-Sperre wiederhergestellt. Shutdown und Cancellation behalten ihre vorhandenen Grenzen. SMTP bleibt außerhalb der Status-Transaktion. Ein unklarer Transportausgang bleibt IN_PROGRESS; keine automatische Wiederholung. Claim-Prüfung und Statusänderung committen zusammen; Ergebnislogs und gegebenenfalls Adminhinweis folgen wie bisher danach.
- **Avatar:** Normalisierung bleibt außerhalb der Kontosperre. Identitäts- und Versionsprüfung, Bild-/Modusprüfung, Flush und After-Commit-Ereignis bleiben innerhalb der bisherigen Schreibtransaktion. Streams werden geschlossen; alte Drafts und verspätete Antworten dürfen einen neu geöffneten Dialog nicht verändern.
- **Benutzerformular und UI:** Zustand gehört genau einem geöffneten Formular. Optimistische Version, Rollenauflösung, Selbstschutz und Schutz des letzten aktiven Superadmins bleiben erhalten. Technische E-Mail-Vorprüfungsfehler blockieren weiterhin das Speichern, auch wenn sie erstmals beim Speichern auftreten. Passwortfehlermeldungen und Übersetzungen bleiben dynamisch; Schließen/Detach entfernt die registrierten Refresher. Kein Layout- oder Iconwechsel.

## Regressionen

Vier neue ausgeführte Testausprägungen, ausschließlich mit isolierten Fixtures:

1. **DE und EN:** Gleichzeitig geöffnetes Bearbeitungs- und Neuanlageformular halten Passwortvalidierung, Fehleranzeige, Speichern und Schließen getrennt. Der Test prüft ausdrücklich zwei verschiedene Dialoginstanzen.
2. **Queue:** Nach einem abgelehnten Batch kann der nächste Lauf seine Kandidaten vollständig annehmen; abgelehnte Aufgaben werden nicht ausgeführt.
3. **Avatar:** Wird der Dialog vor Übernahme einer erfolgreichen Uploadantwort geschlossen und neu geöffnet, bleibt die neue Auswahl unverändert, der alte Upload wird abgewiesen und sein Eingabestream geschlossen.

Bestehende Assertions wurden nicht abgeschwächt. Der Dialog-Testhelfer wählt bei mehreren offenen Dialogen jetzt ausdrücklich den neu geöffneten Dialog statt erneut den ersten vorhandenen. Die übrigen vorhandenen Tests decken unter anderem parallele Admission-Zustände, Rollback, beide Loginwege, Remember-me, Wartung, Rechtewechsel, Queue-Unterbrechung/-Ablehnung/-Shutdown, SMTP-Ausgänge, Avatarversionen und DE-/EN-Komponentenverhalten ab.

## Tatsächlich ausgeführte Prüfungen

- JDK **25.0.4.1** und Maven Wrapper **3.9.16** geprüft, auch vor dem getrennten Abschlussbuild.
- Nach jeder Java-Runde im Projektroot **Spotless apply, Spotless check und Checkstyle** vor Tests ausgeführt; final erfolgreich, null Stilabweichungen.
- Zunächst **191/191 gezielte Sicherheitsprüfungen** erfolgreich. Nach Abschluss und Korrektur des neuen Testaufbaus **459/459 gezielte Fälle** in 13 Suites erfolgreich, null Failures, Errors oder Skips. Diese Teilmengen sind im vollständigen Lauf enthalten und werden nicht hinzuaddiert.
- Finaler vollständiger **`-Pproduction clean verify`: 2.128/2.128 Anwendungstests**, null Failures, Errors oder Skips. Surefire-XML separat aufsummiert: 108 Berichte. Gegenüber dem vorherigen grünen Stand vier zusätzliche Ausprägungen.
- Shared-Modul in der frischen Buildkopie zuerst mit **`-f shared/pom.xml clean install`** erfolgreich gebaut. Produktions-JAR gebaut und durch Spring Boot repackaged; eingebettetes `flow-build-info.json` bestätigt `productionMode: true`. Abschluss am 20.10.2026 um **23:25:15 +02:00**.
- Frische JaCoCo-Daten aus diesem vollständigen Clean-Lauf: **10.311/11.714 Zeilen = 88,02 %**, **3.006/3.874 Zweige = 77,59 %**, **222 analysierte Anwendungsklassen** laut JaCoCo. Dies ist JUnit-Coverage der Anwendung, keine Browser-, Shared- oder Sonar-New-Code-Coverage.
- Entwicklungsprojekt danach geprüft: alle fünf Workbox-Pakete auflösbar und Vite-Entwicklungskonfiguration erfolgreich geladen. Kein Entwicklungsserver gestartet und kein npm-Installationslauf erforderlich. Paketdateien und Konfigurationen im Arbeitsverzeichnis unverändert.
- Abschließender Scope-/Whitespace-Review erfolgreich. Keine Änderungen an vorhandenen Datenbanken, Containern oder fremden Prozessen; kein realer Mailversand.

### Zwischenfehler und getrennte Buildausführung

Ein früher Admin-Teillauf scheiterte beim Laden einer kompilierten Klasse. Ein weiterer Versuch brach vor den Tests ab, weil Vaadin eine generierte Importdatei nicht ersetzen konnte. Im Originalprojekt liefen inzwischen eine IntelliJ-Anwendung und der zugehörige Compiler. Diese Prozesse wurden weder beendet noch umkonfiguriert. Der frühere Klassenladefehler ist damit nicht abschließend einer Ursache zugeordnet.

Alle abschließenden Tests und der Produktionsbuild liefen deshalb in einer **getrennten Quellkopie** ohne vorhandene Build-Ausgaben oder Anwendungsdaten. 358 versionierte Build-Eingaben wurden übernommen, einschließlich des aktuellen geänderten Java-Stands; keine `.env`, Datenbanken, Logs oder vorhandenen node_modules. SHA-256-Abgleich nach dem finalen Lauf: **alle 358 Eingaben im ursprünglichen Arbeitsverzeichnis identisch zum geprüften Snapshot**. Das neue JAR liegt in dieser Prüfumgebung; das ursprüngliche Entwicklungs-JAR wurde nicht damit ersetzt.

Der erste vollständige Lauf dort führte 2.128 Tests aus: die bestehenden Tests bestanden, zwei neue DE-/EN-Ausprägungen scheiterten an der Dialogauswahl im neuen Testhelfer. Dieser wählte versehentlich den ersten bereits offenen Dialog. Der korrigierte Aufbau mit Prüfung verschiedener Instanzen bestand den gezielten und den abschließenden vollständigen Nachlauf. Keine Produktregel oder bestehende Assertion wurde dafür geändert.

## Offene Grenzen und Einführung

- Keine neue Browserabnahme, keine Browsercoverage und kein Dockerimage. Für dieses Paket lag keine gesonderte Browserfreigabe vor. Bestehende DE-/EN-Komponentenprüfungen sind enthalten, ersetzen aber keine visuelle Browserabnahme.
- Vollständige Vaadin-UI-/Sessionserialisierung, breite Konstruktor-/Architekturhinweise und andere Exportregeln bleiben außerhalb dieses Pakets. Die privaten Dialogcallbacks halten jetzt den zusammengehörigen Editorzustand; vollständige Serialisierung dieses bereits zuvor nicht entsprechend geprüften Formulargraphen wird nicht zugesichert. Sperr- und Fehlerverhalten ist durch die genannten Regressionen abgesichert, keine allgemeine Garantie für alle externen Transportstörungen.
- Keine Commits, Pushes, Merges, Sonar-Scans oder Statuswechsel durchgeführt. Reviewbare Änderungen liegen uncommittet auf `main`.
- Der Export enthält zusätzlich noch die zehn bereits im vorherigen Paging-/Testpaket bearbeiteten IDs. Sie werden hier nicht erneut als Leistung gezählt. Falls diese zehn und die jetzigen 19 nach dem nächsten Scan ohne Folgehinweise entfallen, wären **25 offene Findings** übrig; das ist ausschließlich die rechnerische Erwartung aus dem alten 54er-Export.
- Für die Übernahme **Java neu bauen und Anwendung neu starten**. Ein Browser-Refresh allein übernimmt neue Methoden und die Dialoggliederung nicht. Keine sichtbare Designänderung beabsichtigt; die laufende Anwendung wurde von dieser Prüfung nicht neu gestartet.
