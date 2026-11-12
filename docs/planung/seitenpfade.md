# Einheitliche Seitenpfade

## Freigegebene Zuordnung

Vaadin 25.3.5 enthält `Breadcrumbs` und `BreadcrumbsItem` bereits im eingebundenen
Flow-JAR. Die Quell-API wurde geprüft: `Mode.MANUAL`, `add`, `setText` und
`setPath` unterstützen unsere dynamischen Bereiche. Ein Item ohne Pfad ist nicht
klickbar. Kein Add-on und kein selbst gebauter Breadcrumb-Ersatz sind nötig.
[Offizielle Dokumentation](https://vaadin.com/docs/latest/components/breadcrumbs).

`CosmosSectionHeaderComponent` und `PublicPageComponent` behalten ihre
vorhandenen Headerpositionen. Beide verwenden denselben fachlichen `CosmosBreadcrumbsComponent` mit
nativen Breadcrumbs. **Manueller Modus** ist nötig, weil Abschnitte und
Tabs über Queryparameter wechseln und nicht jeweils eigene Flow-Routen sind.

| Bereich | Blauer Elternpfad | Große Überschrift |
| --- | --- | --- |
| Besucherseiten einschließlich öffentlicher Startseite, Anmeldung, Registrierung und Reset | `PORTAL` | Aktueller Seitentitel |
| Angemeldete Startseite | `PORTAL` | Startseite |
| Persönliche Daten, Sicherheit, Geräte & Sitzungen | `PORTAL / MEIN KONTO` | Jeweiliger Seitentitel |
| Eigene Benachrichtigungen | `PORTAL` | Benachrichtigungen |
| Admin-Dashboard, Benutzerverwaltung, Tickets und Benachrichtigungen | `ADMINISTRATION` | Jeweiliger Seitentitel |
| Sämtliche Konfigurationsgruppen einschließlich Anmeldeanbieter und 2FA | `ADMINISTRATION / KONFIGURATION` | Gewählte Gruppe |
| Mailtest | `ADMINISTRATION` | Mail Test |
| 2FA, Wiederherstellung und Hilfe aus dem Konto | `PORTAL / MEIN KONTO / SICHERHEIT` | Aktueller Schritt |
| 2FA beim Login, ausstehende Verifikation und Reaktivierung | `PORTAL` | Aktueller Schritt |
| Verifikations-, E-Mail-Änderungs- und Reaktivierungslinks | `PORTAL` | Aktueller Seitentitel |

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

## Noch offen

- [ ] Gemeinsame Sichtung der Seitenpfade und Überschriften durch den Benutzer.

Der neue Baustein und die Anbindung aller Header sind umgesetzt. Die geltende
Gestaltung steht in der [Theme-Anleitung](../theme.md). Die gezielte Suite
`breadcrumbs` ist in DE und EN mit jeweils drei erfolgreichen Browserfällen
abgeschlossen: Desktop und Smartphone, alle Konfigurationsgruppen, zulässige
Elternziele auch für gewöhnliche Admins, Profilentwurfsschutz, Sprachwechsel,
Tastaturfokus und natives Überlaufmenü. 2FA, Hilfe, ausstehende E-Mail-Bestätigung,
Verifikationslink und Reaktivierung haben keine Pfadlinks; die bestehenden
Direktaufrufsperren bleiben erhalten. Vorher-/Nachheraufnahmen wurden verglichen.
Beide Sprachläufe verwenden dasselbe unveränderte Vorschauimage. Zusätzlich sind
246 gezielte Anwendungstests und 26 Browser-Auswahltests erfolgreich, einschließlich
Textkatalog und Sprachwechsel. Spotless, Checkstyle, CSS- und Diffprüfung sind
erfolgreich; die geänderten Bereiche wurden auf Lesbarkeit geprüft.

Ein gezielter Test ersetzt keine große Testrunde. Der reguläre Testimage-Build
mit vollständigen Anwendungstests ist inzwischen erfolgreich. Der aktuelle
Nachweis steht im [Prüfstand](../status.md), verbleibender automatisierter Bedarf
im [Testbedarf](../testbedarf.md). Bestehende Anwendungsdaten oder laufende
Anwendungen wurden nicht als Testfixture verwendet.

Nach Build und Anwendungsneustart ist die Änderung sichtbar; eine Datenbankänderung
ist nicht erforderlich. Bei der gemeinsamen Sichtung Portal/Konto, Admin-Konfiguration
und einen eingeschränkten Sicherheitsablauf ansehen. Weitere Entscheidungen sind
für die technische Umsetzung nicht offen.
