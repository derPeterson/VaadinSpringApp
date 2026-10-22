# HTTPS für die lokale Anwendung

Status: Technische Umsetzung einschließlich IDE-Komfortstart abgeschlossen und
gezielt geprüft. Neun Starterregressionen einschließlich vollständigem HTTPS-
Anwendungsstart und Prozessabbau sind bestanden. BAT-Start und IDE-Komfortstart
sind vom Benutzer abgenommen: Start, Stop, harter Reset, Debug mit Breakpoint und
HotSwap wurden in IntelliJ erfolgreich geprüft. Offen sind ausschließlich die unten
genannten Prüfungen und die gemeinsame Featureabnahme.
Branch: `feature/lokales-https`, Basis: `2ffcd4a5d7a1d13e73f94b273871afc6ba32f077` (lokales main).
Die drei vorgeschalteten Zertifikatsprüfungen sind erfolgreich: normales und
persistentes Playwright-Chromium mit echtem Prozessneustart und Secure-Cookie,
Caddy-Neustart sowie vollständiger Rückbau mit anschließender Zertifikatsablehnung.
Der früheste freigegebene Startzeitpunkt (21. Oktober 2026, 21:45 Uhr Europe/Berlin)
ist verstrichen. Der einmalige Zeitauftrag ist beendet. Der ausdrücklich beauftragte
lokale Merge nach `main` ist erfolgt; die Umsetzung bleibt auf Benutzerwunsch offen.

## Geltender Rahmen

- TLS an Caddy, intern HTTP; IDE, manuelles Docker und isolierte Browserworker.
- IDE: `https://localhost:8443/`, HTTP-Weiterleitung auf 8081; App auf Loopback 8080.
- Manuelles Docker: `https://127.0.0.1:18443/`, HTTP-Weiterleitung auf 18080;
  Appport nicht veröffentlicht. Unterschiedliche Hosts vermeiden Cookiekollisionen.
- Jeder Browserworker hat eigenen Proxy, Tageszertifikat samt Schlüssel, Daten,
  Browserprofile und drei Ports. Eine separate Browser-Test-CA wird einmalig bewusst
  im Windows-Benutzerkonto eingerichtet; normale Läufe verändern Trust nicht.
  Ihr privater Schlüssel bleibt geschützt auf dem Host außerhalb von Git und Containern.
  Keine TLS-Fehlerausnahmen. Manuelle Zugänge verwenden weiterhin ihre eigene CA.
- Konkretes Proxy-Vertrauen über native Tomcat-Verarbeitung; kein pauschales Vertrauen
  privater Netze. Lokale IDE-Prozesse sind untereinander nicht isoliert. Caddy ersetzt
  fremde Forwarded-Header und begrenzt die äußere Origin. Lokale Zugänge ohne HSTS.
- Frische isolierte Testdaten; keine Backfills oder Änderungen bestehender Daten.
  Gespeicherte Mailbasis hat weiterhin Vorrang und wird bei Bedarf bewusst angepasst.
- Keine LAN-/Firewallfreigabe, kein öffentliches Hosting, kein Designumbau.

Bedienung und Grenzen: [Entwicklung](../entwicklung.md#lokales-https),
[Docker](../docker/README.md), [Sicherheit](../sicherheit.md) und
[Browserrunner](../../browser-tests/README.md#isolation).

## Prüfstand und verbleibender Abschluss

Die große interaktive Runde ist dokumentiert. Der danach freigegebene Umbau für
unbeaufsichtigte Nachtläufe ist umgesetzt und gezielt geprüft: 75 Helfertests,
je 8 DE-/EN-Browserfälle mit zwei Workern und echtem Profil-/Browserneustart,
TLS-Negativtest und harter Workerabbruch. Windows-Vertrauen und dauerhafte
CA-Dateien blieben während der Läufe unverändert; eigene Worker-Schlüssel und
Compose-Ressourcen sind entfernt. Der frühere interaktive Runner-Timeout ist damit
gezielt nachgeprüft; die gesamte große Runde wurde nicht erneut ausgeführt.
Tatsächliche Ergebnisse und Grenzen: [Prüfstand](../status.md).

- [ ] Gemeinsame Gesamt-Abnahme des HTTPS-Features. Die bestehende menschliche
  IDE-/BAT-Abnahme bleibt gültig; keine erneute Pflichtprüfung dieses Komfortstarts.
- [ ] Die sieben verbliebenen Sonar-Findings aus Paket 2 bearbeiten und mit passendem
  Nachscan ein grünes Gate nachweisen. Paket 1 ist lokal integriert und nachgeprüft;
  Gate aktuell ERROR. [Paket 2](sonar-pakete.md) ist beauftragt und in Bearbeitung.
- [ ] Nach gemeinsamer Abnahme erledigten TODO-Eintrag, diesen Plan und Verweise
  entfernen. Offenen Testbedarf unabhängig erhalten. `docs/status.md` bleibt der
  Stand der großen QA einschließlich ihrer belegten Grenzen.
  Kein Planarchiv und kein zusätzlicher Abschlussbericht.
