# Prüfstand

## Große Testrunde vom 18.11.2026

Geprüft wurde der saubere Stand `486a54c0b848cdf1a8b78a15b00f527521abce9c`
auf main, gegenüber der letzten vollständigen QA `f3c21623`.
Dieser Stand ist der lokale Release **0.1.0**, annotiert als `v0.1.0`.
Die Aura-Umstellung ist damit technisch abgeschlossen. Die weitergehende
[Vaadin-Komponentenumstellung](planung/vaadin-komponenten-abgleich.md) bleibt offen.

Der anschließende Entwicklungsstand **0.1.1-SNAPSHOT** enthält die gemeinsame
Versionsfortschreibung und den Dokumentationsabschluss. Er wurde nicht als eigener
neuer Anwendungsstand vollständig browsergetestet; die folgenden Belege gehören
zum unveränderten Releasecommit.

### Ergebnisse

| Prüfung | Tatsächliches Ergebnis |
| --- | --- |
| Optimierter Root-Reactor mit Produktion und Coverage | Bestanden; Shared vor seinen Verbrauchern gebaut |
| Anwendungs-JUnit | 3.129 bestanden, keine Fehler; zwei optionale IDE-/Caddy-HTTPS-Prüfungen übersprungen |
| Browser-Helfertests | 142 bestanden, keine Fehler oder Skips |
| Frontend-Offlineprüfungen | 20 bestanden |
| Coverage-/Sonar-Pythonprüfungen | 18 beziehungsweise 49 bestanden |
| Release-Helfertests | 14 bestanden; eigene temporäre Git-Repositories und synthetische JARs |
| Browser Deutsch, gesamte Suite | 386 von 386 bestanden, keine Skips |
| Browser Englisch, gesamte Suite und gezielter Nachtest | Alle 386 Fälle erfolgreich abgedeckt; ursprünglicher Gesamtlauf 385/386, Nachtest siehe unten |
| Aufbau, Abbau und HTML-Berichte | Für beide Gesamtläufe und den Nachtest bestanden |
| Java-, Python- und CSS-Stilprüfungen | Spotless, Checkstyle, Ruff, Prettier und Stylelint bestanden |

Die zwei optionalen JUnit-Sonderprüfungen für die reale IDE-/Caddy-Umgebung wurden
nicht aktiviert. Die isolierten HTTPS-Browserfälle waren Bestandteil der Gesamtrunde.

DE und EN verwendeten dasselbe unveränderte Image:

`sha256:807e7ad79b13fd907f254d899a5a0aa25082de9f4e37565144676348e84ba195`

Die Gesamtläufe nutzten je vier eigene Testumgebungen, innerhalb jedes Workers
sequenziell. Geprüft wurde mit Chromium auf Desktop und emulierten Smartphones.
Die Hintergrundprüfung umfasst 1440, 1280, 1100 und 390 px sowie reduzierte Bewegung.

### Englischer Browser-Nachtest

Im englischen Gesamtlauf brach MFA-03 bei 1280 px während der Navigation mit
`net::ERR_NO_BUFFER_SPACE` ab. Die übrigen 385 Fälle bestanden. Eine anschließende
Ressourcenaufnahme zeigte keinen anhaltenden Speicher- oder Verbindungsengpass;
die genaue Ursache des einmaligen Transportabbruchs ist nicht belegt.

Die vorhandene Suite `two-factor-configuration` wurde danach mit frischem Browser
und eigener neuer Testumgebung vollständig in EN wiederholt: **2/2 bestanden**
bei 1280 und 390 px, ohne Änderung an Anwendung, Tests oder Image.
Der Abgleich von vollständigem Testnamen und Parameteranzeige bestätigt, dass der
zuvor abgebrochene Fall enthalten ist. Zusammen sind damit alle 386 englischen
Fälle am finalen Image erfolgreich abgedeckt; der ursprüngliche Gesamtlauf wird
nicht nachträglich als fehlerfrei ausgegeben.

