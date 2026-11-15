# Große Testrunde vom 14.11.2026

Die Formularpakete sind auf `codex/vaadin-komponenten-folgearbeiten` umgesetzt,
jeweils reviewt und separat committed. Vergleichsbasis ist die letzte vollständig
belegte QA bei `7d127bcb`. Die Anwendungsquellen des Gesamtlauf-Images entsprechen
`487f9253`; bis zum Abschluss dieser Gesamtrunde wurden danach ausschließlich
Browsertests und Dokumentation korrigiert.
Der Testcode dieser Gesamtrunde steht bei `240c1008`.

**Die unten aufgeführten automatisierten Tests sind erfolgreich abgeschlossen.**
Der aktuelle Feature-Branch hat noch keinen zugeordneten Sonar-Scan.
Die Formularlayouts und die nachfolgende öffentliche Card-Übernahme sind vom Benutzer abgenommen.
Weitere offene Pakete stehen im [Komponentenplan](planung/vaadin-komponenten-abgleich.md).
Kein Merge, Push oder Release wurde durchgeführt.

## Ergebnisse

Die gemeinsame native Kartenbasis für Konto, geschützte Zwischenansichten,
Administration und Tickets ist anschließend gezielt geprüft worden. Der optimierte
isolierte Image-Build einschließlich Anwendungstests sowie 128 Browser-Helfertests
sind bestanden. Je 18 Browserfälle vor und nach der Übernahme sind in DE und EN
auf Desktop und Smartphone bestanden. Je Sprache stimmen 44 Karten- und 68
Formular-Messwertsätze bei Größen, Innenabständen, Farben und Schriftwerten überein;
Vorher-/Nachher-Aufnahmen liegen zur menschlichen Sichtung bereit. Die technische
Änderung des berechneten Card-Gaps von `normal` auf `0px` verändert die Maße nicht.
Ältere Testbaumsuchen wurden an die öffentlichen Card-Slots angepasst.
Diese Zwischenprüfung ersetzt die unten dokumentierte letzte Gesamtrunde nicht;
Innen-/Kontextkarten, übrige Bestätigungen und der vollständige Bereinigungslauf
werden im freigegebenen Nachtauftrag noch gesondert umgesetzt.

| Prüfung | Ergebnis |
| --- | --- |
| Vollständiger Maven-Reactor mit Java 25, Produktion und Coverage | Bestanden |
| Spotless, Checkstyle, wirksames Maven-Modell und Coverage-Prüfer | Bestanden |
| Anwendungstests | 3.005 bestanden, zwei bewusst optionale Caddy-Tests übersprungen |
| Browser-Helfertests | 127 bestanden, keine Fehler oder Skips |
| Frontend-Offlinetests | 20 bestanden |
| Coverage-/Sonar-Offlineprüfungen | 18 beziehungsweise 49 bestanden |
| Browser `all` DE | 328 von 328 bestanden, keine Fehler oder Skips |
| Browser `all` EN | 328 von 328 bestanden, keine Fehler oder Skips |
| Aufbau, isolierter Start, HTTPS, Abbau und HTML-Berichte | In beiden Gesamtläufen bestanden |
| Anmeldung und Seitenpfade nach Korrektur der UI-Wartebedingung | Je zehn Fälle DE/EN bestanden |
| Vorher-/Nachher-Formularvergleich | Je zwölf Fälle vor und nach der Umstellung in DE/EN bestanden |
| Sonar | Verfügbarer main-Stand lesend geprüft; keine Analyse des aktuellen Feature-Branchs |

Der Produktions-/Coverage-Reactor der Gesamtrunde prüfte die anschließend als `240c1008`
committete Testkorrektur. Der deutsche Gesamtlauf gehört zum Testcode `48131d95`;
die danach geänderte Anmelde-Wartebedingung ist zusätzlich mit den genannten
DE-/EN-Nachtests belegt. Der abschließende englische Gesamtlauf gehört zu
`240c1008`. Die Anwendungsquellen blieben dabei unverändert.
Die Offlineprüfungen gehören zu ihren unveränderten JavaScript-/Pythonquellen.
Überlappende Paket-, Vergleichs- und Nachtests werden nicht zu den Zahlen des
Reactors oder der vollständigen Browserläufe addiert.

Die Browserprüfungen verwenden dasselbe unveränderte Anwendungsimage:

