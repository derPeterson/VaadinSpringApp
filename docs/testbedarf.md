# Offener Testbedarf

## Browser-Tests

- **Tooltip der Ticketnavigation nach letzter Aktivität: offen.** In DE/EN auf
  Desktop und Smartphone zusätzlich zum Verschwinden des Punkts prüfen, dass
  „Neue Aktivitäten im Ticketsystem“ auch nach erneutem Hover nicht mehr erscheint.

- **Ticketlink bei aktivem Vorgangsfilter: offen.** In DE/EN auf Desktop und
  Smartphone einen Ticketlink aus der Glockenvorschau bei entgegenstehendem
  aktivem Filter öffnen.

- **OTP-Autofill: offen.** Beide sechsstelligen Felder (Einrichtung und Anmeldung)
  in DE/EN durch echtes Browser- oder Passwortmanager-Autofill vervollständigen:
  Ein vollständiger Code löst genau eine Prüfung aus, ein Teilcode keine.
  Nach Ablehnung muss die erneute Eingabe möglich sein und der Bestätigungsbutton
  bedienbar bleiben.

- **Ticketdarstellung – verbleibende Detailprüfungen: offen.**
  In DE/EN auf Desktop und Smartphone prüfen, dass die Adminliste unter einem
  Eintrag bündig abschließt und bei vielen Einträgen im begrenzten Scroller
  bedienbar bleibt.
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

- **Globale Scrollleisten – plattformabhängige Darstellung: offen.** Prüfen, dass
  systemseitig eingeblendete Scrollleisten auf dem Gerät keine Inhalte verdecken.
  Lange Auswahlmenüs mit eigener Scrollfläche im tatsächlichen Systemkontrast prüfen.

- **2FA-Wiederherstellungsanfragen – verbleibende Konkurrenzfälle: offen.** Die
  Zähler für andere Tickets bei Live-Aktualisierungen prüfen. Bei parallel
  eintreffenden Nachrichten oder
  Nutzerabschlüssen darf eine ältere Lesequittierung spätere Hinweise nicht
  quittieren. Interne Vermerke dürfen die Nutzerzähler nicht ändern.
  Die Bearbeiteravatare in Adminliste und Detail mit „Sie“/„You“ nur beim eigenen
  Konto prüfen. Namens-/E-Mail-Tooltips müssen in beiden Ticketansichten zweizeilig sein.
  Die Nutzeransicht darf keine Bearbeiternamen oder -adressen erhalten.

## JUnit-Tests

Aktuell keine offenen Ergänzungen.

Menschliche Abnahmen bleiben im
[manuellen Katalog](manual-tests/README.md).
