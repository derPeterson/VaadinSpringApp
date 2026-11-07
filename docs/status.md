# Große Testrunde vom 07.11.2026

Geprüft wurde `main` auf `3afa7366881e21b34dc488c6ae053ff49d8abc4a`.
Der vorherige vollständig belegte Stand war `623feb67`. Dazwischen liegen die
2FA- und Ticket-Erweiterung, ihre Integration und die Sonar-Korrekturen.

## Ergebnisse

| Prüfung | Ergebnis |
| --- | --- |
| Optimierter Maven-Reactor unter JDK 25 | Bestanden mit gemeinsamer Java-Coverage und geprüftem Maven-Modell |
| Spotless und Checkstyle | Bestanden |
| Anwendungstests | 2.734 bestanden, zwei vorgesehene HTTPS-Opt-in-Fälle übersprungen |
| Browser-Helfertests | 96 bestanden |
| Frontend-JavaScript | 20 bestanden |
| Coverage- und Sonar-Offlineprüfungen | 18 und 47 bestanden |
| JaCoCo-Zeilenabdeckung | Anwendung 86,59 %, Shared 100 % |
| Browser DE | 253 von 253 bestanden |
| Browser EN | 253 von 253 bestanden |
| SonarQube | 0 offene Findings, 0 Hotspots, grünes Quality Gate |

Die beiden Browser-Gesamtläufe prüften dasselbe Image
`sha256:0d14953a0980095b9363f345061cff9ab76718e23c89887c3c6cc237bf9f0cf4`.
Je vier Worker nutzten getrennte Anwendungs-, H2- und Mailpit-Umgebungen.
Aufbau und Abbau waren in beiden Sprachen erfolgreich. Das JAR des Merge-Stands
startete zusätzlich mit einer eigenen In-Memory-H2-Datenbank bis HTTP 200.

Beim ersten vollständigen Build scheiterten lokale HTTP- und SMTP-Testverbindungen.
Die betroffenen Klassen bestanden im gezielten Nachlauf mit 17 von 17 Fällen.
Die anschließenden vollständigen Builds und der final geprüfte Stand waren grün.

## Sonar und geprüfte Korrekturen

Sonar-Analyse `d7d9b331-4e0d-402c-9df2-de1b39ac0a28` bestätigt den geprüften
`main`-Stand. Von 173 offenen Ausgangsbefunden wurden 172 durch Korrekturen
geschlossen. Der einzelne Hinweis zu HMAC-SHA-1 wurde für das bestehende
[TOTP-Format](zwei-faktor-authentifizierung.md) am Befund begründet akzeptiert.
Die 51 früheren Ausnahmen bleiben bestehen.

Die Korrekturen betreffen die Transaktionseinstiege für Ticket-Livezugriffe,
PDF-Dateinamen für Ersatzcodes, die Struktur der Anmelde- und 2FA-Prüfungen
sowie Ticketansichten. Fehlertests prüfen die vorgesehene Ausnahmeoperation
getrennt von ihrer Vorbereitung. Die Browserläufe decken unter anderem
Ticketsprung, Filter, Pagination, Ticketverlauf, OTP-Bedienung,
2FA-Regelauswahl und Feldhilfe ab.

## Offene Abnahme

Die gemeinsame Sichtabnahme für [2FA und Admin-Tickets](planung/zwei-faktor-bestaetigung.md)
bleibt offen. Echte Authenticator-Apps, Anbieter-Sonderfälle, Mobilgeräte,
Bildschirmtastatur, Zoom, Screenreader und PDF-Druck stehen im
[manuellen Katalog](manual-tests/README.md). Weitere offene automatisierte
Szenarien stehen im [Testbedarf](testbedarf.md).
