# Theme-Struktur vollständig geprüft – 15.10.2026

Die CSS-Umstrukturierung auf `feature/theme-css-structure` wurde vollständig
nachgeprüft. Ausgangscommit: `e429f86fa6962d556f62684b82dc26c76aa0176d`; die Theme- und
Dokumentationsänderungen lagen bei der Prüfung uncommittet vor.

## Ergebnis

| Ausgeführte Prüfung | Ergebnis |
| --- | --- |
| Shared-Modul, sauber installiert; Java 25.0.4.1 / Maven 3.9.16 | Erfolgreich |
| Anwendungs-JUnit, frisch mit `clean test jacoco:report` | 1085 Tests; 0 Fehler, Fehlschläge oder Skips |
| Browser-Helper/Reporter | 34 Tests; 0 Fehler, Fehlschläge oder Skips |
| Frischer Docker-Produktionsbuild einschließlich JUnit und Frontend | Erfolgreich |
| Vollständige Browserprüfung Deutsch | 100/100 bestanden |
| Vollständige Browserprüfung Englisch | 100/100 bestanden; dasselbe Produktionsimage |
| Aufbau, Abbau, Allure-/Markdown-Berichte | Beide vollständig erfolgreich |
| Sieben produktiv ausgelieferte CSS-Dateien | HTTP 200, CSS-Inhaltstyp; geparste Regeln identisch mit dem Quellstand |

Browser: Chromium 153.0.8010.12. Beide Läufe verwendeten getrennte
Docker-Projekte und eigene H2-/Mailpit-Volumes, erfundene Konten und echte reguläre
SMTP-Testmails. Die IDE-Anwendung und Bestandsdaten waren keine Testziele.
Eigene Testcontainer, Netzwerke und Volumes wurden vollständig abgebaut.
In dieser Nachprüfung waren keine weiteren Anwendungs-/Designkorrekturen erforderlich.

## Was sich geändert hat

- Zentrale `cosmos-tokens.css` für 103 Designwerte: Palette, Komponentenfarben,
  Verläufe, Schatten und bereits vorhandene gemeinsame Größenwerte.
- Gemeinsame Regeln getrennt in `cosmos-theme.css`, `cosmos-shell.css` und
  `cosmos-admin.css`; `styles.css` wurde zur lokalen `admin-users.css`.
- Dekoration übernimmt ihre Farben ebenfalls aus den zentralen Tokens.
- Abschnittskommentare und [Theme-Anleitung](../theme.md) erklären Zuständigkeiten,
  CSS-Variablen, Vaadin-Parts, Vererbung und die Vorlage für ein weiteres Theme.

Das abgestimmte Aussehen und die vorhandenen Größen/Icons bleiben erhalten.
Der statische Abgleich löste 51 neue Komponenten-/Dekorationsvariablen auf und
bestätigte gleiche ursprüngliche Standardwerte, Selektoren und Deklarationsreihenfolge.
[Verbindlicher Designstandard](../design-standard.md).

## Wichtige Erkenntnisse

Die explizite Ladereihenfolge ist Teil der CSS-Kaskade: Schrift, Tokens, gemeinsame
Komponenten, App-/Admin-Bausteine, Dekoration; die lokale Benutzerverwaltung kommt an
AdminView hinzu. Portalisierte Overlays benötigen weiter ihre ausdrückliche
Cosmos-Markierung. Die benannten Lumo-Grid-@media-Einschübe bleiben erhalten.

Produktions-CSS wird minifiziert. Ein Vergleich der rohen Textdateien würde deshalb
Kommentare, Leerzeichen und Anführungszeichen fälschlich als Abweichung behandeln.
Die Prüfung hat die tatsächlichen CSS-Regeln verglichen und die korrekte Auslieferung
aller sieben Dateien bestätigt. Die Browserfälle prüfen zusätzlich sichtbare
Journeys und Layoutgrenzen in beiden Sprachen. Desktop-Bearbeitungsdialog, Filterpanel
und mobile Registrierung wurden zusätzlich anhand der erzeugten Screenshots gesichtet;
dabei waren keine Verschiebungen erkennbar.

## Frische JUnit-Coverage und Grenzen

| Messung | Ergebnis |
| --- | ---: |
| Anweisungen | 71,44 % (18795/26309) |
| Zeilen | 71,16 % (3689/5184) |
| Zweige | 66,37 % (1127/1698) |

Die Messung verwendet frische Ausführungsdaten und analysiert 97 Anwendungsklassen.
Der höhere Anteil gegenüber dem [Redesign-Abschluss](2026-10-15-redesign-abgeschlossen.md)
beruht auf dem nachfolgenden kleineren Codeumfang nach Entfernung ungenutzter Teile;
daraus folgt keine zusätzliche fachliche Abdeckung.
Browser-Code-Coverage wurde in dieser Runde nicht erneut instrumentiert.
100/100 ist eine Erfolgsquote vorhandener Browserfälle, keine CSS-/JavaScript-Coverage
und keine Behauptung vollständiger fachlicher Abdeckung.

Echte Geräte/Bildschirmtastaturen, vollständige Accessibility-/Zoomprüfung,
externe Mailprogramme/-zustellung und besondere Betriebs-/Migrationsfälle bleiben
außerhalb dieses Gesamtnachweises. Aktuelle Grenzen:
[Abdeckungsübersicht](../../browser-tests/abdeckung.md).

Dieser Meilenstein enthält die maßgeblichen Ergebnisse selbst. Lokale Laufordner
mit Screenshots, Traces und HTML-Berichten sind bereinigbare Diagnoseausgaben;
ihre dauerhafte Aufbewahrung ist nicht erforderlich.
