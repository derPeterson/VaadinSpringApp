# Offener Testbedarf

## Browser-Tests

- **Kartenvergleich mit Ticketaktivität – offen:** Die Ticketsystem-Navigation
  muss mit und ohne ungelesene Aktivität erreichbar sein. Der Status ergänzt
  ihren zugänglichen Namen; die Auswahl darf deshalb keinen leeren Ticketbestand
  voraussetzen. Der Kartenfall erzeugt dafür einen eigenen ungelesenen Vorgang
  und schließt nur diesen anschließend über die UI. In DE/EN auf Desktop und
  Smartphone sind je zwei gezielte Fälle bestanden, einschließlich Aufbau,
  Abbau und Bericht. Offen bleibt die Wiederholung in der großen Testrunde.

- **2FA-Push nach Wechsel des Konfigurationstabs – offen:** Der Gesamtlauf blieb
  beim synchronen Schließen eines administrativen Hilfstabs stehen. Der Test
  überlässt diese kurzlebigen Tabs jetzt dem gemeinsamen BrowserContext-Abbau.
  In DE/EN auf Desktop und Smartphone die echten Inaktivitätsgrenzen,
  serverseitig eingehende Benachrichtigungen und anschließende erneute Bestätigung
  prüfen. Die getrennte Nutzersitzung und alle Sicherheitsassertions erhalten.
  Die drei gezielten Fälle sind inzwischen in DE und EN bestanden, einschließlich
  Abbau und Bericht. Offen bleibt die saubere Wiederholung der vollständigen
  Gesamtrunde am unveränderten Image.

- **Advanced-Hintergrund und Rückwechsel auf Base – offen:** Für die große
  Testrunde der freigegebenen Nachtarbeit isoliert in beiden Varianten prüfen.
  Der gemeldete Fehler auf der Anmeldung ist durch Selektoren für `cosmos-page-frame`
  korrigiert; der Benutzer hat die Sichtbarkeit dort und auf der Startseite bestätigt.
  Den automatisierten Nachtest über öffentliche Rahmen ohne `cosmos-page` und
  die übrigen Ansichten weiterhin abdecken.
  DE/EN auf Desktop und Smartphone in Besucherbereich, Portal und Administration
  prüfen: Horizont, Sterne, Mond, Sternschnuppen und Satellit hinter den Inhalten,
  unveränderte Kopfzeilen/Controls, weiter erreichbare Aktionen und normale
  Scrollbereiche ohne zusätzliche horizontale Scrollleiste. Zeigerparallaxe,
  Navigation und reduzierte Bewegung einschließlich dynamischem Wechsel prüfen.
  Nach Wechsel der zentralen Auswahl und frischem Build Base mit dem bisherigen
  Stand vergleichen; je Auswahl DE/EN am selben unveränderten Image testen.

- **Scrollbereiche nach Regression prüfen – wartend:** Betroffene Ansicht,
  konkretes Fehlerbild und Reproduktionsschritte sind noch zu klären.
  Der Benutzer möchte Aussehen und Verhalten später gemeinsam festlegen;
  keine Scrollgestaltung in der Nachtarbeit ändern. Normale Erreichbarkeit weiter prüfen.
  Nach der Behebung die betroffenen Scrollpanes gezielt prüfen: Inhalte und
  Aktionen bleiben vollständig erreichbar, Scrollen funktioniert wie vorgesehen.
  Passende bestehende Browserfälle wiederverwenden beziehungsweise ergänzen.
  Ein Browserlauf benötigt eine Freigabe für den Behebungsauftrag.
  Fehlerdetails: [Scrollbereiche erneut fehlerhaft](../BUGS.md#scrollbereiche-erneut-fehlerhaft).

- **Gestaltung der manuellen Mailqueue-Bereinigung – offen:** Den aktuellen
  Gesamtlauf im freigegebenen Nachtauftrag nachprüfen. In DE/EN auf Desktop und Smartphone den äußeren Button
  „Manuelle Bereinigung“, Brush-Icons, blaue Statusüberschrift und Variante-A-Dialog
  prüfen. Gelbe Warnung mit fettem Label und oben ausgerichtetem Icon, dezente
  Infobox, Textumbrüche und rechtsbündige gleich breite Aktionen beurteilen.
  Abbrechen, Close, Escape und Außenklick dürfen keine Bereinigung starten;
  Bestätigen führt sie einmal aus. Sprachwechsel bei geöffnetem Dialog prüfen.
  Bestehende `MailQueueResolutionBrowserTest`-Fälle erweitern; 100 ist
  ausschließlich die interne Paketgröße. Die Oberfläche zeigt keine Gesamtgrenze.
  Die Java-Hülle `PortalConfirmationDialog` verwendet nativen `ConfirmDialog`.
  Darstellung und Abbrechen sind im gezielten Dialogvergleich bestanden. Der aktuelle
  Queue-Nachlauf besteht je neun Fälle in DE/EN; alle vier Abbruchwege sind bereits
  in EN bestanden. Die aktuelle deutsche Abbrucherweiterung und der vollständige
  Gesamtstand bleiben für die abschließende Runde offen.

- **Vollständiger Mailqueue-Bereinigungslauf – offen:** Die gezielten Queue-Fälle sind
  jeweils neunmal in DE/EN bestanden, einschließlich 101 Aufträgen, Warten und
  Rechteentzug. Den finalen Gesamtstand in der großen Testrunde nachprüfen. In DE/EN auf Desktop und Smartphone mit mehr als
  100 passenden Testaufträgen starten; die Gesamtzahl muss über 100 liegen,
  beispielsweise 250. Die freigegebene Statusgestaltung mit neutralem, grünem
  beziehungsweise rotem Badge, grüner Abschlusszeile und roter Fehlerzeile prüfen;
  „Noch nicht ausgeführt“ hat keine zusätzliche Erfolg-/Fehlerzeile. Nach manueller
  Ausführung passende Erfolgs-/Fehlernotification prüfen, Erfolg auch bei 0 Treffern.
  Beim bloßen Öffnen des Dialogs noch keine Erfolgsmeldung. Bei laufendem Mailversand
  wird ein bestätigter manueller Auftrag gelb als „Wartet auf Ausführung“ vorgemerkt,
  mit passender Information statt Fehlermeldung. Ohne weiteren Klick nach Ende
  des Versands automatisch ausführen und das echte Ergebnis anzeigen; wiederholte
  Klicks erzeugen keinen zweiten Auftrag. Teilweise abgeschlossene Läufe gelb mit
  Warnnotification statt vollständigem Erfolg kennzeichnen. Keine sichtbare
  100er-Grenze; fachlich richtige Warnungen,
  verständliche Absätze und passende Gesamt-/Teilfehlermeldung prüfen.
  Bestehende Mailqueue-Fälle mit eigenen isolierten Daten erweitern und mit den
  bereits offenen Dialognachtests zusammenführen, sobald der Zielstand umgesetzt ist.

## JUnit-Tests

Kein gesonderter offener JUnit-Testbedarf.

Ergebnisse und Grenzen stehen im [aktuellen Prüfstand](status.md).
Menschliche Sonderprüfungen bleiben im [manuellen Katalog](manual-tests/README.md).
Die bisherigen UI-Pakete und die neue Queue-Karte sind vom Benutzer abgenommen;
zurückgestellte Folgepakete stehen im
[Komponentenplan](planung/vaadin-komponenten-abgleich.md).
