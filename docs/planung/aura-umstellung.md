# Umstellung auf Vaadin Aura mit neu gegliedertem Cosmos-Theme

Branch: `design/aura-theme`. Auftrag des Benutzers vom 17.11.2026.

## Ziel und Grenzen

- Basis wechselt von Lumo auf das Vaadin-Theme **Aura** (`aura/aura.css`,
  Vaadin 25.3.5). Der Name **Cosmos** bleibt für das eigene Theme erhalten.
- Die Anwendung sieht danach **wie bisher** aus (dunkles Schema). Erlaubt sind nur
  kleine Abweichungen durch vereinheitlichte Farben, Schriftgrößen oder Kontrastkorrekturen;
  jede bewusste Abweichung steht in diesem Plan bzw. danach in der Theme-Anleitung.
- Dark Mode bleibt Standard. Aura arbeitet mit `light-dark()`; die Tokens werden so
  angelegt, dass ein späterer Light-Modus mit Benutzerwahl ergänzt werden kann.
  Der Light-Modus selbst ist **nicht** Teil dieses Auftrags.
- Schrift: Inter, Ersatz Roboto/Systemschrift; Space Mono für Codes, Schlüssel und
  E-Mail-Adressen in Codeoptik. Markenvorgaben (Google-Schrift im Google-Button) bleiben.
- Lumo-Bezüge im Java-Code werden mit umgestellt. Browsertests werden im Nachgang
  angepasst; bis dahin dürfen sie an geänderten Klassen/Varianten scheitern.
- Modernisierungs- und Barrierefreiheitsideen, die das Aussehen spürbar ändern würden,
  werden nur gesammelt (Abschnitt „Spätere Verbesserungsvorschläge“), nicht umgesetzt.
- Freigaben: eigener Branch, App-Start mit isolierten Testdaten, Screenshots, Einführung
  von Stylelint/Prettier für CSS. Commits nur auf ausdrücklichen Auftrag.

## Ausgangslage

| Kennzahl | Wert |
| --- | --- |
| CSS-Dateien unter `META-INF/resources/custom-theme/` | 33, zusammen ca. 460 KB |
| Laden | `Application` (10 globale), ~40 Klassen per `@StyleSheet` |
| `var(--lumo-*)`-Verwendungen | 257 (47 verschiedene; 13 davon nur Lumo-Standardwerte) |
| Verschiedene Hex-Farben / `rgba()` | 141 / 85 |
| `!important` | 65 |
| Schriftgrößen | 16 feste px-Werte (9–30 px) neben der Skala `--cosmos-font-size-*` |
| Schriftstärken | 400, 450, 500, 550, 600, 630, 650, 700 |

Zwei Paletten existieren parallel: die ältere **Cosmos**-Palette (Indigo/Lavendel,
z. B. `#526ebc`, `#a9c0fb`) und die neuere **Reference**-Palette (Slate/Himmelblau,
z. B. `#0088cc`, `#38bdf8`). Sichtbar ist überwiegend Reference, weil
`TrackedUserAppLayout` und `PublicPageComponent` sie setzen. Die alten Werte schlagen
an nicht überschriebenen Stellen durch, etwa `--lumo-primary-color-10pct/50pct` bei
Badges, Panelicons und Avatarrändern. Java enthält Lumo-Bezüge: `ButtonVariant.LUMO_*`,
`LumoUtility`, `var(--lumo-success-color)` in `UserStatus`, `/lumo/`-Pfade in
`SecurityConfig`, `MaintenanceHttpFilter`, `TwoFactorHttpFilter`.

## Zielstruktur

Ladereihenfolge in `Application`: Aura → Cosmos-Schichten. Jede Datei hat genau
eine Zuständigkeit und beginnt mit einem kurzen Kopfkommentar.

