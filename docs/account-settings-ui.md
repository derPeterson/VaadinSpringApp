# Mein Konto: Oberfläche und Profildaten speichern

Stand: 16.10.2026. Feature-Branch `feature/account-settings-ui`.

## Zugang und Umfang

„Konto verwalten“ im bestehenden Avatar-/Benutzermenü öffnet `/account` in derselben
Anwendung, mit dem App-Rahmen von Home. Der Zugang ist für alle angemeldeten Rollen
vorgesehen, einschließlich Gast. Anonyme Aufrufe führen zur Anmeldung.
Der Wartungsschutz bleibt wirksam; Admin/Superadmin behalten wie bisher Zugang.
Die Account-Route steht in den gemeinsamen Routenkonstanten und im `AppRoute`-Enum.

Die HTML-/Screenshot-Vorschau wurde mit bestehenden Cosmos-Bausteinen umgesetzt.
Der nächste Schritt ergänzt das Speichern der eigenen Profildaten:

- Kontoübersicht mit bestehendem Avatar, eigenem Namen, E-Mail und Aktivierungsstatus.
- Bedienbare Tabs „Persönliches Profil“, „Sicherheit“, „Geräte & Sitzungen“.
- Profilfelder werden aus dem angemeldeten Konto gelesen. Tab- und Sprachwechsel
  erhalten ungespeicherte Entwürfe. „Änderungen speichern“ übernimmt Vorname,
  Nachname, Geburtsdatum und bevorzugte Sprache (DE/EN).
- Die E-Mail bleibt sichtbar, ist aber deaktiviert und nicht fokussierbar.
  Beide E-Mail-Ändern-Buttons öffnen den unten beschriebenen Dialogentwurf.
  Profilbild-, Passwort- und Sitzungsabmeldeaktionen sind für die visuelle Abnahme
  aktiv dargestellt, besitzen aber noch keine Aktionslistener oder Serviceaufrufe.
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

## Speichervertrag

`AccountProfileService` bestimmt das Zielkonto bei jedem Speichern erneut über die
bestehende Anmeldung. Das Formular übergibt keine Konto-ID oder E-Mail. Eine eigene
Transaktion lädt und sperrt den aktuellen Datensatz; die beim Öffnen gelesene Version
muss noch passen. Bei Änderungen aus einem anderen Formular erscheint die vorhandene
Konfliktmeldung. Dann die Seite neu laden und die Änderungen prüfen.

Nur Vorname, Nachname, Geburtsdatum und Sprache werden zugewiesen. E-Mail, Passwort,
Geschlecht, Freigabe, Verifikationszustand, Rollen, Status und Tokens werden nicht aus
einer alten UI-Entity zurückgeschrieben. Alle vier Rollen dürfen ihr eigenes Profil
speichern; Anmeldung, Kontofreigabe und Wartungsschutz werden erneut geprüft.

Pflichtfelder und Geburtsdatum verwenden dieselben zentralen Regeln und UI-Helfer wie
die Registrierung: Namen dürfen nicht leer sein, das Geburtsdatum muss vor heute liegen.
Die Sprache muss DE oder EN sein. Die Prüfung erfolgt beim Speichern; Fehler zeigen
rote Feldmarkierungen und die vorhandene Fehlernotification. Bei einem Speicherfehler
bleiben die Eingaben erhalten. Erfolg wird ebenfalls per Notification bestätigt.
Nach bestätigtem Commit aktualisieren sich Name in Übersicht/Benutzermenü und die
Sitzungssprache. Die neue Datensatzversion wird für weiteres Speichern übernommen.

## Gezielte Prüfung

Für die Profilspeicherung 74 unterschiedliche JUnit-Fälle erfolgreich, keine verbleibenden
Fehler oder übersprungenen Fälle: 19 isolierte H2-/Transaktionsfälle für Profilspeicherung,
14 Kontokomponenten-Fälle, fünf Konto-View-Fälle, 29 Sicherheitsketten-Fälle, vier
bestehende Status-/Menüregressionen und drei Übersetzungskatalog-Fälle. Geprüft sind
unter anderem alle Rollen, erhaltene fremde Konten und Sicherheitsfelder/Tokens,
Speicherwiederholung, veraltete Formulare, anonyme/deaktivierte/gelöschte Konten,
Wartungsschutz, serverseitig ungültige Eingaben sowie rote Fehlermeldungen und
Erfolgsmeldungen in DE/EN. Java 25, Shared-Build und Google Java Format geprüft.

Die neue Speicherfunktion wurde noch nicht im laufenden Browser geprüft. Die gesamte
Anwendungstestsuite und Browser-Gesamtsuite wurden nicht erneut gestartet. Frühere
Coverage-Zahlen messen diesen neuen Stand nicht. Als manuelle Abnahme noch offen:
Speichern, anschließendes Neuladen und erneute Anmeldung mit gespeicherter Sprache
sowie die Darstellung des deaktivierten E-Mail-Feldes.

## E-Mail ändern: Dialogentwurf

Über „Ändern“ beim Profilfeld oder „E-Mail ändern“ im Sicherheitstab öffnet sich
dieselbe `AccountEmailChangeDialog`-Komponente. Sie verwendet die vorhandenen
Cosmos-Header-, Icon-, Feld-, Hinweis-, Schließen- und Buttonbausteine. Die
Dialogfläche folgt der Card, der Header dem Benutzerdialog und der Footer dem
Konto-Footer. Der Hintergrund-Blur von 6px auf dem nativen Backdrop ist inzwischen
zentraler Standard für alle Cosmos-Dialoge; siehe [Designstandard](design-standard.md).

Der Entwurf enthält neue E-Mail-Adresse, deren Bestätigung und aktuelles Passwort
mit Sichtbarkeitsschalter. Ein Hinweis mit Info-Icon erläutert den späteren
Bestätigungsschritt. DE/EN und Sprachwechsel erhalten die offenen Eingaben.
Schließen per Icon, Abbrechen, Escape oder Außenklick verwirft die Dialogeingaben,
einschließlich des Passworts. Ungespeicherte Profildaten bleiben erhalten.

„Bestätigung anfordern“ ist für die Gestaltungsabnahme aktiv, führt aber noch
keine Validierung, Passwortprüfung, Mailzustellung oder Kontoänderung aus.
Die eigentliche E-Mail-Änderung ist weiterhin offen.

29 gezielte JUnit-Fälle für diesen Entwurf erfolgreich: sechs Dialogfälle,
15 Kontokomponenten-Fälle, fünf Konto-View-Fälle und drei Katalogfälle; keine
Fehler oder übersprungenen Fälle. Java 25, Shared-Build und Google Java Format
geprüft. Browserdarstellung, Blur und echte Außenklick-/Escape-Bedienung sind noch
visuell abzunehmen; im JUnit-Test wurden die native Schließkonfiguration und das
Verwerfen beim Schließen geprüft. Keine neue Browser-Gesamttestrunde gestartet.
