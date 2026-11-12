# Einheitliche Seitenpfade

## Bestandsaufnahme und Entscheidungsvorschlag

Vaadin 25.3.5 enthält `Breadcrumbs` und `BreadcrumbsItem` bereits im eingebundenen
Flow-JAR. Die Quell-API wurde geprüft: `Mode.MANUAL`, `add`, `setText` und
`setPath` unterstützen unsere dynamischen Bereiche. Ein Item ohne Pfad ist nicht
klickbar. Kein Add-on und kein selbst gebauter Breadcrumb-Ersatz sind nötig.
[Offizielle Dokumentation](https://vaadin.com/docs/latest/components/breadcrumbs).

`CosmosSectionHeaderComponent` erhält bisher einen einzelnen `Span` für den Pfad.
`PublicPageComponent` baut den öffentlichen beziehungsweise eingeschränkten
Kontext separat auf. Beide sollen künftig denselben fachlichen Pfadbaustein mit
nativen Breadcrumbs verwenden. **Manueller Modus** ist nötig, weil Abschnitte und
Tabs über Queryparameter wechseln und nicht jeweils eigene Flow-Routen sind.

| Bereich | Derzeit | Vorgeschlagener blauer Elternpfad / große Überschrift |
| --- | --- | --- |
| Besucher: Anmeldung, Registrierung, Reset | „Portal · Besucherbereich“ | `PORTAL` / aktueller Seitentitel |
| Portal-Startseite | Freier Begrüßungskontext | `PORTAL` / Startseite |
| Persönliche Daten, Sicherheit, Geräte | „Mein Konto“, ohne wiederholte aktuelle Seite | `PORTAL / MEIN KONTO` / Persönliche Daten, Sicherheit oder Geräte & Sitzungen |
| Eigene Benachrichtigungen | „Portal“ | `PORTAL` / Benachrichtigungen |
| Admin-Benutzer, Tickets, Benachrichtigungen | Administration plus wiederholte aktuelle Seite | `ADMINISTRATION` / jeweiliger Seitentitel |
| Konfigurationsgruppen | Administration / Konfiguration / Gruppe, H1 nochmals Konfiguration | `ADMINISTRATION / KONFIGURATION` / gewählte Gruppe |
| Mailtest | Eigene Kontextkombination | `ADMINISTRATION` / Mailtest |
| 2FA nach Kontoeinstieg | „Mein Konto · Sicherheit“, andere Schreibweise | `PORTAL / MEIN KONTO / SICHERHEIT` / aktueller Schritt |
| 2FA beim Login, Verifikation, Reaktivierung | „Portal · Sichere Anmeldung“ | `PORTAL` / aktueller Schritt |

Die große Überschrift nennt den Ort; der blaue Pfad wiederholt ihn nicht. Die
vorhandenen Panelüberschriften bleiben erhalten. Einheitlich Großschreibung und
`/` als Trennung verwenden, mit den bestehenden blauen Tokens und Abständen.
Der Punkt am großen Seitentitel bleibt. Die englischen Bezeichnungen kommen aus
dem Textkatalog, nicht aus zusammengestellten deutschen Strings.

## Ziele und eingeschränkte Abläufe

- Portal führt zur bestehenden Startseite. „Mein Konto“ führt zur vorhandenen
  Profilansicht `account?section=profile`, Sicherheit zu
  `account?section=security&tab=portal` gemäß den vorhandenen Routenkonstanten.
- Administration führt zum Dashboard. Konfiguration führt zur ersten für den
  aktuellen Benutzer zulässigen Gruppe; keine Superadmin-Gruppe als allgemeines
  Standardziel fest verdrahten. Bestehende `section`-/Gruppenparameter verwenden.
- Der aktuelle Untertab bleibt ein Tab. Er wird nicht noch einmal als großer
  Seitentitel oder zusätzlicher Breadcrumb kopiert, solange er keine eigene Seite ist.
- Im gesperrten 2FA-, Wiederherstellungs-, Hilfs-, Verifikations- und
  Reaktivierungsablauf **alle** Kontextitems ohne Pfad ausgeben. Die bestehenden
  expliziten Zurück-/Abmeldeaktionen und serverseitigen Sperren bleiben maßgeblich.

## Noch auszuführen

- [ ] Gemeinsame fachliche Pfaddefinition mit nativen Breadcrumbs einführen,
  bestehende Headerpositionen und Stylewerte erhalten.
- [ ] Öffentliche und angemeldete Header sowie Admin-Gruppen mit obiger Zuordnung
  anbinden. Keine automatische Ableitung aus sichtbaren Übersetzungstexten.
- [ ] Linkziele, Rechte, Queryparameter, Sprachwechsel und gesperrte Abläufe prüfen.
- [ ] Vorher-/Nachhervergleich DE/EN Desktop/Smartphone, Fokus und Überlauf prüfen.

Dies ist ein seitenübergreifender Umbau, größer als die Einheitenauswahl. Die
native API ist vorhanden, die optische Angleichung aber noch nicht implementiert
oder im Browser geprüft. Die Pflichtpakete der Nacht haben Vorrang.
