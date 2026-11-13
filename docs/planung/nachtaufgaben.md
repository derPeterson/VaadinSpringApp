# Nachtaufgaben und gemeinsame Sichtung

## Freigabe und Stand

Die freigegebenen technischen Nachtarbeiten und Planungen sind bearbeitet,
je Paket reviewt und separat committet. Paket 1 und 5 sind umgesetzt;
Paket 2 bis 4 umfassen ausschließlich Analyse und Planung.

**Die laufende Gesamtumsetzung bleibt bis zur gemeinsamen Abnahme offen.**
Der lokale Zwischenmerge und die bestandene große Testrunde ersetzen diese
Abnahme nicht. Es wurden keine Releases erstellt, Versionsnummern geändert,
Passkeys umgesetzt, Dienste gebucht oder Anwendungen öffentlich bereitgestellt.

## Ergebnisse der fünf Pakete

| Paket | Technischer Stand | Einstieg zur Sichtung |
| --- | --- | --- |
| 1. E-Mail-Queue | Native Reiter Allgemein und Assistenten, innere Bereinigungskarte, gemeinsame Lade-/Speicheraktionen und dauerhafte Laufhistorie mit konfigurierbarer Aufbewahrung umgesetzt. | Konfiguration → E-Mail-Queue; Assistenten ausschließlich als Superadmin. [Fachvertrag](../mailqueue.md), [Theme](../theme.md). |
| 2. Releases und Neuigkeiten | Release-Ablauf, Versionierung/Snapshots, künftiger Skill und kuratierte Änderungsnotizen sowie ein späterer Neuigkeitenbereich geplant. | [Release-Plan](releases.md) und [Was ist neu?](neuigkeiten.md). |
| 3. Passkeys | Zusammenspiel mit vorhandener 2FA, Sicherheitsgrenzen, Mehrwert und späterer Umbau analysiert. Empfehlung: vorerst zurückstellen. | [Passkey-Plan](passkeys.md). |
| 4. Öffentlicher Testbetrieb | Anbieter, grobe Kosten, Ressourcen, echter Mailversand, CI/Deployment, Wartung und konkrete Vorarbeiten verglichen. | [Hostingplanung](oeffentlicher-testbetrieb.md). |
| 5. Browser-Laufablage | Frei konfigurierbaren Stamm einschließlich Berichten, Belegen, Workern und Launchern umgesetzt; lokal D:/VaadinSpringApp/browser-runs eingestellt. Alte Läufe erhalten. | [Ablage konfigurieren](../../browser-tests/README.md#ablageort-lokal-konfigurieren). |

Die technische Historie enthält tatsächliche manuelle und automatische
Bereinigungsversuche einschließlich Nullergebnissen und Blockierung durch
laufenden Versand. Der tägliche Löschlauf verwendet die gespeicherte
Historienfrist und erhält den letzten Status sowie die Intervallsteuerung.
Eine sichtbare Verlaufsliste ist ausdrücklich eine spätere eigene Aufgabe.

## Noch gemeinsam ansehen oder entscheiden

- [ ] Die neue Queue-Karte unter Allgemein und Assistenten auf Desktop und
  Smartphone ansehen; als Admin den allgemeinen Bereich und als Superadmin
  den Assistenten durchgehen. Feldhilfen, Laden/Speichern und Statusbereich
  gehören zur gemeinsamen Sichtung.
- [ ] Release- und Neuigkeitenplanung lesen; Versionsschema, ersten Release
  und späteren Skill-/Umsetzungsauftrag gemeinsam festlegen.
- [ ] Passkey-Einschätzung besprechen und die Empfehlung zum Zurückstellen
  beziehungsweise einen späteren eigenen Auftrag festlegen.
- [ ] Hostingvarianten und Budget durchgehen; Anbieter, Domain, Maildienst
  und den späteren Einrichtungsauftrag entscheiden.
- [ ] Die neue lokale Laufablage und den Berichtszugriff bei Bedarf gemeinsam
  durchgehen. Bestehende C:-Läufe werden weiterhin nur auf eigenen Auftrag
  verschoben oder gelöscht.

Die Prüfungen im Browser sind automatisiert abgeschlossen. Diese Liste
enthält menschliche Sichtung und Entscheidungen, keine offenen automatisierten
Nachtests. Neue tatsächliche Testlücken gehören ausschließlich in den
[Testbedarf](../testbedarf.md).

## Durchgeführter Abschlussblock

Die Pakete wurden lokal nach `main` zwischenintegriert. Build, Tests und
isolierter Start sind geprüft. Der vollständige Sonar-Ablauf mit reviewten
Korrekturpaketen und Nachscans ist durchgeführt; die große Testrunde begann
vor der vereinbarten spätesten Startzeit 09:00 Uhr Europe/Berlin.
Beide vollständigen Browserläufe sind am selben unveränderten Image bestanden.

Der aktuelle Nacharbeitsbranch ist
`codex/vaadin-komponenten-folgearbeiten`. Quelle und Zwischenstände bleiben
erhalten; es gab keinen Push. Maßgeblich für Zahlen, Sonar, Korrekturen und
Prüfgrenzen ist ausschließlich der [aktuelle Prüfstand](../status.md).

Für diese Nachtaufgabe umfassten die Reviews ausdrücklich die Lesbarkeit der
**geänderten Bereiche**. Die allgemeine Vorgabe zur vollständigen Klassenprüfung
wurde dadurch nicht geändert. Ungeplante notwendige Korrekturen sind in eigenen
Commits enthalten; die bestehenden Fach-, Rechte- und Sicherheitsverträge bleiben.

## Vereinbarte spätere Arbeiten

Die sichtbare Bereinigungshistorie wird zunächst gemeinsam entworfen.
Release-/Neuigkeiten-, Passkey- und Hostingumsetzung brauchen jeweils einen
eigenen Auftrag. Die übrigen zurückgestellten Komponentenpakete bleiben im
[Komponentenplan](vaadin-komponenten-abgleich.md) und in [TODO](../../TODO.md).
