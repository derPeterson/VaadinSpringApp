# Geplante Nachtarbeit

**Status: Gesamtpakete 1–3 technisch abgeschlossen und lokal zwischenintegriert;
Sonar ist mit null offenen Findings und bestandenem Gate bestätigt;
die abschließende große Testrunde folgt auf dem neuen Folgebranch.**

Der ausdrückliche Startauftrag umfasst alle fünf folgenden Arbeitspunkte.
Sie konkretisiert die Nachtpakete der laufenden
[Vaadin-Komponentenübernahme](vaadin-komponenten-abgleich.md) auf dem Branch
`codex/vaadin-komponenten-folgearbeiten`. Die gesamte Komponentenübernahme bleibt
auch nach der beauftragten Zwischenintegration offen.

## Gesamtpaket 1: Verbleibende Panels und Karten auf native Vaadin-Cards umstellen

Ziel ist die Übernahme der verbleibenden geeigneten Kartenhüllen auf native
Vaadin-`Card`-Komponenten. Aussehen, Texte, Icons, Abstände, Reihenfolge,
Bedienung und fachliches Verhalten bleiben erhalten. Bereits übertragene
öffentliche Standardpanels, Anmeldeanbieter-Nebenkarte und Kennzahlkarten
bilden die Vergleichsgrundlage und werden nicht erneut umgebaut.

