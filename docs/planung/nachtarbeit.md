# Geplante Nachtarbeit

**Status: gesammelt, noch nicht zur Umsetzung gestartet.**

Diese Liste wird erst nach einem ausdrücklichen Startauftrag abgearbeitet.
Sie konkretisiert die Nachtpakete der laufenden
[Vaadin-Komponentenübernahme](vaadin-komponenten-abgleich.md) auf dem Branch
`codex/vaadin-komponenten-folgearbeiten`. Das Anlegen dieser Datei startet weder
Umbauten noch Tests oder Commits.

## Gesamtpaket 1: Verbleibende Panels und Karten auf native Vaadin-Cards umstellen

Ziel ist die Übernahme der verbleibenden geeigneten Kartenhüllen auf native
Vaadin-`Card`-Komponenten. Aussehen, Texte, Icons, Abstände, Reihenfolge,
Bedienung und fachliches Verhalten bleiben erhalten. Bereits übertragene
öffentliche Standardpanels, Anmeldeanbieter-Nebenkarte und Kennzahlkarten
bilden die Vergleichsgrundlage und werden nicht erneut umgebaut.

Die vorläufige Inventur umfasst 17 Erzeugungsstellen größerer Panels,
sechs kleinere Vorschau-/Kontextkartenstellen und fünf wiederverwendete
Innenkarten-Typen. Diese 28 technischen Stellen sind keine 28 getrennten
Seiten; gemeinsame Bausteine und dynamische Listen haben mehrere Verbraucher.
Vor jedem Teilpaket die tatsächlichen Aufrufer und die fachliche Eignung der
nativen Card prüfen. Unklare oder ungeeignete Fälle begründet zurückstellen.

### Arbeitspaket 1.1: Gemeinsame Kontopanels

- [ ] `AccountPanelComponent` und seine Verwendung für persönliche Daten,
  Sicherheit sowie Geräte & Sitzungen prüfen und geeignet umstellen.
- [ ] Kopf, Metadaten, Inhalt, Hinweisfooter und Aktionen über die öffentlichen
  Card-APIs erhalten; gemeinsame Cosmos-Gestaltung weiterverwenden.
- [ ] Alle von einer Änderung der gemeinsamen Basis betroffenen Verbraucher
  einbeziehen, auch wenn sie fachlich in einem späteren Teilpaket stehen.

### Arbeitspaket 1.2: Geschützte Zwischen- und Wiederherstellungsseiten

- [ ] 2FA-Einrichtung, Anmeldebestätigung und Wiederherstellung prüfen.
- [ ] Sitzungslimit beim Anmelden und Wiederherstellungshilfe einbeziehen.
- [ ] Bestehende Ablauf- und Navigationssperren, Fokus, Codeeingabe,
  Bestätigungsaktionen und Ticketfunktionen unverändert erhalten.

### Arbeitspaket 1.3: Administrative Konfigurationskarten

- [ ] Allgemeine Konfigurationskarte für die verschiedenen Einstellungsgruppen
  und die gesonderten Anmeldeanbieter-/2FA-Karten umstellen.
- [ ] Mailtest und innere Karte des Mailqueue-Bereinigungsassistenten einbeziehen.
- [ ] Tabs, Feldhilfen, Einstellungszähler, Formularschutz, Lade-/Speicheraktionen
  sowie Rollen- und Sichtbarkeitsregeln erhalten.

### Arbeitspaket 1.4: Verwaltung, Benachrichtigungen und Tickets

- [ ] Hauptkarte der Benutzerverwaltung sowie die Profilkarte der Sidebar prüfen.
- [ ] Persönliches Benachrichtigungspanel sowie administrative Versand- und
  Historienkarten umstellen.
- [ ] Administrative Ticketliste und Ticketdetail prüfen; bereits über die
  gemeinsame Basis erfasste Wiederherstellungskarten berücksichtigen.
- [ ] Listen, Tabellen, Filter, Pagination, Vorschau und Aktionen unverändert erhalten.

### Arbeitspaket 1.5: Kleine Innen-, Vorschau- und Kontextkarten

- [ ] Karten für Passwort/E-Mail, 2FA, verbundene Anmeldeanbieter, einzelne
  Sitzungen und Kontoverwaltungsaktionen prüfen und geeignet umstellen.
- [ ] Benachrichtigungsvorschau sowie `PortalContextCard` und seine Verbraucher
  in Bestätigungsdialogen einbeziehen.
- [ ] Dynamische Wiederholungen über gemeinsame Bausteine abdecken; keine
  parallelen Kartendesigns oder unnötigen Einzelkomponenten einführen.

### Ablauf nach dem Startauftrag

1. Teilpakete nacheinander bearbeiten. Vor dem jeweiligen Umbau die bestehenden
   Ansichten und passenden Vaadin-APIs prüfen und isolierte Vorher-Aufnahmen sichern.
2. Nach dem Umbau gezielt in DE und EN auf Desktop und Smartphone prüfen.
   Pro Vergleichsstand dasselbe unveränderte Anwendungsimage verwenden.
3. Vorher-/Nachher-Screenshots und Layoutmesswerte vergleichen; Ansichten mit
   langen Texten, Fehlern, leeren Zuständen und verfügbaren Aktionen berücksichtigen.
4. Je Teilpaket Review einschließlich Lesbarkeit nach
   [Entwicklungsanleitung](../entwicklung.md#codestil) durchführen, nötige Korrekturen
   nachprüfen und einen eigenen aussagekräftigen Commit erstellen.
5. Gemeinsame Grundlagen nicht vorzeitig an ungeprüften Verbrauchern verändern.
   Durch Wiederverwendung bereits erledigte Teilaufgaben nicht doppelt umbauen;
   keine leeren Commits zur Erfüllung der Paketnummerierung erzeugen.
6. Für die gemeinsame Sichtung die betroffenen Stellen und die Vorher-/Nachher-
   Vergleiche bereitstellen. Die gesamte Komponentenübernahme nicht eigenmächtig
   als abgeschlossen erklären.

## Direkt nach Gesamtpaket 1: Große Testrunde

Die große Testrunde folgt nach den fünf Teilpaketen einschließlich ihrer Reviews
und Commits, bevor ein weiteres Gesamtpaket begonnen wird.

- [ ] Den gesamten entstandenen Änderungsumfang gegen den Ausgangsstand prüfen.
- [ ] Große Testrunde gemäß Projektablauf mit JUnit und isolierten Browserprüfungen
  in DE/EN durchführen; Desktop und Smartphone sowie Wechselwirkungen abdecken.
- [ ] Erkannte Fehler beheben und passend nachprüfen. Offene automatisierte
  Prüfungen ausschließlich im [Testbedarf](../testbedarf.md) erhalten.
- [ ] [Prüfstand](../status.md), Theme-Dokumentation und offene Planung auf den
  tatsächlichen Stand bringen. Ergebnisse und nötige menschliche Sichtungen nennen.

## Rahmen für die Nachtarbeit

Die laufende Entwicklungsanwendung und ihre Daten bleiben für diese Arbeit
unverändert. Prüfungen verwenden eigene isolierte Dienste und Testdaten.
Merge, Push, Release, Sonar-Bereinigung und die zurückgestellten Planungen gehören
nicht zu diesem Nachtpaket.
