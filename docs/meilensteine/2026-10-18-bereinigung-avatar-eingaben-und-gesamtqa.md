# Bereinigung, Profilbilder und Eingaben vollständig geprüft

18.10.2026. Ausgangspunkt: sauberer Branch `main`, Commit
`85712fee0ba1246784f815a185075db0525ad6f7`. Ausführung nach dem freigegebenen Termin
05:30 Europe/Berlin; Änderungen bleiben uncommittet auf `main`.

## Ergebnis

| Prüfung | Tatsächlicher abschließender Nachweis |
| --- | --- |
| Java / Maven / Shared | Java 25.0.4.1, Wrapper 3.9.16, Shared `clean install` erfolgreich |
| Anwendungs-JUnit | 1524/1524, 0 Fehler/Fehlschläge/Skips |
| Browserhelfer / Reporter | 47/47, 0 Fehler/Fehlschläge/Skips |
| Python / JavaScript offline | 205/205 und 26/26, keine Fehler/Skips, keine Liveprovider-Anfragen |
| Produktionsbuild | Frisches Docker-Frontend/JAR erfolgreich gebaut und in eigenen Teststacks gestartet |
| Browser `all` DE / EN | Je 141/141, 26 Klassen; identische Fall-/Parameteridentitäten, keine Fehler/Skips |
| Parallelisierung | Je zwei isolierte JVM-/Docker-/H2-/Mailpit-Worker, 59/59 und 82/82 |
| Berichte / Abbau | Aufbau, HTML, Coverage und Abbau in beiden abschließenden Läufen erfolgreich |
| Format / Review | Spotless Apply/Check und Git-Diffprüfung; keine Python-Dateien geändert |

DE endete am 18.10. um 06:58:28, EN um 07:21:31 Europe/Berlin.
Beide prüfen exakt das Produktionsimage
`sha256:13d46c5afc56f276746f9431b2047115041c800c147546fcae4ee98dd872818f`.
Alle 34 Fast-Fälle sind enthalten; es wurde kein redundanter Fast-Lauf ausgeführt.

| Getrennte JaCoCo-Messung | Zeilen | Zweige |
| --- | ---: | ---: |
| Anwendungs-JUnit | 7735/9113 (84,88 %) | 2165/2987 (72,48 %) |
| Browser DE | 7769/9113 (85,25 %) | 1910/2987 (63,94 %) |
| Browser EN | 7780/9113 (85,37 %) | 1914/2987 (64,08 %) |

174 ausführbare Anwendungs-Java-Klassen; 210 Klassen im XML einschließlich Klassen
ohne ausführbare Zeilen. Shared, Bibliotheken und Testcode sind nicht enthalten.
Browserdaten werden je Sprache aus beiden Workern gegen das exakt getestete JAR
zusammengeführt. Start/Hintergrundjobs zählen mit; keine Mittelwerte, keine Vermischung
mit früheren Runs. Das ist keine CSS-/JavaScript- oder vollständige Anforderungsabdeckung.
Die JUnit-Java-Messung folgt dem letzten Java-Fix; die anschließenden CSS-Änderungen
verändern diesen Java-Stand nicht.

## Bereinigungen und bestätigte Korrekturen

- Unbenutzten `JdbcHelper`, Tooltip-Übersetzungsmethode und privaten Browser-Testhelfer entfernt.
- Drei verwaiste eigene CSS-Klassen mit vier Regeln entfernt; native Grid-/Lumo-Regeln und dynamische Status-Glows erhalten.
- Vier alte Sitzungs-Demo-Getter und je vier Sprachschlüssel entfernt: 451 eindeutige Schlüssel pro Sprache statt 455, Sprachparität und Codec erhalten.
- Deutsche kompakte Datumsanzeige auf `dd.MM.yyyy` korrigiert; normale kurze/getrennte Eingaben, 1950–2049, Locale und Bereichsprüfung bleiben erhalten.
- Abgeschnittene Abmeldung bei 320×568 behoben: Popoverinhalt schrumpft/scrollt innerhalb der nativen verfügbaren Overlayhöhe.
- Veraltete Browserselektoren, einen fehlenden Navigationsschritt und native Animationswartebedingungen gezielt korrigiert; keine fachlichen Assertions abgeschwächt.

Nachweise zur Nichtverwendung und bewusst erhaltenen Framework-/Autorenressourcen:
[Bereinigungsbericht](../codebereinigung-2026-10-18.md).

## Neue sichtbare Journeys

