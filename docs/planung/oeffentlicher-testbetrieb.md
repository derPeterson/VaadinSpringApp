# Öffentlicher Testbetrieb für Freunde und Bekannte

## Ziel und Empfehlung

Planung aus [Nachtpunkt 4](nachtaufgaben.md), noch keine Bereitstellung oder Buchung.
Freunde sollen sich regulär registrieren, eine echte Bestätigungsmail erhalten und
die vorhandenen Anmeldewege benutzen. Keine gemeinsam verwendeten Demokonten.

Empfehlung zum Einstieg: **eine kleine Hetzner-Cloud-VM in Deutschland, Docker mit
Caddy, eine eigene Domain, externer SMTP-Versand und gehostete CI**. Das passt zur
vorhandenen Anwendung und vermittelt Betriebserfahrung. Ein realistisch angesetztes
Budget liegt eher bei **rund 10–12 Euro monatlich** einschließlich Domain und
einfacher Sicherung als sicher unter fünf Euro. Ohne Zusatzdienste und mit günstiger
Domain kann es etwas darunter liegen. Kapazität und tatsächlichen Bestellpreis vor
einer Buchung erneut prüfen; es wird heute kein Anbieter festgelegt.

## Konkreter Projektstand und Ressourcen

Auswertung am aktuellen Quellstand, ohne bestehende Datenbank, Zugangsdaten oder
Anwendungslogs zu lesen:

| Gegenstand | Feststellung / Startbudget |
| --- | --- |
| Produktions-JAR unter `app/target/` | Rund 91 MB beziehungsweise 87 MiB; vorhandenes gebautes Artefakt. |
| Vorhandenes lokales App-Container-Image | Docker meldet 612 MB; dieser ältere lokale Imagebestand ist ein Größenanhalt, kein frisch vermessenes Hosting-Release. Testimage mit Werkzeugen etwa 1,48 GB, nicht ausliefern. |
| Laufende Anwendung | Java 25, Vaadin Flow mit serverseitigen Sitzungen, Spring und H2-Dateidatenbank. RAM wächst mit gleichzeitig geöffneten Ansichten und Benutzeraktionen, nicht nur mit Kontenzahl. |
| VM zum Einstieg | 2 vCPU, 4 GB RAM, etwa 40 GB SSD für wenige gleichzeitige Tester. Planwert, kein belegter Lasttest. |
| JVM-Budget | Zunächst etwa 1–1,5 GB maximaler Heap, zusätzlicher nativer Speicher und Reserve für OS, Proxy und DB. Tatsächliches RSS und Startspitzen vor Freigabe messen; Heap ist nicht der gesamte RAM-Bedarf. |
| Bauen und Testen | Maven, Node/Vaadin-Build und Browser benötigen vorübergehend deutlich mehr Ressourcen. In CI bauen, geprüftes fertiges Image auf die VM ziehen. Keine Browser-Suiten auf dem kleinen Zielserver. |
| Datenwachstum | H2, Avatare, ausstehende Mails, Ticketnachrichten und Sicherungen; bestehende Aufbewahrungsregeln nutzen. Docker-Images und Logs begrenzen. Keine Gigabyte Browserbelege dort sammeln. |

Eine VM mit 8 GB ist die erste Erweiterung, falls Messwerte oder tatsächliche
Testnutzung die Reserve aufbrauchen. Zum Nachweis vor Öffnung: Start/Neustart,
mehrere reale Sitzungen, Mailqueue und Kontoverwaltung prüfen; Container-RAM/CPU,
Datenträger und Antwortzeiten beobachten. Keine unbelegte Zusage für beliebig
viele gleichzeitige Vaadin-Benutzer.

## Dienste und ungefährer Preis

Recherche vom 13. November 2026. Anbieterpreise sind keine dauerhafte Zusage;
Schätzungen und gerundete Zusatzbudgets stehen ausdrücklich dabei.

