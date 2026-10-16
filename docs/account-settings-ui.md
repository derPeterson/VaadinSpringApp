# Mein Konto: erste Gestaltungsstufe

Stand: 16.10.2026. Feature-Branch `feature/account-settings-ui`.

## Zugang und Umfang

„Konto verwalten“ im bestehenden Avatar-/Benutzermenü öffnet `/account` in derselben
Anwendung, mit dem App-Rahmen von Home. Der Zugang ist für alle angemeldeten Rollen
vorgesehen, einschließlich Gast. Anonyme Aufrufe führen zur Anmeldung.
Der Wartungsschutz bleibt wirksam; Admin/Superadmin behalten wie bisher Zugang.
Die Account-Route steht in den gemeinsamen Routenkonstanten und im `AppRoute`-Enum.

Diese erste Stufe setzt die gewünschte HTML-/Screenshot-Vorschau als Vaadin-
Oberfläche mit bestehenden Cosmos-Bausteinen um. Sie ergänzt keine Kontoänderungs-
oder Sitzungsverwaltungsdienste:

- Kontoübersicht mit bestehendem Avatar, eigenem Namen, E-Mail und Aktivierungsstatus.
- Bedienbare Tabs „Persönliches Profil“, „Sicherheit“, „Geräte & Sitzungen“.
- Profilfelder werden aus dem angemeldeten Konto gelesen; Eingaben sind nur lokale
  Entwürfe. Tab- und Sprachwechsel erhalten diese Entwürfe.
- Die E-Mail ist nur lesbar. Profilbild-, Speicher-, E-Mail-, Passwort- und
  Sitzungsabmeldeaktionen sind für die visuelle Abnahme aktiv dargestellt, besitzen
  aber noch keine Aktionslistener oder Serviceaufrufe. Klicks verändern nichts.
- Sitzungen, Geräte, Zeiten und das dargestellte Limit sind klar bezeichnete
  Beispieldaten. Es erfolgt keine Abfrage des tatsächlichen Sitzungsregisters.
- Vorhandene Passwörter, Hashes und Sitzungstokens werden weder gelesen noch angezeigt.
- DE/EN-Texte liegen im gemeinsamen typisierten `MessageTexts`-Katalog.

Die vorhandene Sitzungsbegrenzung bleibt eigenständig wirksam. Die eigentliche
[Sitzungsverwaltung](umsetzungsplaene/kontokonfiguration/sitzungsverwaltung.md)
und weitere Kontofunktionen werden später gesondert implementiert und geprüft.

## Gestaltung

`AccountView` verwendet denselben Hintergrund, dieselbe Topbar und dasselbe
Benutzermenü wie Home. `CosmosSectionHeaderComponent`, `CosmosCardComponent` und
die vorhandenen Panel-/Badge-/Button-/Inputregeln bleiben unverändert.
`AccountPanelComponent` ordnet die bestehenden Panelbausteine für diese Seite an.

`cosmos-account.css` wird nur an `AccountView` geladen. Die Selektoren sind auf
`.cosmos-account` begrenzt. Die Datei ergänzt nur lokale Anordnung, kleine
Bildschirme und die neue Tab-Leiste; Farben kommen aus den vorhandenen Tokens.
Native [Vaadin-Tabs](https://vaadin.com/docs/latest/components/tabs/styling) behalten
Tastaturbedienung, Fokuszustände und Scrollbuttons. Es werden weder die HTML-Vorlage
noch ihre JavaScript-Demoaktionen in die Anwendung eingebunden.

Der Entwurf ist für die schrittweise visuelle Abstimmung vorgesehen. Gemeinsame
Styles, Icons, Buttonmaße und das übrige Anwendungsdesign wurden nicht umgestellt.

## Übernehmen

Der Shared-Katalog wird vor der Anwendung gebaut. Für den neuen Java-/Routenstand
die Anwendung in der IDE neu starten und anschließend die Browserseite aktualisieren.
Bei Verwendung eines gepackten Produktionsartefakts ist vorher ein neuer Build nötig.
Ein Browser-Refresh allein lädt keine neuen Java-Komponenten.

## Gezielte Prüfung

44 unterschiedliche JUnit-Fälle erfolgreich: 29 Sicherheitsketten-Fälle, vier
bestehende Status-/Menüregressionen, fünf Kontokomponenten-Fälle, drei Konto-View-
Fälle und drei Übersetzungskatalog-Fälle. Keine verbleibenden Fehler oder
übersprungenen Fälle. Geprüft sind unter anderem alle vier Rollen, anonyme
Weiterleitung, Wartungsschutz, Tabwechsel, DE/EN, unveränderte Kontodaten und
regionale Spracheinstellungen. Shared-Modul gebaut, Java 25 und Google Java
Format geprüft. Die bestehende Browser-Testanwendung ist erfolgreich kompiliert.

Die Oberfläche wurde in dieser Gestaltungsstufe noch nicht im laufenden Browser
visuell abgenommen. Es wurden weder die gesamte Anwendungstestsuite noch die
Browser-Gesamtsuite erneut gestartet. Frühere Coverage-Zahlen messen diesen neuen
UI-Stand nicht.
