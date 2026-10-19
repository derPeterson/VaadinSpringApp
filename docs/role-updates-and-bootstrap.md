# Initiale Benutzerlisten und Rollenänderungen ohne erneute Anmeldung

Stand: 18.10.2026, Ausgangscommit `5d5ddc5`.
Implementierung und Java-Prüfung abgeschlossen; Browserabnahme und Produktionsbuild offen.

## Initialisierung

`app.bootstrap.enabled` aktiviert die optionale Anlage konfigurierter Konten;
Vorgabe `false`. Die vier unabhängigen Listen heißen:

| Liste | Ausschließlich gespeicherte Rolle |
| --- | --- |
| `app.bootstrap.superadmins` | `ROLE_SUPERADMIN` |
| `app.bootstrap.admins` | `ROLE_ADMIN` |
| `app.bootstrap.users` | `ROLE_USER` |
| `app.bootstrap.guests` | `ROLE_GUEST` |

Eine Liste mit `[0]` legt ein Konto an; `[1]`, `[2]` usw. ergänzen weitere.
Leere Gruppen sind erlaubt. Die Initialisierung erzeugt keine versteckten Zusatzrollen.
Superadmin erhält weiterhin effektiv Adminrechte über die bestehende Rollenhierarchie,
ohne einen zusätzlichen gespeicherten Admin-Eintrag.

Vollständiges [Properties-Beispiel](bootstrap-accounts.example.properties) mit zwei
Superadmins und je einem Admin/User/Guest. Die Werte sind erfunden. Eigene Zugangsdaten
nur lokal setzen. Die vorhandene lokale `.env` wird als Java-Properties-Datei importiert;
Listen dort mit Punkt-/Klammernotation schreiben, beispielsweise:

```properties
APP_BOOTSTRAP_ENABLED=true
app.bootstrap.superadmins[0].email=admin@example.com
app.bootstrap.superadmins[0].password=EigenesLokalesPasswort!
app.bootstrap.admins[0].email=administrator@example.test
app.bootstrap.admins[0].password=AnderesLokalesPasswort!
```

Bei echten Betriebssystem-/Docker-Umgebungsvariablen entspricht dies
`APP_BOOTSTRAP_SUPERADMINS_0_EMAIL`, `APP_BOOTSTRAP_SUPERADMINS_0_PASSWORD` bzw.
`APP_BOOTSTRAP_ADMINS_0_EMAIL`, `APP_BOOTSTRAP_ADMINS_0_PASSWORD`.
Die Indizes sind Bestandteil der Variablennamen. Die vorhandene `.env`-Datei ist dagegen
eine importierte Properties-Quelle; dort die oben gezeigten Listenschlüssel verwenden.
Beide Bindungsarten sind in Java geprüft.

Pflichtfelder je Konto: gültige E-Mail und Passwort nach derselben Regel wie Registrierung.
Optionale Angaben: `first-name`, `last-name`, `birth-date`, `gender`, `preferred-locale`.
Vorgaben sind John, Doe, 1990-01-01, OTHER und en. Angegebene Namen dürfen nicht leer sein,
Geburtstage müssen vergangen sein, Sprache muss de oder en sein.

Vor Benutzeranlage werden alle aktivierten Listen validiert. Kanonisch gleiche
E-Mail-Adressen – auch innerhalb verschiedener Gruppen – sind ein Fehler, keine Aufforderung
zur Rollenvereinigung. Rollen- und Benutzeranlage erfolgen gemeinsam in einer Transaktion;
ein späterer Persistenzfehler hinterlässt keine teilweise angelegte Benutzerliste.
Passwörter werden BCrypt-gehasht. Fehlermeldungen und die neuen Bootstrap-Logs geben
keine Passwörter aus; Konfigurationsobjekte haben keine Zugangsdaten ausgebende `toString`-Methode.

Bereits bestehende E-Mail-Adressen werden übersprungen: keine Rollenänderung, keine
Passwort-/Profilüberschreibung, keine automatische Beförderung. Wiederholte Starts legen
keine Duplikate an. Bei aktivierter Konfiguration werden noch fehlende Konten ergänzt;
es gibt keine automatische Löschung von später aus einer Liste entfernten Konten.
Initiale Rollenzuweisungen erzeugen keine Mitteilung „Berechtigungen geändert“.

