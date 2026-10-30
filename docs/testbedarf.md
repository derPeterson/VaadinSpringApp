# Offener Testbedarf

Hier stehen ausschließlich offene Aufgaben zur Ergänzung oder Anpassung
automatisierter Tests. Pflege und Abnahme: [Testablauf](tests.md#übergabe-offener-testaufgaben).

## Browser-Tests

- Offen: Die vollständige Adminübernahme in den bestehenden Admin-Suiten DE/EN
  absichern: Dashboard-Schnellzugriffe und rollenabhängige Navigation, acht
  Konfigurationsgruppen mit Feldhilfe ohne Layoutverschiebung und ohne Überdecken
  des sichtbaren Copyrightfooters (auch nach Scrollen/Resize), Pflichtpunkte und
  Checkboxen, Mailtest mit globaler Validierung und SMTP-Zugang, vertikalem
  Vergrößern/Verkleinern der Textarea mit Griff direkt an der Eingabeecke,
  fester Breite und erhaltener Höhe nach
  Texteingaben sowie Benachrichtigungen
  mit bündiger zweispaltiger Vorschau, filterbarer zweizeiliger Empfängerauswahl,
  Anzahl innerhalb des Felds, vertikal vergrößerbarer Textarea auch deutlich über
  zehn Zeilen ohne Leerraum zwischen Feldrand und Ziehgriff, vollbreiten
  Aktionstrennlinien, Empfängerdetails neben den Avataren (Maus und Tastatur),
  grauer Leeranzeige, gestapelter Empfängerliste und Herkunftsbadge in der Vorschau,
  identischer Textposition beim Wechsel zwischen allen Konten und Einzelauswahl,
  Entwertung und reduziertem Verlauf. Benutzerdialoge mit nativer Rollenmehrfachauswahl,
  übersetzter Auswahlanzahl, Filterung durch Tippen ohne Auswahlverlust,
  mindestens einer Rolle und berechtigtem Rollenangebot
  sowie Rollen/Aktiv nebeneinander beziehungsweise mobil gestapelt und Hinweisfooter
  mit durchgehender Trennlinie prüfen. Die angepassten Rollen-Selektoren sind kompiliert;
  der automatisierte DE-/EN-Lauf bleibt offen. Benutzerfilter
  als Entwurf prüfen: Mehrfachauswahl innerhalb einer Gruppe Oder, zwischen Gruppen
  Und; Suche und Zurücksetzen erst nach Anwenden, Abbrechen/Escape/Außenklick ohne
  Tabellenänderung. Desktop-SideNav mit echten Mausklicks prüfen: Der Inhaltsrahmen darf die
  Navigationslinks nicht überdecken. Hover und Auswahl auch für Startseite und
  Kontotabs vergleichen: gleiche Hoverfläche, aktive Auswahl bleibt blau,
  Zeilen bleiben linksbündig und vertikal zentriert. Mailtest-Innenabstände
  auf Desktop und Smartphone sowie identische Kopf-/Footerfarbe der Feldhilfe
  absichern. Mobile SideNav, Dialoge und Pagination beibehalten. Gezielte
  interaktive Sichtung und isolierte JUnit-Regressionen ersetzen den Suite-Lauf nicht.


- Offen: Getrennte Passwortlink-Ergebnisse in DE/EN am aktuellen Image prüfen:
  unbekannt/benutzt/widerrufen rot, abgelaufen auch nach erneutem Öffnen gelb,
  passende Titel und Texte, kein Passwortformular. Ablauf nach bereits geöffnetem
  Formular muss bei Absenden abgewiesen werden und die Ablaufansicht zeigen.
  Page-Helfer und vorhandener Ablauf-Test sind angepasst; Suite-Lauf noch offen.


- Offen: Die Besucherübernahme in den vorhandenen Anmelde-, Registrierungs-,
  Passwortlink-, Verifikations-, E-Mail-Bestätigungs- und Wartungsfällen DE/EN
  automatisiert absichern. Öffentliche Navigation und Sprachwahl, mobile Formulare,
  native Picker, Pflichtmarkierung und globale Validierungsmeldungen sowie die
  bestehenden Link-/Anmeldeverträge erhalten. Die getrennte interaktive Sichtung
  des Besuchervergleichs ersetzt diesen Suite-Nachweis nicht.

- Offen: Die Lucide-Umstellung der ausdrücklich gesetzten Icons in den vorhandenen
  Besucher-, Konto- und Adminsuiten DE/EN absichern: Header, Navigation, Dialoge,
  Meldungen und Statusanzeigen. Die angepassten bestehenden Icon-Selektoren prüfen;
  native Feld-, Checkbox- und Sortiersymbole bleiben bewusst bei Vaadin.
  Gezielte interaktive Sichtung ersetzt diesen Suite-Nachweis nicht.

- Offen: Angepasste `AccountSettingsBrowserTest`-Fälle in DE/EN am selben aktuellen
  Image ausführen: Geschlecht auswählen, speichern, neu laden und unverändert
  wiederfinden; E-Mail nur lesbar und fokussierbar, mobile Navigation und
  E-Mail-Dialog mit neuem Header-/Footerlayout. Die nativen Status-/Avatarprüfungen
  an die abgenommene Kontoreferenz anpassen, einschließlich Statuspunkt und Rahmen.
  Dabei Sicherheitskarten, kontextbezogene Überschriften und globale Kontomeldungen
  im Referenzstil mitprüfen; Nachrichtenanzahlen bleiben echte Werte.
  Der gezielte interaktive Sicht-/Funktionsvergleich ersetzt diesen Suite-Nachweis nicht.
- Offen: Die angepassten Header-/Sprach-, Nachrichten- und Sitzungssuiten in DE/EN
  am selben aktuellen Image ausführen. Gemeinsame Portalnavigation auf Startseite,
  Inbox und Konto, mobile Überlagerung samt Escape/Fokusrückgabe, echte Lesefilter-
  zähler und Seitengrößen prüfen. Neue Einzel-/Sammelbestätigungen für Lesen/Löschen
  sowie aktive und gespeicherte Gerätezugänge ausdrücklich bestätigen und abbrechen;
  Abbrechen verändert nichts, neue Nachrichten bleiben außerhalb der erfassten
  Lesebestätigung. Benutzermenü, Statusauswahl, Profilbild-/Crop-Zustände und globale
  Meldungen mitprüfen. Manuelles Neuladen von Postfach und Sitzungsliste mit
  Erfolg-/Fehlermeldung und stille
  automatische Aktualisierung sowie die direkt sichtbaren Statusoptionen im
  Benutzermenü absichern. Page-Helfer sind angepasst; der automatisierte DE-/EN-Lauf
  wurde im begrenzten Umsetzungsauftrag noch nicht durchgeführt.

- Offen: alle neun Admin-Zeitfelder mit Zahl und Einheit in DE/EN.
  Einheit wechseln, leeren Standardwert, Bereichsgrenzen, Speichern/Neuladen und
  aktuelle Umrechnung ausschließlich in der Feldhilfe automatisiert prüfen.
  Außerdem native Konfigurations-Checkboxen links beschriftet, rechts angeordnet
  sowie Klick auf Text und Kontrollkästchen prüfen.
  Gezielte JUnit-Abdeckung für Umrechnung, Grenzen und Sprachwechsel vorhanden.

## JUnit-Tests

Derzeit keine offenen Ergänzungen.

Menschliche Abnahmen und die nicht beauftragte optionale externe SMTP-Abnahme
bleiben im [manuellen Katalog](manual-tests/README.md).