## Sonar und Coverage

- Analyse `8da158f2-42a7-4e72-b55c-f6708adab628` gehört exakt zum Releasecommit.
- **Quality Gate OK, null offene Issues und null Hotspots.**
- Die 46 bestehenden Ausnahmen blieben unverändert; keine neuen Suppressions,
  Statusausnahmen oder Scope-Einschränkungen.
- Umfang: 604 Java-Dateien, vier Maven-POMs und alle 32 eigenen CSS-Dateien.
- Sonar-Coverage: 83,0 % insgesamt, 86,0 % Zeilen, 73,7 % Branches.
- Gemeinsamer lokaler Coverage-Prüfer und wirksames Maven-Modell bestanden;
  lokale Zeilenabdeckung der Anwendung 85,29 %, Shared 100 %.

## Geprüfte Änderungen und Korrekturen

- **Versionierung:** Einheitliche Modulversionen und interne Abhängigkeiten,
  ausführbares `app.jar`, Build-Herkunft, DE-/EN-Changelog und lokaler Release-Ablauf.
  Das tatsächlich im Browser getestete JAR wurde aus dem Image übernommen,
  auf Version, saubere Git-Revision und SHA-256 geprüft und mit Manifest archiviert.
  Tag und nächster Patch-Snapshot wurden anschließend mit dem neuen Helfer erstellt.
- **Aura:** Native Reiter und Overlays, Icon-Zentrierung, Filter-Chips, Feldfehler,
  deaktivierte Felder, Kalender, erneut geöffnete Zeitauswahl und Hintergrund geprüft.
  Die Dauerfeldtests lesen die Meldung am äußeren CustomField; der alte
  Dauerfeld-Nachtest ist in DE und EN bestanden.
- **Darstellung korrigiert:** Kalenderaktionen überstimmen nun die allgemeine
  Buttonfläche; deaktivierte Textbereiche nutzen dieselbe Text-/Platzhalterfarbe
  wie die übrigen Felder; normale Aktionsbuttons haben die vereinbarte weiße Schrift.
- **Testtreiber präzisiert:** Kurzlebige Queue-Meldung vor den längeren Aufnahmen
  prüfen, mobile Kalenderbreite nativ verteilen lassen, Checkbox-/Schalterzustand
  am tatsächlichen Input und Marker prüfen und das geladene Pfeil-SVG absichern.
  Die gezielten Aura-Fälle bestanden vor den abschließenden Gesamtläufen.
- **Fachliche Regressionen:** Anmeldung, Registrierung, Verifikation, Anmeldeanbieter,
  Rollen und administrative Sperre, 2FA und Wiederherstellung, Sitzungen,
  Kontolebenszyklus, Benachrichtigungen, Konfiguration und Mailqueue einschließlich
  Warten auf Versand und vollständiger Bereinigung über mehrere 100er-Pakete.

## Abschluss und Grenzen

Erledigte Aura-/Releasepläne und ihre Verweise sind entfernt. Der bestandene
Dauerfeld-Nachtest sowie die abgedeckten Aura- und Queue-Einträge sind aus
[BUGS](../BUGS.md) und [Testbedarf](testbedarf.md) entfernt.

Echte Mobilgeräte, reale Passwortmanager, Screenreader, Systemscrollleisten sowie
echte Anbieter-/SMTP-Integrationen bleiben gesonderte menschliche Prüfungen im
[manuellen Katalog](manual-tests/README.md). Die automatisierten Läufe verwendeten
ausschließlich eigene isolierte Daten und Testdienste; bestehende Entwicklerdaten,
gespeicherte Passwörter und echter Mailversand wurden nicht verwendet.

Der Release ist lokal archiviert. Bedienung und Grenzen stehen unter
[Versionierung und Releases](releases.md). Die sichtbare Neuigkeiten-Seite und die
übrigen [offenen Aufgaben](../TODO.md) sind keine Bestandteile dieses Abschlusses.