Die bisherige einzelne `app.bootstrap.default-admin.*`-Konfiguration und deren drei
`APP_BOOTSTRAP_DEFAULT_ADMIN_*`-Variablen sind abgelöst. Lokale Einträge bei Bedarf in die
neuen Listen übertragen; die echte lokale `.env` wurde weder gelesen noch verändert.
Die E-Mail `admin@example.com` hat keine fest codierten Sonderrechte: steht sie in der
Superadmin-Liste, erhält das neu angelegte Konto genau Superadmin.

## Rollen vergeben und Konten schützen

`AdminRolePolicy` liest die aktuellen ausdrücklich gespeicherten Rollen des angemeldeten
Handelnden frisch aus der Datenbank. Stale Principals und veraltete Formularwerte erteilen
keine zusätzlichen Rechte.

- Administratoren dürfen Admin, User und Guest vergeben und entfernen.
- Nur Superadmins dürfen Superadmin vergeben oder entfernen.
- Normale Admins dürfen bestehende Superadminkonten auch nicht durch Passwort-/Adressänderung,
  Sperren oder Löschen übernehmen beziehungsweise umgehen. Die entsprechenden Zeilenaktionen
  sind deaktiviert; die Superadmin-Checkbox ist nur für Superadmins veränderbar.
- Die Fachlogik prüft unabhängig von der Oberfläche. Neuer administrativer Benutzer ist eine
  zusammenhängende Operation `createAdminUser`; Rollenänderungen über generisches Speichern
  werden ebenfalls geprüft. Öffentliche Registrierungen erhalten weiter ausschließlich User.
- Der letzte aktive Superadmin darf nicht gelöscht, deaktiviert oder seiner Superadmin-Rolle
  beraubt werden, auch wenn andere normale Admins vorhanden sind oder er zusätzlich Admin ist.
  Für normale Admins gibt es keinen Mindestbestand. Alle Mutationen teilen eine Datenbanksperre;
  konkurrierende Änderungen und bereits geänderte Entities werden ebenfalls geprüft.
  Der separate UI-Selbstschutz bleibt bestehen. Ein anfänglicher Zustand ohne Superadmin wird
  nicht automatisch repariert; zur Initialisierung mindestens einen Superadmin konfigurieren.

## Bestehende Anmeldungen

Eine reine Änderung der Rollen beendet die Anmeldung nicht. `SecurityService` lädt frische
Berechtigungen und ersetzt die Authentifizierung in der HTTP-Sitzung sowie bei interaktiven
Anfragen im aktuellen Sicherheits-/Vaadin-Kontext. Identität, HTTP-Sitzungs-ID, Sitzungsplatz
und Remember-me-Zugang bleiben erhalten. Ein Remember-me-Principal bleibt ein Remember-me-
Principal; vorhandene echte Spring-Authentifizierungsfaktoren werden übernommen. Unbekannte
erfundene Kontoprivilegien werden weiterhin abgewiesen.

Die aktuelle Oberfläche lädt bei einer Rollenänderung automatisch als neues Dokument:
Menüs und Views werden damit anhand der neuen Berechtigungen aufgebaut. Beim Verlust des
Adminzugangs wird aus dem Adminbereich auf Home weitergeleitet. Auf erlaubten Seiten bleiben
Pfad und Queryparameter erhalten, beispielsweise `/account?tab=security`. Offene Dialoge und
nicht gespeicherte Eingaben werden durch dieses Neuladen verworfen.

`RoleUiGuard` bindet den ursprünglichen Berechtigungsstand je UI. Die alte UI bleibt bis
zum Dokumentwechsel als veraltet markiert. Ein alter HTTP-UIDL-Aufruf wird vor fachlicher
RPC-Ausführung abgefangen und erhält einen Reload-Befehl durch Vaadins eigenen UIDL-Writer.
Da der ursprüngliche Aufruf verworfen wird, ist die Reload-Ausgabe als asynchrone
Framework-Nachricht markiert. Auf bestehenden WebSockets stoppt der Request-Interceptor
alte RPCs und stößt denselben Dokumentwechsel an. Alte Dialoge können so keine entzogenen
Rechte weiterverwenden. Zusätzlich prüfen administrative Benutzermutationen frisch im Service.

