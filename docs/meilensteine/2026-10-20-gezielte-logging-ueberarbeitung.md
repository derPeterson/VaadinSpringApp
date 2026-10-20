# Gezielte Logging-Überarbeitung vom 20.10.2026

## Ausgangspunkt und Reihenfolge

Branch `feature/user-messages`, Basiscommit
`11526c964a4f8dd7863a082ede6f052e9e498606` plus die uncommitteten QA-Erweiterungen.
Die [vollständige QA](2026-10-20-gesamtqa-und-benachrichtigungsredesign.md) wurde
zuerst abgeschlossen: DE/EN je 152 eindeutige Browserfälle erfolgreich am identischen
Produktionsimage, 1823 Anwendungs-JUnits und ergänzende Offlineprüfungen erfolgreich.
Erst danach begann die separat beauftragte Logging-Überarbeitung.

## Änderung

Der bereitgestellte Auszug enthielt 348 `Retrieved SecurityContextImpl`-Zeilen bei
666 Zeilen insgesamt. Ursache war die DEBUG-Freigabe des Anwendungspakets: Der
geerbte Spring-Security-Logger verwendet den Namen der eigenen Unterklasse.
Normalbetrieb ist jetzt INFO. Die beiden konkreten geerbten Context-/Remember-me-
Logger bleiben ausdrücklich INFO, auch bei Diagnose-DEBUG am Elternpaket.
Die vorhandene Logrotation bleibt erhalten.

Wichtige fachliche Ergebnisse, Mailzustände und behandelbare Störungen verwenden
gezielt INFO/DEBUG/WARN/ERROR. Erfolgsmeldungen aus Transaktionen folgen dem Commit;
kein Erfolg nach Rollback oder Commitfehler. Einreihen einer Mail und Annahme durch
den Transport sind ausdrücklich unterschiedliche Aussagen. Startmeldungen für jeden
bereits vorhandenen Konfigurationseintrag sind durch eine Zusammenfassung ersetzt.
Anwendungslogs enthalten interne IDs/Anzahlen/Typen statt Adressen, Inhalten oder
Formularwerten. Sichere Exceptionkopien erhalten Typnamen, Stackframes und Ursachen;
freie Exceptiontexte mit möglichen SQL-/Mail-/Tokeninhalten werden nicht ausgegeben.
Originale Exceptions und die bestehende fachliche Fehlerbehandlung bleiben erhalten.

Betroffene Bereiche: Start/Konfiguration, Benutzer-/Rollenverwaltung, Konto/Avatar,
Kontosicherheit, Verifikation/Reset, Benachrichtigungen, Loginversuchssperre,
Sitzungszulassung/-widerruf, Mailqueue/Worker und Bereinigungen. Häufige Abrufe,
Aktivitätsupdates und unveränderte Zustände erhalten keine zusätzlichen Meldungen.
Regeln für zukünftige Arbeiten stehen in AGENTS.md und [Logging](../logging.md).

## Beobachtete technische Nachweise

- 356 gezielte Tests erfolgreich, keine Fehler/Fehlschläge/Skips.
- Anschließend alle **1843 Anwendungs-JUnit-Tests** erfolgreich, keine Fehler/Skips;
  20 zusätzliche Ausprägungen gegenüber dem QA-Ausgangspunkt.
- Echte H2-Transaktionen prüfen Rollback, Commit, idempotenten Versand und fehlende
  private Logwerte. Weitere Tests prüfen Commitfehler, begrenzte/zyklische Ursachen,
  weiterhin sichtbare WARN/ERROR sowie geschützte Security-Pegel bei Paket-DEBUG.
- Die vorhandenen UI-Fehlerassertions verlangen nun sicheren Originaltyp statt
  unverändertem vertraulichem Exceptiontext. Mailtests behalten Versuchs-/Retry-
  und fachliche Versandverträge bei; die neue Ausgabe nennt Queue-ID statt Adresse.
- Eine unbeabsichtigte Änderung des Mailtest-Fixturetyps wurde im gezielten Vorlauf
  erkannt und zurückgenommen; der ursprüngliche Standardtyp NOTIFICATION bleibt.
- JUnit-JaCoCo: **10003/11439 Zeilen (87,45 %)**,
  **2805/3706 Zweige (75,69 %)**, 214 ausführbare Anwendungsklassen.

Produktionsbuild erfolgreich. Anschließend `notifications` DE und EN je **8/8**
erfolgreich, ohne Fehler/Skips. Setup, Coverage, Berichte und Bereinigung erfolgreich.
Beide Nachrunden verwenden das identische neue Produktionsimage
`sha256:d5f831296d95628bf92c32feaea2ecf296ae5224491f3a39aec6c502d0394ba2`.
Beide extrahierten Produktions-JARs sind bytegleich (SHA-256):
`09a00088780beb8a085e7f8b0e3a0864aaf80d526bcff624a312e18527c0a6e7`.

Die vollständig erfassten Testserverlogs beider Sprachen enthalten jeweils null
`Retrieved SecurityContext`-, Principal- oder Credentials-Dumps, null geprüfte
Testadressen und null Bootstrap-Testkennwörter. Pro Server erscheint genau eine
Konfigurations-Startzusammenfassung. Die neuen Versand- und Sammellöschereignisse
sind sichtbar (je 19 bzw. 4 Meldungen) und nennen IDs/Anzahlen statt Inhalte.
Das ist ein konkreter Laufnachweis für die geprüften Muster, kein Nachweis für
jede denkbare Fehler-/Bibliotheksausgabe.

292 relevante Java-/Ressourcendateien wurden zwischen aktuellem Quellstand und
JUnit-Kopie per SHA-256 abgeglichen; die POMs sind unverändert. Spotless, Checkstyle
und `git diff --check` erfolgreich. Es gibt keine ungeklärten Testfehlschläge.
Browser-Coverage wurde auch für die Nachrunden erzeugt; diese acht Fälle pro Sprache
bilden eine gezielte Teilmenge. Die JUnit- und Browsermessungen werden nicht gemittelt.
Die ursprünglichen 152er-Runden gelten für den Stand vor dieser Logging-Änderung.
Der Nachtest ersetzt keine neue vollständige 152er-Abnahme des geänderten Images.

## Einführung

Neu bauen und die Anwendung vollständig neu starten bzw. das neue Produktionsimage
starten. Ein Browser-Refresh reicht nicht. Die laufende Entwicklungsanwendung und
reale Daten wurden für diese Arbeiten nicht verändert. Kein Commit oder Push.
