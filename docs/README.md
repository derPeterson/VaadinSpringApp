# Dokumentation

Diese Dokumentation beschreibt den aktuellen Entwicklungsstand. Die Anwendung läuft
lokal mit Testdaten; eine neue Testdatenbank wird aus dem aktuellen Schema angelegt.

| Thema | Inhalt |
| --- | --- |
| [Anwendung](anwendung.md) | Funktionen, Konto, Administration und Benachrichtigungen |
| [Sicherheit](sicherheit.md) | Anmeldung, Rollen, Tokens, Sitzungen, CSRF und Datenverträge |
| [Entwicklung](entwicklung.md) | Voraussetzungen, Aufbau, Konfiguration, Build und Codestil |
| [Arbeiten mit Codex](codex-skills.md) | Die fünf Projekt-Skills, Aufträge, Grenzen und Ergebnisse |
| [Docker](docker/README.md) | Lokale Testanwendung und Mailpit |
| [Theme](theme.md) | Verbindliche Gestaltung, CSS-Zuständigkeiten und Themewechsel |
| [Frontend-Referenzen](design/frontend-referenzen/README.md) | Freigegebene HTML-Gestaltung für angemeldete und nicht angemeldete Benutzer sowie die Admin-Benutzerverwaltung, offline gesichert |
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

- [Designkritik und Spielwiese mit Stitch](planung/designkritik-stitch.md):
  Der zusammenhängende HTML-Portalprototyp aus 3a–3c ist technisch bereit:
  fünf angemeldete Benutzerseiten samt Overlays/Kontodialogen und die ergänzte
  Besucher-Vorschau mit neun öffentlichen Szenen. Feste Palette und gemeinsame
  Desktop-/Smartphone-Vorschau. Angemeldeter Bereich und Besucherbereich sind
  gestalterisch als feste HTML-Referenzen abgenommen. Auch die überarbeitete
  Variante 2 der Benutzerverwaltung ist als Adminreferenz abgenommen und in derselben
  Sammlung gesichert. Als Nächstes folgen die übrigen Adminbereiche anhand dieser
  Referenzen. Umfang und Reihenfolge
  des anschließenden Vaadin-Piloten bleiben festzulegen; die Übertragung ist
  noch nicht freigegeben.

Weitere vereinbarte Arbeiten stehen in [TODO](../TODO.md); gültige Bedienung und
Konfiguration in den obigen Fachdokumenten.
