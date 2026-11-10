# Klassenweise Nachprüfung der Java-Lesbarkeit

Auf `codex/java-lesbarkeit-nachpruefung` ab `9ffccc95` die 522
versionierten Java-Dateien in App, Shared und Browser-Tests einzeln prüfen.
Maßstab ist der [Codestil](../entwicklung.md#codestil).

## Offene Pakete

- Übrige Klassen direkt in `app.views`: `AdminView`, `LoginView`,
  `RegistrationView`, `VerificationView` und `UserNotificationsView`.
  In `LoginView` wurde bisher nur ein Aufruf an den geänderten Methodennamen angepasst.
- Admin-Bereiche, gemeinsame UI-Komponenten, Layouts und UI-Hilfsklassen
- App-Fachlogik, Sicherheit, Infrastruktur und Persistenz
- Gemeinsame Java-Quellen in Shared
- App-Testklassen, Browser-Tests, Page Objects und Runner

In `app.views` sind die Klassen `AccountView`, `ConfirmEmailChangeView`,
`ExternalProofNotice`, `ExternalRegistrationView`, `ForgotPasswordView`,
`HomeView`, `MainView`, `MaintenanceView`, `RecoveryHelpView`,
`ResetPasswordView` und `TwoFactorView` gesichtet.

Bei jeder Klasse die Namen, Zuständigkeiten, Aufrufer und relevanten
Seiteneffekte im Zusammenhang prüfen. Fachverhalten, UI, Transaktionsgrenzen
und Fehlerbehandlung erhalten. Je Paket den Diff und die Stilchecks prüfen
und bei umgebauten Abläufen passende vorhandene Tests ausführen.
