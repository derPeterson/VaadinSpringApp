# Gesamt-QA und fertiges Benachrichtigungsredesign vom 20.10.2026

## Auftrag und geprüfter Stand

Ausgangspunkt: sauberer Branch `feature/user-messages`, Commit
`11526c964a4f8dd7863a082ede6f052e9e498606`. Vollständige technische Testprüfung,
Browsertests um das fertig abgestimmte Benachrichtigungsdesign erweitern,
Fehler untersuchen/beheben und Coverage samt Grenzen zusammenfassen.
Die früher ausgenommenen Header-/Archivoberflächen sind jetzt im Auftrag enthalten.
Kein Commit, Merge oder Push; reale Daten und laufende Entwicklungsanwendung bleiben erhalten.

**Abgeschlossen: alle 152 eindeutigen Browserfälle in DE und EN erfolgreich, einschließlich erforderlicher gezielter Nachtests.**
Historische Ergebnisse ersetzen diesen Nachweis nicht.

## Erweiterter Fallkatalog

NOTIFY-03 bei 1280 und 390 px prüft die gemeinsame Kopfzeile und Kontonavigation,
120 Zeichen Betreff/4000 Zeichen Inhalt, gekürzte Vorschauen ohne horizontalen Überlauf,
volle Details ohne automatische Lesemarkierung und unterschiedliche Dialognavigation
je nach Herkunft. Iconaktionen/Metadaten, Filter, Einzel-X und Sammellöschen werden
über echte UI-Aktionen geprüft; ungelesene Einträge bleiben beim Sammellöschen erhalten.

NOTIFY-04 sendet elf Nachrichten über das native Formular, prüft Versandarchiv samt
Details/Pagination, persönliche Seiten 10+1, Größen 20/50, 56px Auswahl, Suche mit
Lesefiltern/Leerzustand, Vorschaugrenze fünf und persistentes Sammellesen aller elf.
`notifications` enthält acht Ausprägungen, `all` 152 Fälle in 28 Klassen.
COSMOS-06 ergänzt zwei Breiten für die Infoicon-Ausrichtung in allen drei
Kontotabs, allen acht Konfigurationsgruppen, Mail-Test und Versandpanel.
USER-05A erhält die 56px-Breitenprüfung für die Benutzerverwaltung. Die Zeilenaktionen
prüfen außerdem lokalisierte Tooltips und die echte Tab-Reihenfolge.
`fast` bleibt unverändert bei 34. Keine Backendabkürzung oder DOM-Zustandsmanipulation.

## Beobachtete Ergebnisse

| Prüfung | Tatsächliches Ergebnis |
| --- | --- |
| Anwendungs-JUnit mit JaCoCo, eigene Quellkopie | 1823 erfolgreich, 0 Fehler/Fehlschläge/Skips |
| Browser-Helper/Reporter | 48 erfolgreich, 0 Fehler/Fehlschläge/Skips |
| Python offline | 205 erfolgreich, keine Fehler |
| JavaScript offline | 26 erfolgreich (14 Workflow, 12 UI), 0 Fehler/Skips |
| Produktionsbuild im Docker-Testaufbau | erfolgreich |
| Benachrichtigungen DE, erste Vorprüfung | 6/8 erfolgreich; zwei Testabfragen zählten Tooltiptext als sichtbare Beschriftung |
| Browser DE, vollständiger erster Lauf | 146/150 erfolgreich, vier Testhelfer-/Erwartungsbefunde, 0 Skips |
| Browser DE, vier Klassen nachgeprüft | 32/35 erfolgreich; Tooltip-Rahmen und inneres ComboBox-Input falsch gemessen |
| Browser DE, letzter gezielter Nachtest | 13/13 erfolgreich, 0 Fehler/Skips |
| Browser DE, eindeutiger endgültiger Katalog | 152/152 nach vollständiger Runde und gezielten Nachtests erfolgreich |
| Browser EN, vollständiger Lauf | 151/152 erfolgreich, ein zu früher URL-Lesezugriff im Test, 0 Skips |
| Konfiguration EN und DE nach URL-Wartekorrektur | jeweils 9/9 erfolgreich, 0 Fehler/Skips |
| Browser EN, eindeutiger endgültiger Katalog | 152/152 nach vollständiger Runde und gezieltem Nachtest erfolgreich |