| Lösung | Grobes Monatsbudget | Nutzen und Grenze |
| --- | --- | --- |
| Hetzner CX23, Deutschland, bei Verfügbarkeit | Server aktuell 6,53 € inkl. 19 % MwSt., ohne IPv4; etwa 0,60 € IPv4, etwa 1,31 € Serverbackup. Mit geschätzten 1–2 € Domainanteil rund 9,50–10,50 €, etwas Reserve einplanen. | Günstiger und weitgehend fester Rahmen; Linux-/Docker-Pflege liegt bei uns. Keine eingebaute Mailzustellung oder fertige Anwendungs-CI. |
| Hetzner CX33 als Ausbau | Server 10,10 € inkl. MwSt.; mit IPv4, Backup und Domain grob 14–16 €. | Mehr RAM-Reserve, aber über dem ursprünglichen 10-Euro-Ziel. Nicht vorsorglich buchen. |
| Railway Hobby | 5 US-Dollar Mindestumsatz mit Nutzungsguthaben; RAM derzeit 10 Dollar/GB/Monat, CPU, Speicher und Übertragung zusätzlich. Für durchlaufende Java-Anwendung grob 12–25 Dollar plus Domain als Planbudget, erst messen. | Weniger Serverwartung, einfacher Containerbetrieb. Verbrauchsabhängig; ein persistentes Volume und passende Region nötig. 5-Dollar-Tarif bedeutet keinen unbegrenzten 5-Dollar-Betrieb. |

