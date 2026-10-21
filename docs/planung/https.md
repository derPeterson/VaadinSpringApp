# HTTPS für die lokale Anwendung

Status: beschlossen, noch nicht umgesetzt. Umsetzung nach den Codex-Arbeitsabläufen.
Dieses Dokument ist ein befristeter Arbeitsauftrag und wird bei Abschluss gelöscht.

## Vereinbarter Rahmen

- Die Anwendung soll über einen vertrauenswürdigen HTTPS-Zugang verwendbar sein.
- TLS über einen vorgeschalteten Reverse Proxy umsetzen; Caddy ist die vorgesehene
  Referenzlösung. Die Anwendung kann dahinter intern HTTP verwenden.
- IDE-Start, lokale Docker-Anwendung und isolierte Browserumgebung berücksichtigen.
- Vorhandene Fachfunktionen und Sicherheitsverträge erhalten. Kein Designumbau und
  keine Daten-/Schemaübernahme; geprüft wird mit frischen eigenen Testdaten.
- Öffentliches Hosting ist kein Teil dieser Aufgabe. Der Aufbau soll eine spätere
  eigene Domain und öffentlich vertrauenswürdige Zertifikate ermöglichen.

Diese Entscheidungen gelten als Arbeitsgrundlage. Hostnamen, Ports und konkrete
Dateipfade sind Umsetzungsdetails; sie werden passend zu den vorhandenen Umgebungen gewählt.

## 1. Zugangs- und Zertifikatskonzept festlegen

- [ ] Eine eindeutige äußere HTTPS-Adresse je lokaler Umgebung bestimmen. Interne
  Appadresse, Browseradresse und Mail-Link-Basis sauber unterscheiden.
- [ ] Lokale Zertifikatsausstellung, Ablage, Erneuerung und Vertrauen auf Windows,
  dem tatsächlich verwendeten Browser und gegebenenfalls dem Laptop einrichten.
  Zertifikatsvertrauen innerhalb von Containern ist nicht automatisch Hostvertrauen.
- [ ] Private Schlüssel und lokale Zertifikatsdaten aus Git, Images und Logs heraushalten;
  notwendige Proxy-Daten bei normalen Neustarts erhalten.
- [ ] Dokumentieren, wie der Proxy mit der IDE-App und mit der Docker-App verbunden wird.
  Nur die vorgesehenen lokalen Ports veröffentlichen; interne Appports nicht versehentlich
  als ungeschützten alternativen Zugang im Netzwerk anbieten.

## 2. Proxy und Anwendung gemeinsam konfigurieren

- [ ] HTTPS-Einstieg, HTTP-Weiterleitung und Erreichbarkeitsprüfung einrichten. Unsichere
  HTTP-Anfragen dürfen keine Fachaktionen ausführen; Loginformulare und Linkziele verwenden HTTPS.
- [ ] Ursprünglichen Host, Port und HTTPS-Schema korrekt an Spring/Tomcat weitergeben.
  Forwarded-Header nur aus dem vorgesehenen Proxy-Vertrauensbereich berücksichtigen;
  vom Client eingeschleuste Header dürfen den Zugangsschutz nicht umgehen.
- [ ] Alle relevanten URL-Erzeuger auf die äußere Adresse abstimmen: Navigation,
  Weiterleitungen, Verifikation, Passwortreset, E-Mail-Änderung und Konfigurationsvorgaben.
  Bei `ConfigService` den Vorrang bereits gespeicherter Adminwerte beachten.
- [ ] Session- und Remember-me-Cookies unter HTTPS mit passenden Secure-, HttpOnly-,
  SameSite- und Pfadregeln prüfen. Anmeldung, Abmeldung und Widerruf müssen weiter funktionieren.
- [ ] Vaadin Flow einschließlich UIDL, Upload und Push/WebSocket über den Proxy testen;
  sichere Verbindungen verwenden. Keine pauschalen CSRF-Ausnahmen oder schwächeren
  Originprüfungen als HTTPS-„Workaround“ einführen.