`sha256:c783c1b268be0cebbc891410dbccc05c34cda864a5f03d1a4ceeec440ede002e`

Die Gesamtläufe verwenden jeweils vier getrennte Browser-, Anwendungs-, H2-,
HTTPS- und Mailpit-Umgebungen mit frischen Daten und aktuellem Schema. Bestehende
Entwicklerdaten, gespeicherte Passwörter, echte Mailkonten und die laufende
IntelliJ-Anwendung dienen nicht als Fixtures.
Alle acht Worker meldeten erfolgreiche Tests und erfolgreichen Abbau.

## Gezielte Komponentenprüfungen nach der Gesamtrunde

Die elf öffentlichen Standardseiten verwenden nun gemeinsam `PublicPanelComponent`
auf nativer Vaadin-`Card`; die Google-/Microsoft-Nebenkarte verwendet ebenfalls
`Card`. Kopf, Inhalt und Footer bleiben in ihren bisherigen Cosmos-Zonen.
Der separate Pilotbaustein ist entfallen. Besondere Konto-/Ticketkarten für 2FA,
Wiederherstellungshilfe und Sitzungslimit sind nicht Teil dieses Pakets.
Die öffentliche Card-Übernahme ist vom Benutzer abgenommen.

Ein vollständiger Reactor-Testlauf bestand mit 3.005 Anwendungstests und 127
Browser-Helfertests; die zwei ausdrücklich optionalen Caddy-Tests waren übersprungen.
Die abschließende Ausrichtung der Überschrift-Boxen bestand zusätzlich mit 60
gezielten Anwendungstests. Der abschließende Änderungsreview bestätigte diese
60 Tests erneut; Namen und Absätze der betroffenen Testhelfer wurden präzisiert.
Spotless, Checkstyle und der optimierte Docker-Build
mit `clean verify` waren erfolgreich. Die vier vorhandenen JUnit-Baumsuchen
berücksichtigen nun über `ComponentTestTree` auch die öffentlichen Card-Slots;
keine bisherigen Aktions- oder Sprachwechselprüfungen wurden abgeschwächt.

Die Suite `public-panels` bestand vor und nach der Umstellung mit jeweils
14 Fällen in DE und EN, jeweils bei 1280/390 px. Alle vier Läufe hatten
erfolgreichen Aufbau, Abbau und HTML-Bericht, keine Fehler oder Skips.
Die beiden Nachher-Läufe verwendeten dasselbe unveränderte Image:

`sha256:52b822de424b035239783fc1a7cc3390fae5618096a5d77a1a5b8b42a3f831ef`

In allen 68 Messpaaren stimmen Positionen, Größen, Schriften, Farben, Rahmen
und Innenabstände überein. 64 vollständige Aufnahmepaare sind pixelidentisch.
Die übrigen vier enthalten ausschließlich unterschiedliche Löschzeitpunkte der
jeweils neu angelegten selbstdeaktivierten Testkonten. Die Vergleiche umfassen
Anmeldung, Registrierung samt Anbieterkarten, externe Erstregistrierung,
Passwortanfrage/-Reset mit Formular- und Linkfehlern, Verifikation, ausstehende
Verifikation und Reaktivierung, E-Mail-Änderungslink und Wartung.
Tatsächliche UI-Aktionen und eigene Mailpit-Links belegen die zugehörigen Abläufe.

Diese Nachprüfung ersetzt nicht die obigen vollständigen 328er-Browserläufe.
Das Ausgangsimage für die Vorher-Aufnahmen war der abgenommene Pilotstand
`sha256:88d00ed40c19d85317df1b51a9f6e7e98e5a04c6002a4ef6e236b57c3da522bc`.
Der unten ausgewiesene Sonar-Nachweis umfasst die Card-Übernahme nicht.

Der nachfolgende `ConfirmDialog`-Pilot betrifft ausschließlich das Sammellesen
in Inbox und Glockenvorschau. Die Suite `confirmation-dialog` bestand vor und
nach der Übernahme mit je zwei Fällen in DE und EN bei 1280/390 px. Die finalen
Nachher-Läufe verwenden dasselbe Image
`sha256:4d4581a24850c80102f8703855bbcdaf709523fe442f472e3150c9dc1ac15e5f`.
Alle acht Dialogvergleiche mit insgesamt 88 sichtbaren Element-Messpaaren haben
gleiche Positionen, Größen, Farben und gemessene Textstile. Die Aufnahmen sind
wegen kleiner Rasterunterschiede nicht pixelidentisch. Anfangsfokus auf Close,
Tastaturreihenfolge, Abbrechen, Close, Escape und Außenklick sowie die erst nach
Bestätigung dauerhafte Lesemarkierung sind geprüft. Die eigentliche Leseaktion
behält ihren beim Öffnen erfassten Snapshot.