Preisgrundlagen: [Hetzner inklusive deutscher MwSt.](https://docs.hetzner.com/de/general/infrastructure-and-availability/price-adjustment/),
[IPv4](https://docs.hetzner.com/de/cloud/servers/primary-ips/overview/),
[Backup-Aufschlag und Abrechnung](https://docs.hetzner.com/cloud/billing/faq/),
[Railway-Ressourcenpreise](https://docs.railway.com/pricing).
Domainbudget ist eine Schätzung je Endung und Anbieter, kein abgefragtes konkretes
Domainangebot. Bei Angeboten immer Folgeverlängerung statt nur Aktionspreis prüfen.

## Echte E-Mails

Eine Cloud-VM enthält kein empfangendes Postfach und keinen automatisch gut
zustellenden Transaktionsmaildienst. Empfehlung: eigene Absenderdomain und ein
SMTP-Relay, das unsere Mailqueue ohne neuen Versandadapter nutzen kann.

| Kandidat | Kleiner Einstieg | Abwägung |
| --- | --- | --- |
| Brevo | Free aktuell bis 300 Mails pro Tag. | Für Freunde ausreichend; Branding und Tarifbedingungen vor Wahl prüfen. Domain authentifizieren und Zustellprobleme im Anbieterportal untersuchen können. |
| Resend | Free aktuell 3.000 Mails pro Monat, 100 pro Tag. SMTP mit verifizierter Domain, unter anderem Port 587/STARTTLS. | Entwicklerfreundlicher alternativer Versandweg; SMTP passt zum vorhandenen Java-Mailer. Standort/Vertrag und Limits vor Wahl prüfen. |

Quellen: [Brevo Free](https://help.brevo.com/hc/de/articles/208580669-FAQ-Welche-Einschr%C3%A4nkungen-gelten-f%C3%BCr-das-Free-Paket),
[Resend-Tarife](https://www.resend.com/pricing?product=transactional),
[Resend SMTP](https://resend.com/docs/send-with-smtp).

Für die Domain SPF/DKIM und eine passende DMARC-Regel einrichten; Absender,
Antwortadresse und echtes empfangendes Supportpostfach bewusst unterscheiden.
Ein Domain-/Webhostingpaket mit Postfach kann dieses Postfach abdecken, ersetzt
aber nicht automatisch einen für unsere Anwendung geeigneten SMTP-Dienst.
Ein kostenloses Kontingent garantiert keine Zustellung; Registrierung, Reset und
Reaktivierung gezielt bei mehreren Empfängerdiensten nachprüfen.

Bei Hetzner sind 25/465 standardmäßig gesperrt, 587 ist laut
[Cloud-FAQ](https://docs.hetzner.com/cloud/servers/faq/) als Relay-Alternative offen.
Keinen eigenen Mailserver aufbauen. Mailpit bleibt für isolierte Testmails;
eine öffentlich zugängliche Mailpit-Inbox würde sensible Links offenlegen. Ein
zusätzliches Mailpit-Relay im echten Versandweg bringt hier keinen nötigen Nutzen.

## Was vor Veröffentlichung noch zu ändern oder nachzuweisen ist

| Aktueller Quellstand | Erforderlicher Schritt |
| --- | --- |
| `compose.yaml` ist ein lokales Testsetup: Loopback-Ports, Mailpit, bekannte Testzugänge, H2-Konsole ausdrücklich eingeschaltet. | Eigene Deployment-Konfiguration mit abgeschalteten Entwicklungsfunktionen, echten individuellen Adminzugängen und ausschließlich intern erreichbarem App-/DB-Port. Lokale Compose-Datei nicht durch öffentliche Defaults ersetzen. |
| `docker/https/Caddyfile` verwendet standardmäßig interne TLS-Ausstellung und entfernt HSTS für lokale Tests. | Öffentliche Domain mit ACME-Zertifikat und dauerhafter Caddy-Datenablage; Firewall 80/443, SSH nur eingeschränkt. HTTPS- und Headervertrag für öffentliche Nutzung gezielt prüfen. |
| `application-https.properties`, `HttpsTransportConfiguration` prüfen äußere Origin und vertrauenswürdigen Proxy. | Öffentliche Origin, Proxyhostname und Basis-URL konsistent setzen; Weiterleitungsheader nicht pauschal von beliebigen Clients vertrauen. Sichere Cookies, WebSocket-/Push-Verbindung und Links hinter dem Proxy testen. |
| `application.properties` enthält lokale Fallbacks, darunter Remember-me-Beispielschlüssel und SMTP-Vertrauen `*`. | Eigene dauerhafte zufällige Schlüssel setzen, SMTP-Zertifikatsprüfung für den Anbieter überprüfen, Debug aus. Verschlüsselungsschlüssel zusammen mit Wiederherstellungskonzept verwahren; keine Geheimnisse in Git oder Image. |
| `ConfigService` initialisiert fehlende Datenbankwerte aus Defaults. | Nur ENV zu ändern überschreibt nicht automatisch schon gespeicherte Konfiguration. Frische öffentliche Instanz bewusst initialisieren und gespeicherte Werte kontrollieren. |
| Google/Microsoft sind konfigurierbare OIDC-Anbieter. | Öffentliche Redirect-URIs/Origins bei beiden Anbietern registrieren; Veröffentlichungs-/Testbenutzerstatus und Tenantregeln prüfen. Keine erfundenen Clients aktivieren. Passwortregistrierung funktioniert unabhängig davon. |
| E-Mail-Nachweis, Kontosperren, 2FA, Recovery, Loginlimits und zweckgebundene Bestätigungen bestehen. | Unter echter Domain erneut durchgehend testen, einschließlich nicht bestätigter und selbstdeaktivierter Konten. Bestehende Regeln beibehalten; privilegierte Zugänge mit 2FA und gesicherten Ersatzcodes betreiben. |
| Login-Missbrauchsgrenzen sind vorhanden; Registrierung/Reset/öffentliche Ressourcen sind zusätzlich erreichbar. | Bestehende Begrenzungen pro Endpunkt prüfen, gemeinsames Missbrauchsbudget am Proxy ergänzen, falls Lücken bestehen. Keine neue Captcha-Pflicht ohne konkreten Bedarf. |
| H2-Datei, aktuelles SQL-Schema und JPA-Validierung sind für lokale austauschbare Daten ausgelegt. | Vor echten Freundeskonten Datenerhalt ausdrücklich neu entscheiden. Single-Instance-H2 ist technisch möglich; konsistente Sicherung/Wiederherstellung und spätere Schemaänderungen brauchen dann einen verbindlichen Ablauf. PostgreSQL wäre ein eigener bewusst beauftragter Umbau, keine spontane Voraussetzung dieser Planung. |
| Keine `.github/workflows` im aktuellen Repository vorhanden. | CI und Deployment erst einrichten; vorhandenen Docker-Build und Testbefehle wiederverwenden. |

Damit ist die Anmeldung eine gute fachliche Grundlage, aber **kein ungeprüfter
Internetfreigabestand**. Öffentliche Konten, Kontaktdaten und Nutzungsbedingungen
beziehungsweise Datenschutzhinweise vor der Einladung bewusst klären. Keine
lokalen Entwicklungsdaten oder gespeicherten Testmails veröffentlichen.

## Deployment, Zugriff und Rückkehr zu einem bekannten Stand

1. Benutzer bucht Domain/Server und Maildienst, bestätigt Konten/Zahlung und
   hinterlegt die nötigen Zugangsmittel einmalig sicher. Domain und Budget freigeben.
2. GitHub Actions baut/testet einen bezeichneten Commit, erstellt ein Image mit
   eindeutiger Revision beziehungsweise Releaseversion und veröffentlicht es in
   einer Registry. Kein `latest` als alleinige Rückkehrreferenz verwenden.
3. Getrennter Deployment-Zugang zieht genau dieses Image, sichert konsistent die
   Daten und startet den Dienst. Nicht bei jedem Zwischencommit automatisch
   öffentliche Daten neu aufsetzen. Freigegebener manueller Deployment-Auslöser
   genügt zunächst; ein späterer Release-Skill kann denselben Ablauf aufrufen.
4. Healthcheck intern und eine echte äußere HTTPS-Prüfung ausführen. Bei Fehlern
   alten Containerstand wiederherstellen; Datenbankschema und Datensicherung müssen
   zum Stand passen. Ein altes Image allein garantiert keinen sicheren DB-Rollback.
5. CLI/API und begrenzte SSH-/Registry-Zugänge ermöglichen spätere Arbeit durch
   den Agenten im erteilten Auftrag. Keine unbeschränkten persönlichen Cloudschlüssel
   im Projekt. Ausgaben dürfen keine Secrets enthalten. Ohne laufenden Auftrag
   gibt es keine dauerhafte Betreuung durch den Chat.

[GitHub-Deployment-Umgebungen](https://docs.github.com/en/actions/concepts/workflows-and-actions/deployment-environments)
bieten dafür Zuordnung und Schutzregeln; verfügbare Funktionen hängen von
Repositorysichtbarkeit und Tarif ab. Vor Umsetzung prüfen, keine kostenlose
private Freigabeumgebung oder unbegrenzten CI-Minuten versprechen.

**Jenkins als spätere Lernoption:** etabliert und frei betreibbar, aber eigener
Controller, Plugins, Sicherungen und Build-Agenten benötigen Pflege und Ressourcen.
Die [Jenkins-Empfehlung für ein kleines Team](https://www.jenkins.io/doc/book/installing/linux/)
nennt 4 GB+ RAM und 50 GB+ Speicher. Deshalb zunächst gehostete CI, kein Jenkins
neben der Anwendung auf derselben 4-GB-VM. Ein separater Lernserver kostet zusätzlich;
der heimische PC als Agent muss für Builds eingeschaltet und sicher angebunden sein.

## Logs, Sicherungen und Pausen

- Einstieg: rotierte Container-/Anwendungslogs, geschützter SSH-Zugriff und
  Erreichbarkeits-/Platten-/RAM-Überwachung. Keine öffentliche Logkonsole oder H2-Konsole.
  Den [Logging-Vertrag](../logging.md) beibehalten.
- Optional Grafana Cloud: [Free Logs](https://grafana.com/products/cloud/logs/)
  derzeit 50 GB Aufnahme pro Monat, 14 Tage Aufbewahrung. Für wenige Tester mehr
  als genug, aber Datenstandort, Vertragsbedingungen und übertragene Metadaten
  prüfen. Selbst gehostetes Grafana/Loki erhöht Ressourcen- und Wartungsbedarf.
- Anbieterbackup ergänzt eine konsistente Anwendungssicherung. H2-Dateien nicht
  während Schreibvorgängen beliebig kopieren. Wiederherstellung einschließlich
  Verschlüsselungsschlüsseln praktisch testen; Zugang zur Sicherung begrenzen.
- Anwendung stoppen und öffentliche Ports sperren beendet den Portalzugriff.
  Ein weiterlaufender Server braucht trotzdem Systempflege. Eine gestoppte VM
  kostet bei Hetzner weiterhin; Kosten enden nicht allein durch Ausschalten.
  Für eine lange Pause gesichert löschen oder ganz außer Betrieb nehmen und später
  vor erneuter Veröffentlichung aktualisieren. Domain/Snapshots/IP können weiter kosten.

## Geordnete spätere Umsetzung

- [ ] Domain, Anbieter, Maildienst und monatliches Kostenlimit gemeinsam wählen.
- [ ] Datenhaltung/Backup und Regeln ab Beginn echter Konten verbindlich entscheiden.
- [ ] Deployment-Konfiguration und Secrets vorbereiten; Entwicklungszugänge ausschließen.
- [ ] CI, Registry, begrenzten Deployment-Zugang und Wiederherstellung einrichten.
- [ ] Öffentliche HTTPS-/Mail-/OIDC-/2FA-Abläufe mit eigenen Testkonten prüfen.
- [ ] Ressourcenmessung, Missbrauchsgrenzen, Diagnosen und Betriebs-/Pausenanleitung abnehmen.
- [ ] Erst danach Freunde zur normalen Registrierung einladen.

Heute wurden weder Dienste gebucht noch DNS, Server oder bestehende Anwendungsdaten
verändert. Diese Planung bleibt bis zur gemeinsamen Entscheidung offen.
