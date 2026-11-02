# Dokumentation

Diese Dokumentation beschreibt den aktuellen Entwicklungsstand. Die Anwendung läuft
lokal mit Testdaten; eine neue Testdatenbank wird aus dem aktuellen Schema angelegt.

| Thema | Inhalt |
| --- | --- |
| [Anwendung](anwendung.md) | Funktionen, Konto, Administration und Benachrichtigungen |
| [Externe Anmeldung](externe-anmeldung.md) | Google-/Microsoft-Einrichtung, Kontoverknüpfung und Sicherheitsnachweise |
| [Sicherheit](sicherheit.md) | Anmeldung, Rollen, Tokens, Sitzungen, CSRF und Datenverträge |
| [Zwei-Faktor-Authentifizierung](zwei-faktor-authentifizierung.md) | Authenticator, Ersatzcodes, Regeln, Entwicklungsoptionen und Wiederherstellung |
| [Entwicklung](entwicklung.md) | Voraussetzungen, Aufbau, Konfiguration, Build und Codestil |
| [Arbeiten mit Codex](codex-skills.md) | Projekt-Skills, Aufträge, Grenzen und Ergebnisse |
| [Docker](docker/README.md) | Lokale Testanwendung und Mailpit |
| [Theme](theme.md) | Verbindliche Gestaltung, CSS-Zuständigkeiten und Themewechsel |
| [Frontend-Referenzen](design/frontend-referenzen/README.md) | Lokale HTML-Gestaltung für Besucher, angemeldetes Portal und Administration mit drei bedienbaren Momentaufnahmen |
| [Mailqueue](mailqueue.md) | Versand, Wiederholungen und unklare Versandausgänge |
| [Logging](logging.md) | Logpegel, zulässige Inhalte und Diagnose |
| [Entscheidungen](entscheidungen.md) | Wesentliche technische Entscheidungen, Vorteile und Grenzen |
| [Tests](tests.md) | Testarten und Ausführung |
| [Offener Testbedarf](testbedarf.md) | Gemeinsame Aufgabenliste für fehlende oder anzupassende automatisierte Tests |
| [Aktueller Stand](status.md) | Funktionsstand und Ergebnisse der letzten vollständigen Testrunde |
| [Manuelle Sonderprüfungen](manual-tests/README.md) | Menschliche Abnahmen, Mailbeispiele und Anleitungen für Sonderfälle |
| [Offene Fehler](../BUGS.md) | Kurze priorisierte Liste bestätigter Anwendungsfehler |
| [Offene Aufgaben](../TODO.md) | Vereinbarte nächste Arbeiten |

Details zu automatisierten Browserfällen stehen bei deren Testbeschreibungen und
in der [Browser-Testanleitung](../browser-tests/README.md).

## Offene Arbeiten

Die Designübernahme für Besucher, angemeldetes Portal und Administration ist
umgesetzt und abgenommen. Die freigegebenen Bausteine stehen im [Theme-Standard](theme.md),
die drei schlanken HTML-Referenzen bleiben in `docs/design/frontend-referenzen`.
Der Stand ist lokal nach `main` integriert. Die Sonar-Bereinigung und die vollständige
große Testrunde in Deutsch und Englisch sind bestanden. Aktuelle Ergebnisse und
verbleibende Grenzen stehen im [Prüfstand](status.md).

Weitere vereinbarte Arbeiten stehen in [TODO](../TODO.md); gültige Bedienung und
Konfiguration in den obigen Fachdokumenten.

Die [Google-/Microsoft-Anmeldung](externe-anmeldung.md) und freiwillige Angaben zu
Geschlecht/Geburtsdatum sind umgesetzt und vom Benutzer abschließend abgenommen.
Die gewählten Designkorrekturen stehen im [Theme-Standard](theme.md).
Die technisch umgesetzte [2FA-Erweiterung](planung/zwei-faktor-bestaetigung.md) umfasst konfigurierbare
Rollen-/Funktionsregeln, eine Superadmin-Konfiguration, TOTP und Wiederherstellung.
Sie ersetzt die frühere Google-Zwischenlösung für sensible Aktionen durch einen
Faktornachweis. Der geschützte Ersteinrichtungsweg und seine Grenzen stehen in der
[Sicherheitsdokumentation](externe-anmeldung.md#google-bestätigung-und-geschützte-ersteinrichtung).
Die gewünschte gewarnte
Entwicklungs-Ausnahme ist vom sicheren Betrieb getrennt. Die
Anbieterverwaltung ausschließlich für Superadmins gehört verbindlich zum Paket.
Für die erste Google-2FA-Einrichtung sind erneute Anbieterbestätigung, E-Mail-Code
und erster TOTP mit ausdrücklich akzeptiertem Restrisiko abgestimmt. Bestehende
2FA kann dadurch nicht ersetzt werden. Die Umsetzung und ihre gezielten isolierten
Nachprüfungen sind abgeschlossen. Die gemeinsame fachliche und gestalterische
Abnahme auf dem Feature-Branch steht noch aus.
Bedienung und Grenzen stehen in der [2FA-Dokumentation](zwei-faktor-authentifizierung.md).
Weitere bewertete Möglichkeiten stehen in der
[Entscheidungsvorlage für Portal-Erweiterungen](planung/portal-erweiterungen.md).
Die [Vaadin-Komponentenprüfung](planung/vaadin-komponenten-abgleich.md) enthält die
abgenommene Bestandsaufnahme und Einschätzung zu Badges und weiteren möglichen nativen
Bausteinen. Der Umbau ist als separate spätere Umsetzung vorgesehen und gehört nicht
zur abgeschlossenen Google-/Microsoft- oder zur 2FA-Umsetzung. Start, Paketumfang und
Reihenfolge sind noch freizugeben.
