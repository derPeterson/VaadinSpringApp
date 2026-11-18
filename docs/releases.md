# Versionierung und lokale Releases

## Version und Entwicklungsstand

Root, App, Shared, Browsermodul und Coverage-Berichtsmodul führen dieselbe
Projektversion. App behält den Spring-Boot-Parent; interne Abhängigkeiten verwenden
`${project.version}`. Der Release-Helfer ändert die Projektversionen gemeinsam,
ohne Bibliotheksversionen zu ersetzen.

- `0.1.0-SNAPSHOT`: beweglicher Entwicklungsstand vor dem ersten Release.
- `0.1.0`: erster bewusst geprüfter lokaler Entwicklungsrelease, keine Zusage eines
  stabilen öffentlichen Betriebs. Historische Commits erhalten keine erfundenen Releases.
- `0.1.1-SNAPSHOT`: folgende kleine Entwicklungslinie. Fehlerkorrekturen erhöhen
  die Patch-Stelle; neue Funktionen bzw. Vertragsänderungen bewusst als Minor
  einordnen. `1.0.0` oder spätere Major-Sprünge werden ausdrücklich entschieden.

`prepare` entfernt den Snapshotzusatz der bereits gewählten Entwicklungslinie;
es erhöht nicht ungefragt die Versionsbasis. Für einen bewusst freigegebenen
Minor-/Major-Schritt zuerst die Projektversionen aller fünf POMs gemeinsam auf
den gewünschten Snapshot setzen, prüfen und separat committen. Bibliotheksversionen
bleiben dabei unverändert. Danach die Vorschau und den normalen Release-Ablauf verwenden.

Ein Commit ist keine neue Releaseversion. Maven-Snapshots erhalten erst beim
Veröffentlichen in ein entsprechendes Maven-Repository Zeitstempel/Buildnummer;
lokal genügen Projektversion und Git-Revision. Jenkins und öffentliches Hosting
sind keine Voraussetzung und werden durch einen lokalen Release nicht eingerichtet.

## Notizen und Artefakt

[CHANGELOG.md](../CHANGELOG.md) ist die eine redaktionelle Quelle. Oben steht
`## [Unreleased]`, darunter jeweils `### DE` und `### EN`. Releaseabschnitte heißen
`## [X.Y.Z] - YYYY-MM-DD`. Beide Sprachen beschreiben dieselben nutzbaren Funktionen
oder wichtigen Fehlerkorrekturen; keine Buttonverschiebungen oder Commitchronik.
Bei fachlicher Featureabnahme ergänzen, beim Review prüfen. Leere unveröffentlichte
Sprachabschnitte sind erlaubt, leere Releases oder einseitige Übersetzungen nicht.
Die sichtbare Neuigkeiten-Seite ist ein eigener späterer Auftrag.

Das ausführbare JAR liegt versionsunabhängig unter `app/target/app.jar`.
`META-INF/build-info.properties` im gepackten Spring-Boot-JAR enthält Projektversion,
Git-Revision und Dirty-Zustand. Spring Boots bereits verwaltetes Git-Plugin
liefert lokal ausschließlich Commit-ID und Dirty-Zustand; keine Autoren- oder
Remoteangaben. Normale lokale Builds benötigen den Repository-Kontext.
Es entsteht kein neuer öffentlicher Info-Endpunkt.

Docker erhält die Herkunft über die Buildargumente `APP_SOURCE_REVISION` und
`APP_SOURCE_DIRTY`. Der isolierte Browserrunner setzt sie selbst aus seinem
Quellstand. Andere Compose-Builds können diese Umgebungswerte bewusst übergeben;
ohne Angaben bleiben sie unbekannt/dirty. `.git` und private Einstellungen werden
nicht in den Docker-Kontext aufgenommen. Herkunftsmetadaten sind keine Garantie
gegen absichtlich gefälschte Buildparameter.

## Befehle

Python aus der Projektumgebung verwenden. Die Befehle laufen im Root:

