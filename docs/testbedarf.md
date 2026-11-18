# Offener Testbedarf

## Browser-Tests

- **Native Löschkreuze in bearbeitbaren Texteingaben und TextAreas — offen.**
  In DE/EN auf Desktop und Smartphone Benutzeranlage/-bearbeitung, externen
  Registrierungsabschluss, Konto-/Passwortdialoge, Anmeldeanbieter, Mail-Test,
  Benachrichtigungen, Tickets und Ersatzcodeeingabe stichprobenartig prüfen.
  Bei gefüllten bearbeitbaren Feldern X sichtbar; X/Esc leeren den Wert bzw.
  gesamten TextArea-Entwurf und lösen vorhandene Änderungs-/Dirty-Erkennung aus.
  Bei Leerwert, Disabled und Readonly kein X. Einzelne OTP-Ziffern und E-Mail-Code
  ohne X, Ersatzcode mit X; Methodenwechsel erhält diese Unterscheidung. Anzeige-/
  Kopierfelder schreibgeschützt. Zahlen-/Dauerfelder behalten ihre Löschaktionen.
  Vorhandene Dialogfälle auf Esc prüfen: Ein gefülltes fokussiertes Feld wird
  zunächst geleert, statt gleichzeitig den Dialog zu schließen. Kein Browserlauf freigegeben.

- **Persönliche Daten: Prüfung erst beim Speichern und native Löschkreuze — offen.**
  In DE/EN auf Desktop und Smartphone Vor-/Nachnamen eingeben und über das native X
  leeren: zunächst keine Fehlermarkierung; beim Speichern Notification und passende
  Markierungen. Änderungen entfernen alte Feldfehler, Verwerfen stellt gespeicherte
  Werte wieder her. Pflichtsprache und ungültige Geburtsdaten weiterhin prüfen.
  Bei ursprünglich leerem Geburtsdatum unlesbaren Text eingeben: Unsaved-Changes-
  und Browser-Verlassensschutz müssen trotz fehlender Rotmarkierung greifen.
  Leeren beseitigt die Änderung, solange sonst nichts verändert wurde. Die E-Mail
  bleibt schreibgeschützt. JUnit prüft den Serverablauf; kein Browserlauf freigegeben.

- **Pflichtfeldindikatoren: natives rotes Sternchen — offen.**
  In DE/EN auf Desktop und Smartphone Pflicht- und optionale Felder in Registrierung,
  Login, Benutzerbearbeitung, Konfiguration, Benachrichtigungen und 2FA/Tickets prüfen.
  Pflichtfelder zeigen ein rotes Sternchen hinter der Beschriftung; kein blauer Punkt
  und keine violette Pflichtmarkierung bei internen Vermerken. Optionale Felder
  erhalten keinen Indikator. Disabled, Readonly und Feldfehler bleiben unterscheidbar.
  Kein Browserlauf für diesen Auftrag freigegeben; sichtbare Abnahme steht aus.

- **Login: Fehler erst beim Absenden — offen.**
  In DE/EN auf Desktop und Smartphone E-Mail/Passwort eingeben, leeren und verlassen;
  ein falsches E-Mail-Format bleibt vor dem Absenden ebenfalls unmarkiert. Beim
  Klick oder Enter passende Notification und Markierung anzeigen; Änderungen löschen
  nur die alte Markierung am bearbeiteten Feld. Falsche Zugangsdaten markieren beide
  Felder mit allgemeiner Meldung. Wartung/Drosselung/technische Fehler nicht als
  Eingabefehler markieren; Fehlerrückmeldungen nach Weiterleitungen ebenfalls ohne
  Eingabemarkierung anzeigen. Gültige Anmeldung samt 2FA, ausstehender Verifikation,
  Reaktivierung und Sitzungslimit weiterführen. JUnit prüft den Serverablauf;
  der Client-/Blur-Nachtest ist noch nicht freigegeben.

- **Registrierung: Fehler erst beim Absenden — offen.**
  In DE/EN auf Desktop und Smartphone Pflichtfelder eingeben, leeren und verlassen;
  ebenso unvollständige E-Mail-Adressen eingeben: vor dem Absenden keine rote
  Markierung oder Notification. Beim Registrieren erscheinen übersetzte Meldungen
  und passende Feldmarkierungen. Beim Tippen alte Markierung am bearbeiteten Feld
  zurücknehmen, andere Markierungen erhalten und beim nächsten Absenden neu prüfen.
  Unlesbares/zukünftiges Geburtsdatum ablehnen; ein optionales leeres Datum zulassen.
  Bestehenden Registrierungsfall nutzen. JUnit prüft Serverablauf und Komponenten;
  der tatsächliche Client-/Blurablauf benötigt den noch nicht freigegebenen Browsernachtest.

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
  berücksichtigen, auch bei langen Meldungen und kleiner Fensterhöhe. Der
  rechte Desktopabstand ist auf 24 px erweitert: mit und ohne Seiten-/AppLayout-
  Scrollleiste ausreichend Abstand zur sichtbaren Leiste prüfen; oberer Abstand
  und Smartphoneposition bleiben gleich. Nur der
  native Aura-Innenabstand bleibt; Warnung zeigt ein gelbes Dreieck, Fehler ein
  rotes Kreuz im Kreis. Gemeinsame DE-/EN-Titel ohne Ausrufezeichen anzeigen;
  Nachrichtentexte und Links wie „Passwort vergessen?“ bleiben unverändert.
  JUnit prüft Position, Umschaltung, Icons und Fensterisolation;
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
