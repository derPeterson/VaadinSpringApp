# Große Testrunde vom 07.11.2026

## Prüfstand

Geprüft wurde der Feature-Branch `codex/zwei-faktor-authentifizierung` auf Basis von
`13fc8a57d139b1ac07d6d481a8478978b4a1c2d5` mit den lokalen Korrekturen dieser Runde.
Der vorherige Feature-Prüfstand lag auf Basis von `0f1dd816`. Seitdem änderten sich
vor allem Ticketzustände und Benachrichtigungen, Ticketverlauf, Filter und Pagination,
OTP-Bedienung, Scrollleisten und die administrative 2FA-Konfiguration.

**Die automatisierten Prüfungen sind bestanden.** Der Prüfstand bleibt wegen des
fehlenden Sonar-Nachweises für den Feature-Stand unvollständig. Die letzte
vollständig belegte Main-Baseline `623feb67` bleibt erhalten.

## Ergebnisse

| Prüfung | Ergebnis |
| --- | --- |
| Root-Reactor `clean verify` mit Production und Coverage | Bestanden unter JDK 25 in einer separaten Quellkopie |
| Anwendungstests | 2.734 bestanden, zwei vorgesehene Opt-in-HTTPS-Prüfungen übersprungen |
| Browser-Helfertests | 96 bestanden |
| Frontend-JavaScript | 20 bestanden |
| Coverage-Offlineprüfungen | 18 bestanden |
| Sonar-Offlineprüfungen | 47 bestanden |
| Spotless und Checkstyle | Bestanden |
| Effektives Maven-Modell und Coverage-Zuordnung | Bestanden, 86,57 % Zeilenabdeckung der Anwendung und 100 % in Shared |
| Gezielter Browsernachtest für Ticketfilter und 2FA-Konfiguration | Je vier Fälle in DE und EN bestanden |
| Browser-Gesamtrunde DE | 251 von 253 im ersten Lauf bestanden. Die beiden betroffenen Fälle bestanden nach Korrektur in gezielten Nachtests. |
| Browser-Gesamtrunde EN | 253 von 253 im Gesamtlauf bestanden |

Die Browserläufe verwendeten dasselbe Anwendungsimage
`sha256:379d8878dcbf43115092cabe2ddfd66ec395a2bb8745bdf0c2704168660adb6e`.
Die Gesamtrunden liefen mit je vier getrennten Workern. Jede Testumgebung hatte
eine eigene Anwendung, H2-Datenbank und Mailpit-Instanz. Der Abbau war erfolgreich.
Der lokale QA-Zustand enthält die Quellfingerabdrücke und Belegpfade.

## Korrekturen und Nachtests

- **Ticketsprung und Pagination:** MFA-22 und MFA-23 prüften bei 1.280 und 390 px
  die Eingabe der Ticketnummer und das Einfügen mit Raute, erhaltene Filter bei
  einem Fehlsprung, die Auswahl bei einem Treffer und das Blättern mit 10, 20
  und 50 Einträgen. Der Einfügetest fokussiert das Feld nach dem Öffnen des Dialogs.
- **2FA-Konfiguration:** MFA-03 prüfte bei beiden Breiten das Auswahlmenü am Feld,
  Bedienung per Maus und Tastatur, Auswahlmarkierung und Werterhalt beim Leeren.
  Die Feldhilfe zeigte den zugänglichen Feldnamen, den Standardwert und die
  übersetzten Eingaberegeln.
- **Testimage:** Der Test für die leere Ticketdetailansicht erwartet das rahmenlose
  Mauszeiger-Icon. Die Textliste der Feldhilfe wird auch über den vorhandenen
  String-Getter abgerufen, damit der Katalogtest alle Schlüssel prüft.
- **Gesamtrunde:** MFA-19 erwartete noch das alte List-Icon. OIDC-09 maß den
  Copyrightbereich gegen die Fensterbreite einschließlich Scrollleiste.
  Beide Erwartungen wurden korrigiert. Die englische Gesamtrunde und die
  deutschen Nachtests `two-factor-ticket-layout` (3 Fälle) und
  `external-login-layout` (8 Fälle) sind bestanden.

## Sonar und verbleibende Abnahme

Die lesende Sonar-Abfrage für `vaadin-spring-app` ergab ein grünes Gate für die
Main-Analyse von `623feb67`. Sie gehört weder zum aktuellen lokalen Main-Stand noch
zum geprüften Feature-Stand. Ein neuer Scan erfolgt nach Projektregel auf sauberem
`main`.

Die gemeinsame Sichtabnahme für [2FA und Admin-Tickets](planung/zwei-faktor-bestaetigung.md)
bleibt offen. Der [manuelle Katalog](manual-tests/README.md) führt die Abnahme mit
echten Authenticator-Apps, Anbieter-Sonderfällen, Mobilgeräten, Bildschirmtastatur,
Zoom, Screenreader und PDF-Druck. Die Grenzen der Google-Ersteinrichtung stehen
in der [Fachdokumentation](externe-anmeldung.md#google-bestätigung-und-geschützte-ersteinrichtung).
Weitere offene automatisierte Szenarien stehen im [Testbedarf](testbedarf.md).
