# Offener Testbedarf

## Browser-Tests

- **Globale Scrollleisten: offen.** Dokument, Wiederherstellungsverlauf und
  Admin-Filterdialog mit ausreichend Inhalt prüfen. Schmale Scrollleisten und
  transparente Bahn sollen sichtbar bleiben und sich per Maus, Touch und
  Tastatur bedienen lassen.

- **2FA-Adminnavigation und Kopftexte: offen.** Im Browser in DE/EN den vollständigen
  übersetzten Namen im Tooltip und die unterschiedlichen Seiten-/Panelhinweise prüfen.
  Die Sichtbarkeit nach Rolle und die Navigation über das gekürzte Label „2FA“
  sind bereits abgedeckt.

- **2FA-Wiederherstellungsanfragen: offen.** In DE/EN mit getrennten Konten
  prüfen, dass Nutzer ihre Anfrage schließen können, der Status in beiden
  Ansichten erscheint und die Administration sie abschließen kann. In beiden
  geschlossenen Zuständen muss der Verlauf lesbar bleiben und die Antwortaktion
  fehlen. Beim Aktualisieren nach einer konkurrierenden Änderung den neuen
  Status und die passende Rückmeldung prüfen.
  In getrennten Sitzungen zusätzlich Live-Aktualisierung ohne Klick, Erhalt eines
  Entwurfs, die Zähler für andere Tickets und das Löschen des Zählers beim
  Öffnen auch in einem zweiten Tab desselben Kontos prüfen. Beim Schließen in
  einer anderen Sitzung darf der alte Antwortentwurf nicht im Formular für eine
  neue Anfrage erscheinen. Abos für zugewiesene Admins und beobachtende Superadmins in
  DE/EN prüfen. Interne Vermerke dürfen Nutzeransicht und Nutzerzähler nicht ändern.
  MFA-07 deckt bereits Live-Antworten in beide Richtungen, den Erhalt des
  Admin-Entwurfs, die Sichtbarkeit interner Vermerke, die Admin-Tabs und den
  Sicherheitshinweis auf Desktop und Smartphone ab.
  Nach der Bearbeitertext-Anpassung außerdem „Von Ihnen übernommen“ und den
  Namen eines anderen Bearbeiters in der Adminliste und im Detail prüfen.
  Die Nutzeransicht darf keine Bearbeiternamen erhalten. Im Benachrichtigungsdialog
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