```powershell
.\.venv\Scripts\python.exe scripts/release/release.py preview
# Beispiel: nächster Patch aus 0.1.1-SNAPSHOT, mit gepflegten Unreleased-Notizen.
.\.venv\Scripts\python.exe scripts/release/release.py prepare --version 0.1.1
# Kandidaten-Diff prüfen und bewusst committen, danach bauen und vollständig prüfen.
.\.venv\Scripts\python.exe scripts/release/release.py verify
.\.venv\Scripts\python.exe scripts/release/release.py tag
.\.venv\Scripts\python.exe scripts/release/release.py next
# Die folgende Snapshotänderung separat committen.
```

`preview` schreibt nichts. `prepare` benötigt einen sauberen, passenden Snapshot
und verschiebt dessen zweisprachige Notizen in einen datierten Kandidatenabschnitt;
es erzeugt noch keinen Tag und keinen Commit. `verify` liest das JAR und vergleicht
Version und Revision mit dem sauberen Kandidaten. `tag` erfordert zusätzlich die
belegte Gesamt-QA und erzeugt einen annotierten `vX.Y.Z`-Tag. Bestehende Tags,
abweichende archivierte Artefakte und Manifeste werden nicht überschrieben.
Nach Fehlern den tatsächlichen Zustand prüfen: Ein vorbereitetes Artefakt allein
ist noch kein erfolgreicher Release.

Das geprüfte JAR und `manifest.json` liegen anschließend im ignorierten lokalen
`.codex/releases/X.Y.Z/`. Das Manifest ordnet Version, Commit, SHA-256 und QA zu.
Diese ausdrücklich aufbewahrten Release-Artefakte sind keine temporären Browserbelege;
keine automatische Löschung. `next` funktioniert nur direkt am getaggten Stand und
stellt alle Projekt-POMs auf den folgenden Patch-Snapshot um.

## QA-Freigabe

Der bestehende [QA-Zustand](../.agents/skills/grosse-testrunde/references/zustand.md)
bleibt die einzige Quelle für den Prüfstand. Der Agent trägt nach tatsächlich
erfolgreichen Prüfungen zusätzlich `last_success.release_qualification` ein:

| Feld | Nachweis |
| --- | --- |
| `version`, `commit`, `sha256` | Exakt das geprüfte Kandidaten-JAR und seine Revision |
| `checks` | `junit`, `frontend`, `offline`, `coverage`, `browser_de`, `browser_en` jeweils `passed` |
| `quality_gate`, `sonar_revision` | `OK` und identische Kandidatenrevision für den Sonar-Nachweis |
| `browser_image_de`, `browser_image_en` | Identische Docker-Image-ID beider Sprachläufe |

Außerdem muss `last_success.phase` `complete`, dessen `commit` identisch mit dem
Kandidaten und `current` leer (`null`) sein.
Der Helfer bewertet keine fachliche Testabdeckung und ersetzt keinen Testlauf.
Er verhindert, dass versehentlich die QA eines anderen Artefakts zum Taggen genügt.
Die zugrunde liegenden Reports und Zahlen prüft der Release-Ablauf; eine erfundene
JSON-Freigabe wäre kein gültiger Nachweis.

Wartende Bugs oder notwendige Tests nicht für einen grünen Abschluss aus den Listen
löschen. Ein Git-Rollback setzt keine Datenbank zurück. Bis zum Livebetrieb gelten
unser aktuelles Schema und austauschbare Testdaten.

## Gezielte Helfertests

```powershell
.\.venv\Scripts\python.exe -m unittest discover -s scripts/release -p 'test_*.py' -v
```

Eigene temporäre Git-Repositories und synthetische JARs prüfen Vorschau,
Modulversionen, beide Sprachen, falsche Metadaten, fehlende oder veraltete QA,
unveränderliche Tags/Artefakte und den nächsten Patch. Die Helfertests starten
weder Portal noch Docker und veröffentlichen nichts.