Ein lokaler Rollen-Broadcaster informiert offene UIs nach Commit; Detach entfernt die
Abonnements. Wird ein Livehinweis verpasst, wird der Wechsel vor der nächsten Interaktion
erkannt. Wie bei den übrigen lokalen Broadcastern gilt unmittelbare Zustellung zunächst
innerhalb einer laufenden Anwendungsinstanz, ohne instanzübergreifenden Broker.

Kontosperre, Kontolöschung, Änderung der Anmeldeidentität und ausdrücklich widerrufene/
abgelaufene Sitzungen bleiben davon getrennt: Ihre bestehende Abmeldewirkung ist erhalten.
Der Push-Interceptor entfernt seinen Spring-Sicherheitskontext bei Ende/Abbruch, damit
keine Identität auf einem wiederverwendeten Workerthread zurückbleibt.

## Mitteilung und Einführung

Bei tatsächlich geänderter Rollenmenge wird synchron in derselben Transaktion ein
`ROLES_CHANGED`-Eintrag für das betroffene Konto gespeichert. Text und Titel sind DE/EN;
die Mitteilung verlangt keine neue Anmeldung. Es wird keine zusätzliche E-Mail versandt.
Unverändertes Speichern und Rollbacks erzeugen keinen dauerhaften Erfolgseintrag.
Scheitert die Pflichtmitteilung, rollt die Rollenänderung zurück. Der Rollen-Push folgt
erst nach Commit. Die bestehende Inbox behält ihre übrigen Regeln und Aufbewahrungsfrist.

Beim Start erweitert `UserNotificationSchemaUpdater` eine gegebenenfalls bestehende native
H2-Enum-Spalte `user_notifications.type` um die bekannten Java-Werte. Bestehende Mitteilungen
bleiben erhalten; VARCHAR-Spalten brauchen diese Erweiterung nicht. Die Migration ist
wiederholbar und mit künstlichem alten H2-Schema getestet.

Shared-Modul neu bauen/installieren und die Anwendung vollständig neu starten. Die neuen
Beans, Properties-Bindung, Request-Handler und Schemaeinführung sind kein reiner HotSwap-
Wechsel. Danach sind weitere Rollenänderungen ohne neue Anmeldung möglich. Die alten lokalen
Bootstrap-Einstellungen bei Bedarf auf die neuen Listen umstellen; Bestandskonten werden
absichtlich nicht beim Neustart umgestuft.

## Tatsächliche Prüfung

Java 25.0.4.1, Shared neu gebaut/installiert, Spotless Apply/Check erfolgreich und Diff geprüft.

| Prüfung | Ergebnis |
| --- | --- |
| Anwendungs-JUnit vollständig | **1667/1667**, 0 Fehler, 0 Fehlschläge, 0 Skips |
| Neue Fälle gegenüber `5d5ddc5` | 54: 12 Bootstrap-Persistenz/Binding, 28 echte Rollen-/Mitteilungsfälle, 10 UI-/UIDL-Guards, 2 Schemafälle, 1 Checkboxfall, 1 zusätzlicher Superadmin-Bestandsfall |
| Browser-Helfer ohne Browserstart | **47/47**, 0 Fehler, 0 Fehlschläge, 0 Skips |
| JaCoCo Zeilen | **8643/10120 (85,41 %)** |
| JaCoCo Zweige | **2486/3387 (73,40 %)** |
| Messumfang | 209 ausführbare Anwendungs-Java-Klassen, frischer vollständiger Lauf dieses Stands |

Bestehende Session-/Security-/Push-/Benutzerformularfälle wurden dem neuen Vertrag angepasst:
reale alte RPCs bleiben gesperrt, Rollenänderungen erhalten die Sitzung, Sperre/Löschung/
Widerruf melden weiterhin ab. Neue Tests prüfen echte frische Rollenprüfung gegen Manipulation,
keine unberechtigte Superadmin-Anlage/-Änderung, Session-ID-Erhalt, Mitteilung nur beim Empfänger,
Rollback der Rollen bei Mitteilungsfehler, Publish erst nach Commit, native UIDL-Ausgabe ohne
Dispatch, Livefilter/Detach und Bootstrap-Atomizität/Duplikate/Properties-/Umgebungsbindung.

Die erste vollständige Runde zeigte zwei zuvor unvollständige Sicherheitsfixtures: „anonym“
hatte nur den Thread-Kontext geleert, ein zweites Konto nur den Thread-Principal ersetzt,
während die erste HTTP-Anmeldung noch bestand. Fixtures verwenden jetzt tatsächlich getrennte
anonyme beziehungsweise angemeldete Sitzungen. Die Sicherheitsassertionen sind erhalten.

