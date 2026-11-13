# Große Testrunde vom 13.11.2026

Geprüfter Code: `7d127bcb34d6f0c1152ec58e4683a68f02556aaf`.
Die Nachtpakete und die reviewten Sonar-/QA-Korrekturen wurden lokal nach
`main` zwischenintegriert. Die Browserrunde lief auf
`codex/vaadin-komponenten-folgearbeiten` mit demselben unveränderten Codestand.
Vergleichsbasis der letzten vollständig belegten QA ist `9c0d53e8`.
Die nachträglich ergänzte zweizeilige Queue-Statusausgabe ist vom Benutzer auf
Desktop und Smartphone manuell abgenommen und mit drei erfolgreichen
Sprachkatalogtests geprüft. Der automatisierte Browsernachtest dieser kleinen
Änderung bleibt im [Testbedarf](testbedarf.md); die Gesamtläufe unten gehören
weiterhin zum oben genannten Codestand.

**Der technische Prüfabschluss ist keine Abnahme der laufenden Gesamtumsetzung.**
Die bisherigen UI-Pakete und die externe Browser-Laufablage sind vom Benutzer
abgenommen. Die Gesamtumsetzung wird auf dem Feature-Branch fortgeführt;
zurückgestellte Pakete stehen im [Komponentenplan](planung/vaadin-komponenten-abgleich.md).
Kein Push oder Release.

## Ergebnisse

| Prüfung | Ergebnis |
| --- | --- |
| Optimierter vollständiger Maven-Reactor mit Java 25, Produktion und Coverage | Bestanden |
| Spotless, Checkstyle, wirksames Maven-Modell und Coverage-Prüfer | Bestanden |
| Anwendungstests | 2.988 bestanden, zwei bewusst optionale Caddy-Tests übersprungen |
| Browser-Helfertests | 123 bestanden, keine Fehler oder Skips |
| Frontend-Offlinetests | 20 bestanden |
| Coverage-/Sonar-Offlineprüfungen | 18 beziehungsweise 49 bestanden |
| Browser `all` DE | 312 von 312 bestanden, keine Fehler oder Skips |
| Browser `all` EN | 312 von 312 bestanden, keine Fehler oder Skips |
| Aufbau, isolierter Anwendungsstart, HTTPS, Abbau und HTML-Berichte | In beiden Gesamtläufen bestanden |
| Aktueller Sonar-Scan auf sauberem `main` | Quality Gate OK, null offene Issues und Hotspots |

Die Offlineprüfungen bleiben dem unveränderten jeweiligen JavaScript-/Pythoncode
zugeordnet; ihre Quellen wurden durch die anschließenden Java-/CSS- und
Berichtskorrekturen nicht verändert. Überlappende Paket- und Nachtests werden
nicht zu den Zahlen des vollständigen Reactors oder der Gesamtläufe addiert.

DE und EN verwendeten dasselbe unveränderte Image:

`sha256:471c9262c37ad2973d18ca0c7a1bae6a258a5a3b442b88d7891fc4494d422f07`

Jeder Lauf hatte vier getrennte JVM-, Browser-, Anwendungs-, H2-, HTTPS- und
Mailpit-Umgebungen. Die Anwendung startete jeweils mit frischen isolierten
Daten und dem aktuellen Schema. Alle acht Worker meldeten erfolgreiche Tests
und erfolgreichen Abbau. Bestehende Entwicklerdaten, gespeicherte Passwörter
und echte Mailkonten dienten nicht als Fixtures.

## Abdeckung und Korrekturen

Die Gesamtrunde schließt die bestehenden Konto-, Anmelde-, 2FA-, Ticket-,
Benachrichtigungs- und Administrationsfälle ein. Die UI-Fälle prüfen Deutsch
und Englisch einschließlich Desktop- und Smartphonebreite.

- **Mailqueue:** fünf allgemeine Felder, Superadmin-Assistent mit vier
  Einstellungen, disabled Zeitfelder bei ausgeschalteter Automatik,
  unabhängig bedienbare Historienfrist, erhaltene Entwürfe beim Tabwechsel,
  gemeinsames Speichern/Laden, Feldhilfen und bewusste Statusmeldungen.
  Monospace-Ausgabe, Aktionen und Kartenbegrenzung sind auf beiden Breiten
  geprüft; die aktuellen DE-/EN-Screenshots wurden angesehen.
- **Bereinigungshistorie:** manuelle und automatische Versuche, Nullergebnisse,
  laufender Versand, Intervallgrenzen, Rechte, Transaktionsabschluss und
  tägliche Aufbewahrungsbereinigung sind durch gezielte JUnit-Regressionen
  und den vollständigen Reactor abgedeckt. Keine Empfänger oder Mailinhalte
  werden in die Historie übernommen.
- **Externe Laufablage:** Pfadkonfiguration, ungültige Ziele, Pfadbegrenzung,
  Junctions, Launcher sowie Berichts-/Trace-Auflösung sind geprüft. Beide
  vollständigen Läufe samt Belegen und Berichten liegen am konfigurierten
  D:-Stamm. Die neuen Dokumentationslinks zeigen korrekt ins ursprüngliche
  Projekt; Belege bleiben relativ zum Laufordner. Alte C:-Läufe blieben erhalten.
  Buildausgaben und Maven-/Browser-/Docker-Caches bleiben an ihren bisherigen Orten.
