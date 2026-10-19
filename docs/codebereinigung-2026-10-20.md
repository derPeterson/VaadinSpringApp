# Code-, Sprach- und Theme-Bereinigung am 20.10.2026

Ausgangspunkt: sauberer Branch `feature/user-messages`, Commit
`b370692969b20184431b67b5a56caa47f298a07c`. Das vollständig bestätigte Redesign ist
enthalten. Die Bereinigung bleibt uncommittet; Index, Branch und Remote werden
nicht verändert.

## Umfang und Vorgehen

Geprüft wurden die handgeschriebenen Quellen von Anwendung, Shared und Browsertests:
339 Java-Dateien, davon 179 Produktionsdateien. Klassen-, Methoden- und Feldkandidaten
wurden gegen Aufrufer, Framework-Anbindung und Tests abgeglichen. Sprachkatalog,
Vorlagen, Konfiguration und die neun Stylesheets wurden zusätzlich auf Referenzen
untersucht. Abhängigkeiten/generierte Frontenddateien, reale Daten, `.env` und Logs
sind keine Bereinigungsziele. Ein fehlender direkter Texttreffer ist allein kein
Löschbeweis.

## Code

Entfernt wurden tatsächlich nicht mehr von der Anwendung aufgerufene Methoden:

- `UserNotificationService.inbox` und `searchInbox` mit booleschem Filter. Die
  Anwendung verwendet ausschließlich `ReadFilter`; sämtliche betroffenen Tests
  wurden auf `ALL`/`UNREAD` umgestellt, die bestehenden `READ`-Fälle bleiben erhalten.
- `VerificationService.deleteToken` und die beiden Repositorymethoden
  `deleteByToken` für Verifikations-/Passwortreset-Tokens. Die tatsächlichen
  Statuswechsel und die zeitgesteuerte Bereinigung bleiben erhalten.
- `PasswordResetService.existsToken`: nur noch als Test-Abfrage verwendet. Die
  Persistenzprüfungen beobachten den Zustand jetzt direkt über das Repository.
  `VerificationService.existsToken` bleibt erhalten, da `VerificationView` es nutzt.
- `EmailQueueRepository.findByStatus`: nur Test-Abfrage, durch die bestehende
  Repository-Lesefunktion mit anschließender Statusauswahl ersetzt. Status- und
  Anzahlassertions bleiben unverändert.
- `ImageHelper.convertImageToBase64`: der aktive Mailversand verwendet ein CID-Bild
  mit Bytes aus dem Classpath. `readImageBytes` samt Test für das echte Mail-Logo
  bleibt erhalten. Die nur hierfür verwendete direkte `commons-codec`-Abhängigkeit
  entfällt; keine Versionen oder übrigen Abhängigkeiten wurden geändert.
- Ein unbenutzter Logger in `EmailService`. Die gleiche Herkunftsbestimmung in
  Inbox, Overlay und Detaildialog wird nun gemeinsam verwendet.

Es wurde keine nachweislich unbenutzte Produktionsklasse gefunden. Spring-Beans,
Vaadin-Routen/Callbacks, JPA-Lifecyclemethoden, Repository-Implementierungen und
Framework-Overrides bleiben erhalten, auch ohne sichtbaren direkten Aufrufer.
`RetiredConfigEntry` und die Behandlung älterer gespeicherter Logins bleiben aktive
Migrationen; ihre Namen machen sie nicht zu totem Code.

Sieben Testausprägungen ausschließlich für entfernte APIs entfallen; keine Tests
sind deaktiviert oder übersprungen. Die Assertions der weiterhin verwendeten
Funktionen wurden nicht abgeschwächt.

## Deutsche und englische Texte

Beide Kataloge sinken von jeweils 540 auf **538 eindeutige Schlüssel**:

- `notifications.page` und `getNotificationsPage`: Überbleibsel der alten Pagination.
- `adminView.config.helper.token_duration_formats` und
  `getAdminConfigHelperTokenDurationFormats`: nur noch in einem Browser-Helfertest
  abgefragt, kein sichtbarer Aufrufer. Die aktuellen feldbezogenen Tooltips und
  Validierungsnachrichten bleiben erhalten.

DE/EN besitzen dieselben Schlüssel in derselben Reihenfolge, ohne Duplikate oder
leere Werte. Der statische Abgleich der 532 verbliebenen typisierten Getter liefert
keinen weiteren unbenutzten Kandidaten; interne Enum-Dispatcher und dynamische
Texte sind berücksichtigt. `MessageCatalogueTest` bestätigt beide Sprachen,
vorhandene Getter, Enumvarianten und Singular/Plural sowie die Erreichbarkeit aller
Schlüssel. Das bestehende Latin-1-Properties-Encoding bleibt erhalten.

## Ressourcen und CSS

