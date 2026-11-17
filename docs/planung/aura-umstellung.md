# Umstellung auf Vaadin Aura mit neu gegliedertem Cosmos-Theme – offene Punkte

Branch: `design/aura-theme`. Auftrag des Benutzers vom 17.11.2026.

Umgesetzt sind Aura als Basis, die eine Palette, die Typografieskala, zusammengeführte
Token-Familien, Prettier/Stylelint und die neu gegliederte Dateistruktur. Der gültige
Stand steht in der [Theme-Anleitung](../theme.md#basis-vaadin-aura-mit-cosmos-schichten)
einschließlich akzeptierter Abweichungen; Werkzeuge in der
[Entwicklungsanleitung](../entwicklung.md#css-prüfen-und-formatieren).

Grenzen bleiben: Die Anwendung sieht wie bisher aus; Änderungen, die das Aussehen spürbar
ändern würden, werden nur unten gesammelt. Ein Light-Modus ist nicht Teil des Auftrags.
Commits nur auf ausdrücklichen Auftrag.

## Offene Arbeiten

- [ ] Offene Fehler aus Paket 6 klären (Details in [BUGS](../../BUGS.md)): leere
      `errorMessage` im Dauerfeld. Browserfälle und JUnit-Tests sind an Aura angepasst;
      vollständige Läufe DE/EN am selben Image vom 17.11.2026: 372/380 bzw. 373/380
      bestanden, Rest siehe BUGS und [Testbedarf](../testbedarf.md). Die Scrollleisten sind
      inzwischen mit der vom Benutzer gewählten Variante „Himmelblau-Strich“ umgesetzt;
      COMPONENT-06 besteht in DE und EN.
- [ ] Regeln, die nur außerhalb der Portal-/Overlayrahmen greifen, auf Nutzung prüfen.
      Bisher sind nur Klassen ohne Vorkommen im Java-Code entfernt; alle sichtbaren Seiten
      tragen `cosmos-reference-portal` bzw. `-overlay`, ältere Grundregeln davor sind daher
      teils überschrieben.
- [ ] Abschlussvergleich Desktop/Smartphone gegen `main` (Lumo) nach Paket 6; danach diesen
      Plan entfernen und Restpunkte übernehmen.

## Wiedereinstieg

Vergleiche vorher/nachher bisher pixel- und stylegleich (zuletzt 46 Aufnahmen nach Paket 2).

**Werkzeuge (temporär, im Scratchpad der Sitzung; bei Verlust neu anlegen):**
`Shots.java` (Playwright-Aufnahmen inkl. 2FA-Ersteinrichtung und acht Overlays; die
Ersteinrichtung braucht eine frische H2-Datei, also vor jeder Aufnahmerunde neu starten),
`Diff.java` (Pixelvergleich mit Vergleichsbildern vorher|nachher|Differenz; `Shots.java`
schreibt zusätzlich berechnete Styles je Aufnahme, `sgroup.py` vergleicht sie gruppiert),
`restart.ps1` (isolierter Neustart, siehe unten), Referenz von `main` aus einem
temporären Worktree auf Port 8091.

- Testlauf ohne `.env`: `.\mvnw.cmd -pl app compile`, danach die Anwendung direkt mit
  `java` (Classpath `app/target/classes` plus
  `.\mvnw.cmd -pl app dependency:build-classpath`, `-Dvaadin.project.basedir=<app>`) aus
  einem eigenen leeren Arbeitsverzeichnis starten, Argument
  `--spring.config.additional-location=file:<design-run.properties>` (Port 8090, eigene
  H2-Datei, `app.bootstrap.*` mit den Testkonten aus `.env.example`, eigens erzeugte
  Testschlüssel für `app.security.totp-encryption-key` und
  `app.security.provider-secrets-encryption-key`). Grund: Die POM setzt das
  Arbeitsverzeichnis von `spring-boot:run` fest auf den Projektroot; dort lädt
  `spring.config.import` die lokale `.env`, und eine zusätzliche Konfigurationsdatei kann
  diesen Import nicht abschalten.
- Aufnahmen: Playwright-Java-Skript `Shots.java` (Classpath aus
  `.\mvnw.cmd -pl browser-tests dependency:build-classpath -Dmdep.includeScope=test`),
  `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1` setzen. Das Skript meldet den Superadmin an,
  durchläuft die 2FA-Ersteinrichtung und fotografiert Portal, Konto und alle Adminbereiche.
- Vorher-Stand bei Bedarf aus einem temporären Worktree von `main` neu erzeugen.

## Spätere Verbesserungsvorschläge

Hier sammeln, was beim Umbau auffällt, aber das Aussehen spürbar ändern würde.

- Seitenbeschreibung rechts oben läuft unter den Mond-Dekor (z. B. „Melden Sie sich …“);
  Text wird verdeckt.
