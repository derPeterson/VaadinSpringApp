# Offener Testbedarf

## Browser-Tests

- **2FA-Adminnavigation und Kopftexte: offen.** Nach der Textkürzung den vorhandenen
  CONFIG-05A-/MFA-03-Ablauf gezielt in DE/EN nachprüfen: „2FA“ nur für Superadmins
  sichtbar, vollständiger übersetzter Name im Tooltip und unterschiedliche
  Seiten-/Panelhinweise, einschließlich der angepassten HTML-Referenz.
  Die JUnit-Navigation und Sprachkataloge sind geprüft; ein neuer Browserlauf für
  diese nachträgliche UI-Anpassung ist noch nicht beauftragt.

- **Wiederkehrende 2FA-Anmeldung und Hilfe: wartend auf Entwurfsentscheidung.**
  Nach Freigabe und Übernahme der im [2FA-Plan](planung/zwei-faktor-bestaetigung.md)
  beschriebenen Gestaltung die vorhandenen Anmelde-, Ersatzcodewechsel- und
  Hilfe-Journeys anpassen und auf Desktop/Smartphone in DE/EN prüfen.
  Erwartung: Umschaltung zwischen sechsstelliger OTP-Eingabe und langem Ersatzcode
  ohne Abschneiden oder Verwechslung der Zwecke; Rückwege und Hilfeaktionen bleiben
  erreichbar, ein Faktorwechsel gibt erst nach Bestätigung der neuen App Zugriff.
  Auch die bislang deutschen Bestätigungs-/Hilfemomentaufnahmen der HTML-Referenz
  sprachlich angleichen. Keine Umsetzung oder neue Testkopien vor der Designfreigabe.

## JUnit-Tests

Aktuell keine offenen Ergänzungen.

Menschliche Abnahmen bleiben im
[manuellen Katalog](manual-tests/README.md).
