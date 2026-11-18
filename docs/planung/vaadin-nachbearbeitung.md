# Abarbeitungsliste: Formulare, Benachrichtigungen und AvatarGroup

**Stand: Umsetzung aller 14 Punkte nach Review und Commit dieser Liste freigegeben.**

Diese Liste konkretisiert die nächsten vereinbarten Arbeiten des
[Vaadin-Komponenten-Abgleichs](vaadin-komponenten-abgleich.md). Die Gesamtumsetzung
bleibt offen. Ausgangsstand ist `86f9cb25` auf
`codex/vaadin-komponenten-folgearbeiten-5`.

## Gemeinsamer Ablauf

- Das ausdrückliche Go liegt vor. Zuerst diese Liste prüfen und committen,
  anschließend die Pakete gemäß den folgenden Regeln abarbeiten.
- Punkte 1–13 einzeln abarbeiten: Änderungen umsetzen, passende vorhandene
  JUnit-Tests anpassen beziehungsweise sinnvolle Regressionen ergänzen und ausführen,
  anschließend Review und **jeweils einen eigenen Commit** erstellen.
- Commit-Messages auf Deutsch, aussagekräftig und in abgeschlossener Form schreiben.
  Prüfungen und wesentliche Entscheidungen kurz berichten.
- Verbindliche Formatierungs- und Stilprüfungen gemäß `AGENTS.md` ausführen.
  Bei Reviews die Lesbarkeit der geänderten Klassen prüfen; keine nebenläufige
  Projektbereinigung eröffnen.
