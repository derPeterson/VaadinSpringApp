# Dokumentation und Einstieg

Die Dokumentation beschreibt den aktuellen lokalen Entwicklungsstand mit
austauschbaren Testdaten. **Versionen und Implementierungsdetails stehen im Code**;
hier stehen Bedienung, Entwicklungsregeln und wesentliche Entscheidungen.

## Funktionen und Sicherheit

| Thema | Was du dort findest |
| --- | --- |
| [Anwendung](anwendung.md) | Konto, Profil, Benachrichtigungen und Administration |
| [Rollen und Berechtigungen](rollen.md) | Zusätzliche Rechte je Rolle und gemeinsame Kontofunktionen |
| [Sicherheit](sicherheit.md) | Anmeldung, Kontosperren, Tokens, Sitzungen und Datenverträge |
| [Externe Anmeldung](externe-anmeldung.md) | Google/Microsoft einrichten und mit dem Konto verbinden |
| [Zwei-Faktor-Authentifizierung](zwei-faktor-authentifizierung.md) | Authenticator, Ersatzcodes, Regeln und Wiederherstellungsanfragen |
| [Betreiber-Notfallweg](betrieb/2fa-notfall.md) | Eng begrenzter Eingriff bei verlorenem Superadmin-Faktor |
| [Mailqueue](mailqueue.md) | Versand, Wiederholungen und Bereinigung unklarer Versandausgänge |

## Entwickeln und gestalten

| Thema | Was du dort findest |
| --- | --- |
| [Entwicklung](entwicklung.md) | Aufbau, Konfiguration, Start, Build und verbindlicher Codestil |
| [Startkonten](bootstrap.md) | Bootstrap, Entwicklerkonten und E-Mail-Nachweis |
| [Theme](theme.md) | Aura/Cosmos, gemeinsame Komponenten, Tokens und CSS-Zuständigkeiten |
| [Logging](logging.md) | Logpegel, erlaubte Inhalte und sichere Diagnose |
| [Technische Entscheidungen](entscheidungen.md) | Gründe, Vorteile und Grenzen des geltenden Modells |
| [Arbeiten mit Codex](codex-skills.md) | Projekt-Skills, passende Aufträge und Ausführungsgrenzen |
| [Versionierung und Releases](releases.md) | Changelog, Kandidatenprüfung, unveränderliche Tags und nächster Snapshot |
| [Docker](docker/README.md) | Eigene lokale Testanwendung, HTTPS und Mailpit |

## Prüfen und nachschlagen

| Thema | Zweck |
| --- | --- |
| [Tests](tests.md) | Testarten und technische Ausführung |
| [Browser-Testanleitung](../browser-tests/README.md) | Auswahl, Isolation, Runner und Berichte |
| [Manuelle Sonderprüfungen](manual-tests/README.md) | Menschliche Abnahmen, echte Geräte und Mailprogramme |
| [Aktueller Prüfstand](status.md) | Letzte große Testrunde und Grenzen späterer Teilprüfungen |
| [Offener Testbedarf](testbedarf.md) | Noch nötige automatisierte Prüfungen |
| [Offene Fehler](../BUGS.md) | Bestätigte Fehler mit erforderlichem Nachtest |
| [Offene Aufgaben](../TODO.md) | Vereinbarte nächste Arbeiten |

## Offene Arbeiten

Offene Umsetzungen stehen unter `docs/planung/`; sie sind von der gültigen
Fachdokumentation getrennt. Ein Plan ist noch kein Umsetzungs-, Release- oder Testauftrag.

- Zurückgestellte Planungen: [Neuigkeiten](planung/neuigkeiten.md), [Passkeys](planung/passkeys.md) und
  [öffentlicher Testbetrieb](planung/oeffentlicher-testbetrieb.md).
- Weitere bewertete Ideen: [Portal-Erweiterungen](planung/portal-erweiterungen.md).

Ausführungsfreigaben und offene Abnahmen nicht aus einem Planlink ableiten.
Tatsächliche Prüfergebnisse stehen ausschließlich im Prüfstand.
