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
ist verstrichen. Der einmalige Zeitauftrag ist beendet. Kein Commit/Merge/Push beauftragt.

## Geltender Rahmen

- TLS an Caddy, intern HTTP; IDE, manuelles Docker und isolierte Browserworker.
- IDE: `https://localhost:8443/`, HTTP-Weiterleitung auf 8081; App auf Loopback 8080.
- Manuelles Docker: `https://127.0.0.1:18443/`, HTTP-Weiterleitung auf 18080;
  Appport nicht veröffentlicht. Unterschiedliche Hosts vermeiden Cookiekollisionen.
- Jeder Browserworker hat eigenen Proxy, CA, Daten, Browserprofile und drei Ports.
  Windows-Benutzervertrauen wird temporär mit Eigentumsmarker und Zeitgrenze
  eingerichtet und entfernt. Keine TLS-Fehlerausnahmen. Dauerhaftes Vertrauen der
  manuellen Zugänge benötigt die bewusst ausgeführte Einrichtung durch den Benutzer.
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

- [x] Offene HTTPS-JUnit-/Helferregressionen ergänzt und erfolgreich geprüft:
  Produktionsbuild mit 2.241 bestandenen Anwendungstests (zwei optionale IDE-Skips)
  sowie 63 bestandene Browser-Helfertests. Tatsächlicher Stand: [Prüfbericht](../status.md).
- [x] Vollständige HTTPS-Browserfälle am selben unveränderten Image geprüft:
  DE 161/161 und EN 161/161 bestanden. Beim DE-Rückbau trat ein Windows-
  Bestätigungs-Timeout auf; der Elternrunner hat ihn erfolgreich bereinigt.
  EN einschließlich Abbau bestanden. Alle eigenen Testressourcen sind entfernt.
- [ ] Aktuellen Sonar-Nachweis nach ausdrücklich beauftragter Integration auf
  sauberem `main` erbringen. Die vorhandene grüne Analyse belegt den HTTPS-Stand nicht.
- [ ] Nach gemeinsamer Abnahme erledigten TODO-Eintrag, diesen Plan und Verweise
  entfernen. Offenen Testbedarf unabhängig erhalten. `docs/status.md` bleibt der
  Stand der großen QA einschließlich ihrer belegten Grenzen.
  Kein Planarchiv und kein zusätzlicher Abschlussbericht.
