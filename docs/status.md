# Große Testrunde – 03.11.2026

## Prüfstand und Ergebnis

Geprüft wurde die 2FA-Erweiterung auf `codex/zwei-faktor-authentifizierung`,
Basis `85937d11eeb49f62358d0f67651dc3753d561977` samt den QA-Änderungen.
Vergleichspunkt ist die letzte vollständig belegte Main-Runde auf `623feb67`.
Der tatsächliche Diff umfasst insbesondere 2FA, Rollen-/Funktionsregeln,
Sitzungsschutz, Wiederherstellung, Anbieterkonfiguration und verschlüsselte Secrets.

**Der automatisierte Testteil ist bestanden.** Nach den passenden Korrekturen und
Nachtests sind **245 eindeutige Browserfälle je Sprache** erfolgreich belegt.
Es bleiben keine ungeklärten Testfehler oder bestätigten offenen Anwendungsfehler
zurück. Die gemeinsame Design-/Funktionsabnahme und ein aktueller Sonar-Nachweis
für diesen Featurestand sind weiterhin offen. Das ist kein insgesamt bestätigter
grüner Qualitätsstand und kein Abschluss der Umsetzung.

Anwendungsquellen, Shared, Root-POM und Dockerfile wurden in dieser Runde nicht
geändert. Korrekturen betreffen die Browsertests, deren Auswahl und eine defekte
HTML-Referenz. Die genaue Zuordnung von Quellfingerabdrücken und lokalen
Laufbelegen bleibt im QA-Zustand erhalten.

Seit dieser Gesamtrunde wurden der administrative Menüpunkt auf „2FA“ gekürzt,
sein vollständiger Name als nativer Tooltip ergänzt und die doppelten Kopftexte
in Vaadin sowie der HTML-Referenz getrennt. Für diese nachträgliche Textanpassung
bestanden 43 gezielte JUnit-Fälle für Navigation und Sprachkataloge; sie gehört
nicht zum oben genannten Browsernachweis des unveränderten QA-Images.

## Ausgeführte Prüfungen

| Prüfung | Beobachtetes Ergebnis |
| --- | --- |
| Vollständiger Root-Reactor `clean verify`, Production und Coverage | Erfolgreich unter JDK 25 |
| Anwendungs-JUnit | 2.628 bestanden, zwei erwartete optionale Skips; 2.630 insgesamt |
| JUnit-Browser-Helfer | 88 bestanden |
| Browser Deutsch, Gesamtlauf und passende Nachtests | 245/245 eindeutige Fälle bestanden |
| Browser Englisch, Gesamtlauf und passender Nachtest | 245/245 eindeutige Fälle bestanden |
| Frontend-JavaScript | 20 bestanden |
| Sonar-Offlineprüfungen | 47 bestanden |
| Coverage-Offlineprüfungen | 18 bestanden |
| Spotless und Checkstyle | Bestanden, keine Checkstyle-Verstöße |
| Ruff für die vier gepflegten Python-Dateien | Formatierung und Lint bestanden |
| Effektives Maven-Modell und Coverage-Zuordnung | Bestanden am frischen Abschlussbuild |

Die zwei JUnit-Skips betreffen ausschließlich die vorgesehenen opt-in
IDE-/Caddy-Prüfungen. Shared wird durch App- und Helfertests mitgeprüft.
JaCoCo-Zeilenabdeckung: **84,39 % App**, **100 % Shared**. Der Abschlussbuild erzeugte
den Bericht nach den letzten Helferbuilds neu; ein vorheriger Bericht war durch
neuere Klassen zeitlich überholt. Zusätzliche serverseitige Browser-Coverage wurde
nicht erhoben.

Alle Browserbelege gehören unverändert zu diesem Anwendungsimage:
`sha256:02ba455cfd1f88845baa926012141af56cf14e7df5641d4857dbbff13354d734`.
Die Gesamtläufe verwendeten 35 Klassen und vier getrennte Worker, jeweils mit
eigener JVM, Anwendung, H2-Datenbank, HTTPS-Umgebung und Mailpit. Innerhalb eines
Workers lief JUnit sequenziell. Die gezielten Nachtests verwendeten denselben
Runner und dasselbe Image. Chromium prüfte Desktop und die vorhandenen schmalen
Fälle; repräsentative Vaadin-/HTML-Bilder wurden zusätzlich gesichtet.

Alle eigenen Browserumgebungen wurden erfolgreich abgebaut. Vorhandene Runs,
Entwicklerdaten und fremde Dienste blieben erhalten. Echte Anbieterzugänge,
externer Mailversand, Provider-Smokes und der eingefrorene OpenCode-Workflow
gehörten nicht zur Prüfung.

## Korrekturen und neue Nachweise

- **HTML-Referenz:** Nach „Codes gesichert“ warf eine veraltete Vorlage wegen
  `codeFields` außerhalb ihres Gültigkeitsbereichs einen JavaScript-Fehler. Die
  unbenutzte Vorlage ist entfernt; der sichtbare Klickweg einschließlich
  Wiederherstellung und der QR-/Schlüsselumschaltung ist auf Desktop und Smartphone
  nachgeprüft. Die korrigierte Referenz benötigt im Browser ein Neuladen.
- **Einrichtung:** Bestehende Fälle prüfen jetzt ausdrücklich 42-px-Tabs, geladenen
  mittigen QR-Code, schreibgeschützten zweizeiligen Schlüssel, leere Flow-Hostwerte
  trotz sichtbarer Inhalte, Kopieren ohne Gruppierung, verweigerten Clipboard-Zugriff
  und Erhalt des manuellen Tabs beim Sprachwechsel. Die native OTP-Eingabe wird mit
  Punkt-Platzhaltern, Positionswechseln, Rücktaste/Pfeilen, Einfügen von `123456` und
  `123 456` sowie Standardfehlermeldung und roter Markierung bei leerem Code geprüft.
