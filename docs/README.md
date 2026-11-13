# Dokumentation

Diese Dokumentation beschreibt den aktuellen Entwicklungsstand. Die Anwendung läuft
lokal mit Testdaten; eine neue Testdatenbank wird aus dem aktuellen Schema angelegt.

| Thema | Inhalt |
| --- | --- |
| [Anwendung](anwendung.md) | Funktionen, Konto, Administration und Benachrichtigungen |
| [Externe Anmeldung](externe-anmeldung.md) | Google-/Microsoft-Einrichtung, Kontoverknüpfung und Sicherheitsnachweise |
| [Sicherheit](sicherheit.md) | Anmeldung, Rollen, Tokens, Sitzungen, CSRF und Datenverträge |
| [Rollen und Berechtigungen](rollen.md) | Zusätzliche Befugnisse je Rolle, gemeinsame Kontorechte und Grenzen |
| [Zwei-Faktor-Authentifizierung](zwei-faktor-authentifizierung.md) | Authenticator, Ersatzcodes, Regeln, Entwicklungsoptionen und Wiederherstellung |
| [Entwicklung](entwicklung.md) | Voraussetzungen, Aufbau, Konfiguration, Build und Codestil |
| [Arbeiten mit Codex](codex-skills.md) | Projekt-Skills, Aufträge, Grenzen und Ergebnisse |
| [Docker](docker/README.md) | Lokale Testanwendung und Mailpit |
| [Theme](theme.md) | Verbindliche Gestaltung, CSS-Zuständigkeiten und Themewechsel |
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

Die geplante [Versionierung mit Release-Ablauf](planung/releases.md) und der
darauf aufbauende [Neuigkeitenbereich](planung/neuigkeiten.md) sind zur gemeinsamen
Durchsicht ausgearbeitet. Sie sind noch nicht umgesetzt. Der übergreifende
Arbeitsauftrag steht in der [Nachtliste](planung/nachtaufgaben.md).

Die [Passkey-Analyse](planung/passkeys.md) bewertet die Einbindung in vorhandene
2FA und empfiehlt vorerst eine Zurückstellung. Eine Umsetzung ist nicht freigegeben.

Die Designübernahme für Besucher, angemeldetes Portal und Administration ist
umgesetzt und abgenommen. Die freigegebenen Bausteine stehen im [Theme-Standard](theme.md).
Für neue Designaufgaben dient die laufende Vaadin-Oberfläche als Ausgangspunkt.
Die früheren HTML-Referenzen sind über den Git-Tag `archiv/html-referenzen` abrufbar.
Der Stand ist lokal nach `main` integriert. Die Sonar-Bereinigung und die vollständige
große Testrunde in Deutsch und Englisch sind bestanden. Aktuelle Ergebnisse und
verbleibende Grenzen stehen im [Prüfstand](status.md).

Weitere vereinbarte Arbeiten stehen in [TODO](../TODO.md); gültige Bedienung und
Konfiguration in den obigen Fachdokumenten.

Die [Google-/Microsoft-Anmeldung](externe-anmeldung.md) und freiwillige Angaben zu
Geschlecht/Geburtsdatum sind umgesetzt und vom Benutzer abschließend abgenommen.
Die gewählten Designkorrekturen stehen im [Theme-Standard](theme.md).
Die [2FA-Erweiterung](zwei-faktor-authentifizierung.md) umfasst konfigurierbare
Rollen-/Funktionsregeln, eine Superadmin-Konfiguration, TOTP und Wiederherstellung.
Sie ersetzt die frühere Google-Zwischenlösung für sensible Aktionen durch einen
Faktornachweis. Der geschützte Ersteinrichtungsweg und seine Grenzen stehen in der
[Sicherheitsdokumentation](externe-anmeldung.md#google-bestätigung-und-geschützte-ersteinrichtung).
Die gewünschte gewarnte
Entwicklungs-Ausnahme ist vom sicheren Betrieb getrennt. Die
Anbieterverwaltung ausschließlich für Superadmins gehört verbindlich zum Paket.
Für die erste Google-2FA-Einrichtung sind erneute Anbieterbestätigung, E-Mail-Code
und erster TOTP mit ausdrücklich akzeptiertem Restrisiko abgestimmt. Bestehende
2FA kann dadurch nicht ersetzt werden. Die Umsetzung ist fachlich und gestalterisch
abgenommen. Die automatisierten Ergebnisse stehen im [Prüfstand](status.md).
Weitere bewertete Möglichkeiten stehen in der
[Entscheidungsvorlage für Portal-Erweiterungen](planung/portal-erweiterungen.md).
Die [Vaadin-Komponentenprüfung](planung/vaadin-komponenten-abgleich.md) enthält die
aktuelle Bestandsaufnahme der Flow-Oberfläche, geprüfte Standardkomponenten und
die noch offenen Umbaupakete und Abnahmen. Bereits freigegebene Anpassungen stehen
im Theme-Standard; zurückgestellte Pakete benötigen einen neuen Umsetzungsauftrag.