| Schicht | Inhalt |
| --- | --- |
| 1. Fonts | `@font-face` Inter/Space Mono |
| 2. Palette | Rohfarben (Neutral-, Akzent-, Statusfarben) – einzige Stelle für Hexwerte |
| 3. Semantische Tokens | Text, Flächen, Rahmen, Akzent, Status, Fokus; Abbildung auf `--aura-*`/`--vaadin-*` |
| 4. Skalen | Schrift, Abstände, Radien, Größen, Schatten |
| 5. Vaadin-Bausteine | Buttons, Felder, Grid, Dialoge, Tabs, Badges … (gemeinsam) |
| 6. App-Rahmen | Kopf, Seitenrahmen, Navigation, Hintergrund |
| 7. Fachbereiche | Konto, Admin, Benachrichtigungen, 2FA, öffentliche Seiten |

Regeln: keine Hex-/RGBA-Werte außerhalb der Palette (Ausnahme: dekorative Hintergrundszene),
keine `--lumo-*`-Variablen, `!important` nur mit begründendem Kommentar, keine
doppelten Selektoren über Dateien hinweg.

## Farbvorschlag (eine Palette)

Grundlage ist die sichtbare Reference-Palette. Alte Indigo-Werte entfallen.
Kontraste nach WCAG 2.x (normaler Text ≥ 4,5:1, große Schrift/Bedienelemente ≥ 3:1).

| Rolle | Neu | Ersetzt | Kontrast |
| --- | --- | --- | --- |
| Seite | `#050811` | `#03050b`, `#040710`, `#02040a` (außer Szene) | – |
| Panel | `#080e1b` | `#0b1220`, Card-Verlauf alt | – |
| Eingabe | `#091120` | – | – |
| Dialog/Overlay | `#0e1726` | `#11192a`, `#0f172a` | – |
| Hover | `#152238` | – | – |
| Auswahl | `#122342` | `#1b2b42` (Tabelle) | – |
| Text | `#e2e8f0` | `#edf2ff`, `#e4edfb` | 15,6 auf Panel |
| Überschrift | `#ffffff` | `#f8fafc`, `#f3f6ff` | – |
| Label | `#cbd5e1` | – | 13,0 |
| Sekundär | `#94a3b8` | `#a7b7d2` | 7,5 |
| Gedämpft | `#7c8aa0` **(angehoben)** | `#64748b`, `#899ab8`, `#748199` | 5,4 auf Eingabe (alt 3,97 ✗) |
| Akzent (Text/Icon) | `#38bdf8` | `#a9c0fb`, `#7dd3fc` | 9,0 |
| Primary-Fläche | `#0077b6` **(abgedunkelt)** | `#0088cc`, `#526ebc`, Indigo-Verlauf | Weiß 4,9 (alt 3,89 ✗) |
| Primary-Hover | `#00679e` | `#0078b4` | Weiß 6,0 |
| Erfolg | `#34d399` | `#468c69`, `#a3d5b8` | 10,0 |
| Warnung | `#fbbf24` | `#f59e0b`, `#c99950`, `#efcb8a` | 11,6 |
| Fehler | `#f87171` | `#bc596e`, `#efa4b1`, `#e05a6c` | 7,0 |
| Fehler-Text hell | `#fca5a5` | – | – |
| Zählerbadge | `#b4233b` | – | Weiß 6,5 |
| System (Violett) | `#a855f7` / Text `#d8b4fe` | – | Text 9,5; Weiß auf Violett 3,96 → nur als Rahmen/Akzent |

Transparente Flächen (10 %/20 %/50 %) werden per `color-mix()` aus diesen Farben
gebildet, statt eigene RGBA-Werte zu pflegen. Die dekorative Hintergrundszene
(Sterne, Horizont, Mond) behält eigene Verlaufswerte, gebündelt in einer Datei.

## Typografieskala

