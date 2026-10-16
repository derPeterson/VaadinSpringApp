# Kontoprofil, E-Mail-Dialog und Neustartprüfungen

Prüfung begonnen am 16.10.2026, abgeschlossen am 17.10.2026 auf `feature/account-settings-ui`, Basis
`0956ff62325e83d5fb786053f2285861d4e45432`. Die hier beschriebenen Ergänzungen
liegen uncommittet vor. Die bestehenden Konto- und Cosmos-Designvorgaben bleiben
unverändert; ein funktionaler E-Mail-Wechsel gehört weiterhin zum nächsten Schritt.

## Änderungen und Befunde

Sieben neue Account-Browserfälle prüfen Profiländerung mit Neuladen und zweitem
Speichern, ungültige Pflichtfelder/Geburtstage ohne Persistierung, die gespeicherte
Sprache nach neuer Anmeldung und die beiden E-Mail-Einstiege. Die Dialogfälle prüfen
Icon, Abbrechen, Escape und Außenklick, jeweils mit leerer Wiederöffnung und
unverändertem Profilentwurf. Geprüft werden auch übersetzte Texte, deaktivierte
E-Mail, Bestätigungsicon und der berechnete Backdrop-Blur.

Dabei wurde ein echter Fehler im E-Mail-Dialog behoben: Escape konnte vor der
Wertsynchronisierung schließen, sodass ein Passwortentwurf später wieder erschien.
Alle drei Eingaben synchronisieren jetzt unmittelbar; Schließen und erneutes Öffnen
verwerfen die Entwürfe. Eine zusätzliche JUnit-Regression prüft auch verspätet
synchronisierte Werte. Es wurde keine Mail-/Kontoänderungsfunktion vorgezogen.

Der Sprachtest enthielt nach bestandener fachlicher Prüfung eine zusätzliche
Menübereinigung. Diese scheiterte beim sofortigen Escape nach Wiederanmeldung;
die gemeinsame Fixture bereinigt den Kontext bereits. Der doppelte Schritt entfällt.
Die Speicherung und Sprachübernahme bleiben unverändert streng geprüft.

In der englischen Runde zeigte USER-05C einen zeitabhängigen Helferfehler: Die
virtuelle Zeitliste behält einen unsichtbaren Mitternachtseintrag im DOM. Der Helfer
leitete daraus am späten Abend die falsche Scrollrichtung ab. Maßgeblich ist jetzt
die erste im Scrollfenster tatsächlich sichtbare Zeile. Die Assertion auf echte
Scrollbewegung und die anschließende Filterwirkung bleiben erhalten. Der bestehende
Fall navigiert zusätzlich zum Tagesende und zurück, damit beide Richtungen unabhängig
von der Uhrzeit geprüft werden. Die vollständige Filter-Suite bestand danach DE/EN
jeweils 4/4, einschließlich dieses verschärften Falls.

AUTH-07C/D ergänzen echte Serverneustarts mit zwei offenen Tabs. Zusammen mit
AUTH-07A/B werden echte Browser- und Serverneustarts mit/ohne Remember-me sowie
Logout geprüft. Nur die eigene Testanwendung wird neu gestartet. Die JaCoCo-Daten
innerhalb ihres frischen Volumes bleiben über den Neustart erhalten (`append=true`).

## Tatsächlich bestätigter Prüfstand

| Prüfung | Ergebnis |
| --- | --- |
| Java / Shared | Java 25.0.4.1; Shared gebaut und installiert |
| Vollständige Anwendungs-JUnit-Suite | 1298/1298, keine Fehler oder Skips |
| Browserhelfer / Reporter / Workeraufteilung | 45/45, keine Fehler oder Skips |
| Python offline | 205/205, keine Live-Provider- oder Modellanfragen |
| JavaScript offline | 14/14, keine Fehler oder Skips |
| Produktionsbuild | Erfolgreich einschließlich Tests und Produktionsfrontend |
| Browser DE, vollständiges all | 126/127; ein Fehler ausschließlich in doppelter Testbereinigung |
| Browser DE, betroffene Suite nach Korrektur | account-settings 7/7 |
| Filter-Nachtests am endgültigen Helfer, DE / EN | Je 4/4; keine Fehler oder Skips |
| Browser EN, vollständiges all | 126/127; ausschließlich der korrigierte Zeitlistenhelfer |
| Unterschiedliche erfolgreiche Fallidentitäten | Je alle 127 aus all; DE/EN identisch, keine Doppelungen |
| Formatierung | Google Java Format und Ruff-Formatprüfung erfolgreich |

