# Build, JUnit und isolierte Startprüfung

Dies ist ein technischer Startnachweis des gebauten Zielstands, keine Browserabnahme.
Keine echten Anwendungsprozesse, Entwicklerdatenbanken oder externen Maildienste verwenden.

## Build und Tests

- Vor Maven die tatsächliche Java-Version prüfen; JDK 25 ist verbindlich. Nach eigenen
  Java-/Pythonkorrekturen die jeweiligen Stilprüfungen gemäß `AGENTS.md` durchführen.
- Shared bei frischem Stand/geänderten Quellen zuerst bauen/installieren, damit
  Anwendung und Tests keinen veralteten gemeinsamen Katalog verwenden.
- Optimierten Build mit vollständigen Anwendungstests bevorzugt zusammen ausführen:
  `mvnw.cmd clean verify -Pproduction -Dvaadin.productionMode=true` im Projektroot.
  Ein reiner Build ohne Tests bzw. ein leerer Testlauf ist kein bestandener JUnit-Nachweis.
- Tatsächliche Testzahlen und Skips aus den Reports ermitteln. Erwartete manuelle
  Opt-in-Ausschlüsse getrennt von unerwarteten Skips behandeln. Vorhandene passende
  Nachweise vor Integration ergänzen die Prüfung, ersetzen den beauftragten Ziellauf nicht.
- Bei Buildfehlern keinen alten JAR-/Image-Stand als erfolgreiche Zielprüfung starten.

## Start bis zur Betriebsbereitschaft

- Den frisch gebauten Zielstand in einer eigenen temporären Umgebung starten.
  Bestehende Testinfrastruktur bei Eignung nutzen; keinen zweiten Browserrunner
  oder ungeprüften neuen Startmechanismus als Nebenprodukt einführen.
- Frische isolierte H2-Daten und eigene freie Ports verwenden. Vorher den wirklichen
  Konfigurations-/Umgebungsladeweg prüfen: keine lokale `.env`, echten SMTP-Zugänge,
  Datenbankpfade, Bootstrap-Konten oder Hintergrundarbeiten versehentlich übernehmen.
  Schlüssel aus dem bestehenden Vertrag/den Quellen bestimmen, keine wirkungslosen
  Fantasie-Properties für Isolation erfinden. Reale Zustellung ausschließen.
- Ist eine sichere Startfixture nicht verfügbar, den Startnachweis als blockiert
  melden. Nicht ersatzweise mit dem normalen Entwicklerprofil starten.
- Eigenen Prozess/Container, Arbeitsverzeichnis, Ports und begrenzte Wartezeit
  eindeutig festhalten. Bereitschaft und passende lokale HTTP-Antwort prüfen;
  Portbelegung oder bloßes Prozessleben allein genügt nicht. Keine Playwright-Journeys
  oder längeren Benutzerabläufe ohne zusätzliche Freigabe anschließen.
- Bei Fehler/Timeout die erste relevante Ursache untersuchen und den Nachweis als
  fehlgeschlagen ausweisen. Logs nach den Projektregeln behandeln; keine Secrets ausgeben.
- Zum Abschluss oder Abbruch nur die eigenen eindeutig zugeordneten Prüfprozesse/
  Container beenden. Temporäre eigene Daten bei Bedarf nach Prüfung der absoluten
  Zielpfade bereinigen; fremde Prozesse, Volumes und Entwicklungsdaten erhalten.
- Bei Unterbrechung eigene Prozesskennungen und den echten Prüfstand im Chat oder
  eigenen temporären Arbeitszustand erhalten. Nicht erneut einen laufenden eigenen
  Prozess starten. Keine automatische Fortsetzungsautomation ohne entsprechenden Auftrag.
