# P1-CSRF – Umsetzung und Prüfbericht

**Nachtrag:** Der unten beim Browserabschluss noch offene H2-Restpunkt ist
inzwischen [gezielt umgesetzt und über echtes HTTP geprüft](h2-console-csrf-2026-10-20.md).
Aktuell 1956 Anwendungs-JUnits und 48 Helfer erfolgreich, Produktionsbuild grün.
Die nachfolgenden 1912er-JUnit- und 44er-Browsernachweise dokumentieren den Stand
vor diesem H2-Nachtrag; ihre Imagekennung bezeichnet weiterhin genau diesen Stand.

Stand: 20.10.2026, Branch `fix/sonar-csrf`, Ausgangspunkt
`6e8aa610dda9d452b6f97959645fef964f9d4479`. Eigene Änderungen uncommittet;
keine Commits, Pushes oder Sonar-Statusänderungen.

Die Implementierung aktiviert Spring-CSRF für klassische HTTP-Mutationen und
behält den eigenen Schutz echter Flow-Transporte bei. Die freigegebene gezielte
Browserabnahme ist auf Deutsch und Englisch erfolgreich abgeschlossen. Die
Bedienintegration der optionalen H2-Konsole wurde anschließend im verlinkten
Nachtrag ergänzt. Der Befund wird
hiermit nicht als im Sonar-Server erledigt behauptet.

## Änderungen

| Datei / Bereich | Ergebnis und Grund |
| --- | --- |
| `security/SecurityConfig.java` | Globale Deaktivierung entfernt; CSRF-Matcher mit konfiguriertem Servlet-Mapping eingesetzt. CSRF-Fehler ergeben 403 statt Navigationsredirect. Spring-Standardrepository und maskierender Handler bleiben erhalten. |
| `security/FlowCsrfRequestMatcher.java` | Sichere HTTP-Methoden nach Spring-Standard. Bekannte Flow-Roottypen, echter Push und geprüfte dynamische Ressourcen werden anhand des installierten HandlerHelper erkannt. Klassische Endpunkte, unbekannte Rootmarker und Hilla erhalten keine pauschale Ausnahme. |
| `security/LoginCsrfTokenFilter.java` | GET `/login` liefert den maskierten Token im Header `X-CSRF-TOKEN`, mit `Cache-Control: no-store` und derselben Sitzung. Der Filter läuft nach Remember-me und dessen Rotation. Andere öffentliche GETs erzeugen hierfür keine Sitzung. |
| `security/SessionAdmissionService.java` | `CsrfAuthenticationStrategy` innerhalb der bestehenden Login-Completion nach Zulassung/Session-Fixation und vor Veröffentlichung ergänzt. HTTP-Passwort, manueller Flow-Login und Remember-me nutzen dieselbe Grenze. Gewöhnliches `complete()` und Rechte-Refresh rotieren nicht. Bestehende Sperren, Limits und Rollbacks bleiben erhalten. |
| `security/CsrfAdmissionTest.java` | Rotation, genau eine ID-Änderung, Slot-Wiederverwendung, unveränderter Token beim Context-Persistieren, Ablehnung vor Veröffentlichung und Rollback ohne Beeinträchtigung anderer Sitzungen. |
| `security/FlowCsrfRequestMatcherTest.java` | Methoden, tatsächliche Root-/Push-/Ressourcenpfade, unbekannte Pfade/Marker, Traversal, Context-Root und abweichendes Servlet-Mapping. |
| `service/CsrfSecurityChainTest.java` | Tatsächliche SecurityFilterChain mit isoliertem H2: fehlende/falsche/fremde Tokens, gefälschte Marker, Header/Body, Context-Root, HTTP-/Flow-Login, Rotation/Refresh, Remember-me und GET-/POST-Logout samt eigener Serien-/Cookiebereinigung. |
| Bestehende `SecurityChainRegressionTest` | HTTP-Helfer beziehen echte Tokens. Beim Rollenentzug geschieht der Tokenbezug vor dem Entzug; erst der untersuchte POST muss neue Rechte erkennen. Die Wartungsprüfung erlaubt die notwendige anonyme CSRF-Sitzung, verbietet aber authentifizierten Context und Slot. |
| `views/MaintenanceViewTest.java` | Im Dockerbuild aufgefallene Testfixture stabilisiert: VaadinSession wird als Feld stark gehalten, weil CurrentInstance nur schwache Referenzen speichert. Andernfalls kann die dynamische Übersetzung auf Englisch zurückfallen. Fünf gezielte Nachtests mit englischer System-Locale erfolgreich; keine Produktivänderung. |
| Bestehende `PushRpcSecurityPersistenceTest` | Echte Root-UIDL- und Push-Anfragen durch HTTP-Filter bis zum realen RPC-Handler; ungültiger Flow-Token ohne Datenmutation. Dynamische Upload-Anfrage bis zum realen StreamRequestHandler mit registrierter Ressource; unbekannter Schlüssel, verstecktes/deaktiviertes Element werden abgewiesen. |

