# Browserabdeckung und offene Punkte

## Avatar-Parser vom 20.10.2026

Fünf vorhandene Parser-Findings gezielt bearbeitet; Sonar-Abgleich ausstehend.
70 neue öffentliche Bild-/Metadatenfälle; vor Überarbeitung 84/84 Charakterisierungen,
danach 104/104 gezielte Avatarfälle und final **2070/2070 Anwendungs-JUnits**,
keine Fehler oder Skips. Produktionsbuild, Spotless und Checkstyle erfolgreich.
Frische JUnit-Coverage: **87,91 % Zeilen / 77,50 % Zweige**, 220 Klassen.
AvatarImageProcessor: **94,84 % Zeilen / 78,48 % Zweige**. Keine neue Browserabnahme
oder Browsercoverage. Workbox-Imports und Entwicklungs-Vite-Konfiguration nach
Produktionsbuild erfolgreich nachgeprüft; Paketdateien unverändert.
[Änderungen, Finding-IDs, Nachweise und Grenzen](../docs/sonar-avatar-parser-2026-10-20.md).

## Kleines Sonar-Wartbarkeitspaket vom 20.10.2026

17 Findings in neun Java-Dateien gezielt bearbeitet; neun weitere kleine Hinweise
begründet zurückgestellt. **326/326 gezielte Tests und 2000/2000 Anwendungs-JUnits**,
keine Fehler oder Skips. Produktionsbuild, Spotless und Checkstyle erfolgreich.
Frische JUnit-Coverage: **87,79 % Zeilen / 77,03 % Zweige**, 219 Klassen.
Keine neue Browserabnahme, kein Dockerimage und kein erneuter Browser-Helferlauf;
vorhandene DE-/EN-Komponentenfälle sind enthalten. Kein neuer Sonar-Scan. Nach ausdrücklicher Folgefreigabe vier
Kontextfälle als Accepted mit Kommentar gespeichert und geprüft; fünf weitere
Zurückstellungen bleiben offen. Sonar danach: 75 offene Findings vor dem nächsten
Scan. Ausgangsexport: 79; tatsächliche Code-Reduktion nach dem Scan prüfen.
[Änderungen, Finding-IDs, Verträge und Grenzen](../docs/sonar-wartbarkeit-2026-10-20.md).

## Sonar-Abendpaket vom 20.10.2026

48 Findings einzeln geprüft: 31 mit Codeänderungen bearbeitet, 17 begründet
beibehalten; kein neuer Scan und keine Server-Statusänderungen. Unter den
Codeänderungen bleibt die Benutzerermittlung ein erforderliches Sonar-Review.
17 neue Testausprägungen; **591/591 gezielte Fälle und 2000/2000 Anwendungs-
JUnits**, keine Fehler oder Skips. Produktionsbuild, Spotless und Checkstyle
am finalen Stand erfolgreich. Frische JUnit-Coverage: **87,79 % Zeilen /
77,06 % Zweige**, 219 Klassen. Sechs neue Prüfungen schlugen zuvor reproduzierbar
am unveränderten Produktionscode fehl. Keine neue Browserabnahme, kein
Dockerimage und kein erneuter Browser-Helferlauf; vorhandene DE-/EN-Komponentenfälle
sind enthalten. Der neue Einzel-Metadaten-/Repository-Proxytest verwendet eigene H2-Daten.
[Änderungen, 48 Finding-IDs, Nachweise und Grenzen](../docs/sonar-abendpaket-2026-10-20.md).

## Kosmetische Sonar-Bereinigung auf main vom 20.10.2026

275 exportierte IDs in 84 Java-Dateien gezielt bearbeitet; die tatsächliche
Sonar-Reduktion ist erst nach dem nächsten Scan bekannt. **1983/1983 Anwendungs-
JUnits**, keine Fehler oder Skips; Produktionsbuild, Spotless und Checkstyle
am finalen Stand erfolgreich. Frische JUnit-Coverage: **87,73 % Zeilen /
76,91 % Zweige**, 219 Klassen. Keine neue Browserabnahme, kein Dockerimage,
kein erneuter Browser-Helferlauf. Bestehende DE-/EN-Komponententests enthalten.
Die vier früher offenen Kontextentscheidungen wurden inzwischen ausdrücklich
freigegeben und im Sonar-Server umgesetzt (2 False Positive, 2 Accepted);
das Gate des Ausgangsstands war danach grün. Kein neuer Scan dieses Pakets.
[Änderungen, Finding-IDs, Nachweise und Grenzen](../docs/sonar-kosmetik-2026-10-20.md).

## Sieben Sonar-Codehinweise auf main vom 20.10.2026

Eine H2-Nullabsicherung und sechs kleine Wartbarkeits-/Stilhinweise sind umgesetzt.
174 gezielte H2-/CSRF-/Flow-/Komponentenfälle sowie **1978/1978 Anwendungs-JUnits
und 48/48 Helfer** erfolgreich, ohne Fehler/Skips. Produktionsbuild, Spotless und
Checkstyle erfolgreich. Frische JUnit-Coverage: **87,72 % Zeilen / 76,88 % Zweige**,
219 analysierte Klassen. Keine neue Browserabnahme oder Dockerimage. Vier
Kontextmeldungen und die erneute Sonar-/Quality-Gate-Bewertung bleiben offen.
[Änderungen, Nachweise und Einführungsgrenzen](../docs/sonar-nachscan-korrekturen-2026-10-20.md).

## Mail-Worker-Unterbrechung vom 20.10.2026

Der Koordinator stoppt nach erkanntem Interrupt vor weiterer Aufgabenannahme,
wartet bereits angenommene Arbeit ab und stellt sein Interruptflag wieder her.
Sechs gesteuerte Regressionen sind am unveränderten Worker fehlgeschlagen und
nach der Korrektur erfolgreich. **1977/1977 Anwendungs-JUnits und 48/48 Helfer**,
ohne Fehler/Skips; Produktionsbuild, Spotless und Checkstyle erfolgreich.
Frische JUnit-Coverage: **87,71 % Zeilen / 76,86 % Zweige**, 219 analysierte Klassen.
Keine neue Browserabnahme oder Dockerimage. Die folgenden Browsernachweise gelten
weiterhin für ihre ausdrücklich genannten historischen Stände.
[Änderungen, Reproduktion und Grenzen](../docs/email-worker-interrupt-2026-10-20.md).

## P2-TIME: Aktivitätszeitpunkte vom 20.10.2026

Aktivitätszeiten sind auf `Instant` umgestellt, Anzeige und Kalendereingabe auf
`Europe/Berlin` festgelegt. Dauer, Statusgrenzen, Filter und Sortierung verwenden
eindeutige Zeitpunkte. 15 neue Testausprägungen sichern beide Zeitwechsel,
verschiedene Clock-/H2-Sitzungszeitzonen und DE-/EN-Komponentenverhalten ab.
**1971/1971 Anwendungs-JUnits und 48/48 Helfer**, ohne Fehler/Skips;
Produktionsbuild, Spotless und Checkstyle erfolgreich.
Frische JUnit-Coverage: **87,71 % Zeilen / 76,85 % Zweige**, 219 analysierte Klassen.
Keine neue Browser-UI-Runde oder Dockerimage für dieses Paket. Die folgenden
Browsernachweise bleiben historische Abnahmen ihrer jeweils genannten Stände.
Einführung mit frischem Datenbankschema gemäß vereinbartem Entwicklungs-Neuaufbau;
keine Bestandsdaten durch diese Arbeit verändert.
[Änderungen, Einführung und Prüfgrenzen](../docs/sonar-time-umsetzung-2026-10-20.md).

## H2-Ergänzung nach der CSRF-Browserabnahme (20.10.2026)

Die optionale lokale H2-Konsole ist mit eng begrenztem Ursprungsschutz wieder
angebunden; die Spring-Tokenprüfung der Anwendung bleibt aktiv. 1956 Anwendungs-
JUnits und 48 Helfer sowie Produktionsbuild und Stilprüfungen erfolgreich.
44 zusätzliche H2-Testausprägungen, darunter neun echte HTTP-Fälle mit nativem
H2-Servlet, vollständiger SecurityFilterChain und eigener flüchtiger Datenbank.
Frische JUnit-Coverage: 87,59 % Zeilen / 76,66 % Zweige bei 218 Klassen.
Kein neuer Browser-UI-Lauf oder Dockerimage-Build für diesen Nachtrag.
Die folgenden 44/44-Runden je Sprache dokumentieren den Stand vor der H2-Ergänzung.
[Änderungen, Bedienung und Prüfgrenzen](../docs/h2-console-csrf-2026-10-20.md).

## CSRF-Nachprüfung vom 20.10.2026 – gezielte Abnahme abgeschlossen

Auf `fix/sonar-csrf` ist Spring-CSRF für klassische HTTP-Mutationen aktiv; echte
Flow-Transporte behalten ihren eigenen Schutz. 1912 Anwendungs-JUnits und 48
Browser-Helfertests erfolgreich; 51 neue Testausprägungen, Produktionsbuild und
Stilprüfungen grün. Ein instabiles Wartungsansichts-Testfixture wurde durch eine
stark gehaltene VaadinSession korrigiert; keine Produktivänderung dafür.

Nach ausdrücklicher Freigabe DE und EN jeweils **44/44 Browserfälle**, keine
Fehler/Skips: `session-limits` 35 plus `maintenance` 9. Die Anmeldefälle sind bereits
in `session-limits` enthalten. Alle Runden am identischen neuen Produktionsimage,
Coverage und eigener Testumgebungsabbau erfolgreich. Ein durch das Dockerupdate
unterbrochener Lauf wurde vollständig wiederholt und zählt nicht als Abnahme.

Frische JUnit-Coverage: 87,5 % Zeilen / 76,2 % Zweige. Gemeinsame Browser-Coverage
der vier gezielten Runden: **68,41 % / 46,51 %**, jeweils 216 Anwendungsklassen.
Die Browserquote ist wegen der kleineren Fallauswahl nicht mit einer vollständigen
152er-Runde gleichzusetzen. Die übrigen Browserfälle wurden hier nicht wiederholt.
Die beim Abschluss offene H2-Kompatibilität wurde im oben verlinkten Nachtrag
gezielt bearbeitet; keine pauschale Ausnahme. Kein neuer Sonar-Scan oder Sonar-Statuswechsel.
[Änderungen, Nachweise und Grenzen](../docs/sonar-csrf-umsetzung-2026-10-20.md),
[Meilenstein](../docs/meilensteine/2026-10-20-csrf-browserabnahme.md).

## Aktuelle Gesamt-QA vom 20.10.2026 – abgeschlossen

Der vollständige Auftrag umfasst das fertige Header-Overlay, persönliche Inbox und
Versandarchiv. Fünf neue Browserausprägungen ergänzen `all` auf 152 Fälle.
1823 Anwendungs-JUnits, 48 Helfer, 205 Python- und 26 JavaScript-Offlinetests sind
erfolgreich. DE und EN: jeweils alle 152 eindeutigen Fälle nach vollständigen Runden
und gezielten Nachtests erfolgreich, ohne Skips, am identischen Produktionsimage.
JUnit: 87,34 % Zeilen / 75,62 % Zweige; Browser DE/EN zusammen: 85,50 % / 66,06 %.
Die nachfolgenden älteren Abschnitte dokumentieren frühere Zwischenstände; die dort
noch offene Browserabnahme des fertigen Redesigns ist mit dieser QA abgeschlossen.
[Nachweis, Korrekturen und Messgrenzen](../docs/meilensteine/2026-10-20-gesamtqa-und-benachrichtigungsredesign.md).

## Anschließende Logging-Nachprüfung vom 20.10.2026

Nach vollständigem QA-Abschluss wurde die gezielte Logging-Überarbeitung umgesetzt.
Alle 1843 Anwendungs-JUnits erfolgreich; neues Produktionsimage gebaut und
`notifications` DE/EN je 8/8 erfolgreich am identischen neuen Image, keine Fehler/Skips.
Die Runtime-Logs zeigen keine geprüften Context-/Principal-/Credentials-Dumps oder
Testzugangsdaten. JUnit jetzt 87,45 % Zeilen / 75,69 % Zweige bei 214 Klassen.
Die 152er-Gesamtrunden und ihre Browserquote oben bleiben der Nachweis vor Logging;
die anschließenden acht Fälle je Sprache sind gezielte Nachtests.
[Logging-Änderungen, tatsächliche Nachweise und Grenzen](../docs/meilensteine/2026-10-20-gezielte-logging-ueberarbeitung.md).

