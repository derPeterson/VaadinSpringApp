# Offener Testbedarf

Hier stehen ausschließlich offene Aufgaben zur Ergänzung oder Anpassung
automatisierter Tests. Pflege und Abnahme: [Testablauf](tests.md#übergabe-offener-testaufgaben).

## Browser-Tests

| Funktion | Szenario und Erwartung | Zustand |
| --- | --- | --- |
| Multi-Modul-Umbau | Bestehende vollständige Browser-Suite DE/EN nach Pfad-/Buildumbau am selben aktuellen Image ausführen; Ressourcen, HTTPS, Neustarts und isolierter Workerabbau funktionieren unverändert. Passende bestehende Fälle wiederverwenden. | Docker-/Runnerpfade und gezielte Helfer-/Start-/Coverageprüfungen bestanden; wartend auf abschließenden Buildstand und ausdrückliche Browserfreigabe gemäß [aktivem Plan](planung/multi-modul-sonarqube.md). |

## JUnit-Tests

| Funktion | Szenario und Erwartung | Zustand |
| --- | --- | --- |
| Gemeinsamer Sonar-Helfer | Gepflegte Offline-Regressionen auf Reactor-Aufrufe, Modulfehler, fehlende Berichte und vollständige Quellen-/Revisionszuordnung erweitern. Fehler verhindern einen als vollständig erfolgreichen Upload; Sicherheits- und Serverabschlussfälle erhalten. | Wartend auf Umsetzung; technische Python-Helferprüfung entsprechend der Testanleitung. |
| Modulübergreifende Coverage | Vorhandene App-/Helfertests für Shared-Regeln nutzen; JaCoCo ordnet Ausführung den richtigen App-/Shared-Quellen zu. Fehlender Report wird von gemessener Null unterschieden, Testcode nicht als Anwendungscode gezählt. | Wartend auf Coverage-Umstellung; vorhandene Tests/Reportprüfungen passend erweitern. |

Menschliche Abnahmen und die nicht beauftragte optionale externe SMTP-Abnahme
bleiben im [manuellen Katalog](manual-tests/README.md).