| Token | Wert | Einsatz |
| --- | --- | --- |
| `2xs` | 10 px | Großbuchstaben-Labels (Eyebrows, Kennzahlbeschriftungen) |
| `xs` | 11 px | Badges, Metadaten, Tabellenköpfe |
| `s` | 12 px | Labels, Hilfetexte |
| `m` | 13 px | Fließtext in Panels, Eingaben, Buttons |
| `l` | 14 px | Hervorgehobener Text, Navigation |
| `xl` | 15 px | Bestätigungsfragen |
| `2xl` | 16 px | Marke, Abschnittstitel |
| `title` | 18 px | Panel- und Dialogtitel |
| `heading-s`/`heading-m`/`heading-l` | 20/24/30 px | Seiten-Überschriften (mobil/Desktop) |
| `3xl`/`display` | 28/32 px | Große Symbolschrift, Hero-Zahlen |

Stärken: 400 regulär, 500 mittel, 600 halbfett, 700 fett; 450/550/630/650 sind gerundet.
Die frühere Vorgabe „9/10 px auf 11 px anheben“ wurde verworfen: Die Großbuchstaben-Labels
behalten 10 px als eigene Stufe, damit sich Breiten und Umbrüche nicht ändern.

## Werkzeuge

Prettier formatiert CSS einheitlich, Stylelint prüft Regeln (unbekannte Eigenschaften,
doppelte Selektoren, Hexwerte außerhalb der Palette, keine `--lumo-*`).
Einbindung als npm-Entwicklungsabhängigkeit im `app`-Modul; Befehle stehen nach
Einrichtung in der [Entwicklungsanleitung](../entwicklung.md).

## Arbeitsschritte und Stand

- [x] Branch `design/aura-theme` angelegt, Bestand erfasst (siehe oben).
- [x] Vorher-Aufnahmen: 36 ganzseitige Screenshots 1440×900 und 390×844 (DE, isolierte
      H2, Superadmin mit echter 2FA-Einrichtung). Ablage nur temporär im Scratchpad;
      jederzeit von `main` reproduzierbar (siehe Wiedereinstieg).
- [x] Farb- und Schriftvorschlag mit Kontrastprüfung (dieser Plan).
- [x] Aura laden, Lumo entfernen; fehlende Lumo-Standardwerte als Cosmos-Tokens anlegen;
      alle `var(--lumo-*)` ersetzen; Java-Bezüge (`LumoUtility`, `ButtonVariant.LUMO_*`,
      `UserStatus`, Sicherheits-Pfade) umstellen. Ersatzklassen in `CosmosCssClasses`
      (`INLINE_BUTTON`, `ICON_BUTTON`, `EQUAL_WIDTH_TABS`, `TEXT_*`); Grid-Zeilenfarben über
      öffentliche `--vaadin-grid-row-*`-Variablen und den Part `odd-row-cell`.
- [x] Nachher-Aufnahmen gegen Vorher vergleichen und Abweichungen beheben (43 Aufnahmen
      inkl. Overlays). Behoben: AppLayout-Rahmen, Reiterleiste, Pflichtfeldpunkt,
      Feldsymbolgröße, Schalterraster, Haken in Checkboxen, doppelte FormLayout-Abstände,
      Einrückung in Aufklappbereichen, Grid-Zeilenhöhe. Neue Aura-Anpassungen stehen
      vorläufig am Ende von `cosmos-theme.css` (Abschnitte 9 und 10) und werden beim
      Neugliedern einsortiert.
- [x] Palette konsolidieren: neue Datei `cosmos-palette.css` (einzige Stelle für Farbwerte,
      geladen direkt nach Aura). Semantische Tokens und frühere Referenzwerte verweisen
      darauf; alle Literale in Fachdateien sind durch Palettenfarben bzw. `color-mix()` ersetzt.
      Ausnahmen: dekorative Hintergrundszene und Google-Markenfarben. Die beiden parallelen
      Tokenfamilien (`--cosmos-*` und `--cosmos-reference-*`) haben jetzt identische Werte;
      ihre Zusammenführung zu einer Familie gehört zum Neugliedern.
