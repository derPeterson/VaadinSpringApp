# Große Testrunde vom 14.11.2026

Die Formularpakete sind auf `codex/vaadin-komponenten-folgearbeiten` umgesetzt,
jeweils reviewt und separat committed. Vergleichsbasis ist die letzte vollständig
belegte QA bei `7d127bcb`. Die Anwendungsquellen des aktuellen Testimages entsprechen
`487f9253`; danach wurden ausschließlich Browsertests und Dokumentation korrigiert.
Der aktuelle Testcode steht bei `240c1008`.

**Die unten aufgeführten automatisierten Tests sind erfolgreich abgeschlossen.**
Der aktuelle Feature-Branch hat noch keinen zugeordneten Sonar-Scan.
Die technische Prüfung ersetzt außerdem nicht die offene menschliche Sichtung
der übrigen Formulare im [Komponentenplan](planung/vaadin-komponenten-abgleich.md).
Kein Merge, Push oder Release wurde durchgeführt.

## Ergebnisse

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

Der letzte vollständige Reactor prüfte genau die anschließend als `240c1008`
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

## Abdeckung und Korrekturen

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

Für den beauftragten Umfang gibt es keine offenen gezielten Browser- oder
JUnit-Ergänzungen und keine bestätigten offenen Anwendungsfehler.
[Testbedarf](testbedarf.md) und [BUGS](../BUGS.md) bleiben die zentralen Listen.
Die menschliche Sichtung der übrigen Formulare bleibt offen.
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
