# Offene Fehler

## Zusätzlicher Abstand im Anmeldungsbereich des Benutzereditors

Nach Aufklappen von „Anmeldung“ bleibt durch den nativen Außenabstand von
`vaadin-details` zusätzlicher Leerraum innerhalb der Infobox. Der vereinbarte
kompakte Rahmen soll am letzten Inhaltsbereich enden; Titel und Icon bleiben
zentriert. Die bestehende Geometrieprüfung `OIDC-14` schlägt bei 1280 und 390 px fehl.
Lokale Details-Regel mit den übrigen kompakten Bereichen abgleichen und danach
`sign-in-session-layout` beziehungsweise `all` in DE/EN auf beiden Breiten prüfen.

Die bewussten Grenzen der Google-Ersteinrichtung stehen in der
[Sicherheitsdokumentation](docs/externe-anmeldung.md#google-bestätigung-und-geschützte-ersteinrichtung).
Offene automatisierte Ergänzungen stehen im [Testbedarf](docs/testbedarf.md).
