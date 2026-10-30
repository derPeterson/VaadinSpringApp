# Echte Anwendung und HTML-Referenz vergleichen

## Besucherbereich

Der zusätzliche Besuchervergleich läuft in einer eigenen JVM mit eigener flüchtiger
Datenbank. Er braucht keine Anmeldung und verändert die feste Besucherreferenz nicht:

```powershell
.\docs\design\frontend-referenzen\quellen\anwendungsvergleich\vergleich.ps1 -Visitors -Port 60829 -Start
```

[Besuchervergleich öffnen](http://127.0.0.1:60829/images/visitor-review/index.html#login).
Ein aktueller optimierter Build muss wie unten beschrieben vorhanden sein; bei Bedarf
`-Build` ergänzen. Der Starter beendet keine fremden Prozesse. Der eigene Prozess und
die Laufzeitkopien liegen unter `target/visitor-reference-review/`.

Die 15 Szenen umfassen Startseite, Anmeldung, Registrierung, Passwortanforderung,
gültigen/ungültigen/abgelaufenen Passwortlink, bestätigte/abgelaufene/ungültige
Aktivierung, offene/abgelaufene/ungültige E-Mail-Bestätigung, Sitzungsgrenze und Wartung.
Größenauswahl und unabhängige Bedienung entsprechen dem angemeldeten Vergleich.
DE/EN lässt sich jeweils im Kopf der echten Anwendung auswählen. Die Texte und
Beispieldaten links und rechts dürfen fachlich begründet voneinander abweichen.
Die eingebettete Referenz lädt ohne Sprunganker; der Viewer setzt die Route danach
ohne Ankerscrollen. Jede Szenenauswahl beginnt dadurch am oberen Seitenrand.
Der eingeschränkte Anmeldeschritt bleibt innerhalb derselben Browsersitzung aktiv,
bis er beendet wird. Vor dem Wechsel zurück zur normalen Anmeldung dort
„Abbrechen“ wählen; die Szenenauswahl umgeht diesen echten Anmeldevertrag nicht.

`VisitorReviewExamples` erzeugt ausschließlich in dieser Testumgebung eigene
Tokenzustände und eine ausstehende Registrierung. Die bestätigte Verifikation wird
als bereits verbrauchter Link eines aktivierten Testkontos vorbereitet, ohne einen
realen Bestätigungsvorgang zu behaupten. Die Test-URLs werden erst zur Laufzeit in
`links.json` geschrieben. Die feste Referenz enthält keine echten Anwendungstokens.

Zwei ausschließlich in dieser Sichtung registrierte Testwege zeigen die echten
View-Komponenten: `VisitorMaintenancePage` hält die Wartungsansicht geöffnet, ohne
die restliche Sichtung global zu sperren. `VisitorRecoveryPage` bereitet über den
unveränderten Sitzungsdienst den eingeschränkten Anmeldeschritt für das feste
Testkonto vor. Die Fixture belegt dessen drei Sitzungsplätze. Es gibt keine neue
Produktionsroute und keinen Ersatz für fachliche Prüfungen. Beide Klassen und
der Datengenerator liegen außerhalb des ausgelieferten JARs.

Die Vergleichsdateien werden unter dem bereits öffentlichen statischen Ressourcenpfad
`/images/visitor-review/` bereitgestellt; die Anwendungssicherheit bleibt unverändert.
Passwort-/E-Mail-Aktionen zur Sichtung öffnen und abbrechen beziehungsweise leer
validieren. Kein externer Versand; SMTP bleibt auf Loopback-Port 1 beschränkt.

## Angemeldeter Bereich

Der Viewer stellt die unveränderte angemeldete HTML-Referenz neben die echte
Vaadin-Anwendung. Die Auswahl öffnet Startseite, Benachrichtigungen, persönliche
Daten, Sicherheit oder Geräte/Sitzungen auf beiden Seiten. Navigation, Dialoge
und Overlays bleiben unabhängig bedienbar. Desktop, Smartphone und Querformat
verwenden jeweils dieselbe innere Bildschirmgröße; „Originalgröße“ zeigt beide
Ansichten ohne Verkleinerung.

## Lokal vorbereiten und starten

Im Projektroot mit JDK 25:

```powershell
.\docs\design\frontend-referenzen\quellen\anwendungsvergleich\vergleich.ps1 -Build -Start
```

Bei vorhandenem aktuellem optimierten Build reicht `-Start`.
Mit `-Administrator` erhält ausschließlich das austauschbare Testkonto die
Administratorrolle, um auch dessen Avatar-Menüaktion prüfen zu können. Ohne
diesen Schalter bleibt es ein normales Benutzerkonto.

Der Starter beendet
keinen belegten Port und keine fremden Prozesse. Vor einem Neubau die eigene
Sichtungsinstanz beenden. Die Prozess-ID liegt unter
`target/account-reference-review/process-id.txt`; vor dem Beenden deren Zugehörigkeit
zur dortigen `java.args` prüfen. Nach einem Rechnerneustart erneut starten.

1. [Eigene Testanwendung anmelden](http://localhost:60827/login):
   `reference-user@example.test` / `ReferenceUser1!`.
2. [Vergleich öffnen](http://localhost:60827/vergleich/index.html).

Diese Zugangsdaten gehören ausschließlich zum neu angelegten lokalen Testkonto.
`localhost` verwenden, damit die Sitzung nicht mit dem früheren Piloten auf
`127.0.0.1` kollidiert. Ohne Anmeldung ist die geschützte Vergleichsseite nicht
erreichbar. Die feste HTML-Referenz enthält andere Beispieldaten und wird nicht
an das Testkonto angepasst.

## Isolation und Grenzen

`FrontendReviewApplication` liegt ausschließlich unter `app/src/test/java/`,
außerhalb des Anwendungsscans und des ausgelieferten JARs. Der Starter kompiliert
diesen Einstieg separat und lädt ansonsten den echten optimierten Anwendungsstand.
Es gibt keine nachgebauten Konto-, Nachrichten- oder Sitzungsdienste.

Die eigene H2-Datenbank liegt vollständig im Arbeitsspeicher und wird bei jedem
Neustart neu erstellt. Sie enthält ein synthetisches Benutzerkonto, 14 gelesene
und ungelesene Nachrichten, eine zusätzliche Geräte-Sitzung ohne nutzbaren
SecurityContext mit zugehöriger gespeicherter Anmeldung sowie eine alleinstehende
gespeicherte Beispielanmeldung. Die Beispieldaten werden
direkt in dieser Testfixture angelegt; sie behaupten keine tatsächlich erfolgten
Passwort-/E-Mail-Änderungen. Systemnachrichten werden im Gegensatz zu manuell
verfassten Beispielen durch den bestehenden Katalog DE/EN übersetzt.

Der Starter entfernt geerbte Anwendungseinstellungen und deaktiviert den `.env`-
Import. SMTP zeigt nur auf Loopback-Port 1; bestehende Datenbanken und fremde
Dienste werden nicht verwendet. Keine echten Zugangsdaten eingeben. Passwort-
und E-Mail-Dialog zum Vergleich öffnen, leer validieren und abbrechen; kein
Versand oder Passwortwechsel ist für die Sichtung nötig. Zurücksetzen der
gesamten Beispieldaten erfolgt durch Neustart dieses eigenen Prozesses.

Der Viewer verändert die Referenz nicht und führt keine zusätzlichen UI-Aktionen
in den Frames aus. Er lädt beim Bereichswechsel beide Ausgangsansichten neu.
Die Quellen dieses Viewers, Startskript und Referenz liegen außerhalb `target/`;
ein Clean entfernt nur die daraus vorbereitete Laufzeitumgebung.

## Administration vergleichen

Die eigene Adminsichtung verwendet eine getrennte flüchtige Umgebung:

```powershell
.\docs\design\frontend-referenzen\quellen\anwendungsvergleich\vergleich.ps1 -Administration -Port 60830 -Build -Start
```

Nach einem aktuellen Build genügt `-Administration -Port 60830 -Start`.
Prozesszugehörigkeit vor Beenden über `target/admin-reference-review/java.args`
und `process-id.txt` prüfen. Der Starter beendet keine fremden Prozesse.

Zuerst [im eigenen Admin-Testkonto anmelden](http://127.0.0.1:60830/login),
anschließend den [Adminvergleich öffnen](http://127.0.0.1:60830/vergleich/admin.html).
Das oben dokumentierte synthetische Testkonto erhält nur in dieser Umgebung die
Superadminrolle. Weitere 24 synthetische Konten machen Filter, Pagination und
Empfängerauswahl bedienbar. SMTP bleibt auf Loopback mit unbesetztem Port begrenzt.
Es werden keine bestehenden Daten oder echten Mailadressen verwendet.

Die Auswahl bietet Dashboard, Benutzerverwaltung, alle acht Konfigurationsgruppen,
Mailtest und Benachrichtigungen. Desktop, Smartphone und Querformat bleiben
umschaltbar; die feste Admin-HTML wird unverändert samt Revision kopiert. Ihr
bestehender Nachrichtenempfänger für die äußere Vorschau wählt die Referenzszene.
Die feste Referenz ist deutsch; die echte Anwendung kann über das Benutzermenü
zwischen Deutsch und Englisch wechseln. Native Grids, Kalender und Eingabemenüs
bleiben Vaadin-Komponenten. Der Vergleich dient der gemeinsamen Sichtung und
ersetzt weder die automatisierten Browser-Suiten noch eine Designabnahme.