- **Keine Browserläufe starten.** Offene automatisierte Browserprüfungen zentral in
  [Testbedarf](../testbedarf.md) ergänzen oder bestehende Einträge aktualisieren.
  Menschliche Sichtung erfolgt später durch den Benutzer; Abnahmen im
  [manuellen Katalog](../manual-tests/README.md#hum-01) festhalten.
- Alle betroffenen Texte und dynamischen Meldungen auf Deutsch und Englisch pflegen.
  Vorhandene Cosmos-Tokens und native Vaadin-Komponenten verwenden.
- Bestehende Berechtigungen, serverseitige Sicherheitsprüfungen, Versand- und
  Konsistenzregeln erhalten. Ein Komponentenwechsel allein erweitert keine Rechte.
- Bei einer notwendigen fachlichen Entscheidung oder einer fehlenden nativen API
  die konkrete Lücke erklären und vor der davon abhängigen Sonderlösung nachfragen.
  Unabhängige freigegebene Punkte können weiterbearbeitet werden.
- **Punkt 14 erst nach den abgeschlossenen und committeten Punkten 1–13 bearbeiten.**
  Dafür den sauberen Ausgangsstand und Commit festhalten. Punkt 14 bleibt vollständig
  uncommittet; danach keinen Sammel- oder Abschlusscommit anlegen.
- Kein automatischer Merge, Release oder Abschluss des gesamten Komponentenumbaus.

## Gemeinsames Validierungsverhalten für Punkte 2, 3, 4 und 7

- Vor der jeweiligen Aktion keine roten Markierungen allein durch Eingeben,
  Leeren oder Verlassen eines Feldes.
- Erst bei Speichern, Senden beziehungsweise Vorschau auf dem Server prüfen,
  bevor die Fachaktion ausgeführt wird.
- Betroffene Felder rot markieren, aber **keine Fehlertexte unter den Feldern**
  anzeigen. Bestehende normale Feldhilfen bleiben erhalten.
- Eine Notification mit der Überschrift **„Fehlgeschlagen“ / „Failed“** und
  konkreter Erklärung anzeigen. Keine zusätzliche Zeile mit dem Feldnamen vor
  dem Fehlertext einfügen. Eine fachlich sinnvolle Erwähnung im Satz bleibt möglich.
- Prüfungen nachvollziehbar von oben nach unten führen. Beim Bearbeiten eines
  Feldes dessen alte Markierung zurücknehmen; bei der nächsten Aktion erneut prüfen.
- Gezielte Zeilenumbrüche nur dort verwenden, wo inhaltlich verschiedene Aussagen
  getrennt werden. Nicht automatisch nach jedem Satz umbrechen.
- Bereits passendes Verhalten beibehalten. Pflichtangaben, erlaubte Leerwerte,
  technische Fehler, Rechte und fachliche Sperren weiterhin unterscheiden.

## 1. Benutzer anlegen und bearbeiten: Rollen und Switches

- Rollen-Auswahl auf Desktop nur in der **linken Spalte** anzeigen, wie Geschlecht
  in der darüberliegenden Feldgruppe. Die rechte Spalte bleibt leer.
- Zugang gesperrt, Verifikation und Entwicklerkonto durch native **Switches** ersetzen.
- Jeder Switch steht in einer eigenen Zeile über **beide Spalten**: technisch
  Spaltenüberspannung beziehungsweise Colspan 2, kein Rowspan.
- Auf Smartphone die vorhandene einspaltige Anordnung beibehalten.
- Beschriftungen, Untertexte und Fachregeln erhalten. Auf ausdrückliche Rückmeldung
  bleiben die bisherigen Sichtbarkeitsregeln bestehen: Verifikation und Entwicklerkonto
  sind nur bei der berechtigten Anlage verfügbar. Im Bearbeitungsdialog wird der
  vorhandene Schalter für die administrative Sperre umgestellt.

- [x] Umsetzung und passende JUnit-Prüfungen abgeschlossen.
- [x] Review abgeschlossen und eigener Commit erstellt.

## 2. Konfiguration: Fehlertexte ohne vorangestellten Feldnamen

- Gesamte Konfiguration einschließlich eigener Speicherabläufe prüfen.
- Die zusätzliche Feldnamen-Zeile aus Fehlernotifications entfernen. Beispiel:
  nach „Fehlgeschlagen“ direkt „Bitte eine gültige URL mit http:// oder https:// eingeben.“
  anzeigen; keine eigene Zeile „Basis-URL“ davor.
- Ebenso beim Anwendungsnamen und allen übrigen Validierungen vorgehen.
- Alle betroffenen Fehlertexte auf Verständlichkeit, fachliche Richtigkeit und
  Wiederholungen prüfen. Rote Feldmarkierungen erhalten.
- Vorhandene JUnit-Erwartungen und offenen Browser-Testbedarf an die neue Meldungsform
  anpassen; keine bisherigen Prüfregeln abschwächen.

- [x] Umsetzung und passende JUnit-Prüfungen abgeschlossen.
- [x] Review abgeschlossen und eigener Commit erstellt.

## 3. Mail-Test: Prüfung erst bei „Test-E-Mail senden“

- Das gemeinsame Validierungsverhalten auf die gesamte Mail-Test-Eingabe anwenden.
- Das Leeren eines Feldes darf vor dem Senden keine rote Markierung auslösen.
- Bei „Test-E-Mail senden“ die jeweiligen Fehler markieren und konkret erklären,
  beispielsweise „Bitte geben Sie einen Betreff ein.“
- Keine Inline-Fehler oder zusätzliche Feldnamen-Zeile anzeigen. Änderungen nehmen
  die jeweilige Markierung zurück.
- Bestehende Versand-, Zugriffs- und Verfügbarkeitsbedingungen erhalten.

- [x] Umsetzung und passende JUnit-Prüfungen abgeschlossen.
- [x] Review abgeschlossen und eigener Commit erstellt.

## 4. Ticketsystem: Formulare und Nachrichtensenden vereinheitlichen

- Die Formulare und Eingaben des Ticketsystems vollständig gegen das gemeinsame
  Validierungsverhalten prüfen. Bereits richtige Stellen beibehalten.
- Insbesondere „Nachricht senden“ beziehungsweise „Antwort hinzufügen“ wie die
  Sendeaktion im Mail-Test behandeln: Prüfung bei der Aktion, nicht schon beim Leeren.
- Betroffene Felder markieren und konkrete Fehlernotifications ohne Inline-Texte
  oder zusätzliche Feldnamen-Zeile anzeigen.
- Rücknahme alter Markierungen, Sprachwechsel sowie fachliche Unterschiede zwischen
  Antworten und internen Vermerken berücksichtigen. Sichtbarkeit und Rechte erhalten.

- [x] Prüfung, nötige Umsetzung und passende JUnit-Prüfungen abgeschlossen.
- [x] Review abgeschlossen und eigener Commit erstellt.

## 5. Ticketsystem: Sicherheitshinweis beim internen Vermerk

- Nach dem ersten Satz des Sicherheitshinweises einen bewussten Zeilenumbruch setzen.
- Die Warnung vor Passwörtern, Authenticator-Geheimschlüsseln und Ersatzcodes von
  der Erklärung der Leserechte trennen.
- Wortlaut und Gestaltung beibehalten; die gleiche inhaltliche Trennung in Englisch
  umsetzen. Die Änderung betrifft den Hinweis bei internen Vermerken.

- [x] Umsetzung und passende Prüfungen abgeschlossen.
- [x] Review abgeschlossen und eigener Commit erstellt.

## 6. Ticketsystem: deaktiviertes „Anleitung senden“ untersuchen

- Ausgangsfall: Ticket „Hilfe beim Authenticator-Zugriff“, Status „In Bearbeitung“,
  angemeldete Person ist zugeordneter Bearbeiter, Button „Anleitung senden“ ist deaktiviert.
- Die Aktivierungsbedingungen in Oberfläche und Fachlogik prüfen. Der Screenshot
  allein bestätigt noch keinen Anwendungsfehler.
- Einen berechtigten Sperrgrund erklären. Falls das Verhalten falsch ist, die Ursache
  beheben und mit einer gezielten Regression absichern.
- Ergebnis der Prüfung: Bei sichtbarer Aktion sind ein Entwicklerkonto oder eine
  bereits beauftragte Anleitung die beiden beabsichtigten Sperrgründe. Beide Fälle
  und das freigegebene reguläre Konto sind isoliert abgesichert. Der konkrete
  Screenshot unterscheidet diese beiden Ursachen nicht; vorhandene Kontodaten
  wurden nicht gelesen. Keine Fachsperre wurde entfernt.
- Keine fachliche Sperre nur entfernen, um den Button anklickbar zu machen.
  Nötige neue Vertragsentscheidungen mit dem Benutzer klären.

- [x] Ursache geklärt, Ergebnis erklärt und nötige Korrektur geprüft.
- [x] Review abgeschlossen und eigener Commit erstellt; bei unverändert richtigem
      Verhalten nur die erforderliche Dokumentation beziehungsweise Tests committen.

## 7. Benachrichtigungen: Prüfung bei der Vorschau

- Gemeinsames Validierungsverhalten auf den Benachrichtigungsentwurf anwenden.
- Erst beim Klick auf **„Vorschau“** prüfen, dann die betroffenen Felder rot markieren
  und konkret erklären, was zu korrigieren ist.
- Keine vorzeitige Rotmarkierung, keine Inline-Fehler und keine zusätzliche
  Feldnamen-Zeile. Beim Bearbeiten alte Markierungen zurücknehmen.
- Die bestehende Bindung zwischen gültigem Entwurf, Vorschau und tatsächlichem
  Senden erhalten; Änderungen dürfen eine veraltete Vorschau nicht weiter freigeben.

- [x] Umsetzung und passende JUnit-Prüfungen abgeschlossen.
- [x] Review abgeschlossen und eigener Commit erstellt.

## 8. Benachrichtigungen: „Alle aktiven Konten“ als Switch

- Die Checkbox für das Senden an **„Alle aktiven Konten“** durch einen nativen
  Switch ersetzen.
- Beschriftung, erklärenden Untertext, Empfängerauswahl und Funktion erhalten.
- Dieser Punkt betrifft Benachrichtigungen, nicht das Ticketsystem.

- [x] Umsetzung und passende JUnit-Prüfungen abgeschlossen.
- [x] Review abgeschlossen und eigener Commit erstellt.

## 9. AvatarGroup: vollständige Empfängerliste über die Avatarreihe

### Gewünschte Bedienung

- Bestehende Avataroptik, Überlaufanzahl und Tooltips erhalten.
- Die **gesamte Avatarreihe** wird zum Einstieg: Handzeiger und Öffnen des gleichen
  Overlays durch Klick auf jeden Avatar, „+N“ oder einen Zwischenraum der Reihe.
- Der Einstieg funktioniert auch bei wenigen Empfängern ohne „+N“ und ist mit
  Tastatur, Enter beziehungsweise Leertaste bedienbar.
- Das Overlay zeigt **alle Empfänger**, nicht nur die vom nativen Überlauf
  ausgeblendeten Personen.
- Je Person Avatar und Name anzeigen; darunter mit Zeilenumbruch die E-Mail-Adresse
  in sekundärer Schrift.
- Oben eine Suche nach **Name und E-Mail-Adresse** anbieten.
- Die Liste scrollen lassen und ihre Höhe begrenzen: etwa 600–700 px als grober
  Desktop-Richtwert, bei kleineren Fenstern und auf Smartphone passend zur verfügbaren Höhe.

### Technische Prüfung und Abnahme

- Zuerst anhand der tatsächlich verwendeten Vaadin-Version prüfen, was über öffentliche
  native APIs möglich ist. Keine unbestätigte Annahme über Renderer oder Überlaufaktionen.
- Falls eine Sonderlösung erforderlich ist, konkrete Lücke und native Alternative
  vorlegen und vor dem Ersatz die Benutzerentscheidung abwarten.
- Zusätzlichen Empfänger-Button, „Details“ und die darunter aufgeklappte Liste nur
  dann entfernen, wenn der neue Einstieg die vollständige Funktion abdeckt.
- Das bestehende offene AvatarGroup-Paket erst nach technisch vollständiger Umsetzung
  **und ausdrücklicher Benutzerabnahme** aus dem Komponenten-Abgleich entfernen.
  Technischer Commit allein ist keine Abnahme; nötige Browsernachtests bleiben zentral offen.

- [x] Native Möglichkeiten geprüft; Popover, Suchfeld, Grid und gekapselter
      Ereignis-Listener ausdrücklich freigegeben. Keine private Vaadin-API.
- [x] Freigegebene Umsetzung und passende JUnit-Prüfungen abgeschlossen.
- [x] Review abgeschlossen und eigener Commit erstellt.
- [ ] Benutzer hat ausdrücklich abgenommen; offenes AvatarGroup-Paket bereinigt.

## 10. Benachrichtigungs-Overlay: Titel und Statuslinie

- Unnötigen Einzug der Titel entfernen. Titel, Nachrichtentext und Metadaten sollen
  links bündig stehen; Ursache der Abweichung prüfen.
- Die linke Rahmen-/Akzentlinie auch bei gelesenen Karten anzeigen:
  **ungelesen Blau**, **gelesen Grau** passend zur vorhandenen grauen Statusgestaltung.
- Lesestatus von Ticketzuständen wie „Bereits übernommen“ fachlich getrennt halten.
- Weitere Farben für mögliche künftige Zustände nur als spätere Möglichkeit festhalten,
  jetzt keine zusätzlichen Zustände einführen.

- [x] Umsetzung und passende Prüfungen abgeschlossen.
- [x] Review abgeschlossen und eigener Commit erstellt.

## 11. Benachrichtigungsdialog: „Als gelesen markieren“ als Primary-Button

- Den Button im Mitteilungsinhalt als nativen **Primary-Button in Blau** darstellen.
- Schriftgröße, Höhe und Innenabstände an die vorhandenen Standardbuttons angleichen.
- Den Abstand zum Kartenrand korrigieren; ungewöhnlich große Schrift und zu knappe
  Randabstände auf lokale Sonderregeln prüfen und unnötige Regeln entfernen.
- Funktion und übersetzte Beschriftung erhalten.

- [x] Umsetzung und passende Prüfungen abgeschlossen.
- [x] Review abgeschlossen und eigener Commit erstellt.

## 12. Benachrichtigungsdialog: Metadaten und Innenabstände

- Die Metadatenzeile mit Typ, Datum und Status erhält nach oben **S**
  (`--cosmos-space-s`, 8 px) und nach unten **M** (`--cosmos-space-m`, 12 px).
- Individuelle Pixelwerte dort durch die vorhandenen Cosmos-Tokens ersetzen.
- Die Herkunft des äußeren Paddings prüfen: Aura-Standard, gemeinsamer Cosmos-Baustein
  oder lokale Sonderregel. Vergleichbare Mitteilungskarten einbeziehen.
- Für die geöffnete Mitteilung **L** (`--cosmos-space-l`, 16 px) als Innenabstand
  verwenden, abgestimmt mit Punkt 13.
- Die Darstellung später gemeinsam sichten. XL ist eine mögliche Nachjustierung,
  wenn L zu gedrängt wirkt; nicht schon vor der Sichtung pauschal vergrößern.

- [ ] Umsetzung und passende Prüfungen abgeschlossen.
- [ ] Review abgeschlossen und eigener Commit erstellt.

## 13. Gesamtübersicht und kompaktes Overlay: Abstände vereinheitlichen

- Unter „Alle Benachrichtigungen anzeigen“ den großen Leerraum über einzelnen
  Mitteilungstiteln untersuchen, einschließlich Titelbutton und oberer Werkzeugleiste.
- Zwischen Werkzeugleiste und Titel einen passenden Cosmos-Abstand **S oder M**
  anhand der bestehenden Gestaltung wählen; die endgültige Wirkung beurteilt der Benutzer.
- Für das äußere Padding gelten die zuletzt vereinbarten Werte:

  | Mitteilungsdarstellung | Innenabstand |
  | --- | --- |
  | Gesamtübersicht | **L**, 16 px |
  | Geöffnete Nachricht im Dialog | **L**, 16 px, siehe Punkt 12 |
  | Kompaktes Benachrichtigungs-Overlay | **M**, 12 px |

- Den Einfluss der linken Statuslinie auf die gemessenen Abstände prüfen.
  Keine prozentualen oder halbierten Paddingwerte berechnen.
- Die zuvor diskutierten 18–20 px beziehungsweise pauschal XL sind durch diese
  Startwerte ersetzt. Bei zu gedrängter Wirkung kann nach Sichtung auf **XL**, 20 px,
  nachjustiert werden.

- [ ] Umsetzung und passende Prüfungen abgeschlossen.
- [ ] Review abgeschlossen und eigener Commit erstellt.

## 14. Anwesenheitsstatus: Designversuch zuletzt, ohne Commit

**Ausnahme von der Commit-Regel: Änderungen dieses Punkts nicht committen.**

- Erst nach den Punkten 1–13 am sauberen, committeten Stand beginnen.
- Nur den jeweils ausgewählten Anwesenheitsstatus verändern:
  - Überschrift bleibt weiß.
  - Rahmen erhält die jeweilige Statusfarbe.
  - Hintergrund wird dezent in der Statusfarbe getönt.
  - Icon bleibt kräftig und klar erkennbar, Beschreibung weiterhin sekundär.
- Eine kräftige identische Hintergrund- und Iconfarbe vermeiden, damit das Icon
  nicht mit der Fläche verschwimmt. Die bestehenden Statusfarben verwenden.
- Auf die Anwesenheitsauswahl begrenzen; keine anwendungsweite Änderung des
  gemeinsamen Auswahlrahmens ableiten.
- Passende Stilprüfungen und bei Bedarf gezielte JUnit-Prüfungen ausführen,
  Browserbedarf zentral festhalten. Keine Browserrunde starten.
- Versuch und Reviewergebnis zeigen. Benutzer entscheidet anschließend über Behalten
  oder Zurücknehmen; keine eigenmächtige Rücknahme fremder Änderungen.
- Auch nach dem Review **keinen Commit** erstellen. Den Versuch einschließlich
  zugehöriger Änderungen zur Entscheidung im Arbeitsbaum lassen.

- [ ] Sauberer Ausgangscommit nach Punkt 13 festgehalten.
- [ ] Designversuch und passende Prüfungen abgeschlossen.
- [ ] Review abgeschlossen; Versuch uncommittet zur Benutzerentscheidung bereit.
