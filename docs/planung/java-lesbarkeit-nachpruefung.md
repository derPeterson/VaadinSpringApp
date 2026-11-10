# Klassenweise Nachprüfung der Java-Lesbarkeit

Auf `codex/java-lesbarkeit-nachpruefung` ab `6f3edddb` die 522
versionierten Java-Dateien in App, Shared und Browser-Tests erneut einzeln prüfen.
Maßstab ist der [Codestil](../entwicklung.md#codestil).

## Offene Pakete

- Übrige Klassen in `app.views.admin`: `AdminConfigurationSection` und
  `AdminUserManagementSection`
- Gemeinsame UI-Komponenten, Layouts und UI-Hilfsklassen
- App-Fachlogik, Sicherheit, Infrastruktur und Persistenz
- Gemeinsamer Textkatalog `MessageTexts` in Shared
- App-Testklassen, Browser-Tests, Page Objects und Runner

Die 16 Klassen direkt in `app.views`, die zwei Klassen in `app.events` und
die vier Konfigurationsklassen im Modul Shared sind gesichtet. In
`app.views.admin` sind `AdminDashboardSection`, `AdminExternalLoginSection`,
`AdminMailTestSection`, `AdminTwoFactorSection`, `AdminUserActivityFilter`,
`AdminUserActivityText`, `AdminUserFilterBar`, `AdminUserFilters` und
`AdminUserNotificationsSection` gesichtet.

Bei jeder Klasse die Namen, Zuständigkeiten, Aufrufer und relevanten
Seiteneffekte im Zusammenhang prüfen. Fachverhalten, UI, Transaktionsgrenzen
und Fehlerbehandlung erhalten. Je Paket den Diff und die Stilchecks prüfen
und bei umgebauten Abläufen passende vorhandene Tests ausführen.
