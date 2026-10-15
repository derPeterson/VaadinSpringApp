# Anwendung für gezielte Handprüfungen

Stand 15.10.2026. Nur die gewählten [Sonderprüfungen](README.md) vorbereiten;
dies ist keine zweite vollständige Browser-Testanleitung. Eine Dokumentationsänderung
allein erfordert keinen Anwendungsneustart. Die folgenden Starts werden vom Prüfer
bei Bedarf ausgeführt, nicht automatisch durch diese Anleitung.

## 1. Eigener lokaler Prüfungsordner

In PowerShell 7 im Projektordner arbeiten; vorhandene Entwicklungsdatenbank und `.env` unverändert
lassen. Der folgende Block legt einen **neuen** Ordner samt Testkonfiguration und
Protokoll an. Er startet keine Anwendung und löscht nichts:

```powershell
$manualCommit = (git rev-parse --short HEAD).Trim()
$manualRunId = (Get-Date -Format 'yyyy-MM-dd_HH-mm-ss') + '__' + $manualCommit
$manualFolder = Join-Path (Get-Location).Path ('data/manual-tests/' + $manualRunId)
if (Test-Path -LiteralPath $manualFolder) {
    throw 'Prüfungsordner existiert bereits. Eigenen neuen Namen verwenden.'
}
New-Item -ItemType Directory -Path $manualFolder -ErrorAction Stop | Out-Null
$manualProperties = Get-Content 'docs/manual-tests/properties.example' -Raw
$manualProperties.Replace('__LAUF__', $manualRunId) |
    Set-Content -LiteralPath (Join-Path $manualFolder 'manual-tests.properties') -Encoding utf8
$manualProtocol = Get-Content 'docs/manual-tests/vorlage.md' -Raw
$manualProtocol = $manualProtocol.Replace('(README.md)', '(../../../docs/manual-tests/README.md)')
$manualProtocol = $manualProtocol.Replace('(anwendung.md)', '(../../../docs/manual-tests/anwendung.md)')
$manualProtocol = $manualProtocol.Replace('(vorlage_ausgefüllt.md)', '(../../../docs/manual-tests/vorlage_ausgefüllt.md)')
$manualProtocol = $manualProtocol.Replace('(../../browser-tests/abdeckung.md)', '(../../../browser-tests/abdeckung.md)')
$manualProtocol | Set-Content -LiteralPath (Join-Path $manualFolder 'protokoll.md') -Encoding utf8
Write-Output ('Prüfungsordner: ' + $manualFolder)
Write-Output ('IDE-Argument: --spring.config.import=file:./data/manual-tests/' + $manualRunId + '/manual-tests.properties[encoding=utf-8]')
```

Beide lokalen Dateien in der IDE öffnen. Alle `__LAUF__`-Platzhalter müssen ersetzt
sein; der Block erledigt dies für die Properties und passt die Dokumentationslinks
im lokalen Protokoll an seinen Ablageort an.

## 2. Nur notwendige Testwerte ergänzen

| Eintrag in manual-tests.properties | Was ist nötig? |
| --- | --- |
| spring.datasource.url | Neuer eigener Pfad aus dem Block; für Fortsetzung/Neustart unverändert lassen. |
| app.bootstrap.default-admin.password | Eigenes lokales Testpasswort, z. B. erfundenes `NurFuerTests1!`; keine echten Zugangsdaten. |
| app.config.defaults.rememberMe.secret.key | Eigener lokaler zufälliger Schlüssel; nicht leer lassen, nicht ins Protokoll kopieren. |
| server.port / app.config.defaults.base.url | Zusammen passend; vorbereitet ist 18090 / http://127.0.0.1:18090/. |
| SMTP | Lokal Mailpit auf 18125, Weboberfläche 18126; keine externe Weiterleitung. |

Die Vorlage enthält nur Isolation, Adminanlage und lokales SMTP. Queuefristen,
Rollendaten, Tokenlaufzeiten und zusätzliche Sonderfallwerte erst für den ausgewählten
Fall über die Oberfläche ändern und Originalwerte notieren. Keine pauschalen verkürzten
Tokenfristen. Datenbank-Konfiguration hat nach der ersten Anlage Vorrang vor
`app.config.defaults.*`.

Ein geeigneter zufälliger Testschlüssel kann in PowerShell mit
`[Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))`
erzeugt und ausschließlich in die lokale Datei eingetragen werden.

## 3. Mailpit nur bei Bedarf

Ohne Mailfall/Registrierung braucht diese Prüfung kein neues SMTP-Ziel.
Vorhandenes **eigenes Test-Mailpit** weiterverwenden und die Ports anpassen oder eine
eigene portable Instanz starten (Pfad zur vorhandenen Installation anpassen):

```powershell
& 'C:/Dev/Tools/Mailpit/mailpit.exe' --listen '127.0.0.1:18126' --smtp '127.0.0.1:18125'
```

Mailoberfläche: `http://127.0.0.1:18126/`. Für AUTO-02 kann eine zweite eigene
Instanz mit `--listen '127.0.0.1:18128' --smtp '127.0.0.1:18127'` nötig sein.
Nur die für den eigenen Test gestartete Instanz stoppen. Kein Relay an reale Postfächer;
für AUTO-08 ist ein gesondert freigegebenes Testziel erforderlich.

## 4. Start über IntelliJ oder Eclipse

