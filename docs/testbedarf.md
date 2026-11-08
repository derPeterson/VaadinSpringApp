# Offener Testbedarf

## Browser-Tests

- **OTP-Autofill: wartend auf eine eigene Geräteumgebung mit echtem Autofill.**
  Beide sechsstelligen Felder (Einrichtung und Anmeldung)
  in DE/EN durch echtes Browser- oder Passwortmanager-Autofill vervollständigen:
  Ein vollständiger Code löst genau eine Prüfung aus, ein Teilcode keine.
  Nach Ablehnung muss die erneute Eingabe möglich sein und der Bestätigungsbutton
  bedienbar bleiben. Die Geräteprüfung steht im [manuellen Katalog](manual-tests/README.md#hum-09).

<a id="avatar-tooltip"></a>

- **Ticketdarstellung – lange Werte und Avatar-Tooltips: offen.** Bei der isolierten
  Desktopprüfung mit einer langen Testadresse brach die E-Mail im Bearbeiter-Tooltip
  der Ticketliste auf mehrere Zeilen um. Erwartet werden insgesamt zwei Zeilen für
  Name und E-Mail. Nach der Korrektur in Liste und Detail in DE/EN bei 1280 und 390 px
  prüfen. Den Antragsteller-Tooltip nach Ticketwechsel und Live-Antwort prüfen.
  Lange Betreffe und Bearbeiternamen dürfen Status und Ungelesen-Badge nicht
  verdrängen. Auf Smartphone müssen lange Werte im Detailkopf umbrechen können.
  Die Nutzerkarten behalten ihre bisherige Anordnung. Bei der Detailabnahme
  auch die Akzentfarbe der Rollenüberschriften und den grünen Rahmen des
  „Sie“-/„You“-Badges prüfen.

- **Globale Scrollleisten – plattformabhängige Darstellung: wartend auf eine
  eigene Geräteumgebung.** Prüfen, dass
  systemseitig eingeblendete Scrollleisten auf dem Gerät keine Inhalte verdecken.
  Lange Auswahlmenüs mit eigener Scrollfläche im tatsächlichen Systemkontrast prüfen.
  Die Geräteprüfung steht im [manuellen Katalog](manual-tests/README.md#hum-10).

- **2FA-Wiederherstellungsanfragen – Nutzerabschluss und Ungelesen-Zähler: offen.**
  Bei einem parallel eintreffenden Nutzerabschluss darf eine ältere
  Lesequittierung den Abschlusshinweis nicht quittieren. Der Ungelesen-Zähler
  des Antragstellers muss bei einem internen Vermerk auch dann unverändert
  bleiben, wenn der zugehörige Verlauf gerade nicht sichtbar ist.

## JUnit-Tests

Aktuell keine offenen Ergänzungen.

Menschliche Abnahmen bleiben im
[manuellen Katalog](manual-tests/README.md).
