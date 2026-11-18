# Offene Nachbearbeitungen

**Stand: Punkt 9 ist nicht vom Benutzer abgenommen; Nachbesserungen stehen aus.**

Die neue Nachbearbeitung ist zur Bearbeitung freigegeben. Punkte 1 und 2 liefern
zunächst Bestandsaufnahme und Vorschläge; Gestaltungsänderungen daraus benötigen
die gemeinsame Abstimmung. Die Korrekturen aus Punkten 3–5 sind beauftragt.
Je Punkt Review und aussagekräftigen Commit durchführen. Keine Browserläufe;
notwendigen Browsernachtest im zentralen Testbedarf erhalten.

Die übrigen bisherigen Nachbearbeitungen sind abgeschlossen und abgenommen.
Dieser Plan enthält den offenen Restpunkt und neu aufgenommene Aufgaben des
[Vaadin-Komponenten-Abgleichs](vaadin-komponenten-abgleich.md).
Die Gesamtumsetzung bleibt offen; aktueller Branch ist
`codex/vaadin-komponenten-folgearbeiten-5`.

## Neue Nachbearbeitung – Punkt 1: Badge-Größen analysieren und vereinheitlichen

**Bestandsaufnahme und Vorschlag freigegeben; Gestaltung anschließend abstimmen.**

- [ ] Projektweit die Badge-Einsatzstellen und ihre aktuellen Höhen erfassen und
      eine verständliche Übersicht erstellen.
- [ ] Einen Vorschlag für zentrale Größen-Tokens mit benannten Abstufungen
      (beispielsweise XS, S, M und L) vorlegen, nahe an den bisherigen Größen.
      Nur tatsächlich sinnvolle Größen vorsehen; weitere Abstufungen bei Bedarf begründen.
- [ ] Als projektweite Vorgabe berücksichtigen: Badges innerhalb eines fachlich
      zusammengehörigen Bereichs erhalten eine einheitliche Größe, insbesondere
      dieselbe Höhe. Unterschiedliche Beschriftungen oder Zustandsfarben allein
      begründen keine Größenunterschiede. Die vier Badges im Benachrichtigungs-Overlay
      sind ein Beispiel für diese allgemeine Regel.
- [ ] Bewusste Hervorhebungen dürfen eine größere Größenstufe verwenden, etwa die
      Limitangabe bei „Geräte und Sitzungen“. Größen nach fachlicher Funktion und
      Gewichtung zuordnen; gleichartige Badges im jeweiligen Themenbereich gleich gestalten.
- [ ] Vorschlag gemeinsam abstimmen; die Umsetzung erst nach Freigabe beginnen.

## Neue Nachbearbeitung – Punkt 2: Eingaben, Buttons und Formularschrift erfassen

**Bestandsaufnahme und Vorschlag freigegeben; Änderungen anschließend abstimmen.**

- [ ] Alle verwendeten Eingabekomponenten erfassen, nicht nur Textfelder:
      unter anderem TextAreas, Passwort-, Zahlen-, Datums- und Dauerfelder,
      ComboBox/Select, Mehrfachauswahl, Checkboxen, Switches und weitere vorhandene Typen.
- [ ] Je Variante die aktuelle Höhe, Einsatzstellen und maßgeblichen Größenregeln
      auflisten. Bei mehrzeiligen oder zusammengesetzten Komponenten die eigentliche
      Eingabefläche von Beschriftung, Hilfetext und Gesamthöhe unterscheiden.
- [ ] Alle gerahmten Buttons einschließlich reiner Icon-Buttons ebenso erfassen:
      Höhe, Einsatzstellen und Größenregeln gegenüberstellen.
- [ ] Prüfen, ob gleichartige Komponenten innerhalb zusammengehöriger Formularbereiche
      einheitlich dimensioniert sind. Bewusste funktionale Größenunterschiede von
      unbeabsichtigten Abweichungen unterscheiden.
- [ ] Die Schriftgestaltung in Formularen und vergleichbaren Bedienbereichen mit
      den vorhandenen Typografie-Tokens abgleichen und Abweichungen ausweisen,
      insbesondere bei Schriftgröße, Schriftgewicht und Zeilenhöhe.
      Individuelle Gestaltung auf der Startseite gehört nicht zu dieser Prüfung.
- [ ] Eine verständliche Übersicht und einen Vorschlag zur Vereinheitlichung anhand
      vorhandener Tokens vorlegen; nötige Ergänzungen gesondert begründen.
      Änderungen erst nach gemeinsamer Abstimmung und Freigabe umsetzen.

## Neue Nachbearbeitung – Punkt 3: Positionssprung zwischen Zahlenwert und Placeholder

**Analyse einschließlich Screenshot-Messung und Korrektur freigegeben.**

- [ ] Den gemeldeten Rechtsversatz beim Löschen eines Zahlenwertes untersuchen:
      Sowohl im Cosmos-Dauerfeld mit Einheitenauswahl als auch im normalen Zahlenfeld
      springt die Darstellung beim Wechsel zum Placeholder nach rechts.
- [ ] Bei der späteren Bearbeitung die beiden bereitgestellten Screenshots ausmessen
      und die Positionen von Wert und Placeholder vergleichen. Die Aufnahmen haben
      unterschiedliche Bildbreiten; die Lage im jeweiligen Feld berücksichtigen.
- [ ] Ursache anhand der verwendeten Vaadin-Komponenten und unserer CSS-Regeln klären;
      insbesondere den Einfluss des beim Leeren verschwindenden Löschen-Buttons prüfen.
      Schriftgröße oder eine Vaadin-Eigenheit nicht ungeprüft als Ursache annehmen.
