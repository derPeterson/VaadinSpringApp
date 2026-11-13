# Offene Sonar-Pakete zur Nachtintegration

Grundlage: vollständige Serveranalyse `544b14d1-1efc-4707-85f5-032db83807a1`
von main `3fe5f121`. Noch 29 offene Testhinweise, keine offenen Hinweise im
Anwendungscode, keine Bugs, Schwachstellen oder Hotspots. Die Codekorrekturen
von Paket A und B sind bestätigt. Die zwei bewussten Ausnahmen zur Vaadin-
Vererbungstiefe stehen mit konkreter Begründung direkt am jeweiligen Finding.
Gate bleibt wegen der Testhinweise rot. Der vollständige lokale Export liegt
ignoriert unter `sonar-findings.json`.

## C. Tests und Browserhelfer — 29 Hinweise

- Ausnahmeassertionen auf den tatsächlich erwarteten Aufruf begrenzen, Argumente
  vorher vorbereiten. Erwartungen und Fallunterscheidungen erhalten.
- Vergleichsassertionen, unnötige Matcher/Throws, ungenutzten Zustand und Mockaufbau
  korrigieren; keine Assertion zur Beruhigung des Scanners entfernen.
- Lange Browserfälle in fachlich benannte Prüfphasen gliedern, keine künstliche
  Verteilung einzelner Assertions. Regex mit rückverfolgungsfreier Alternative
  ersetzen und das bisherige Normalisierungsergebnis erhalten.
- Nachweise: betroffene JUnit-/Helfertests; entsprechende Browserfälle in der
  großen DE-/EN-Runde, eigener Review/Commit und vollständiger main-Nachscan.

## Abschluss

- Nach jedem Paket aktuelle Findings zuordnen und erledigte Paketbeschreibung
  entfernen; neue berechtigte Folgehinweise im Umfang korrigieren.
- Ziel: null offene berechtigte Issues und grünes Gate am tatsächlichen main.
  Keine Regel-, Quellumfangs- oder Coverage-Abschwächung.
- Danach sauberen Folgebranch anlegen und die große Browserrunde all DE/EN am
  selben finalen Image spätestens 09:00 Europe/Berlin starten. Gesamtumsetzung
  bleibt zur gemeinsamen Abnahme offen.
