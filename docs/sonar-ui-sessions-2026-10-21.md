# UI-/Sessionserialisierung und dauerhafte Anmeldung

Stand: 21.10.2026, Ausgangspunkt main, bbdc6e07b8b4bedf4e4098b6bc814505ca5e3f85.
Keine Commits, Pushes oder Sonar-Statusänderungen. 17 Export-Findings bearbeitet:
16 S1948 und ein S2441. [Jede ID mit Codestelle und Maßnahme](sonar-ui-sessions-findings-2026-10-21.csv).
Der Export enthält noch 54 Einträge, einschließlich bereits vorher bearbeiteter
Pakete. Tatsächlichen Entfall und neue Folgehinweise erst nach erneutem Benutzer-
Scan beurteilen. Eine Reduktion um genau 17 wird nicht als Scanergebnis behauptet.

## Abgestimmter Umfang

Auf ausdrückliche Wahl des Benutzers wurde zusätzlich zum ursprünglichen
Wartbarkeitspaket dauerhaft wiederherstellbare Anmeldung eingeführt. Das ist eine
Funktionsänderung mit zusätzlicher Spring-Session-JDBC-Abhängigkeit und zwei
Tabellen, kein rein kosmetisches Refactoring. Maßgeblich ist der
[Speicher-, Neustart- und Einführungsvertrag](durable-session-contract.md).

- Typisierte unveränderliche Notification-/Filterwerte sind serialisierbar.
- MessageProperties verwendet einen Proxy mit dynamischem bzw. festem Locale;
  keine zweite Übersetzungsliste und kein serialisierter Providergraph.
- Pagination-/Filteraktionen haben echte serialisierbare Callbacktypen.
- UI-Guards halten serialisierbare Beanreferenzen und lösen nach Lesen ausschließlich
  den aktuellen verwalteten Dienst im aktiven Servletkontext auf.
- Registration speichert keine Registry/Map-Graphen. Nach Lesen erfolgt Freigabe
  über den aktuellen Dienst. Test prüft, dass die alte Registry unberührt bleibt.
- Entity-basierte Editoren lehnen Passivierung ausdrücklich ab. Das entspricht dem
  gewählten UI-Neuaufbau; pauschales Serializable an JPA-Entities oder verlustbehaftete
  transient-Listen wurden vermieden. Dieser Vertrag ist getestet und offen benannt.

## Sicherheits- und Lebensdauerregeln

JDBC-Sessionzustand ist eine feste versionierte JSON-Typauswahl. Kein beliebiges
Java-Deserialisieren, keine persistierten Vaadin-Locks/-UIs oder Login-Recovery-Proofs.
Anmeldung, Admission-Metadaten, CSRF, Fixation-Wechsel, Ablauf und Widerruf haben
einen gemeinsamen dauerhaften Bestand. Echte Authentifizierungsfaktoren behalten
ihre Zeit; Kontorollen werden frisch gelesen. Unzulässige gespeicherte Zustände
werden verworfen, Datenbankausfälle nicht mit lokalem Zugang kaschiert.
Ein eigener Passwortwechsel erhält nach Commit die ausdrücklich aktuelle Sitzung;
andere Passwortversionen werden verworfen. Runtime-UI-Cleanup benachrichtigt den
Vaadin-Dienst je Sitzung genau einmal. Verlorene UI-Transporte dürfen ausschließlich
ein frisches Dokument anfordern, keine alte Fachaktion nachholen.

Die bestehenden Kontosperren, Reservierung vor Contextveröffentlichung,
Completion-Rollback, Remember-me-Serien und Wartungs-/Rechteentscheidungen bleiben
erhalten. Kein neues Design, keine CSS-/i18n-Inhaltsänderung. Der Betrieb bleibt
auf eine Instanz begrenzt; gemeinsame Datenbank allein reicht für Cluster nicht.

## Automatisierte Nachweise

JDK 25.0.4.1 und Maven Wrapper 3.9.16 geprüft. Nach jeder Java-Runde Spotless
Apply/Check und Checkstyle vor Tests; zuletzt grün. Wegen laufender Entwicklungs-
umgebung Anwendungsbuild in isolierter Quellkopie, 371 Build-Eingaben per SHA-256
mit dem Arbeitsstand abgeglichen, keine Abweichung. Keine .env, bestehenden Daten-
banken, Logs oder laufenden Entwicklungsprozesse verwendet.

