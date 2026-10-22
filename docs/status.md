# Aktueller Prüfstand

Stand: 23.10.2026. Ausschließlich lokale Entwicklung mit austauschbaren Testdaten.

## Ergebnis der gemeinsamen Funktionsprüfung

Arbeitspaket 6 des Multi-Modul-Umbaus ist vollständig geprüft. Der optimierte
Reactor-Build, die gepflegten Offlineprüfungen, der isolierte JAR-Start und Browser
`all` auf Deutsch und Englisch sind bestanden. Beide Sprachläufe einschließlich
Aufbau und Abbau endeten mit Exitcode 0; Zertifikatsdialoge waren nicht nötig.

Geprüfter Codestand: `feature/multi-modul-sonarqube`, Commit
`e9f5884ff923118cf2815380e37e55e8f53cd377`, mit den uncommittierten Dokumentations-
und Pfadkorrekturen dieses Pakets. Anwendungs-, Shared- und Browserquellen sowie
Buildkonfiguration wurden während der Prüfung nicht verändert. Diese Prüfung
ersetzt den früheren Gesamtprüfstand, ist aber kein Sonar-Servernachweis.

| Prüfung | Tatsächliches Ergebnis |
| --- | --- |
| Java 25, Spotless-Check und Checkstyle | Bestanden; keine Checkstyle-Verstöße |
| Optimierter vollständiger Reactor mit `production,coverage` | `clean install` erfolgreich, alle fünf Reactorprojekte einschließlich Berichtsmodul erfolgreich |
| Anwendungs-JUnit | 2.246 erfasst, 2.244 bestanden, 0 Fehler, 2 erwartete optionale IDE-/Caddy-Skips |
| Shared | Im Reactor gebaut und installiert; keine eigenen Testquellen, Ausführung durch App-/Helfertests gemessen |
| Browser-Helfertests | 83 bestanden, keine Fehler oder Skips |
| Sonar-Python-Helfer | 44 bestanden, einschließlich künstlicher DPAPI-Testwerte; kein Serverzugriff |
| Coverage-Python-Helfer | 18 bestanden |
| Frontend-JavaScript | 12 bestanden |
| Ruff-Formatprüfung und Lint der gepflegten Sonar-/Coverage-Dateien | Bestanden |
| Gemeinsame JaCoCo-Coverage | Wirksames Maven-Modell und aktueller Bericht geprüft; App 87,53 % Zeilenabdeckung, Shared 586/586 gedeckte Zeilen |
| Gepacktes JAR | Frischer isolierter Start und Prozessabbau bestanden; Healthcheck 200, direkter HTTP-Login 403, Zugriff mit vertrauenswürdigen Proxyheadern 200, keine SMTP-Verbindung |
| Aktuelles Dockerimage | Über den Reactor gebaut; beide Sprachläufe und alle vier Worker bestätigen dieselbe feste Image-ID |
| Browser `all` DE | 161/161 bestanden; 2 Worker, keine Fehler oder Skips, Gesamtexit 0 |
| Browser `all` EN | 161/161 bestanden; 2 Worker, keine Fehler oder Skips, Gesamtexit 0 |
| Eigene Testressourcen | Alle vier Workerprojekte ohne verbliebene Container, Volumes oder Netze; keine privaten Worker-Schlüssel verblieben |
| Windows-Zertifikatsvertrauen | `CurrentUser/Root` und `CurrentUser/CA` vor/nach beiden Läufen identisch; dauerhafte Test-CA absichtlich erhalten |

Das gemeinsame Produktionsimage ist
`sha256:52ec870d64199a10e7fc873ad28328e7efed6269dcf80d9c11b0c566fec42407`.
DE hat den aktuellen Imagebuild aufgerufen; EN verwendete ausdrücklich diese
unveränderte ID ohne Neubau. Innerhalb jedes Workers lief JUnit sequenziell;
Datenbank, Mailpit, TLS-Identität und Browserprofile waren je Worker getrennt.
Die aktuellen lokalen Laufbelege sind über die Metadaten unter
`browser-tests/runs/` nachvollziehbar; keine dauerhafte Berichtserie.

## Geprüfte Verträge und Korrekturen

- Root-Reactor, App-Artefakt unter `app/target/`, Shared-JAR und originale
  Übersetzungs-/Mailressourcen funktionieren im gemeinsamen Aufbau. Alte Rootordner
  `src` und `node_modules` sind durch den Host-Build nicht erneut entstanden.
- Anleitungen und Projektregeln verwenden die aktuellen App-/Frontend-/Ressourcenpfade.
  Gezielte manuelle Mail- und TLS-Testaufrufe wählen ihren Verbraucher im Reactor.
  Die Dockeranleitung und die gepflegten Starter wurden mit den Quellen abgeglichen.
- Die Browserfälle bestätigen Ressourcen, tatsächliche englische/deutsche UI-Texte,
  Mail-Origins, HTTPS, sichere Cookies, Upload, Push, Konten, Rechte, Widerruf,
  Tokenablauf, Wartung sowie echte Browser- und Anwendungsneustarts.
- Der Sonar-Helfer und die gemeinsame Coverage-Vorprüfung sind offline abgesichert.
  Aktueller Bytecode, getrennte Quell-/Testverzeichnisse, beide Messsitzungen und
  gemeinsame App-/Shared-Zuordnung sind nach den Browserläufen erneut geprüft.
  Optionales Messen der echten Browserläufe wurde nicht aktiviert.