- [ ] Ressourcen, Bilder und Browseraufrufe auf gemischte HTTP-/HTTPS-Inhalte prüfen.
  H2-Konsole und Mailpit behalten ihren bewusst lokalen Entwicklungsrahmen und werden
  nicht durch die Proxy-Einrichtung öffentlich freigegeben.
- [ ] HSTS für die gewählten Hostprofile bewusst festlegen. Keine ungeprüfte dauerhafte
  Preload-/Subdomain-Bindung für wechselnde lokale Entwicklungsadressen einführen.

## 3. Reproduzierbare Testumgebung ergänzen

- [ ] Proxy-/Zertifikatskonfiguration in die eigene Docker-Testumgebung integrieren;
  Healthchecks und Startreihenfolge auf die äußere Erreichbarkeit abstimmen.
- [ ] Browserrunner um den HTTPS-Einstieg und korrektes Vertrauen in die eigene Test-CA
  ergänzen. Die Abnahme darf Zertifikatsfehler nicht pauschal ignorieren.
- [ ] Frische isolierte Datenbanken und eigenes Mailpit nutzen. Keine Entwicklerdaten,
  fremden Container, realen Postfächer oder produktiven Dienste als Fixture verwenden.
- [ ] Dasselbe optimierte Anwendungsimage für DE und EN verwenden. Während laufender
  Neustartfälle keine Images neu bauen, umhängen oder entfernen.

## Abnahme und Regressionen

- [ ] HTTPS funktioniert ohne Zertifikatswarnung auf den vereinbarten Clients;
  HTTP-Weiterleitung und Schutz vor unsicheren Fachaktionen sind nachgewiesen.
- [ ] Gezielte technische Tests für vertrauenswürdige/gefälschte Proxy-Header,
  Weiterleitungsziele, Cookieattribute, CSRF und erzeugte HTTPS-Mail-Links ergänzen.
- [ ] Java 25 sowie vorgeschriebene Stilprüfungen, vollständige JUnit-/Helferprüfungen
  und optimierten Build erfolgreich ausführen; betroffene Frontend-/Werkzeugtests berücksichtigen.
- [ ] Nach ausdrücklichem Auftrag für die HTTPS-Browserabnahme vollständige DE-/EN-Runden
  am selben Image durchführen: beide Loginwege, Logout, Remember-me, Sessionlimit,
  Verwaltung/Widerruf, Serverneustart, Wartung, Tokenabläufe, Upload und Push.
  Diese Plandatei selbst ist noch kein Auftrag zum Starten von Browserläufen.
- [ ] Aufgefangene Verifikations-/Reset-/E-Mail-Änderungslinks verwenden die richtige
  äußere HTTPS-Adresse und funktionieren über die Oberfläche. Keine echten Mails nötig.
- [ ] Fehler fachlich beheben und erforderliche Nachtests durchführen. Ergebnisse,
  tatsächlich geprüfte Clients und verbleibende Grenzen ehrlich dokumentieren.

## Pflichtabschluss: Plan entfernen

- [ ] Den tatsächlich eingerichteten Betrieb in den vorhandenen Dokumenten
  `docs/entwicklung.md`, `docs/docker/README.md`, `docs/sicherheit.md` und der
  Browser-Testanleitung beschreiben; Einstieg/Beispieladressen konsistent aktualisieren.
  Nur aktuelle Bedienung, Voraussetzungen und verbleibende Aufgaben dokumentieren.
- [ ] Prüfstand in `docs/status.md` ersetzen, erledigten TODO-Eintrag und Planverweis
  im Dokumentationseinstieg entfernen.
- [ ] **Diese Plandatei im selben Arbeitsstand löschen. Kein Abschluss-/Planarchiv
  anlegen.** Verweise prüfen und einen danach leeren Ordner `docs/planung/` entfernen.
  Die separate Design-Spielwiese bleibt die nachfolgende Aufgabe.

Referenzen: [Caddy HTTPS und lokales Vertrauen](https://caddyserver.com/docs/automatic-https),
[Spring Boot hinter einem Proxy](https://docs.spring.io/spring-boot/how-to/webserver.html#howto.webserver.use-behind-a-proxy-server).
