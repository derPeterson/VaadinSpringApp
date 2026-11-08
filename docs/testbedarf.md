# Offener Testbedarf

## Browser-Tests

- **Ersatzcode im Dialog für Kontoaktionen: offen.** In DE/EN auf Desktop und
  Smartphone eine Standard-Kontoaktion und eine Anbieterverbindung ausdrücklich
  mit einem gültigen Ersatzcode prüfen. Erst der Aktionsbutton darf bestätigen.

- **Vorgangsseiten bei Live-Updates: offen.** In DE/EN auf Desktop und Smartphone
  prüfen, dass Live-Updates die aktuelle Seite erhalten oder bei einer leeren
  letzten Seite auf die letzte verfügbare Seite zurückgehen.

- **Ticketnavigation ohne ungelesene Vorgänge: offen.** In DE/EN auf Desktop
  und Smartphone den Wechsel vom Zustand ohne ungelesene Vorgänge zur ersten
  relevanten Nachricht und wieder zurück prüfen. Punkt und Tooltip müssen ohne
  Öffnen des Ticketsystems erscheinen und beim Wegfall der letzten relevanten
  ungelesenen Nachricht verschwinden.

- **Vorgangsfilter – verbleibende Bedienfälle: offen.** In DE/EN auf Desktop und
  Smartphone Filter bei Live-Updates erhalten und Antwortentwürfe beim Ausblenden
  des ausgewählten Vorgangs erhalten. Den Außenklick auch auf schmalen Displays
  mit einer sichtbaren Fläche neben dem Dialog prüfen. Ticketlinks trotz
  entgegenstehender Filter öffnen.
  Benachrichtigungslinks müssen beispielsweise `section=support&ticket=10003`
  ohne kodierte Raute öffnen.

- **OTP-Autofill: offen.** Beide sechsstelligen Felder (Einrichtung und Anmeldung)
  in DE/EN durch echtes Browser- oder Passwortmanager-Autofill vervollständigen:
  Ein vollständiger Code löst genau eine Prüfung aus, ein Teilcode keine.
  Nach Ablehnung muss die erneute Eingabe möglich sein und der Bestätigungsbutton
  bedienbar bleiben.

- **Ticketzähler, Listenabschluss und Anwesenheitsauswahl: wartend auf Browserfreigabe.**
  In DE/EN auf Desktop und Smartphone „1 Anfrage“/„mehrere Anfragen“ von den
  Nachrichtenzählern unterscheiden. Die Adminliste soll unter einem Eintrag bündig
  abschließen und bei vielen Einträgen im begrenzten Scroller bedienbar bleiben.
  Den rechten Anwesenheitspfeil mit Maus und Tastatur auf-/zuklappen, den Erhalt des
  Zustands beim Sprachwechsel und die unveränderte Statuswahl prüfen.
  Die Ticket-Tabs außerdem mit der Anbieterleiste vergleichen: gemeinsame Höhe,
  Schrift, aktive Oberkante und unterbrochene untere Trennlinie, auch bei schmaler Breite.
  Der große Ticketsystem-Seitenkopf muss beim Bereichswechsel und Sprachwechsel
  im gemeinsamen Seitenrahmen erhalten bleiben; Panel- und Seitenbeschreibung sind unterschiedlich.
  Die Admin-Ticketkarten mit langem Betreff/Bearbeiternamen prüfen: Metadaten oben,
  Betreff auf voller Breite, Bearbeiter und Zeit in der Fußzeile; Status- und
  Ungelesen-Badges bleiben sichtbar. Die Nutzerkarten behalten ihre bisherige Anordnung.
  Den quadratischen Aktualisieren-Iconbutton samt übersetztem Tooltip prüfen.
  Den Bearbeiteravatar mit Name/E-Mail per Hover und Tastatur prüfen; grünes
  „Sie“/„You“-Badge mit Rahmen
  darf nur beim eigenen Bearbeiterkonto erscheinen. Unzugewiesene Tickets zeigen ihren Text kursiv.
  Den Antragsteller im Admin-Ticketkopf mit eigenem Avatar, Name und sichtbarer
  E-Mail sowie im Verlauf mit Avatar/Name prüfen; lange Werte dürfen auf Smartphone
  umbrechen. Name und E-Mail im Avatar-Tooltip müssen zweizeilig bleiben und bei
  Ticketwechsel/Live-Antwort dem richtigen Antragsteller folgen.
  Im Detailkopf die zwei Rollenspalten mit blauen Großbuchstaben prüfen: Name/E-Mail
  neben 32-px-Avatar beim Antragsteller und fremden Bearbeiter, beim eigenen Bearbeiter
  nur Avatar und grünes „Sie“-/„You“-Badge, sonst „Nicht Zugewiesen“. Der gesamte
  Bearbeiterblock ist rechtsbündig; beide Rollenüberschriften haben keinen Doppelpunkt.

