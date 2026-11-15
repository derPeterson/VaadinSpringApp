# Offener Testbedarf

## Browser-Tests

- **Advanced-Hintergrund und Rückwechsel auf Base – wartend:** Der Benutzer
  übernimmt die Sichtung; für diesen Auftrag sind keine Browserläufe freigegeben.
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

- **Mailqueue-Reiter über URL und nach Neuladen – wartend:** Der Benutzer hat
  für diese Korrektur nur JUnit-Prüfungen beauftragt; Browserfreigabe steht aus.
  In DE/EN auf Desktop und Smartphone „Allgemein“ und „Assistenten“ wechseln,
  URL kopieren, direkt öffnen und mit F5 neu laden. Auswahl und Einstellungszähler
  müssen passen; Reiterwechsel ohne Neuladen erhält ungespeicherte Eingaben.
  Als normaler Admin einen Assistenten-Direktlink öffnen: nur „Allgemein“ zugänglich.
  Fehlende, unbekannte oder mehrfache `tab`-Parameter fallen auf „Allgemein“ zurück.
  Bestehende Mailqueue-/Navigationsfälle passend erweitern.

- **Gestaltung der manuellen Mailqueue-Bereinigung – wartend:** Auf ausdrücklichen
  Benutzerwunsch keine Browserprüfung dieses Auftrags; für eine spätere freigegebene
  große Testrunde erhalten. In DE/EN auf Desktop und Smartphone den äußeren Button
  „Manuelle Bereinigung“, Brush-Icons, blaue Statusüberschrift und Variante-A-Dialog
  prüfen. Gelbe Warnung mit fettem Label und oben ausgerichtetem Icon, dezente
  Infobox, Textumbrüche und rechtsbündige gleich breite Aktionen beurteilen.
  Abbrechen, Close, Escape und Außenklick dürfen keine Bereinigung starten;
  Bestätigen führt sie einmal aus. Sprachwechsel bei geöffnetem Dialog prüfen.
  Bestehende `MailQueueResolutionBrowserTest`-Fälle erweitern; 100 ist eine
  Höchstzahl, weitere Aufträge bleiben bis Nachtpaket 3 erhalten. Die Java-Hülle
  `PortalConfirmationDialog` verwendet inzwischen nativen `ConfirmDialog`.
  Darstellung und Abbrechen sind bereits im gezielten Dialogvergleich bestanden;
  die weiteren aufgeführten Fach- und Abbruchfälle bleiben für die Gesamtrunde offen.

- **Vollständiger Mailqueue-Bereinigungslauf – wartend:** Umsetzung und Start von
  Nachtpaket 3 stehen aus. In DE/EN auf Desktop und Smartphone mit mehr als
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

- **Vollständige paketweise Mailqueue-Bereinigung – wartend:** Nachtpaket 3 noch
  nicht gestartet. Bestehenden Test für 101 Aufträge auf den neuen Gesamtlauf
  umstellen und größere Mengen sowie leere/exakte Paketgrenzen abdecken.
  Summierte Anzahl und einen Historieneintrag je logischem Lauf prüfen.
  Rollback nur des fehlerhaften Pakets, erhaltene frühere Abschlüsse, ehrliches
  Teilergebnis und erneuten Versuch absichern. Status und manuelle Notifications
  müssen Vormerkung, Abschluss, Fehler und Teilfortschritt korrekt unterscheiden;
  ein fehlerfreier Lauf ohne Treffer ist erfolgreich. Laufenden Versand, konkurrierende
  Bereinigungen, Rechteentzug, feste Altersgrenze und ausbleibenden Fortschritt
  berücksichtigen; kein Neuversand und bestehende Löschbestätigungsregeln erhalten.
  Vorgemerkten manuellen Auftrag nach Freigabe der Versandkoordination ohne erneuten
  Klick genau einmal übernehmen; Doppelklick, konkurrierende Schedulerübernahme,
  Neustart und Entzug der nötigen Berechtigung absichern. Automatische Prüfungen
  dürfen den vorgemerkten Auftrag weder verlieren noch als „Übersprungen“ abschließen.

Ergebnisse und Grenzen stehen im [aktuellen Prüfstand](status.md).
Menschliche Sonderprüfungen bleiben im [manuellen Katalog](manual-tests/README.md).
Die bisherigen UI-Pakete und die neue Queue-Karte sind vom Benutzer abgenommen;
zurückgestellte Folgepakete stehen im
[Komponentenplan](planung/vaadin-komponenten-abgleich.md).
