# Offene Sonar-Pakete zur Nachtintegration

Grundlage: vollständige Serveranalyse `ee430dcd-5c8f-4816-8849-37d348fe0aaa`
von main `18cd8ba0`. 51 offene Codehinweise vor den beiden begründeten
Vererbungsentscheidungen, keine Bugs, Schwachstellen oder Hotspots.
Die neun Fachlogikhinweise sind im Nachscan behoben. Gate bleibt rot wegen neuer
Hinweise; Coverage und Duplikation liegen innerhalb des Gates. Bestehende
Ausnahmen unverändert lassen. Der vollständige lokale Export liegt ignoriert
unter `sonar-findings.json`.

## B. UI-Bausteine und Gliederung — 22 Hinweise

- Serialisierbarkeit von Komponentenzustand fachlich prüfen und erhalten.
- Kognitive Komplexität bei Konfigurationsaufbau/-speicherung, Benutzerformular
  und Ticketnavigation durch fachliche Teilmethoden reduzieren; keine Prüfungen,
  Aufrufreihenfolgen oder UI-Zustände entfernen.
- Gemeinsame Attribute/Literale, lokale Verzweigungen, Imports und Typzugriffe
  bereinigen; Breadcrumb-Vertrag in den passenden Datentyp legen.
- Die zwei Hinweise zur Vererbungstiefe von AccountView/AdminView anhand der
  tatsächlichen Vaadin-Hierarchie prüfen. Keine gemeinsame Architektur nur für
  eine Kennzahl ersetzen; eine fachlich begründete Ausnahme ausdrücklich dokumentieren.
- Nachweise: vorhandene betroffene UI-/Service-JUnit-Fälle, gezielte Browserfälle
  soweit erforderlich und Gesamt-QA; eigener Review/Commit und main-Nachscan.

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
