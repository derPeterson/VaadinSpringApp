# Mail-Worker: Unterbrechung vor weiterer Aufgabenannahme

Stand: 20.10.2026, Branch `fix/mail-worker-interrupt`, Ausgangspunkt `d46b2d5`.
Keine Commits, Pushes oder Sonar-Statusänderungen durch diese Arbeit.

## Nachgewiesenes Problem

Der im [Sonar-Phase-2-Bericht](sonar-phase2-2026-10-20.md) beobachtete
Worker-Abbruchfall war in späteren Läufen zwar erfolgreich, blieb aber fachlich
offen. Der Koordinator stoppte das Nachfüllen ausschließlich nach einer
`InterruptedException` beim Abwarten eines Ergebnisses. Ein bereits fertiges
`FutureTask.get()` muss nicht mehr warten und kann mit gesetztem Interruptflag
zurückkehren. Auch während der ersten Poolbefüllung fehlte eine Prüfung.

Sechs neue, gesteuerte Testausprägungen scheiterten gegen den unveränderten
Produktivworker mit genau den erwarteten zusätzlichen Annahmen:

- Vier Nachfüllfälle: Poolgröße 1 oder 2, jeweils erfolgreiche oder fehlgeschlagene
  bereits abgeschlossene Aufgaben. Erwartet N angenommene Aufgaben, tatsächlich N+1.
- Bereits beim Eintritt unterbrochener Koordinator: erwartet 0 Aufrufe, tatsächlich 1.
- Unterbrechung während der ersten Poolbefüllung: erwartet 1, tatsächlich 2.

Die Nachfülltests verwenden absichtlich sofort abgeschlossene Futures und eine
kontrollierte Unterbrechung bei der nächsten Kandidatenprüfung. Damit bilden sie
den relevanten Zustand zwischen Ergebnisverarbeitung und Nachfüllung ohne
Sleep oder zufälliges Threadtiming ab. Das ist kein nachträglich instrumentierter
Beweis des exakten Ablaufs im früheren fehlgeschlagenen Gesamtlauf.

## Korrektur

`EmailQueueWorkerPool.process()` prüft unmittelbar vor jeder weiteren
Kandidatenannahme `Thread.interrupted()`. Eine erkannte Unterbrechung stoppt die
Nachfüllung für den Rest dieses Batches. Das Flag wird beim Erkennen gelöscht,
damit bereits angenommene Aufgaben weiterhin abgewartet werden können; der
bestehende `finally`-Block stellt es bei Rückkehr wieder her.

Es bleiben dieselben Sperren, Transaktionsgrenzen, Poolgrößen und Abschlussregeln.
Unbeanspruchte Queueeinträge bleiben für einen späteren Lauf verfügbar.
Bereits angenommene Aufgaben werden weder neu eingereicht noch wegen eines
Koordinatorinterrupts abgebrochen. Ein späterer Batch kann nach Behandlung des
Interruptstatus wieder laufen. Keine neue Meldung pro Interrupt oder Pollingzyklus.

Die Unterbrechung bleibt kooperativ: Sie ist kein atomarer Widerruf einer bereits
laufenden Aufgabenannahme. Ein erst nach der Prüfung eintreffender Interrupt kann
eine gerade gleichzeitig begonnene Annahme nicht rückwirkend verhindern. Auch
blockierende Fremdbibliotheken werden dadurch nicht garantiert beendet. Die
bestehenden Shutdown- und konservativen Recoveryregeln bleiben maßgeblich.

## Prüfung

| Tatsächlich ausgeführte Prüfung | Ergebnis |
| --- | --- |
| Neue Regressionen vor der Produktivkorrektur | 6 Tests, 6 erwartete Failures, keine Errors/Skips; zusätzliche Annahmen nachgewiesen |
| Gezielte Worker-/Konfigurations-/Queue-Nachprüfung | **174/174 erfolgreich**, keine Failures/Errors/Skips |
| Vollständiger Maven-Lauf mit Produktionsprofil und `verify` | **1977/1977 erfolgreich**, keine Failures/Errors/Skips; ausführbares Produktions-JAR gebaut |
| Browser-Helfertests ohne Browser-/Anwendungsstart | **48/48 erfolgreich**, keine Failures/Errors/Skips |
| Spotless apply/check und Checkstyle nach letzter Java-Änderung | Erfolgreich, keine Stilabweichungen |
| Abschließende Diff-/Whitespaceprüfung | Erfolgreich |

Sechs zusätzliche Testausprägungen gegenüber dem Ausgangsstand. Die gezielten
174 Fälle sind Teil der vollständigen 1977 und werden nicht zusätzlich gezählt.
JDK 25.0.4.1 und Maven Wrapper 3.9.16; keine Abhängigkeitsänderung.

Frische JaCoCo-Messung ausschließlich aus dem vollständigen aktuellen Testlauf:
**10202/11631 Zeilen = 87,71 %**, **2959/3850 Zweige = 76,86 %**,
219 analysierte Anwendungsklassen. Der Koordinator selbst erreicht 73/76 Zeilen
und 31/34 Zweige. Vorhandene übrige Lücken bleiben sichtbar; diese Messung ersetzt
keine Betriebsabnahme oder Prüfung aller möglichen Threadabläufe.

Die sechs neuen Tests prüfen zusätzlich die Wiederherstellung des Interruptflags
und, wo vorgesehen, die erneute Verwendbarkeit des Batchlocks. Eigene Tests löschen
ihr Test-Interruptflag im Cleanup. Der bisherige echte Thread-/Latch-Abbruchtest
bleibt mit seinen strikten Assertions erhalten. Vorhandene Fälle für Parallelität,
Überlappung, Ablehnung, Workerfehler sowie graceful und erzwungenen Shutdown
werden nicht abgeschwächt.

## Einführung und Prüfgrenzen

Anwendung mit dem geänderten Java-Code bauen und vollständig neu starten;
ein Browser-Refresh übernimmt die Änderung nicht. Kein Schema-, Konfigurations-,
UI- oder Übersetzungswechsel. Keine bestehende Datenbank und kein echter
Mailprovider wurden für diese Arbeit verwendet; Integrationstests besitzen eigene
flüchtige Datenbanken und gegebenenfalls einen lokalen synthetischen SMTP-Server.
Keine neue Browserabnahme oder Dockerimage für dieses Paket. Kein Sonar-Scan oder
manueller Statuswechsel im Sonar-Server.

Grundlagen: [Java 25 FutureTask.get()](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/FutureTask.html#get())
und [Thread.interrupted()](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/Thread.html#interrupted()).
