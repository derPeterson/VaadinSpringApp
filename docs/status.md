# Prüfstand

## Große Testrunde vom 19.11.2026

Verglichen wurde der Folgebranch `codex/vaadin-komponenten-folgearbeiten-6`
mit der letzten vollständig erfolgreichen Testrunde auf `486a54c0`.
Die aktuellen Gesamtläufe gehören zum Anwendungsstand `1272bcf4` mit
den überarbeiteten Browserprüfungen. Danach wurde der Rückfokus der
Empfängerliste korrigiert und auf dem neu gebauten Image gezielt geprüft.

| Prüfung | Ergebnis |
| --- | --- |
| Optimierter Root-Reactor mit Produktion und Coverage | Bestanden. Die Anwendung und der Browserrunner verwenden dasselbe im Reactor gebaute Shared-Modul. |
| Anwendungs-JUnit | 3.270 bestanden, zwei optionale IDE-/Caddy-HTTPS-Prüfungen übersprungen |
| Browser-Helfertests | 143 bestanden |
| Frontend-Offlineprüfungen | 23 bestanden |
| Coverage-, Sonar- und Release-Helfer | 18, 49 und 14 bestanden |
| Lokale Zeilenabdeckung | Anwendung 86,10 %, Shared 100 %. Gemeinsame Coverage-Prüfung bestanden. |
| Java- und CSS-Stilprüfungen | Spotless, Checkstyle, Prettier und Stylelint bestanden |
| Browser Deutsch, gesamte Suite | 373 von 387 bestanden, 14 fehlgeschlagen, keine Skips |
| Browser Englisch, gesamte Suite | 374 von 387 bestanden, 13 fehlgeschlagen, keine Skips |
| Aufbau, Abbau und HTML-Bericht | In beiden Gesamtläufen bestanden |

Die beiden Gesamtläufe verwendeten je vier isolierte Worker und dasselbe
unveränderte Anwendungsimage
`sha256:50bf676f078080c2b35d722a2d657de3ea4fe7687dc5d6db3a67044524687886`.
Die Browsernachtests nach der Fokuskorrektur verwenden das Image
`sha256:a2ef1dacbfe5dc8ed7202461f8504ad9cf4354c76a064481a4468c40554f82b3`.

| Browsernachtest | Deutsch | Englisch |
| --- | --- | --- |
| Empfängerliste | 2/2 | 2/2 |
| Sitzungen | 9/9 | 9/9 |
| Benutzerverwaltung | 22/22 | 22/22 |
| Konfiguration | 19/19 | 19/19 |
| Kontoeinstellungen | 22/22 | 22/22 |
| Ticketdetails | 7/7 | 7/7 |

Reale Anbieter, echter SMTP-Versand und menschliche Sonderprüfungen stehen
im [manuellen Katalog](manual-tests/README.md).

## Geprüfte Änderungen

- **Aktionsleisten:** Die übrigen acht Aufrufer des Skripts zur
  Breitenangleichung verwenden CSS Grid oder Subgrid. In beiden Sprachen
  bestanden je 27 einschlägige Browserfälle, darunter feste Dialogpaare,
  variable Aktionsleisten, schmaler Benutzereditor und Anbieterkarten.
  Alle 16 zur Suite für native Bestätigungen gehörenden Fälle bestanden
  pro Sprache. Die Gruppen überschneiden sich.
- **Korrekturen beim Layout:** Anbieterkarten teilen die Spalten auch auf
  schmalen Ansichten. Die Aktionen im mobilen Benutzereditor füllen ihre
  Rasterzeile. „Abmelden“ steht wieder rechts im Benutzermenü.
  Die Profilbildreiter bestanden je drei Größenprüfungen. Der Übergang
  unter den Reitern wurde auch in OTP- und Identitätsdialogen geprüft.
- **Empfängerliste:** Schließen des Popovers gibt den Fokus an die
  Avatargruppe zurück. Der Nachtest bestand auf Desktop und schmaler
  Ansicht in DE und EN mit je zwei Fällen.
- **Browserprüfungen:** Selektoren und Erwartungen an native Schalter,
  Validierung beim Absenden, lokalisierte Meldungen, responsive Notifications,
  Escape und das mobile Vorgangsdetail angepasst. Die Messung nativer
  Bestätigungen wartet das Ende der Öffnungsanimation ab.

## Offene Nachweise

- **TextArea:** Beim Ziehen am sichtbaren Griff im isolierten Chromium
  änderte sich die Höhe in MAIL-02, MAIL-03 und NOTIFY-01 (1280 und 390 px)
  in DE und EN nicht. Die Ursache ist offen. Folgeschritte in den
  betroffenen Szenarien benötigen einen erfolgreichen Nachtest.
- **Gesamtstand:** Die gezielten Nachtests auf dem jüngsten Anwendungsimage
  ergänzen die Gesamtläufe. Ein vollständig grüner DE-/EN-Nachweis
  für den Endstand steht aus. Die bisherige erfolgreiche Baseline bleibt
  `486a54c0`. Konkrete offene Szenarien stehen im [Testbedarf](testbedarf.md).

## Sonar

Die verfügbare Main-Analyse `452d3e46-52bf-4f20-b0f5-f600651fc1d9`
gehört zu `aa9c70fa`. Das Quality Gate steht auf OK, mit null offenen
Issues, null offenen Hotspots und 45 bestehenden Ausnahmen. Für den
Folgebranch liegt keine eigene Sonar-Analyse vor.
