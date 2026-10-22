# Logging

Bei Loggingarbeiten diesen Vertrag einschließlich der Beispiele lesen und anwenden.
Eigene Logmeldungen sind einheitlich **Englisch**, unabhängig von der UI-Sprache.
Keine Übersetzung über `MessageProperties`; Dokumentation und Kommunikation bleiben Deutsch.

Beim eigenen IDE-HTTPS-Supervisor ist stdout ausschließlich das maschinenlesbare
`READY <pid>`-Startprotokoll samt Flush. Das ist keine Logmeldung. Die vom eigenen
Caddy bereits erzeugte Diagnose wird als Prozessausgabe unverändert an stderr
weitergeleitet. Normale eigene Lifecyclemeldungen verwenden den Logger; die beiden
Prozesskanäle nicht durch zusätzliche Loggerformatierung verändern.

## Pegel und Diagnose

Normalbetrieb ist INFO. Die Dateiausgabe steht in `src/main/resources/logback-spring.xml`;
Spring-Pegel in `application.properties`. Für eine Untersuchung gezielt einen Dienst
auf DEBUG setzen, danach wieder zurückstellen.

Die geerbten Logger `AdmittedSecurityContextRepository` und `DeviceRememberMeServices`
bleiben ausdrücklich INFO, auch bei DEBUG am Anwendungspaket. Kontext-/Principal-
Dumps sind für normale Anfragen nicht nötig. WARN und ERROR bleiben sichtbar.
Dateirotation: 25 MiB pro Teil, tägliche Aufteilung und maxHistory=1.

| Pegel | Zweck |
| --- | --- |
| INFO | Wichtige erfolgreiche Fachaktionen und Start-/Betriebszustände |
| DEBUG | Gezielte fachliche Diagnose und begründete No-ops |
| WARN | Behandelbare Störungen oder verworfene ungültige Zustände |
| ERROR | Fehlgeschlagene Verarbeitung, Persistenz-/Commit- und technische Fehler |

Sinnvolle Ereignisse sind der Abschluss einer wichtigen Konto-/Adminaktion,
Queueaufnahme und Transportergebnis, ausgeschöpfte Wiederholungen oder ein
behandelbarer Speicher-/Konfigurationsfehler. Diagnose soll erkennen lassen, welcher
Vorgang betroffen ist und ob eine nächste Maßnahme nötig ist; zulässige Metadaten
stehen unten. Technische Umsetzungsvorgaben gelten auch für neue Features:
[Agentenregeln](../AGENTS.md).

Keine Protokollierung jedes Methodenaufrufs, Polls oder `if`-/`else`-Zweigs. Eine
Fehlergrenze protokolliert den Fehler; nicht dieselbe Exception auf jeder Ebene
erneut ausgeben. DEBUG nur ergänzen, wenn es eine konkrete Diagnosefrage beantwortet.

## Inhalte

Erlaubt sind interne IDs, Typen, Anzahlen, Versuchsnummern und unkritische Zustände.
Nicht erlaubt sind Kennwörter/-hashes, Tokenwerte/-digests/-serien, Sitzungskennungen,
Cookies, Principalobjekte, E-Mail-Adressen, Betreff/Body und rohe Formularwerte.

`LogSafety.safeFailure` entfernt freie Exceptiontexte und erhält Typ/Stackframes.
Die fachliche Exception selbst wird unverändert weiterbehandelt. Entities nicht als
verzögert ausgewertete Logargumente übergeben.

## Erfolg und Commit

Ein transaktionaler Erfolg wird über `AfterCommitLog` erst nach Commit gemeldet.
Nach einem eigenständig abgeschlossenen TransactionTemplate darf direkt geloggt werden.
Queueaufnahme, SMTP-Annahme und bestätigtes Ergebnis sind verschiedene Ereignisse.
Ein unklarer IN_PROGRESS-Auftrag wird nicht als sicher zugestellt oder fehlgeschlagen ausgegeben.

## Kurze Beispiele

Die Ausschnitte zeigen die Schreibweise an der zuständigen Fehler-/Erfolgsgrenze;
sie sind keine zusätzlichen Loggingpflichten für jeden Ablauf.

Nach einer erfolgreichen Profiländerung innerhalb einer Transaktion wird erst nach
Commit ein Erfolg gemeldet. Die interne ID vorher als unveränderlichen Wert erfassen,
keine Entity als verzögertes Argument übergeben:

```java
AfterCommitLog.info(log, "Account profile updated: accountId={}", accountId);
```

Gezieltes DEBUG kann erklären, warum eine konkrete Bearbeitung nicht übernommen wurde,
ohne Formulardaten auszugeben. Das ist kein Log bei jedem Laden oder Poll:

```java
log.debug("Profile update rejected: accountId={}, reason=VERSION_CONFLICT", accountId);
```

Ein technischer Fehler wird einmal an der verantwortlichen Grenze mit sicheren
Metadaten gemeldet. Die Originalexception weiter wie vorgesehen behandeln, nicht
durch die bereinigte Logkopie ersetzen:

```java
log.error("Profile update failed: accountId={}", accountId, LogSafety.safeFailure(failure));
```

Ausgeschöpfte Mailwiederholungen können als WARN gemeldet werden; jeder normale
Scheduler-Durchlauf oder dieselbe weiterbestehende Störung braucht keine neue Meldung.
Queueaufnahme bedeutet nur vorgemerkten Versand, SMTP-Annahme noch keine bestätigte
Posteingangszustellung. Logtexte müssen diese Unterschiede ausdrücklich erhalten.
