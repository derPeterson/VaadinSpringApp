# Meilensteine

Hier stehen dauerhafte, eigenständig lesbare Zusammenfassungen wichtiger Projektstände.
Sie bleiben auch dann verständlich, wenn lokale Testausgaben bereits bereinigt wurden.

| Datum | Meilenstein |
| --- | --- |
| 17.10.2026 | [Sitzungsverwaltung und Kontosicherheit vollständig geprüft](2026-10-17-sitzungsverwaltung-und-kontosicherheit.md) |
| 17.10.2026 | [Kontoprofil, E-Mail-Dialog und Neustartprüfungen](2026-10-16-kontoprofil-und-browserpruefungen.md) |
| 16.10.2026 | [Wartungsmodus umgesetzt und vollständige Gesamtrunde abgeschlossen](2026-10-16-wartungsmodus-und-gesamttests.md) |
| 15.10.2026 | [Redesign abgeschlossen und vollständig nachgeprüft](2026-10-15-redesign-abgeschlossen.md) |
| 15.10.2026 | [Theme-Struktur vollständig geprüft](2026-10-15-theme-struktur-geprueft.md) |

## Was festgehalten wird

Nur besondere Abschlüsse und wichtige Erkenntnisse dokumentieren, nicht jeden Testlauf:
Datum und geprüfter Ausgangsstand, wesentliche Änderungen, tatsächlich ausgeführte
Prüfungen mit Ergebnissen, gemessene Coverage samt Messgrenzen und verbleibende Risiken.
Fehlgeschlagene Zwischenversuche nur erwähnen, wenn ihre Ursache künftig hilfreich ist.
Keine Zugangsdaten oder personenbezogenen Testdaten übernehmen.

Die Zusammenfassung enthält die entscheidenden Zahlen und Erkenntnisse selbst.
Keine Links oder konkreten Pfade zu lokalen Laufordnern, Screenshots, Traces,
HTML-Berichten oder Coverage-Rohdaten als dauerhafte Nachweise eintragen.
Der [aktuelle Prüfstand](../../browser-tests/abdeckung.md) bleibt getrennt von
abgeschlossenen Meilensteinen; der [Designstandard](../design-standard.md)
beschreibt die verbindliche Gestaltung.

## Testausgaben aufbewahren und bereinigen

Die ignorierten Ordner unter `browser-tests/runs/` sind lokale Diagnoseausgaben.
Screenshots, Traces, Allure-Daten und bei Coverage zusätzlich JAR/Klassendateien
können viel Speicher belegen. Sie müssen nicht dauerhaft aufbewahrt werden.
Nach Auswertung können nicht mehr benötigte vollständige Laufordner entfernt werden;
bei einem wichtigen Meilenstein vorher die Ergebnisse hier zusammenfassen.
Eine Sicherung aller alten Runs an anderer Stelle ist nicht erforderlich.

Es gibt keine vorgeschriebene Mindestaufbewahrung oder automatische Löschung.
Nur für laufende Fehleranalysen gezielt benötigte Belege vorübergehend behalten.
Die Links innerhalb eines einzelnen Run-Berichts gelten nur, solange dessen
Laufordner vorhanden ist. Das Bereinigen solcher Ordner ändert keinen Meilensteinbericht.
