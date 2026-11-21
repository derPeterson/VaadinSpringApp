# Offener Testbedarf

## Browser-Tests

Oberflächenprüfungen in DE und EN auf Desktop und schmaler Ansicht ausführen,
sofern das Szenario keine andere Breite vorgibt. Manuelle Abnahmen gelten weiter.

- **Portalneuigkeiten — offen.**
  In DE und EN am Desktop sowie bei 320 px prüfen. „Was ist neu?“ steht mit der
  Tabler-Glühbirne zwischen Startseite und Benachrichtigungen und erscheint mobil
  im Menü. Ohne Anmeldung führt der Direktaufruf zur Anmeldung. Nach dem
  Sprachwechsel bleiben Version, Metadaten und sichtbare Release-Notizen
  konsistent. Das Versionsbadge
  entspricht den Cosmos-Badges für Kopfmetadaten. Zwischen Versionsbadge und Datum
  steht ein mittiger Punkt. Beim neuesten sichtbaren Release „HIGHLIGHTS“ mit
  dem Lucide-Icon `sparkles` und „WEITERE VERBESSERUNGEN“ in Großbuchstaben
  prüfen. Release-Titel und Kartentitel sind halbfett. Einleitung,
  Kartentexte und Verbesserungen haben reguläres Schriftgewicht. Bei rund 875 px
  Rasterbreite passt „Wiederherstellungsanfragen“ bei 14 px in eine Zeile. Lange
  Kartentitel bleiben bei schmaler Breite lesbar. Karten zeigen die Icons ihrer
  Bereiche.
  Verbesserungen sind mit Tabler-Icons nummeriert. Mehrstellige Zahlen setzen
  sich aus einzelnen Ziffernicons zusammen. Die Texte bleiben bündig. Der Abstand
  nach der Überschrift ist XL, zwischen den Zeilen L. Ab zwei sichtbaren Ausgaben
  erscheint „FRÜHERE VERSIONEN“ in Primärfarbe mit dem Tabler-Konturicon.
  Archiveinträge starten eingeklappt mit primärfarbenem 16-px-Aufklapppfeil,
  halbfettem Titel, Versionsbadge in Größe S und Datum. Die Metadaten sind durch
  mittige Punkte getrennt, die bei Umbrüchen bei ihrem Text bleiben. Im geöffneten
  Archiv sind die Teilüberschriften halbfett mit Icon und Großbuchstaben.
  Einleitung und Listentexte sind regulär gesetzt. Die Highlight-Zwischenüberschriften sind etwas größer
  und halbfett und enden mit einem Doppelpunkt. Beide Listen starten bei 1.
  Markdown für aktuelle und ältere Ausgaben herunterladen. Der Archiv-Button
  erscheint erst beim Aufklappen rechts unter den Notizen. Die Trennlinie reicht
  über die Kartenbreite, der Button hat denselben seitlichen Innenabstand wie
  der Text. Nach Sprachwechsel Dateiname, Version und UTF-8-Inhalt prüfen.
  Ohne Anmeldung liefert die Download-Adresse keine Datei.
  Optionales Bild mit Beispieldaten prüfen. Im normalen Build bleiben
  Unreleased und der aktuelle Kandidat ausgeblendet.

- **Eigene Wiederherstellungsanfragen als VirtualList — offen.**
  Mit 1/2/5/6/20 Tickets Listenhöhe, Begrenzung auf fünf Zeilen und Scrollen bis
  zum letzten Eintrag prüfen. Lange Titel, Badges, linke Markierung und lückenlose
  Auswahlfläche innerhalb der Zeile prüfen. Die ganze Zeile per Maus, Enter und
  Leertaste aktivieren. Strg+Klick hebt die Auswahl auf. Nach Wiederöffnen erscheint
  der Antwortentwurf, nach Nachladen oder Live-Update bleibt eine bewusst leere
  Auswahl erhalten. Sprachwechsel mit ausgewähltem Ticket prüfen. Die einfachen
  Trennlinien des Admin-Grids mit denen der Karten vergleichen.

- **Schließen-Icons – zusätzliche Tooltips — wartend.**
  Feldhilfe und Nachrichtendetail haben geprüfte Tooltips. In Glockenvorschau,
  Empfänger-Popover, Meldungen, Benutzereditor sowie Konto-, 2FA-, Anbieter- und
  Bestätigungsdialogen tragen die Schließen-Buttons einen lokalisierten
  zugänglichen Namen. Sobald für diese Buttons zusätzliche sichtbare Tooltips
  vorgesehen sind, Anzeige per Maus und Tastatur prüfen.

- **Empfängervorschau – vollständige Popover-Liste — offen.**
  Öffnen per Klick in den Zwischenraum zwischen Avataren und die
  Namen-Tooltips prüfen. Bei 200 Empfängern bis zum letzten Eintrag scrollen.
  Mauswahl, Strg+Klick auf eine andere Zeile und Erhalt der Auswahl beim
  Scrollen prüfen. Die Versandempfänger bleiben unverändert.
  Dialogkopf, abgesetzte Content-Fläche, 380-px-Wunschbreite mit seitlichem
  L-Abstand und Suchfeld (40/13/14 px für Höhe/Schrift/Lupe) vermessen.
  Overlayhöhe bei 200 Empfängern im Fenster halten. Zentrierten Leerzustand
  mit dem der Vorgangsliste vergleichen. Sekundäre E-Mail, lange Texte,
  vollflächige Auswahl samt Akzentlinie, Hover/Fokus,
  einfache Zeilentrenner und Scrollleisten in Chromium und Firefox prüfen.
  Eine zusätzliche Touch-Geste zum Abwählen wartet auf die Entscheidung im TODO.

