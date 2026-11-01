# Offene Fehler

- **Bekannte Sicherheitsschwäche: Google bestätigt keine erneute aktive Anmeldung.**
  Als ausdrücklich freigegebene Zwischenlösung reicht ein neuer OIDC-Durchlauf
  mit dem bereits verbundenen Google-Konto. Eine bestehende Google-Sitzung kann
  ihn ohne Passwort oder zweiten Faktor abschließen. Wer Zugriff auf die offene
  Portal- und Google-Sitzung hat, kann deshalb sensible Kontoaktionen bestätigen.
  Konto-/Sitzungs-/Zweckbindung, Tokenprüfungen und einmalige Fünf-Minuten-Freigabe
  bleiben erhalten. Nächste separate Umsetzung: eingerichtete 2FA vorrangig und
  verbindlich verwenden, einschließlich geschützter Einrichtung und Wiederherstellung.
  [Risiko und Übergangsvertrag](docs/externe-anmeldung.md#google-zwischenlösung-und-bekannte-sicherheitsschwäche),
  [Lösungsplan](docs/planung/zwei-faktor-bestaetigung.md).