Keine reale Datenbank, lokale Zugangsdaten oder Nutzerprozesse verwendet. Alle JPA-/Schema-
Prüfungen liefen in eigenen H2-Datenbanken. Keine reguläre App und keine Dockerumgebung gestartet.
Die bestehenden Offline-/Queue-JUnits nutzen ausschließlich ihre eigenen Fixtures.

Kein Browserlauf oder Produktionsbuild. Die Browserquellen sind kompiliert und die 47
Helfer geprüft, jedoch keine Browser-Journeys dieses Stands ausgeführt. Der Browser-Compose-
Override verwendet getrennte erfundene Admin-/Superadmin-Konten; Tests, die Superadmin
vergeben, melden sich dafür mit dem Superadmin an. Vorherige Browserergebnisse bleiben historisch.
Keine CSS-/JavaScript-/Screenreader-Abdeckung gemessen; Shared, Testcode und Bibliotheken
sind nicht im JaCoCo-Nenner enthalten.

Offen für die spätere Browserfreigabe: Rollen hinzufügen/entziehen in zwei Tabs/Geräten,
Admin→Home ohne Login, Erhalt des Konto-Tabs, offene alte Verwaltungsdialoge, Glockenzähler
und Rollenmitteilung in DE/EN, Superadmin-Checkbox und Ablehnungen mit normalem Admin,
Remember-me/Sitzungslimit nach Rollenwechsel sowie vollständiger Start am erweiterten Schema.

## Ergänzter Mindestbestandsschutz

Der endgültige Lauf enthält 14 zusätzliche Fälle gegenüber dem Rollen-/Bootstrap-Stand mit
1653 Tests. Geprüft sind letzter aktiver Superadmin bei Löschen/Deaktivieren/Rollenentzug,
Superadmin → Admin trotz vorhandenem normalen Admin, kombinierte Admin-/Superadmin-Rolle,
deaktivierter zweiter Superadmin, erfolgreiche Entfernung bei vorhandenem Ersatz sowie
der fehlende Mindestbestand für normale Admins mit und ohne anfänglichen Superadmin.
Die bestehenden Parallelitäts-, Batch-/Dirty-Entity-, Rollback- und DE/EN-Notificationfälle
prüfen jetzt dieselbe Superadmin-Regel. Fehlgeschlagene Entzüge erzeugen weder Mitteilung
noch Rollen-Push. Vier Sitzungsfixtures besitzen für ihren erlaubten Superadmin-Entzug
einen zweiten aktiven Superadmin; die Assertions zum erhaltenen Login sind unverändert.

Shared erneut gebaut/installiert, Spotless Apply/Check und alle 1667 Anwendungs-JUnits
sowie 47 Helfertests erfolgreich. Browserquellen gegen den neuen Katalog kompiliert,
weiterhin kein Browser-/Produktionslauf. Für die Übernahme die Anwendung neu starten;
die neue Shared-Katalogsignatur und Texte werden dann gemeinsam geladen.
## Nächtliche Gesamt-QA vom 19.10.2026

USER-07H belegt Rollenvergabe und -entzug in zwei echten getrennten Browserkontexten:
neue Adminnavigation, geöffnetes Adminformular, Weiterleitung auf Home nach Entzug,
identische Session-ID, kein erneuter Login-POST und persönliche Rollenbenachrichtigung.
Der Fall ist auf DE/EN erfolgreich. SESSION-01A folgt nun diesem erhaltenen Login;
Sperre/Löschung verlangen weiterhin 401 und Anmeldung. USER-07E berücksichtigt die
Bootstrap-Fixture mit ausschließlich Adminrolle, ohne den Selbstschutz abzuschwächen.

Die aktuellen Gesamtzahlen, Coverage und verbleibenden Kombinationen stehen im
[QA-Meilenstein](meilensteine/2026-10-19-benachrichtigungen-rollen-und-gesamtqa.md).
Die früheren Aussagen „kein Browserlauf“ sind historische Stände; der genaue aktuelle Browserumfang ist im Meilenstein ausgewiesen.
Konto-Tab-Erhalt und Remember-me nach Rollenwechsel sind weiterhin keine neuen
separaten Browserkombinationen; die zugehörigen technischen Regeln bleiben geprüft.
