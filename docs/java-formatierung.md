# Java-Formatierung als Arbeitsstandard

Seit 16.10.2026 gilt **Google Java Format**, Stil `GOOGLE` mit 2 Leerzeichen,
für künftige Java-Änderungen. Die Konfiguration liegt zentral im Root-`pom.xml`:
Spotless 3.10.4 verwendet die festgelegte Formatter-Version 1.36.1. Benötigt wird wie
für das Projekt Java 25.

## Verbindlicher Ablauf für Coding-Agenten

Nach jeder eigenen Java-Änderung, vor Tests und spätestens vor dem Abschluss:

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd spotless:check
```

Auch kleine Korrekturen ohne Build/Testlauf werden so formatiert. Nach weiteren
Java-Korrekturen beide Schritte wiederholen. Der Auftrag ist dauerhaft in
[AGENTS.md](../AGENTS.md) gespeichert und gilt damit auch in späteren Chats.

Die Befehle im Projektroot erfassen Java-Quellen und Tests aus Anwendung,
`shared` und `browser-tests`, obwohl die beiden Hilfsmodule eigenständige
Maven-Projekte sind. Generierte Dateien und Buildausgaben sind nicht eingeschlossen.

## Umfang und Schutz vorhandener Arbeit

`ratchetFrom=HEAD` beschränkt die Formatierung auf Dateien mit Änderungen gegenüber
dem letzten Commit sowie neue Dateien. Die ausgewählten **Dateien werden vollständig**
formatiert; die Auswahl bedeutet keine Beschränkung auf einzelne geänderte Zeilen.
Ein Commit ist danach der neue Ausgangspunkt. Das ist eine lokale Arbeitsregel,
keine vollständige CI-Stilprüfung des gesamten Bestands.

Wenn bereits fremde Java-Änderungen vorhanden sind, muss der Agent zusätzlich
`-DspotlessFiles=...` bei beiden Befehlen verwenden und damit ausschließlich seine
eigenen Dateien auswählen. Spotless erwartet dafür kommaseparierte reguläre
Ausdrücke, die auf absolute Dateipfade passen. Pfade und Sonderzeichen korrekt
maskieren; nicht lediglich einen relativen Pfad als Ausdruck einsetzen.

Eine Umformatierung des gesamten Bestands ist ein eigener Auftrag. CSS, Properties,
Markdown und sonstige Dateitypen werden von dieser Java-Konfiguration nicht formatiert.

## Ausführung und Prüfung

Der Formatter ist ein verbindlicher Arbeitsschritt des Agenten. Es wird kein
Hintergrund-Dateiwächter und kein persönlicher IDE-Speicher-Hook installiert.
Auch wird `spotless:apply` nicht in den normalen Maven-Build eingebunden: Ein Build
ändert dadurch keine Quellen, und Docker benötigt weiterhin kein `.git`-Verzeichnis.
Zum expliziten Formatieren über Spotless wird ein lokales Git-Checkout mit `HEAD`
benötigt. Der Eclipse-Formatter ist nicht mehr der von Spotless verwendete Standard.

Grundlagen: [Google Java Format](https://github.com/google/google-java-format)
und [Spotless Maven](https://github.com/diffplug/spotless/tree/main/plugin-maven).

## Prüfung der Einrichtung

Die Kombination wurde am 16.10.2026 mit Java 25 geprüft: Drei vorübergehende,
absichtlich unformatierte Java-Dateien aus Anwendung, Shared und Browsertests
wurden erkannt, mit 2 Leerzeichen formatiert und anschließend erfolgreich geprüft.
Ein erneuter Formatierungslauf änderte die Ergebnisse nicht. Die Probedateien
wurden danach entfernt. Eine bestehende fremde Java-Änderung war über die
Dateiauswahl ausgeschlossen. Es wurden keine Anwendungs- oder Browsertests gestartet.