Die Framework-API wurde gegen die lokal aufgelösten Sources von Flow/vaadin-spring
25.3.3 geprüft (Plattform 25.3.2), Spring Security 7.1.1. Beim Root-Mapping
vermittelt SpringServlet über seine ForwardingRequestWrapper; die Upload-Prüfung
verwendet diesen tatsächlichen Wrapper. Der Matcher verwendet die Pfadaufbereitung
des HandlerHelper, nicht eine zweite URI-Auslegung mit abweichendem führendem Slash.

## HTTP-Vertrag und Einführung

Klassische Clients beziehen zuerst GET `/login`, behalten das Sessioncookie und
senden für POST `/login` den Response-Token als `X-CSRF-TOKEN` oder `_csrf`-Parameter.
Nach erfolgreicher Anmeldung ist der alte Token ungültig; vor der nächsten
klassischen Mutation gegebenenfalls einen frischen Token beziehen. Ein
`v-r=uidl` auf `/login`, `/logout` oder `/h2/login.do` ersetzt keinen Spring-Token.
Keine Tokens in URLs oder Logs; keine CORS-Freigabe hinzugefügt.

GET `/logout` meldet nicht mehr ab. POST `/logout` braucht einen gültigen Token;
fehlende/falsche Tokens lassen Anmeldung und eigene gespeicherte Serie bestehen.
Der Flow-Abmeldebutton bleibt ein geschützter Flow-Callback mit seinem bisherigen
Sitzungs-/Remember-me-Cleanup.

Die normale Flow-Oberfläche erhält keinen neuen Formularablauf. Die Javaänderung
wird erst nach Neubau und Neustart der vorgesehenen Anwendung wirksam. Für die
freigegebene Browserabnahme wurde ein eigenes Produktionsimage gebaut; nur isolierte
Testcontainer mit frischen Testdaten und Mailpit wurden gestartet und anschließend
abgebaut. Bestehende Anwendungsdaten und fremde Dienste wurden nicht verändert.
Ein Browser-Refresh allein übernimmt den neuen Servercode nicht.

## Nachweise

- Java 25.0.4.1 und Maven Wrapper 3.9.16 verifiziert.
- Spotless apply/check und Checkstyle nach den Javaänderungen erfolgreich.
- Gezielter Lauf: **121 Tests, 0 Failures, 0 Errors, 0 Skips**.
- Darunter **51 zusätzliche Testausprägungen** gegenüber dem Ausgangsstand.
- Vollständiger Maven-Testlauf mit `-Pproduction verify`: **1912 Tests, 0 Failures,
  0 Errors, 0 Skips**, Produktions-JAR erfolgreich gebaut. Der finale Nachlauf
  nach Präzisierung von Context-Root und tatsächlichem H2-Pfad war ebenfalls grün;
  seine Coverage wurde in einer eigenen Datei frisch aufgezeichnet (append=false).
- Unabhängige Browser-Helfer: **48 Tests, 0 Failures, 0 Errors, 0 Skips**.
- Docker-Produktionsbuild einschließlich vollständiger Maven-Prüfung nach der
  Fixturekorrektur erfolgreich. Keine Tests übersprungen oder ausgeschaltet.
- Browser nach ausdrücklicher Freigabe: **DE 44/44, EN 44/44**, keine Fehler/Skips.
  Pro Sprache `session-limits` 35/35 (enthält `authentication` bereits) und
  `maintenance` 9/9. Alle vier Runden mit erfolgreicher Coverage-Erfassung und Cleanup.
- Ein erster Dockerbuild hatte genau einen Testfehler in der oben beschriebenen
  Wartungsfixture. Ein nachfolgender Browserlauf wurde durch das vom Benutzer
  gestartete Dockerupdate unterbrochen: 35 Verbindungsfehler, kein fachlicher
  Nachweis. Nach dem Update wurde diese Runde vollständig erfolgreich wiederholt;
  die unterbrochenen Ergebnisse gehen nicht in die 88 erfolgreichen Fälle ein.

Image aller erfolgreichen Browserrunden:
`sha256:b343230aad715efb2f32cbf8e2dbb8243f6d797cafc10924bcef426ed18ed61a`.
Die aus allen vier Containern gesicherten Anwendungs-JARs haben denselben SHA-256:
`ca531a1867805710d607e91181f93168bfce070ae5c361ff46c49404172a71f1`.
Keine erneuten Imagebuilds zwischen den erfolgreichen Sprachrunden.

Die bestehenden fachlichen JUnits für Admission-Konkurrenz, Login-Context,
Sitzungsverwaltung/-Recovery, Rechteentzug, Wartung und Push laufen im vollständigen
Paket mit. Negative Tests erzeugen absichtlich WARN-/ERROR-Ausgaben und simulierte
Datenbankkonflikte; entscheidend sind ihre Assertions und das erfolgreiche Testergebnis.

## Coverage und Messgrenzen