- **Bestehender Komponentenstand:** Zeitfelder, segmentierte Auswahl,
  Seitenpfade, Formularschutz, Kontoaktionen, Verifikation/Reaktivierung,
  administrative Sperre gegenüber Selbstdeaktivierung sowie Anmeldeinfos
  sind im vollständigen aktuellen DE-/EN-Nachweis enthalten.

Der erste deutsche Gesamtlauf war fehlgeschlagen und wird nicht als grüner
Nachweis gewertet. Er zeigte einen zusätzlichen nativen Details-Außenabstand
im Benutzereditor, einen mehrdeutigen Überschriften-Locator, das Mitzählen
versteckter Feldhilfen und eine überlastete Worker-Gruppe. Die lokale
Details-Regel, die beiden Test-Locatoren und die gemessenen Laufzeitgewichte
wurden getrennt korrigiert und reviewt. Die vorhandenen Geometrie-/Feldprüfungen,
Assertions und das 60-Minuten-Gesamtlimit blieben erhalten.

Die gezielten DE-Nachtests bestanden mit 4 Fällen für Anmeldung/Sitzungen,
12 Komponentenfällen und 8 Feldhilfefällen. Anschließend bestanden beide
vollständigen Gesamtläufe am finalen Image. Ein weiterer Helfertest sichert
die Dokumentationslinks externer Berichte mit Leerzeichen und Umlauten ab.

## Sonar

Die vollständige Serveranalyse `83099a65-522a-4179-85b6-b2d8a23dac3b`
gehört genau zum oben genannten `main`-Code. Die abschließende lesende
Serverprüfung bestätigt **Quality Gate OK, null offene Issues, Bugs,
Schwachstellen und Hotspots**. Sonar-Gesamtcoverage: 82,4 %.

Von 60 ursprünglich offenen Hinweisen wurden 58 durch Codeänderungen in drei
reviewten Paketen behoben. Zwei Vererbungstiefen-Hinweise bei `AccountView`
und `AdminView` wurden mit fachlicher Begründung als Ausnahmen akzeptiert:
Die getrennten Sicherheits-, Rahmen- und Aktivitätsverträge sowie die
Vaadin-Vererbung bleiben erhalten. Diese beiden Fälle sind keine Codebehebungen.
Der Server führt insgesamt 46 bestehende beziehungsweise begründete Ausnahmen.
Keine Prüfregel wurde deaktiviert; Analyseumfang und Gate-Grenzen blieben erhalten.

Ein vorheriger Scannerabbruch durch Windows-Konsolencodierung wurde mit
UTF-8-Ausgabe und zwei Offline-Regressionen behoben. Das war kein Serverfinding.

[SonarQube-Projekt](http://localhost:9000/dashboard?id=vaadin-spring-app).

## Grenzen und nächste Sichtung

Aktuell bestehen keine bestätigten offenen Anwendungsfehler. Der automatisierte
Nachtest des nachträglichen Zeilenumbruchs steht im [Testbedarf](testbedarf.md).
[BUGS](../BUGS.md) und der Testbedarf bleiben die zentralen Stellen für neue Befunde.

Echte Mobilgeräte, Passwortmanager, Screenreader, reale Anmeldeanbieter und
Mailprogramme bleiben von den isolierten Chromium-/OIDC-/Mailpit-Prüfungen
getrennt. Die frühere Autofill-Korrektur im Benutzereditor ist vom Benutzer
abgenommen; Registrierung darf Passwortmanager und Kontaktdaten-Autofill nutzen.
[Manuelle Sonderprüfungen](manual-tests/README.md).

Ein unklarer SMTP-Ausgang ist weiterhin kein sicherer Versandnachweis.
Die Bereinigung wiederholt solche Aufträge nicht; eine zuvor angenommene Mail
kann später eintreffen. Es gelten die [Mailqueue-Grenzen](mailqueue.md).
Der native Formularschutz ist kein Autosave und schützt nicht zuverlässig
vor Absturz oder erzwungenem Prozessende.

Die drei Planungsaufgaben sind keine Umsetzung von Releases, Neuigkeiten,
Passkeys oder Hosting. Die Planungen sind durchgelesen und ihre Umsetzungen
zurückgestellt. Über einen ersten Release wird erst am Ende der laufenden
Gesamtumsetzung entschieden. Die neue Queue-Karte ist abgenommen.
Die sichtbare Bereinigungshistorie und zurückgestellte Komponentenpakete
bleiben in [TODO](../TODO.md).

Für die neue Queue-Oberfläche sind Build, Anwendungsneustart und Browser-Refresh
nötig. Der Build ist geprüft; die lokale Entwicklungsanwendung wurde nicht
als Testfixture gestartet oder verändert. Der öffentliche Betrieb ist noch
nicht freigegeben und benötigt die Vorarbeiten aus der
[Hostingplanung](planung/oeffentlicher-testbetrieb.md).
