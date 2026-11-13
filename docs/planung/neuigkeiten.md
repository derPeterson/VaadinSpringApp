# „Was ist neu?“ im Portal

## Empfehlung

Noch nicht umgesetzt. Ein kleiner, bewusst erreichbarer Neuigkeitenbereich passt
zum Portal und kann seine Entwicklung für Benutzer nachvollziehbar machen. Er
ersetzt weder Startseite noch Dashboard. Ein automatisches Popup bei jeder
Anmeldung ist nicht Teil des Vorschlags.

Als erste Oberfläche bietet sich ein Navigationseintrag **Was ist neu?** im Portal
an. Er öffnet eine Cosmos-Seite mit Version, Datum und wenigen Funktionsnotizen
pro Release. Der jüngste veröffentlichte Release steht zuerst. Auf der Startseite
kann später ein unaufdringlicher Link ergänzt werden. Ob der Besucherbereich den
gleichen Inhalt sehen soll, wird vor der UI-Umsetzung entschieden. Admins nutzen
dieselbe Quelle; es entsteht keine zweite redaktionelle Verwaltungsoberfläche.

## Eine Quelle, keine doppelte HTML-Pflege

Die geplante `CHANGELOG.md` aus der [Releaseplanung](releases.md) enthält sowohl
die Entwicklerübersicht als auch die DE-/EN-Inhalte. Den Runtime-Import beim Build
als dedizierte Classpath-Ressource einrichten. Im gepackten JAR ausschließlich
Streams verwenden, niemals `src/`- oder lokale `docs/`-Pfade zur Laufzeit lesen.

Version und Datum bekommen ein kleines festgelegtes Abschnittsformat. DE und EN
stehen darunter in identisch gegliederten Sprachabschnitten. Ein kleiner Loader
trennt Metadaten und ausgewählten Sprachinhalt. Der noch unveröffentlichte Bereich
bleibt in der Benutzeroberfläche unsichtbar. Ein Build-/Helpertest prüft doppelte
Versionen, fehlende Sprache, ungültige Datumsangaben und leere Releaseabschnitte.

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

Die Notizen erklären, was Benutzer mit einer Neuerung tun können. Technische
Konfigurationsnamen, interne Findings, sensible Details und kleinteilige
Designkorrekturen gehören nicht hinein. Administrative Funktionen klar als solche
bezeichnen. Ob bestimmte Einträge rollenabhängig angezeigt werden müssen, vor
der Festlegung des Quellformats fachlich entscheiden; reine DOM-Ausblendung
wäre kein Zugriffsschutz für vertrauliche Inhalte.

Vorhandenen Seitenpfad, Header, Cards, Typografie und mobile Abstände übernehmen.
Keine ungefragte Änderung bestehender Seiten oder automatische Gelesen-Zähler
einführen. DE/EN-Sprachwechsel, Tastatur, Links und natürliche Zeilenumbrüche
gehören zur Abnahme. Ein später gewünschtes „Neu“-Kennzeichen beziehungsweise
„seit letztem Besuch“ wäre ein eigener gespeicherter Zustand und noch kein Teil
dieser ersten Planung.

## Offene Umsetzungspakete

- [ ] Position in der Navigation, Besucherzugang und erste Darstellung gemeinsam
  anhand einer kleinen Skizze abnehmen.
- [ ] Quellformat mit der Releaseplanung festlegen und genau eine Ressource in
  den JAR-Build übernehmen. Noch kein Changelog mit erfundenen Alt-Releases anlegen.
- [ ] Loader und native Markdown-/Cosmos-Darstellung umsetzen, Formatfehler mit
  sinnvollen Helpertests absichern.
- [ ] Sichtbare Inhalte, DE/EN, Sprachwechsel sowie Desktop/Smartphone gezielt
  isoliert prüfen. Manuelle Abnahme der tatsächlichen Texte danach erhalten.

Die technische Planung wird vor einer späteren Umsetzung mit dem dann aktuellen
Projektstand abgeglichen. Sie ist keine Freigabe zum Einbau der Oberfläche.

Grundlagen: [Vaadin Markdown](https://vaadin.com/docs/latest/components/markdown),
[redaktionelle Changelogs](https://keepachangelog.com/en/2.0.0/).
