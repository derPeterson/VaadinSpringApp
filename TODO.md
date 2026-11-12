# Offene Aufgaben

## Laufende Umsetzung

- [ ] [Vaadin-Komponenten und vereinbarte Portalpakete](docs/planung/vaadin-komponenten-abgleich.md)
  weiterführen. Konto deaktivieren/löschen und die noch offenen Komponenten
  gemeinsam ansehen. Der fehlende aktuelle Sonar-Nachweis steht im Plan.
  Die ausdrücklich zurückgestellten
  Pakete bleiben bis zu einer späteren Freigabe offen.

- [ ] [Integrierte Einheitenauswahl](docs/planung/einheitenauswahl.md) nach der
  abgeschlossenen Analyse gemeinsam sichten und die Umsetzung freigeben.
- [ ] [Einheitliche Seitenpfade](docs/planung/seitenpfade.md) nach der
  abgeschlossenen Analyse gemeinsam sichten und die Umsetzung freigeben.

## Künftige Anwendungsfunktionen

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
