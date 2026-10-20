# Mail-Workerpool

Stand: 18.10.2026. Implementiert; keine Schemaänderung.

**Nachtrag vom 20.10.2026:** Die Unterbrechung des Koordinators wird jetzt
zusätzlich vor jeder weiteren Aufgabenannahme geprüft. Der zuvor offene
Nachfüllbefund ist mit sechs gesteuerten Regressionen behoben; 1977 Anwendungs-
JUnits, 48 Helfer und Produktionsbuild erfolgreich.
[Ursache, Vorher-/Nachher-Nachweis und Grenzen](email-worker-interrupt-2026-10-20.md).

## Konfiguration und Wirkung

| Einstellung | Wirkung | Aktivierung |
| --- | --- | --- |
| `email.queue.pool.size` | Höchstens N parallele Queue-Aufgaben pro Instanz, mindestens 1, Standard 5 | Beim Start; Änderung braucht Neustart |
| `email.queue.capacity` | Höchstens so viele fällige Kandidaten pro Lauf, mindestens 1, Standard 50 | Vor jedem Lauf |
| `email.queue.max_retry` | Wiederholungen nach bewiesenem Nichtversand, mindestens 0 | Vor jedem Lauf |

Beispiel: Pool 5 und Kapazität 50 betrachten bis zu 50 fällige Nachrichten und
bearbeiten bis zu fünf gleichzeitig. Die Datenbank bleibt der dauerhafte Wartebereich;
es gibt kein neues Limit der gespeicherten Mails. Ein Lauf lädt nur eine Kandidatenseite.
Die Reihenfolge der Auswahl bleibt erhalten, die Abschlussreihenfolge kann abweichen.
Dies ist keine Zusage über Mails pro Sekunde oder Kontingente eines Mailanbieters.
Mehrere Anwendungsinstanzen haben jeweils einen eigenen Pool.

## Ausführung und Fehler

`ThreadPoolConfig` erzeugt den ausdrücklich benannten `emailTaskExecutor` mit N
Plattformthreads und begrenzter Übergabe. `EmailQueueWorkerPool` nimmt höchstens N
Aufgaben gleichzeitig an und füllt nach jeder abgeschlossenen Aufgabe einen freien
Platz. Der kleine Executor-Puffer hängt von N ab, nicht von der live veränderbaren
Batchkapazität; er dient nur der Übergabe und ist keine weitere dauerhafte Warteschlange.

Überlappende Batchaufrufe derselben Instanz werden übersprungen. Ihre unbeanspruchten
Mails bleiben für den nächsten Lauf erhalten. Der reguläre Scheduler wartet auf den
Batchabschluss und dann weitere fünf Sekunden. Er verwendet keinen `@Async`-Selbstaufruf.

Zwischen Koordinator und Workern werden nur IDs übertragen. Keine JPA-Entities,
offenen Transaktionen, Vaadin-UI-/Sitzungsobjekte oder Benutzerkontexte werden übergeben.
Die Threadfactory deaktiviert die Vererbung von InheritableThreadLocals.
Der bestehende [Queuevertrag](email-queue-processing.md) bleibt verbindlich:
frischer Claim unter Datenbanklock, Versand erst nach Claim-Commit ohne Queue-Transaktion,
separater Ergebnis-Commit und Adminaudit. Sender/MIME-Nachricht sind aufruflokal.

Bei Executor-Ablehnung vor Aufgabenstart wird nicht geclaimt; der verbleibende Batch
wird nicht weiter eingereicht. Bereits angenommene Aufgaben werden abgewartet.
Workerfehler beeinflussen die Ergebnisse anderer Worker nicht. Ein unterbrochener
Koordinator reicht nicht weiter nach, wartet auf angenommene Aufgaben und stellt seinen
Interruptstatus wieder her. Unklare Transport-/Ergebnisfehler behalten `IN_PROGRESS`.
SMTP und Datenbank bleiben nicht atomar; es gibt keine Exactly-once-Garantie.

## Herunterfahren

Das Context-Close-Signal verhindert weitere Aufgabenstarts und Nachfüllung.
Die Poolzerstörung wartet bis zu 30 Sekunden auf laufende Aufgaben. Danach werden
Unterbrechung und Future-Abbruch angefordert, auch für noch nicht gestartete Aufgaben;
wartende Koordinatoren können dadurch abschließen. Noch nicht geclaimte Mails bleiben
`PENDING`. Geclaimte Mails werden nicht automatisch freigegeben oder erneut versandt.

Ein Threadinterrupt beendet keine beliebige blockierende Fremdbibliothek garantiert.
Die Worker sind deshalb Daemonthreads. Ein weiter blockierter Versand hält das JVM-Ende
nicht auf, sein Ausgang bleibt gegebenenfalls ungeklärt. Die 30 Sekunden sind die
Wartezeit dieses Pools; andere Spring-Komponenten haben eigene Shutdownphasen.
Es werden keine neuen SMTP-Timeouts oder Recoveryregeln eingeführt.

## Automatisierte Nachweise und Grenzen

- `EmailQueueWorkerPoolTest`: echte Threads und Latches für N=1/2/3, Nachfüllung trotz
  langsamer Aufgabe, Überlappung, Fehler, teilweise/vollständige Ablehnung, Close,
  graceful/erzwungener Shutdown und Koordinatorunterbrechung.
- `ThreadPoolConfigTest`: positive/ungültige Größen, benannter Executor, begrenzte
  Übergabe, Daemonstatus, keine Kontextvererbung und tatsächliche Spring-Verdrahtung.
- `EmailQueueWorkerIntegrationTest`: echtes isoliertes H2, zwei unabhängige Pools
  gegen gemeinsame Claims, Batchgrenze, Nichtfälligkeit, Claim-/Ergebnis-Commitfehler,
  getrennte Versandausgänge/Adminaudit und ungeklärter Claim nach erzwungenem Shutdown.
  Eigener Loopback-SMTP-Server belegt zwei gleichzeitig offene Transporte über den
  echten Mail-Sender und getrennte MIME-Inhalte dreier synthetischer Nachrichten.

Bestehende Claim-/Transaktionstests verwenden einen ausdrücklich injizierten synchronen
Testkoordinator. Sie behalten ihre Assertions; die Pool-/Integrationstests prüfen die
neue Threadgrenze separat. Keine realen Datenbanken/Postfächer oder Provider verwendet.
Kein aktueller Browser- oder Produktionsnachweis aus diesen JUnit-Tests abgeleitet.
Aktuelle Zahlen: [Abdeckungsübersicht](../browser-tests/abdeckung.md).

## Einführung

Shared und Anwendung bauen, Anwendung vollständig neu starten. HotSwap allein genügt
für den neuen Bean-/Executor-Lebenszyklus nicht. Die vorhandenen 28 Konfigurationsfelder
bleiben erhalten; nur die Feldhilfen für Poolgröße und Kapazität werden in DE/EN präzisiert.
Keine Design-, Schema-, Retry-, Mailtext- oder Aufbewahrungsänderung.

JDK-Grundlagen: [Threadfactory ohne Kontextvererbung](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Thread.Builder.OfPlatform.html),
[FutureTask-Abschluss und Abbruch](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/FutureTask.html).
