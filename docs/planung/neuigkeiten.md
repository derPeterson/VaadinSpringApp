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

Die bestehende `CHANGELOG.md` aus dem [Release-Ablauf](../releases.md) enthält sowohl
die Entwicklerübersicht als auch die DE-/EN-Inhalte. Den Runtime-Import beim Build
als dedizierte Classpath-Ressource einrichten. Im gepackten JAR ausschließlich
Streams verwenden, niemals `src/`- oder lokale `docs/`-Pfade zur Laufzeit lesen.

Version, Datum sowie DE-/EN-Abschnitte haben bereits ein festgelegtes Quellformat.
Titel, Einstieg, die beiden Inhaltsabschnitte und optionale Bildverweise werden
in den jeweiligen Sprachabschnitten der `CHANGELOG.md` gepflegt.
Ein kleiner Loader trennt Metadaten und ausgewählten Sprachinhalt. Der noch
unveröffentlichte Bereich bleibt in der Benutzeroberfläche unsichtbar.
Der Release-Helfer prüft bereits doppelte
Versionen, fehlende Sprache, ungültige Datumsangaben und leere Releaseabschnitte.
Der spätere Runtime-Import muss dieses Format übernehmen; kein zweites Format einführen.

Vaadin bietet einen nativen Flow-`Markdown`-Baustein für Formatierung und Listen.
Die offizielle Dokumentation zeigt die Java-API und interne HTML-Bereinigung.
Vor der späteren Umsetzung die tatsächlich verwendete Vaadin-Version und ihr
Artefakt nochmals prüfen. Damit lassen sich die Notizen in native Cosmos-Cards
einbetten, ohne unabhängig gestaltete HTML-Seiten zu erzeugen. Metadaten und
Navigation bleiben normale Flow-Komponenten. Kein Markdown-Editor, keine
Benutzereingaben und keine Erweiterung zu einem allgemeinen CMS.

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

- [ ] Bestehendes Changelog-Format übernehmen und genau eine Runtime-Ressource in
  den JAR-Build einbinden. Nur bewusst freigegebene Releaseabschnitte anzeigen.
- [ ] Loader und native Markdown-/Cosmos-Darstellung umsetzen, Formatfehler mit
  sinnvollen Helpertests absichern.
- [ ] Zugriff bei direktem Aufruf ohne Anmeldung, sichtbare Inhalte, DE/EN,
  Sprachwechsel sowie Desktop/Smartphone gezielt isoliert prüfen. Manuelle
  Abnahme der tatsächlichen Texte danach erhalten.

Die technische Planung wird vor einer späteren Umsetzung mit dem dann aktuellen
Projektstand abgeglichen. Sie ist keine Freigabe zum Einbau der Oberfläche.

Grundlagen: [Vaadin Markdown](https://vaadin.com/docs/latest/components/markdown),
[redaktionelle Changelogs](https://keepachangelog.com/en/2.0.0/).
