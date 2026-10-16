# Wartungsmodus: Zugang, Sitzungen und Wiederfreigabe

Stand: 16.10.2026. Implementierung auf `feature/maintenance-mode`, noch uncommittet.
Abgenommen mit 1.226 Anwendungstests und vollständigem `all` auf Deutsch und Englisch
(je 118 Fälle). [Dauerhafter Abschlussnachweis](meilensteine/2026-10-16-wartungsmodus-und-gesamttests.md).

## Bedienung und Wirkung

Unter Admin → Konfiguration → Sicherheit steuert `maintenance.mode` die normale
Nutzung. Standard ist `false`. Nach Speichern gilt die Entscheidung beim nächsten
relevanten Zugriff ohne Neustart. Für diese erstmalige Java-Erweiterung muss die
Anwendung einmal mit dem neuen Stand gebaut und neu gestartet werden. Der Shared-
Katalog wird nach Projektvorgabe vor dem Anwendungsbuild installiert.

| Zugriff bei aktiver Wartung | Verhalten |
| --- | --- |
| Anonymer Besucher, Benutzer, Gast | Wartungscard statt normaler Nutzung |
| Passwortlogin Benutzer/Gast | Nach gültiger Passwortprüfung abgewiesen; kein neuer Sitzungsplatz oder Remember-me-Cookie |
| Admin/Superadmin | Login und Verwaltung einschließlich Ausschalten weiter erreichbar |
| Bereits angemeldeter Benutzer/Gast | Fachaktionen pausieren; Sitzung bleibt gespeichert |
| Registrierung, Verifikation, Passwortreset | Vor Konto-/Token-/Queueänderung gesperrt |
| Logout | Weiter erreichbar |
| Benötigte Styles, Icons, Fonts und UI-Ressourcen | Weiter ausgeliefert |
| Mailqueue, Statusjobs und Bereinigung | Laufen weiter |

Die Wartungscard verwendet bestehende Cosmos-/Statusbausteine und DE/EN-Texte aus
`MessageTexts`. Sie bietet administrative Anmeldung, erneuten Versuch und bei
bestehender Anmeldung eine Abmeldung. Die Admin-Seitenleiste zeigt unten dauerhaft den Betriebsstatus: grün mit
„Status: Online / Alle Dienste bereit“, bei Wartung rot mit
„Status: Wartung / Dienste eingeschränkt“. Ein Lesefehler der Konfiguration zeigt
„Status: Nicht verfügbar“ ebenfalls rot. Die Anzeige verwendet bestehende
Cosmos-Flächen, Rahmen und Lumo-Statusfarben. Sie folgt Sprachwechsel,
Bereichswechsel und dem Speichern des Wartungsschalters in derselben Ansicht.
Der Status ist kein technischer Healthcheck einzelner Hintergrunddienste. Globale Styles und
bestehende Icons wurden nicht neu gestaltet.

„Administrative Anmeldung“ wird ausschließlich ohne bestehende Anmeldung
angezeigt und öffnet das normale Loginformular. Bei bestehender Anmeldung ist
stattdessen „Abmelden“ sichtbar. „Erneut versuchen“ bleibt in beiden Fällen
verfügbar. Es erfolgt keine automatische Abmeldung.

Wartung ist **kein Datenbank-/Worker-Shutdown**. Für Schemaarbeiten oder Eingriffe
in Daten ist weiterhin der geregelte Anwendungs- und Worker-Stopp erforderlich.

## Schutz und Fehlerverhalten

`MaintenanceService` liest den aktuellen Boolean für neue Zulassungsentscheidungen.
Ungültige oder nicht lesbare Konfiguration ergibt den Zustand `UNAVAILABLE` und
öffnet den Zugang nicht still. Neue Logins zeigen dann den vorhandenen technischen
Verfügbarkeitshinweis. Scheduler werden nicht über einen allgemeinen Thread- oder
Authentifizierungszwang gesperrt.