- [x] Typografie auf die Skala bringen: alle `font`/`font-size`/`font-family`-Angaben
      verwenden `--cosmos-font-size-*`, `--cosmos-font-family` (mit Roboto-/Systemersatz)
      bzw. `--cosmos-font-family-mono`. Space Mono ist jetzt global in `typography.css`
      definiert (vorher nur auf 2FA-Seiten geladen, sonst Ersatzschrift Consolas).
      Stärken auf 400/500/600/700 gerundet. Einzelwerte gerundet: 9→10, 17→18, 21→20,
      22→20, 25→24 px. Zeilenhöhen sind noch Einzelwerte (nächster Schritt beim Gliedern).
- [ ] CSS-Dateien nach Zielstruktur neu gliedern, Duplikate und ungenutzte Regeln entfernen,
      `!important` abbauen.
- [ ] Prettier/Stylelint einrichten, alle CSS-Dateien formatieren und prüfen.
- [ ] Abschlussvergleich Desktop/Smartphone, `docs/theme.md` auf den neuen Stand bringen,
      Plan entfernen bzw. Restpunkte übernehmen.

## Wiedereinstieg

**Nächster Schritt: Neugliedern.** Reihenfolge:

1. `--cosmos-reference-*` und die gleichwertigen `--cosmos-*`-Rollen zu einer Familie
   zusammenführen (gleiche Werte, siehe `cosmos-tokens.css`); danach den Referenzblock
   samt `cosmos-reference-portal`/`-overlay`-Doppelungen in Selektoren abbauen.
2. Aura-Anpassungen (Abschnitte 9/10 am Ende von `cosmos-theme.css`) in eine eigene
   Datei `cosmos-aura.css` direkt nach den Tokens verschieben.
3. Fachdateien zusammenfassen, wo nur kleine Fragmente existieren (`cosmos-native-card`,
   `cosmos-public-card`, `cosmos-content-card`, `cosmos-icons`, `cosmos-mfa-otp`), und
   `cosmos-admin-reference.css` (Einzeilerstil) in `cosmos-admin.css`/`admin-users.css`
   aufteilen. Versteckte `@import`s in `cosmos-theme.css` durch `@StyleSheet` ersetzen.
4. Danach Prettier/Stylelint einführen und alle Dateien formatieren; Stylelint-Regeln:
   keine Hexwerte außerhalb `cosmos-palette.css` und Hintergrunddateien, keine `--lumo-*`,
   `declaration-block-no-duplicate-properties`, `no-duplicate-selectors`.
5. Nach jedem Schritt Vergleichsaufnahmen gegen `main`; Zeilenhöhen vereinheitlichen.

**Werkzeuge (temporär, im Scratchpad der Sitzung; bei Verlust neu anlegen):**
`Shots.java` (Playwright-Aufnahmen inkl. 2FA-Ersteinrichtung und sieben Overlays),
`Diff.java` (Pixelvergleich mit Vergleichsbildern vorher|nachher|Differenz),
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

## Akzeptierte Abweichungen durch Aura

Vom Benutzer freigegeben: kein pixelgenauer Nachbau; Aura-Standards dürfen bleiben,
wenn sie nicht stören.

- Eingabefelder sind teils 2 px niedriger (vom Benutzer ausdrücklich akzeptiert).
- Feldsymbole stammen aus Aura (u. a. anderes Kalendersymbol), Größe auf ca. 16 px abgeglichen.
- Überschriften brechen mit `text-wrap: balance` ausgewogener um.
- Checkboxen 16 statt 18 px; Buttons mit Aura-Innenabstand teils 2–4 px breiter.
- Textbereich und Mehrfachauswahl verwenden denselben dezenten Rahmen wie alle übrigen Felder.

## Spätere Verbesserungsvorschläge

Hier sammeln, was beim Umbau auffällt, aber das Aussehen spürbar ändern würde.

- Seitenbeschreibung rechts oben läuft unter den Mond-Dekor (z. B. „Melden Sie sich …“);
  Text wird verdeckt.