1. Vorhandene funktionierende Application-Startkonfiguration kopieren, z. B.
   **Vaadin – Handprüfung**. Java/JRE 25 und Projektordner als Arbeitsverzeichnis wählen.
2. Das ausgegebene `--spring.config.import=file:...` als **Programmargument** eintragen.
   Dateiimport verpflichtend lassen: falscher Pfad soll abbrechen statt unbemerkt
   die Entwicklungsdatenbank zu öffnen. UTF-8-Dateiimport ist in der
   [Spring-Boot-Konfiguration](https://docs.spring.io/spring-boot/reference/features/external-config.html) beschrieben.
3. Geerbte DB-/SMTP-Overrides, aktive Profile und verknüpfte Env-Dateien aus dieser
   Testkonfiguration entfernen, soweit sie die Testdatei übersteuern würden.
4. Die IDE muss Java kompilieren/Ressourcen kopieren. Bei frischer Maven-Einbindung
   oder Shared-Änderungen zunächst `shared/pom.xml` → Maven → Lifecycle → `install`,
   danach Anwendung starten. Neue Übersetzungsgetter benötigen dieses aktualisierte JAR.
5. Bei der ersten Einrichtung die eigene Datenbankdatei im neuen Ordner und den
   Bootstrap-Admin prüfen. `http://127.0.0.1:18090/` öffnen; tatsächliche URL im Protokoll festhalten.
6. Nach erfolgreicher Adminanlage in der lokalen Datei Bootstrap auf `false` setzen
   und das Bootstrap-Passwort leeren. Das Konto bleibt gespeichert; die Änderung gilt
   für den nächsten Prozessstart. Die Datei ist keine Passwortänderung bestehender Konten.

**Kein zwingender zweiter Start zur Erstbefüllung:** `ConfigService` liest konfigurierte
Defaults bereits vor dem `DatabaseInitializer`; SMTP/Remember-me greifen darauf zu.
Die alte Begründung eines grundsätzlich nötigen zweiten Starts ist damit überholt.
Ein tatsächlich gespeicherter Wechsel startgebundener SMTP-/Remember-me-Einstellungen
benötigt weiterhin einen vollständigen Neustart mit derselben Testdatenbank (AUTO-02).
Das ist anhand des Codes abgeglichen, kein durch dieses Dokument ausgeführter Starttest.

Für Startprobleme zuerst falschen Import, Ports, fehlendes Bootstrap-Passwort und
höher priorisierte Werte prüfen. Kein Löschen bestehender Datenbank-/Lockdateien als
Abkürzung. Für aktuelle Styles muss die IDE Ressourcen kopieren; danach Refresh,
bei Bedarf normaler IDE-Neustart. Ein zusätzlicher Konsolenbuild ist für CSS nicht nötig.

## 5. Nur bei Prüfung auf einem echten Mobilgerät

`127.0.0.1`/`localhost` auf dem Handy ist nicht der PC. Für HUM-02 die Testkonfiguration
bewusst im eigenen lokalen Netz erreichbar machen: `server.address=0.0.0.0` und
`base.url` auf die erreichbare PC-LAN-Adresse mit Port 18090 setzen. Bei vorhandener
DB den Basis-URL-Wert im Adminbereich ändern; Properties-Defaults überschreiben ihn nicht.
Eine nötige Firewallfreigabe auf das eigene private Testnetz begrenzen. Nach der
Prüfung wieder auf lokale Bindung und ursprüngliche Basis-URL zurückstellen.

Neue Mails erst **nach** korrigierter Basis-URL anfordern; alte Links ändern sich nicht.
Mailpit kann auf dem PC lokal bleiben. Für HUM-02 ist kein externer Internetzugriff nötig.

## 6. Protokoll, Unterbrechung und Ende

- [vorlage.md](vorlage.md) enthält Schritte/Erwartungen und Ergebnisfelder;
  [vorlage_ausgefüllt.md](vorlage_ausgefüllt.md) zeigt alle Statusarten außer OFFEN sowie Fehler und Nachtest.
- Bei zwei Identitäten eigene Browserprofile verwenden; gleiche Profil-Tabs sind
  keine unabhängigen Konten. Nur erfundene Testkonten auf der eigenen Instanz anlegen.
- Bei fehlendem Gerät/SMTP-Ziel/Beobachtungsverfahren BLOCKIERT eintragen. Nicht
  verfügbare Voraussetzungen nicht durch Datenbankänderungen ersetzen.
- Für eine Fortsetzung dieselbe DB/Datei verwenden, Bootstrap bleibt deaktiviert.
  Für einen neuen unabhängigen Lauf einen neuen Ordner erzeugen.
- Nach einem Fix neuen Code-/Konfigurationsstand und Nachtest protokollieren;
  alte FEHLER-Zeile bleibt als Befund erhalten.
- Am Ende Verbindung, SMTP, LAN-Bindung und geänderte Konfiguration zurückstellen;
  nur eigene Prozesse beenden. Keine fremden Docker-Stacks oder bestehenden Datenbanken anfassen.
- Protokolle/Belege müssen nicht dauerhaft wachsen: wichtige Ergebnisse kurz unter
  [Meilensteine](../meilensteine/README.md) dokumentieren, lokale Diagnosen danach bei Bedarf bereinigen.
  Diese Überarbeitung löscht keine Testdatenbanken oder lokalen Properties-Dateien.
