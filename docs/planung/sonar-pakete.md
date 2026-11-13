# Offene Sonar-Pakete zur Nachtintegration

Grundlage: vollständige Serveranalyse `ae4306b6-9a10-4a49-ab7a-1729ce5ebf43`
von main `f5e93bd7`. 60 offene Codehinweise, keine Bugs, Schwachstellen oder
Hotspots. Gate rot wegen neuer Hinweise; neue Coverage 81,3 % und Duplikation
liegen bereits innerhalb des Gates. Bestehende 46 begründete Ausnahmen unverändert
lassen. Der vollständige lokale Export liegt ignoriert unter `sonar-findings.json`.

## A. Fachlogik und Transaktionsadapter — 9 Hinweise

- ConfigService: Validierung und Schreiben verständlicher trennen, verschachtelte
  Verzweigungen reduzieren und wiederholte Nachricht zentral benennen.
- UserService: öffentliche Transaktionsadapter von gemeinsamem privaten Ablauf
  trennen. Die Selbstaufrufe verwenden heute dieselbe REQUIRED-Transaktion;
  keine neue Transaktion, kein neuer Service und keine neue Fehlergrenze einführen.
- Konto-/Queue-Dienste: wiederholte Texte benennen, ungenutzte Catch-Variablen
  durch unbenannte Muster ersetzen. PendingProof gezielt dekonstruieren.
- Nachweise: ConfigService-/Persistenz-, UserService-/Verifikations-, Konto- und
  Queue-Regressionen; Formatierung, Lesbarkeitsreview der geänderten Bereiche,
  Commit, Integration und vollständiger main-Nachscan.

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
