# Mein Konto: Profildaten, Sicherheit und Sitzungen

Stand: 17.10.2026. Aktuelle Erweiterung auf `feature/account-session-management`.

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
  Der E-Mail-Ändern-Button unter Sicherheit öffnet den unten beschriebenen Änderungsdialog.
  Im Profil gibt es keinen Ändern-Button und keinen zusätzlichen E-Mail-Hilfetext.
  Passwortänderung und E-Mail-Änderung sind jetzt funktional: [Sicherheitsvertrag](account-security-contract.md).
  Nur die Profilbildaktion bleibt ein bedienbarer Entwurf ohne Speicherfunktion.
- „Geräte & Sitzungen“ zeigt echte eigene Sitzungen und gespeicherte Anmeldungen.
  Einzelwiderruf, „Alle anderen abmelden“, Aktualisieren und das dynamische Limit sind
  angebunden. „Aktuelle Sitzung“ bleibt deaktiviert. Die statischen Entwurfsgeräte
  wurden entfernt. [Verhalten und Grenzen](session-management-contract.md).
- Die Oberfläche zeigt keine gespeicherten Passwörter, Hashes oder Sitzungstokens an.
- DE/EN-Texte liegen im gemeinsamen typisierten `MessageTexts`-Katalog.

Die vorhandene Sitzungsbegrenzung bleibt wirksam. Nach korrektem Passwort kann bei
erreichtem Limit eine befristete, eingeschränkte Sitzungsliste geöffnet werden;
die normale Anwendung bleibt bis zur erfolgreichen Zulassung gesperrt.
Die vollständige QA dieser Erweiterung ist am 17.10.2026 nach der Freigabe ab 08:00 Uhr
erfolgreich abgeschlossen; die manuelle Nutzerabnahme steht noch aus.

## Gestaltung

`AccountView` verwendet denselben Hintergrund, dieselbe Topbar und dasselbe
Benutzermenü wie Home. `CosmosSectionHeaderComponent`, `CosmosCardComponent` und
die vorhandenen Panel-/Badge-/Button-/Inputregeln bleiben unverändert.
`AccountPanelComponent` ordnet die bestehenden Panelbausteine für diese Seite an.

