# Google-/Microsoft-Anmeldung: offene Designentscheidung

Die technische Integration ist lokal auf `main` übernommen und vollständig geprüft.
Der [aktuelle Prüfstand](../status.md) beschreibt Review, Sonar und große Testrunde.
Die Kontosicherheit und die Google-Zwischenlösung sind händisch abgenommen.
Der Featureabschluss bleibt auf ausdrücklichen Benutzerwunsch trotzdem offen.

## Noch zu entscheiden

Die [gespeicherte Stitch-Kritik zu Anmeldeanbietern und ODER-Trenner](../design/stitch-kritik-anmeldeanbieter-anmeldung.md)
mit dem Benutzer besprechen. Er trifft die Designentscheidung später, bevor die
nächste Umsetzung beginnt. Bis dahin keine weiteren Vorschläge aus der Kritik
übernehmen. Den beschlossenen Umfang anschließend umsetzen oder bewusst verwerfen
und erst nach abschließender Abnahme diesen Plan samt TODO-Verweis entfernen.

Der Quellbranch `feature/google-microsoft-anmeldung` bleibt erhalten. Neue Änderungen
müssen auf dem dann aktuellen Main-Stand aufbauen; die Integration allein schließt
hier ausdrücklich nicht das Feature ab. Kein Push ohne eigenen Auftrag.

## Separate Folgeumsetzungen

Die [2FA-Bestätigung](zwei-faktor-bestaetigung.md) ist als nächste separate Umsetzung
vereinbart. Die akzeptierte Google-Sitzungsschwäche bleibt bis zur passenden Ablösung
in [BUGS](../../BUGS.md), der zugehörige automatisierte Bedarf im
[Testbedarf](../testbedarf.md) erhalten. Aktuelle Verträge stehen in
[Externe Anmeldung](../externe-anmeldung.md), echte Sonderfälle im
[manuellen Katalog](../manual-tests/README.md#hum-08).
Der [Vaadin-Komponentenabgleich](vaadin-komponenten-abgleich.md) bleibt ebenfalls
separat; daraus jetzt keine weiteren Bausteinumbauten ableiten.