Die gemeinsame `SessionAdmissionService` prüft neue Passwort- und Remember-me-
Sitzungen vor Registrierung, Context-Veröffentlichung und Abschluss des Logins.
Eine ausdrückliche erneute Anmeldung wird auch bei einem bereits belegten eigenen
Sitzungsplatz geprüft. Bloße Context-Wiederherstellung bleibt davon getrennt.
Bestehende registrierte Sitzungen dürfen ihren Context wiederherstellen; die
Transport- und Routenschutzschichten pausieren ihre normalen Aktionen. Bestehende
Privilegienprüfungen gegen gesperrte, gelöschte oder umgestufte Konten bleiben davor
wirksam. Die Wartung selbst invalidiert keine bestehende Sitzung.

Die Passwortprüfung und der bisherige Anmeldeversuchsschutz bleiben aktiv.
Ein falsches Passwort zählt weiterhin als Fehlversuch; Wartungsablehnung nach
korrektem Passwort ist kein zusätzlicher Passwortfehler. Admin/Superadmin umgehen
weder Passwortsperre noch Sitzungslimit.

`MaintenanceHttpFilter` läuft nach der Privilegienprüfung und vor der Servlet-
Ausführung. Normale gesperrte Dokumentaufrufe werden mit **HTTP 503** intern an die
Wartungsansicht weitergegeben. Es wird keine Endzeit erfunden und deshalb kein
`Retry-After` gesetzt. Ein gezielter Index-HTML-Listener erhält beim internen
Forward den Ressourcenpfad zur Anwendungswurzel, auch für verschachtelte Tokenlinks.
UIDL erhält eine Vaadin-JSON-Weiterleitung statt HTML.
Unbekannte oder gefälschte UI-IDs erhalten keine Freigabe allein wegen `/login` im
URL-Pfad: Die tatsächlich zur Sitzung gehörende aktive Login-/Wartungs-UI entscheidet.
Eine tatsächlich vom Server angelegte, noch leere UI darf ihre erste UIDL zur
Routenauflösung ausführen. Sie hat noch keine aktive Fachansicht. Diese
Initialisierung wird zusätzlich durch den UI-Routenlistener und die fachlichen
Service-Einstiege geschützt. Init/Heartbeat gelten nur am tatsächlichen
Flow-Servicepfad; ein gefälschter Vaadin-Parameter gibt weder H2-Konsole noch
andere Servlets frei. Heartbeat führt keine Fachcallbacks aus.

Der vorhandene Push-Interceptor prüft auch Nachrichten auf schon offenen
WebSocket-Verbindungen vor `ServerRpcHandler` und DOM-Callbacks. Bei Wartung wird
weitere Ausführung gestoppt und eine zuordenbare UI zur Wartung geleitet. Die Rolle
stammt aus der angefragten Sitzung bzw. ihrem Request-Principal; ein fremder
Thread-Kontext erteilt weder normalen noch anonymen Sockets eine Adminausnahme. Ein bloßer
Routenlistener oder ausgeblendete Buttons sind nicht der Zugriffsschutz.

Öffentliche Mutations-Einstiege für Verifikation und Passwortreset prüfen vor
Seiteneffekten. `RegistrationService` verbindet Wartungsprüfung, Benutzeranlage und
erste Verifikationsmail in einer Transaktion; auch ein später beobachteter
Wartungswechsel oder ein Vorbereitungsfehler rollt die Anlage zurück. Scheduler-
Bereinigungen bleiben getrennt. Vor Umschalten bereits zugelassene laufende Vorgänge
können enden; der Schalter beendet keine laufende Transaktion rückwirkend.

## Tokens, Remember-me und Wiederfreigabe

Gültige Verifikations-/Resetlinks werden durch eine Wartungsablehnung nicht
verbraucht. Ihre Zeit läuft weiter; es gibt keine automatische Verlängerung.
Nach Freigabe gilt die bisherige Validierung einschließlich abgelaufener Links.
Bereits eingereihte Mails werden nicht gelöscht oder umgeschrieben.

