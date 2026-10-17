# Mein Konto: Oberfläche und Profildaten speichern

Stand: 17.10.2026. Feature-Branch `feature/account-settings-ui`.

## Zugang und Umfang

„Konto verwalten“ im bestehenden Avatar-/Benutzermenü öffnet `/account` in derselben
Anwendung, mit dem App-Rahmen von Home. Der Zugang ist für alle angemeldeten Rollen
vorgesehen, einschließlich Gast. Anonyme Aufrufe führen zur Anmeldung.
Der Wartungsschutz bleibt wirksam; Admin/Superadmin behalten wie bisher Zugang.
Die Account-Route steht in den gemeinsamen Routenkonstanten und im `AppRoute`-Enum.

Die HTML-/Screenshot-Vorschau wurde mit bestehenden Cosmos-Bausteinen umgesetzt.
Das Speichern der eigenen Profildaten ist inzwischen implementiert:

- Kontoübersicht mit bestehendem Avatar, eigenem Namen, E-Mail und Aktivierungsstatus.
- Bedienbare Tabs „Persönliches Profil“, „Sicherheit“, „Geräte & Sitzungen“.
  Die Auswahl steht als `tab=profile`, `tab=security` oder `tab=sessions` in der URL.
- Profilfelder werden aus dem angemeldeten Konto gelesen. Tab- und Sprachwechsel
  erhalten ungespeicherte Entwürfe. „Änderungen speichern“ übernimmt Vorname,
  Nachname, Geburtsdatum und bevorzugte Sprache (DE/EN).
- Die E-Mail bleibt sichtbar, ist aber deaktiviert und nicht fokussierbar.
  Der E-Mail-Ändern-Button unter Sicherheit öffnet den unten beschriebenen Dialogentwurf.
  Im Profil gibt es keinen Ändern-Button und keinen zusätzlichen E-Mail-Hilfetext.
  Auch „Passwort ändern“ öffnet unter Sicherheit einen Dialogentwurf.
  Beide Dialoge verändern noch keine Kontodaten. Profilbild- und Sitzungsabmeldeaktionen
  sind für die visuelle Abnahme aktiv dargestellt, besitzen aber noch keine Aktionslistener
  oder Serviceaufrufe. „Aktuelle Sitzung“ ist als reine Kennzeichnung deaktiviert.
- Die drei Geräte und Aktivitätszeiten sind weiterhin statische Entwurfsdaten.
  Der zusätzliche Beispielhinweis und die Zusätze „(Beispiel)“ wurden für die Gestaltung entfernt. Die Überschrift lautet „Aktive Sitzungen“, darunter „Geräte und Aktivitätszeiten“.
  Das Limit rechts enthält dagegen den tatsächlichen Wert von `max.sessions.per.user`
  beim Öffnen der Kontoseite, ohne Beispielzusatz. Es erfolgt noch keine Abfrage des
  tatsächlichen Sitzungsregisters und keine Abmeldung von Geräten.
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
Profil, Sicherheit und Geräte/Sitzungen verwenden dieselbe Footerfläche einschließlich
Farbe, oberem Border, Innenabständen und mobilem Verhalten.
Die anschließende Sitzungsanpassung wurde mit 27 gezielten JUnit-Fällen geprüft,
einschließlich DE/EN und vom Standardwert abweichenden Konfigurationslimits 7 und 11.
Es wurde dafür kein Browserlauf gestartet.

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

## Automatisierte Prüfung

Für die anschließende Profil-/Sicherheitsbereinigung bestanden 30 gezielte
JUnit-Fälle (Kontokomponente, E-Mail-Dialog, Konto-View und Übersetzungskatalog)
sowie 45 Browser-Helpertests. Die Browserquellen sind auf den einzigen E-Mail-Einstieg
unter Sicherheit angepasst und kompiliert; die Browserabläufe und visuelle Abnahme
dieser Bereinigung wurden noch nicht erneut ausgeführt.

Die vorherige vollständige Kontoprofil-Gesamtprüfung umfasste 1.298 JUnit-Fälle ohne Fehler
oder Skips, einschließlich isolierter H2-/Transaktionsprüfungen für Profiländerungen,
aller Rollen, Versionskonflikte, Sicherheitsfelder und Wartungsschutz. DE/EN-Meldungen,
rote Feldmarkierungen, wiederholtes Speichern und Übersetzungskatalog sind enthalten.

Die neue Browser-Suite `account-settings` enthält sieben Fälle: dauerhaftes Speichern
und erneutes Speichern nach Neuladen, Pflichtfelder und Datumsregel ohne Persistierung
ungültiger Werte, gespeicherte Sprache nach erneuter Anmeldung sowie die vier
Schließwege des E-Mail-Dialogs. Der E-Mail-Einstieg unter Sicherheit, unveränderte Profildrafts,
deaktivierte E-Mail, übersetzte Felder, Bestätigungsicon und berechneter 6px-Blur
werden über die echte Oberfläche geprüft. Ein berechneter Style ersetzt keine
menschliche Abnahme der Darstellung auf echten Geräten.

Aktueller Abschlussstatus und tatsächliche Messergebnisse stehen in der
[Abdeckungsübersicht](../browser-tests/abdeckung.md). Der eigentliche E-Mail-Wechsel
ist ausdrücklich kein Bestandteil des Nachweises.

## Konto-Tab in der URL