JaCoCo 0.8.15 aus dem finalen vollständigen JUnit-Lauf, ohne alte Messdaten:
**10055/11490 ausführbare Zeilen (87,5 %)** und **2843/3730 Zweige (76,2 %)**
über 216 Anwendungsklassen. Diese Messung stammt aus JUnit und wird getrennt von
der nachfolgenden Browsermessung ausgewiesen.

| Betroffene Produktionsklasse | Zeilen | Zweige |
| --- | --- | --- |
| FlowCsrfRequestMatcher | 16/16 (100 %) | 17/18 (94,4 %) |
| LoginCsrfTokenFilter | 10/10 (100 %) | 5/6 (83,3 %) |
| SessionAdmissionService, gesamte Klasse | 149/164 (90,9 %) | 57/72 (79,2 %) |
| SecurityConfig, gesamte Klasse | 148/168 (88,1 %) | 33/68 (48,5 %) |

Die Klassenwerte der bestehenden Admission und SecurityConfig umfassen auch
unveränderte Konfiguration und Fehlerpfade. Insbesondere werden damit nicht alle
Ausfallzweige oder H2-Freigabekonstellationen abgedeckt. Vollständige neue
Zeilenabdeckung ist kein Beleg für vollständige Sicherheits- oder Transportabdeckung.

Gemeinsame Browser-Coverage aus ausschließlich den vier erfolgreichen Runden:
**7860/11490 Zeilen (68,41 %)** und **1735/3730 Zweige (46,51 %)** über dieselben
216 Anwendungsklassen. Die Messdaten wurden zusammengeführt und gegen die Klassen
des tatsächlich geprüften Produktions-JARs ausgewertet. Die 44 Fälle pro Sprache
sind eine gezielte Sicherheitsabnahme, keine erneute 152er-Gesamtrunde. Insbesondere
werden damit viele Konfigurations-, Upload- und andere UI-Abläufe nicht erneut
vollständig geprüft. Die niedrigere Quote gegenüber früheren Gesamtrunden ist
kein direkt vergleichbarer Qualitätsrückgang.

## Grenzen und verbleibende Entscheidungen

1. **Gezielte Browserabnahme DE/EN abgeschlossen.** Flow-Login, Remember-me mit
   Browser-/Anwendungsneustart, Logout, zweiter Tab, eingeschränkte Sessionverwaltung,
   Rechteentzug sowie Wartung/Wiederaufnahme sind in den gewählten Suites erfolgreich
   geprüft. Die frühere 152er-Gesamtrunde bleibt ein historischer eigener Stand;
   die übrigen Browserfälle wurden in diesem Paket nicht erneut ausgeführt.
2. **Optionale H2-Konsole:** Standardpfad ist tatsächlich `/h2`, standardmäßig
   deaktiviert. Bei Freigabe durch `APP_SECURITY_EXPOSE_H2_CONSOLE=true` gelten
   die Spring-Prüfungen weiterhin. Ihr unverändertes Browserformular bindet den
   Spring-Token nicht ein; tokenlose Konsolen-POSTs werden mit 403 abgewehrt.
   Im ausgelieferten H2-2.4.240-Template ist das Loginformular tatsächlich
   `method="post" action="login.do?jsessionid=..."`; die H2-eigene Sitzungskennung
   ersetzt den Spring-Token nicht.
   Dies war die offene Kompatibilitätslücke beim Browserabschluss. Der anschließende
   [H2-Nachtrag](h2-console-csrf-2026-10-20.md) schließt sie für die lokale Entwicklung
   mit eng begrenztem Ursprungsschutz; kein Umschreiben nativer Formulare und keine
   pauschale `/h2/**`-Ausnahme. Bestehende Daten und Laufzeitkonfiguration blieben erhalten.
3. **Upload-Grenze:** Der Test verwendet den realen StreamRequestHandler und dessen
   Ressourcen-/Elementprüfungen; Dateieinlesen und Multipart-Verarbeitung am
   UploadHandler bleiben eine ersetzte I/O-Grenze. Die Browserrunde enthält keinen
   gesonderten manipulierten Multipart-Upload oder WebSocket-Upgrade-Negativfall;
   daraus wird keine vollständige Transportabdeckung behauptet.
4. **Zusätzliche anonyme Sitzung bei GET `/login` ist beabsichtigt.** Sie speichert
   den CSRF-Zustand und belegt vor erfolgreicher Zulassung keinen authentifizierten Slot.
5. Kein neuer Sonar-Scan und keine Sonar-Statusänderung. P2-TIME, Worker-Abbruch und
   andere zurückgestellte Pakete wurden nicht bearbeitet.

Implementierung, JUnits und die freigegebene gezielte Browserabnahme sind
abgeschlossen. Die H2-Kompatibilität ist im anschließenden Nachtrag bearbeitet;
die genannten Transportsonderfälle bleiben außerhalb der Abnahme. Der ursprüngliche
[Vorschlag und Fallkatalog C01–C13](sonar-csrf-vorschlag-2026-10-20.md) bleibt als
Planungsstand erhalten; C13 wurde mit den beschriebenen 44 Fällen je Sprache abgenommen.
