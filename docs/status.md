# Große Testrunde – 01.11.2026

## Prüfstand und Ergebnis

Die technische Integration der Google-/Microsoft-Anmeldung ist lokal auf `main`
übernommen und automatisiert geprüft. Der Review-/Featurecommit ist `6cfea564`,
die Sonar-Korrekturen sind `31ed947`, `1b05f96` und `ad594c8`.
Der vollständig geprüfte Browser-Teststand ist
`18d4d3d6a8b21d5aeffb2eb69b9a30158fe9cc57`.

Die Anwendung selbst entspricht `ad594c845115373383a5811b9c72426222a91071`.
Der folgende Commit änderte ausschließlich zwei Browsertests. Anwendung, Shared,
Root-POM und Dockerfile blieben nachweislich unverändert. Nachfolgende Änderungen
an Prüfstand und Aufgabenlisten ändern diesen Anwendungs-/Teststand ebenfalls nicht.
Die zugehörigen Commit-, Quellen- und Runzuordnungen stehen im lokalen QA-Zustand.

Die nachfolgenden UI- und Sprachkorrekturen sind in `3a57a2b` übernommen und gezielt
geprüft. Sie gehören nicht zum Quellstand dieser letzten großen Testrunde. Der
Benutzer hat die Google-/Microsoft-Umsetzung inzwischen abschließend abgenommen.

Auf `codex/zwei-faktor-authentifizierung` ist inzwischen die 2FA-Erweiterung technisch
umgesetzt. Sie gehört nicht zum nachfolgenden Main-Prüfstand. Ihre gezielten
Nachprüfungen und die noch offene gemeinsame Abnahme werden im
[aktiven Plan](planung/zwei-faktor-bestaetigung.md) abgegrenzt. Die dort behobene
Google-Schwäche wird unten ausschließlich als Grenze der damaligen Main-Baseline beschrieben.

Ausgangspunkt war der vorher dokumentierte Featureprüfstand auf Basis `782a9719`
samt damaligen uncommittierten Änderungen. Eine vollständig bestätigte Main-Baseline
lag noch nicht vor. Maßgeblich waren deshalb der tatsächliche Diff, die vorhandenen
Regressionen und die jetzige vollständige Prüfung des integrierten Stands.

**Ergebnis:** Beide vollständigen Browserrunden sind bestanden. Keine ungeklärten
Testfehler bleiben zurück. Die bekannte, ausdrücklich akzeptierte Google-Schwäche
bleibt für die separate 2FA-Umsetzung offen. Die Designentscheidung und der
Featureabschluss sind erfolgt. Es wurde nicht gepusht; die Quellbranches bleiben erhalten.

## Ausgeführte Prüfungen

| Prüfung | Beobachtetes Ergebnis |
| --- | --- |
| Vollständiger optimierter Root-Reactor mit Production und Coverage | Erfolgreich unter JDK 25 |
| Anwendungstests | 2.437 bestanden, zwei erwartete optionale Skips |
| JUnit-Browser-Helfer | 85 bestanden |
| Browser `all`, Deutsch, finaler vollständiger Lauf | 208/208 bestanden, keine Skips |
| Browser `all`, Englisch, finaler vollständiger Lauf | 208/208 bestanden, keine Skips |
| Frontend-JavaScript | 12 bestanden |
| Sonar-Offlineprüfungen | 47 bestanden |
| Coverage-Offlineprüfungen | 18 bestanden |
| Spotless und Checkstyle | Erfolgreich, keine Checkstyle-Verstöße |
| Ruff für die fünf gepflegten Python-Helfer | Formatprüfung und Lint erfolgreich |
| Isolierter Start des frisch gebauten JARs | Anwendung und frisches Schema initialisiert, Login erreichbar |

Der geschützte Sonar-Einstieg führt einen vollständigen `clean install` mit
`production,coverage` und `vaadin.productionMode=true` aus. Dieser enthält den
vollständigen Verify-Lauf; ein zweiter identischer Build ist dafür nicht erforderlich.
Wirksames Maven-Modell und gemeinsamer Coverage-Prüfer bestätigten den Bezug von
Quellen, Klassen und Messdaten. JaCoCo-Zeilenabdeckung: **88,62 % App**, **100 % Shared**.
Die zwei Skips betreffen ausschließlich die opt-in IDE-/Caddy-Prüfungen in
`IdeHttpsProxyTest`. Shared wird über die Anwendung und Helfertests mitgeprüft.
Es wurde keine zusätzliche serverseitige Browser-Coverage erhoben.

Beide finalen Sprachläufe verwendeten unverändert dasselbe Anwendungsimage:
`sha256:a865aa8bf04971c418538afbc8028962da12f57662769efd80ca5fc354682743`.
Je Sprache liefen 32 Testklassen mit vier getrennten Workern. Jeder Worker hatte
seine eigene JVM, H2-Datenbank, Anwendung, HTTPS-Umgebung und Mailpit-Instanz.
Innerhalb eines Workers lief JUnit sequenziell. Desktop und die vorhandenen
Smartphonefälle wurden in Chromium geprüft. Die lokalen HTML-Referenzen gehören
zu den bestehenden Layout-/Themefällen. Repräsentative Desktop-/Smartphonebilder
wurden zusätzlich gesichtet; dies ersetzt keine menschliche Gesamtabnahme.

