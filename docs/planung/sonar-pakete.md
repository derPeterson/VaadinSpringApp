# Offene Sonar-Bereinigung

Ausgangsanalyse vom 18.11.2026: `f5c228c3-6a8a-43da-87c7-c139bfba6172`,
main `28a8eb24afbd13e092d513f9ec280c06fd2310f1`. Sechs offene Findings,
46 bestehende Ausnahmen, keine Hotspots; Quality Gate `ERROR`.

## Paket 1: Überschriebene Schriftangaben und unbenutzte Wiederholungen

- **Priorität und Umfang:** Vier CSS-Bugs und zwei Java-Code-Smells in drei Dateien.
  Keine Fach-, Sicherheits- oder Architekturentscheidung erforderlich.
- **CSS (`css:S4657`):** In der Benachrichtigungsvorschau die vier durch den
  nachfolgenden gültigen `font`-Shorthand überschriebenen Angaben entfernen.
  Die wirksame Schriftdefinition und damit das Aussehen bleiben erhalten.
  Finding-IDs: `03af5ef0-caf7-4cbb-934b-bf05e96e2165`,
  `f1bed326-4464-44d7-abbc-4b97a2508771`,
  `b4feed92-c8bb-47f5-8f5f-c95eb66cb4bd`,
  `c90eabb8-17fd-4dbf-b7db-716d2a5e1d52`.
- **Java (`java:S1192`):** Den dreifach verwendeten Sternschnuppen-Klassennamen
  in `CosmosBackgroundComponent` als Konstante führen.
  Finding-ID: `09a8e3d6-583c-4613-a4f0-5874e6e8acf6`.
- **Testcode (`java:S7467`):** In `CosmosComponentsBrowserTest` die unbenutzte
  `TimeoutError`-Variable durch `_` ersetzen; Fehlerdiagnose unverändert erhalten.
  Finding-ID: `0cd56c4b-2243-47fd-9325-81d2ac31507f`.
- **Nachweise:** Java-Stil, Prettier, Stylelint, vorhandene Theme-Ressourcentests,
  Kompilierung der Browserquellen sowie vollständiger optimierter Reactor samt
  Anwendungstests, Browser-Helfern und gemeinsamer Coverage. Danach main-Nachscan
  mit unverändertem Java-/POM-/CSS-Umfang; keine Statusausnahmen oder Regelabschaltungen.
- **Risiko:** Gering; keine beabsichtigte Änderung sichtbarer Werte oder Testaussagen.
  Keine zusätzliche Browserrunde: die gemeinsame DE/EN-Prüfung bleibt zurückgestellt.
- **Zustand:** Korrekturen umgesetzt und reviewed. Java-Stil, Prettier, Stylelint,
  drei vorhandene Theme-Ressourcentests und die Kompilierung der Browserquellen
  bestanden. Lokale Integration und bestätigender main-Nachscan noch offen.
