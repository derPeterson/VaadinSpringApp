# Offene Aufgaben

Stand: 20.10.2026. Diese Datei bündelt die vereinbarten nächsten Arbeitspakete.
Ausführliche Prüfschritte und Ergebnisse bleiben in den verlinkten Fachdokumenten.

## Nächste Arbeitspakete

- [ ] Designkritik mit Stitch vorbereiten: repräsentative Ansichten auswählen,
  einen konkreten Prüfauftrag erstellen und Verbesserungsvorschläge gemeinsam
  priorisieren. Grundlage bleibt der abgestimmte [Designstandard](docs/design-standard.md).
- [ ] Anwendung auf HTTPS umstellen: Zielumgebung und Zertifikatsverwaltung festlegen;
  Weiterleitungen, externe Anwendungslinks und Sitzungsverhalten über HTTPS prüfen.

## Ergänzende Abnahme- und Betriebsprüfungen

- [ ] Reale Mobilgeräte und Bedienbarkeit prüfen: Bildschirmtastatur, Tastaturfokus,
  Zoom und Screenreader sowie Verständlichkeit der deutschen und englischen Texte.
- [ ] E-Mail-Betrieb abnehmen: Darstellung in den vorgesehenen Mailprogrammen,
  tatsächliche Zustellung über den vorgesehenen SMTP-Server mit Auth/TLS sowie
  Ausfall, Wiederanlauf und Recovery bei unklarem Versandausgang.
- [ ] Bestandsmigrationen an vorbereiteten Testkopien prüfen; Mehrinstanzbetrieb
  bei entsprechender Zielumgebung gesondert nachweisen.

Die detaillierten Restprüfungen einschließlich ergänzender Browser-, Neustart- und
Offline-/Online-Folgen stehen im [Prüfkatalog](docs/manual-tests/README.md).
Die Ausführung richtet sich nach den vorgesehenen Geräten, Mailzielen und Betriebsumgebungen.

## Pflege und Nachweise

Neue vereinbarte Aufgaben hier ergänzen und erledigte Punkte entfernen.
Abgeschlossene Arbeiten werden bei Bedarf unter [Meilensteine](docs/meilensteine/README.md)
festgehalten. Aktuelle automatisierte Testnachweise und Messgrenzen stehen in der
[Browserabdeckung](browser-tests/abdeckung.md).
