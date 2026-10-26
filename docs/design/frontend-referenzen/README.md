# Freigegebene Frontend-Referenzen

Feste HTML-Momentaufnahmen der gemeinsam abgestimmten Gestaltung. Die Dateien
liegen auf Benutzerwunsch außerhalb von `target/`, damit ein Clean sie nicht
entfernt. Sie gehören zur Git-versionierbaren Projektdokumentation.

## Öffnen

| Bereich | Vorschau mit Steuerung | Eigenständige Oberfläche |
| --- | --- | --- |
| Angemeldete Benutzer | [Referenz öffnen](index.html) | [Portal](portal.html) |
| Nicht angemeldete Benutzer | [Referenz öffnen](visitors.html) | [Besucherportal](public.html) |

`index.html` oder `visitors.html` im Browser öffnen. Die Vorschauen sind gegenseitig
verlinkt. Für die Steuerung alle vier HTML-Dateien im selben Ordner belassen.
Die zwei Oberflächendateien lassen sich auch einzeln öffnen; die äußere Steuerung
ergänzt Bildschirmgrößen, Beispieldaten und bei Besuchern die Seiten-/Zustandswahl.

## Inhalt und Grenzen

- CSS, Inter-Schriften, SVGs und Demo-Skripte sind in den HTML-Dateien eingebettet.
  Zum Betrachten sind weder Anwendung noch Build, Datenbank, CDN oder laufender
  Vorschau-Server nötig.
- Die angemeldete Referenz enthält Startseite, Benachrichtigungen, persönliche
  Daten, Sicherheit, Geräte/Sitzungen und die zugehörigen Overlays/Dialoge.
- Die Besucherreferenz enthält neun öffentliche Szenen samt Beispielzuständen,
  gemeinsamen Controls und globalen Meldungen.
- Ausschließlich erfundene Beispieldaten. Keine echten Zugangsdaten eingeben.
  Aktionen simulieren das Design; sie melden niemanden an, verschicken keine
  E-Mail und verändern keine echten Konten oder Sitzungen. Neuladen setzt die
  lokalen Beispieldaten zurück. Die Sprachauswahl bleibt Designvorschau.
- Gestalterische Abnahme des HTML-Prototyps; echte Mobilgeräte, Bildschirmtastatur,
  Screenreader und die technische Umsetzung sind gesondert zu prüfen.
- Die Service-Logoauswahl ist zurückgestellt. Die aufbewahrten
  [Kandidaten D/E/F](../service-logo/README.md) bleiben unabhängig erhalten.

## Pflege

Offline-Prüfung ohne zusätzliche Pakete, aus dem Projektroot:

```powershell
node docs/design/frontend-referenzen/pruefen.mjs
```

Die Prüfung kontrolliert lokale Verknüpfungen, eingebettete Scripts/CSP und die
Besucher-Zustandsauswahl einschließlich nachlaufender Hash-Navigation sowie
Absender-/Origin-Prüfung für HTTP und lokale Dateien. Auch die Leerung sichtbarer
Passwortentwürfe nach einer erfolgreichen Demo-Aktion ist abgedeckt. Sie verwendet für die
Routinglogik eine kleine DOM-Testumgebung; eine gerenderte Browserprüfung oder
eine Prüfung der echten Anwendungsabläufe ersetzt sie nicht.

Diese Dateien bilden den freigegebenen Stand ab. Spätere Versuche zunächst in der
Spielwiese bearbeiten und eine hier gesicherte Referenz nur nach bewusst
abgestimmter Änderung ersetzen. Der aktuelle Feature-Plan bleibt unter
[Designkritik mit Stitch](../../planung/designkritik-stitch.md).

Die CSS-Einbettung dient der vollständigen, transportablen Referenzsicherung.
Die Vaadin-Umsetzung verwendet weiterhin die gemeinsamen Theme-Quelldateien und
Komponenten gemäß [Theme-Anleitung](../../theme.md). Generatoren und temporäre
Stitch-Rohentwürfe unter `target/designkritik-stitch/` gehören nicht zu dieser
HTML-Sicherung und benötigen bei Bedarf eine eigene Sicherung vor einem Clean.