Alle eigenen Browserumgebungen wurden erfolgreich abgebaut. Die vorherigen Runs
und ihre Diagnosedaten bleiben erhalten. Entwicklerdaten, echte Google-/Microsoft-
Konten und externe SMTP-Ziele wurden nicht als Fixture verwendet. Der eingefrorene
OpenCode-Workflow wurde weder geprüft noch verändert.

## Korrekturen und bestätigte Nachtests

Die Sonar-Bereinigung behob unter anderem fehlende Serialisierbarkeit kurzlebiger
Sitzungsnachweise, den unkontrollierten Nullfall beim OIDC-Issuer und uneindeutige
interne Transaktionsaufrufe. Anbietertoken unterscheiden bei Gleichheit auch ihren
Anbieter. Unbenutzter Sicherheitscode entfiel. UI-Abläufe und lange Tests wurden
gegliedert, wiederkehrende Bezeichner zentralisiert und Bildwerte für Arrayinhalte
konsistent gemacht. Bestehende Zugriffs-, Widerrufs-, Zweck- und Versionsprüfungen
blieben erhalten. Neue Regressionen prüfen Sitzungsserialisierung, fehlenden Issuer,
Anbietergleichheit und das Entfernen einer leeren Autorisierungsanfrage.

Der erste deutsche Gesamtlauf bestand **206/208 Fälle** und zeigte zwei Testfehler:

- **LANG-02A:** Der Test navigierte nach einem Klick auf den übersetzten Logoutbutton
  weiter, bevor die echte Abmeldeantwort abgeschlossen war. Der Trace belegt die
  Überlappung. `UiActions.complete` wartet jetzt die tatsächliche Antwort ab.
  Loginroute und gespeicherte Sprache werden weiterhin unverändert geprüft.
- **AVATAR-01 Desktop:** Icon und Hinweistext wurden während der Öffnungsanimation
  in verschiedenen Frames gemessen. Beide Mittelpunkte werden jetzt atomar im
  selben Browserausdruck gelesen. Die Toleranz bleibt unverändert bei einem Pixel.
  Styling und Animation wurden nicht verändert.

Beide Korrekturen stehen in `18d4d3d`. Anschließend wurden **beide vollständigen
208-Fälle-Runden** erfolgreich ausgeführt. Der Sprachfall sowie die Profilbildfälle
auf Desktop und Smartphone bestanden in DE und EN. Keine Assertions wurden
abgeschwächt und keine Wiederholungsversuche eingebaut. Der fehlerhafte Erstlauf
bleibt als Diagnose erhalten; seine erfolgreichen Teilfälle wurden nicht als
Ersatz für die finalen Gesamtläufe ausgegeben.

## Sonar und offene Grenzen

Der geschützte Main-Scan war nach der vollständigen Bereinigung grün: **0 offene
Issues, 0 Bugs, 0 Vulnerabilities und 0 Hotspots**. Ausgangspunkt waren 95 offene
Issues. Scope, Regeln und Quality Gate blieben unverändert. Die bestehenden
48 Ausnahmen blieben erhalten. Drei weitere Hinweise sind direkt am Finding mit
konkreter deutscher Begründung als False positive dokumentiert und nachgelesen:
interne Spring-Passwortfehlergrenze, geerbte Spring-Benutzeridentität und die native
Vaadin-Vererbung des gemeinsamen Seitenrahmens. Ausnahmen wurden nicht als
Codebehebung gezählt. Die vollständige Analyse-/Findingzuordnung steht im einzigen
aktuellen lokalen Sonar-Bericht unter `.codex/sonar/bericht.md`.

**Bekannte Google-Grenze:** Eine offene Portal- und Google-Sitzung kann einen neuen
Google-Durchlauf ohne erneute Passwort-/Faktoreingabe abschließen und damit eine
sensible Kontoaktion bestätigen. Dies ist der ausdrücklich genehmigte Zwischenvertrag.
Konto-, Sitzungs-, Zweck-, Token- und Einmalprüfungen sind weiterhin aktiv und geprüft.
Ziel der nächsten separaten Umsetzung ist verbindliche eingerichtete 2FA ohne
Umgehung über Google oder Portalpasswort. Risiko, Reproduktion und Lösungsrichtung
stehen in [BUGS](../BUGS.md) und [Externe Anmeldung](externe-anmeldung.md).
Der [2FA-Testbedarf](testbedarf.md) bleibt wartend erhalten.

Die [gespeicherte Stitch-Kritik](design/stitch-kritik-anmeldeanbieter-anmeldung.md)
ist entschieden. Die ausgewählten Korrekturen sind umgesetzt und das Feature ist
abgenommen. Die bekannte Google-Schwäche bleibt davon unabhängig offen.

Echte private sowie Firmen-/Schulkonten bei Microsoft, weitere echte Anbieter-
Sonderfälle, Mobilgeräte, Bildschirmtastatur, Zoom und Screenreader bleiben im
[manuellen Katalog](manual-tests/README.md#hum-08). Provider-Smokes und echter
Mail-Sichtversand gehören nicht zu dieser automatisierten Runde.

Für eine bereits laufende Entwicklerinstanz sind ein aktueller Build und Neustart
nötig. Es wurde keine Entwickleranwendung eigenmächtig neu gestartet. Offene Arbeiten
stehen in [TODO](../TODO.md) und im [2FA-Plan](planung/zwei-faktor-bestaetigung.md).
