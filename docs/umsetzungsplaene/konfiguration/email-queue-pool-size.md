# Den Mail-Workerpool tatsächlich verwenden

Stand: 15.10.2026; Ausgangscommit `1d57a5e`. **Umsetzungsplan, noch nicht implementiert.**

| Eigenschaft | Wert |
| --- | --- |
| Konfiguration | `email.queue.pool.size` / `ConfigEntry.EMAIL_QUEUE_POOL_SIZE` |
| Enum-Standard | `5` |
| Gruppe | Mailqueue |
| Vorgeschlagene Aktivierung | Workerzahl beim Start; Änderung benötigt zunächst Neustart |
| Priorität | Eigenes Paket nach den Anmelderegeln |

## Heutiger Stand

`ThreadPoolConfig` baut `emailTaskExecutor` mit gleicher Kern-/Maximal-Threadzahl aus
diesem Wert. Der Queuepfad hat aber keinen `@Async`-Verbraucher, keine benannte
Executor-Injection und keinen `execute`-/`submit`-Aufruf. `EmailQueueService.processQueue()`
läuft geplant fünf Sekunden nach Ende des vorherigen Laufs und versendet ausgewählte
Kandidaten in einer normalen Schleife. Die Poolgröße steuert damit nicht die beabsichtigte
gleichzeitige Queuezustellung.

Die Queue unterstützt bereits konkurrierende Bearbeitung über Datenbank-Claims.
SMTP läuft außerhalb der Claim-/Ergebnistransaktion. Unklare Ausgänge bleiben
`IN_PROGRESS`. Diese implementierten Verträge bleiben die Grundlage.

## Empfohlener erster Umfang

**Vorschlag:** Bis zu N Queue-Versandaufgaben gleichzeitig **pro Anwendungsinstanz**;
bei N=1 sequenziell. Ein Koordinator wählt weiterhin höchstens `email.queue.capacity`
fällige Kandidaten pro Lauf und füllt freie Workerplätze innerhalb dieses Batches nach.

Beispiel: Pool 5, Kapazität 50. Ein Lauf betrachtet höchstens 50 Kandidaten und bearbeitet
höchstens fünf gleichzeitig. Endet einer, kann der nächste dieses Batches starten.
Viele gespeicherte Queueeinträge werden deshalb nicht verworfen. Die nächste geplante
Runde beginnt wie bisher nach Abschluss des Laufs plus Verzögerung.

Das ist ein Parallelitätslimit, keine Mails-pro-Sekunde-Zusage. Mehrere Instanzen können
zusammen mehr als N Transporte ausführen; ein globales SMTP-Kontingent bräuchte weitere
Koordination. Bestehende minimale Poolgröße 1 erhalten. Eine neue Ressourcenobergrenze
wäre separat zu begründen und zu dokumentieren, nicht heimlich einzuführen.

## Verhältnis zu `email.queue.capacity`

Dieser zweite Wert ist **bereits wirksam**: Er begrenzt die fällige Kandidatenseite.
Zusätzlich konfiguriert er den Puffer des bisher ungenutzten Executors. Die Bedeutungen
vor Aktivierung bewusst auseinanderhalten.

Mein Vorschlag: Die Datenbank bleibt dauerhafter Wartebereich. `capacity` behält seine
wirksame Bedeutung als Kandidatenlimit. Der Koordinator hält eine begrenzte Kandidaten-
liste und höchstens N laufende Aufgaben; keinen großen zusätzlichen RAM-Taskstau.
Executor für begrenzte Übergabe konfigurieren und die bisherige zweite, ungenutzte
Puffer-Bedeutung in der Feldhilfe bewusst ersetzen. Für eine dauerhafte zusätzliche
Taskqueue wäre ein gesonderter Vertrag nötig. Keine neue Gesamtgrenze für gespeicherte Mails.

Kapazität und Retrylimit wie bisher vor Auswahl/Versand pro Lauf validieren und für
diesen Lauf festhalten. Die Workerzahl bleibt zunächst startgebunden. Die Feldhilfen
müssen diese unterschiedlichen Wirkungszeitpunkte erklären.

## Sichtbare Änderung

Mehrere fällige Mails können parallel abgearbeitet werden. Eine langsame SMTP-Verbindung
belegt einen Worker; andere können in freien Plätzen fortfahren. Die Auswahl bleibt
nach Fälligkeit und `createdAt`/`id` sortiert, Abschlussreihenfolgen können abweichen.

Kontoansichten, Mailtexte, Retryabstand und Aufbewahrung ändern sich nicht. Ein neues
Monitoring-Dashboard oder eine manuelle Recoveryoberfläche gehört zunächst nicht dazu.
Workerzahl-/Kapazitätshilfe werden auf DE/EN präzisiert; Stile bleiben erhalten.

## Technischer Umsetzungsweg

1. Einen kleinen **Koordinator und Worker neu vorgeschlagen** abgrenzen. Bestehende
   Claim-/Versand-/Ergebnisregeln wiederverwenden, nicht für Asynchronität duplizieren.
   `emailTaskExecutor` ausdrücklich injizieren; `@Async` an einer selbst aufgerufenen
   Methode wäre keine ausreichende Verdrahtung.
