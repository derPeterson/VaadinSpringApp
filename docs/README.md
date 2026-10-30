# Dokumentation

Diese Dokumentation beschreibt den aktuellen Entwicklungsstand. Die Anwendung läuft
lokal mit Testdaten; eine neue Testdatenbank wird aus dem aktuellen Schema angelegt.

| Thema | Inhalt |
| --- | --- |
| [Anwendung](anwendung.md) | Funktionen, Konto, Administration und Benachrichtigungen |
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

- [Designübernahme und gemeinsame Abnahme](planung/designkritik-stitch.md):
  Besucherbereich, angemeldetes Portal und Administration sind in Vaadin umgesetzt.
  Sie verwenden den gemeinsamen Seitenrahmen; Portal und Admin teilen die native
  Seitennavigation. Die Vergleiche bleiben zur gemeinsamen Endabnahme erhalten.
  Aktuelle technische Nachweise stehen ausschließlich im [Prüfstand](status.md).
  Die Auslagerung der großen Referenzsammlung und der Branchabschluss benötigen
  die im Plan beschriebenen gemeinsamen Entscheidungen.

Weitere vereinbarte Arbeiten stehen in [TODO](../TODO.md); gültige Bedienung und
Konfiguration in den obigen Fachdokumenten.
