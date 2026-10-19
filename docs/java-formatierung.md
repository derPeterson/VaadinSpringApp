# Java-Formatierung und Lesbarkeit als Arbeitsstandard

Seit 16.10.2026 gilt **Google Java Format**, Stil `GOOGLE` mit 2 Leerzeichen,
für künftige Java-Änderungen. Die Konfiguration liegt zentral im Root-`pom.xml`:
Spotless 3.10.4 verwendet die festgelegte Formatter-Version 1.36.1. Benötigt wird wie
für das Projekt Java 25.

## Verbindlicher Ablauf für Coding-Agenten

Nach jeder eigenen Java-Änderung, vor Tests und spätestens vor dem Abschluss:

```powershell
.\mvnw.cmd spotless:apply
.\mvnw.cmd spotless:check
.\mvnw.cmd checkstyle:check
```

Auch kleine Korrekturen ohne Build/Testlauf werden so formatiert. Nach weiteren
Java-Korrekturen alle drei Schritte wiederholen. Der Auftrag ist dauerhaft in
[AGENTS.md](../AGENTS.md) gespeichert und gilt damit auch in späteren Chats.

Die Befehle im Projektroot erfassen Java-Quellen und Tests aus Anwendung,
`shared` und `browser-tests`, obwohl die beiden Hilfsmodule eigenständige
Maven-Projekte sind. Generierte Dateien und Buildausgaben sind nicht eingeschlossen.

## Verbindliche Lesbarkeitsregeln

Seit 20.10.2026 gelten zusätzlich die ausdrücklich abgestimmten Regeln:

- `if`, `else`, `for`, erweitertes `for`, `while` und `do/while` erhalten immer
  geschweifte Klammern, auch bei nur einer Anweisung. `else if` darf eine Kette
  bleiben. Bei `try`/`catch`/`finally` sind Klammern ohnehin Java-Syntax;
  Absätze und überschaubare Blöcke machen die Fehlerbehandlung lesbar.
- Explizite lokale Typen sind der Standard. Besonders bei Service-/Factory-
  Rückgaben soll der Leser den Objekttyp direkt erkennen. `var` bleibt erlaubt,
  wenn es einen konkreten Vorteil hat: bei anonymen oder nicht direkt benennbaren
  Typen oder sehr langen generischen Typen, die bereits im Konstruktor sichtbar
  sind. Es gibt keinen pauschalen `var`-Zwang und kein pauschales Verbot.
- Normale und statische Imports nennen die tatsächlich verwendeten Typen bzw.
  Mitglieder. Keine Sternchenimporte. Spotless sortiert statische Imports zuerst,
  normale Imports danach und entfernt unbenutzte Imports.
- Fachliche Objekte und längere Lambdas bekommen sprechende Namen. Kurze
  Schleifenindizes bleiben angemessen; Methodennamen beschreiben die Aufgabe.
- Verschachtelte ternäre Ausdrücke durch verständliche Schritte oder benannte
  Methoden ersetzen. Einfache kurze Fallunterscheidungen dürfen ternär bleiben.
- Große Methoden nach echten Aufgaben gliedern. Keine starren Zeilenlimits und
  kein Zerlegen in bedeutungslose Weiterleitungsfunktionen. Zusammengehörige
  Formularbindungen dürfen zusammenstehen; Transaktionen, Sperren, Fehlergrenzen
  und Listener-Lebensdauer bleiben erhalten.
- Kommentare erklären Gründe, Verträge und Besonderheiten. Namen und Struktur
  sollen den offensichtlichen Ablauf bereits verständlich machen.

```java
UserEntity currentUser = userService.loadUser(userId);

if (currentUser.isEnabled()) {
  refreshAccount(currentUser);
} else {
  showAccountUnavailable();
}
```

Das Beispiel erläutert den Stil, keine neue Service-Schnittstelle. `var` bedeutet
weiterhin statische Typableitung beim Kompilieren; es bietet keinen Laufzeit-
oder Leistungsvorteil. Die verbliebenen konkreten Beispiele im Projekt sind
überwiegend anonyme Test-/Wrappertypen und der gemeinsame, nicht unmittelbar
benennbare Typ von Vaadin-TextField und TextArea.

## Automatische Stilprüfung

Die zentrale [Checkstyle-Konfiguration](../config/checkstyle.xml) prüft mit
`NeedBraces` und `AvoidStarImport` sämtliche handgeschriebenen Java-Quellen der
Anwendung, von Shared und der Browsertests, einschließlich Tests. Der Root-POM
verwendet Maven Checkstyle Plugin 3.6.0 und Checkstyle 14.3.0 ausschließlich als
Buildwerkzeug; Anwendungsabhängigkeiten und Google Java Format bleiben erhalten.

```powershell
.\mvnw.cmd checkstyle:check
```

Anders als der Spotless-Ratchet prüft dieser Aufruf den gesamten Bestand und
benötigt kein Git-Checkout. Er verändert keine Quellen. Der Aufruf ist bewusst
explizit und nicht an den normalen Build oder einen IDE-Hook gebunden. Für
Coding-Agenten ist er über `AGENTS.md` vor Tests und Abschluss verbindlich.
Bei Bedarf kann dieselbe Prüfung separat in eine CI-Pipeline aufgenommen werden;
eine solche Workflowänderung wurde nicht vorgenommen.

Checkstyle prüft Klammern und Imports, nicht die fachliche Qualität eines Namens,
die sinnvolle `var`-Ausnahme oder die richtige Methodengliederung. Diese Punkte
bleiben Teil des Reviews. Regeln:
[NeedBraces](https://checkstyle.org/checks/blocks/needbraces.html),
[AvoidStarImport](https://checkstyle.org/checks/imports/avoidstarimport.html),
[Maven-Check-Ziel](https://maven.apache.org/plugins/maven-checkstyle-plugin/check-mojo.html).

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
Auch werden `spotless:apply` und die explizite Checkstyle-Prüfung nicht in den
normalen Maven-Build eingebunden: Ein Build
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