Alle fünf abschließend ausgewerteten Browserläufe verwenden dasselbe Produktionsimage
`sha256:e169c5bd7880aeef8f1199d2ef4e8642a04ea11beb1cfbd2c4d4aa389cc4d4f5`.
Die Wiederholungen werden nicht als zusätzliche unterschiedliche Fälle gezählt. Der
Nachweis je Sprache besteht aus Gesamtrunde und gezielten erfolgreichen Nachtests;
es wird kein vollständig grüner all-Einzellauf behauptet. Die Anwendung wurde zwischen
diesen Nachweisen nicht geändert. Aufbau, Abbau, Berichte und Coverage waren bei
allen fünf ausgewerteten Läufen erfolgreich.
Die Auswahl `fast` bleibt bei 34 Fällen und wurde nicht separat erneut ausgeführt.

## Frische Coverage und Messgrenzen

JUnit: **5289/6920 Zeilen (76,43 %) und 1432/2071 Zweige (69,15 %)**.
Die Messung folgt einem vollständigen sauberen Testlauf. Der JaCoCo-Klassenzähler
umfasst 127 Anwendungs-Java-Klassen mit ausführbarem Code. Shared-JAR und
Drittbibliotheken gehören nicht zu diesem Reportumfang.

Browser DE und EN jeweils **5894/6920 Zeilen (85,17 %) und 1362/2071 Zweige (65,77 %)**.
Je Sprache wurden Gesamtrunde und erfolgreiche Nachtests gegen die bytegleich
verglichenen Produktionsklassen zusammengeführt. Enthalten sind auch die ausgeführten
Abschnitte vor den jeweiligen Helferfehlern; diese Messung behauptet keine fehlerfreien
Gesamteinzelrunden. Serverseitige Browser-Coverage enthält Start und Hintergrundjobs;
sie ist keine CSS-/JavaScript-Coverage und kein
prozentualer Nachweis vollständig abgedeckter fachlicher Anforderungen. Berechneter
Blur und automatisierte Chromium-Abläufe ersetzen keine Abnahme auf echten Geräten,
Screenreadern oder verschiedenen Browsern.

## Neustartbefund und Betriebsgrenzen

Der gemeldete Remember-me-Verlust beim IntelliJ-Neustart ist **nicht reproduziert
und nicht als behoben bewertet**. Produktionsneustarts in den isolierten Browserfällen
funktionieren mit Remember-me. Das lokale Sitzungsregister wird beim vollständigen
Serverneustart neu aufgebaut; DevTools-/IDE-Sitzungswiederherstellung ist ein eigener
Fall. [Analyse und Grenzen](../login-restart-analysis.md).

Überlappende vollständige Browserrunden führten auf Windows zu nativer
Netzwerkerschöpfung (`ERR_NO_BUFFER_SPACE` beim Laden von JavaScript). Die eigenen
betroffenen Läufe wurden kontrolliert abgebrochen und ihre Umgebungen abgebaut.
Anschließend wurden die Sprachrunden nacheinander ausgeführt, mit jeweils höchstens
zwei isolierten Workern. Keine Assertion wurde zur Umgehung abgeschwächt und kein
globaler Timeout erhöht. Abgebrochene Läufe zählen nicht als erfolgreicher Nachweis.

Es wurden ausschließlich frische eigene Docker-/H2-/Mailpit-Umgebungen verwendet;
Konten und Mailzustände entstanden über UI und eigene abgefangene Testmails. Die
reguläre Anwendung, ihre Datenbank, echte Zugangsdaten/Postfächer und fremde Docker-
Umgebungen blieben unverändert. Alte Run-Verzeichnisse wurden nicht gelöscht.
Die dauerhafte Dokumentation benötigt keine lokalen Runs, Traces oder Screenshots.

Für die Java-Änderung die Anwendung in der IDE neu starten, danach Strg+F5.
Ein gepacktes Produktionsartefakt muss für die Übernahme neu gebaut werden.
