# „Was ist neu?“ im Portal

## Zugang und Gestaltung

Die Seite ist ausschließlich für angemeldete Benutzer zugänglich. In der
Navigationsgruppe **Portal** steht ihr eigener Menüeintrag zwischen **Startseite**
und **Benachrichtigungen**. Die Beschriftung lautet auf Deutsch **Was ist neu?**
und auf Englisch **What's New?**. Auch Administratoren öffnen die Seite im Portal.

## Aufbau der Release-Notizen

Die Cosmos-Seite zeigt den jüngsten veröffentlichten Release zuerst. Jede
Ausgabe hat einen eigenen redaktionellen Titel, Version und Datum sowie einen
kurzen Einstieg. Unter **Highlights** stehen die wichtigsten Neuerungen.
**Weitere Verbesserungen** fasst kleinere Änderungen knapp zusammen.

Für die erste Fassung kann ein Release einen Screenshot mit Beispieldaten
enthalten, wenn er ein Highlight verständlicher macht. Der Screenshot wird
redaktionell beim Release ausgewählt. Kurze Videos gehören zu einer späteren
Erweiterung. Auf der Startseite kann später ein unaufdringlicher Link ergänzt
werden.

## Eine Quelle, keine doppelte HTML-Pflege

Die bestehende `CHANGELOG.md` aus dem [Release-Ablauf](../releases.md) enthält beide
Sprachfassungen. Der Maven-Build übernimmt sie als dedizierte Classpath-Ressource.
`PortalReleaseNotes` liest sie über einen Stream.

Version, Datum sowie DE-/EN-Abschnitte haben bereits ein festgelegtes Quellformat.
Titel, Einstieg, die beiden Inhaltsabschnitte und optionale Bildverweise werden
in den jeweiligen Sprachabschnitten der `CHANGELOG.md` gepflegt.
Der Loader trennt die Metadaten vom Sprachinhalt. Die Ausgabe des aktuellen
Kandidaten wird ab dem nach dem Tag gebauten Folge-Snapshot sichtbar. So können
der endgültige Release und das vor dem Tag geprüfte JAR identisch bleiben.
Der [Release-Ablauf](../releases.md#notizen-und-artefakt) beschreibt die Grenze.

Der native Flow-`Markdown`-Baustein stellt die Notizen in einer Cosmos-Card
dar und bereinigt das gerenderte HTML. Metadaten und Navigation verwenden
die vorhandenen Flow-Komponenten des Portals.

Eine zusätzlich generierte statische HTML-Fassung wäre später für ein externes
Release-Portal denkbar. Für unsere derzeitige Anwendung bietet sie keinen klaren
Vorteil, erhöht aber den Build-/Gestaltungsaufwand. Deshalb zunächst nur Markdown
als Quelle plus native Darstellung vorsehen.

## Inhalt und Bedienung

Alle angemeldeten Benutzer sehen in der ersten Fassung dieselben Release-Notizen.
Allgemein lesbare Hinweise zu Admin-Funktionen gehören ebenfalls in das gemeinsame
Changelog. Die Notizen erklären, was Benutzer mit einer Neuerung tun können.
Technische Konfigurationsnamen, interne Findings, sensible Details und kleinteilige
Designkorrekturen gehören nicht hinein. Eine Kennzeichnung nach Zielgruppe ist
als mögliche Erweiterung im [TODO](../../TODO.md#künftige-anwendungsfunktionen)
festgehalten.

Vorhandenen Seitenpfad, Header, Cards, Typografie und mobile Abstände übernehmen.
Keine ungefragte Änderung bestehender Seiten oder automatische Gelesen-Zähler
einführen. DE/EN-Sprachwechsel, Tastatur, Links und natürliche Zeilenumbrüche
gehören zur Abnahme. Ein später gewünschtes „Neu“-Kennzeichen beziehungsweise
„seit letztem Besuch“ wäre ein eigener gespeicherter Zustand und noch kein Teil
dieser ersten Planung.

## Offene Umsetzungspakete

- [ ] Sprachwechsel, Archiv mit mehreren Ausgaben und schmale Ansichten im Portal
  prüfen. Die automatisierten Browserprüfungen stehen im
  [Testbedarf](../testbedarf.md#browser-tests).

Grundlagen: [Vaadin Markdown](https://vaadin.com/docs/latest/components/markdown),
[redaktionelle Changelogs](https://keepachangelog.com/en/2.0.0/).
