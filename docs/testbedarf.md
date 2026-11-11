# Offener Testbedarf

## Browser-Tests

- E-Mail-Nachweis und administrative Kontoanlage, **wartend auf späteren Browserauftrag**:
  Selbstregistrierung und normale Adminanlage mit echtem Mailpit-Link bestätigen;
  korrektes Passwort führt vorher nur zur gelben Bestätigungsseite, falsches
  Passwort bleibt allgemein. Direkte Portal-/Adminaufrufe, Refresh, Sprachwechsel,
  erneuter Versand mit Wartezeit sowie Rückkehr zur Anmeldung prüfen.
  Superadmin-Ausnahme und ungeprüfte Startkonten zeigen Nicht geprüft; normale Admins können
  die Ausnahme nicht nutzen. Startkonten mit `email-verification-required=true`
  durchlaufen dagegen Bestätigung und initialen Versand. Die vorhandenen
  Browserhilfen für administrative Kontoanlage entsprechend aktualisieren.
  Konto-/Admin-Badges und Versandaktionen verwenden
  gespeicherte Adressen, erhalten Entwürfe und heben Sperren/MFA nicht auf.
  Im Konto steht das Status-Badge neben „E-Mail-Adresse“, die Adresse darunter,
  einschließlich sauberem Umbruch auf schmalen Displays.
  „Verifikation“ ist bei der Superadmin-Kontoanlage ausgewählt und entspricht bei
  Checkbox, Beschriftung und Hilfetext der „Aktiv“-Option. Abwählen legt ausdrücklich
  ein ungeprüftes Konto an. Den Passwortstatus mit und ohne Portalpasswort sowie nach
  erfolgreichem Einrichten prüfen: Badge neben der Überschrift, Aktion Einrichten/Ändern.
  Gezielt isoliert in DE/EN auf Desktop und Smartphone ausführen; Benutzer hat
  diesen Lauf ausdrücklich zurückgestellt. Die JUnit-Fälle ersetzen diese sichtbare Prüfung nicht.

- Gemeinsame Hoverfarbe und Bestätigungshinweise, **offen, Benutzer prüft zunächst selbst**:
  Den zentralen Hoverwert bei Tabellen, neutralen Buttons, Navigation, Eingaben, Auswahl-/
  Kalenderlisten und Scrollleisten in DE/EN auf Desktop/Smartphone nachprüfen.
  Ausgewählte Einträge und aktuelle Seitennummern müssen ihre bisherige Farbe
  behalten. Primär- und Löschbuttons müssen wieder ihren bisherigen blauen bzw.
  roten Hover haben, der gelbe Verwerfen-Button einen Warn-Hover. Normale Badge-, Sprachgruppen- und andere dauerhafte Flächen bleiben
  unverändert. Bei der Anbieterbestätigung erscheint mit Portalpasswort der kurze
  Passworthinweis, ohne Portalpasswort die Anbieteranweisung. Passwort-, App-Code-
  und Ersatzcode-Hinweise enden mit Doppelpunkt. Kein neuer Browserlauf ausgeführt.

- Tages-Suffix und vollständige MultiSelect-Rahmen, **offen, Benutzer prüft selbst**:
  In der Benachrichtigungskonfiguration müssen Trennlinie und Tage ganz rechts
  hinter Löschen und Plus stehen. Mit Feldfokus ist die Einheit wie die Eingabe
  weiß, ohne Fokus bleibt sie dezent. Bei mehreren ausgewählten Rollen und Empfängern
  müssen alle vier Rahmenkanten sichtbar bleiben, mit 2 px Abstand zwischen den
  markierten Flächen. Die Quellkorrektur und die angepassten Prüfungen in
  `form-details` sind noch nicht erneut im Browser ausgeführt. Gezielter
  automatisierter DE-/EN-Nachtest auf Desktop/Smartphone bleibt offen.

- Inhaltsabhängige Buttonbreite im Formularschutz, **offen**: Nach der nachträglichen
  Breitenkorrektur die Suite `form-details` in DE/EN auf Desktop und Smartphone
  prüfen. Beide Buttons sind gleich breit und richten sich nach der längeren
  Beschriftung, statt den Footer zu füllen. Verwerfen steht vor dem blauen
  Weiterbearbeiten-Button. Verwerfen steht am linken und Weiterbearbeiten am rechten
  Footerrand, der freie Platz liegt dazwischen; auf schmalen Displays
  bleibt die Reihenfolge beim Umbruch erhalten. Den Erklärungstext zusätzlich in
  normaler heller Textfarbe bei unveränderten 12 px und Schriftgewicht 400 prüfen.
  Der Kopf zeigt Tabler `file-diff` in den Warnfarben für Icon, Rahmen und dezente Fläche;
  Verwerfen ist gelb mit Tabler `file-x`, Weiterbearbeiten blau mit Lucide `chevron-right`;
  Schließen, Escape und der Anfangsfokus auf Weiterbearbeiten bleiben erhalten.

- Formularschutz bei Browseraktionen, **wartend auf die Umsetzung** aus dem
  [Komponentenplan](planung/vaadin-komponenten-abgleich.md#ungespeicherte-formularänderungen).
  Persönliche Daten und die Speicherformulare der Konfiguration einschließlich
  Anbieter-/2FA-Einstellungen sowie das Benutzerformular mit ungespeicherten
  Eingaben prüfen. Szenarien sind F5/Strg+R, Browser-Aktualisieren, Tab-/Fenster-
  Schließen und Verlassen über Adresszeile oder externe Navigation.
  Erwartung: Eine Warnung erlaubt das Abbrechen mit erhaltenem Entwurf oder
  bewusstes Verlassen. Unveränderte und erfolgreich gespeicherte Formulare
  warnen nicht. Gezielt in DE/EN bei Desktop- und Smartphonebreite prüfen.

## JUnit-Tests

Aktuell keine offenen Ergänzungen.

Menschliche Abnahmen bleiben im
[manuellen Katalog](manual-tests/README.md).
