# Google-/Microsoft-Anmeldung und freiwillige Profilangaben

## Stand und freigegebene Integration

Quellbranch: `feature/google-microsoft-anmeldung`. Die technische Integration nach
`main` ist ausdrücklich beauftragt: Review, Commit, lokaler Merge, vollständige
Sonar-Bereinigung und anschließend große Testrunde mit eigenen isolierten Daten.
Die Umsetzung bleibt bis zur noch offenen Stitch-Entscheidung als Feature offen.
Der Merge gilt hier ausdrücklich nicht als endgültiger Featureabschluss. Kein Push.

Die Kontosicherheit mit den Tabs „Portalzugang“ und „Anmeldeanbieter“ sowie die
Google-Zwischenlösung sind händisch abgenommen. Der Benutzer konnte nach dem
Google-Durchlauf sein Portalpasswort ändern. Die Zwischenlösung ist zusätzlich mit
Testanbietern auf Desktop und Smartphone in DE/EN geprüft: fehlender/alter
Google-Zeitpunkt, strenge Microsoft-Prüfung sowie Ablehnung von Kontowechsel,
Abbruch und ungültigen Antworten. Aktuelles Verhalten und Grenzen stehen in
[Externe Anmeldung](../externe-anmeldung.md), Gestaltungsbausteine in
[Theme](../theme.md). Den letzten Gesamtprüfstand beschreibt [Status](../status.md).
Gezielte Nachtests ersetzen diese große QA nicht.

## Noch offene Entscheidung

Die [gespeicherte Stitch-Kritik zu Anmeldeanbietern und ODER-Trenner](../design/stitch-kritik-anmeldeanbieter-anmeldung.md)
mit dem Benutzer besprechen. Er trifft die Designentscheidung später, bevor die
nächste Umsetzung beginnt. Bis dahin keine weitere Gestaltung aus der Kritik
übernehmen. Diesen Plan und seinen TODO-Verweis auch nach Integration und QA erhalten.
Nach der Entscheidung den beschlossenen Umfang umsetzen beziehungsweise bewusst
verwerfen und erst nach abschließender Abnahme den Featureplan entfernen.

## Beauftragte technische Abschlussprüfung

Sonar und große QA auf dem integrierten `main` noch durchführen. Dabei den offenen
2FA-Testbedarf ausdrücklich als wartende Folgeumsetzung erhalten. Nach belegtem
Abschluss diesen Prüfauftrag aus dem Plan und TODO entfernen; die Stitch-Entscheidung
bleibt davon unabhängig offen. Echte Anbieter-/Geräte-Sonderfälle stehen weiterhin
im [manuellen Katalog](../manual-tests/README.md#hum-08).

## Separate Folgeumsetzungen

Die [2FA-Bestätigung](zwei-faktor-bestaetigung.md) ist als nächste separate Umsetzung
vereinbart. Die akzeptierte Google-Sitzungsschwäche bleibt bis zur passenden Ablösung
in [BUGS.md](../../BUGS.md) erhalten. Sie gehört nicht zur jetzigen Sonar-Bereinigung.
Der [Vaadin-Komponentenabgleich](vaadin-komponenten-abgleich.md) bleibt ebenfalls eine
separate spätere Umsetzung. Keine neuen Bausteinumbauten aus der Integration ableiten.
