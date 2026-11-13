# Offener Testbedarf

## Browser-Tests

- **Benutzereditor / Anmeldungsbereich — offen:** Den zusätzlichen nativen
  Details-Außenabstand korrigieren. Die vorhandene Geometrieprüfung `OIDC-14`
  unverändert erhalten und in DE/EN bei 1280 und 390 px erfolgreich nachprüfen.
  Abdeckung: Suite `sign-in-session-layout`, anschließend Gesamt-QA am finalen Image.
- **Auswahllisten / Tagesfelder — offen:** Den Fokuswechsel-Locator im bestehenden
  Fall `choiceHoverPaginationAndDaySuffixUseTheSharedFieldStyle` auf die
  Konfigurationskartenüberschrift begrenzen. Nach den einheitlichen Seitentiteln
  trifft der unbeschränkte Überschriften-Locator auf H1 und H2. Alle Feld-,
  Hover- und Rahmenassertionen erhalten; in DE/EN auf Desktop und Smartphone nachprüfen.
- **Konfigurations-Feldhilfen — offen:** `CONFIG-05D` muss nach der Queue-Tabtrennung
  ausschließlich die sichtbaren Infoelemente der aktuellen Gruppe zählen und öffnen.
  Vaadin behält im ausgeblendeten bisherigen Feldcontainer noch DOM-Elemente.
  Kartenanzahl und vollständige Abdeckung aller 33 Adminfelder unverändert prüfen,
  jeweils DE/EN bei Desktop- und Smartphonebreite.
- **Gesamtlauf / Worker-Verteilung — offen:** Die gewachsenen 2FA- und
  Konfigurationsklassen anhand der gemessenen Laufzeiten neu gewichten. Der erste
  Gesamtlauf überschritt bei einer Gruppe das bestehende Gesamtlimit und brach
  dadurch drei Mailfälle ab. Vollständige DE-/EN-Läufe müssen die geänderte
  Verteilung bestätigen; einzelne Testtimeouts und Assertions bleiben unverändert.

## JUnit-Tests

Keine offenen gezielten Ergänzungen für den aktuellen Umfang.

Die Ergebnisse der letzten großen Testrunde und der anschließenden gezielten
Nachprüfungen einschließlich ihrer Grenzen stehen im [Prüfstand](status.md).
Menschliche Abnahmen bleiben im [manuellen Katalog](manual-tests/README.md);
die gemeinsame Sichtung neuer Komponenten steht im
[Komponentenplan](planung/vaadin-komponenten-abgleich.md).