- [ ] Den Positionssprung nach geklärter Ursache für beide Feldtypen beheben,
      ohne Wert-, Placeholder- oder Bedienverhalten unbeabsichtigt zu ändern.

Screenshot-Referenzen für die spätere Bearbeitung (lokale temporäre Dateien):

- Gefüllt: `C:/Users/chris/AppData/Local/Temp/codex-clipboard-aea08c88-53e0-4a46-b859-1c5351f870b1.png`
- Placeholder: `C:/Users/chris/AppData/Local/Temp/codex-clipboard-6a8e74e0-d580-443e-80cf-28af9f8c5fc7.png`

## Neue Nachbearbeitung – Punkt 4: Fokusrahmen bei TextAreas korrigieren

**Ursachenanalyse und Korrektur freigegeben.**

- [ ] Den fehlerhaften Fokusrahmen der TextArea untersuchen und mit dem korrekt
      dargestellten Fokusrahmen des TextField vergleichen.
- [ ] Die Ursache klären und die TextArea-Fokusdarstellung an den bestehenden
      TextField-Standard angleichen. Relevante TextArea-Einsatzstellen berücksichtigen.
- [ ] Den TextField-Standard als Referenz erhalten; keine neue Fokusgestaltung einführen.

Screenshot-Referenzen für die spätere Bearbeitung (lokale temporäre Dateien):

- TextArea mit Fehler: `C:/Users/chris/AppData/Local/Temp/codex-clipboard-84763deb-e0ed-413b-8989-93da7d25cf51.png`
- TextField als Soll: `C:/Users/chris/AppData/Local/Temp/codex-clipboard-8834ddfe-7630-41a2-865d-b4a49cda3fdb.png`

## Neue Nachbearbeitung – Punkt 5: Empfänger-Overlay kompakter im gemeinsamen Stil gestalten

**Umsetzung freigegeben; konkretisiert den bisherigen offenen Punkt 9. Abnahme bleibt offen.**

- [ ] Das an der Avatargruppe verankerte Overlay beibehalten. Die zuletzt gewünschte
      Variante ist kein mittiger Dialog mit abgedunkeltem Seitenhintergrund.
- [ ] Den vorhandenen Dialogstil für die Gestaltung aufgreifen: Header mit dem
      bestehenden Accounts-/Personengruppen-Icon (zwei Personen), Titel „Empfänger“
      und gemeinsamem Schließen-Button; darunter eine Trennlinie.
- [ ] Den Content-Bereich mit der dafür bestehenden, vom Header abgesetzten
      Hintergrundfläche gestalten. Vorhandene Cosmos-Farben und Abstände verwenden.
- [ ] Die Suche nach Name und E-Mail mit sichtbarer Lupe wie in den vorhandenen
      Filtern gestalten. Reihenfolge: Header, Suche, Empfängerliste.
- [ ] Die Größe am Inhalt ausrichten und eine Maximalbreite statt unnötig großer
      fester Breite verwenden; auf kleinen Bildschirmen den verfügbaren Platz einhalten.
- [ ] Höchstens fünf Empfängerzeilen gleichzeitig anzeigen, weitere Einträge innerhalb
      der Liste scrollbar machen. Bei beispielsweise zwei Empfängern soll die Höhe
      entsprechend schrumpfen; keinen leeren Listenbereich reservieren.
- [ ] Die native Liste mit Avatar, Name und sekundärer E-Mail beibehalten und ihre
      Zeilen an den bestehenden Tabellenfarben der Benutzerverwaltung ausrichten,
      einschließlich abwechselnder Zeilenflächen. Details innerhalb dieses Rahmens
      bei der späteren Bearbeitung sinnvoll abstimmen.
- [ ] Nach Umsetzung gemeinsam sichten; die AvatarGroup bleibt bis zur ausdrücklichen
      Abnahme offen.

Screenshot-Referenz für die spätere Bearbeitung (lokale temporäre Datei):

`C:/Users/chris/AppData/Local/Temp/codex-clipboard-95e51db4-922e-4250-88ff-6a81ff700104.png`

## Restpunkt aus der bisherigen Nachbearbeitung

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
- Die Liste bei mehr als fünf Einträgen scrollen lassen und bei weniger Einträgen
  schrumpfen lassen. Der neue Punkt 5 konkretisiert die Gestaltung und ersetzt
  den früheren groben Höhenrichtwert von 600–700 px.

### Bestehende technische Grundlage

Die native AvatarGroup dient als Vorschau. Für die vollständige Liste sind
Popover, Suchfeld und Grid sowie der kleine gekapselte Ereignis-Listener
bereits ausdrücklich genehmigt und umgesetzt; private Vaadin-APIs werden
nicht verwendet. Die bisherige Details-Liste und das native Überlaufmenü sind
durch diesen gemeinsamen Einstieg ersetzt.

### Noch offen

- [ ] Die unter neuem Punkt 5 festgehaltenen Nachbesserungen nach Startfreigabe bearbeiten.
- [ ] Vereinbarte Änderungen umsetzen, passende JUnit-Regressionen prüfen und reviewen.
- [ ] Ausdrückliche Benutzerfreigabe der Avatar-/Empfängergruppe erhalten.
- [ ] Erst nach dieser Freigabe das offene AvatarGroup-Paket im Komponentenplan schließen
      und diesen dann erledigten Restplan entfernen.

Die bestehende Umsetzung ist ein Zwischenstand, keine fachliche Abnahme.
Berechtigungen, Empfängerauswahl und Versandregeln bleiben erhalten.
Texte und dynamische Meldungen auf Deutsch und Englisch pflegen.
Keine Browserläufe starten; offene automatisierte Prüfungen bleiben zentral im
[Testbedarf](../testbedarf.md). Die menschliche Abnahme steht im
[manuellen Katalog](../manual-tests/README.md#hum-01).
