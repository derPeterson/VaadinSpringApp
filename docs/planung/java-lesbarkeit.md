# Offene Sonar-Befunde zur Java-Lesbarkeit

Der lokale Nachscan auf `main` bei `6b156cc9` meldet sechs offene Findings.
Die bestandene große Testrunde steht im [Prüfstatus](../status.md).

| Regel | Betroffene Stelle | Zu klärender Vertrag |
| --- | --- | --- |
| S106 (zweimal) | `IdeHttpsProxy` | stdout überträgt das Startsignal, stderr die unveränderte Ausgabe des eigenen Caddy-Prozesses. |
| S1181 | `HttpsDevelopmentApplication` | Bei einem Startfehler wird der eigene Proxy abgebaut und der ursprüngliche Fehler weitergegeben. |
| S1948 | `AdminUserActivityFilter` | Die verwendete Systemuhr und die feste Testuhr sind nachweislich serialisierbar. |
| S4276 | `UserRoleBroadcaster` | Der bestehende Listenervertrag verwendet eine Benutzer-ID als `Long`. Eine Umstellung auf `long` ändert den Umgang mit `null`. |
| S4502 | `SecurityConfig` | Spring-CSRF bleibt für reguläre HTTP-Änderungen aktiv. Die Ausnahmen betreffen erkannte Flow-Transporte und die lokal begrenzte H2-Konsole mit eigenem Ursprungsschutz. |

Die fachlich begründeten Ausnahmen in Sonar abstimmen. Die automatische
Freigabe hat den Versuch abgelehnt, einen der Prozesskanäle als Fehlalarm
zu kennzeichnen. Danach den aktuellen Gate-Status verifizieren. Bei einer
Codekorrektur die betroffenen Tests und den vollständigen Sonar-Nachscan
auf dem neuen Stand ausführen.
