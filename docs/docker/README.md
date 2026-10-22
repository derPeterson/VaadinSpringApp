# Lokale Docker-Testumgebung

Docker Desktop verwendet Linux-Container. `compose.yaml` startet die Anwendung
mit eigener H2-Datei sowie Mailpit für synthetische Testmails. Der optimierte
Vaadin-Build im Container ist kein produktiver Einsatz der Anwendung.

Der Build verwendet die Root-POM mit `-pl app -am`: Shared wird vor der Anwendung
gebaut, Browserjourneys werden nicht ausgewählt. Das App-Artefakt stammt aus
`app/target/`. Der Buildkontext erlaubt nur Wrapper, Reactor-/Modul-POMs und die
benötigten App-/Shared-Quellen und Frontendkonfigurationen. Lokale `.env`, Daten,
Logs, Browserbelege, `target/`, `node_modules/` und generierte Frontendbundles
bleiben ausgeschlossen. Das Browsermodul wird nur mit seiner POM zur Reactor-
Erkennung aufgenommen.

Für die optionale Browser-Coverage enthält das lokale Testimage zusätzlich
`/app/coverage-sources.jar`. Dieses Archiv enthält ausschließlich die
Java-Quellen von App und Shared aus demselben Imagebuild; lokale Konfiguration,
Daten und CA-Schlüssel sind nicht enthalten. Der Browserrunner verwendet es mit
den Klassen aus dem getesteten App-/Shared-JAR, ohne lokale Buildstände zu mischen.
Laufzeitnutzer und HTTPS-/Health-Vertrag bleiben unverändert.

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

Anwendung: `https://127.0.0.1:18443/home`; Mailpit: `http://localhost:18025`.
Caddy veröffentlicht TLS auf 18443; GET/HEAD auf HTTP-Port 18080 werden dorthin
umgeleitet, andere Methoden erhalten 403. Der Appport wird nicht veröffentlicht.
Compose bindet die äußeren Zugänge nur an Loopback. SMTP ist intern `mailpit:1025`.
Die Compose-Datei definiert die lokalen Bootstrap-Testkonten; sie überschreibt
beim Wiederanlauf keine bestehenden Konten. Keine echten SMTP-Ziele erforderlich.

## Konfiguration

Vor dem ersten Browserzugriff das eigene CA-Vertrauen einrichten:

```powershell
New-Item -ItemType Directory -Force .\data\https\docker | Out-Null
docker compose cp proxy:/data/caddy/pki/authorities/local/root.crt .\data\https\docker\root.crt
docker compose cp proxy:/data/caddy/pki/authorities/local/intermediate.crt .\data\https\docker\intermediate.crt
.\scripts\https\trust.ps1 -Action Add -Certificate .\data\https\docker\root.crt -Intermediate .\data\https\docker\intermediate.crt
```

Der bewusst bestätigte Windows-Trust bleibt für diese lokale Umgebung erhalten.
Zum Rückbau dieselben Zertifikatsdateien mit `-Action Remove` verwenden, **bevor**
die Dateien oder Proxy-Volumes gelöscht werden. CA-Schlüssel bleiben ausschließlich
im eigenen `proxy-data`-Volume; sie gehören nicht ins Repository oder ins Appimage.
Normales `down` erhält auch die Proxy-CA. `down --volumes` entfernt sie; vor dem
nächsten Start altes Trustmaterial entfernen und danach die neue CA einrichten.
Hintergrund und IDE-Bedienung: [Lokales HTTPS](../entwicklung.md#lokales-https).

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
der geschützte H2-Ursprung. Aufruf: `https://127.0.0.1:18443/h2/`.

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
