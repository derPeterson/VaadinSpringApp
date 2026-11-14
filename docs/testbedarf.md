# Offener Testbedarf

## Browser-Tests

- **Übrige native Formularlayouts — offen:** Allgemeine Konfiguration, 2FA-Regeln,
  Anmeldeanbieter und verbleibende Formularraster inklusive Superadmin-Zusatzfelder
  isoliert vor/nach der Umstellung in DE/EN bei 1280 und 390 px vergleichen.
  Maße, Umbrüche, Feldhilfe-Ausrichtung, Scrollbereiche, Schaltzustände, Speichern
  und Tastaturbedienung erhalten; vorhandene Fachfälle wiederverwenden.
  Für den aktuellen Nachtauftrag freigegeben, Umsetzung und Prüfung laufen.

- **Mailqueue / Reiteranschluss — offen:** Nach der lokalen Darstellungskorrektur
  müssen Header und Tab-Leiste in DE/EN auf Desktop und Smartphone direkt
  ohne Abstand oder zusätzliche Trennlinie aneinander anschließen.
  Die Prüfung ist im vorhandenen Queue-Reitertest
  ergänzt; Browsernachtest für diese Korrektur noch nicht ausgeführt.
  Der Einstellungszähler muss bei „Allgemein“ fünf und bei „Assistenten“ vier
  Einstellungen anzeigen, auch nach Tab- und Sprachwechsel.
- **Mailqueue / Statusausgabe — offen:** Den festen Zeilenumbruch zwischen
  letztem Lauf und Anzahl bereinigter Aufträge in DE/EN auf Desktop- und
  Smartphonebreite prüfen. Die Anzahl muss auch bei ausreichender Breite
  unter dem Laufzeitpunkt beginnen; schmale Ansichten dürfen weiter natürlich
  umbrechen. Vom Benutzer auf Desktop und Smartphone manuell abgenommen;
  automatisierter DE-/EN-Nachtest noch nicht freigegeben oder ausgeführt.

## JUnit-Tests

Keine offenen gezielten Ergänzungen für den aktuellen Umfang.

Ergebnisse und Grenzen stehen im [aktuellen Prüfstand](status.md).
Menschliche Sonderprüfungen bleiben im [manuellen Katalog](manual-tests/README.md).
Die bisherigen UI-Pakete und die neue Queue-Karte sind vom Benutzer abgenommen;
zurückgestellte Folgepakete stehen im
[Komponentenplan](planung/vaadin-komponenten-abgleich.md).
