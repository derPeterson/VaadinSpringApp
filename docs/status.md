# Große Testrunde vom 05.11.2026

## Prüfstand

Geprüft wurde der lokale Stand auf `codex/zwei-faktor-authentifizierung` mit Basis
`0f1dd8160b15e3fecfb38cb1ac4b3631bd4ac118` und den uncommitteten Änderungen.
Die letzte vollständig belegte Main-Baseline ist `623feb67`. Seit der vorherigen
Feature-Runde sind insbesondere die Vaadin-Ansichten für 2FA, die öffentliche
Sprachwahl, die gemeinsamen Scrollleisten sowie Live-Verlauf, Lesestände,
Ticket-Abos und die administrative Ticketansicht hinzugekommen.

**Die automatisierte Testrunde ist bestanden.** Für den Featurestand bleiben
der aktuelle Sonar-Nachweis und die gemeinsame Abnahme offen. Die konkreten
Quellfingerabdrücke und Laufbelege stehen im lokalen QA-Zustand.

Die danach angepasste Bearbeiteranzeige und die neue Benachrichtigungsanordnung
mit Popover-Aktionsliste und direktem Ticketzugang sind mit 276 gezielten
JUnit-Fällen geprüft. Die Suite `notifications` bestand anschließend mit je acht
Browserfällen in Deutsch und Englisch am selben Anwendungsimage
`sha256:ce25b0177cb2be87da9f6164c4d842453146d17b794e09f54e428bea125b0229`.
Geprüft sind unter anderem die Leseaktion in der Karte, die beschrifteten
Menüaktionen, Escape ohne Schließen des Dialogs und bestätigtes Löschen auf
Desktop und Smartphone. Eine falsche Sichtbarkeitsprüfung des unsichtbaren
Vaadin-Hosts wurde auf den tatsächlich sichtbaren Overlayinhalt korrigiert.
Alle isolierten Testumgebungen wurden vollständig abgebaut. Dieser Nachtest ersetzt
die Gesamtrunde nicht; die ticketbezogenen Browserprüfungen und die gemeinsame
Sichtabnahme bleiben offen.

## Ausgeführte Prüfungen

| Prüfung | Beobachtetes Ergebnis |
| --- | --- |
| Root-Reactor `clean verify` mit Production und Coverage | Bestanden unter JDK 25 |
| Anwendungs-JUnit | 2.660 bestanden, zwei vorgesehene opt-in HTTPS-Prüfungen übersprungen |
| Browser-Helfertests | 90 bestanden |
| Frontend-JavaScript | 20 bestanden |
| Sonar-Offlineprüfungen | 47 bestanden |
| Coverage-Offlineprüfungen | 18 bestanden |
| Spotless und Checkstyle | Bestanden, keine Checkstyle-Verstöße |
| Effektives Maven-Modell und Coverage-Zuordnung | Bestanden, 85,17 % Zeilenabdeckung App und 100 % Shared |
| Browser Deutsch | 245 von 245 eindeutigen Fällen nach den gezielten Nachtests bestanden |
| Browser Englisch | Gesamtlauf bestanden, 245 von 245 |

Die vollständigen Browserläufe verwenden je vier getrennte Worker mit eigener
JVM, Anwendung, H2-Datenbank und Mailpit. Das aktuelle Anwendungsimage für beide
Sprachen ist
`sha256:00ffac55a7a38216b8e068dcb75b52f2c4b5161c7e42f4d25c1ac1f3e1e3ceb1`.
Der deutsche Gesamtlauf bestand 239 von 245 Fällen. Die Nachtests am selben
Image bestanden in den Suiten `operations` (3), `cosmos-design` (16),
`external-login-flow` (29) und `two-factor` (32). Der neueste Status je
Allure-Fallkennung ergibt 245 bestandene Fälle. Alle eigenen Testumgebungen
wurden abgebaut.

## Korrekturen und Prüfungen

- **Admin-Tickets:** Die Auswahl nach dem Kategorie-Tabwechsel konnte auf schmalen
  Ansichten verloren gehen, wenn beim Laden derselbe Datenbestand erneut in das
  Grid gesetzt wurde. Die Liste wird bei gleichen Daten weiterverwendet. Der
  korrigierte Stand bestand MFA-07 in Deutsch und Englisch bei 1.280 und 390 px.
  Der Fall prüft Kategorien, Auswahl, Übernahme, den Live-Verlauf in beide Richtungen,
  den Erhalt des Entwurfs, die Sichtbarkeit interner Vermerke nur in der
  Administration und die abgeschickte Standardanleitung im Testpostfach.
- **JUnit:** Der Sprachkatalogtest deckt auch den Titel für eine Ticketbenachrichtigung
  ohne Nummer ab. Der SVG-Test berücksichtigt die eigenen Kontur-Icons und das
  vorhandene ausgefüllte Material Symbol mit seiner lokalen Lizenz.
- **Browser-Nachtests:** OIDC-01 berücksichtigt nach dem Abmelden die zuvor gewählte
  öffentliche Sprache. MFA-12 und der SMTP-Ausfalltest vermeiden das erneute
  Auslösen der bereits aktiven Sprache beim 2FA-Schritt. COSMOS-06 nutzt den
  gekürzten Linknamen „2FA“ und erwartet den vollständigen Titel im Panel.
  Diese Anpassungen am Testcode sind in beiden Sprachen am selben Anwendungsimage geprüft.

## Sonar und verbleibende Abnahme

Für den aktuellen Featurestand fehlt ein Sonar-Nachweis. Die konfigurierte
Anbindung war beim Abruf nicht erreichbar, und der vorhandene Export gehört zu
einem älteren Main-Stand. Der neue Scan erfolgt nach Projektregel auf sauberem
`main`. Die letzte vollständig belegte QA-Baseline bleibt erhalten.

Die gemeinsamen Sichtabnahmen für [2FA und Admin-Tickets](planung/zwei-faktor-bestaetigung.md)
sind offen. Der [manuelle Katalog](manual-tests/README.md) führt die Prüfung mit
echten Authenticator-Apps, realen Anbieter-Sonderfällen, Mobilgeräten,
Bildschirmtastatur, Zoom, Screenreader und PDF-Druck. Die vereinbarten Grenzen
der Google-Ersteinrichtung sind in der
[Fachdokumentation](externe-anmeldung.md#google-bestätigung-und-geschützte-ersteinrichtung)
beschrieben. Offene automatisierte Szenarien stehen im [Testbedarf](testbedarf.md).
