# Logging

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