Die Testhelfer wurden auf die tatsächlichen nativen Bedienelemente abgestimmt:
sichtbare Radiosegment-Beschriftung, zugängliche Tooltip-Rolle und ComboBox-Rahmen.
Tooltip-/Zugänglichkeitstexte werden nicht als sichtbare Buttonbeschriftung gezählt.
Die Konfigurationsprüfung erfasst jetzt 30 Felder in acht Gruppen und verlangt für
die beiden Aufbewahrungsfelder die bewusst abweichende Voreinstellung samt Erklärung
für unbegrenzte Aufbewahrung. Alle bisherigen Geheimnis-/Regelprüfungen bleiben erhalten.
Keine abgeschwächte Sicherheitsassertion und keine Anwendungskorrektur erforderlich.
Die 152 eindeutigen Fälle je Sprache sind anhand ihrer Allure-History-IDs abgeglichen;
rote Vorläufe werden durch spätere grüne Nachweise derselben Fälle erklärt.
DE und EN enthalten exakt dieselben Fall-IDs. CONFIG-05A wartet jetzt mit einer
präzisen, wiederholenden URL-Assertion auf die tatsächliche Navigation, statt direkt
nach einer möglicherweise von einem Poll stammenden UI-Antwort die alte URL zu lesen.
Alle neun Konfigurationsfälle wurden danach in beiden Sprachen erfolgreich nachgeprüft.
Setup, Datenbereinigung, Coverage-Erzeugung und HTML-Berichte waren in allen sechs
abschließend verwendeten Läufen erfolgreich. Keine Anwendungskorrektur erforderlich.

Alle Browserläufe verwenden das feste Produktionsimage
`sha256:e92a821d8377532bde350ab12ad0dee07fff0463db144137a3f0b4554f6a5bac`.

## Coverage und Grenzen

Frische JUnit-Messung: **9868/11298 Zeilen (87,34 %)** und
**2754/3642 Zweige (75,62 %)**, 211 ausführbare Anwendungsklassen.
Shared-JAR, Testcode und Bibliotheken gehören nicht zu dieser Messung.
Alle sechs zur finalen Auswertung verwendeten Produktions-JARs stimmen per SHA-256 überein:
`31da97d74dfc564f4060dba105462c979d34094c717323b2005eea1e24a8a005`.
Zusammengeführte Browsermessung DE/EN: **9660/11298 Zeilen (85,50 %)** und
**2406/3642 Zweige (66,06 %)**. Zusammengeführt wurden ausschließlich Rohdaten
desselben Produktions-JARs; JUnit und Browser bleiben getrennte Messungen.
Browserwerte enthalten auch Serverstart und Hintergrundaufgaben. Sie zeigen
Java-Ausführung, keine vollständige fachliche Abdeckung und keine Frontend-Coverage.
Die geänderte Zeilenstruktur nach der Lesbarkeitsüberarbeitung begrenzt den direkten
Prozentvergleich mit älteren Quellständen.
JUnit-Lücken liegen unter anderem in sichtbarer Verifikations-/Loginsteuerung,
Adminformularen und UI-Aktualisierungen; die Browserjourneys prüfen diese gesondert.
Keine behauptete CSS-/JavaScript-Coverage oder vollständige Kombinationenabdeckung.
Reale Geräte, Screenreader und reale externe Mailclients bleiben menschliche Sonderprüfungen.

## Abschluss und Übergang

Build, Formatierungs-/Klammer-/Importprüfung und `git diff --check` erfolgreich.
287 Anwendungs-/Shared-Quell-, Ressourcen- und POM-Dateien wurden per SHA-256
gegen den getesteten Stand abgeglichen; alle 69 Browser-Java-Dateien entsprechen
dem zuletzt geprüften Helfer-/Kompilierstand. Die eigene temporäre Quellkopie wird
nach diesem Abgleich entfernt. Keine realen Daten oder fremden Prozesse verändert.
Die Änderungen betreffen nur Tests und Dokumentation; sie werden beim nächsten
Testlauf verwendet. Für diese QA ist kein Neustart der Entwicklungsanwendung nötig.

Die separat beauftragte Logging-Überarbeitung folgt erst nach diesem vollständigen
QA-Abschluss und erhält einen eigenen Änderungs-/Prüfnachweis. Die oben genannten
Browserquoten gelten für den geprüften Stand vor dieser nachfolgenden Überarbeitung.
