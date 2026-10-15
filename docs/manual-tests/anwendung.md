# VaadinSpringApp frisch für manuelle Tests aufsetzen

> Anleitung, Version 1.2 · aktualisiert am 10.10.2026; Konfiguration und Protokolle je Durchlauf.
> Mit dem aktuellen Quellcode abgeglichen. Der beschriebene Start wurde für diese
> Anleitung nicht ausgeführt; sein Ergebnis wird im manuellen Testprotokoll festgehalten.

Abgleich 15.10.2026: Für den normalen IDE-Start müssen aktuelle Java-Klassen und
Ressourcen kopiert werden. Neue gemeinsame Übersetzungsgetter erfordern zuerst
`shared` → Maven → Lifecycle → `install`; danach Anwendung neu starten und die Seite
aktualisieren. Der [Designstandard](../design-standard.md) gilt auch für manuelle
Prüfungen. Diese Einrichtungsanleitung dokumentiert keinen aktuellen manuellen Lauf;
automatisierte Nachweise stehen in der [Abdeckung](../../browser-tests/abdeckung.md).
Wichtige Ergebnisse dauerhaft unter [Meilensteine](../meilensteine/README.md)
zusammenfassen; lokale Laufordner und umfangreiche Belege sind danach bereinigbar.

## Inhaltsverzeichnis