164 gezielte Anwendungstests und 34 Browserauswahltests bestanden, einschließlich
der neuen Regressionen zur nativen Ereignisreihenfolge und zum einmaligen
Ausführen beziehungsweise Verwerfen der Aktion. Spotless und Checkstyle waren
erfolgreich. Das generierte Produktionsbundle enthält jetzt auch die native
ConfirmDialog-Komponente. Die Browserbelege gehören zum Stand vor der abschließenden
serverseitigen Absicherung gegen ein verspätetes ClosedEvent nach erneutem Öffnen;
dieser Randfall ist zusätzlich mit JUnit abgesichert. Die Darstellung blieb unverändert.
Diese gezielte Nachprüfung ersetzt keine große Testrunde und keinen Sonar-Nachweis.
Der Pilot ist vom Benutzer abgenommen.

Die nachfolgende URL-Korrektur betrifft ausschließlich die Mailqueue-Reiter:
`admin?section=configuration&group=queue&tab=general` beziehungsweise
`tab=assistants`. Die Auswahl wird beim Direktaufruf aus der URL wiederhergestellt;
ein Reiterwechsel aktualisiert die Adresse ohne Navigation oder Neuladen der
Formularwerte. Fehlende, ungültige oder mehrdeutige Reiter und fehlende
Assistentenrechte führen zu „Allgemein“.
47 Navigationstests und 34 Konfigurationsgruppentests bestanden, ebenso Spotless
und Checkstyle. Abgedeckt sind auch das erste Einhängen der Ansicht nach
URL-Wiederherstellung, URL-Aktualisierung, Rechteentzug und Entwurfserhalt.
Der Benutzer hat ausschließlich JUnit-Prüfungen freigegeben; es gab für diese
Korrektur keinen Browserlauf und keinen Anwendungsstart. Der tatsächliche
F5-/Direktlink-Nachtest in DE/EN auf Desktop und Smartphone bleibt im
[Testbedarf](testbedarf.md) offen. Die früheren Browserbelege prüfen diese
nachfolgende Änderung nicht; ihre menschliche Abnahme steht ebenfalls noch aus.

Die Gestaltung der manuellen Mailqueue-Bereinigung folgt anschließend der
freigegebenen Stitch-Variante A: Brush-Icons für äußere Aktion, Dialogkopf und
Bestätigung, „Manuelle Bereinigung“ als äußerer Buttontext und Dialogtitel,
„Jetzt bereinigen“ als Bestätigung und eine blaue Statusüberschrift.
Eine gelbe Warnung zum möglichen späteren Empfang und eine separate Infobox
ersetzen die bisherigen wiederholten Hinweise. DE-/EN-Texte verwenden den
gemeinsamen typisierten Katalog; die übrigen Bestätigungsdialoge wurden nicht geändert.

119 gezielte JUnit-Tests bestanden: sechs neue Komponentenfälle für DE/EN,
Sprachwechsel, Abbruch und einmalige Ausführung, die bestehenden Dialog-,
Katalog-, Navigations- und Konfigurationsfälle sowie elf isolierte
Mailqueue-Bereinigungstests. Der bestehende 101-Aufträge-Fall bestätigt die
unveränderte Begrenzung auf 100 mit verbleibendem Auftrag für den nächsten Lauf.
Spotless und Checkstyle waren erfolgreich. Der Benutzer hat Browserprüfungen
für diese Gestaltung ausdrücklich zurückgestellt; es gab weder Browserlauf noch
Start der Entwicklungsanwendung. Die Sichtung durch den Benutzer und die zentral
erfassten Browsernachtests stehen noch aus. Die spätere technische Übernahme
der Dialoghülle bleibt ein eigenes Nachtarbeitspaket.