Entfernt ist die nicht eingebundene `custom-theme/theme.json`. Die Anwendung lädt
Lumo, Utility und eigenes CSS über `@StyleSheet` in `Application.java`; es gibt
keine `@Theme`-Anbindung. Die aktuelle Flow-25.3.0-Implementierung `ThemeUtils` liest
`theme.json` aus benannten Themeordnern, nicht aus diesem statischen Verzeichnis.
Siehe auch [Vaadin-25-Umstellung](https://vaadin.com/docs/latest/upgrading).
Der frühere Bereinigungsbericht hatte die Datei vorsorglich behalten; die erneute
Prüfung klärt diese Altlast nun ausdrücklich.

Die sieben aktiven Bild-/Icon-/Fontdateien, Fontlizenz und je zehn HTML-/MJML-
Mailvorlagen bleiben erhalten. Die MJML-Dateien sind bearbeitbare Autorenquellen;
fehlende direkte Java-Aufrufer sind dafür kein Löschgrund. Spring-Profile und die
automatisch geladene Logback-Konfiguration bleiben ebenfalls erhalten.

Die neue `cosmos-notifications.css` fasst Header-Vorschau, Inbox, Detaildialog,
Verfassen und Versandarchiv zusammen. Gemeinsame Konto-Tabs stehen in
`cosmos-theme.css`, der gemeinsame Inhaltsrahmen in `cosmos-shell.css`.
`UserNotificationsView` lädt keine lokale Konto-CSS-Datei mehr. Die neun
Stylesheets sind einheitlich mit vier Leerzeichen und einer Eigenschaft pro Zeile
formatiert; zwei benachbarte doppelte Versandvorschau-Regeln sind zusammengeführt.

Alle Farben/Flächen bleiben in `cosmos-tokens.css`. Seitenbreite, Vorschauweite und
Vorschauhöhenlimit sind drei zusätzliche zentrale Größenparameter. Für andere
Paletten wird weiterhin nur die Tokendatei ersetzt, Schrift bei Bedarf separat.
Die feste Ladefolge, Verantwortlichkeiten, Werte und das Vorgehen für eine helle
Variante stehen in der aktualisierten [Theme-Anleitung](theme.md). Es wurde kein
Laufzeit-Themewechsler oder neues Design eingeführt.

Der statische Vergleich mit dem Ausgangscommit ergibt:

- 2.555 verglichene Selektor-/Eigenschaftswerte nach Auflösung der Basistokens,
  ohne Wertabweichungen.
- Alle 179 bisherigen Basiswerte unverändert; drei neue Parameter, jetzt 182.
- 844 Cosmos-Verweise und die zwei lokalen Schriftpfade geprüft; keine Tokenzyklen.
- Media-/Keyframe-Bedingungen erhalten. Keine Farbliterale außerhalb der Palette.
- Keine verwaiste eigene CSS-Klasse gefunden. Die nativen Grid-Klassen und vier
  dynamisch über `OverlayUserIcon` verwendeten Avatar-Schatten bleiben erhalten.
  Die dynamisch gebildeten Profil-/Sicherheits-Panel-IDs und die absichtlichen
  `vh`-/`dvh`-Fallbacks werden ebenfalls nicht als Altlast entfernt.

Dieser Vergleich belegt Quellwerte und Bedingungen. Er ist kein Nachweis für jedes
Browser-Rendering, Vererbung oder die vollständige Kaskade nach der Aufteilung.

## Validierung und Übernahme

- Java 25.0.4.1 und Maven Wrapper 3.9.16 bestätigt; Shared gebaut und installiert.
- Alle Java-/Testquellen in einer eigenen temporären Quellkopie kompiliert.
  358 Quelldateien per Hash mit dem Arbeitsstand abgeglichen. Nach Entfernung
  der alten Theme-Datei Produktionsressourcen frisch erzeugt und Java neu
  kompiliert: alle 42 Ressourcen bytegleich im finalen Classpath, entfernte Datei
  dort nicht mehr vorhanden.
- **1.823 Anwendungs-JUnits und 48 Browser-Helfertests erfolgreich**, jeweils
  0 Fehler, 0 Fehlschläge, 0 Skips. Sämtliche Anwendungsfälle wurden ausgewählt.
  Nach der abschließenden Lesbarkeitsanpassung der Test-Abfragen zusätzlich
  beide betroffenen Queue-Testklassen erneut geprüft: 78/78 erfolgreich,
  ebenfalls ohne Fehler oder Skips.
- Spotless apply/check, CSS-Syntax-/Token-/Wertevergleich, Katalogprüfung und
  `git diff --check` erfolgreich.
- Keine Browser-UI-Prüfung und kein Produktions-/Frontendbuild. Dafür wurde keine
  neue Freigabe erteilt. Keine neue Gesamt-Coverage gemessen.

Die laufende Anwendung, IntelliJ/Vite, reale Daten und Mailkonten bleiben unberührt.
Für die Übernahme wegen entfernter Methoden, Maven-Abhängigkeit und geänderter
Stylesheet-Anbindung: Maven-Projekt in der IDE aktualisieren, Anwendung vollständig
neu starten, anschließend Strg+F5. Die Änderungen sind nicht automatisch in die
bereits laufende Anwendung übernommen.
