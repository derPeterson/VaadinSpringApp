# Prüfstand

## Große Testrunde vom 20.11.2026

Geprüft wurde der saubere Release-Kandidat `05814927` auf `main`.
Die vorherige vollständig erfolgreiche Testrunde lief auf `486a54c0`.

| Prüfung | Ergebnis |
| --- | --- |
| Root-Reactor mit Produktionsbuild und Coverage | Bestanden |
| Anwendungs-JUnit | 3.270 bestanden, zwei optionale IDE-/Caddy-HTTPS-Prüfungen übersprungen |
| Browser-Helfertests | 143 bestanden |
| Frontend-Offlineprüfungen | 23 bestanden |
| Coverage-, Sonar- und Release-Helfer | 18, 49 und 14 bestanden |
| Lokale Zeilenabdeckung | Anwendung 86,10 %, Shared 100 %. Gemeinsame Coverage-Prüfung bestanden. |
| Java- und CSS-Stilprüfungen | Spotless, Checkstyle, Prettier und Stylelint bestanden |
| Browser Deutsch, gesamte Suite | 389 von 389 bestanden, keine Fehler oder Skips |
| Browser Englisch, gesamte Suite | 389 von 389 bestanden, keine Fehler oder Skips |
| Aufbau, Abbau und HTML-Bericht | In beiden Browserläufen bestanden |

Beide Browserläufe verwendeten je vier isolierte Worker und dasselbe
unveränderte Anwendungsimage
`sha256:8ea12c5422a66073b3827eda12b9a1edaeae1093a75d4e0b25644f4a377b7198`.

## Geprüfte Änderungen

- Die Größenänderung in den Mailformularen und im Mitteilungsentwurf
  einschließlich der Folgeschritte in MAIL-02, MAIL-03 und NOTIFY-01 ist
  in beiden Sprachen bestätigt. Die SMTP-Testkonfiguration wird nach dem
  Wiederherstellen anhand der gespeicherten Werte geprüft.
- Im Vorgangsdetail sind die Aktionen auch bei 320 px erreichbar. Die Tests
  decken Rückkehr und Fokus, Entwürfe auf einer gefilterten Folgeseite,
  Größenänderung und Scrollen im Antwortfeld sowie die Pflichtfeldprüfung ab.
- Die Empfängervorschau schließt gesperrte, deaktivierte und unbestätigte
  Konten aus. Der Versand weist zwischenzeitlich ungültig gewordene Auswahlen zurück. Die
  Empfängerliste, der Rückfokus und die Inbox-Aktionen sind geprüft.
- Mobile Kontokarten und Admin-Benachrichtigungsaktionen wurden bei 390 und
  320 px geprüft. Die Anbieterlogos halten den vorgesehenen Abstand zum
  Text ein. Beim Verlassen des Profilbilddialogs trat der behobene
  JavaScript-Fehler im Nachtest nicht mehr auf.

## Offene Nachweise

Weitere automatisierte Szenarien stehen im [Testbedarf](testbedarf.md).
Reale Anbieter, echter SMTP-Versand und menschliche Sonderprüfungen
stehen im [manuellen Katalog](manual-tests/README.md).

## Sonar

Die geschützte Analyse `e93e1381-ae45-4026-9a37-20f6e4f5949f` für
`05814927` meldet Quality Gate OK, null offene Issues, null offene Hotspots
und 45 bestehende Ausnahmen. Sonar weist 83,8 % Gesamtabdeckung, 86,8 %
Zeilenabdeckung und 74,6 % Zweigabdeckung aus.