Remember-me ist keine Rollenausnahme. Eine neu abgewiesene automatische Anmeldung
bekommt keinen Sitzungsplatz; ihr Browser kann das Remember-me-Cookie verlieren und
muss nach Freigabe gegebenenfalls manuell anmelden. Andere Geräte werden dadurch
nicht pauschal abgemeldet. Noch gültige bestehende Sitzungen können nach Ausschalten
weiterverwendet werden; normale Timeouts und Privilegienänderungen gelten weiter.

## Prüfumfang

JUnit prüft Rollen/Anonymität, Konfigurationswechsel und Lesefehler, Zulassung vor
Sitzungsanlage, erhaltene Sitzungen, Dokumentstatus und UIDL-Umgehungsversuche.
Die echte Spring-Security-Kette prüft Passwort- und Logoutverhalten. Der echte
`PushHandler` wird mit gültigen RPC-Nachrichten gegen Wartung und anschließende
Wiederfreigabe geprüft. Isolierte H2-Persistenztests prüfen unveränderte Tokens,
Passwörter/Queue und vollständigen Registrierungsrollback.

`maintenance` enthält neun Browserfälle (MAINT-01, MAINT-02A–D, MAINT-03,
MAINT-04A/B, MAINT-05) für HTTP 503, alte Formulare, alle Rollen, echte Mailtokens,
erhaltene Sitzungen und tatsächlichen Chromium-Neustart. Sie sind auch in `all`
enthalten; `fast` bleibt unverändert. Tatsächliche Ausführungen und Coverage stehen
in [Browserabdeckung](../browser-tests/abdeckung.md).

Die Prüfungen verwenden eigene Docker-/H2-/Mailpit-Daten. Produktive Daten,
Postfächer und Benutzeranwendungen werden nicht verwendet. Browsertests blockieren
wie bisher Service Worker; echte PWA-Cache-/Offline-Sonderfälle bleiben eine eigene
Betriebs-/Geräteprüfung. Der Wartungszustand enthält keine geplante Endzeit.

## Gezielte Nachkorrektur: Anmeldeaktionen der Wartungscard

Am 16.10.2026 wurde die Sichtbarkeit auf den tatsächlichen Anmeldestatus
abgestimmt: anonym nur administrative Anmeldung, angemeldet nur Abmelden;
erneuter Versuch jeweils verfügbar. Die zwischenzeitliche Fehlermeldung für
bereits angemeldete normale Konten und ihre gesonderten Übersetzungen entfallen.

`MaintenanceViewTest` prüft die Aktionen für alle vier Rollen und anonyme
Besucher, DE/EN-Sprachwechsel, den unveränderten Sitzungskontext und die Navigation
zum Login. MAINT-01 ist um die sichtbaren beziehungsweise ausgeblendeten Aktionen
ergänzt. Die oben verlinkten vollständigen Browser- und Coverage-Ergebnisse bleiben
der davor gemessene Stand.

Gezielter Nachtest: 47 JUnit-Fälle aus `MaintenanceViewTest` und
`MaintenanceServiceTest` erfolgreich, keine Fehler oder übersprungenen Fälle.
Shared-Modul gebaut, Google Java Format geprüft; ergänzten Browserfall
kompiliert, für diese Sichtbarkeitskorrektur nicht erneut im Browser ausgeführt.

## Gezielter Nachtest: Betriebsstatus in der Admin-Seitenleiste

79 gezielte JUnit-Fälle aus `CosmosServiceStatusComponentTest`,
`AdminConfigurationGroupTest`, `AdminTokenDurationValidationTest`,
`MaintenanceServiceTest` und `MaintenanceViewTest` erfolgreich, keine Fehler oder
übersprungenen Fälle. Die zehn neuen Komponentenfälle prüfen DE/EN, beide
Schaltrichtungen über den echten Speichern-Button und die Anzeige bei nicht lesbarer
Konfiguration. Der frühere Topbar-Hinweis und sein Übersetzungseintrag sind entfernt.
MAINT-01 erwartet jetzt den Status in der Seitenleiste; der geänderte Browserfall
ist kompiliert, für diese Gestaltung aber nicht erneut im Browser ausgeführt.
Die bestehenden vollständigen Browser-/Coverage-Ergebnisse gelten weiterhin für
den davor gemessenen Stand.
