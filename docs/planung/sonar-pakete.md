# Offene Sonar-Nachprüfung

Auf ausdrücklichen Benutzerauftrag direkt auf `main` arbeiten. Die aktuelle
Analyse `5aee744a-9168-4fe4-8397-7e3a01616d29` zu `f0ccadf3` enthält genau einen
offenen Hinweis; die 46 bestehenden Ausnahmen und null Hotspots bleiben erhalten.

## NotificationHelper: leere Hervorhebung eindeutig abgrenzen

- Finding `a494c427-dde9-444a-b576-bc64216130cf`, Regel `java:S2259`.
- Die bisherige Null-/Leerbehandlung ist über einen Index-Zwischenwert verteilt.
  Eine ausdrückliche frühe Rückgabe macht den unveränderten Vertrag direkt lesbar
  und für die Analyse nachvollziehbar. Keine Suppression oder Statusausnahme.
- Regression: Eine sichtbare Hervorhebung kann beim Aktualisieren fehlen oder leer
  werden und später wiederkehren; kompletter Literaltext und dieselbe Notification
  bleiben erhalten. Vorhandene Null-/Leer- und Sprachwechselprüfungen beibehalten.
- Nach Codeprüfung und Commit den vollständigen geschützten Scan auf `main`
  wiederholen, danach den beauftragten kurzen gezielten JUnit-Lauf durchführen.

Die Vaadin-Komponentenumstellung und ihr Plan bleiben ausdrücklich offen.
