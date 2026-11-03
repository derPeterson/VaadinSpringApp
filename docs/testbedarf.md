# Offener Testbedarf

## Browser-Tests

- **2FA-Adminnavigation und Kopftexte: offen.** Nach der Textkürzung den vorhandenen
  CONFIG-05A-/MFA-03-Ablauf gezielt in DE/EN nachprüfen: „2FA“ nur für Superadmins
  sichtbar, vollständiger übersetzter Name im Tooltip und unterschiedliche
  Seiten-/Panelhinweise, einschließlich der angepassten HTML-Referenz.
  Die JUnit-Navigation und Sprachkataloge sind geprüft; ein neuer Browserlauf für
  diese nachträgliche UI-Anpassung ist noch nicht beauftragt.

- **2FA-Bestätigung und Faktorwechsel: offen.** Die bestehenden vollständigen
  Anmelde- und Wiederherstellungsfälle nach der Umstellung auf getrennte Eingaben
  in DE/EN erneut ausführen. MFA-12 prüft dabei den Wechsel mit dem letzten
  Ersatzcode bis zur Freigabe nach Bestätigung der neuen App. Die fokussierte
  Suite MFA-18 deckt den Wechsel der Eingabemethode und die Ansichten ab.

## JUnit-Tests

Aktuell keine offenen Ergänzungen.

Menschliche Abnahmen bleiben im
[manuellen Katalog](manual-tests/README.md).
