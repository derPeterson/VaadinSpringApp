# Geplante Nachtarbeit

**Status: vollständig zur Umsetzung freigegeben; Bearbeitung gestartet.**

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
geschützten Zwischenansichten verwenden die native Card-Basis. Offen bleiben die
folgenden Innenkarten und die gemeinsame menschliche Sichtung gemäß
[HUM-01](../manual-tests/README.md#hum-01).

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

## Gesamtpaket 2: Verbleibende Bestätigungen auf nativen ConfirmDialog umstellen

Dieses Paket folgt nach Gesamtpaket 1. Die große Testrunde steht erst nach allen
Gesamtpaketen am Ende der Nachtarbeit. Es umfasst zehn weitere Dialogfälle.
Mehrfache Verwendungen derselben Komponente und Varianten für aktive beziehungsweise
gespeicherte Sitzungen werden nicht als eigene Umbauten gezählt.

Der abgenommene Pilot „Alle Benachrichtigungen als gelesen markieren“ in Inbox
und Glockenvorschau bildet die Grundlage. Aussehen, Texte in DE/EN, Icons,
Warnfarben, Kontextkarten, Buttonbreiten und fachliches Verhalten bleiben erhalten.
Formular-, Bearbeitungs-, Identitäts- und seitliche Hilfedialoge bleiben auf
Vaadin-`Dialog` und gehören nicht zu diesem Paket.

### Arbeitspaket 2.1: Weitere Benachrichtigungsbestätigungen – drei Fälle

- [ ] Einzelne Benachrichtigung als gelesen markieren.
- [ ] Einzelne Benachrichtigung löschen.
- [ ] Gelesene Benachrichtigungen gesammelt löschen.
- [ ] Inbox, Glockenvorschau und gegebenenfalls geöffnetes Nachrichtendetail
  berücksichtigen; Auswahl und Zeitpunkt der jeweiligen Aktion erhalten.

### Arbeitspaket 2.2: Sitzungsbestätigungen – zwei Fälle

- [ ] Einzelne Sitzung abmelden beziehungsweise gespeicherte Anmeldung widerrufen.
- [ ] Andere Sitzungen und gespeicherte Anmeldungen gesammelt widerrufen.
- [ ] Kontextangaben, Schutz der aktuellen Sitzung und eingeschränkte
  Wiederherstellungsabläufe in allen Verbrauchern erhalten.

### Arbeitspaket 2.3: Profilbildlöschung – ein Fall

- [ ] Löschbestätigung im Profilbilddialog umstellen.
- [ ] Entwurfszustand und Schutz gegen inzwischen geschlossenen oder durch
  einen anderen Upload veränderten Bildentwurf erhalten; die Bestätigung
  darf keine vorgezogene dauerhafte Löschung auslösen.

### Arbeitspaket 2.4: Ungespeicherte Änderungen – ein Fall

- [ ] Gemeinsame Verwerfungsbestätigung für Aktionen und interne Navigation
  umstellen und alle bestehenden Formularverbraucher einbeziehen.
- [ ] Weiterbearbeiten, Verwerfen sowie Fortsetzen oder Abbrechen einer
  angehaltenen Navigation unverändert erhalten. Suchfelder und Filter bleiben
  außerhalb des Formularschutzes; die Browser-Verlassenswarnung bleibt erhalten.

### Arbeitspaket 2.5: Administrative Bestätigungen – zwei Fälle

- [ ] Die Bestätigung der manuellen Mailqueue-Bereinigung technisch auf
  `ConfirmDialog` übertragen; Superadmin-Prüfung und Verwendung der gespeicherten
  Einstellungen erhalten. Die separat beauftragte Gestaltung nach Variante A
  mit Brush-Icons ist die Grundlage und kein erneut abzuarbeitender Designauftrag.
- [ ] Administrative Benutzerlöschung mit Benutzerkontext umstellen;
  Berechtigungen, Löschschutz und bestehende Fehlerbehandlung erhalten.

### Arbeitspaket 2.6: Wiederherstellungsanleitung – ein Fall

- [ ] Vorschau und Versandbestätigung der Wiederherstellungsanleitung im
  Ticketbereich umstellen.
- [ ] Längeren Vorschauinhalt, Scrollverhalten und Aktionen berücksichtigen;
  Versand erfolgt weiterhin erst nach Bestätigung und erneuter fachlicher
  Prüfung des Tickets.

### Ablauf und Prüfungen für Gesamtpaket 2

1. Vor jedem Teilpaket bestehende Ansicht, Aufrufer und öffentliche APIs der
   verwendeten Vaadin-Version prüfen. Geeignete gemeinsame Dialoggestaltung
   wiederverwenden; bei einer konkreten Funktionslücke vor einem eigenen Ersatz
   Rückfrage halten. Die Vorschau in Arbeitspaket 2.6 besonders auf Eignung prüfen.
2. Gemeinsame Grundlagen nur mit Prüfung aller betroffenen Verbraucher ändern.
   Bereits durch eine gemeinsame Übernahme erledigte Fälle nicht doppelt umbauen
   und keine leeren Commits erzeugen.
3. Isolierte Vorher-/Nachher-Screenshots in DE/EN auf Desktop und Smartphone
   erstellen und Layoutmesswerte vergleichen. Pro Vergleichsstand dasselbe
   unveränderte Anwendungsimage verwenden.
4. Passende Regressionen für Bestätigen, Abbrechen, Close, Escape, Außenklick,
   Tastaturfokus und Sprachwechsel ausführen. Einmalige Ausführung, native
   Ereignisreihenfolge, erneutes Öffnen, verspätete Schließereignisse sowie
   fachliche Fehler- und Widerrufsfälle absichern.
5. Nach jedem Teilpaket Lesbarkeit und Änderungen reviewen, nötige Korrekturen
   nachprüfen und einen eigenen aussagekräftigen Commit erstellen.
   Gemeinsame Theme-Dokumentation und offene Planung aktuell halten.
6. Betroffene Stellen und Vorher-/Nachher-Vergleiche zur gemeinsamen Abnahme
   bereitstellen. Offenen automatisierten Testbedarf ausschließlich in
   [Testbedarf](../testbedarf.md) führen.

Gesamtpaket 2 ist mit dem gemeinsamen Startauftrag freigegeben.

## Gesamtpaket 3: Mailqueue vollständig in internen Paketen bereinigen

**Status: zur Umsetzung freigegeben.**
Ein Bereinigungslauf soll alle zu seinem Start passenden alten ungeklärten
Versandaufträge bearbeiten können, ohne sichtbare Gesamtgrenze von 100.
Intern bleiben Pakete von höchstens 100 Einträgen mit kurzen Transaktionen.
Das gilt für manuelle und automatische Läufe des gemeinsamen Bereinigungsdienstes.
„Bereinigen“ bedeutet fachlich Abschließen ohne erneuten Versand, nicht pauschales
Löschen aller Mails oder der Queue. Die bestehenden Sonderregeln für
Löschbestätigungen bleiben erhalten.

### Arbeitspaket 3.1: Paketweise Verarbeitung des vollständigen Laufs

- [ ] Eine bestätigte manuelle Bereinigung als serverseitigen Auftrag vormerken.
  Läuft noch Mailversand, bleibt sie unter „Wartet auf Ausführung“ vorgemerkt
  und startet im Hintergrund automatisch, sobald eine sichere Ausführung möglich
  ist. Kein wiederholtes Klicken und kein manueller Status „Übersprungen“ nötig.
- [ ] Weitere Klicks bei vorgemerktem oder laufendem Auftrag dürfen keinen zweiten
  Lauf erzeugen. Vormerkung, Übernahme und Abschluss zuverlässig koordinieren;
  Neustart, Rechteentzug und konkurrierende automatische Läufe berücksichtigen.
  Die Vormerkung ersetzt keine spätere Berechtigungsprüfung. Ein vorgemerkter
  Bereinigungsauftrag ist kein Auftrag zum erneuten Mailversand.
- [ ] Eine Schleife über interne 100er-Pakete ergänzen; Abfrage und Änderungen
  je Paket in einer eigenen kurzen Transaktion ausführen.
- [ ] Laufbeginn, gespeicherte Einstellungen und Altersgrenze einmal bestimmen;
  nicht während des Laufs immer neue inzwischen gealterte Aufträge aufnehmen.
- [ ] Verarbeitung beenden, wenn keine passenden Einträge mehr vorhanden sind.
  Veränderte oder nicht mehr bereinigungsfähige Einträge korrekt behandeln;
  Stillstand und Endlosschleifen verhindern.
- [ ] Schutz vor gleichzeitigem Versand und konkurrierenden Bereinigungsläufen,
  frische Berechtigungsprüfungen und bestehende Status-/Datenschutzregeln erhalten.
  Die Sperrgrenzen passend zu den getrennten Transaktionen prüfen.

### Arbeitspaket 3.2: Gesamtergebnis, Historie und Teilfehler

- [ ] Die vom Benutzer freigegebene Statusgestaltung aus der HTML-Skizze umsetzen:
  neutrale Kennzeichnung „Noch nicht ausgeführt“, grün „Abgeschlossen“,
  rot „Fehlgeschlagen“, ergänzt um gelb „Wartet auf Ausführung“ für einen
  vorgemerkten manuellen Auftrag. Warten ist kein Fehler und zeigt keine
  fehlgeschlagene Bereinigung an. Bei Abschluss zusätzlich in Grün
  „Die Bereinigung konnte erfolgreich abgeschlossen werden.“ anzeigen;
  bei Fehler die entsprechende rote Erklärung beibehalten. Für „Noch nicht
  ausgeführt“ ist keine zusätzliche Erfolg-/Fehlerzeile vorgesehen.
  Überschrift bleibt blau; Layout, Terminal-Icon und Aktionen bleiben wie gezeigt.
- [ ] Beim manuellen Ausführen zusätzlich die bestehenden Notifications für
  tatsächlichen Abschluss beziehungsweise Fehler anzeigen und in DE/EN pflegen.
  Bei Vormerkung informieren: „Die Bereinigung wartet auf den laufenden Mailversand.“
  Nach der automatischen Ausführung folgt die tatsächliche Abschluss- oder
  Fehlermeldung. Das Öffnen des Bestätigungsdialogs ist noch kein Erfolg.
  Auch ein erfolgreicher Lauf mit 0 Treffern bekommt die Erfolgsmeldung;
  bei Fehlern oder Teilergebnissen
  keinen vollständigen Erfolg behaupten. Automatische Hintergrundläufe erzeugen
  keine zusätzlichen allgemeinen UI-Notifications.
- [ ] Ein echtes Teilergebnis gelb mit passender Warnnotification kennzeichnen,
  ohne vollständigen Erfolg zu behaupten. Eine bloße Versandblockierung vor dem
  manuellen Start führt dagegen zum Warten. Übersprungene automatische Prüfungen
  davon getrennt behandeln und einen vorgemerkten manuellen Auftrag nicht mit
  solch einem Ergebnis überschreiben. Die bisherige Speicherung von Zeitpunkt,
  Anzahl und `busy` genügt für Vormerkung, Fehler-/Teilstatus noch nicht;
  Modell und aktuelles Schema bei der Umsetzung passend ergänzen.
- [ ] Bereinigte Aufträge über alle erfolgreich abgeschlossenen Pakete summieren.
  Statusanzeige, Rückmeldung und Historie bilden einen logischen Gesamtlauf ab;
  Ergebnisse über 100 korrekt anzeigen, beispielsweise 250 als Gesamtergebnis
  statt der einzelnen Paketstände 100, 100 und 50.
- [ ] Bei einem späteren Fehler bereits bestätigte Pakete nicht als zurückgerollt
  darstellen. Teilfortschritt und Fehler beziehungsweise Blockierung ehrlich
  melden und speichern; kein vollständiger Erfolg bei unvollständigem Lauf.
- [ ] Leere Läufe, laufender Versand, Rechteentzug und einen späteren erneuten
  Versuch berücksichtigen. Verbleibende Einträge müssen weiter bearbeitbar sein.

### Arbeitspaket 3.3: Dialogtexte und Informationsgliederung in DE/EN

- [ ] Die sichtbare Aussage „höchstens 100 pro Lauf“ vollständig entfernen;
  die interne Paketgröße ist kein für die Bedienung nötiges Oberflächenlimit.
- [ ] Dialog und zugehörige Hinweise vollständig fachlich gegenprüfen und
  verständlich formulieren: gespeicherte Einstellungen, Mindestalter,
  kein erneuter Versand und möglicher späterer Empfang. Doppelte Aussagen
  vermeiden; Texte müssen auch zu Teilfehlern und automatischen Läufen passen.
- [ ] Die freigegebene Variante A mit Brush-Icons, Warnbox und dezenter Infobox
  beibehalten. Zusammengehörige Aussagen dürfen mit einem Zeilenumbruch getrennt
  werden; unterschiedliche Gedanken erhalten lesbare Absätze.
- [ ] Vorhandene Absatzabstände, insbesondere bei 2FA, als Vorlage prüfen und
  bestehende Cosmos-Klassen/Tokens übernehmen. Keine feste neue 8-px-Vorgabe
  ableiten und keine globale Typografieänderung vornehmen.

### Arbeitspaket 3.4: Nachweise und Abschluss

- [ ] Die zentral erfassten JUnit- und Browserprüfungen im
  [Testbedarf](../testbedarf.md) passend ergänzen und ausführen; eigene isolierte
  Testdaten verwenden. DE/EN auf Desktop und Smartphone einschließlich der
  Textumbrüche und Infoabsätze prüfen und Vorher-/Nachher-Belege bereitstellen.
- [ ] Überschneidungen mit der Dialogübernahme in Paket 2.5 berücksichtigen;
  die Mailqueue-Gestaltung nicht ein zweites Mal neu entwerfen.
- [ ] Nach jedem Teilpaket Review, nötige Nachprüfung und eigenen aussagekräftigen
  Commit vorsehen; Fachdokumentation und offene Planung auf den tatsächlichen
  Stand bringen. Noch offene Testarbeit bleibt ausschließlich im Testbedarf.

## Rahmen für die Nachtarbeit

Die laufende Entwicklungsanwendung und ihre Daten bleiben für diese Arbeit
unverändert. Prüfungen verwenden eigene isolierte Dienste und Testdaten.
Nach jedem Teilpaket sind Review und Commit freigegeben. Die Lesbarkeitsprüfung
beschränkt sich für diesen Nachtauftrag auf die veränderten Bereiche.
Push, Release und die zurückgestellten Planungen gehören nicht zum Auftrag.

## Arbeitspunkt 4: Zwischenintegration, Sonar-Bereinigung und Folgebranch

- [ ] Nach Gesamtpaket 3 den geprüften und committeten Arbeitsstand lokal nach
  `main` mergen. Den Quellbranch erhalten; die gesamte Vaadin-Komponentenübernahme
  ist ausdrücklich noch nicht abgeschlossen. Offene Planung und Abnahmen erhalten.
- [ ] Auf `main` den vollständigen geschützten Sonar-Scan durchführen, alle
  berechtigten Findings beheben und den Endstand durch einen Nachscan mit null
  offenen Findings bestätigen. Befunde fachlich prüfen, nichts für die Null
  unterdrücken. Erforderliche lokale Korrekturcommits und Zwischenbranches sind
  vom Gesamtauftrag umfasst; keine Pushes.
- [ ] Anschließend einen neuen Folgebranch mit ähnlichem Namen anlegen,
  beispielsweise `codex/vaadin-komponenten-folgearbeiten-3`, sofern frei, und die
  weitere Komponentenplanung dort fortführen.

## Arbeitspunkt 5: Große Testrunde als letzter Schritt

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
