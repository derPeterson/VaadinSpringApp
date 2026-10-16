# Browserabdeckung und offene Punkte

**Aktueller Abschluss 17.10.2026: 1.298 Anwendungstests, 45 Browserhelfer,
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
Betriebsfixtures an: Neustart mit derselben eigenen Testdatenbank, SMTP-Stopp und
Wiederanlauf sowie deren Bericht. Weitere Browserpakete betreffen Sprachwechsel bei sichtbaren Admin-Feldfehlern,
neue Mails desselben Kontos nach Sprachwechsel und unterbrochene Verbindungen. Schmale Home-/Adminansichten sind durch
COSMOS-05A–C, der mobile Konfigurations-Tooltip durch CONFIG-05F geprüft.
Die fünf Kontoansichten einschließlich Reset sind inzwischen responsiv geprüft.
Queue-/Audit-Innenzustände werden dafür nicht in gewöhnliche UI-Tests gepresst.

## Grenzen der Testarten

- Regeln, Transaktionen, Sperren, Parallelität, exakte Zeitgrenzen und interne
  Queue-/Audit-Zustände bleiben in JUnit-/Persistenztests. Ein UI-Erfolg beweist sie nicht vollständig.
- Neustart mit derselben Datenbank, kontrollierter SMTP-Ausfall/Wiederanlauf,
  Retrylimit, unklarer Versand-/Commitausgang und Recovery benötigen eigene begrenzte
  Betriebsfixtures. Keine SQL-/Service-Abkürzungen zum Vorbereiten von Browserzuständen.
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
| Gleiche Datenbank nach Serverneustart und Wirksamkeit startgebundener SMTP-Änderungen | AUTO-01/02 | Zur Automatisierung vorgesehen – kontrollierte Betriebsfixture |
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
