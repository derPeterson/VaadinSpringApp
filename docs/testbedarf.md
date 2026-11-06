# Offener Testbedarf

## Browser-Tests

- **Vorgangsfilter: offen, wartend auf Browserfreigabe.** In DE/EN auf Desktop und
  Smartphone den Filterbutton in der separaten Werkzeugleiste prüfen: native Status-
  Mehrfachwahl, Zuständigkeit, Anwenden, Entwurf zurücksetzen und Abbruch über Button,
  Escape oder Außenklick. Aktive Gruppen zählen, keine Treffer korrekt anzeigen,
  Filter beim Aktualisieren und Live-Update erhalten. Antwortentwürfe beim Ausblenden
  des ausgewählten Vorgangs erhalten und Ticketlinks trotz entgegenstehender Filter öffnen.
  Benachrichtigungslinks müssen beispielsweise `section=support&ticket=10003`
  ohne kodierte Raute öffnen; direkt eingegebene sichtbare Ticketnummern wählen
  denselben berechtigten Vorgang aus.

- **Automatische OTP-Bestätigung: offen, wartend auf Browserfreigabe.** Beide
  sechsstelligen Felder (Einrichtung und Anmeldung) in DE/EN durch Tippen,
  Einfügen und Autofill vervollständigen: Nach der sechsten Ziffer genau eine
  Prüfung, bei Teilcodes keine. Gruppierte und durch das Add-on gekürzte eingefügte
  Codes berücksichtigen. Nach Ablehnung muss erneute Eingabe möglich sein;
  Bestätigungsbutton bleibt bedienbar. JUnit prüft die serverseitige Ereignisbehandlung.

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

- **Globale Scrollleisten: offen.** Dokument, Wiederherstellungsverlauf und
  Admin-Filterdialog mit ausreichend Inhalt prüfen. Schmale Scrollleisten und
  transparente Bahn sollen sichtbar bleiben und sich per Maus, Touch und
  Tastatur bedienen lassen.

- **2FA-Adminnavigation und Kopftexte: offen.** Im Browser in DE/EN den vollständigen
  übersetzten Namen im Tooltip und die unterschiedlichen Seiten-/Panelhinweise prüfen.
  Die Sichtbarkeit nach Rolle und die Navigation über das gekürzte Label „2FA“
  sind bereits abgedeckt.

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

- **Öffentliche Sprachwahl: offen.** In DE/EN nach der Sprachauswahl die
  Browsersitzung ohne Löschung der Cookies neu öffnen. Die öffentliche Ansicht
  übernimmt die Auswahl, nach dem Anmelden hat die Kontosprache Vorrang.

## JUnit-Tests

Aktuell keine offenen Ergänzungen.

Menschliche Abnahmen bleiben im
[manuellen Katalog](manual-tests/README.md).