Der anschließend separat beauftragte Hintergrundvergleich ist mit **Advanced**
(„Horizont Plus“) ausgewählt; **Base** erhält die vorherige Gestaltung.
Ein zentraler Wert wählt sowohl Stylesheet als auch dekorative Ebenen für den
gemeinsamen Seitenrahmen. Ein Vergleich bestätigte die unveränderte Base-CSS
abgesehen vom Kennzeichnungskommentar. Die Inhalts-Scrollflächen wurden nicht umgebaut.
123 gezielte JUnit-Tests für Ressourcen, Seitenrahmen, Kopfzeilen, Navigation und
Notification-Isolation bestanden, ebenso Spotless und Checkstyle.
Die drei betroffenen CSS-Dateien ließen sich mit PostCSS parsen; alle Advanced-Tokens
sind vorhanden. Die Zeigerparallaxe wurde zusätzlich offline mit künstlichen
Ereignissen auf Werte, reduzierte Bewegung, Szenenwechsel und Abmeldung geprüft.
Das ist kein gerenderter Browsernachweis. Öffentliche Rahmen tragen
`cosmos-page-frame`, jedoch nicht das zunächst verlangte `cosmos-page`.
Die Advanced-Selektoren verwenden den gemeinsamen Rahmen; der Benutzer hat den
Hintergrund auf der Startseite und nach der Korrektur auch auf der Anmeldung
bestätigt. Der konkrete Sichtbarkeitsfehler ist damit behoben. Automatisierte
Browsernachtests für die übrigen Ansichten, Größen und Bewegungseinstellungen
bleiben wartend im [Testbedarf](testbedarf.md); die früheren Browserbelege prüfen
diesen neuen Hintergrund nicht. Die laufende Anwendung wurde vom Agenten nicht
gestartet oder neu geladen. Die korrigierten Ressourcen wurden über den Reactor
aktualisiert; ein Wechsel der Variante braucht Build, Neustart und Refresh.

## Abdeckung und Korrekturen der Gesamtrunde

Die Gesamtsuite prüft die bestehenden Anmelde-, Konto-, 2FA-, Ticket-,
Benachrichtigungs- und Administrationsabläufe in DE/EN einschließlich Desktop-
und Smartphonebreite. Die Vorarbeiten seit dem Vergleichsstand umfassen native
Kennzahlkarten und Avatare, Formularlayouts, segmentierte Umschalter sowie
Korrekturen der Mailqueue-Reiter und des Einstellungszählers.

- **Formulare:** Allgemeine Konfiguration, Google/Microsoft, alle vier
  2FA-Rollenansichten, Mailtest, öffentliche Formulare und Kontodialoge verwenden
  passende native FormLayout-, Zeilen- und Spaltenlayouts. Bereits passende
  native Anordnungen und freigegebene Sonderlösungen bleiben erhalten.
  Der gemeinsame Feldhilfebaustein bewahrt Eingabebreiten und Hilfe-Ausrichtung.
  Der Vorher-/Nachher-Abgleich bei 1280 und 390 px enthält 136 Messpaare ohne
  Maßabweichung. Von 272 Bildpaaren sind 151 pixelidentisch; die übrigen enthalten
  unter anderem Fokus-, Hintergrundscroll- und Kantenglättungsunterschiede.
  Nicht alle Bilder sind pixelidentisch.
- **Regressionen:** Der Formularfall ergänzt vorhandene Speicher-, Rechte-,
  Fehler-, Fokus- und Sicherheitsprüfungen. Veraltete Feldhilfe-Locatoren wurden
  an die native Zeilenstruktur angepasst; Inhalts- und Geometrieprüfungen
  bleiben erhalten. Der feste Zeilenumbruch der Bereinigungsausgabe ist im
  bestehenden Queue-Test auf beiden Breiten abgesichert. Die gezielten
  Nachtests für 2FA-Konfiguration, Mailtest und Queue bestanden mit je neun
  Fällen in DE und EN.
- **Browserbereitschaft:** LoginPage wartet auf den HTML-Aufbau und anschließend
  auf das sichtbare Vaadin-Formular. URL-, Titel-, Feld- und Überschriftenprüfungen
  bleiben erhalten. Weder Assertions noch Timeouts wurden abgeschwächt;
  automatische Wiederholungen wurden nicht ergänzt.