- [1. Was „frisch“ hier bedeutet](#1-was-frisch-hier-bedeutet)
- [2. Anwendung stoppen und Stand erfassen](#2-anwendung-stoppen-und-stand-erfassen)
- [3. Lokalen Mailserver vorbereiten](#3-lokalen-mailserver-vorbereiten)
- [4. Neue Testdatenbank vorbereiten](#4-neue-testdatenbank-vorbereiten)
- [5. IntelliJ für diesen Test einrichten](#5-intellij-für-diesen-test-einrichten)
- [6. Erster Start und Grundeinstellungen](#6-erster-start-und-grundeinstellungen)
- [7. Vollständig neu starten](#7-vollständig-neu-starten)
- [8. Einrichtung prüfen und Testprotokoll beginnen](#8-einrichtung-prüfen-und-testprotokoll-beginnen)
- [9. Optionaler Start mit PowerShell](#9-optionaler-start-mit-powershell)
- [10. Beim nächsten Mal und nach dem Test](#10-beim-nächsten-mal-und-nach-dem-test)
- [11. Häufige Probleme](#11-häufige-probleme)

## 1. Was „frisch“ hier bedeutet

Du verwendest den vorhandenen Projektcode, startest ihn aber mit einer **neuen,
leeren Testdatenbank**. Sie enthält zunächst keine bisherigen Benutzer, Tokens,
Einstellungen, Warteschlangeneinträge oder Admin-Mailaudits.

Die bisherige Datenbank bleibt erhalten. Dafür bekommt der Test einen eigenen
Datenbankpfad. Du musst weder das Git-Projekt neu klonen noch IntelliJ, Java,
Node.js oder OpenCode neu installieren. Auch Benchmarkarchive bleiben erhalten.

**Nicht für diesen Schritt erforderlich:** ZIP-Dateien neu einspielen,
`node_modules` löschen oder `ddl-auto=create` einstellen. Der aktuelle Stand
verwendet H2 und `ddl-auto=update`; bei einem neuen Datenbankpfad werden die
Tabellen neu angelegt. Die dokumentierten **Bestandsmigrationen** sind bei einer
tatsächlich leeren Datenbank nicht erforderlich. Prüfe trotzdem, ob der Start das
aktuelle Schema fehlerfrei anlegt.

Ziel für heute: **IntelliJ + getrennte H2-Testdatenbank + lokaler SMTP-Testserver**.
Die Anwendung selbst muss dafür noch nicht in Docker laufen.

## 2. Anwendung stoppen und Stand erfassen

1. Laufende Anwendung in IntelliJ mit **Stop** beenden.
2. Weitere von dir gestartete Instanzen ebenfalls ordentlich beenden. Ein offener
   Browser allein hält den Java-Prozess nicht an.
3. IntelliJ-Projekt `C:\Dev\Projects\VaadinSpringApp` öffnen.
4. In PowerShell im Projektordner den Stand erfassen:

```powershell
Set-Location 'C:\Dev\Projects\VaadinSpringApp'
git branch --show-current
git rev-parse HEAD
git rev-parse --short HEAD
git status --short
```

Für den vereinbarten Gesamttest den gewünschten, bereits gemergten Stand auf
`main` verwenden. Lokale Änderungen dokumentieren; nicht einfach verwerfen.

5. In IntelliJ prüfen: Projekt-JDK und JRE der Startkonfiguration sind **Java 25**.
6. Falls du Maven verwenden möchtest, zuerst prüfen:

```powershell
.\mvnw.cmd --version
```

Bei einer anderen Java-Version zunächst die lokale JDK-Auswahl korrigieren.
Ein Maven-Aufruf immer mit Ziel ausführen: Im aktuellen Projekt startet ein
Wrapper-Aufruf **ohne Ziel** die Anwendung.

- [ ] Anwendung beendet; zu testender Commit und Java 25 festgehalten.

## 3. Lokalen Mailserver vorbereiten

Nutze einen SMTP-Testserver, der Mails lokal sammelt. **Mailpit** ist dafür eine
passende Möglichkeit. Damit kannst du echte SMTP-Annahme, Mailinhalt und Links
prüfen, ohne Nachrichten an reale Postfächer zuzustellen. Externe Zustellung
ist damit noch nicht getestet.

### Variante A: Mailpit unter Windows

1. Über die [offizielle Installationsanleitung](https://mailpit.axllent.org/docs/install/)
   das Windows-Paket passend zur Prozessorarchitektur herunterladen.
2. Entpacken, beispielsweise nach `C:\Dev\Tools\Mailpit`.
3. In einem eigenen PowerShell-Fenster starten; Pfad an deine Ablage anpassen.
   Bei deinem aktuellen Aufbau liegt die portable EXE unter `C:\Dev\Tools\Mailpit`:

```powershell
& 'C:\Dev\Tools\Mailpit\mailpit.exe' --listen '127.0.0.1:8025' --smtp '127.0.0.1:1025'
```

4. Im Browser `http://localhost:8025/` öffnen.
5. Mailpit während des Tests laufen lassen. Keine Weiterleitung/Relay-Konfiguration
   zu einem externen SMTP-Server aktivieren.

Die beiden Bindungsoptionen sind in den [Mailpit-Laufzeitoptionen](https://mailpit.axllent.org/docs/configuration/runtime-options/)
dokumentiert. `8025` ist die Mailoberfläche, `1025` der SMTP-Port für die Anwendung.
Der Start bindet beide an den lokalen Rechner.

### Variante B: Wenn Docker bereits eingerichtet ist

Statt Variante A kannst du den Mailserver als Container starten:

```powershell
docker run -d --name vaadin-manual-mailpit -p 127.0.0.1:8025:8025 -p 127.0.0.1:1025:1025 axllent/mailpit
```

Die [offiziellen Mailpit-Docker-Images](https://mailpit.axllent.org/docs/install/docker/)
verwenden dieselben Ports. Läuft dieser Container bereits, ihn weiterverwenden;
bei einem Namenskonflikt keinen zweiten gleichnamigen Container anlegen.
Für die Wiederholbarkeit die verwendete Image-ID bzw. Version im Protokoll notieren.
Für fest reproduzierbare spätere Läufe einen konkreten Release-Tag statt des
beweglichen Standard-Tags verwenden.

**Für beide Varianten:** Mailpit-Version und Ausgangszustand der Mailablage
notieren. Alte Testmails anhand von Zeitpunkt und Testadresse unterscheiden.
Belege vor dem Leeren sichern; Nachrichten nach einem Neustart nicht ungeprüft
als weiterhin vorhanden voraussetzen.

- [ ] Mailoberfläche erreichbar; lokaler SMTP-Port 1025 bereit.

## 4. Neue Testdatenbank vorbereiten

Wähle einen eindeutigen Ordner, beispielsweise:

```text
C:\Dev\Projects\VaadinSpringApp\data\manual-tests\2026-10-10_14-30__56f66ac
```

Datum, Uhrzeit und Commit-Kürzel sind **Beispiele**. Das Kürzel erhältst du
mit `git rev-parse --short HEAD`. In einem Durchlauf liegen Konfiguration
und Datenbank normalerweise zusammen im gleichnamigen Testordner.
Für eine neue Einrichtung einen bisher
unbenutzten Namen wählen. Der folgende Block legt nur einen neuen Ordner an;
er löscht keine vorhandenen Daten:

```powershell
Set-Location 'C:\Dev\Projects\VaadinSpringApp'
$manualDbDirectory = Join-Path (Get-Location).Path 'data\manual-tests\2026-10-10_14-30__56f66ac'
if (Test-Path -LiteralPath $manualDbDirectory) {
    throw 'Dieser Testordner existiert bereits. Für eine frische Einrichtung einen neuen Namen wählen.'
}
New-Item -ItemType Directory -Path $manualDbDirectory -ErrorAction Stop
```

**Für die Beispielablage lautet die vollständige Datenbank-URL:**

```text
jdbc:h2:file:C:/Dev/Projects/VaadinSpringApp/data/manual-tests/2026-10-10_14-30__56f66ac/appdb;AUTO_SERVER=TRUE;MODE=MySQL;DB_CLOSE_DELAY=-1;SCHEMA=PUBLIC
```

Hinter `file:` steht der neue Speicherpfad. `appdb` ist der Datenbank-Basisname,
keine bereits vorhandene Datei. H2 legt beim Start beispielsweise `appdb.mv.db`
an. Die übrigen URL-Optionen entsprechen der bisherigen Projektkonfiguration.

**Die URL an deinen tatsächlich neu angelegten Ordner anpassen.** Für Neustarts
innerhalb desselben Tests dieselbe URL behalten. Sonst würdest du wieder eine
neue Datenbank statt des gerade eingerichteten Zustands öffnen.

- [ ] Neuer Ordner angelegt; vollständige Datenbank-URL im Protokoll notiert.

## 5. IntelliJ für diesen Test einrichten

1. **Run → Edit Configurations…** öffnen.
2. Deine funktionierende Startkonfiguration für `Application` kopieren und
   beispielsweise **Vaadin – manuelle Tests** nennen.
3. Folgende Werte prüfen:

| Feld | Wert |
| --- | --- |
| Main class | `de.derpeterson.app.Application` |
| JRE | Java 25 |
| Working directory | `C:\Dev\Projects\VaadinSpringApp` |
| Mehrere gleichzeitige Instanzen | Für diesen Durchlauf deaktiviert |

### Separate lokale Konfigurationsdatei

4. Die mitgelieferte Datei `properties.example` kopieren und im
   Projekt als **`data/manual-tests/2026-10-10_14-30__56f66ac/manual-tests.properties`** ablegen.
   Falls dort bereits eine eigene Datei liegt, diese nicht blind überschreiben.
5. Datei im Editor öffnen und folgende Werte anpassen:

| Schlüssel in der Datei | Was du einträgst |
| --- | --- |
| `spring.datasource.url` | Vollständige neue Datenbank-URL aus Abschnitt 4 |
| `app.bootstrap.default-admin.password` | Eigenes lokales Testpasswort |
| `app.config.defaults.rememberMe.secret.key` | Eigener langer zufälliger Testschlüssel |
| `app.config.defaults.base.url` | Erreichbare Anwendungsadresse; lokal `http://localhost:8080/` |

Die übrigen Werte sind für den lokalen Mailpit-Test bereits vorbereitet.
**Die beiden geheimen Felder sind in der Vorlage absichtlich leer; vor dem ersten
Start ausfüllen.** Du kannst hierfür eigene bekannte lokale Testwerte verwenden. Ohne gesetztes
Bootstrap-Passwort entsteht kein Admin. Die Bootstrap-Anlage prüft das
Passwort nicht wie die Registrierung: `Password1` kann beim Bootstrap
funktionieren, während die normale Registrierung ein Sonderzeichen
verlangt, beispielsweise `Password1!`. Dies sind ausschließlich Testzugänge.

Die Datei verwendet **Java-Properties-Schlüssel** wie `spring.datasource.url`,
nicht Umgebungsvariablennamen wie `SPRING_DATASOURCE_URL` oder `APP_CFG_MAIL_HOST`.
Pro Zeile gilt `schlüssel=wert`. Werte nicht in Anführungszeichen einschließen;
die Semikolons der DB-URL bleiben in derselben Zeile. Backslashes sind in diesem
Format Escapezeichen; für Windows-Pfade deshalb wie in der Vorlage `/` verwenden.

Ein selbst gewählter Schlüssel ist für diesen lokalen Test ausreichend;
für die Bearbeitung im Adminbereich mindestens acht Zeichen verwenden.
Der folgende Zufallsgenerator ist eine **optionale** Alternative. Du kannst
in PowerShell Folgendes ausführen und
die Ausgabe einmal lokal in das Schlüsselfeld kopieren:

```powershell
$manualKeyBytes = New-Object byte[] 32
$manualKeyGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $manualKeyGenerator.GetBytes($manualKeyBytes)
} finally {
    $manualKeyGenerator.Dispose()
}
[Convert]::ToBase64String($manualKeyBytes)
```

Ausdrücklich freigegebene, erfundene lokale Testpasswörter dürfen in der
Kontentabelle des Laufprotokolls stehen. Für echte Zugangsdaten und den
Remember-me-Schlüssel dort nur die Konfigurationsdatei referenzieren.
Die lokale Datei liegt
bewusst unter `data/`: Dieser Ordner wird im aktuellen Projekt bereits durch
`.gitignore` ausgeschlossen. Die bisherige `.env` bleibt erhalten und braucht
keine `.bak`-Kopie, weil du sie nicht bearbeitest. Die Vorlage ohne Geheimnisse
kannst du beispielsweise unter `docs/manual-tests/properties.example`
versionieren.

### Ein Eintrag in IntelliJ

6. In der kopierten Startkonfiguration das Feld **Program arguments /
   Programmargumente** öffnen, gegebenenfalls über **Modify options** einblenden.
7. Diese einzelne Zeile eintragen:

```text
--spring.config.import=file:./data/manual-tests/2026-10-10_14-30__56f66ac/manual-tests.properties[encoding=utf-8]
```

8. **Apply → OK**. Es ist keine Tabelle mit einzelnen Umgebungsvariablen erforderlich.
9. Die benannte Konfiguration dauerhaft speichern, falls IntelliJ sie noch als
   temporär führt. So kannst du sie nach einer Testpause zuverlässig auswählen.

Das Argument wählt für diese Startkonfiguration die Testdatei anstelle des
bisherigen `.env`-Imports. Die Basiskonfiguration aus `application.properties`
bleibt verfügbar. Der Import ist bewusst verpflichtend: Ein falscher Pfad soll
den Start abbrechen und nicht unbemerkt die bisherige Datenbank öffnen.

Die IDE-Bedienung ist in der [IntelliJ-Anleitung zu Programmargumenten](https://www.jetbrains.com/help/idea/program-arguments-and-environment-variables.html)
beschrieben; das Arbeitsverzeichnis in der [Startkonfiguration](https://www.jetbrains.com/help/idea/run-debug-configuration-spring-boot.html).
Der Dateiimport und das UTF-8-Attribut sind in der [Spring-Boot-Konfigurationsanleitung](https://docs.spring.io/spring-boot/reference/features/external-config.html)
dokumentiert.

### Vorhandene lokale Einstellungen beachten

- Die ursprüngliche Startkonfiguration verwendet weiterhin den bisherigen
  `.env`-Import, sofern sie keine eigenen Overrides enthält.
- Wenn du schon Variablen aus der früheren Anleitung in die **Testkonfiguration**
  eingetragen hast, diese dort entfernen, insbesondere `SPRING_DATASOURCE_URL`.
  Die Werte sollen künftig aus der Testdatei kommen.
- Weitere Programmargumente, VM-Optionen wie `-Dspring.datasource.url`, aktive
  Profile, verknüpfte IDE-Env-Dateien und `SPRING_APPLICATION_JSON` auf abweichende
  Einstellungen kontrollieren. Höher priorisierte Werte können Dateiwerten vorgehen.

- [ ] Neue Startkonfiguration eingerichtet; keine abweichenden DB-/SMTP-Overrides.

## 6. Erster Start und Grundeinstellungen

1. Mailpit läuft; jetzt die neue IntelliJ-Startkonfiguration starten.
2. Warten, bis der Start abgeschlossen ist. Bei Fehlern die erste relevante
   Ursache im Log erfassen; nicht auf einen anderen Datenbankpfad ausweichen,
   ohne das im Protokoll festzuhalten.
3. Prüfen, dass im neuen Testordner die H2-Datei entstanden ist.
4. Im Log die Anlage/Prüfung von Rollen, Konfiguration und Bootstrap-Admin prüfen.
5. `http://localhost:8080/login` öffnen und mit dem Testadmin anmelden.
   **Remember me beim Einrichtungsschritt noch nicht aktivieren.**
6. Im Adminbereich die Benutzerliste prüfen: Bei einer frischen Einrichtung
   sollte dort zunächst dein Bootstrap-Admin und kein bisheriger Testbenutzer stehen.
7. Im Konfigurationsbereich die folgenden Werte prüfen und bei Bedarf speichern.
   Die Oberfläche gruppiert Einstellungen; gegebenenfalls die jeweilige Gruppe wechseln.

| Konfigurationsschlüssel | Sollwert für diesen lokalen Test |
| --- | --- |
| `service.name` | z. B. `Vaadin Test` |
| `base.url` | `http://localhost:8080/` |
| `email.from` | `no-reply@example.test` |
| `email.admin` | `admin@example.test` |
| `mail.host` | `127.0.0.1` |
| `mail.port` | `1025` |
| `mail.smtp.auth` | `false` |
| `mail.smtp.starttls.enable` | `false` |
| `mail.smtp.ssl.enable` | `false` |
| `mail.debug` | `false` |
| `mail.username`, `mail.password` | Für diesen Server nicht benötigt; leer |
| `rememberMe.secret.key` | Eigener Testschlüssel, nicht der eingebaute Platzhalter |

**Warum hier noch einmal prüfen?** Es gibt zwei Ebenen:

1. Die Testdatei liefert mit `app.config.defaults.*` die Vorgaben für die Erstbefüllung.
2. Danach verwenden die Dienste die **gespeicherten Datenbankwerte**.

Geänderte Vorgaben überschreiben bestehende Werte nicht automatisch. Auch
`base.url` und `service.name` sind in der Testdatei vorbereitet; bei bereits
vorhandenen Einstellungen Änderungen im Adminbereich speichern.

### Besonderheit des allerersten Starts

`MailConfig` und der Remember-me-Dienst lesen ihre Werte schon bei der Erstellung
der Spring-Komponenten. Die erstmalige Konfigurationsbefüllung läuft erst danach
im `DatabaseInitializer` als `CommandLineRunner`.

Bei leerer Datenbank verwenden diese Komponenten zunächst die eingebauten
Standardwerte. Dazu gehört derzeit `mail.smtp.auth=true`, obwohl du in der Testdatei
`false` als Vorgabe gesetzt hast. Nach der Befüllung werden die bereits erstellten
Komponenten nicht automatisch neu eingerichtet.

Deshalb **noch keinen Mail-/Remember-me-Test bewerten**, sondern zuerst den
vollständigen Neustart aus Abschnitt 7 durchführen. Den ersten Start einschließlich
dieser Besonderheit im Testprotokoll dokumentieren.

- [ ] Adminzugang funktioniert; gewünschte Einstellungen gespeichert und geprüft.

## 7. Vollständig neu starten

1. In IntelliJ **Stop** drücken und warten, bis der Java-Prozess beendet ist.
2. In `data/manual-tests/2026-10-10_14-30__56f66ac/manual-tests.properties` die Zeile
   `app.bootstrap.default-admin.enabled=false` setzen und speichern.
3. Den Wert bei `app.bootstrap.default-admin.password=` leeren. Das Konto ist
   bereits gespeichert; das Passwort wird beim Neustart nicht neu gesetzt.
4. **Datenbank-URL unverändert lassen.**
5. Anwendung mit derselben Testkonfiguration erneut starten.
6. Nach erfolgreichem Start Browser neu laden; bei Bedarf mit dem Testadmin neu anmelden.

Ein Browser-Reload oder IntelliJ-Hot-Reload genügt hier nicht. Der komplette
Prozessneustart sorgt dafür, dass SMTP- und Remember-me-Komponenten die inzwischen
gespeicherten Einstellungen lesen.

- [ ] Zweiter Start erfolgreich; gleicher Datenbankpfad, Bootstrap ausgeschaltet.

## 8. Einrichtung prüfen und Testprotokoll beginnen

### Kurze Funktionsprobe

1. `/admin` erreichbar; Testadmin weiterhin vorhanden.
2. Über den vorgesehenen Mailtest eine Nachricht an `probe@example.test` senden.
3. In Mailpit prüfen: richtige Nachricht, Empfänger, Absender, Betreff und Umlaute.
   Ein UI-Erfolg ohne eintreffende Nachricht ist noch kein bestandener Mailtest.
4. Ein neues Testkonto, beispielsweise `user@example.test`, registrieren.
5. Verifikationsmail in Mailpit öffnen und den Link im Browser ausprobieren.
6. Prüfen, dass der Link auf diese Testanwendung zeigt und das Konto danach
   entsprechend dem Verifikationsablauf angemeldet werden kann.

Wenn ein Schritt scheitert: Abweichung und Beleg ins Testprotokoll übernehmen.
Die Einrichtung nicht als vollständig erfolgreich markieren.

### Vollständigen Durchlauf anlegen

1. Die manuelle Testvorlage als `docs/manual-tests/vorlage.md` ablegen.
2. Für denselben Durchlauf zwei Kopien anlegen:
   - `anwendung.md` → `runs/JJJJ-MM-TT_HH-mm__commit__app.md`: Einrichtung.
   - `vorlage.md` → `runs/JJJJ-MM-TT_HH-mm__commit__tests.md`: Funktionstests.
   Datum, Uhrzeit und Commit in beiden Namen gehören zu demselben Durchlauf.
3. Das tatsächliche Commit-Kürzel ermitteln:

```powershell
git rev-parse --short HEAD
```

4. Datum/Uhrzeit im Dateinamen und vollständigen Commit, Datenbank-URL,
   Konfiguration, Browserprofile sowie Mailpit-Version im Kopf festhalten.
5. Im `__tests.md`-Protokoll die Einrichtungsdatei verlinken. Bereits ausgeführte Einrichtungsschritte mit ihren Ergebnissen dokumentieren;
   weitere Tests nach der Vorlage fortsetzen.
6. Für Admin A und Benutzer B getrennte Browserprofile verwenden. Zwei Tabs
   desselben Profils reichen für zwei unabhängige Konten nicht aus.

### Falls du vom Handy testen möchtest

- `localhost` im Handy-Browser bezeichnet das Handy, nicht deinen PC.
- Die Anwendung muss im gewünschten lokalen Netzwerk erreichbar sein.
  Für diesen Durchlauf eine passende PC-Adresse wie `http://<PC-IP>:8080/`
  verwenden und `base.url` im Adminbereich dazu passend ändern.
- Danach eine **neue Mail** anfordern. Alte Mails behalten das beim Erstellen
  verwendete Linkziel.
- Mailpit in dieser Anleitung ist nur auf dem PC erreichbar. Mails zunächst
  dort ansehen; ein mobiler Zugriff auf die Mailoberfläche wäre gesondert
  einzurichten. Ein externer Internetzugang ist dafür nicht erforderlich.

- [ ] SMTP-Probe und erster Kontenablauf geprüft; Ergebnisse protokolliert.
- [ ] Vollständiger manueller Testdurchlauf mit passenden Browserprofilen begonnen.

## 9. Optionaler Start mit PowerShell

Wenn du weiterhin IntelliJ verwendest, kannst du diesen Abschnitt überspringen.
**Nicht zusätzlich starten**, solange die IntelliJ-Anwendung noch läuft.

1. Neue Testdatenbank und lokale Konfigurationsdatei aus Abschnitt 4/5 vorbereiten.
2. Ein PowerShell-Fenster ohne die alten Test-Umgebungsvariablen verwenden.
   Insbesondere ein verbliebenes `SPRING_DATASOURCE_URL` kann den Dateiwert übersteuern.
3. Im Projektordner die Wrapper-Version prüfen:

```powershell
Set-Location 'C:\Dev\Projects\VaadinSpringApp'
.\mvnw.cmd --version
```

4. Mit bestätigtem Java 25 die gleiche Datei wie in IntelliJ laden:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--spring.config.import=file:./data/manual-tests/2026-10-10_14-30__56f66ac/manual-tests.properties[encoding=utf-8]'
```

Der Parameter zur Argumentübergabe ist in der [Spring-Boot-Maven-Anleitung](https://docs.spring.io/spring-boot/maven-plugin/run.html)
dokumentiert. Datenbank und Grundeinstellungen kommen aus derselben Datei;
separate Umgebungsvariablen musst du dafür nicht anlegen.

5. Grundeinstellungen aus Abschnitt 6 prüfen.
6. Anwendung mit `Strg+C` beenden und auf Prozessende warten.
7. Datei wie in Abschnitt 7 bearbeiten: Bootstrap ausschalten, Bootstrap-Passwort
   leeren, Datenbank-URL beibehalten.
8. Mit exakt demselben Startbefehl erneut starten; dann Abschnitt 8 ausführen.

## 10. Beim nächsten Mal und nach dem Test

**Hier musst du für die heutige Einrichtung nichts zusätzlich durchführen.**
Wenn die Grundeinrichtung fertig ist, arbeitest du jetzt in der Datei
`runs/<durchlauf>__tests.md` die Funktionsprüfungen ab. Die folgenden Hinweise
sind für eine spätere Pause, einen neuen Durchlauf oder das tatsächliche Testende.

### A. Heute oder nach einer Pause weiterprüfen

1. Mailpit starten, falls es beendet wurde.
2. Dieselbe IntelliJ-Konfiguration wählen und denselben Dateiimport behalten.
3. Datenbank-URL unverändert lassen; Bootstrap bleibt `false`.
4. Falls nötig Anwendung starten und anmelden; im bestehenden Testprotokoll fortfahren.

Die beiden angelegten Konten bleiben bestehen. Du musst sie nicht erneut anlegen
und die Einrichtung nicht wiederholen. Für den Registrierungstest einer wirklich
neuen Identität eine zusätzliche Testadresse verwenden, wenn das erste Konto
schon existiert. Teilprüfungen und frühere Beobachtungen im Protokoll festhalten.

### B. Später einen neuen vollständigen Durchlauf beginnen

1. Anwendung stoppen und neuen Zeitpunkt sowie getesteten Commit bestimmen.
2. Beide Protokollvorlagen mit dem neuen gemeinsamen Durchlaufnamen kopieren.
3. Einen neuen, bisher unbenutzten Datenbank-/Konfigurationsordner anlegen.
4. `properties.example` dorthin als `manual-tests.properties` kopieren.
5. Neue DB-URL und Testwerte eintragen; IntelliJ-Import auf diese Datei umstellen.
6. Bootstrap für die erste Anlage einschalten; nach der Einrichtung wieder
   ausschalten und vollständig neu starten (Abschnitt 6/7).

Alte Protokolle, Konfigurationen und Datenbanken bleiben für die Nachvollziehbarkeit
erhalten. Das ist eine neue Einrichtung und keine bloße Fortsetzung von A.

### C. Nach dem tatsächlichen Testende

- [ ] Ergebnisse, offene Fehler und Endzeit im `__tests.md`-Protokoll eingetragen.
- [ ] Benötigte Mail-/Screenshotbelege archiviert.
- [ ] Veränderte Fristen/Retry-Einstellungen wiederhergestellt oder Abweichungen notiert.
- [ ] Anwendung und bei Bedarf Mailpit beendet.
- [ ] Verwendete Testkonfiguration und Datenbank für den nächsten Start dokumentiert.

Diese Abschlussliste erst nach den Funktionstests bearbeiten, nicht bereits
nach der Kontenanlage. Wenn du wieder mit der bisherigen Entwicklungsdatenbank
arbeiten möchtest, nach dem Stoppen die ursprüngliche Startkonfiguration wählen.
Für die laufenden manuellen Tests bleibst du bei der Testkonfiguration.

## 11. Häufige Probleme

| Beobachtung | Zuerst prüfen |
| --- | --- |
| Alte Benutzer erscheinen | Dateiimport und neue DB-URL wirklich wirksam? Höher priorisierte Parameter? Versehentlich ursprüngliche Startkonfiguration gewählt? |
| Kein Admin vorhanden | Bootstrap beim ersten Start aktiviert, E-Mail und Passwort gesetzt? Logs zur Anlage prüfen. Ohne Bootstrap entsteht kein automatischer Admin. |
| Geändertes Bootstrap-Passwort funktioniert nicht | Vorhandene Konten werden durch Bootstrap nicht geändert. Passwort über den vorgesehenen Kontenablauf ändern; nicht für diesen Zweck eine DB löschen. |
| Port 8080 ist belegt | Eine zweite Anwendung läuft noch. Bekannte Instanz ordentlich stoppen oder bewusst anderen Port und passende `base.url` verwenden. |
| Datenbankdatei gesperrt | Vorherige Instanz bzw. eine Datenbankverbindung noch offen? Keine Lockdateien bei laufendem Prozess löschen. |
| Mailversand scheitert beim ersten Start | Erstbefüllung und vollständigen Neustart beachten; gespeichertes `mail.smtp.auth=false`, Host, Port und Mailpit prüfen. |
| SMTP-Änderung wirkt nicht | Im Adminbereich speichern, danach vollständigen Prozessneustart durchführen. `app.config.defaults.*` überschreibt bestehende Datenbankwerte nicht. |
| Keine Mail bei `probe@example.test` im echten Postfach | Die Testmail wird in Mailpit gesammelt. Dort ansehen; keine externe Zustellung eingerichtet. |
| Mail-Link zeigt auf falschen Host | `base.url` prüfen und anschließend neue Mail anfordern. |
| Zugriff auf `/h2` nicht möglich | In dieser Anleitung bewusst deaktiviert; für normale Browserabläufe nicht erforderlich. |
| „Command line is too long“ in IntelliJ | In der Test-Startkonfiguration die bereits bewährte Einstellung zur Verkürzung der Kommandozeile verwenden. |
| Frontend-/Startfehler trotz erfolgreicher JUnit-Tests | Erste konkrete Laufzeitursache protokollieren. Automatischer Testlauf bestätigt noch keinen erfolgreichen Browserstart. |

## Projektgrundlage dieser Anleitung

Abgeglichen wurden `README.md`, `src/main/resources/application.properties`,
`config/DatabaseInitializer.java`, `config/MailConfig.java`, `service/ConfigService.java`,
`model/enums/ConfigEntry.java`, `security/SecurityConfig.java`,
`views/admin/AdminConfigurationSection.java` und der Einstieg `Application.java`.
Quellcode und bestehende Datenbanken wurden dafür nicht verändert.