- **Ticketsystem – Validierung und erneuter Anleitungsversand — offen.**
  Neue Anfrage und internen Vermerk leer absenden, öffentliche Antwort auf
  schmaler Ansicht leer absenden. Die Längengrenze bei allen drei Eingaben
  prüfen. Ohne Absenden keine Markierung, danach konkrete Meldung ohne
  Inline-Text oder Feldnamenzeile. Änderungen löschen die alte Markierung.
  Rechte-, Status- und Versionskonflikte beim Absenden sowie den Zeilenumbruch
  im Hinweis für interne Vermerke prüfen.
  Bei regulären und Entwicklerkonten den unterschiedlichen Untertitel,
  die erneute Freigabe nach 60 Sekunden ohne F5 und das Ende des Nachladens
  beim Verlassen prüfen. Bei regulären Konten blockiert zusätzlich ein
  Mailauftrag in PENDING oder IN_PROGRESS. Sperrtooltip per Maus und Tastatur
  mit fettem „Gesperrt:“ und normalen Markdown-Abständen prüfen. Der offene
  Mailauftrag hat Vorrang vor der Wartezeit. Bei Entwicklerkonten erscheint
  nur die Wartezeit.
  Bei Entwicklerkonten steht der gelbe Hinweis zu Beginn der Vorschau.
  Jeder bestätigte Wiederholungsversand erscheint im Ticket und erzeugt nur
  für reguläre Konten einen Mailauftrag. Sprachwechsel im offenen Dialog prüfen.

- **Konfiguration – Validierung und Sprachwechsel — offen.**
  Vor dem Speichern bleiben die Felder unmarkiert, nach dem Speichern
  werden ungültige Felder markiert. Die Meldung erklärt den ersten Fehler
  ohne zusätzliche Feldnamenzeile. Die offene Meldung soll beim Sprachwechsel mit
  unveränderten Werten übersetzt werden. Nur das bearbeitete Feld verliert
  seine alte Markierung, der Fokus springt beim Fehler nicht.
  Über die bereits geprüften Text-, Ganzzahl- und Dauerbeispiele hinaus
  E-Mail-Werte, Anbieter und Mailqueue-Assistenten sowie weitere Felder
  einbeziehen. Unlesbare Zahlen bei leerem Modellwert dürfen weder
  Standardwerte noch unbegrenzte Aufbewahrung speichern. Echte Leerwerte
  behalten ihren Vertrag. 2FA-Inaktivitätsfehler vor der Identitätsbestätigung
  prüfen, gültiges Speichern bleibt geschützt.

- **Benutzerformular – weitere Grenzfälle beim Speichern — offen.**
  Pflichtrollen und ungültige Geburtsdaten prüfen, insbesondere unlesbare
  Eingabe ohne Modellwert und Änderungsschutz. Die für Pflichtfelder,
  E-Mail und Passwort vorhandenen Prüfungen auf schmaler Ansicht absichern.
  Beim Korrigieren nur die betroffene Markierung entfernen. Zeilenumbruch
  der Passwortregeln und Sprachwechsel bei offener Meldung prüfen.
  Rollen auf Desktop nur links, auf schmaler Ansicht alle Zeilen einspaltig.
  Tastaturfokus der Schalter und Änderungen nur am Entwurf prüfen.

- **Native Löschkreuze — offen.**
  In Benutzereditor, externem Registrierungsabschluss, Kontodialogen,
  Anmeldeanbietern, Mail-Test, Benachrichtigungen, Tickets und Ersatzcodefeld
  stichprobenartig Sichtbarkeit und Leeren per X/Escape prüfen. Leeren muss
  die Änderungs-/Dirty-Erkennung auslösen, auch für den gesamten TextArea-Wert.
  Bei leerem, deaktiviertem oder schreibgeschütztem Feld bleibt das X
  verborgen. OTP-Ziffern und E-Mail-Code mit dem Ersatzcodefeld vergleichen,
  einschließlich Methodenwechsel. Anzeige- und Kopierfelder bleiben
  schreibgeschützt. Escape zuerst am gefüllten Feld prüfen, über den bereits
  geprüften E-Mail-Dialog hinaus.

- **Globale Notifications – lesbare Links — wartend.**
  Sobald eine globale Meldung mit Link vorgesehen ist, dessen Lesbarkeit prüfen.
  Die aktuellen Meldungen erzeugen Text, Listen und Texthervorhebungen.

- **ComboBox-Overlays – sichtbare Auswahlhaken — offen.**
  Die verschärfte Prüfung in `form-details` und `inbox-pagination` in DE und EN
  ausführen. Jede sichtbare ausgewählte Option zeigt ihren Auswahlhaken.

- **Link-Statusseiten und Sicherheitshinweise – weitere Iconvarianten — offen.**
  Abgelaufene und ungültige Links sowie technische Fehler für
  Verifikation und E-Mail-Änderungsbestätigung prüfen. Beim Passwort-Reset
  ist zusätzlich das Icon für technische Fehler offen. Das Warndreieck
  und seine Farbe in Hinweisen bei Kontoaktionen, Reaktivierung,
  Benutzereditor, Neustarthilfe, Wiederherstellung und Tickets prüfen.

## JUnit-Tests

Kein gesonderter offener JUnit-Testbedarf.

Menschliche Sonderprüfungen und Abnahmen stehen im
[manuellen Katalog](manual-tests/README.md#hum-01).
