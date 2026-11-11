# Große Testrunde vom 11.11.2026

Geprüft wurde der Stand dieses Commits auf
`codex/vaadin-komponentenanpassungen` gegenüber dem letzten vollständigen
Prüfstand auf `main`, `9c0d53e8`. Der Umfang enthält die Vaadin-Badges,
Auswahlfelder und Kalender, den Schutz ungespeicherter Formulare sowie die
eigene Kontodeaktivierung, Reaktivierung und endgültige Löschung. Die drei
Umsetzungspakete wurden getrennt committet. Die Abschlusskorrekturen und
Prüfnachweise gehören zu diesem Stand.

| Prüfung | Ergebnis |
| --- | --- |
| Optimierter Maven-Reactor mit Java 25 und Coverage | Bestanden |
| Spotless, Checkstyle und Diff-Prüfung | Bestanden |
| Anwendungstests | 2.816 bestanden, zwei optionale Caddy-Tests übersprungen |
| Browser-Helfertests | 98 bestanden |
| Frontend-Offlinetests | 20 bestanden |
| Coverage- und Sonar-Offlineprüfungen | 18 und 47 bestanden |
| Wirksames Maven-Modell und Coverage-Prüfer | Bestanden, App rund 84,9 %, Shared 100 % Zeilenabdeckung |
| Browser `all` DE | 274 von 274 bestanden, keine Fehler oder Skips |
| Browser `all` EN | 274 von 274 bestanden, keine Fehler oder Skips |
| SonarQube-Analysestand | Nicht bestätigt, authentifizierte Anbindung hier nicht verfügbar |

Beide vollständigen Browserläufe verwendeten je vier getrennte Anwendungs-,
H2-, HTTPS- und Mailpit-Umgebungen mit demselben unveränderten Image
`sha256:d6992f888e037f71573cf5f000e4bcc99c5dd2d0309fcb6a0384bd83a39d60e9`.
Aufbau und Abbau waren erfolgreich. Die Anwendungsstarts und das erweiterte
Schema wurden dabei mit frischen isolierten Daten geprüft. Bestehende
Entwicklerdaten und Dienste dienten nicht als Testfixture.

## Korrekturen und ergänzte Abdeckung

Die erste Beschriftung von „Konto verwalten“ fehlte, weil der Tab erst nach dem
gesammelten Textaufbau ergänzt wird. Er erhält seinen übersetzten Text jetzt
sofort und behält die Aktualisierung bei Sprachwechseln. Der Deaktivierungsdialog
verwendet den vorhandenen Cosmos-Dialograhmen und den gemeinsamen Textgrößen-Token.
Der native Bestätigungsdialog zum Verwerfen von Änderungen übernimmt ebenfalls
die bestehenden Flächen, Abstände, Schriftwerte und Buttonvarianten. Der sichere
Anfangsfokus bleibt auf „Weiter bearbeiten“.

Die Browserfälle prüfen Profilentwürfe, allgemeine Konfiguration sowie Anbieter-
und 2FA-Entwürfe bei Desktop- und Smartphonebreite. Die Kontoverwaltung ist mit
falschem und richtigem Portalpasswort, Ersatzcode, empfangener Reaktivierungsmail
und Löschbestätigung abgedeckt. Die JUnit-Persistenztests prüfen unter anderem
festgeschriebene Fristen, einmalige Erinnerung, Ablauf, Admin-Sperre,
Superadmin-Schutz und die Bereinigung zugehöriger Daten und Sitzungen.

Veraltete Testannahmen zur Tabanzahl, Feldanzahl, Badgefarbe und zum Verlassen
bearbeiteter Formulare wurden an die vereinbarten Verträge angepasst. Die
Wartelogik erkennt den tatsächlichen Aktionsabschluss oder den Schutzdialog.
Ein Anbieterfall tippte vor dem vollständigen Öffnen des Dialogs. Er wartet jetzt
auf Ende der Öffnungsanimation und den initialen Fokus. Die korrigierten Abläufe
sind in beiden vollständigen Sprachläufen erfolgreich. Die geänderten Klassen
wurden einzeln auf Lesbarkeit geprüft.

## Grenzen und offene Abnahme

SonarQube ist erreichbar. Die ausschließlich lesenden Abfragen für Analyse,
Findings und Quality Gate erhielten ohne verfügbare authentifizierte Verbindung
HTTP 401. Es wurde kein neuer Scan und keine Statusänderung ausgeführt. Die
vorherige bestätigte Analyse auf `main` deckt diese neuen Pakete nicht ab.
Die Funktionstests sind vollständig erfolgreich, ein aktuelles Sonar-Gate wird
damit nicht behauptet.

Der Formularschutz greift bei interner Navigation, den Neu-laden-Aktionen der
Konfiguration und dem Schließen der Benutzerformulare. Ein Browser-Refresh oder
das Schließen des Browser-Tabs wird davon nicht abgefangen. Die Browsernachweise
verwenden Chromium mit Desktop- und Smartphonebreiten.

Der [Testbedarf](testbedarf.md) führt den Nachtest der noch nicht umgesetzten
Erweiterung des Formularschutzes auf Browseraktionen als wartend auf.
Die gemeinsame Sichtung der drei Pakete steht im
[Komponentenplan](planung/vaadin-komponenten-abgleich.md#offene-abnahme-der-portalpakete).
Menschliche Geräte-, Darstellungs- und Mailclientprüfungen bleiben im
[manuellen Katalog](manual-tests/README.md). Für die lokale Sichtung des erweiterten
Datenmodells sind eine frische Entwicklungsdatenbank und ein Anwendungsneustart nötig.