Die vorherigen fehlgeschlagenen Läufe werden nicht als erfolgreiche Gesamtnachweise
gewertet. Sie zeigten drei veraltete Feldhilfe-Locatoren, eine durch den eigenen
Diagnosezugriff verursachte Windows-Dateisperre beim Berichtszusammenbau, einmalig
ERR_NO_BUFFER_SPACE in Chromium und ein ausbleibendes allgemeines load-Ereignis
bei bereits sichtbarer Anmeldung. Locatoren und Anmelde-Wartebedingung wurden
korrigiert; die Anleitung erklärt sperrfreie Diagnosezugriffe. Der betroffene
Profilfall bestand anschließend gezielt mit fünf Fällen. Eine grundsätzliche
Ursache des einmaligen Chromium-Netzwerkfehlers ist nicht belegt.
Der abschließende vollständige EN-Lauf bestand mit korrigierter Wartebedingung
am unveränderten Image; auch der zuvor betroffene Profilfall ist darin erfolgreich.

## Sonar

Die lesende Serverprüfung bestätigt für die vorhandene **main-Analyse vom
13.11.2026, 05:37 UTC** weiterhin Quality Gate OK, null offene Issues und Hotspots.
Diese Analyse gehört zu `7d127bcb`, nicht zu den aktuellen Formularänderungen.
Ein aktueller Sonar-Nachweis für den Feature-Branch ist daher **nicht bestätigt**.
Gemäß Projektablauf wurde auf dem Feature-Branch kein Scan hochgeladen und kein
Ersatzprojekt angelegt. Finding-Status und Regeln blieben unverändert.

[SonarQube-Projekt](http://localhost:9000/dashboard?id=vaadin-spring-app).

## Grenzen und nächste Sichtung

Nach den bisherigen Prüfungen meldete der Benutzer erneut fehlerhafte
Scrollbereiche. Betroffene Ansichten und Reproduktionsschritte sind noch zu klären;
der Fehler ist weder gezielt reproduziert noch behoben. Die bisherigen
Prüfergebnisse belegen keine Fehlerfreiheit für diese Regression.
Die Browsernachtests für Scrollbereiche und die danach mit JUnit geprüfte
Mailqueue-Reiterkorrektur stehen wartend im Testbedarf. Es gibt keine offenen
gezielten JUnit-Ergänzungen für den übrigen beauftragten Umfang.
[Testbedarf](testbedarf.md) und [BUGS](../BUGS.md) bleiben die zentralen Listen.
Formularlayouts und die öffentliche Card-Übernahme sind vom Benutzer abgenommen.
Kennzahlkarten, Avatare, Profil-/Benutzerformulare, segmentierte Umschalter,
Seitenpfade und die Mailqueue-Karte sind bereits vom Benutzer abgenommen.

Echte Mobilgeräte, Passwortmanager, Screenreader, reale Anmeldeanbieter und
Mailprogramme bleiben von isolierten Chromium-/OIDC-/Mailpit-Prüfungen getrennt.
Registrierung darf Passwortmanager und Kontaktdaten-Autofill nutzen; die Korrektur
im Benutzereditor ist vom Benutzer abgenommen.
[Manuelle Sonderprüfungen](manual-tests/README.md).

Ein unklarer SMTP-Ausgang ist kein sicherer Versandnachweis. Die Bereinigung
wiederholt solche Aufträge nicht; eine zuvor angenommene Mail kann später
eintreffen. Es gelten die [Mailqueue-Grenzen](mailqueue.md). Der Formularschutz
ist kein Autosave und schützt nicht zuverlässig vor Absturz oder erzwungenem
Prozessende.

Releases, Neuigkeiten, Passkeys, Hosting, die sichtbare Bereinigungshistorie und
weitere Komponentenpakete bleiben zurückgestellt; siehe [TODO](../TODO.md).
Die laufende Gesamtumsetzung ist nicht abgeschlossen. Der öffentliche Betrieb
benötigt die Vorarbeiten aus der [Hostingplanung](planung/oeffentlicher-testbetrieb.md).

Für die Übernahme der Formularänderungen in die lokale Entwicklungsanwendung
sind Build, Anwendungsneustart und Browser-Refresh nötig. Build und isolierte
Starts sind geprüft; die laufende Entwicklungsanwendung wurde nicht verändert.
Dasselbe gilt für die nachfolgende Übernahme der öffentlichen Cards.