- Es waren keine Anwendungs-/Testkorrekturen oder abgeschwächten Assertions nötig.
  Entwicklerdaten und fremde Prozesse wurden weder verwendet noch verändert.

## Grenzen und bestätigte menschliche Abnahme

Die beiden optionalen echten IDE-/Caddy-Fälle wurden nicht erneut aktiviert.
Ihre bereits bestandenen isolierten Nachweise und die menschliche IDE-Abnahme
bleiben gültig: Start/Stop, harter Reset, Debug/Breakpoint und HotSwap sind bestätigt;
Start/Stop und Debug wurden nach der Maven-Neuverknüpfung mit Modul `app` bestätigt.
Der gesonderte TLS-Negativtest und Einrichtung/Rückbau der dauerhaften Test-CA
wurden in diesem Paket nicht wiederholt. Normale Läufe haben Vertrauen nur gelesen.

Echter Mail-Sichtversand, externe SMTP-Provider und der eingefrorene OpenCode-
Workflow gehören nicht zu dieser Prüfung. Mailpit belegt Annahme im eigenen
Testnetz, keine externe Postfachzustellung. Browserfälle ersetzen keine Abnahme
auf echten Mobilgeräten, mit Hilfsmitteln oder in realen Mailclients.

## Integration und gemeinsamer Sonar-Nachweis

Alle Module sind lokal nach `main` integriert. Geprüfter und hochgeladener Codestand:
`951452da7e89fc3f14db910f910734cfd5bf8008`. Serverauftrag
`e54b1666-44b9-4acb-b046-b6d8a386b6c0` ist SUCCESS; zugehörige Analyse
`226deebd-5a32-4336-8986-5deb9891eb71` vom 22.10.2026, 23:02:38 UTC.
Revision, Projektkey `vaadin-spring-app` und lokales main stimmen überein.

Der Serverumfang ist vollständig mit den lokalen Dateien abgeglichen:
188 App- und fünf Shared-Quellen, 117 App- und 77 Browser-Testquellen. Keine fehlende,
zusätzliche oder falsch klassifizierte Java-Datei; 387 Java-Dateien insgesamt.
Das technische Coverage-POM wird nicht als Anwendung analysiert. Gemeinsame JaCoCo-
Messdaten und wirksames Maven-Modell sind vor dem Upload erfolgreich geprüft.

Quality Gate **OK**: neue Coverage 82,2 % bei mindestens 80 %, neue Duplikation 0 %
bei höchstens 3 %, neue Issues 0 bei zulässig 0. Gesamtcoverage 85,5 %,
Zeilencoverage 88,1 %, Branchcoverage 77,3 %. Keine offenen Issues im Gesamtcode
oder New Code, keine offenen Bugs, Vulnerabilities oder Hotspots.

Die erstmals einbezogenen Shared-/Browser-Findings sind geprüft: 23 ursprüngliche
Hinweise und sieben nachfolgende Stilhinweise sind im Servernachscan als Fixed
bestätigt. Fünf Hinweise sind einzeln begründet Accepted, drei False positives;
die 37 bestehenden Ausnahmen behalten IDs, Status und Kommentare und sind korrekt
auf die verschobenen App-Dateien zugeordnet. Insgesamt bestehen 33 Accepted und
12 False positives. Bei der Finding-Bereinigung wurden Regeln, Profile, Analyseumfang,
Gate und Baseline nicht verändert.
Die sieben `var _`-Stellen sind durch Code behoben und nicht akzeptiert geblieben.

Auf diesem finalen Codestand sind zentraler Stil, optimierter vollständiger Reactor-
Install, 2.244 App-JUnit bei zwei erwarteten Skips und alle 83 Browser-Helfer bestanden.
Vor Integration waren 60 gezielte Regressionen erfolgreich; nach der `var`-Korrektur
acht CA-/Sperrfälle erneut. Der frische isolierte JAR-Start erreicht Health 200,
weist direkten HTTP-Login mit 403 ab und liefert mit vertrauenswürdigen Proxyheadern
200; kein SMTP-Verbindungsversuch, eigener Prozess vollständig beendet.

Dies ersetzt keine neue große QA: Die vollständigen DE-/EN-Läufe oben gehören zum
dort genannten früheren Codestand. Nach den verhaltensgleichen Sonar-Korrekturen
wurden alle Browserquellen kompiliert, keine Journeys erneut gestartet. Keine
Assertions, Sperren oder Abbaugrenzen entfernt. Die bekannten Java-25-Typauflösungs-/
Preview-Warnungen bleiben eine Scannergrenze; der Java-25-Build ist bestanden.

Der Benutzer hat den Multi-Modul-Umbau am 23.10.2026 insgesamt abgenommen und den
Abschluss beauftragt. Die erledigten Pläne und ihre Aufgaben-/Dokumentationsverweise
sind entfernt. Codeänderungen sind lokal in main committiert und integriert; die
abschließenden Dokumentationsänderungen bleiben zur Benutzerprüfung uncommittiert.
Kein Push erfolgt.

## Offene Fehler

Es sind keine bestätigten offenen Anwendungsfehler bekannt; siehe [BUGS.md](../BUGS.md).
Offener automatisierter Ergänzungsbedarf steht im [Testbedarf](testbedarf.md),
menschliche Sonderprüfungen im [manuellen Katalog](manual-tests/README.md).
