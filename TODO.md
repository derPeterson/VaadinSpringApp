# Offene Aufgaben

## Laufende Planung

- [ ] [Was ist neu?](docs/planung/neuigkeiten.md) im Portal gemeinsam abnehmen.

## Zurückgestellte Planungen

- [ ] [Passkey-Planung](docs/planung/passkeys.md) bei einer späteren Freigabe
  erneut gegen den aktuellen Stand prüfen und umsetzen. Planung durchgelesen,
  Umsetzung zurückgestellt; bestehende 2FA beibehalten.
- [ ] [Öffentlichen Testbetrieb](docs/planung/oeffentlicher-testbetrieb.md) später
  auf eigenen Auftrag einrichten. Planung durchgelesen; Anbieter, Domain, SMTP
  und Datenhaltung vor der Bereitstellung entscheiden.

## Künftige Anwendungsfunktionen

- [ ] Für „Was ist neu?“ bei Bedarf eine Kennzeichnung administrativer Hinweise
  im gemeinsamen Changelog und eine darauf basierende Anzeige nach Zielgruppe
  bewerten. Umfang anhand konkreter Einträge gemeinsam entscheiden.

- [ ] Eine Touch-Bedienung zum Aufheben der Auswahl in Ticket- und Empfängerlisten
  gemeinsam festlegen. Desktop unterstützt dort Strg+Klick auf den ausgewählten
  Eintrag. Im Admin-Ticketsystem ist das Abwählen bereits über das native Grid
  möglich; zuerst prüfen, ob dessen vorhandene Smartphone-Bedienung als Vorlage
  geeignet ist. Noch keine zusätzliche mobile Aktion einbauen.

- [ ] Einheitlichen Umgang mit Tooltips auf Smartphones analysieren und gemeinsam
  festlegen. Native Vaadin-Tooltips werden per Hover und Tastaturfokus geöffnet,
  unterstützen aber kein langes Tippen, siehe
  [Vaadin-Tooltip-Dokumentation](https://vaadin.com/docs/latest/components/tooltip).
  Anwendungsweit prüfen, welche Inhalte
  ergänzend und welche für die Bedienung nötig sind und wie diese auf
  Touch-Geräten zugänglich sein sollen. Vorhandene native Feldhilfen und andere
  passende Vaadin-Möglichkeiten berücksichtigen; keine pauschalen zusätzlichen
  Info-Buttons einführen. Eigenständige spätere Aufgabe. Bei langen Benutzerangaben
  bleiben vorerst Kürzung und normale Tooltips wie abgestimmt.

- [ ] Eine sichtbare Verlaufsliste für Bereinigungsläufe der E-Mail-Queue gemeinsam
  entwerfen. Darstellung, Umfang und Bedienung vor der Umsetzung abstimmen.
  Technische Speicherung und konfigurierbare Aufbewahrung sind bereits vorhanden;
  siehe [Mailqueue-Vertrag](docs/mailqueue.md#ergebnisse-und-wiederholung).
  Die History-Oberfläche bleibt eine eigene spätere Umsetzung.

- [ ] Nutzen und Aufwand der Rolle GUEST bei späteren Funktionserweiterungen
  erneut bewerten. Ursprünglich für „Als Gast anmelden“ gedacht, aktuell ohne
  konkreten Anwendungsfall für einen solchen Zugang. Vorerst erhalten und keinen
  zusätzlichen Gastablauf bauen. Bei der Bewertung berücksichtigen, ob die Rolle
  Berechtigungen, 2FA-Regeln, Oberflächen und Tests unnötig verkompliziert;
  Beibehaltung, konkrete Nutzung oder Entfernung dann bewusst entscheiden.
  [Aktueller Rollenstand](docs/rollen.md#gast--guest). Keine Nachtaufgabe.

- [ ] Allgemeine Supportanfragen für Benutzer ermöglichen, unabhängig von
  Problemen mit der Zwei-Faktor-Authentifizierung. Beispielsweise Hilfe bei
  fehlgeschlagener Kontodeaktivierung oder Kontolöschung anbieten. Den Zugang
  auch für nicht mehr angemeldete Benutzer und nötige Missbrauchsgrenzen bei
  einer späteren Planung klären. Gehört nicht zur laufenden Umsetzung oder
  zu den gesammelten Nachtaufgaben.

## Entwicklungsablauf

- [ ] Codex später gemeinsam in IntelliJ über JetBrains Air ausprobieren.
  Den Chat „Allgemeine Entwicklung“ öffnen und an einer kleinen Aufgabe
  testen, wie sich markierter Code als Kontext mitgeben und der Diff direkt
  in der IDE prüfen lässt. Bei einem passenden Laufzeitfehler auch den
  IntelliJ-Debugger ausprobieren.
- [ ] Auf dem Feature-Branch die relevanten Projektdateien mit SonarQube for IDE
  analysieren, berechtigte Findings beheben und die betroffenen Dateien erneut
  prüfen.

## Menschliche Abnahmen

- [ ] Echte Mobilgeräte, Bildschirmtastatur, Zoom, Tastaturfokus und Screenreader.
- [ ] Verständlichkeit der deutschen und englischen Texte durch einen Menschen.

Anleitungen: [Manuelle Sonderprüfungen](docs/manual-tests/README.md).
Offene automatisierte Testaufgaben: [Testbedarf](docs/testbedarf.md).

Erledigte Aufgaben entfernen; aktuelle Funktionen in der Fachdokumentation beschreiben.