- 57 neue Regressionstest-Ausprägungen im finalen Gesamtlauf enthalten; zusätzlich
  alle vorhandenen Kontosicherheitstests erfolgreich.
- Vollständiger Produktions-Clean-Verify: **2185/2185 Tests**, null Fehler/Skips.
  Gezielte Vorläuferläufe sind Teilmengen und werden nicht addiert.
- **49/49 Browser-Helfer-/Reporterprüfungen**, null Fehler/Skips.
- JaCoCo frisch nach clean: **10764/12200 Zeilen (88,23 %)** und
  **3180/4094 Zweige (77,67 %)**; 236 ausführbare Anwendungsklassen.
  Neues security/session-Paket: 345/374 Zeilen (92,25 %), 124/164 Zweige (75,61 %).
- Workbox-Abhängigkeiten und Vite-Entwicklungskonfiguration im ursprünglichen
  Entwicklungsprojekt danach erfolgreich geladen; kein Entwicklungsserver gestartet.

Neue Regressionen verwenden echte JDBC-Commits und getrennte Spring-Kontexte an
derselben eigenen H2: Passwort-/Remember-me-Anmeldung, Factor-Zeit, aktuelle Rollen,
Sperre/Löschung/Passwortwechsel, Keywechsel, Cookieeigenschaften, CSRF/Logout,
Wartung, Reservierungen und konkurrierende Zulassung, ID-/Mutex-Wechsel, fehlgeschlagene
Completion, verspätete Saves, abgelehnte JSON-Typen/-Versionen/-Größen, Runtime-Cleanup
und UI-/Callback-/Dienstreferenz-Roundtrips. Der Flow-Loginhelper in JUnit ist kein
Nachweis eines echten Flow-Browsertransports.

Zwischenfehler wurden korrigiert: Testfixture verglich VaadinServletRequest mit
dessen ursprünglichem Mock, Reflexionsfixture benötigte einen expliziten Cast,
Suite-Helper benötigte List<Class<?>>; PowerShell-Mavenoptionen waren zu zitieren.
Die betroffenen Läufe wurden anschließend erfolgreich wiederholt. Keine vorhandene
Assertion abgeschwächt. Die eigene Passwortwechsel-Wechselwirkung wurde vor der
Browserabnahme erkannt und mit einer weiteren Regression abgesichert.

Die erste Browserrunde fand außerdem einen echten Integrationsfehler: Zu frühes
Entfernen ungültiger Authentifizierung/Laufzeit-UIs verlor den Widerrufsnachweis
für alte RPCs. Jetzt bleibt eine abgelaufene JDBC-Registrierung ohne Authentifizierung
bis zur HTTP-Invalidierung erhalten. Der zentrale Vaadin-Filter erkennt auch
diese Registrierung und weist die alte Fachanfrage mit 401 ab. Ein zusätzlicher
Test an der echten Sicherheitskette prüft 401 und null Fachdispatches. Weitere
14 gezielte Filterfälle sichern Servlet-/Kontextpfade, gefälschte Transportmarker,
Wartung, fehlende/abgelaufene Anmeldung und vorhandene UIs ab.

Zwei Browser-Fixtures wurden präzisiert: Bei bereits im zweiten Tab angekommener
Abmeldung wird unmittelbar die Loginmaske geprüft; sämtliche abschließenden
Zugriffsprüfungen bleiben. Nach absichtlichem Serverneustart erhält auch der Admin-
Fixture ein frisches Dokument vor der Konfigurationswiederherstellung. Keine
fachlichen 401-/Daten-/Rollen-/Limit-Assertions entfernt oder abgeschwächt.

Der finale Review ergänzt außerdem eine gemeinsame Synchronisierung für ID-Rotation,
HTTP-Lookup und periodischen Runtime-Cleanup. Ein nachweislich in der ID-Veröffentlichung
angehaltener Writer und ein gleichzeitig gestarteter Reader prüfen beide Eintrittswege:
alte Kennung danach ungültig, neue Kennung und UI erhalten, kein SessionDestroy.
Beide Parameterfälle sind im finalen vollständigen Lauf enthalten (2185 Fälle).

