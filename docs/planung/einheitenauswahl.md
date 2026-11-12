# Integrierte Einheitenauswahl

## Empfehlung

Den vorhandenen `AdminDurationField` weiterverwenden. Die Zahl bleibt ein natives
`IntegerField` beziehungsweise `BigDecimalField`. Rechts im öffentlichen Suffix
steht ein natives `Select<Unit>` mit eigener zugänglicher Beschriftung „Einheit“.
Ein `HorizontalLayout` verbindet beim präzisen Feld Plusbutton und Einheit.
Kein selbst gebautes Textfeld und keine neuen Speicherformate sind erforderlich.

Geprüfte Grundlage: Vaadin Flow 25.3.5 und die offiziellen Dokumentationen zu
[Number Field](https://vaadin.com/docs/latest/components/number-field),
[Text Field](https://vaadin.com/docs/latest/components/text-field) und
[Select](https://vaadin.com/docs/latest/components/select).
Der Suffix ist eine öffentliche Komponenten-API. Ein `Select` passt hier besser
als eine durchsuchbare ComboBox mit vier festen Einheiten.

## Fachliche Grenzen

| Bestehende Einstellungen | Speicherung und Auswahl |
| --- | --- |
| Gespeicherte Anmeldung, Sitzungs-Timeout, Anmeldeversuchsfenster, Mailwiederholung | Ganze Sekunden, Auswahl Sekunden/Minuten/Stunden/Tage; ganzzahlige Grenzen erhalten |
| Abwesenheit, Verifikations-/Resetfristen und Queueaufbewahrung | `Duration`, vorhandene genaue Dezimalverarbeitung erhalten |
| 2FA-Inaktivität | Sekunden, nur Minuten/Stunden/Tage wie bisher |
| Inbox-/Versandhistorie und Kontodeaktivierungsfristen | Ganze Tage; fester Suffix ohne Aufklappsymbol |

Beim Einheitenwechsel bedeutet die bestehende Bedienung beispielsweise „10 Minuten“
statt „10 Sekunden“: Sie ändert bewusst den Zeitraum, nicht nur die Darstellung.
Eine automatische Umrechnung wäre eine zusätzliche Verhaltensentscheidung. Sie ist
für die reine optische Integration nicht nötig und bleibt deshalb unverändert.
Beim Laden wählt der vorhandene Code bereits eine ohne Rundung darstellbare Einheit.
Sekundenbruchteile oder Sekunden-Aufbewahrung nicht in Tagesfelder hineininterpretieren.

## Noch auszuführen

- [ ] Zusammensetzung zunächst an `AdminDurationField` umsetzen, vorhandene
  Konfigurationsprüfungen und Listener erhalten.
- [ ] Suffix rechts hinter Plus/Löschen mit bestehender Trennlinie anordnen.
  Gemeinsame Feldhöhe und Fokusrahmen erhalten; Select ohne zusätzliche Außenbox,
  aber mit erkennbarer Tastaturfokussierung und eigenem Einheitenlabel gestalten.
- [ ] Tab von Zahl zur Einheit, Pfeiltasten/Enter/Escape, schmale Breiten,
  Disabled-/Fehlerzustände und Overlayposition isoliert prüfen.
- [ ] Bestehende Dauerfeldtests sowie DE/EN-Vorher-/Nachhervergleich ausführen.

Der Umfang ist ein begrenzter Komponentenumbau mit anschließendem Browservergleich.
Die genaue Suffix-Fokusdarstellung ist noch nicht im Browser geprüft. Die Analyse
allein behauptet deshalb keine bereits fertige oder visuell abgenommene Lösung.
