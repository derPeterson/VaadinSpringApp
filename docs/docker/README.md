# Lokale Docker-Testumgebung

Docker Desktop verwendet Linux-Container. `compose.yaml` startet die Anwendung
mit eigener H2-Datei sowie Mailpit für synthetische Testmails. Der optimierte
Vaadin-Build im Container ist kein produktiver Einsatz der Anwendung.

## Bedienung

Im Projektverzeichnis:

| Aktion | Befehl |
| --- | --- |
| Bauen und starten | `docker compose up -d --build --wait` |
| Vorhandenes Image starten | `docker compose up -d --wait` |
| Status | `docker compose ps` |
| Letzte Appmeldungen | `docker compose logs --tail 100 app` |
| Stoppen | `docker compose stop` |
| Nur App neu starten | `docker compose restart app` |
| Container entfernen, Testdaten behalten | `docker compose down` |

Anwendung: `http://localhost:18080/home`; Mailpit: `http://localhost:18025`.
Compose bindet beide Oberflächen nur an Loopback. SMTP ist intern `mailpit:1025`.
Die Compose-Datei definiert die lokalen Bootstrap-Testkonten; sie überschreibt
beim Wiederanlauf keine bestehenden Konten. Keine echten SMTP-Ziele erforderlich.

## Konfiguration

`APP_CFG_*` liefert Startvorgaben; gespeicherte Adminwerte haben Vorrang.
SMTP, Remember-me und Workerzahl benötigen nach Änderung einen Appneustart.
Nach Compose-/Portänderungen `up` verwenden: `restart` übernimmt keine neue
Containerkonfiguration. Nach Java-/CSS-/Ressourcenänderungen mit `--build` bauen.
Die lokale Entwickler-`.env` wird nicht ins Image übernommen.

Die Appdatei liegt im Volume `vaadinspringapp-test_app-data`, Mailpit im separaten
`vaadinspringapp-test_mailpit-data`. Sie sind unabhängig von der IntelliJ-Datenbank.
Ein beabsichtigter frischer Teststart löscht nur diese Compose-Testdaten:

```powershell
docker compose down --volumes
docker compose up -d --build --wait
```

Der erste Befehl entfernt die Testkonten und aufgefangenen Mails dieses Projekts.
Normales Stoppen, Neubauen und Neustarten behält sie. Das aktuelle Schema wird
bei leerem Datenbestand neu angelegt und anschließend durch JPA validiert.

## H2-Konsole

Compose kann die lokale Konsole über `APP_SECURITY_EXPOSE_H2_CONSOLE=true` freigeben;
Standard der Anwendung ist aus. Erlaubt sind die vorgesehenen Loopbackhosts und
der geschützte H2-Ursprung. Aufruf: `http://localhost:18080/h2/`.

Treiber `org.h2.Driver`, Benutzer `sa`, Passwort leer. JDBC URL:

```text
jdbc:h2:file:/app/data/appdb;MODE=MySQL;DB_CLOSE_DELAY=-1;SCHEMA=PUBLIC;IFEXISTS=TRUE
```

Die Verbindung arbeitet direkt auf dieser Testdatenbank. Änderungen sind keine
isolierte Ansicht. Für normale Browserprüfungen ist keine Konsolennutzung nötig.

## Automatisierte Läufe

Der [Browserrunner](../../browser-tests/README.md) verwendet `compose.browser-tests.yaml`,
eigene Projektnamen, Datenvolumes und Ports. Er berührt diese manuelle Umgebung nicht.
Start/Ende, Mailpit und Tests liegen in seinem eigenen Lebenszyklus.
