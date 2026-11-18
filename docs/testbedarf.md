# Offener Testbedarf

## Browser-Tests

- **ComboBox-Overlays: gemeinsame Außen- und Eintragsabstände — offen.**
  In DE/EN auf Desktop und Smartphone Geschlecht/Sprache in Registrierung und
  Profil, Auswahlfelder in Benutzerbearbeitung und Konfiguration, Rollen- und
  Empfänger-Mehrfachauswahl sowie Pagination in Benutzerverwaltung, Inbox und
  Versandhistorie prüfen. Außen überall 8 px nativer Scrollerabstand, zwischen
  Auswahlflächen 2 px; insbesondere letzte ausgewählte Zeile berührt keinen
  Overlayrahmen. Filterung, Scrollen, Tastaturauswahl, Haken und kompakte
  Pagination bleiben bedienbar. Kein Browserlauf für diesen Auftrag freigegeben.

- **Checkboxen: weiße Auswahlmarkierung — offen.**
  In DE/EN auf Desktop und Smartphone Einzelcheckboxen, Checkbox-Gruppen und die
  Tabellen-Gesamtauswahl prüfen: aktive Haken und Teilauswahlstriche weiß, bisherige
  Auswahlflächen und Fokus-/Hoverdarstellung erhalten. Disabled und Readonly
  behalten ihre native Unterscheidbarkeit. Kein Browserlauf für diesen Auftrag freigegeben.

- **DatePicker-Overlay: Textaktionen und Jahresspalte — offen.**
  Vorhandene Datumsauswahl in DE/EN auf Desktop und Smartphone prüfen: „Heute“ und
  „Abbrechen“ als rahmenlose Aktionen in Cosmos-Akzenttextfarbe, Jahresspalte mit
  dunklerer Fläche, feiner Trennlinie und nativer seitlicher Markierung. Auf dem
  Smartphone die native Jahresauswahl öffnen. Fokus, Hover, Jahreswechsel und
  Datumsauswahl bleiben bedienbar; kein Überlauf. Vom Benutzer gesichtet und
  abgenommen; der automatisierte Browsernachtest bleibt ohne Freigabe offen.

- **Globale Notifications: responsive Position, Aura-Abstand und Statussymbole — offen.**
  Bestehende Profil-/Notification-Varianten in DE/EN bei Desktop- und Smartphonebreite
  nachprüfen: ab 768 px oben rechts unter dem Header, darunter unten mittig ohne
  oberen Leerraum. Bei offenem Toast über den Grenzwert verkleinern/vergrößern;
  Inhalt, Dauer und Schließen bleiben erhalten. Header und System-/Bedienleisten
  berücksichtigen, auch bei langen Meldungen und kleiner Fensterhöhe. Nur der
  native Aura-Innenabstand bleibt; Warnung zeigt ein gelbes Dreieck, Fehler ein
  rotes Kreuz im Kreis. JUnit prüft Position, Umschaltung, Icons und Fensterisolation;
  der sichtbare Browsernachtest ist für diesen Auftrag nicht freigegeben.

- **Link-Statusseiten und Sicherheitshinweise: vereinheitlichte Icons — offen.**
  In DE/EN Verifikation, Passwort-Reset und E-Mail-Änderungsbestätigung mit
  abgelaufenem Link (gelbes Dreieck), ungültigem Link und technischem Fehler
  (rotes Kreuz im Kreis) prüfen. Vorhandene Token-/Fehlerfälle und angepasste
  Reset-Page-Assertions nutzen. Sicherheitshinweise bei Kontoaktionen, ausstehender
  Verifikation/Reaktivierung, Benutzerbearbeitung, Neustarthilfe, Wiederherstellung
  und Tickets behalten ihr Dreieck und ihre bisherige Farbgebung. JUnit sichert
  die betroffenen Zustände; der Browserlauf wurde noch nicht freigegeben.

- **Benachrichtigungsvorschau: Löschsymbol nach Aura-Umstellung — offen.**
  In DE/EN auf Desktop und Smartphone eine Nachricht im Glocken-Overlay anzeigen.
  Das rote Löschsymbol muss vollständig und mittig im unverändert großen Button
  sichtbar sein; Hover/Fokus und die abbrechbare Löschbestätigung bleiben erhalten.
  Die bestehenden Benachrichtigungsfälle verwenden und die sichtbare Iconfläche
  berücksichtigen. Der Benutzer hat das wieder sichtbare Löschsymbol in der
  laufenden Anwendung bestätigt. Der automatisierte DE-/EN-Nachtest bleibt offen;
  für diesen Auftrag wurde noch kein Browserlauf freigegeben.

## JUnit-Tests

Kein gesonderter offener JUnit-Testbedarf.

Menschliche Sonderprüfungen und Abnahmen stehen im
[manuellen Katalog](manual-tests/README.md#hum-01).
Zurückgestellte Folgepakete stehen im
[Komponentenplan](planung/vaadin-komponenten-abgleich.md).