- **Globale Scrollleisten – verbleibende Bedienfälle: offen.** Dokument,
  Wiederherstellungsverlauf und Admin-Filterdialog mit ausreichend Inhalt in einem
  touchfähigen Browserkontext prüfen. Scrollen per Touch muss erhalten bleiben;
  die systemabhängige Einblendung darf keine Inhalte verdecken. Zusätzlich reale
  Auswahlmenüs und deren Scrollflächen im Systemkontrast prüfen.

- **2FA-Wiederherstellungsanfragen: offen.** Beim Aktualisieren nach einer
  konkurrierenden Änderung den neuen Status und die passende Rückmeldung prüfen.
  In getrennten Sitzungen zusätzlich Live-Aktualisierung ohne Klick, Erhalt eines
  Entwurfs, die Zähler für andere Tickets und das Löschen des Zählers beim
  Öffnen auch in einem zweiten Tab desselben Kontos prüfen. Beim Schließen in
  einer anderen Sitzung darf der alte Antwortentwurf nicht im Formular für eine
  neue Anfrage erscheinen. Bei parallel eintreffenden Nachrichten oder
  Nutzerabschlüssen darf eine ältere Lesequittierung spätere Hinweise nicht
  quittieren. Zusätzlich zu den geprüften Beobachtungs- und
  Abschlussregeln das eingeschaltete Abo eines normalen Bearbeiters bei neuen
  Nachrichten prüfen. Interne Vermerke dürfen Nutzeransicht und Nutzerzähler nicht ändern.
  MFA-07 deckt bereits Live-Antworten in beide Richtungen, den Erhalt des
  Admin-Entwurfs, die Sichtbarkeit interner Vermerke, die Admin-Tabs und den
  Sicherheitshinweis auf Desktop und Smartphone ab.
  Die Bearbeiteravatare in Adminliste und Detail mit „Sie“/„You“ nur beim eigenen
  Konto prüfen. Namens-/E-Mail-Tooltips müssen in beiden Ticketansichten zweizeilig sein.
  Die Nutzeransicht darf keine Bearbeiternamen oder -adressen erhalten. Im Benachrichtigungsdialog
  entfällt bei Ticketmeldungen die Kontosicherheitsaktion, bei Kontoänderungen bleibt
  sie verfügbar. JUnit deckt die Text- und Datenverträge ab, der Browsernachtest ist offen.

- **Ticketzugang aus Benachrichtigungen: offen.** In DE/EN auf Desktop und
  Smartphone die kontextabhängige Zielaktion für Ticket- und Kontoänderungsmeldungen
  prüfen. Die Zielaktion belegt mobil eine volle Zeile. „Zum Ticket“ wählt genau den
  benachrichtigten Vorgang aus und verändert weder Lesestatus noch Zuständigkeit.
  Normale Nutzer erhalten den administrativen Ticketzugang nicht.

## JUnit-Tests

Aktuell keine offenen Ergänzungen.

Menschliche Abnahmen bleiben im
[manuellen Katalog](manual-tests/README.md).
