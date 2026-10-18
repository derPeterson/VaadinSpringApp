# Code- und Katalogbereinigung am 18.10.2026

Ausgangspunkt: sauberer Branch `main`, Commit `85712fee0ba1246784f815a185075db0525ad6f7`. Die übernommenen Avatar-, Eingabe- und Theme-Änderungen sind enthalten. Änderungen bleiben uncommittet; keine Änderungen an Index oder Remote.

## Prüfung und Bereinigungen

Die Suche umfasst die versionierten handgeschriebenen Java-Quellen von Anwendung, Shared und Browserhilfen, Python-/JavaScript-Hilfen sowie die zugehörigen CSS-, HTML-, Konfigurations- und Dokumentationsreferenzen. Abhängigkeiten, generierte Frontend-/Bundle-Dateien, reale Daten, Logs und Geheimdateien sind ausgeschlossen. 459 passende Textdateien wurden für den statischen Namens-/Referenzabgleich berücksichtigt; Bild-/Font-Ressourcen wurden zusätzlich gegen diese Quellen geprüft. Ein fehlender Texttreffer allein wird nicht als Löschbeweis behandelt.

Entfernt:

- `JdbcHelper`: unbenutzte statische Klasse mit `tableExists`; kein Spring-Baustein, kein Reflection-/Konfigurationsverweis und kein Aufrufer.
- `ComponentTextUpdateHelper.updateToolTipComponents`: unbenutzte statische Methode; der verwendete Komponentenübersetzer bleibt erhalten.
- `PresentationBrowserTest.assertNotice`: private unbenutzte Testmethode.
- Drei verwaiste eigene CSS-Klassen mit vier Regeln: `cosmos-admin-info-dialog`, `cosmos-dialog-text` und `cosmos-admin-form-actions` einschließlich ihrer Kindregel. Es gibt weder feste noch dynamisch erzeugte Aufrufer; heutige Konfigurationsinfos und Dialogaktionen verwenden andere Bausteine. Native Grid-Klassen wie `body-row`, `even-row`, `body-cell` und `details-cell` bleiben ausdrücklich erhalten.
- Vier Getter und jeweils vier Sprachschlüssel für die frühere Sitzungs-Demo: `accountSettings.active.now`, `.active.hours`, `.active.yesterday` und `.logout.hint`. Die echte Sitzungsansicht verwendet andere Texte.

Die Sprachkataloge sinken von jeweils 455 auf **451 eindeutige Schlüssel**, ohne Duplikate und bei DE-/EN-Parität. Die Dateien wurden zeilenweise als Bytes geändert; bestehendes Properties-Encoding und Escape-Sequenzen bleiben erhalten. `MessageCatalogueTest` prüft feste Getter, dynamische Gruppen-/Statusschlüssel, Singular/Plural, nicht leere Texte und die Erreichbarkeit jedes Schlüssels in beiden Sprachen. Mailtexte bleiben erhalten.

## Bewusst erhalten

Spring-Beans, Vaadin-Routen, JPA-Callbacks, Repository-Implementierungen und Framework-Overrides benötigen keine direkten Klassen-/Methodenaufrufe. Das gilt ebenso für JUnit-Fixtures und Python-HTTP-Handler (`do_GET`, `log_message`). Admin-Gruppengetter werden innerhalb des typisierten Katalogs über Enumwerte aufgerufen. Native Lumo-Variablen und die vier aus Java dynamisch gewählten Status-Glow-Tokens bleiben erhalten. Für Bild-/Font-Ressourcen wurde kein weiterer sicher verwaister Kandidat gefunden; keine zusätzlichen Dateien wurden archiviert. Die zwei bereits archivierten alten Avatarressourcen wurden nicht erneut verschoben.