AVATAR-01 läuft getrennt auf Desktop und 390×844: echtes PNG/JPEG, Ausschnitt per
Zoom/Reset, kompakte Uploadfläche, gleiche lokale Aktionsgrößen, Speichern/Reload,
Wiederöffnen, lokaler und Footer-Abbruch, Initialen/Bild/Ersetzen/Löschen sowie Bilder
in Kopfzeile, Popup, Konto und Benutzerverwaltung. Namensänderungen aktualisieren
Initialen. STATUS-03 ergänzt direkte Kontostatuswahl per Maus/Tastatur, Kontursymbole,
aktive Fläche ohne Zusatzhaken und synchronisierte Statusrahmen/Glows.

INPUT-01/02 prüfen vier/sechs/acht Datumsziffern, DE-/EN-Reihenfolge, Grenzjahre,
kurze getrennte Jahre, ungültige Eingaben, Tab/Enter sowie 930/1430 und den echten
Sprachwechsel im geladenen Frontend. CONFIG-05F prüft nun auch mobile Statusfläche,
horizontalen Popupüberlauf, tatsächliches Scrollen und Abmelden.
Die bisherigen Passwort-/E-Mail-/Sitzungs-, Admin-/Zugriffs-/Wartungs- und Mailjourneys
bleiben in beiden vollständigen Runden enthalten. Technische Detail-/Konkurrenzregeln
gehören in JUnit/Offlinetests; Browserzustände stammen aus UI und eigenen SMTP-Mails.

## Einordnung der Zwischenversuche

Die erste vollständige DE-Runde endete mit 139/141 vor dem Popup-Fix und der Korrektur
des neuen Zeit-Testaufbaus. Gezielte Vorprüfungen danach: Konfiguration 9/9, Cosmos 11/11,
Filter 5/5 und Konto 11/11. Diese verwendeten den Popup-Fix vor der letzten Entfernung
wirkungsloser CSS-Regeln; die beiden abschließenden grünen Gesamtrunden prüfen das
endgültige Image oben. Ein identischer fehlerfreier Gesamtstand wurde nicht grundlos
nochmals getestet. Zwei frühe wegen gemeinsamer Selektoren abgebrochene Anläufe zählen
nicht als vollständige Nachweise. Ein System-Python-Aufruf hatte 145 gefundene Tests
mit 6 Importfehlern; die korrekte Projektumgebung besteht vollständig mit 205 Tests.
Alte Runs und deren Diagnosen bleiben erhalten.

## Grenzen und Übernahme

Chromium 153.0.8010.12 und simulierte Fenstergrößen; keine Realgeräte-/Touch-/Screenreader-
oder Mailclientabnahme. Externe SMTP-Provider/Auth/TLS, produktive Bestandsmigrationen,
Clusterbetrieb, umfassende Netzunterbrechungs-/Ladezustände und ein gezielter Avatar-
Anwendungsneustart mit zuvor hochgeladenem Bild bleiben offen. H2-Persistenz und
Reload-Journeys sind geprüft. Die 15 Fälle im [manuellen Katalog](../manual-tests/README.md)
bleiben separat; 141/141 ersetzt keine menschliche Bewertung.

Nur eigene frische Umgebungen/Daten verwendet und alle eigenen Stacks abgebaut;
abschließende Containerkontrolle leer. Keine Nutzer-/IntelliJ-/Vite-Prozesse beendet,
keine alten Runs gelöscht und keine Bestandsdatenbank oder realen Postfächer benutzt.
Im Projekt nichts gestagt, committet, gemergt oder gepusht. Das Produktions-JAR wurde
in Docker geprüft; die reguläre IDE-App wurde nicht gestartet/gestoppt. Für die lokale
Übernahme Shared/App bauen, einmal neu starten und Browser aktualisieren; HotSwap-
Übernahme ist kein gesondert geprüfter Nachweis.

Detaillierte aktuelle Matrix: [Browserabdeckung](../../browser-tests/abdeckung.md).
Die optionale vollständige Designkritik wurde **nicht ausgeführt**. Die frische
Nutzungslimitabfrage nach Abschluss der QA meldete das Fünf-Stunden-Kontingent als
100 % verbraucht; damit war kein ausreichendes reguläres Kontingent für eine sinnvoll
abschließbare Zusatzanalyse vorhanden. Kein Reset und keine zusätzliche Buchung wurden
ausgelöst. Die Designkritik bleibt ein ausdrücklich offener separater Punkt; die
funktionalen QA-Ergebnisse oben sind vollständig abgeschlossen.
