# Offener Testbedarf

Hier stehen ausschließlich offene Aufgaben zur Ergänzung oder Anpassung
automatisierter Tests. Pflege und Abnahme: [Testablauf](tests.md#übergabe-offener-testaufgaben).
Erledigte Einträge werden entfernt; die Datei bleibt als gemeinsame Anlaufstelle bestehen.

## Browser-Tests

Die folgenden Prüfanleitungen benennen noch abzugleichenden Testbedarf. Zuerst die
vorhandenen JUnit- und Browsernachweise prüfen und wiederverwenden; sie sind keine
Liste bereits festgestellter Fehler. Nur fehlende Szenarien ergänzen, keine
gleichwertigen Tests duplizieren. Zustand aller Einträge: **offen**.

| Funktion / Anleitung | Testart | Szenario und erwartetes Verhalten |
| --- | --- | --- |
| [Persistenz nach Neustart, AUTO-01](manual-tests/README.md#auto-01) | Integration/Browser mit eigener Neustartumgebung | Ohne Bootstrap dieselbe Testdatenbank erneut öffnen: Konto, Rollen und gespeicherte Konfiguration bleiben erhalten. Vorhandene Neustart-/Sessiontests berücksichtigen. |
| [SMTP-Konfiguration, AUTO-02](manual-tests/README.md#auto-02) | Integration/Browser | Defaults gelten bereits beim Erststart. Eine gespeicherte Änderung des startgebundenen SMTP-Ziels wirkt erst nach Neustart; mit zwei eigenen SMTP-Testzielen nachweisen. |
| [SMTP-Wiederanlauf, AUTO-03](manual-tests/README.md#auto-03) | Integration/Browser | Nach eindeutigem Verbindungsfehler vor SMTP-Annahme und Wiederanlauf wird eine über die Registrierung angelegte Queue-Mail zugestellt; weiterer Retry erzeugt keine doppelte Zustellung. |
| [Dauerhafter SMTP-Ausfall, AUTO-04](manual-tests/README.md#auto-04) | Integration/Browser | Die echten Transportversuche enden begrenzt und diagnostizierbar. Eine terminal fehlgeschlagene Mail wird beim Wiederanlauf nicht automatisch erneut versendet. |
| [Sprachwechsel bei Feldfehlern, AUTO-05](manual-tests/README.md#auto-05) | Browser DE/EN | Sichtbare Fehlertexte wechseln mit der Sprache; ungültiger Konfigurationsentwurf bleibt erhalten und wird nicht gespeichert. Vorhandener Test für einen gültigen Entwurf ersetzt den Fehlerfall nicht. |
| [Sprache neuer Mails, AUTO-06](manual-tests/README.md#auto-06) | Browser DE/EN | Neue anonyme Passwortanfragen für dasselbe Konto folgen der auslösenden Sitzungssprache; bereits empfangene Mails bleiben unverändert. |
| [Browser-Verbindungsabbruch, AUTO-07](manual-tests/README.md#auto-07) | Browser DE/EN | Offline während einer Bearbeitung speichern und wieder verbinden: kein falscher Erfolg; gespeicherter Stand ist eindeutig feststellbar und die Bearbeitung kann fortgesetzt werden. |

SMTP-Ausfallprüfungen verwenden ausschließlich eigene isolierte Testdienste. Die
[optionale Prüfung eines externen Anbieters, AUTO-08](manual-tests/README.md#auto-08),
ist aktuell nicht beauftragt und kein Pflichtteil der großen Testrunde. Sie setzt
einen ausdrücklich freigegebenen Zugang und Empfänger voraus. Menschliche
Abnahmen bleiben im manuellen Katalog.

## JUnit-Tests

Aktuell sind hier keine konkreten offenen JUnit-Testaufgaben erfasst.
Auch größere oder wegen einer Benutzerentscheidung wartende Aufgaben hier aufnehmen:
betroffene Funktion, Szenario, Erwartung und Zustand mit Begründung nennen.