Die fünf Mailvorlagen `welcome`, `reset_password`, `password_changed`,
`email_change_request` und `email_changed` sind jeweils für DE/EN aktiv zugeordnet.
Die zugehörigen MJML-Dateien sind editierbare Autorenquellen und bleiben erhalten.
Das gilt auch für Fontlizenz, Vaadins `theme.json`, Spring-Profile und die automatisch
gefundene Logback-Konfiguration. Der ergänzende eigene CSS-Tokenabgleich liefert
nur die vier aus Java dynamisch gewählten Schatten als ohne direkte `var(...)`-
Referenz; daraus folgt keine Löschung.

Die statische Suche ist kein Beweis für die Abwesenheit jeder denkbaren dynamischen Nutzung oder jeder unerreichbaren Verzweigung. Unsichere Fälle werden erhalten; es wurde kein großflächiger Refactor vorgenommen.

## Bestätigte Korrekturen aus der QA

Die neue Browserprüfung deckte eine Ausgabeabweichung auf: Der kompakte deutsche Geburtstag `4586` wurde als Datum richtig gespeichert, aber nativ als `4.5.1986` statt des gewünschten `04.05.1986` dargestellt. Die zentrale DatePicker-Konfiguration formatiert jetzt Deutsch gepolstert; kurze getrennte Eingaben und zweistellige Jahre bleiben zusätzliche Eingabeformate. Englisch bleibt bei `M/d/yyyy`. Die Registrierung und die Profil-/Adminformulare erneuern diese Konfiguration beim Sprachwechsel. Bereichsgrenzen und eingegebene Daten bleiben erhalten; ein zusätzlicher JUnit-Fall prüft den Wechsel samt Referenzdatum und Maximalwert.

Bei 320×568 war zusätzlich die Abmeldung im angemeldeten Benutzermenü nicht erreichbar. Die eigene Maximalhöhe bezog sich auf das gesamte Browserfenster statt auf den von Vaadin neben dem Auslöser verfügbaren Platz. Das Cosmos-Popover verwendet jetzt einen vertikalen Flexrahmen und einen schrumpfbaren, intern scrollbar begrenzten Inhalt; Palette, Inhaltsabstände und Avatar-/Buttonmaße bleiben erhalten. CONFIG-05F prüft horizontalen Überlauf, die vollständig im Fenster liegende mobile Statusfläche und tatsächliches Scrollen/Abmelden. Die native mobile Select-Öffnungsanimation wird vor der Bounds-Prüfung abgewartet; die Viewport-Anforderung wurde nicht abgeschwächt. Der Fix wird an einem neuen Produktionsimage geprüft, frühere Bilder werden nicht als Nachweis dafür verwendet.

Veraltete Browserannahmen wurden gezielt korrigiert: Konto-Schnellzugriff und Menübutton haben denselben zugänglichen Namen, daher adressiert der Kontohelfer ausdrücklich den Menüinhalt. Der Statushelfer grenzt die Popup-Auswahl von der neuen Auswahl am Kontobild ab. Die Infrastrukturprüfung erwartet die bestätigten SVG-Kontursymbole. Die Avatarreise wartet auf den exakten Suchtreffer und prüft sichtbare Gridzellen statt wiederverwendeter versteckter Zellen. Fachliche Assertions wurden nicht gelockert.

## Validierung

Die vollständigen gemessenen Ergebnisse, Nachtests und Grenzen stehen im [QA-Meilenstein](meilensteine/2026-10-18-bereinigung-avatar-eingaben-und-gesamtqa.md) und in der [Browserabdeckung](../browser-tests/abdeckung.md). Anwendungs-JUnit 1524/1524, Browserhelfer 47/47, Python 205/205, JavaScript 26/26 und beide abschließenden Browser-Sprachläufe je 141/141 sind erfolgreich, ohne Fehler oder Skips. Java 25.0.4.1 wurde vorab bestätigt. Der Produktionsbuild erfolgt in Docker, ohne die laufenden IntelliJ-/Vite-Prozesse oder deren Frontend-Abhängigkeiten zu verändern.