## Nachfolgende Inbox-Werkzeugleiste (19.10.2026)

Suche links, gemeinsame Segmentauswahl Alle/Gelesen/Ungelesen mittig und zwei
Iconbuttons mit lokalisierten Tooltips rechts. Erklärungstext unter der Leiste entfernt;
Gelesen-Suche wird vor Pagination mit Eigentümer-/Aufbewahrungsregeln gefiltert.
198 gezielte JUnits erfolgreich, keine Fehler/Skips (97 UI-Komponenten, 68 H2,
13 Sprache/Katalog, 20 Header). Fünf neue Ausprägungen; keine neue Browser- oder
Coverage-Messung. Zentrierung, Umbruch und Fokus/Tooltips der neuen Leiste in
Desktop/schmalen DE/EN-Ansichten bleiben im Browser ungeprüft.

## Nachfolgender persönlicher Inbox-Entwurf (19.10.2026)

Persönliche Liste mit Archive-Panelkopf, Gesamt-/Ungelesen-Zählern, Suche,
Nachrichtenvorschauen, Einzel-Leseaktion und nummerierter Pagination umgesetzt.
Einfacher Kartenklick öffnet den Detaildialog ohne automatische Lesemarkierung.
**Kein Browserlauf für diesen neuen Stand**: Zentrierung, Desktop/schmale Ansichten,
Umbrüche und native Fokus-/Klickgeometrie bleiben offen. Historische Browserwerte
unten sind keine Abnahme des neuen Inbox-Entwurfs. Der bestehende Browserhelfer
NOTIFY-02 bleibt auf Titelöffnung und ausdrücklich gelesen markieren ausgerichtet.
Gezielte technische Nachweise: 93 Komponenten-, 65 H2- und 13 Sprach-/Katalogtests
(171 insgesamt) sowie 48 Browser-Helfer-/Reportertests erfolgreich, keine Fehler/Skips.
Nachfolgender Pagination-Nachtest: 95 Komponentenprüfungen erfolgreich, keine
Fehler/Skips; Größen 10/20/50, Seitenreset, Such-/Ungelesen-Erhalt, Trefferzahl und
Sprachwechsel. Der Aufbewahrungshinweis steht darunter mittig. Keine neue Browser-
oder Coverage-Messung; die Ausrichtung ist weiterhin nicht im Browser abgenommen.
Diese Ergebnisse ersetzen keine Browserabnahme.
Vertrag und aktuelle gezielte Ergebnisse: [Benachrichtigungen](../docs/user-notifications.md#persönlicher-posteingang-panelentwurf).

## Nachfolgender Header-Filter (19.10.2026)

Die Header-Vorschau verwendet nun eine native segmentierte Auswahl „Alle / Gelesen /
Ungelesen“ wie der Profilbilddialog. Der neue Gelesen-Filter wird vor Pagination in
der Datenbank angewendet; maximal fünf passende Einträge bleiben die Vorschaugrenze.
DE/EN, Zustandserhalt beim Sprachwechsel, Eigentümertrennung und Aufbewahrung werden
gezielt in Komponenten-/H2-Tests geprüft: 85 Komponenten-, 59 H2- und 13 Sprach-/
Katalogprüfungen erfolgreich (157 insgesamt, keine Fehler/Skips). Darstellung, native Tastaturbedienung und
schmale Browseransichten dieses neuen Filters sind noch nicht im Browser geprüft.
Die nachfolgend historischen Browsernachweise gelten nicht als Abnahme dieses Filters.

## Nachfolgender Benachrichtigungs-Detaildialog (19.10.2026)

Detaildialog mit Paperplane, Herkunft/Datum, Lesestatus-Badge und expliziter
Leseaktion überarbeitet. Öffnen allein verändert den Status nicht mehr. Der
Browserhelfer NOTIFY-02 wurde auf diese Anforderung angepasst; er prüft vor dem
Markieren ausdrücklich „Ungelesen“ und danach „Gelesen“. **Noch kein Browserlauf
für diesen neuen Stand**; die nächtlichen Browser-/Coveragewerte unten gehören
zum früheren Stand und belegen weder das neue Layout noch die neue Lesejourney.
Gezielte aktuelle JUnit-Ergebnisse stehen im [Benachrichtigungsvertrag](../docs/user-notifications.md#detaildialog-entwurf-vom-19102026).

## Nächtliche Gesamt-QA mit Bereichsausnahmen (19.10.2026)

Ausgangspunkt: sauberer Feature-Branch, Commit `3fc09c1`. Produktionsbuild erfolgreich;
alle Browserrunden prüfen dasselbe feste Produktionsimage. Die neuen Browserfälle
stehen im [Fallkatalog](README.md#neue-benachrichtigungs--und-rollenfälle).

| Prüfung | Ergebnis |
| --- | --- |
| Anwendungs-JUnit | 1706/1706, keine Fehler/Skips; fünf ausdrücklich getaggte UI-Ausprägungen nicht ausgewählt |
| Browser-Helper/Reporter | 48/48, keine Fehler/Skips |
| Python offline | 205/205, keine Fehler/Skips |
| JavaScript offline | 26/26 (14 Workflow, 12 Profilbild/Zeit), keine Fehler/Skips |
| Browser DE, erster Gesamtlauf | 146/147, ein Kalender-Synchronisierungsfehler, keine Skips |
| Browser DE, gezielter Nachtest | 10/10, keine Fehler/Skips; alle 147 eindeutigen Katalogfälle danach erfolgreich, kein offener Fehlfall |
| Browser EN, vollständiger finaler Teststand | 147/147, keine Fehler/Skips |

Die erste gesonderte 13-Fall-Vorprüfung hatte sieben Erfolge und sechs rote
Testhelfer-/Fixturebefunde. Diese wurden korrigiert, die rote Runde bleibt erhalten.
Der spätere Kalenderfehler wurde durch Warten auf eine stabile sichtbare Zelle behoben;
Auswahl, Wiederöffnen und echte Registrierung bleiben unverändert gefordert.
Keine Produktionscodeänderung oder abgeschwächte Sicherheitsassertion erforderlich.

**Ausgenommen:** gesendete Historie samt Details/Pagination und das Header-
Benachrichtigungs-Overlay samt Einträgen/Aktionen. Keine Browserfälle dieser
Oberflächen pauschal entfernt; fünf konkrete JUnit-Ausprägungen optional getaggt,
keine dauerhafte Deaktivierung. Backend/Persistenz/Berechtigungen, persönliche Inbox,
Headerzähler und Empfängerdetails der **geprüften Verfassen-Vorschau** bleiben im Umfang.

Neue sichtbare Nachweise: Verfassen/Desktop/schmal, Fehler erst bei Prüfung,
allgemeiner Vorschauhinweis und genaue Feldfehler, native Empfängerauswahl/Zähler,
Vorschau und Freigabe nach Änderungen, eigene persistente Inboxen/Lesestatus,
Live-Rollenwechsel in zwei Benutzerkontexten ohne neuen Login sowie die Sperre beider
Versandpanels bei Wartungswechsel im anderen Tab. `all`: 147 Fälle/27 Klassen;
`fast` unverändert 34 Fälle. Konkrete Schritte und Grenzen stehen im Fallkatalog.

| JaCoCo-Messung | Zeilen | Zweige |
| --- | --- | --- |
| Vollständiger freigegebener JUnit-Umfang | 8978/10450 (85,91 %) | 2566/3474 (73,86 %) |
| Browser DE-Gesamtlauf, separat | 8949/10450 (85,64 %) | 2248/3474 (64,71 %) |
| Browser EN-Gesamtlauf, separat | 8958/10450 (85,72 %) | 2250/3474 (64,77 %) |
| Browser DE + deutscher Nachtest + EN, Rohdaten vereinigt | 8960/10450 (85,74 %) | 2254/3474 (64,88 %) |

209 ausführbare Anwendungsklassen, ohne Shared-JAR, Tests und Bibliotheken.
Browserwerte enthalten Start/Hintergrundaufgaben und stammen aus dem exakt getesteten
Produktions-JAR; vor dem Merge wurden alle drei JAR-Prüfsummen abgeglichen.
Prozentwerte wurden nicht gemittelt. Keine CSS-/JavaScript-Coverage und keine behauptete
vollständige Abdeckung aller denkbaren fachlichen Kombinationen. Die zwei UI-Ausnahmen,
Realgeräte, Screenreader und reale Mailclients sind damit nicht geprüft.

Eigene Umgebungen abgebaut, 355 relevante Quelldateien identisch, isolierte Buildkopie
entfernt, Formatierung und Diffprüfung erfolgreich. Änderungen uncommittet und auf
Tests/Dokumentation beschränkt; kein App-Neustart erforderlich.
[Eigenständig lesbarer Meilenstein](../docs/meilensteine/2026-10-19-benachrichtigungen-rollen-und-gesamtqa.md).

## Benachrichtigungen: aktueller Schnittstellenvertrag (18.10.2026)

Einheitlich `UserNotification`, `/notifications`, `/admin?section=notifications` und
`app.user-notifications.retention-days`; Datenmodell und SQL-Namen folgen demselben Vertrag.
**119 gezielte Anwendungs-JUnits und 47 Helfertests**, 0 Fehler/Fehlschläge/Skips.
Aufbewahrung, Adminnavigation, Rollen-UI-Guard, HTTP-Sicherheitskette, Persistenz und
Java-Komponenten geprüft. Shared-Build, vollständiger App-/Testcompile,
Browserquellencompile und Spotless erfolgreich. Frische isolierte Buildkopie mit
SHA-256-Abgleich der 352 relevanten Quelldateien zum Arbeitsbaum.
Kein Browserstart/Produktionsbuild, kein erneuter vollständiger JUnit-Lauf und keine neue
Gesamt-Coverage-Messung. [Aktueller Vertrag](../docs/user-notifications.md).


## Rollen ohne Neuanmeldung und Bootstrap-Listen (18.10.2026)

Ausgangscommit `5d5ddc5`. Der letzte vollständige
Anwendungs-JUnit-Lauf enthält **1667/1667** erfolgreiche Fälle, 0 Fehler/Fehlschläge/Skips.
54 neue Fälle plus angepasste bestehende Sicherheitsregressionen. Dazu **47/47 Browser-Helfer**
erfolgreich, ebenfalls ohne Fehler oder Skips. Browserquellen kompiliert; **kein Browser gestartet**.

Frische JaCoCo-Daten des vollständigen Laufs: **8643/10120 (85,41 %) Zeilen**,
**2486/3387 (73,40 %) Zweige**, 209 ausführbare Anwendungs-Java-Klassen.
Shared, Tests und Bibliotheken sind nicht im Nenner; keine CSS-/JS-/Browser-Coverage.
Shared-Build, App-/Testcompile und Spotless erfolgreich. Produktionsbuild steht aus.

Echte isolierte H2-/HTTP-/Push-Prüfungen sichern frische Rollen, erhaltene Sitzungen,
gestoppte alte RPCs, weiter wirksame Sperre/Löschung/Widerruf, Superadmin-Vergabeschutz,
Rollenmitteilungen/Rollback, atomare Bootstrap-Listen und Erweiterung des alten H2-Mitteilungs-
Enums. Die ursprünglichen Unit-Fixtures für „anonym“ und Kontowechsel wurden auf tatsächlich
getrennte HTTP-Sitzungen korrigiert; die ursprünglichen Sicherheitsassertionen sind erhalten.

Der Browser-Compose-Override wurde auf getrennte erfundene Admin-/Superadmin-Listen
umgestellt. Die Superadmin-Vergabe in USER-07F und den Wartungsrollen-Fixtures verwendet
nun den dafür berechtigten Superadmin. **Diese Browserfälle wurden hier nicht ausgeführt**;
frühere Browserzahlen gelten ausschließlich für ihre früheren Stände.

[Konfiguration, Verhalten, Einführung und offene Browserprüfungen](../docs/role-updates-and-bootstrap.md).
Vollständiger Neustart erforderlich; bestehende lokale Bootstrap-Einstellungen bei Bedarf
in die neuen Listen übertragen. Die echte `.env` wurde nicht gelesen/geändert.

Der Mindestbestand schützt jetzt ausschließlich aktive Superadmins: Löschen,
Deaktivieren und Entziehen der Superadmin-Rolle sind beim letzten aktiven Konto blockiert,
auch wenn normale Admins verbleiben. Normale Admins haben keinen Mindestbestand.
14 zusätzliche JUnits sowie angepasste Parallelitäts-/Batch-/Dirty-Entity-/DE/EN-Fehlerfälle
sichern diese Erweiterung. Die Java-Prüfung deckt keine sichtbare Browserabnahme ab.

## Mitteilungsfunktion: Java-Nachweis, Browserabnahme offen (18.10.2026)

Ausgangspunkt `7dedccc`. Persönliche Inbox,
Superadmin-Versand und automatische Sicherheitseinträge sind umgesetzt.
Der neue Stand wurde **nicht im Browser ausgeführt**. Die nachfolgenden historischen
Browserzahlen gelten weiterhin nur für die dort benannten Stände.

| Messung | Tatsächliches Ergebnis |
| --- | --- |
| Anwendungs-JUnit gesamt | 1613/1613, 0 Fehler, 0 Fehlschläge, 0 Skips; 58 zusätzliche Fälle |
| Neue eigene Mitteilungstests | 38 Persistenzfälle und 15 Java-Komponentenfälle, ergänzt um bestehende Sicherheits-/Routingtests |
| Java-Build / Shared | Java 25.0.4.1; Shared gebaut/installiert, App und Tests kompiliert |
| JaCoCo gesamt | Zeilen 8410/9871 (85,20 %); Zweige 2354/3225 (72,99 %); 197 ausführbare Anwendungs-Java-Klassen |
| Neue Mitteilungsklassen | Zeilen 540/654 (82,57 %); Zweige 126/187 (67,38 %), inklusive innerer Klassen |
| Katalog / Format | 482 eindeutige DE-/EN-Schlüssel, Codec erhalten; Spotless Apply/Check erfolgreich |
| Browser / Produktion | Nicht gestartet; keine neue Browser-, CSS- oder JavaScript-Coverage |

Frische JaCoCo-Ausführungsdaten ausschließlich aus dem vollständigen JUnit-Lauf dieses
Stands; Shared, Tests und Bibliotheken sind nicht im Nenner. Gemessene Java-Komponenten-
aufrufe belegen weder sichtbare Geometrie noch echtes Browser-Push-/Fokusverhalten.
Spring-Header-Anbindung und HTTP-Zugang zur neuen Route sind in Java geprüft.

Persistenznachweise umfassen konkurrierende Wiederholung, Rollbacks bei Pflichtmitteilungs-
fehlern, Rollen-/Sitzungswiderruf, fremde Empfangs-IDs, aktuelle Empfängervorschau trotz
altem Persistence Context, Snapshots, Lösch-FKs und Retention. JUnit verwendet ausschließlich
isolierte H2-/Mock-Daten; keine Bestandsdatenbank oder reguläre Anwendung gestartet.

[Funktionsvertrag und geplante NOTIF-01 bis NOTIF-07](../docs/user-notifications.md#noch-auszuführende-browser-betriebsprüfungen)
benennen die noch ausstehenden DE-/EN-Journeys, visuelle Prüfung und echte Neustart-/Schemafixture.
Es wurden keine neuen Browserfälle als erfolgreich markiert. Einführung erfordert Shared-
und Anwendungsbuild sowie vollständigen Neustart; HotSwap allein reicht nicht.

## Mail-Workerpool implementiert und mit JUnit geprüft (18.10.2026)

Geprüfter Stand: `main`, Basis `6b7952b`, plus uncommittierte Workerpool-Änderungen.
Keine Browserfreigabe für dieses Paket; kein Browser-/Frontend-/Produktionslauf gestartet.
Vertrag und Einführung: [Mail-Workerpool](../docs/email-worker-pool.md).

| Prüfung | Tatsächliches Ergebnis |
| --- | --- |
| Java / Maven | Java 25.0.4.1; Maven Wrapper 3.9.16 |
| Abschließende vollständige Anwendungs-JUnit-Suite | 1555/1555; 0 Fehler, 0 Fehlschläge, 0 Skips |
| Neue Workerfälle innerhalb dieser Suite | 26: 12 Pool-, 6 Konfigurations-/Spring- und 8 Integrationsfälle |
| Vorangegangene gezielte Queue-/Workerregression | 166/166; anschließend Kontextvererbungsschutz ergänzt und im abschließenden Gesamtlauf geprüft |
| JaCoCo Zeilen | 7823/9173 (85,28 %) |
| JaCoCo Zweige | 2193/2997 (73,17 %) |
| Workerpool allein | 62/64 Zeilen (96,88 %), 29/32 Zweige (90,63 %) |
| Format / Katalog / Diff | Spotless Apply/Check erfolgreich; weiterhin 447 eindeutige DE-/EN-Schlüssel, Codec erhalten; Diff geprüft |

Frische getrennte JUnit-Ausführungsdaten dieses Stands, 177 ausführbare
Anwendungs-Java-Klassen im Messumfang. Kein Shared-/Test-/Bibliothekscode im Nenner;
keine Vermischung mit früheren Messungen und keine neue Browser-, CSS- oder JS-Coverage.
Die nachfolgenden Browserergebnisse gehören weiterhin zu ihren ausdrücklich genannten
früheren Ständen. Ein Compile-/JUnit-Erfolg ist kein Produktions- oder Browsertest.

Belegt sind N=1/2/3 mit Latches, parallele SMTP-Verbindungen über den echten Sender,
Nachfüllung bei langsamer erster Aufgabe, Batchkapazität und Nichtfälligkeit,
übersprungene überlappende Läufe, eindeutige Claims zweier unabhängiger Pools an
isoliertem H2, Claim-/Ergebnis-Commitfehler, getrennte SENT/FAILED/IN_PROGRESS-Ausgänge,
Adminaudit sowie Ablehnung, Graceful-/Force-Shutdown und Koordinatorunterbrechung.
Benannter Executor und Close-Lebenszyklus sind im eigenen Spring-Kontext geprüft;
Worker erben keine InheritableThreadLocals des Aufrufers.

Die erste gezielte Runde deckte Fehler neuer Testfixtures auf (unnötig striktes
Coordinator-Stubbing in reinen Enqueuefällen, fehlender H2-Flush vor Wiederanlage und
SMTP-CRLF im empfangenen Body). Fixtures korrigiert, bestehende Assertions erhalten.
Die abschließenden oben genannten Läufe sind erfolgreich.

Grenzen: keine realen Mailanbieter, keine TLS-/Provider- oder verteilte JVM-Prüfung,
kein nachgestellter Prozessabsturz und kein Echtzeit-/Mails-pro-Sekunde-Versprechen.
SMTP und Ergebnis-Commit bleiben nicht atomar. Ein Interrupt beendet blockierende
Fremdbibliotheken nicht garantiert; unklare Claims werden nicht automatisch freigegeben.
Nur eigene In-Memory-Daten und Loopback-SMTP verwendet; Fixture-Sockets und Worker abgebaut.
Keine reale Datenbank, .env, Postfächer oder Nutzerprozesse angefasst.

Zum Übernehmen Anwendung mit Shared-Abhängigkeit bauen und vollständig neu starten;
HotSwap allein ersetzt den neuen Executor-/Bean-Lebenszyklus nicht.

## Gezielte Nachprüfung der Konfigurationsbereinigung (18.10.2026)

Nach der vollständigen Gesamt-QA wurde ein bisher wirkungsloses SMTP-Feld samt
Eingaberegeln, Defaults, Übersetzungen und Umsetzungsplan entfernt. Der aktuelle
Katalog enthält 28 Konfigurationsfelder und 447 Übersetzungsschlüssel je Sprache.
Ein gezielter Startschritt entfernt alle zentral in `RetiredConfigEntry` aufgeführten
Datenbankwerte; aktuelle Werte und sonstige unbekannte Einträge bleiben erhalten.

- Java 25.0.4.1; Shared neu gebaut und installiert.
- 113 gezielte JUnit-Fälle erfolgreich, 0 Fehler, 0 Fehlschläge, 0 Skips:
  Konfigurationsservice einschließlich isolierter H2-Transaktionen, wiederholbare
  Altwertbereinigung, Startreihenfolge, aktuelle SMTP-Felder, Mail-Sender und DE/EN-Kataloge.
  Der Enumvertrag prüft eindeutige, nicht leere Schlüssel ohne Überschneidung mit aktiven Einträgen.
- Fünf neue Regressionstests sind in diesen 113 Fällen enthalten.
- CONFIG-05D und Fallbeschreibung auf 28 Felder angepasst; Browserquellen erfolgreich
  kompiliert, keine Browserläufe gestartet. Die folgenden Gesamtergebnisse beziehen
  sich weiterhin auf den damaligen Stand vor dieser nachträglichen Bereinigung.
- Spotless Apply/Check und Diff-Prüfung erfolgreich; Properties-Codec unverändert.
  Kein neuer Produktions-/Frontendbuild und keine neue Coverage-Messung.

Sichtbar nach Neubau von Shared und Anwendung, vollständigem Anwendungsneustart
und erneutem Öffnen der Konfiguration. Reale Datenbanken wurden nicht angesprochen.

## Bereinigung, Profilbilder und Eingaben vollständig geprüft (18.10.2026)

Ausgangspunkt: sauberer Branch `main`, Commit `85712fee0ba1246784f815a185075db0525ad6f7`.
Die autorisierten Bereinigungen/Korrekturen bleiben uncommittet. Keine Projekt-Commits,
Stage-, Merge- oder Push-Aktionen. [Bereinigungsnachweis](../docs/codebereinigung-2026-10-18.md),
[QA-Meilenstein](../docs/meilensteine/2026-10-18-bereinigung-avatar-eingaben-und-gesamtqa.md).

| Prüfung des abschließenden Stands | Ergebnis |
| --- | --- |
| Java / Shared / Produktionsbuild | Java 25.0.4.1, Maven Wrapper 3.9.16; Shared und isolierter Docker-Produktionsbuild erfolgreich |
| Vollständige Anwendungs-JUnit-Suite | 1524/1524; 0 Fehler, 0 Fehlschläge, 0 Skips |
| Browserhelfer / Reporter / Workerplanung | 47/47; 0 Fehler, 0 Fehlschläge, 0 Skips |
| Python offline | 205/205; keine Fehler/Skips oder Liveprovider-Anfragen |
| JavaScript offline | 26/26; 8 Avatar-, 4 Zeit- und 14 Workflowfälle; keine Fehler/Skips |
| Browser `all` DE / EN | Je 141/141, 26 Klassen; 0 Fehler/Skips, identische 141 Fall-/Parameteridentitäten |
| Worker je Sprache | 59/59 und 82/82; je eigene JVM, H2, Docker und Mailpit |
| Fast-Auswahl | Alle 34 markierten Fälle in beiden Gesamtrunden enthalten; kein zusätzlicher Fast-Lauf |
| Aufbau / Abbau / HTML / Coverage | Beide abschließenden Sprachläufe vollständig erfolgreich |
| Format / Review | Spotless Apply/Check und Diff-Prüfung; keine Python-Datei geändert, daher kein Ruff-Formatlauf erforderlich |

DE endete um 06:58:28, EN um 07:21:31 Europe/Berlin. Beide verwenden exakt das
gepinnte Produktionsimage `sha256:13d46c5afc56f276746f9431b2047115041c800c147546fcae4ee98dd872818f`.
Die abschließenden Runden prüfen den bereinigten Stand nach allen Produktionskorrekturen.
Frühere Runs oder Vorprüfungen wurden weder in die Erfolgszahlen noch in die folgenden
Coverage-Daten hineingemischt.

| JaCoCo-Messung | Zeilen | Zweige |
| --- | ---: | ---: |
| Vollständige Anwendungs-JUnit-Suite | 7735/9113 (84,88 %) | 2165/2987 (72,48 %) |
| Browser `all` DE | 7769/9113 (85,25 %) | 1910/2987 (63,94 %) |
| Browser `all` EN | 7780/9113 (85,37 %) | 1914/2987 (64,08 %) |

Gemessen werden jeweils 174 ausführbare Anwendungs-Java-Klassen (210 Klassen im XML
inklusive Klassen ohne ausführbare Zeilen). Shared, Testcode und Bibliotheken gehören
nicht zu diesem Nenner. JUnit und Browser sind getrennte Messungen; die Browserdaten
werden je Sprache aus den beiden Workern gegen die Klassen des tatsächlich getesteten
JARs zusammengeführt, nicht gemittelt. Start und Hintergrundjobs zählen mit.
Das misst **keine CSS-/JavaScript-Coverage und keinen Prozentwert aller denkbaren
Fachanforderungen**. 141/141 ist eine Fall-Erfolgsquote. Die JUnit-Messung folgt dem
letzten Java-Fix; spätere CSS-Bereinigungen ändern diesen Java-Messumfang nicht.

### Neue und erhaltene Funktionsabdeckung

| Bereich | Tatsächlich geprüfter Ablauf |
| --- | --- |
| AVATAR-01, zwei Parameterfälle | 1280×900 und 390×844: PNG/JPEG-Auswahl, Zoom/Reset, kompakte Uploadfläche, gleich große lokale Aktionen, Speichern/Reload, Wiederöffnen, lokales und Footer-Abbrechen, Initialen mit erhaltenem Bild, Ersetzen/Löschen; Bilder in Kopfzeile/Popup/Konto/Grid |
| ACCOUNT-01 / STATUS-03 | Initialen nach Namensänderung; direkte Statuswahl am Kontobild, Tastaturöffnung, Kontursymbole, aktive Fläche ohne Zusatzhaken, Statusrahmen/Glow und Synchronisierung |
| INPUT-01 | Vier/sechs/acht Ziffern, DE-/EN-Reihenfolge, 1950/2049, kurze getrennte Jahre, ungültiger Tag, Tab/Enter und tatsächliche normalisierte Feldwerte |
| INPUT-02 | Tatsächliche Frontend-Modulanbindung: 930/1430, Tab/Enter, Sprachwechsel, ungültige Zeit im Aktivitätsfilter |
| CONFIG-05F / Cosmos | 320×568: Info und mobile Statusfläche im Fenster, kein horizontaler Popupüberlauf, Scrollen bis Abmelden und echte Abmeldung; bestehende Desktop-/schmale/Querformat- und Fokusprüfungen bleiben erhalten |
| Bestehende Fachjourneys | Anmeldung/Registrierung/Verifikation/Reset, Passwort-/E-Mail-Wechsel mit eigenen SMTP-Mails, Sitzungen/Remember-me samt echten Neustarts, Admin-CRUD/Filter/Pagination/Konfiguration/Mail, Rollen/Zugriff/Wartung und Tokenablauf |

Technische Grenz-, Transaktions-, Versions-, Byte-/Pixel-/EXIF- und Konkurrenzregeln
bleiben in JUnit/Offlinetests. Sichtbare Browserjourneys erstellen ihre Zustände über
UI und eigene Mailpit-Mails, nicht über Datenbank-/Serviceabkürzungen. Die funktionale
Abdeckung ist die beschriebene Matrix, kein erfundener Vollständigkeitsprozentsatz.

### Bereinigungen, Fehler und Nachtests

Entfernt: `JdbcHelper`, zwei unbenutzte Hilfsmethoden, drei verwaiste eigene CSS-Klassen
mit vier Regeln und vier Getter/Schlüssel je Sprache. Die Sprachkataloge sinken von
455 auf **451 eindeutige Schlüssel** bei Sprachparität und unverändertem Codec.
Native Lumo-/Grid-Regeln, die vier dynamischen Avatar-Schatten und aktive Autoren- und
Frameworkressourcen bleiben erhalten. Keine zusätzliche Bildarchivierung notwendig.

Zwei bestätigte Bedien-/Darstellungsfehler wurden behoben: Deutsch formatiert kompakte
Geburtstage jetzt mit führenden Nullen; das schmale Benutzermenü begrenzt seinen
scrollbaren Inhalt auf Vaadins verfügbare Höhe, sodass Abmelden erreichbar bleibt.
Veraltete Menü-/Status-/Gridselektoren, der fehlende Navigationsschritt im neuen Zeitfall
und die Synchronisierung mit nativen Select-Animationen wurden im Testcode korrigiert.
Assertions wurden nicht abgeschwächt.

Die erste vollständige DE-Runde hatte 139/141: neuer Testaufbau und echter Popupfehler.
Nach gezielten Vorprüfungen (Konfiguration 9/9, Cosmos 11/11, Filter 5/5, Konto 11/11)
wurde wegen der gemeinsamen Popup-CSS-Korrektur ein neuer abschließender Stand gebaut
und vollständig in beiden Sprachen geprüft. Die Vorprüfungen verwendeten den Fix vor
der letzten Entfernung wirkungsloser CSS-Regeln; **beide abschließenden Gesamtrunden**
prüfen das oben genannte endgültige Image. Zwei frühe abgebrochene Anläufe sind keine
vollständigen Nachweise. Ein Python-Aufruf mit dem Systeminterpreter hatte 6 Importfehler
bei 145 gefundenen Tests; mit der vorhandenen Projekt-`.venv` bestehen alle 205.
Weitere Testaufbau-/Kompilierungskorrekturen wurden vor den abschließenden grünen Läufen
behoben. Die alten Runs bleiben erhalten.

### Grenzen und Übernahme

Chromium 153.0.8010.12, simulierte Fenstergrößen und eigene frische H2-/Mailpit-Daten.
Keine Aussage über echte Mobilgeräte/Touch/Screenreader, sämtliche Lade-/Netzausfall-
zustände, externe SMTP-Provider mit Auth/TLS, Mailprogramme, Clusterbetrieb oder
produktive Bestandsmigrationen. Ein gezielter Avatar-Neustartnachweis mit zuvor
hochgeladenem Bild bleibt offen; H2-Persistenz und Reload sind geprüft.
Die 15 begrenzten manuellen/Betriebs-/Integrationsfälle im
[manuellen Katalog](../docs/manual-tests/README.md) bleiben davon getrennt.

Alle eigenen Teststacks sind abgebaut; die abschließende Containerkontrolle war leer.
Keine alten Runs gelöscht, keine Nutzer-/IntelliJ-/Vite-Prozesse beendet und keine
Bestandsdatenbank oder realen Postfächer für Tests verwendet. Produktionsbuild und
gepacktes JAR wurden in Docker gestartet/geprüft, kein regulärer IDE-Start ausgelöst.
Für einen sauberen lokalen Übernahmestand Shared/App bauen, einmal regulär neu starten
und den Browser aktualisieren (gegebenenfalls Strg+F5). HotSwap-Übernahme wurde nicht
als eigener Nachweis geprüft.


Die optionale vollständige Designkritik wurde **nicht ausgeführt**. Die frische
Nutzungslimitabfrage nach Abschluss der QA meldete das Fünf-Stunden-Kontingent als
100 % verbraucht; damit war kein ausreichendes reguläres Kontingent für eine sinnvoll
abschließbare Zusatzanalyse vorhanden. Kein Reset und keine zusätzliche Buchung wurden
ausgelöst. Die Designkritik bleibt ein ausdrücklich offener separater Punkt; die
funktionalen QA-Ergebnisse oben sind vollständig abgeschlossen.

Die folgenden datierten Abschnitte sind historische Nachweise.

## Sitzungsverwaltung und Kontosicherheit vollständig geprüft (17.10.2026)

Branch `feature/account-session-management`, Basis
`6fe300a7709c6b172052d2f355166daa5f75e7ef` mit uncommitteter Implementierung.
Die große Runde begann nach 08:00 Uhr Europe/Berlin; die abschließenden vollständigen
Browserläufe endeten um 09:38:56 (DE) und 10:01:57 (EN). Beide verwenden dasselbe
gepinnte Produktionsimage und je zwei getrennte JVM-/Docker-/H2-/Mailpit-Worker.
[Vertrag Sitzungsverwaltung](../docs/session-management-contract.md),
[Kontosicherheit](../docs/account-security-contract.md),
[dauerhafter Meilenstein](../docs/meilensteine/2026-10-17-sitzungsverwaltung-und-kontosicherheit.md).

| Prüfung | Ergebnis des endgültigen Stands |
| --- | --- |
| Java / Shared / Produktionsbuild | Java 25.0.4.1 bestätigt; erfolgreich |
| Vollständige Anwendungs-JUnit-Suite | 1424/1424, keine Fehler oder Skips |
| Browserhelfer / Reporter / Workeraufteilung | 47/47, keine Fehler oder Skips |
| Python / JavaScript offline | 205/205 und 14/14, keine Live-Provider-/Modellanfragen |
| Browser `all` DE / EN | Je 136/136, 26 Klassen, keine Fehler oder Skips |
| Workeraufteilung je Sprache | 58/58 und 78/78; getrennte Daten und JVMs |
| Fast-Auswahl | Alle 34 markierten Fälle in beiden Gesamtrunden bestanden; kein separater Lauf |
| Aufbau / Abbau / HTML / Coverage | Beide abschließenden Läufe vollständig erfolgreich |
| Format / Diff | Spotless Apply/Check, Ruff-Formatprüfung und Git-Diffkontrolle erfolgreich |

ACCOUNT-SEC-01–04 ergänzen Validierung, Passwortwechsel mit Bestätigungsmail und
vollständigen verifizierten E-Mail-Wechsel. SESSIONS-01–05 ergänzen echte Listen,
Tabzählung, gezielten und Sammelwiderruf, tatsächlichen Serverneustart mit dauerhaftem
Remember-me-Widerruf und eingeschränkten Zugang nach Passwortprüfung bei Limit 1.
SESSIONS-01 erhält zusätzlich Meldungen im anderen Fenster, auch beim Sprachwechsel.
Alle Zustände werden über UI und eigene abgefangene SMTP-Mails aufgebaut;
keine Konto-/Tokenanlage durch direkte Datenbank- oder Serviceaufrufe.

Drei bestätigte Fehler wurden vor den endgültigen Läufen behoben und mit Regressionen
abgesichert: JDBC-Remember-me-Namen waren auf 64 statt 255 Zeichen begrenzt;
NotificationHelper teilte veränderlichen Zustand zwischen UIs; die neue Bereinigung
verwendete eine nur gespeicherte statt der laufenden startgebundenen Remember-me-Dauer.
Zusätzlich wurden neue Browserabläufe bei Dialogsichtbarkeit, Tabwechseln und eigener
Bereinigung sauber synchronisiert. Fachliche Assertions wurden nicht abgeschwächt.

| JaCoCo-Messung | Zeilen | Zweige |
| --- | ---: | ---: |
| Vollständige Anwendungs-JUnit-Suite | 6910/8157 (84,71 %) | 1833/2507 (73,12 %) |
| Browser all DE | 6949/8157 (85,19 %) | 1622/2507 (64,70 %) |
| Browser all EN | 6958/8157 (85,30 %) | 1623/2507 (64,74 %) |

Die XML-Berichte umfassen jeweils 152 ausführbare Anwendungs-Java-Klassen. Die beiden
Browserberichte stammen aus ihren jeweiligen Workern und den Klassen desselben
Produktions-JARs. Keine Vermischung mit alten Images, keine Addition wiederholter
Testfälle und kein Mittelwert verschiedener Messungen. Serverseitige Browser-Coverage
enthält Start und Hintergrundjobs; sie misst **weder CSS/JavaScript noch den Anteil
vollständig geprüfter Anforderungen**. Kleine Sprachunterschiede können aus zusätzlich
ausgeführten Start-/Hintergrundpfaden entstehen.

Aktive Sitzungen bleiben auf eine Anwendungsinstanz begrenzt. Produktive Bestandsmigration,
Clusterbetrieb, echte Geräte/Screenreader, externe SMTP-Provider/Postfächer und Mailclients
bleiben ungeprüft. Manuelle Nutzerabnahme steht aus. Shared und Anwendung neu bauen und
einmal neu starten; die neue Metadatentabelle und ausreichende Token-Username-Länge müssen
im Schema vorhanden sein. Keine Bestandsdatenbanken wurden geöffnet oder verändert.

Nur eigene Teststacks wurden abgebaut; die Containerkontrolle der beiden abschließenden
Läufe war leer. Keine alten Run-Ordner gelöscht. Main, Index und Remote bleiben unangetastet.
Die folgenden datierten Abschnitte beschreiben frühere Nachweise; ihre Zahlen sind
historisch und ersetzen nicht die aktuelle Gesamtprüfung oben.

## Nachtrag: Admin-Menüposition in der URL (17.10.2026)

Auf `feature/account-settings-ui`, Basis `14e4bb62`, ergänzt die AdminView die
bestehenden section-/group-Parameter um Flow-Navigation und Wiederherstellung in
beforeEnter. Benutzerverwaltung, Mail-Test, Dashboard und alle sieben typisierten
Konfigurationsgruppen können direkt geöffnet werden. Refresh und Browserhistorie
stellen dieselbe Auswahl wieder her. Bestehende Zugriffsprüfungen bleiben vorgeschaltet;
fehlende, unbekannte oder mehrdeutige Werte erhalten einen festen Standard.

29 gezielte JUnit-Fälle erfolgreich: 20 neue URL-/Gruppen-/Zugriffsfälle sowie vier
bestehende Konfigurationsgruppen- und fünf Konto-View-Regressionsfälle. Der frische
Produktionsbuild ist erfolgreich. Die gezielte Browser-Suite configuration bestand
DE und EN jeweils 9/9, ohne Fehler oder Skips, am identischen gepinnten Image.
CONFIG-05A prüft jetzt zusätzlich echte Seitenreloads, Direktlink im zweiten Tab und
Zurück/Vorwärts zwischen Benutzerverwaltung und Security, neben den bisherigen
Tastatur-, Sprach- und Entwurfsprüfungen. Kein neuer Browserfall und keine erneute
vollständige all-Runde; Fallzahl und fast-Auswahl bleiben unverändert.

Beide eigenen Docker-/H2-/Mailpit-Teststacks wurden vollständig abgebaut;
Formatierung und Diff-Prüfung sind erfolgreich. Die unten stehenden Gesamtzahlen
und Coverage dokumentieren die vorherige Kontoprofil-QA und messen diese URL-Ergänzung
nicht. Für die Übernahme einmalig die IDE-Anwendung neu starten und Strg+F5 ausführen.

**Abgeschlossene Kontoprofil-Gesamtprüfung 17.10.2026, vor der folgenden Admin-URL-Erweiterung: 1.298 Anwendungstests, 45 Browserhelfer,
205 Python- und 14 JavaScript-Offlinetests erfolgreich. Je 127 unterschiedliche
Browserfälle DE/EN über vollständige Gesamtrunden und gezielte Nachtests nachgewiesen.**

## Kontoprofil, E-Mail-Dialog und Neustart (16.–17.10.2026)

Branch `feature/account-settings-ui`, Basis `0956ff62`, Ergänzungen uncommittet.
[Kontovertrag](../docs/account-settings-ui.md),
[Neustart-Analyse](../docs/login-restart-analysis.md),
[dauerhafter Meilenstein](../docs/meilensteine/2026-10-16-kontoprofil-und-browserpruefungen.md).

| Prüfung | Tatsächliches Ergebnis |
| --- | --- |
| Java / Shared / Produktionsbuild | Java 25.0.4.1; erfolgreich einschließlich Tests und Frontend |
| Vollständige Anwendungs-JUnit-Suite | 1298/1298; keine Fehler oder Skips |
| Browserhelfer / Reporter / Workeraufteilung, endgültiger Stand | 45/45; keine Fehler oder Skips |
| Python / JavaScript offline | 205/205 und 14/14; keine Live-Provider-/Modellanfragen |
| Vollständiges Browser all DE / EN, je zwei isolierte Worker | Je 126/127, je ein Browserhelfer-/Bereinigungsfehler, keine Skips |
| Account-Nachtest nach Bereinigungskorrektur, DE | 7/7 |
| Filter-Nachtest nach Scrollkorrektur, DE / EN | Je 4/4; beide Scrollrichtungen unabhängig von der Uhrzeit |
| Unterschiedliche erfolgreiche Fallidentitäten | Je alle 127 aus all; DE/EN identisch, keine Doppelungen |
| Fast-Auswahl | Alle 34 markierten Fälle in beiden Gesamtrunden bestanden; kein separater Lauf |
| Aufbau / Abbau / HTML / Coverage | Alle fünf abschließend ausgewerteten Läufe erfolgreich |
| Formatstandard / Diff | Google Java Format, Ruff-Formatprüfung und Diff-Prüfung erfolgreich |

ACCOUNT-01–03 und ACCOUNT-04A–D ergänzen sieben UI-Fälle für dauerhaftes Speichern,
Validierung, Sprachpräferenz und den noch funktionslosen E-Mail-Dialog. Ein echter
Escape-Fehler mit erhaltenem Passwortentwurf wurde behoben und mit JUnit sowie
allen vier Browser-Schließwegen nachgeprüft. Zwei weitere Fälle AUTH-07C/D prüfen
echte Serverneustarts. `all` enthält jetzt 127 Fälle in 24 Klassen.

Der deutsche Account-Sprachtest scheiterte nach bestandener fachlicher Prüfung
an doppelter Menübereinigung; die gemeinsame Fixture übernimmt sie bereits.
USER-05C auf EN fand einen zeitabhängigen Helferfehler: Ein unsichtbarer
Mitternachtseintrag führte zur falschen Scrollrichtung. Der Helfer verwendet jetzt
die erste tatsächlich sichtbare Zeile; echte Scrollbewegung und Filterwirkung bleiben
streng geprüft. Der Fall wählt zusätzlich Tagesende und Rückweg, auch nach Mitternacht.
Keine globalen Timeouts erhöht oder Assertions zur Umgehung abgeschwächt.

Der Abschlussnachweis kombiniert vollständige Runden und erfolgreiche Nachtests.
Es wird kein vollständig grüner all-Einzellauf behauptet und keine Summe wiederholter
Fälle als Fallzahl ausgegeben. Zwischen den Nachweisen blieb der Anwendungscode gleich;
es wurden nur die beschriebenen Browserabläufe korrigiert beziehungsweise erweitert.

| JaCoCo-Messung | Zeilen | Zweige |
| --- | ---: | ---: |
| Vollständige Anwendungs-JUnit-Suite | 5289/6920 (76,43 %) | 1432/2071 (69,15 %) |
| Browser DE, Gesamtrunde + Account-/Filternachtests | 5894/6920 (85,17 %) | 1362/2071 (65,77 %) |
| Browser EN, Gesamtrunde + Filternachtest | 5894/6920 (85,17 %) | 1362/2071 (65,77 %) |

Alle Browsernachweise verwenden dasselbe gepinnte Produktionsimage; die extrahierten
Klassen wurden zusätzlich bytegleich verglichen. Die Messdaten wurden je Sprache
gegen diese Klassen zusammengeführt, einschließlich ausgeführter Abschnitte vor den
jeweiligen Helferfehlern. Der JaCoCo-Klassenzähler umfasst 127 Anwendungs-Java-Klassen
mit ausführbarem Code; Shared und Drittbibliotheken sind ausgeschlossen. Serverseitige
Browser-Coverage enthält Start und Hintergrundjobs, **keine CSS-/JavaScript-Messung
oder Prozentzahl vollständig geprüfter fachlicher Anforderungen**.

Der konkrete Remember-me-Verlust nach IntelliJ-Neustart bleibt unbestätigt und gilt
nicht als behoben. Die isolierten Browser-/Produktions-Neustartfälle sind in beiden
Sprachen bestanden. Echte Geräte, Screenreader, weitere Browser und reale Provider/
Postfächer bleiben außerhalb dieser Prüfung. Keine reguläre Anwendung gestartet,
keine echten Daten oder fremden Dockerumgebungen verändert. Nur eigene Teststacks
wurden abgebaut; keine alten Run-Verzeichnisse gelöscht.

Die folgenden Abschnitte dokumentieren frühere abgeschlossene Stände. Ihre
Fallzahlen und Coverage sind historische Messergebnisse, keine aktuellen Werte.

## Wartungsmodus und vollständige Gesamtrunde (16.10.2026)

Branch `feature/maintenance-mode`, Basis `05a67e8ca25ac4cacb326096c18b5f89b57a8406`.
Umsetzung und Dokumentation lagen uncommittet vor.
[Verbindlicher Wartungsvertrag](../docs/maintenance-mode-contract.md),
[dauerhafter Meilenstein](../docs/meilensteine/2026-10-16-wartungsmodus-und-gesamttests.md).

| Prüfung | Ergebnis des endgültigen Stands |
| --- | --- |
| Java / Shared-Modul | Java 25 bestätigt; Shared sauber gebaut und installiert |
| Vollständige Anwendungs-JUnit-Suite | 1226/1226; keine Fehler, Fehlschläge oder Skips |
| Browserhelfer / Reporter / Suite- und Workeraufteilung | 44/44; keine Fehler oder Skips |
| Python-Offline-Regressionen | 205/205; keine Fehler oder Skips; keine Live-Provider-/Modellanfragen |
| Produktionsbuild | Erfolgreich einschließlich Anwendungstests und Frontend |
| Browser `all` DE / EN, jeweils zwei isolierte Worker | Je 118/118, keine Fehler oder Skips; identisches gepinntes App-Image |
| Vollständige Fallidentitäten / Fast-Auswahl | DE und EN identisch, keine Doppelungen; jeweils alle 34 Fast-Fälle enthalten |
| Aufbau / Abbau / HTML / Coverage | Beide endgültigen Gesamtläufe vollständig erfolgreich |
| Formatstandard | Google Java Format über Spotless und Ruff-Formatprüfung erfolgreich |

Vor der Erweiterung wurde die unveränderte Basis ebenfalls vollständig geprüft:
1171 Anwendungstests, 43 Browserhelfer, 205 Python-Offlinetests und `all` mit je
109 Fällen DE/EN erfolgreich. Die folgenden Zahlen beziehen sich ausschließlich
auf den endgültigen erweiterten Stand, nicht auf addierte oder abgebrochene Läufe.

### Wartungsnachweise

MAINT-01 prüft HTTP 503 für öffentliche Dokumente, geladene Cosmos-Card und Erhalt
einer bestehenden Sitzung nach Freigabe. MAINT-02A–D prüfen Benutzer, Gast,
Administrator und Superadmin einschließlich administrativem Ausschalten. MAINT-03
klickt ein vor Wartung geöffnetes Registrierungsformular und weist ausbleibende
Kontoanlage nach. MAINT-04A/B verwenden tatsächlich empfangene eigene SMTP-Mails:
Tokenlinks und altes Resetformular sind während Wartung gesperrt; derselbe gültige
Link funktioniert nach Freigabe. MAINT-05 startet Chromium tatsächlich neu und
prüft Remember-me-Ablehnung ohne zusätzlichen Sitzungsplatz.

JUnit ergänzt Konfigurationsfehler und Live-Wechsel, echte Spring-Security-Kette,
explizite erneute Anmeldung, fremde Thread-Principals, gefälschte UI-IDs und
Vaadin-Parameter an fremden Servletpfaden. Der echte `PushHandler` wird vor gültigen
RPC-Callbacks geprüft. H2-Persistenztests erhalten Tokens/Hash/Queue und weisen
vollständigen Registrierungsrollback nach. Hintergrundoperationen bleiben verwendbar.

### Frisch gemessene JaCoCo-Coverage

| Messung | Zeilen | Zweige |
| --- | ---: | ---: |
| Anwendungs-JUnit | 4624/6425 (71,97 %) | 1340/2023 (66,24 %) |
| Browser all DE | 5413/6425 (84,25 %) | 1325/2023 (65,50 %) |
| Browser all EN | 5415/6425 (84,28 %) | 1328/2023 (65,65 %) |

Die JUnit-Messdatei wurde vor dem letzten vollständigen Testlauf entfernt; die
Browsermessungen wurden pro Sprache aus den jeweiligen beiden Workern gegen die
Klassen ihres geprüften Produktions-JARs zusammengeführt. Beide Berichte umfassen
119 analysierte Anwendungs-Java-Klassen. Shared-JAR und Drittbibliotheken gehören
nicht zu diesem Reportumfang. Browser-Coverage enthält auch Start und Hintergrundjobs;
sie ist **keine CSS-/JavaScript-Coverage und keine Prozentzahl fachlich vollständiger
Browserabdeckung**. Die 118 Fälle bezeichnen bestandene konkrete Abläufe.

`fast` wurde nicht separat erneut gestartet; seine 34 markierten Fälle sind in
beiden `all`-Läufen enthalten. Echte Endgeräte, Screenreader, Service-Worker-/PWA-
Caches, produktive Bestandsmigrationen und reale Provider/Postfächer bleiben außerhalb
dieser isolierten Prüfung. Vorhandene menschliche/Betriebsprüfungen stehen im
[Handprüfkatalog](../docs/manual-tests/README.md).

Bei der Integration wurden HTTP-/Push-Trennung, die erste UIDL einer leeren Server-UI,
Bootstrap-Ressourcen unter verschachtelten Tokenpfaden sowie enge Transport-/Principal-
Ausnahmen korrigiert und erneut geprüft. Alte Zwischenstände und kontrolliert
abgebrochene, überholte Läufe zählen nicht als Abschlussnachweis. Assertions wurden
nicht abgeschwächt; die endgültigen Quellen blieben nach den Läufen unverändert.
Es wurden keine fremden Dockerumgebungen oder Anwendungsdaten verändert.
Der Abbau ist in allen eigenen Eltern-Laufprotokollen erfolgreich bestätigt. Bei
der zusätzlichen Bestandskontrolle am Vormittag war die Docker-Linux-API nicht
erreichbar; eine erneute Live-Inventur wurde deshalb nicht als Nachweis gewertet.



## Gleichzeitige Sitzungen pro Benutzer (15.10.2026)

Branch `feature/max-sessions-per-user`, Ausgangscommit `d69f38b2`; Umsetzung und
Dokumentation lagen bei der Prüfung uncommittet vor.
[Verbindlicher Funktions-/Einführungsvertrag](../docs/session-limit-contract.md).

| Prüfung | Ergebnis |
| --- | --- |
| Java 25 / gemeinsamer Übersetzungskatalog | Geprüft; Shared sauber gebaut und installiert |
| Vollständige Anwendungs-JUnit-Suite | 1171/1171; keine Fehler, Fehlschläge oder Skips |
| Browserhelfer und Suite-/Workeraufteilung | 43/43; keine Fehler, Fehlschläge oder Skips |
| Frischer Produktionsbuild | Erfolgreich, einschließlich Anwendungstests und Frontend |
| `session-limits`, Deutsch / Englisch, je zwei isolierte Worker | Je 27/27; identisches gepinntes App-Image, keine Fehler oder Skips |
| Gezielter deutscher Nachtest `session-security` nach Synchronisierungskorrektur | 5/5; dasselbe App-Image, keine Fehler oder Skips |
| Aufbau, Abbau und Berichte der abgeschlossenen Nachweise | Vollständig erfolgreich |

SESSION-LIMIT-01 prüft die gemeinsame Tab-Zählung, Ablehnung bei voller Belegung und
Freigabe nach Logout. SESSION-LIMIT-02 prüft Reduzierung ohne Verdrängung und sofortige
Erhöhung. SESSION-LIMIT-03 beendet einen echten Chromium-Prozess ohne Logout und
weist nach, dass das keinen Platz freigibt. SESSION-LIMIT-04 startet einen echten
Remember-me-Browser erneut: automatische Anmeldung kann die Grenze nicht umgehen;
die bestehende zweite Sitzung bleibt nutzbar. Die übrigen 23 Fälle prüfen bisherigen
Login, Passwortsperre/-reset, Remember-me, alle Rollen und alte privilegierte Aktionen.

Ein englischer Bestandsfall wartete zunächst auf das globale `load`-Ereignis, obwohl
302-Weiterleitung, Loginantwort und sichtbare Loginmaske bereits vorlagen. Die
Bestandsprüfung wartet nun auf das neue DOM und das vollständige sichtbare Formular.
Die echte 401-Abwehr und die Prüfung, dass keine alte Speichern-Aktion ausgeführt
wurde, bleiben erhalten. Danach bestanden die vollständige englische Suite und die
fünf betroffenen Bestandsvarianten zusätzlich auf Deutsch. Kein erhöhter globaler
Timeout, kein automatisches Wiederholen fehlschlagender Fälle und keine Testausnahme
für das Sitzungslimit wurden eingeführt.

JUnit prüft zusätzlich Grenzwerte 1/3, Normalisierung, gleiche Grenze für alle Rollen,
Sitzungs-ID-Wechsel ohne UI-Verlust, Freigabe durch Invalidierung/Timeout-Ereignis,
parallele Anmeldung am letzten Platz und parallele Kontowechsel in derselben Sitzung.
Speicher-, Status- und Cookiefehler hinterlassen keinen neuen Registerplatz. Die echte
HTTP-Filterkette und Vaadin verwenden denselben Bestand; Remember-me-Ablehnung löscht
keine Tokenserien anderer Geräte. Die Checkbox-Auswahl ist pro Anfrage isoliert.

**Grenzen:** Eininstanzbetrieb mit lokalen Servlet-Sitzungen. Produktive Bestandsdaten
und laufende Benutzeranwendungen wurden nicht angefasst. Ein echtes produktives
Sitzungstimeout wurde nicht abgewartet; JUnit prüft die Lifecycle-Ereignisse. Kein neuer
vollständiger `all`-Browserlauf und keine neue Coverage-Prozentmessung. `all` enthält
damals 109 Fälle in 22 Klassen; `fast` unverändert 34. Ältere Coverage-Werte unten sind
historische Messungen. Die Geräteverwaltung bleibt der separat geplante zweite Ausbau
im zukünftigen Kontokonfigurationspanel.


## Begrenzung der Passwort-Anmeldeversuche (15.10.2026)

Branch `feature/login-attempt-limit`, Ausgangscommit `a37cf0c2`; Umsetzung und
Dokumentationsänderungen lagen bei der Prüfung uncommittet vor.
[Verbindlicher Funktions-/Einführungsvertrag](../docs/login-attempts-contract.md).

| Prüfung | Ergebnis |
| --- | --- |
| Java 25 / gemeinsamer Übersetzungskatalog | Geprüft; Shared sauber gebaut und installiert |
| Vollständige Anwendungs-JUnit-Suite | 1149/1149; keine Fehler, Fehlschläge oder Skips |
| Browserhelfer und Suite-/Workeraufteilung | 43/43; keine Fehler, Fehlschläge oder Skips |
| Frischer Produktionsbuild | Erfolgreich, einschließlich Anwendungstests und Frontend |
| `login-attempts`, Deutsch / Englisch, je zwei isolierte Worker | Je 11/11; identisches gepinntes App-Image, keine Fehler oder Skips |
| Aufbau, Abbau und gemeinsame Berichte | Beide Läufe vollständig erfolgreich |

AUTH-LIMIT-01 prüft tatsächliche Sperre nach gespeichertem Limit, fehlende Anmeldung/
Remember-me-Cookies und Erhalt einer schon angemeldeten Sitzung. AUTH-LIMIT-02 prüft
sofortige Limitreduzierung und gleiche neutrale Sperrhinweise für bekannte/unbekannte
Kennungen. AUTH-LIMIT-03 verwendet eine echte eigene Resetmail: Anfrage allein hebt
nicht auf; erst abgeschlossener Passwortwechsel ermöglicht wieder die Anmeldung.
Die sechs bestehenden Authentifizierungsfälle und zwei Remember-me-Fälle mit echten
Chromium-Neustarts sind in derselben Suite in beiden Sprachen erneut bestanden.

JUnit weist zusätzlich genaue Fenster-/Sperrgrenzen, Konkurrenz bei derselben und
verschiedenen Kennungen, maximale Zählerzahl, Bereinigung, Caller-Rollback,
Passwort-/Token-/Zähler-Rollback, alle Rollen und die echte HTTP-Filterkette nach.
Ein isoliertes Bestandsupgrade erhält das Konto und speichert die Sperre über neue
Persistenzkontexte. Ein Hikari-Pool mit einer Verbindung prüft die transaktionale
Passwortabfrage. Echte Spring-Faktornachweise erhalten eine gültige Sitzung; eine
echte Kontosperre und nachgeahmte Faktorstrings werden weiterhin erkannt.

Zeitablauf wird mit kontrollierter Clock in JUnit geprüft; im Browser wurde keine
15-Minuten-Wartezeit durchlaufen. Mit der damaligen Login-Erweiterung umfasste die vollständige Browser-Suite 105
Fälle in 21 Klassen, wurde diesmal jedoch nicht komplett neu ausgeführt. `fast`
bleibt bei 34 Fällen und wurde nicht als vollständige Fast-Auswahl erneut gestartet.
Es gibt keine neue Coverage-Auswertung. Frühere Prozentwerte gehören zu den unten
getrennt dokumentierten Prüfständen. Bestehende Anwendungsdatenbanken wurden nicht verwendet.

## Vorherige Prüfstände

**Gezielte Rollenprüfung am 15.10.2026: 353 Anwendungs-JUnit-Tests und je 7 Zugriffsfälle auf Deutsch/Englisch erfolgreich.**
Die früheren vollständigen Theme- und Runnerprüfungen bleiben unten getrennt dokumentiert;
sie sind kein vollständiger Nachweis für die anschließend ergänzten Rollen.
Bedienung und Aufbau stehen in der [Browser-Testanleitung](README.md).
Wichtige Abschlüsse bleiben unter [Meilensteine](../docs/meilensteine/README.md)
unabhängig von bereinigbaren lokalen Testausgaben erhalten.

## Superadmin und Gast (15.10.2026)

| Prüfung | Ergebnis |
| --- | --- |
| Java 25 / gemeinsames Übersetzungsmodul | Sauber gebaut und installiert |
| Gezielte Anwendungs-JUnit-Auswahl | 353/353; keine Fehler, Fehlschläge oder Skips |
| `user-access` DE / EN | Je 7/7 mit identischem neu gebautem App-Image; keine Fehler oder Skips |
| Eigene Docker-/H2-/Mailpit-Umgebungen und Berichte | Aufbau, Abbau und Berichtserstellung erfolgreich |

USER-07F weist ausschließlich Superadmin über das Formular zu und prüft erneutes
Öffnen, Adminlogin, Verwaltungsaktion auf Home und Speichern des eigenen Formulars.
USER-07G weist ausschließlich Gast zu und prüft erneutes Öffnen, Login/Home,
fehlende Adminaktion und abgewiesenen direkten Adminzugriff. Beide sind über die
reguläre Oberfläche in beiden Sprachen bestanden. USER-07A–E wurden ebenfalls erneut geprüft.

JUnit prüft zusätzlich die vier persistierten Rollen, Übersetzungen, Zuweisung und exakte
Filter einschließlich Badges, unveränderte Bootstrap-/Registrierungsrollen, die echte
Security-Kette, Rollenentzug für bestehende Sitzungen und RPC-Schutz. Admin und Superadmin
zählen gemeinsam beim Letzter-Admin-Schutz; doppelte Rollen zählen einmal, parallele
Änderungen bleiben gesichert. Ein alter H2-Enum mit zwei Rollen wird in einer frischen
In-Memory-Datenbank erweitert: alte IDs, Verknüpfung und Foreign Key bleiben erhalten;
ein zweiter Aufruf führt kein weiteres ALTER aus. Bestehende Anwendungsdaten wurden nicht benutzt.

Nach der Rollenerweiterung umfasste `all` 102 Browserfälle, `fast` weiterhin 34.
Der spätere Login-Zuwachs ist im aktuellen Abschnitt oben berücksichtigt. Die vollständige 102er-Suite
und der Fast-Modus wurden für diese Rollenänderung nicht erneut ausgeführt.
Es wurde keine neue vollständige Code-Coverage gemessen; die unten genannten Werte
gehören zu ihrem jeweiligen früheren Prüfstand. Rollenstandard: [Projekt-README](../README.md#rollen-und-zugriffsrechte).

## Fast-Modus und isolierte Parallelisierung (15.10.2026)

Branch `feature/browser-tests-fast-parallel`, Ausgangscommit `003f6ac7`;
Runner-/Testmarkierungs- und Dokumentationsänderungen lagen uncommittet vor.
Anwendungsquellen und Styles wurden dafür nicht geändert. Die vorhandenen 100
Browserfälle bleiben erhalten; `fast` ist eine bewusst markierte Teilmenge mit 34 Fällen.
Auswahl, Gründe und Messgrenzen stehen in der [Testanleitung](README.md#schnellprüfung-und-parallele-ausführung).

| Prüfung | Ergebnis |
| --- | --- |
| Java 25 / Helper und Reporter | 43/43; keine Fehler, Fehlschläge oder Skips |
| Fast DE sequenziell | 34/34; Aufbau, Abbau und Bericht erfolgreich |
| Fast DE mit zwei Workern | 34/34; dieselben Fall-/Parameteridentitäten wie sequenziell, keine Doppelungen |
| Fast EN mit zwei Workern und Coverage | 34/34; gemeinsamer JaCoCo-Bericht gegen das geprüfte JAR erfolgreich |
| All DE mit zwei Workern und normalem Image-Build (Cache) | 100/100, 54 bzw. 46 Fälle je Worker; keine Fehler/Skips, identische Image-ID beider Worker, Aufbau/Abbau und gemeinsamer Bericht erfolgreich |
| Absichtlicher Fehler-Probelauf | 1 erwarteter Fehlschlag, Exitcode 1 und roter Gesamtbericht; eigener Abbau erfolgreich |
| Kontrollierter Abbruch nach Aufbau zweier Worker | Exitcode 23 des temporären QA-Helfers; beide eigenen Projekte vollständig abgebaut, `interrupted` samt Endzeit gespeichert |

In der DE-Einzelmessung sank die Zeitspanne aller 34 Fälle von **3:53 auf 2:16 min**;
der gesamte Java-Runner benötigte **4:21 bzw. 2:42 min**. Starter-Vorbereitung davor
ist ausgeschlossen. Der sequenzielle Lauf rief den Image-Build mit Cache auf,
der parallele Vergleich verwendete dasselbe Image mit `-NoBuild`. Dies ist keine
Dauerzusage und kein langfristiger Durchschnitt. Mehr Worker benötigen mehr CPU/RAM.

Geprüft wurde die reale Zwei-Worker-Ausführung. Die Verteilung für 1–4 Worker ist
zusätzlich durch Helpertests abgedeckt; vier gleichzeitige Docker-Umgebungen wurden
hier nicht ausgeführt. Der Abbruchnachweis gilt für kontrollierten JVM-Shutdown,
nicht für einen harten Betriebssystem-/Rechnerabbruch. Die beiden Probeläufe sind
keine bestandenen fachlichen Browserläufe und werden nicht in die Erfolgszahlen addiert.

Der vollständige parallele DE-Lauf benötigte **12:59 min** für den Java-Runner;
die Fallspanne betrug **12:26 min**. Die 100 Fall-/Parameteridentitäten stimmen exakt
mit dem vorherigen vollständigen DE-Lauf überein; alle 34 Fast-Fälle sind enthalten.
Dies ist eine aktuelle Einzelmessung, kein neuer gepaarter Vergleich mit einem
sequenziellen `all`-Lauf. Englisch wurde für den neuen Runner mit `fast` geprüft,
nicht nochmals mit `all`. Nach der Prüfung blieben von den eigenen Testprojekten
keine Container, Netzwerke oder Volumes zurück.

Die Fast-Coverage prüft die technische Zusammenführung serverseitiger Java-Rohdaten.
Es entstehen ein gemeinsamer HTML-/Markdown-Testbericht und nur ein geprüftes JAR,
keine Kopie je Worker. Dies ist keine CSS-/JavaScript-Coverage. Anwendungs-JUnit und
dessen Coverage wurden in dieser Runner-Nachprüfung nicht erneut ausgeführt; der
vorherige vollständige Anwendungsprüfstand folgt unten.

## Vollständige Theme-Prüfung

Branch `feature/theme-css-structure`, Ausgangscommit `e429f86f`;
Theme-/Dokumentationsänderungen lagen bei der Prüfung uncommittet vor.
Zentrale Tokens, Dateiaufteilung und umbenanntes `admin-users.css` sind im frischen
Produktionsimage geprüft. Alle sieben CSS-Dateien werden korrekt ausgeliefert;
ihre geparsten Regeln stimmen trotz Produktionsminifizierung mit dem Quellstand überein.

| Prüfung | Ergebnis |
| --- | --- |
| Java 25.0.4.1 / Shared sauber installiert | Erfolgreich |
| Anwendungs-JUnit | 1085/1085; 0 Fehler, Fehlschläge, Skips |
| Browser-Helper/Reporter | 34/34; 0 Fehler, Fehlschläge, Skips |
| Frischer Produktionsbuild einschließlich JUnit und Frontend | Erfolgreich |
| Browser Deutsch / Englisch | Je 100/100; 0 Fehlschläge oder Skips, identisches App-Image |
| Aufbau, Abbau und Allure-/Markdown-Berichte | Beide vollständig erfolgreich |

JUnit-Coverage: **71,16 % Zeilen** (3689/5184) und
**66,37 % Zweige** (1127/1698); frische Messung.
Gemessen ist serverseitiger Java-Anwendungscode. Browser-Code-Coverage wurde diesmal
nicht erneut instrumentiert; die frühere Messung steht im
[Redesign-Meilenstein](../docs/meilensteine/2026-10-15-redesign-abgeschlossen.md).
Die Fall-Erfolgsquote ist keine CSS-/JavaScript- oder vollständige fachliche Coverage.

[Dauerhafter Theme-Meilenstein](../docs/meilensteine/2026-10-15-theme-struktur-geprueft.md) mit Ergebnissen,
Erkenntnissen und Messgrenzen; [Theme-Anleitung](../docs/theme.md) für die neue Struktur.
Die vollständige Prüfung ist erfolgreich abgeschlossen. Weitere Designkorrekturen
waren in dieser Nachprüfung nicht nötig.

USER-11A/B prüfen Filterentwürfe, Anwenden, einzelne Badges, Datumabhängigkeit und
Scrollen der begrenzten Zeitliste ohne Auswahl. USER-12A/B prüfen die optionale
Passwortbestätigung mit Fehlern erst nach Blur und sichere Schließen-Aktionen.
Beide Sprachläufe verwenden getrennte eigene Docker-/H2-/Mailpit-Umgebungen und Konten.
Die eigenen Container, Netzwerke und Volumes wurden vollständig abgebaut;
IDE-Anwendung und Bestandsdaten waren keine Testziele.

## Gemeinsames Design der Kontoansichten

Login, Registrierung, Verifikation, Passwortanfrage und Passwortänderung verwenden
gemeinsame Java-/CSS-Bausteine, die Palette der Silent-Cosmos-Referenz, animierte
CSS-Dekoration, lokales Inter und übersetzte Platzhalter. Inter gilt global; Home/Admin
verwenden ebenfalls die gemeinsame Cosmos-Palette. Copyright wird über Allgemein gespeichert, seine
Platzhalter werden ersetzt und HTML-Zeichen als Text angezeigt. Die vorherigen
Hintergrundbilder sind aus den aktiven Ressourcen entfernt; die versionierten
Originale bleiben über Git nachvollziehbar. Ein lokaler Backupordner ist aktuell
nicht vorhanden.

| Fälle | Nachweis im vollständigen DE-/EN-Lauf |
| --- | --- |
| COSMOS-01A–E | Alle fünf Ansichten bei 320×568, 390×844, 844×390, 1280×900 und 1280×1200; keine horizontale Abschneidung, gemeinsame Gestaltung, sichtbare Platzhalterschrift und Screenshots |
| COSMOS-02 | Mobile Registrierung, echte Aktivierungsmail, erfolgreiche Verifikation, echte Reset-Mail, Passwortänderung mit Erfolgscard/Häkchen und Anmeldung bei 390×844 |
| COSMOS-03 | Copyright über Admin gespeichert/neu geladen, sicherer Fußtext mit year/serviceName in UI und echten Verifikations-/Resetmails, geladene Inline-Logos und Wiederherstellung; gemeinsame Admin-Palette |
| COSMOS-04 | Schmale Fehlermeldung verwendet Card-Verlauf; Schließen erreichbar, gemeinsame Home-Palette |
| COSMOS-05A–C | Home/Avatar-Popup und Adminnavigation bei drei Größen; Allgemein sowie Benutzerformular innerhalb der Fensterbreite |
| UI-03A–F | Reale Loginbedienung bei fünf Größen einschließlich 360×240, Fehler/Korrektur, Remember-me-Auswahl, Anmeldung und reduzierte Bewegung |

JUnit prüft zusätzlich Copyright-Defaults/Erhalt gespeicherter Werte, sichere Textanzeige,
Notification-Scope sowie wechselnde Registrierungsplatzhalter ohne Verlust der Formulardaten.
Die Anmelde-/Registrierungs-/Tokenaktionen und die Security-Konfiguration wurden gegen
den Ausgangsstand geprüft und blieben unverändert. Der Browser prüft auch die bisherigen
fachlichen Konto-, Admin-, Mail-, Status- und Sicherheitsabläufe.

Sterne behalten 30/45 Sekunden, Nebel 18–24 Sekunden als Animationszeiten.
Reduzierte Bewegung deaktiviert die Animation; kurze Fenster dürfen vertikal scrollen.
Die 100 Browserfälle prüfen konkrete UI-Abläufe. Menschliche Beurteilung und offene
Betriebs-/Integrationsfolgen werden im [neuen Handprüfkatalog](../docs/manual-tests/README.md)
geführt; eine erfolgreiche Browser-Fallzahl bedeutet keine vollständige Betriebsabdeckung.

## Einheitliche Datum-/Uhrzeitbedienung

DatePicker in Registrierung und Benutzerformular sowie Date-/TimePicker im
Aktivitätsfilter verwenden `autoOpen=false`. Direkte Eingabe öffnet kein Overlay;
Icons und die vorgesehenen Pfeiltasten bieten weiterhin Auswahlmöglichkeiten.

Der Geburtstagsplatzhalter entspricht der Registrierung und folgt der Sprache.
Im Aktivitätsfilter ist der Datumsplatzhalter das heutige Datum im DE-/EN-Format;
der Zeitplatzhalter entspricht der aktuellen Uhrzeit, abgerundet auf 15 Minuten.
Die Uhrzeit ist optional und nur bei gültigem Datum bedienbar. Ohne gültiges Datum
wird sie deaktiviert. Öffnen und Scrollen der Zeitliste übernimmt keinen Wert.
Der Geburtstag bleibt beim DE↔EN-Wechsel im Anlegen-/Bearbeiten-Dialog erhalten
(technische Regression).

UI-01A/B bestätigen direkte Geburtstagseingabe, Kalenderauswahl/Wiederöffnen und
tatsächliche Registrierung bei 390×844 bzw. 844×390. USER-05B/C bestätigen
kombinierte Filter, direktes Datum, Kalenderauswahl, optionale Uhrzeit und Clear.
Morgen liefert keine Treffer, gestern zeigt das eigene Konto; beide Zeitvarianten
werden über echte Bedienung übernommen. Vaadin darf führende Nullen beim Blur entfernen;
entscheidend ist das exakt geprüfte kanonische Datum und das tatsächliche Filterergebnis.

Der projizierte Kalenderinhalt liegt im DOM beim DatePicker. Der Browserhelfer
verwendet zugängliche Tageszellen mit dem öffentlichen `date`-Part; die virtualisierte
Uhrzeitliste wird über echte Mausbewegungen gescrollt. Anwendungszustände werden
nicht über SQL-/Service-/API-Abkürzungen erzeugt. Echte Geräte und Bildschirmtastaturen
bleiben ergänzende Prüfungen.

## Aktuelle Benutzerverwaltungsfälle

| ID | Aussage | DE-Nachweis | EN-Nachweis |
| --- | --- | --- | --- |
| USER-05A | Zwölf eigene Konten, Suche, exakte Treffer auf zwei Seiten, Seitengröße und Leerzustand | Bestanden | Bestanden |
| USER-05B | Kombinierte Freigabe-/Status-/Rollenfilter, direktes Datum mit optionaler Zeit, Entfernen/Clear | Bestanden | Bestanden |
| USER-05C | Kalender-Icon, Tagesauswahl/Wiederöffnen und optionale Dropdown-Zeit | Bestanden | Bestanden |
| USER-06A | Zentrale Validierung, Dublettenkorrektur, tatsächliche Anmeldung des neuen Kontos | Bestanden | Bestanden |
| USER-06B | Namen/E-Mail speichern, erneut öffnen, Passwort leer unverändert lassen | Bestanden | Bestanden |
| USER-06C | Löschabbruch, Bestätigung und dauerhaftes Nichtvorhandensein | Bestanden | Bestanden |
| USER-06D | Selbstlöschschutz im eigenen Adminformular | Bestanden | Bestanden |
| USER-06E | Suche und Freigabefilter beim Sprachwechsel in beide Richtungen erhalten | Bestanden | Bestanden |
| USER-06F | Neues Passwort speichern, altes abweisen und mit neuem anmelden | Bestanden | Bestanden |
| USER-10 | Gesamtzahl nach Anlage/Löschung, unabhängig von Filtern und mit korrektem Sprachwechsel | Bestanden | Bestanden |
| USER-11A | Filterentwürfe, Anwenden, Panelzustand und einzelnes Badge-X ohne Übernahme anderer Entwürfe | Bestanden | Bestanden |
| USER-11B | Datumabhängige Zeit, aktueller Platzhalter, begrenztes Overlay und Scrollen ohne Auswahl | Bestanden | Bestanden |
| USER-12A | Optionale Passwortbestätigung, exakter Vergleich, Fehler erst nach Blur und Rücksetzen | Bestanden | Bestanden |
| USER-12B | Sichere Schließen-Aktionen, neue Löschtexte und Erhalt des ausgewählten Kontos | Bestanden | Bestanden |
| USER-07F | Ausschließlich Superadmin: gespeicherte Rolle, Adminlogin, Home-Verwaltungsaktion und eigenes Formular speichern | Bestanden | Bestanden |
| USER-07G | Ausschließlich Gast: gespeicherte Rolle, Anmeldung/Home, keine Adminaktion und kein Adminzugriff | Bestanden | Bestanden |

Suche/Pagination, Filter, Anlage, Bearbeitung, Löschabbruch/-bestätigung,
Selbstlöschschutz, Sprachpräferenz und Passwortwechsel bestehen jetzt in beiden
Gesamtläufen. Der Editierbutton wird vor dem echten Mausklick sichtbar gemacht und
fokussiert, damit die native Grid-Scrollbewegung den Klick nicht verliert. Erwartungen,
Speicher-/Anmeldeprüfungen und Sicherheitsregeln bleiben erhalten.

In der vollständigen Theme-Prüfung bestanden jeweils alle damaligen 100 DE-/EN-Browserfälle.
Die nachfolgende gezielte Rollenprüfung und ihre Grenzen stehen oben.
Noch offene ergänzende Prüfungen stehen im Abschnitt „Ergänzende Handprüfungen“ unten.

## Ergänzte Arbeitspakete

| Paket | Neue UI-Aussagen | Suite / IDs |
| --- | --- | --- |
| 1 | Sperren/Freigeben, gezieltes Aktivieren unverifizierter Konten, Rollenvergabe/-entzug, Superadmin/Gast, echte zwei alte Formulare und Selbstschutz | `user-access`: USER-07A–G |
| 2 | Alte privilegierte Callbacks nach Rollenentzug/Sperre/Löschung; alte Verifikations- und Resetbuttons; neutrale gesperrte Anfrage | `session-security`: SESSION-01A–E (einschließlich Remember-me bei Sperre/Löschung); `blocked-links`: SEC-LINK-01A/B und SEC-LINK-02A/B, RESET-03C |
| 3 | Echter Browserneustart mit/ohne Remember-me und nach Logout; Statuswahl, zweiter Tab, automatische Abwesenheit/Rückkehr | `remember-me`: AUTH-07A/B; `user-status`: STATUS-01/02 |
| 4 | Weitere String-/Boolean-/Integerkonfiguration, Unicode-Testmail, echte Tokenabläufe/Resend und Verifikationsanfragefehler | `configuration`: CONFIG-04A–C; `admin-mail`: MAIL-01; `token-expiry`: VER-03A, RESET-06A, VER-02B |

Jeder Fall enthält eigene Schritte und Erwartungen im erzeugten Markdown-Bericht.
Implementierung allein zählt nicht als erfolgreicher Nachweis; Resultate stehen oben.
Die Oberfläche bereitet sämtliche Anwendungszustände vor. Mailpit liefert nur die
eigene tatsächlich versandte SMTP-Mail. Interne Regeln bleiben technische Tests.

## Noch sinnvoll als nächster Schritt

Die vier UI-Pakete sind geprüft. Als Nächstes bieten sich kontrollierte
Betriebsfixtures an: der zusätzliche Bestandsabgleich bei deaktiviertem Bootstrap,
SMTP-Stopp und Wiederanlauf sowie deren Bericht. Echte Serverneustarts mit derselben
eigenen Datenbank sind für Login und gespeicherte Anmeldungen inzwischen automatisiert. Weitere Browserpakete betreffen Sprachwechsel bei sichtbaren Admin-Feldfehlern,
neue Mails desselben Kontos nach Sprachwechsel und unterbrochene Verbindungen. Schmale Home-/Adminansichten sind durch
COSMOS-05A–C, der mobile Konfigurations-Tooltip durch CONFIG-05F geprüft.
Die fünf Kontoansichten einschließlich Reset sind inzwischen responsiv geprüft.
Queue-/Audit-Innenzustände werden dafür nicht in gewöhnliche UI-Tests gepresst.

## Grenzen der Testarten

- Regeln, Transaktionen, Sperren, Parallelität, exakte Zeitgrenzen und interne
  Queue-/Audit-Zustände bleiben in JUnit-/Persistenztests. Ein UI-Erfolg beweist sie nicht vollständig.
- Der zusätzliche Datenbestandsabgleich nach Neustart ohne Bootstrap, kontrollierter
  SMTP-Ausfall/Wiederanlauf, Retrylimit, unklarer Versand-/Commitausgang und Recovery
  benötigen eigene begrenzte Betriebsfixtures. Login und gespeicherte Zugänge nach
  echtem Serverneustart sind bereits automatisiert. Keine SQL-/Service-Abkürzungen zum Vorbereiten von Browserzuständen.
- Anonyme Login-/Registrierungs-/Tokenformulare bieten derzeit keinen Sprachschalter.
  Dynamischen Wechsel dort nicht durch künstliche Session-/JavaScript-Ereignisse ersetzen.
- Echte Geräte, Bildschirmtastatur, Zoom, Accessibility, visuelle Qualität und
  menschliche Recovery-Entscheidungen benötigen ergänzende Sonderprüfungen.
- Mailpit belegt lokale SMTP-Zustellung, keine externe Postfachzustellung oder
  Darstellung in Outlook/Gmail. Timing-Schutz, Mehrinstanzbetrieb und externer Betrieb sind nicht belegt.

## Ergänzende Handprüfungen

Der aktuelle [Handprüfkatalog](../docs/manual-tests/README.md) enthält ausschließlich
menschliche Abnahmen und noch offene ergänzende Testfolgen. Bereits automatisierte
Normalabläufe werden dort nicht nochmals als Handtest geführt. Eine Mail oder ein
Testkonto darf als Vorbereitung vorkommen; geprüft wird nur die benannte Restlücke.

| Restumfang | Fälle im Handprüfkatalog | Nächster geeigneter Nachweis |
| --- | --- | --- |
| Gestalterische Qualität, echtes Mobilgerät, Tastatur/Zoom, Screenreader, Texte und tatsächliche Mailclients | HUM-01–06 | Menschliche Beurteilung auf den angegebenen Geräten/Hilfsmitteln/Clients |
| Recovery bei unvollständigen Versandbelegen | HUM-07 | Menschliche Schreibtischübung zum Recovery-Vertrag, keine Datenänderung |
| Zusätzlicher Bestandsabgleich nach Neustart ohne Bootstrap und Wirksamkeit startgebundener SMTP-Änderungen | AUTO-01/02 | Zur Automatisierung vorgesehen – kontrollierte Betriebsfixture |
| Realer SMTP-Stopp/Wiederanlauf, begrenzter dauerhafter Fehler und brauchbare Diagnose | AUTO-03/04 | Zur Automatisierung vorgesehen – Betriebsfixture mit korrelierten Belegen |
| Sichtbare Feldfehler beim Sprachwechsel, neue Mails nach Sprachwechsel desselben Kontos und Offline-/Online-Folge | AUTO-05–07 | Zur Automatisierung vorgesehen – echte UI-/Browserfälle |
| Vorgesehener SMTP-Transport einschließlich Auth/TLS und tatsächlichem Testpostfach | AUTO-08 | Zur Automatisierung vorgesehen – gesonderte freigegebene Integrationsfixture |

Diese Zuordnung ist ein offener Prüfauftrag, kein bestandener Nachweis. Die neue
[leere Vorlage](../docs/manual-tests/vorlage.md) enthält Schritte und Erwartungen;
das [ausgefüllte Lernbeispiel](../docs/manual-tests/vorlage_ausgefüllt.md) enthält
**ausschließlich erfundene** Ergebnisse und Korrekturen. Fehlende Werkzeuge/Belege
werden als BLOCKIERT behandelt, nicht als bestätigter Anwendungsfehler.

Bereits durch JUnit/Persistenz belegte interne Zustände, exakte Ablaufgrenzen,
Cleanupregeln und Konkurrenz werden nicht zu manuellen Datenbankprüfungen dupliziert.
Bestandsmigrationen, Mehrinstanzbetrieb und unklare Transport-/Commitausgänge bleiben
in ihren eigenen technischen/Betriebsanleitungen und benötigen passende Fixtures;
der neue kurze Katalog behauptet dafür keine vollständige Freigabe.

## Belege und Pflege

Hier nur den aktuellen Prüfstand, Fallzuordnungen und offene Grenzen pflegen.
Wichtige Abschlüsse eigenständig lesbar unter [Meilensteine](../docs/meilensteine/README.md)
festhalten: Datum, geprüfter Stand, Ergebnisse, Coverage und wesentliche Erkenntnisse.
Keine konkreten lokalen Run-Pfade oder Listen früherer Testläufe als dauerhafte Belege eintragen.

Lokale Laufordner mit Screenshots, Traces, JARs und HTML-/Coverage-Berichten dürfen
nach Auswertung bereinigt werden. Ihre dauerhafte Aufbewahrung oder vollständige
Sicherung ist nicht erforderlich. Die Links innerhalb eines Run-Berichts gelten nur,
solange der zugehörige Ordner vorhanden ist. Keine automatische Löschung ohne Auftrag.

Die Weiterleitung unter `docs/browser-tests/README.md` verweist weiter auf die
aktuelle Browseranleitung. Die manuelle Ergänzung wird ausschließlich im
[Handprüfkatalog](../docs/manual-tests/README.md) gepflegt, ohne alte Run-Verweise.

## Profilbilder: Implementierungsstand 17.10.2026

Upload, Ausschnitt, Speichern/Ersetzen/Löschen, Initialen-/Bildwechsel, geschützte
Bildauslieferung und Profilaktualisierung sind implementiert. Technisch geprüft:
512 gezielte JUnit-Fälle plus 48 erfolgreiche Nachprüfungen der zuletzt angepassten
Dateien und vier JavaScript-Offlinetests. Grenzen und genaue Regeln stehen im
[Profilbildvertrag](../docs/profile-avatar-contract.md).

Nach dem manuellen Uploadbefund wurden die ungesperrten UI-Zugriffe bei der
JPA-Benutzerinitialisierung und beim Anmeldungsfallback behoben. Die folgende gezielte
Regression bestand mit 210/210 JUnit-Fällen. Der neue Nachweis führt Vaadins RAW-Handler,
Speichern und DownloadHandler gegen frisches H2 mit echter VaadinSession und strenger
Sperrprüfung aus; er prüft außerdem die Bildverweigerung nach Kontosperre. DE/EN bei
Registrierung und Admin-Neuanlage behalten ihre gewählte Sprache. Kein Browserlauf;
die visuelle Upload-/Crop-Abnahme bleibt offen.

Für diese Erweiterung ausdrücklich **kein Browserlauf**. Der bestehende INFRA-01
wurde an gerenderte Initialen statt feste Avatar-Bilddateien angepasst, aber noch
nicht neu ausgeführt. Upload-/Crop-Reisen DE/EN, Touch/Handyfotos, mehrere Fenster
und ein tatsächlicher App-Neustart mit Bild bleiben für die nächste freigegebene
Browser-/menschliche Abnahme offen. Alte Browserergebnisse oder JaCoCo-Gesamtwerte
belegen diese neue Funktion nicht; daraus wird keine neue Coverage abgeleitet.