Die gemeinsamen Konto-, Verwaltungs- und Ticketpanelhüllen einschließlich der
geschützten Zwischenansichten sowie die geeigneten Innen-, Vorschau- und
Kontextkarten verwenden native Cards. Die technische Übernahme und die isolierten
Vorher-/Nachher-Prüfungen sind abgeschlossen; offen bleibt die gemeinsame
menschliche Sichtung gemäß [HUM-01](../manual-tests/README.md#hum-01).

## Gesamtpaket 2: Verbleibende Bestätigungen auf nativen ConfirmDialog umstellen

Die zehn weiteren Bestätigungsfälle verwenden inzwischen den nativen
`ConfirmDialog`: weitere Benachrichtigungsaktionen, Sitzungen, Profilbildlöschung,
Formularschutz, manuelle Mailqueue-Bereinigung, Benutzerlöschung und die Vorschau
der Wiederherstellungsanleitung. Die gemeinsame Portal-Hülle erhält ihre
fachlichen Aufrufer; die administrative Benutzerlöschung behält ihren eigenen
Kontext und die erneute ausdrückliche Bestätigung nach einem Fehler.

Die gezielten JUnit-Prüfungen und je 16 Vorher-/Nachher-Browserfälle in DE/EN auf
Desktop und Smartphone sind bestanden. Je Sprache stimmen 61 Messwertsätze
überein; 212 Bildpaare stehen zur Sichtung bereit. Die gemeinsame Grundlage
erledigt mehrere Teilfälle zusammen; dafür werden keine leeren Einzelcommits
erzeugt. Formular-, Bearbeitungs-, Identitäts- und seitliche Hilfedialoge bleiben
auf `Dialog`. Offen bleibt die menschliche Sichtung gemäß
[HUM-01](../manual-tests/README.md#hum-01).

## Gesamtpaket 3: Mailqueue vollständig in internen Paketen bereinigen

Die fachliche Umsetzung ist abgeschlossen: Dauerhafte manuelle Aufträge warten
bei laufendem Versand und werden ohne weiteren Klick fortgesetzt. Der Dienst
verarbeitet einen fest abgegrenzten Gesamtlauf in eigenen 100er-Transaktionen,
summiert die bestätigten Pakete und prüft die aktuelle Berechtigung vor jedem
Paket. Neustart, konkurrierende Anforderungen, Rechteentzug und Teilfehler sind
abgesichert. Historie und Anzeige unterscheiden Warten, laufende Verarbeitung,
Abschluss, Fehler und tatsächlichen Teilfortschritt. Die Oberfläche zeigt keine
Gesamtgrenze und meldet nur tatsächlichen Abschluss beziehungsweise Fehler.
Dialogtexte und Statusmeldungen sind in DE/EN aktualisiert; das freigegebene
Cosmos-Design bleibt erhalten. Der aktuelle Vertrag steht unter
[Mailqueue](../mailqueue.md).

88 gezielte JUnit-Prüfungen und je neun isolierte Queue-Browserfälle in DE/EN
sind bestanden, einschließlich Warten, 101 Aufträgen ohne Neuversand, Rechteentzug
sowie Direktlink, F5 und Rollenprüfung in beiden Breiten. Der englische Dialogfall
prüft zusätzlich alle vier Abbruchwege; die vollständige deutsche Wiederholung
dieser Erweiterung folgt in der großen Testrunde. Die Vergleichsaufnahmen stehen
zur menschlichen Sichtung bereit. Offen bleibt nur der zentral erhaltene
[Testbedarf](../testbedarf.md) und die abschließende Gesamtrunde.

## Rahmen für die Nachtarbeit

Die laufende Entwicklungsanwendung und ihre Daten bleiben für diese Arbeit
unverändert. Prüfungen verwenden eigene isolierte Dienste und Testdaten.
Nach jedem Teilpaket sind Review und Commit freigegeben. Die Lesbarkeitsprüfung
beschränkt sich für diesen Nachtauftrag auf die veränderten Bereiche.
Push, Release und die zurückgestellten Planungen gehören nicht zum Auftrag.

## Arbeitspunkt 4: Zwischenintegration, Sonar-Bereinigung und Folgebranch

Der Nachtstand einschließlich Sonar-Korrekturen ist bei `98186b91` lokal auf
`main` integriert. Der geschützte vollständige Nachscan bestätigt null offene
Issues und Hotspots sowie Quality Gate OK. Die zwölf neuen Hinweise sind durch
Code-/Kommentarkorrekturen erledigt; bestehende 46 Ausnahmen bleiben unverändert.
Produktions-/Coverage-Reactor, 3.086 Anwendungstests, 131 Browser-Helfertests und
der isolierte Start des frischen Main-JARs sind bestanden. Zwei ausdrücklich
optionale Caddy-Fälle sind übersprungen. Der Quellbranch bleibt erhalten; die
gesamte Komponentenübernahme ist ausdrücklich nicht abgeschlossen. Weitere
Arbeit läuft auf `codex/vaadin-komponenten-folgearbeiten-4`. Kein Push oder Release.

## Arbeitspunkt 5: Große Testrunde als letzter Schritt

**Status: nach bestandenem Sonar fortgesetzt.** Die gepflegten Offlineprüfungen und die
gezielten Hintergrundprüfungen sind bestanden. Die vorläufige Gesamtrunde ist
für die wieder mögliche Sonar-Bereinigung bewusst beendet; sie zählt nicht als
abgeschlossener Gesamtnachweis. Anschließend beide Sprachen am endgültigen Image
isoliert prüfen.

Zuerst Gesamtpakete 1, 2 und 3 einschließlich der jeweiligen gezielten Prüfungen,
Reviews und Commits sowie Arbeitspunkt 4 abschließen, danach auf dem neuen
Folgebranch die große Testrunde durchführen.
Sie bleibt auch bei zusätzlich aufgenommenen Arbeitspaketen der letzte Punkt;
weitere Umsetzungen werden davor eingeordnet. Gezielte Prüfungen je Teilpaket
bleiben erhalten und werden nicht bis zum Abschluss aufgeschoben.

- [ ] Den gesamten entstandenen Änderungsumfang gegen den Ausgangsstand prüfen,
  einschließlich aller Karten-, Dialog- und Mailqueue-Änderungen.
- [ ] Große Testrunde gemäß Projektablauf mit JUnit und isolierten Browserprüfungen
  in DE/EN durchführen; Desktop und Smartphone sowie Wechselwirkungen abdecken.
  Die zentral erhaltenen offenen Nachtests berücksichtigen.
- [ ] Erkannte Fehler beheben und passend nachprüfen. Offene automatisierte
  Prüfungen ausschließlich im [Testbedarf](../testbedarf.md) erhalten.
- [ ] [Prüfstand](../status.md), Theme-Dokumentation und offene Planung auf den
  tatsächlichen Stand bringen. Ergebnisse und nötige menschliche Sichtungen nennen.
