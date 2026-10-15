# Docker-Testumgebung

Diese Anleitung beschreibt die lokale Testumgebung aus `compose.yaml`: die Anwendung mit eigener H2-Datenbank und Mailpit zum Auffangen von Test-E-Mails. Docker Desktop muss mit Linux-Containern laufen. Java, Maven und Node werden beim Docker-Build im Container bereitgestellt.

Aktueller Prüfstand des Redesigns: [Browserabdeckung](../../browser-tests/abdeckung.md).
Die automatisierten Läufe verwenden `compose.browser-tests.yaml`, eigene Projekte,
H2-/Mailpit-Volumes und getrennte Ports. `-Coverage` bindet nur dort einen JaCoCo-Agenten
ein. Diese manuelle Compose-Umgebung und vorhandene Daten werden dadurch nicht verändert.

## Inhalt

- [Starten und beenden](#starten-und-beenden)
- [Adressen und Testkonto](#adressen-und-testkonto)
- [H2-Datenbank öffnen](#h2-datenbank-öffnen)
- [Einstellungen](#einstellungen)
- [Änderungen übernehmen](#änderungen-übernehmen)
- [Testdaten und frischer Start](#testdaten-und-frischer-start)
- [Status und Fehlersuche](#status-und-fehlersuche)
- [Dateien und Prüfstand](#dateien-und-prüfstand)

## Starten und beenden

PowerShell im Projektverzeichnis öffnen:

```powershell
Set-Location C:\Dev\Projects\VaadinSpringApp
```

| Aktion | Befehl |
| --- | --- |
| Erster Start oder geänderten Code bauen | `docker compose up -d --build --wait` |
| Vorhandenen Stand starten | `docker compose up -d --wait` |
| Beide Dienste stoppen | `docker compose stop` |
| Nur die Anwendung neu starten | `docker compose restart app` |
| Container und Projektnetzwerk entfernen, Testdaten behalten | `docker compose down` |

`-d` startet im Hintergrund; `--wait` wartet auf die Bereitschaft der Dienste. Beim Build laufen Maven-Tests und der Vaadin-Produktionsbuild. Der erste Build lädt zusätzlich Images und Abhängigkeiten und kann länger dauern. Ein späterer Start ohne `--build` verwendet das vorhandene Image.

## Adressen und Testkonto

| Oberfläche | Adresse |
| --- | --- |
| Anwendung | [http://localhost:18080/home](http://localhost:18080/home) |
| Anmeldung | [http://localhost:18080/login](http://localhost:18080/login) |
| Mailpit: aufgefangene E-Mails | [http://localhost:18025](http://localhost:18025) |
| H2-Konsole | [http://localhost:18080/h2/](http://localhost:18080/h2/) |

Bei einer frischen Datenbank wird mit der aktuellen Compose-Konfiguration dieses Adminkonto angelegt:

- E-Mail: `admin@example.test`
- Passwort: `Password1!`

Das sind lokale Testzugangsdaten. Ein vorhandenes Konto wird beim Neustart nicht mit einem neuen Passwort überschrieben.

Mailpit nimmt die E-Mails der Anwendung entgegen und zeigt sie im Browser an. In der aktuellen Konfiguration werden sie nicht an echte Postfächer weitergeleitet. SMTP läuft zwischen den Containern über `mailpit:1025`; auf Windows muss dafür kein eigener SMTP-Dienst laufen.

## H2-Datenbank öffnen

1. Anwendung starten und [H2-Konsole](http://localhost:18080/h2/) öffnen.
2. Folgende Werte eintragen:

| Feld | Wert |
| --- | --- |
| Driver Class | `org.h2.Driver` |
| User Name | `sa` |
| Password | **leer lassen** |

JDBC URL zum Kopieren:

```text
jdbc:h2:file:/app/data/appdb;MODE=MySQL;DB_CLOSE_DELAY=-1;SCHEMA=PUBLIC;IFEXISTS=TRUE
```

3. **Test Connection** wählen. Bei korrekten Angaben erscheint **Test successful**.
4. Mit **Connect** anmelden; die Anwendungstabellen liegen im Schema `PUBLIC`.

`/app/data/appdb` ist der Pfad **im Container**, kein Windows-Pfad. `IFEXISTS=TRUE` verhindert, dass ein Tippfehler versehentlich eine neue leere Datenbank erzeugt. Die Verbindung öffnet die tatsächliche Docker-Testdatenbank; SQL-Änderungen wirken direkt auf die laufende Testanwendung.

Die Konsole wird in `compose.yaml` mit `APP_SECURITY_EXPOSE_H2_CONSOLE: "true"` freigegeben. Mit `"false"` wird sie deaktiviert. Danach `docker compose up -d --wait` ausführen.

## Einstellungen

Variable Testwerte stehen unter `services.app.environment` in [compose.yaml](../../compose.yaml).

| Einstellung | Aktueller Wert / Bedeutung |
| --- | --- |
| `SPRING_DATASOURCE_URL` | H2-Datei `/app/data/appdb` einschließlich H2-Verbindungsoptionen |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | `sa` / leeres Passwort; bei Änderungen auch die H2-Anmeldung anpassen |
| `APP_CFG_SERVICE_NAME` | Anzeigename `VaadinSpringApp (Docker-Test)` |
| `APP_CFG_BASE_URL` | `http://localhost:18080/`; Basis für Links in E-Mails |
| `APP_SECURITY_EXPOSE_H2_CONSOLE` | Schaltet H2-Konsole und deren Zugriffsfreigabe gemeinsam |
| `APP_BOOTSTRAP_DEFAULT_ADMIN_ENABLED` | Aktiviert die Anlage des ersten Testadmins |
| `APP_BOOTSTRAP_DEFAULT_ADMIN_EMAIL` / `APP_BOOTSTRAP_DEFAULT_ADMIN_PASSWORD` | Zugangsdaten für die erstmalige Adminanlage |
| `APP_CFG_EMAIL_FROM` / `APP_CFG_EMAIL_ADMIN` | Test-Absender und Empfänger für Adminmails |
| `APP_CFG_MAIL_HOST` / `APP_CFG_MAIL_PORT` | `mailpit` / `1025` |
| `APP_CFG_MAIL_USERNAME` / `APP_CFG_MAIL_PASSWORD` | Leer; Mailpit benötigt hier keine Anmeldung |
| `APP_CFG_MAIL_SMTP_AUTH` / `APP_CFG_MAIL_SMTP_STARTTLS_ENABLE` / `APP_CFG_MAIL_SMTP_SSL_ENABLE` | Alle `"false"` für diese lokale Mailpit-Verbindung |
| `APP_CFG_REMEMBER_ME_SECRET_KEY` | Lokaler Testschlüssel für „Remember me“ |
| `TZ` | `Europe/Berlin` in beiden Containern |

**Gespeicherte Einstellungen haben Vorrang:** `APP_CFG_*` liefert Vorgaben für fehlende Datenbankeinträge. Bereits gespeicherte Werte werden durch Änderungen in Compose nicht überschrieben. Diese Werte im Adminbereich ändern; für SMTP und „Remember me“ anschließend die Anwendung neu starten. Auch das Bootstrap-Passwort ändert kein bestehendes Konto.

Die Portzuordnung `127.0.0.1:18080:8080` bedeutet: Windows-Port `18080` führt zum Anwendungsport `8080` im Container. Mailpit verwendet entsprechend `18025:8025`. Beide Oberflächen sind nur auf diesem Rechner erreichbar. Bei Änderung des Anwendungsports auch `APP_CFG_BASE_URL` bzw. den bereits gespeicherten Basis-URL-Wert anpassen.

Feste Docker-Einstellungen stehen in [application-docker.properties](../../src/main/resources/application-docker.properties), etwa Produktionsmodus und interner Port. Die lokale Anwendungs-`.env` wird nicht ins Image kopiert oder von Spring importiert; eine zusätzliche Datei mit manuellen Testwerten ist für diesen Docker-Start nicht nötig.

## Änderungen übernehmen

| Änderung | Danach ausführen |
| --- | --- |
| Compose-Umgebungsvariablen oder Portzuordnung | `docker compose up -d --wait` |
| Java, Bilder, CSS, Properties, Abhängigkeiten oder Dockerfile | `docker compose up -d --build --wait` |
| Nur Neustart mit unveränderter Konfiguration | `docker compose restart app` |

`restart` übernimmt **keine geänderten Compose-Umgebungsvariablen**. Dafür `up` verwenden. Im Produktionsmodus werden Quellcodeänderungen nicht automatisch nachgeladen; nach dem Neubau die Browserseite neu laden. Bei Änderungen ausschließlich an dieser Anleitung ist kein Neubau nötig. Siehe [Compose up](https://docs.docker.com/reference/cli/docker/compose/up/) und [Compose restart](https://docs.docker.com/reference/cli/docker/compose/restart/).

## Testdaten und frischer Start

Docker speichert Daten unabhängig vom Container in zwei benannten Volumes:

- `vaadinspringapp-test_app-data`: H2-Datenbank mit Benutzern, Einstellungen und Mailqueue.
- `vaadinspringapp-test_mailpit-data`: aufgefangene E-Mails.

Stoppen, Neustarten, Neubauen und `docker compose down` erhalten diese Daten. Die IntelliJ-Datenbank wird durch diese Volume-Zuordnung nicht verwendet. Den Compose-Projektnamen `vaadinspringapp-test` beibehalten, wenn dieselben Volumes weiter genutzt werden sollen.

**Nur für einen gewollten vollständigen Testreset:** Der folgende erste Befehl löscht beide Docker-Testvolumes einschließlich aller Testkonten und aufgefangenen E-Mails unwiderruflich. Er ist kein normaler Stop-Befehl.

```powershell
docker compose down --volumes
docker compose up -d --wait
```

Beim nächsten Start wird eine neue Datenbank samt Testadmin angelegt. Wenn zugleich Code geändert wurde, beim zweiten Befehl zusätzlich `--build` verwenden. Zum Verhalten siehe [Compose down](https://docs.docker.com/reference/cli/docker/compose/down/).

## Status und Fehlersuche

```powershell
# Dienste und Bereitschaft anzeigen
docker compose ps

# Letzte Anwendungsmeldungen anzeigen
docker compose logs --tail 100 app

# Anwendungsmeldungen live verfolgen; Strg+C beendet nur diese Anzeige
docker compose logs -f app

# Mailpit-Meldungen anzeigen
docker compose logs --tail 100 mailpit

# Compose-Datei prüfen, ohne etwas zu starten
docker compose config --quiet
```

Bei nicht erreichbarer Anwendung zunächst Docker Desktop und `docker compose ps` prüfen. Für H2 müssen der Schalter aktiv und die JDBC URL korrekt sein. Bei fehlenden Testmails prüfen, ob die gespeicherten SMTP-Werte tatsächlich `mailpit:1025` verwenden; eine bestehende Datenbank kann noch andere Werte enthalten.

## Dateien und Prüfstand

| Datei | Aufgabe |
| --- | --- |
| [compose.yaml](../../compose.yaml) | Startet Anwendung und Mailpit, setzt Testwerte, Ports und Volumes |
| [Dockerfile](../../Dockerfile) | Baut und testet die Anwendung mit Java 25 und Node 24; startet das JAR mit einem eigenen Benutzer |
| [.dockerignore](../../.dockerignore) | Begrenzt die Build-Eingaben; lokale Datenbanken, `.env` und generierte Dateien bleiben draußen |
| [application-docker.properties](../../src/main/resources/application-docker.properties) | Spring-Profil für den Docker-Betrieb |

Am 11.10.2026 geprüft: Docker-Produktionsbuild mit **967 Tests, 0 Fehlern, 0 Fehlschlägen und 0 Skips**; Anwendung und Mailpit bereit; H2-Verbindung erfolgreich und H2-Schalter in beiden Zuständen geprüft. Logo, Avatar und DE-/EN-Flaggen wurden im Browser sichtbar geladen. Diese Prüfungen ersetzen noch keinen vollständigen automatisierten Test aller Benutzerabläufe.