Die Konto-View verwendet wie AdminView die native Flow-Navigation mit Queryparametern:
`/account?tab=profile`, `/account?tab=security` und `/account?tab=sessions`.
`beforeEnter` stellt nach der vorhandenen Zugriffsprüfung den angeforderten Tab wieder
her. Ohne `tab` sowie bei unbekannten, leeren oder mehrfachen Werten wird das Profil
geöffnet. Die festen Kennungen sind unabhängig von der gewählten Sprache.

Tabwechsel schreiben die URL über Flow und erhalten die Formularentwürfe derselben View.
Die Wiederherstellung aus der URL erzeugt selbst keinen weiteren Navigationseintrag;
so können Refresh, Direktlinks und Zurück/Vorwärts denselben Tab auswählen. Ein tatsächlicher
Reload lädt gespeicherte Profildaten neu; ungespeicherte Formulare werden nicht in der URL
oder im Browserspeicher abgelegt. Dialoge werden nicht über die Tab-URL geöffnet.
35 gezielte JUnit-Fälle für Konto-View und Kontokomponente bestanden ohne Fehler oder Skips,
einschließlich Tab-URLs, Rückfallwerten, Zugriffsschutz, Sprachwechsel und Erhalt der Entwürfe.
Die neuen Abläufe sind noch nicht mit einem echten Browser geprüft.

## E-Mail ändern: Dialogentwurf

Über „E-Mail ändern“ im Sicherheitstab öffnet sich die
`AccountEmailChangeDialog`-Komponente. Im Profil wird die E-Mail nur angezeigt. Sie verwendet die vorhandenen
Cosmos-Header-, Icon-, Feld-, Hinweis-, Schließen- und Buttonbausteine. Die
Dialogfläche folgt der Card, der Header dem Benutzerdialog und der Footer dem
Konto-Footer. Der Hintergrund-Blur von 6px auf dem nativen Backdrop ist inzwischen
zentraler Standard für alle Cosmos-Dialoge; siehe [Designstandard](design-standard.md).

Der Entwurf enthält neue E-Mail-Adresse, deren Bestätigung und aktuelles Passwort
mit Sichtbarkeitsschalter. Ein Hinweis mit Info-Icon erläutert den späteren
Bestätigungsschritt und den Erhalt der alten Adresse bis zur Bestätigung. Dieser Hinweis
steht ausschließlich im Dialog. DE/EN und Sprachwechsel erhalten die offenen Eingaben.
Schließen per Icon, Abbrechen, Escape oder Außenklick verwirft die Dialogeingaben,
einschließlich des Passworts. Ungespeicherte Profildaten bleiben erhalten.

„Bestätigung anfordern“ ist für die Gestaltungsabnahme aktiv, führt aber noch
keine Validierung, Passwortprüfung, Mailzustellung oder Kontoänderung aus.
Die eigentliche E-Mail-Änderung ist weiterhin offen.

Die sieben JUnit-Dialogfälle prüfen unter anderem Konfiguration, Sprache und
Verwerfen beim Schließen/Wiederöffnen. Im echten Browser wurde ein verbleibender
Passwortentwurf nach Escape gefunden: Eingaben werden jetzt unmittelbar synchronisiert
und vor jedem Öffnen zusätzlich verworfen. Die Browserfälle prüfen Icon, Abbrechen,
Escape und Außenklick jeweils mit anschließender leerer Wiederöffnung. Die vollständige
Anwendungstestsuite enthält diese Regression; bisherige Teiltestzahlen werden nicht
zum Gesamtergebnis addiert.


## Passwort ändern: Dialogentwurf

„Passwort ändern“ unter Sicherheit öffnet `AccountPasswordChangeDialog` mit Schloss-Icon,
passenden DE/EN-Texten und drei Passwortfeldern: neues Passwort, „Neues Passwort bestätigen“ und
aktuelles Passwort. Wie im E-Mail-Dialog steht das aktuelle Passwort zuletzt. Die bestehenden Platzhalter, Sichtbarkeitsschalter, Schließen-/Abbrechen-
und Bestätigungsicons bleiben der Standard. Der Infohinweis empfiehlt ein einzigartiges
Passwort und die doppelte Eingabe zur Bestätigung.

Beide Kontoänderungsdialoge verwenden dieselben lokalen Layoutregeln in
`cosmos-account.css` und die zentralen Cosmos-Dialogfarben für Header, Inhalt, Footer
und Border. Escape und Außenklick schließen nativ; der vorhandene Backdrop-Blur bleibt.
Beim Schließen und vor jedem Öffnen werden alle Passwortfelder geleert. Ein Sprachwechsel
übersetzt den offenen Dialog und erhält den aktuellen Entwurf. Der Profilentwurf bleibt
beim Öffnen und Schließen unverändert.

Der Bestätigungsbutton ist für die visuelle Abstimmung aktiv, führt aber noch keine
Validierung, Passwortprüfung, Speicherung oder Sitzungsänderung aus. Es gibt keine
Erfolgsmeldung ohne ausgeführte Änderung. Die eigentliche Funktion folgt gesondert.
Für diesen Stand bestanden 38 gezielte JUnit-Fälle (Passwortdialog, E-Mail-Dialog,
Kontokomponente, Konto-View und Übersetzungskatalog), ohne Fehler oder Skips.
Browserprüfungen und visuelle Abnahme dieses neuen Dialogs stehen noch aus; Browserläufe
erfolgen gesammelt und ausschließlich auf ausdrückliche Freigabe.
