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

## Noch offen

- [ ] Vollständige HTTPS-Browserabnahme DE/EN nach gesonderter Freigabe am selben
  unveränderten Image: bestehende Login-, Mail-Link-, CSRF-, Rechte-, Sitzungs-,
  Remember-me-, Neustart-, Upload- und Push-Journeys wiederverwenden.
  Automatisierte Restarbeiten stehen ausschließlich im [Testbedarf](../testbedarf.md).
- [ ] Verbleibende manuelle Einrichtung abnehmen: Docker-Zugang sowie Rückbau des
  Zertifikatsvertrauens prüfen. Der lokale HTTPS-Start über die BAT-Dateien ist vom
  Benutzer als funktionierend bestätigt; dies ist keine Gesamtfreigabe des Features.
- [ ] Nach gemeinsamer Abnahme erledigten TODO-Eintrag, diesen Plan und Verweise
  entfernen. Offenen Testbedarf unabhängig erhalten. `docs/status.md` bleibt der
  Stand der letzten großen QA und wird durch diese Einzelprüfungen nicht ersetzt.
  Kein Planarchiv und kein zusätzlicher Abschlussbericht.