- **Ersatzcodes/PDF:** Einzelkopieren, unveränderte Codes nach Kopierfehlern,
  Ein-/Ausklappen und Sprachwechsel, Sicherungsbestätigung und leere Hostwerte sind
  abgesichert. Ein echter Desktop-Wartefall von mehr als fünf Minuten bestätigt
  weiterhin verfügbare Codes und PDF nach Neuladen. Fehlgeschlagene Downloads
  erzeugen keine TXT-/Fehlerdatei und verlassen die Ausgabe nicht; der echte
  PDF-Download funktioniert. Nach Abschluss sind weitere Abrufe gesperrt.
- **Anbieter:** Nach Speichern ohne neues Secret und nach Neustart werden nun
  Google und Microsoft tatsächlich mit dem eigenen OIDC-Testanbieter verwendet.
  Microsoft durchläuft dabei die reguläre lokale E-Mail-Verifikation. Deaktivieren
  und Reaktivieren, Kontozugriff und zulässige Bestätigungswege sind nachgeprüft.
- **Bestehende Testannahmen:** Normale Admins dürfen die beiden Superadmin-Gruppen
  nicht sehen. Eine Rollenvergabe kann zuerst die verpflichtende 2FA-Einrichtung
  verlangen; danach werden Live-Rollenentzug und dieselbe Sitzung weiter geprüft.
  Logout von einer öffentlichen Startseite wird am anschließenden geschützten
  Kontoaufruf geprüft. Tests wählen ihre Sprache ausdrücklich und warten reale
  Kopiermeldungen sowie den Abschluss von Bestätigungsdialogen ab.
- **Navigationsrennen im Test:** Der englische MFA-16-Lauf navigierte zurück zur
  Konfiguration, während die Anwendung korrekt zur erneuten 2FA-Bestätigung
  weiterleitete. Der Test öffnet den gespeicherten Stand jetzt in einem frischen
  Tab derselben Sitzung. Die Sicherheitsprüfung bleibt bestehen; die gesamte
  Lifecycle-Suite bestand anschließend in DE und EN.

Der deutsche Gesamtlauf bestand nach den ersten Korrekturen **235/245 Fälle**.
Passende finale Nachtests bestanden: Einrichtung **4/4**, Anbietereinstellungen
**6/6**, Benutzerrechte **8/8** und Lifecycle **5/5**. Der englische Gesamtlauf
bestand **244/245 Fälle**; allein das beschriebene Navigationsrennen brach ab.
Der anschließende Lifecycle-Nachtest bestand **5/5**. Der neueste tatsächliche
Status je Allure-Fallkennung ergibt damit je Sprache 245 erfolgreiche Fälle.
Nachtests sind keine zusätzlichen Testfälle. Die ursprünglichen Fehlerberichte
bleiben unverändert erhalten; ein fehlerfreier einzelner Gesamtlauf wird nicht
vorgegeben. Es wurden keine Sicherheitsassertions abgeschwächt oder automatischen
Wiederholungsversuche ergänzt.

Die vorhandene klassenweise Worker-Verteilung berücksichtigt die langen 2FA- und
Anbieterklassen besser. Zwei zusätzliche Auswahlen führen bestehende Fälle gezielt
aus: `two-factor-setup` und `external-login-settings`. Es gibt keinen zweiten Runner.
Der erledigte automatisierte [Testbedarf](testbedarf.md) ist bereinigt. Der
UI-Nachtest nach einer später freigegebenen Entwurfsübernahme bleibt dort wartend.

## Sonar und verbleibende Abnahme

Gelesen wurde die verfügbare Main-Analyse vom 01.11.2026 für `623feb67`. Sie passt
weder zum aktuellen Featurestand noch zum fortgeschrittenen lokalen Main-Stand.
Ihr damaliges grünes Gate ist deshalb kein Nachweis für diese Umsetzung.
Nach Projektregel erfolgt der neue Scan erst auf sauberem `main`; der nötige Merge
ist ein separater Auftrag. Die letzte vollständig belegte QA-Baseline bleibt bis
zum aktuellen Sonar-Nachweis unverändert.

Bei eingerichteter 2FA ersetzen Portalpasswort, Google oder Microsoft den eigenen
Faktor für die entsprechend geschützten Aktionen nicht. Die bewusst vereinbarten
Grenzen der Google-Ersteinrichtung bleiben in der
[Fachdokumentation](externe-anmeldung.md#google-bestätigung-und-geschützte-ersteinrichtung)
benannt; bestandene Tests beseitigen diese Grenzen nicht.

Die Entwurfsentscheidung für die wiederkehrende Codebestätigung,
Authenticator-Wiederherstellung und Hilfeanfragen bleibt im
[aktiven 2FA-Plan](planung/zwei-faktor-bestaetigung.md) offen. Die umgesetzten
QR-/Ersatzcodevorlagen wurden in DE/EN geprüft; ältere Hilfsmomentaufnahmen der
HTML-Referenz sind weiterhin deutsch. Die fachliche und gestalterische Abnahme
sowie echte Authenticator-Apps, reale Google-/Microsoft-Sonderfälle, Mobilgeräte,
Bildschirmtastatur, Zoom, Screenreader und PDF-Druck bleiben in
[HUM-08/HUM-09](manual-tests/README.md). Headless-Bilder ersetzen diese Abnahmen nicht.
