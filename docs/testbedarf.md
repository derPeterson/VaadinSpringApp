# Offener Testbedarf

Hier stehen ausschließlich offene Aufgaben zur Ergänzung oder Anpassung
automatisierter Tests. Pflege und Abnahme: [Testablauf](tests.md#übergabe-offener-testaufgaben).

## Browser-Tests

| Funktion | Szenario und Erwartung | Zustand |
| --- | --- | --- |
| Multi-Modul-Umbau | Bestehende vollständige Browser-Suite DE/EN nach Pfad-/Buildumbau am selben aktuellen Image ausführen; Ressourcen, HTTPS, Neustarts und isolierter Workerabbau funktionieren unverändert. Passende bestehende Fälle wiederverwenden. | Docker-/Runnerpfade und gezielte Helfer-/Start-/Coverageprüfungen bestanden; wartend auf abschließenden Buildstand und ausdrückliche Browserfreigabe gemäß [aktivem Plan](planung/multi-modul-sonarqube.md). |

## JUnit-Tests

Derzeit keine offenen Ergänzungen. Die vollständige gemeinsame Funktionsprüfung
und der echte Sonar-Servernachweis bleiben im [aktiven Plan](planung/multi-modul-sonarqube.md)
offen; vorhandene Prüfungen dafür wiederverwenden.

Menschliche Abnahmen und die nicht beauftragte optionale externe SMTP-Abnahme
bleiben im [manuellen Katalog](manual-tests/README.md).
