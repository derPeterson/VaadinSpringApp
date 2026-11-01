# Dokumentation

Diese Dokumentation beschreibt den aktuellen Entwicklungsstand. Die Anwendung läuft
lokal mit Testdaten; eine neue Testdatenbank wird aus dem aktuellen Schema angelegt.

| Thema | Inhalt |
| --- | --- |
| [Anwendung](anwendung.md) | Funktionen, Konto, Administration und Benachrichtigungen |
| [Externe Anmeldung](externe-anmeldung.md) | Google-/Microsoft-Einrichtung, Kontoverknüpfung und Sicherheitsnachweise |
| [Sicherheit](sicherheit.md) | Anmeldung, Rollen, Tokens, Sitzungen, CSRF und Datenverträge |
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
Der Designbranch ist nach `main` integriert und technisch geprüft. Die Sonar-Bereinigung
ist mit grünem Quality Gate abgeschlossen. Offen bleibt die anschließende große
Testrunde. Die letzte vollständige QA bleibt im [Prüfstand](status.md) erhalten.

Weitere vereinbarte Arbeiten stehen in [TODO](../TODO.md); gültige Bedienung und
Konfiguration in den obigen Fachdokumenten.

Der [aktive Umsetzungsplan](planung/google-microsoft-anmeldung.md) beschreibt die
Google-/Microsoft-Anmeldung und freiwillige Angaben zu Geschlecht/Geburtsdatum.
Die Umsetzung ist isoliert geprüft. Die noch offene gemeinsame Abnahme steht im Plan.
Die [2FA-Bestätigung](planung/zwei-faktor-bestaetigung.md) ist als nächste separate
Umsetzung vorgesehen. Sie soll die in [BUGS.md](../BUGS.md) dokumentierte Schwäche
der ausdrücklich akzeptierten Google-Zwischenlösung bei eingerichteter 2FA ablösen.
Weitere bewertete Möglichkeiten stehen in der
[Entscheidungsvorlage für Portal-Erweiterungen](planung/portal-erweiterungen.md).
Die [Vaadin-Komponentenprüfung](planung/vaadin-komponenten-abgleich.md) enthält die
abgenommene Bestandsaufnahme und Einschätzung zu Badges und weiteren möglichen nativen
Bausteinen. Der Umbau ist als separate spätere Umsetzung vorgesehen und gehört nicht
zur Google-/Microsoft-Umsetzung. Start, Paketumfang und Reihenfolge sind noch freizugeben.