2. Begrenzte Nachfüllung pro Batch: höchstens N aktive Aufgaben, weitere erst nach
   Abschluss oder freiem Platz. Auch überlappende manuell ausgelöste Läufe müssen die
   instanzweite Grenze einhalten. Koordinator wartet ohne Datenbanktransaktion auf
   den Batchabschluss, damit die bestehende fixed-delay-Semantik erhalten bleibt.
3. Nur Queue-IDs an Worker übergeben. Jeder Worker lädt und beansprucht den Eintrag
   unmittelbar vor Versand in eigener frischer Transaktion. Keine Managed-Entities,
   UI, VaadinSession, SecurityContexts oder offenen Transaktionen zwischen Threads
   teilen. Claim gilt erst nach erfolgreichem Commit.
4. Bei Executor-Ablehnung, Shutdown oder Fehler **vor** Claim bleibt die Mail `PENDING`.
   Einen im RAM vorgemerkten Kandidaten nicht vorab auf `IN_PROGRESS` setzen. Bereits
   geclaimte/unklare Sendungen behalten dagegen den bestehenden konservativen Zustand;
   Workerfehler dürfen sie nicht blind erneut starten.
5. Genau einen Claim unter Datenbanklock zulassen. Zwei Instanzen oder Kandidatenlisten
   können dieselbe ID sehen; gespeicherter Zustand verhindert den zweiten Versand.
   Fehler einer Aufgabe dürfen Ergebnisverbuchung anderer Aufgaben nicht abbrechen.
6. Sender und Nachrichtenerzeugung auf aufruflokalen Zustand prüfen. UTF-8/CID und
   `ConnectionAwareJavaMailSender` erhalten. Adminbenachrichtigung bleibt im vorhandenen
   terminalen Fehlerpfad; kein zweiter Versand aus einem Future-Fehlerhandler.
7. Shutdown regeln: neue Aufgaben stoppen, laufende für eine dokumentierte begrenzte
   Zeit beenden lassen. Harte Unterbrechung kann unklare Claims hinterlassen; normale
   Neustarts setzen diese nicht automatisch zurück auf `PENDING`.
8. Logs mit Queue-ID/Workerbezug, ohne Mailbody oder Token. Queuevertrag, Feldhilfen
   und Testanleitung auf tatsächlich geprüfte Regeln aktualisieren.

Keine gemeinsame Transaktion über Futures und kein neuer SMTP-Retry im Executor.
Die feste Retryminute, begrenzte Wiederholungen, Auditierung und Aufbewahrung sind
bereits implementierte eigenständige Funktionen.

## Tests und Abnahme

- **Deterministische JUnit-/Persistenztests:** Mit Barrieren/Latches belegen, dass N
  Transporte gleichzeitig laufen können und N+1 nicht startet. Kürzere Laufzeit oder
  zufällige Sleeps sind kein Parallelitätsnachweis.
- **Batchgrenze:** höchstens Kapazität Kandidaten, Nachfüllen im Batch, N=1, kleine
  Kapazität, nicht fällige Einträge und ungültige Konfigurationswerte.
- **Konkurrenz:** dieselbe ID aus zwei Koordinatoren und zwei getrennten Instanzen
  mit gemeinsamem isoliertem Speicher; kein Doppelversand pro erfolgreichem Claim.
- **Fehler:** Executor-Ablehnung vor Claim, Claim-/Ergebnis-Commitfehler, einzelner
  Workerfehler, unklare SMTP-Annahme, Adminversand und Shutdown.
- **Regression:** vorhandene Queue-, Persistenz- und Verifikations-Konkurrenztests;
  keine Änderung von Retry-/Deduplizierungs-/Retentionverträgen.
- **Integration:** eigene SMTP-Fixture weist gleichzeitig offene Transporte und
  erwartete Mails nach. Browser prüfen weiter echte Registrierung/Resetmail und
  Konfigurationshilfe; sie ersetzen keinen technischen Parallelitätsnachweis.

Fertig ist das Paket bei belegter Parallelität und Grenzen, erhaltenem Claimvertrag
und geprüftem Fehler-/Shutdownverhalten. Die vorgeschlagenen Tests stehen aus.

## Grundlage und Anschlussstellen

[ThreadPoolConfig](../../../src/main/java/de/derpeterson/app/config/ThreadPoolConfig.java),
[EmailQueueService](../../../src/main/java/de/derpeterson/app/service/EmailQueueService.java),
[EmailQueueRepository](../../../src/main/java/de/derpeterson/app/repository/EmailQueueRepository.java),
[Queuevertrag](../../email-queue-processing.md), [Mailvertrag](../../email-service-contract.md).

Spring erläutert Executor-Puffer, Ablehnung und Proxycharakter von `@Async`:
[Task Execution and Scheduling](https://docs.spring.io/spring-framework/reference/integration/scheduling.html).
Der Batch-Koordinator mit begrenzter Nachfüllung ist mein Vorschlag für diese Anwendung.