Ein weiterer Zwischenlauf bestand zunächst 38 Fälle einschließlich aller korrigierten
401-Fälle und SESSIONS-06. Beim vorbereitenden Neubau eines weiteren Images war die alte
Manifestreferenz des laufenden Zwischenimages danach nicht mehr verfügbar; ein Test-
Neustart konnte sie nicht erneut auflösen. Die übrigen elf Fälle scheiterten an dieser
Testumgebung. Das ist kein grüner Anwendungstest. Die abschließenden Sprachläufe
verwenden deshalb dieselbe unveränderte finale Image-Zuordnung ohne weiteren Neubau.

## Browserabnahme

Die ausdrücklich freigegebene **gezielte** Suite session-persistence bestand
**49/49 Fälle auf Deutsch und 49/49 auf Englisch**, keine Skips. Je Sprache zwei
isolierte JVM-/Docker-/H2-/Mailpit-Worker, beide am identischen Produktionsimage:
`sha256:0ee9176c96bd33ccfdb35152e89e9bc9d6c21acecc91a346ecbcedd6d0277fdd`. Worker- und Parent-Metadaten, 49 einzelne grüne Allure-Ergebnisse
je Sprache, Cleanup, Coverage und HTML-Erzeugung erfolgreich. Keine vollständige
153-Fall-Gesamtrunde durchgeführt oder als bestanden behauptet.

| Prüfung | Fälle | Java-Zeilen | Java-Zweige |
| --- | ---: | ---: | ---: |
| Browser gezielt DE | 49/49 | 8826/12200 (72,34 %) | 1976/4094 (48,27 %) |
| Browser gezielt EN | 49/49 | 8825/12200 (72,34 %) | 1976/4094 (48,27 %) |

Gemessen werden 236 ausführbare Anwendungs-Java-Klassen aus dem jeweils tatsächlich
geprüften JAR; Startup/Hintergrundarbeiten eingeschlossen, keine CSS-/JavaScript-
oder vollständige Anforderungsabdeckung. Die Sprachwerte sind getrennte Messungen;
Vorläuferläufe werden nicht addiert. Der spätere endgültige deutsche Lauf ersetzt
den umgebungsbedingt unterbrochenen Zwischenlauf vollständig.

Die neun Klassen prüfen echte Flow-Anmeldung und getrennte Browserkontexte,
Passwort-/Remember-me-Rückkehr, Server-/Chromium-Neustarts, CSRF-Logout, erhaltene
Slots/Metadaten, Einzel-/Sammelwiderruf, eingeschränkte Passwort-Recovery, Rollen,
Sperre/Löschung, Passwort-/E-Mail-Wechsel samt eigenen abgefangenen SMTP-Mails
und Wartung. AUTH-07C/D erhält beide Loginarten und prüft dauerhaften Logout über
den folgenden Neustart. SESSIONS-06 prüft alte tatsächliche Formularaktionen nach
Restart: neuer Dokumentaufbau, Entwurf ungespeichert, Limit erhalten und Widerruf
weiter wirksam. Browserfälle und Fallzahlen sind in README/Suite-Helpern nachgeführt.

## Grenzen und Einführung

Build und Anwendungsneustart erforderlich. Beim ersten Upgrade bisherige lokale
Anmeldungen erneut anmelden; danach dieselbe Datenbank/Secrets erhalten. Neues
Schema wird idempotent angelegt, kein Zurücksetzen alter Inhalte. Keine Bestands-
datenbank-Migration oder reale IDE-Neustartabnahme nachgewiesen. Kein Cluster,
kein Rolling Upgrade, keine Wiederaufnahme offener Formulare/Uploadentwürfe.
Ein Sitzungscookie verspricht keine Anmeldung nach echtem Browserschließen.
Gespeicherte Zugänge ohne aktive Sitzung nach Timeout sind weiterhin durch JDBC-
Tests geprüft; die Browserrunde wartet keinen produktiven 30-Minuten-Timeout ab.
Coverage misst Java, keine CSS-/JavaScript-Abdeckung und keine vollständige
Anforderungsabdeckung. Sonar-Nachscan steht aus.
