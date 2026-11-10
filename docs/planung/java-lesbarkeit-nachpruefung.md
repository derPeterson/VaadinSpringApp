# Klassenweise Nachprüfung der Java-Lesbarkeit

Auf `codex/java-lesbarkeit-nachpruefung` ab `6f3edddb` die 522
versionierten Java-Dateien in App, Shared und Browser-Tests erneut einzeln prüfen.
Maßstab ist der [Codestil](../entwicklung.md#codestil).

## Offene Pakete

- App-Testklassen, Browser-Tests, Page Objects und Runner

Die 16 Klassen direkt in `app.views`, die elf Klassen in `app.views.admin`,
die 63 Klassen in `app.ui` und `app.helper.ui`, die zwei Klassen in `app.events`,
die 191 übrigen App-Produktionsdateien sowie der gemeinsame Textkatalog
`MessageTexts` und die vier Konfigurationsklassen im Modul Shared sind gesichtet.

Bei jeder Klasse die Namen, Zuständigkeiten, Aufrufer und relevanten
Seiteneffekte im Zusammenhang prüfen. Fachverhalten, UI, Transaktionsgrenzen
und Fehlerbehandlung erhalten. Je Paket den Diff und die Stilchecks prüfen
und bei umgebauten Abläufen passende vorhandene Tests ausführen.