`cosmos-account.css` wird an `AccountView` und für die eingeschränkte Sitzungsliste an `LoginView` geladen. Die Selektoren sind auf
`.cosmos-account` begrenzt. Die Datei ergänzt nur lokale Anordnung, kleine
Bildschirme und die neue Tab-Leiste; Farben kommen aus den vorhandenen Tokens.
Native [Vaadin-Tabs](https://vaadin.com/docs/latest/components/tabs/styling) behalten
Tastaturbedienung, Fokuszustände und Scrollbuttons. Es werden weder die HTML-Vorlage
noch ihre JavaScript-Demoaktionen in die Anwendung eingebunden.

Der Entwurf ist für die schrittweise visuelle Abstimmung vorgesehen. Gemeinsame
Styles, Icons, Buttonmaße und das übrige Anwendungsdesign wurden nicht umgestellt.
Profil, Sicherheit und Geräte/Sitzungen verwenden dieselbe Footerfläche einschließlich
Farbe, oberem Border, Innenabständen und mobilem Verhalten.
DE/EN und vom Standardwert abweichende Konfigurationslimits sind durch JUnit geprüft;
die realen Sitzungsabläufe sind zusätzlich in beiden vollständigen Browserläufen enthalten.

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

Der abschließende Gesamtstand vom 17.10.2026 bestand **1.424 JUnit-Fälle**, 47
Browserhelfer-/Reporterprüfungen und `all` mit **je 136/136 Browserfällen DE/EN**,
jeweils ohne Fehler oder Skips. JUnit umfasst isolierte H2-/Transaktionsprüfungen
für alle Rollen, Versionskonflikte, Sicherheitsfelder, Wartung und Sitzungswiderruf.

Die Browser-Suite `account-settings` enthält sieben Fälle: dauerhaftes und wiederholtes
Speichern nach Neuladen, Pflichtfelder und Datumsregel ohne Persistierung ungültiger
Werte, gespeicherte Sprache sowie die vier Schließwege des E-Mail-Dialogs. Einstieg
unter Sicherheit, unveränderte Profildrafts, deaktivierte E-Mail, übersetzte Felder,
Bestätigungsicon und berechneter 6px-Blur werden über die echte Oberfläche geprüft.
`account-security` ergänzt vier Fälle für Validierung und erfolgreiche Sicherheitsänderungen
mit tatsächlich empfangenen eigenen Mailpit-Mails. `session-management` ergänzt fünf
Fälle einschließlich gezieltem Widerruf, aktuellem Zugang, Serverneustart und Limit-Recovery.

[Abdeckung und Messgrenzen](../browser-tests/abdeckung.md),
[dauerhafter Meilenstein](meilensteine/2026-10-17-sitzungsverwaltung-und-kontosicherheit.md).
Automatisierte Styles und Desktop-Chromium ersetzen keine menschliche Abnahme auf
echten Geräten. Profilbildbearbeitung ist weiterhin nicht implementiert.

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
JUnit prüft Tab-URLs, Rückfallwerte, Zugriffsschutz, Sprachwechsel und Erhalt der Entwürfe.
Die vollständigen Browserläufe DE/EN prüfen die verwendeten Tabwechsel und Neuladen;
die neue Sitzungsreise bestätigt dabei auch erhaltene Entwürfe und Tabzählung.

## Passwort und E-Mail ändern

Beide Dialoge unter Sicherheit sind funktional. Sie verwenden unverändert die
abgestimmten Cosmos-Header, Card-/Footerflächen, Icons, Feld- und Notificationbausteine.
Eingaben werden erst beim Absenden geprüft und ungültige Felder rot markiert. Schließen
per Icon, Abbrechen, Escape oder Außenklick verwirft die Eingaben; Sprachwechsel erhalten
Entwürfe. Passwort und Bestätigung stehen vor dem aktuellen Passwort, entsprechend der
Anordnung im E-Mail-Dialog.

„Passwort ändern“ prüft das aktuelle Passwort erneut, speichert einen neuen BCrypt-Hash
und merkt eine Bestätigungsmail in der Outbox vor. Andere Sitzungen und gespeicherte
Anmeldungen werden widerrufen; die aktuelle Sitzung bleibt erhalten. Profilentwürfe
bleiben bestehen; eine fremde zwischenzeitliche Profiländerung wird nicht durch bloßes
Fortschreiben der UI-Version übergangen.

„Bestätigung anfordern“ prüft aktuelle Zugangsdaten und die neue Adresse. Die bisherige
Adresse bleibt bis zur ausdrücklichen Bestätigung gültig. Ein zeitlich begrenzter Einmallink
führt zu einer eigenen Cosmos-Bestätigungsseite. Erst der Bestätigungsbutton ändert die
Adresse, beendet alle bisherigen Sitzungen und merkt Sicherheitsmails an beide Adressen vor.
Danach wird die neue Adresse zur Anmeldung verwendet. Der vollständige Ablauf einschließlich
Fehlern, Versuchsbegrenzung und Prüfgrenzen steht im [Sicherheitsvertrag](account-security-contract.md).

Die statischen Infohinweise erklären die Sitzungsfolgen vor dem Absenden; die Sicherheitsmails
wiederholen sie. Die Erfolgsmeldungen unterscheiden eine gespeicherte Änderung bzw. vorgemerkte
Mail von einer tatsächlich nachgewiesenen Zustellung. Die realen Browser-/Mailpit-Abläufe
beider Änderungen sind in DE/EN bestanden. Reale externe Postfächer und Mailclients
sind damit nicht geprüft.
