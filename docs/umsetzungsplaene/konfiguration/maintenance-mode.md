# Wartungsmodus mit kontrolliertem Zugang

Stand: 15.10.2026; Ausgangscommit `1d57a5e`. **Umsetzungsplan, noch nicht implementiert.**

| Eigenschaft | Wert |
| --- | --- |
| Konfiguration | `maintenance.mode` / `ConfigEntry.MAINTENANCE_MODE` |
| Enum-Standard | `false` |
| Gruppe | Sicherheit |
| Vorgeschlagene Aktivierung | Nach Speichern ab nächstem relevanten Zugriff, ohne Neustart |
| Priorität | Nach Klärung der Anmelde- und Sitzungswege |

## Heutiger Stand

Die Checkbox wird geladen und gespeichert. Keine Route, Anmeldung oder Fachaktion
liest ihren Wert. Home und Kontoansichten bleiben öffentlich erreichbar. Der bestehende
HTTP-/Push-Schutz verhindert Aktionen mit entzogenen Sitzungsprivilegien, enthält
aber keine Wartungsentscheidung.

## Empfohlener erster Umfang

**Vorschlag:** Wartung sperrt die normale Nutzung für anonyme Besucher, Benutzer und
Gast. Admin und Superadmin können sich weiterhin anmelden und verwalten. Diese
bewusste Ausnahme ermöglicht auch das Ausschalten des Modus.

| Vorgang bei eingeschalteter Wartung | Empfohlenes Verhalten |
| --- | --- |
| Öffentlicher Besuch von Home | Wartungsseite statt normaler Inhalte |
| Neue Anmeldung | Formular erreichbar; nach Passwortprüfung nur Admin/Superadmin zulassen |
| Angemeldeter Benutzer/Gast | Ab nächster Aktion Wartungsansicht; keine weiteren Fachänderungen |
| Angemeldeter Admin/Superadmin | Verwaltung und Ausschalten bleiben erreichbar |
| Registrierung, Verifikation, Passwortreset | Keine neuen Fachaktionen; vorhandene Tokens nicht verbrauchen |
| Logout | Für alle weiterhin erreichbar |
| Theme, Icons, Fonts, erforderliche UI-Ressourcen | Für Wartungsseite und Login weiter ausliefern |
| Mailqueue, Bereinigungs-/Statusjobs | Zunächst weiter ausführen |

Normale Sitzungen werden durch Wartung nicht dauerhaft abgemeldet. Nach Abschalten
können sie bei weiterhin gültiger Sitzung und unveränderten Rechten wieder verwendet
werden. Für Remember-me-Rückkehr gelten dieselben Rollen- und Sitzungsgrenzen.

Für Eingriffe in Schema oder Datenbestand ist zusätzlich der normale Anwendungs-/
Worker-Shutdown nötig, weil dieser erste Wartungsmodus Hintergrundjobs weiterlaufen lässt.
Er begrenzt den Zugang zur Oberfläche und zu ihren Fachaktionen.

## Sichtbare Änderung

Eine schlichte Wartungscard verwendet vorhandene Cosmos-/Statusbausteine, verständliche
DE/EN-Texte und einen Weg zur administrativen Anmeldung. Admin/Superadmin sehen bei
aktiver Wartung einen dezenten Zustandshinweis. Globale Stile und Icons bleiben erhalten.

Ein normaler erster Dokumentrequest bekommt bei Wartung eine passende HTTP-503-Antwort.
`Retry-After` nur setzen, wenn ein Wiederfreigabezeitpunkt bekannt ist; der vorhandene
Boolean enthält keine Endzeit. Für Vaadin-RPC und WebSocket-Nachrichten eine eigene
verständliche Sperrbehandlung vorsehen, statt HTML in UIDL zu schreiben oder Loginloops
auszulösen. 503 beschreibt zeitweilige Überlastung bzw. Wartung:
[RFC 9110, Service Unavailable](https://www.rfc-editor.org/rfc/rfc9110.html#section-15.6.4).

## Technischer Umsetzungsweg

1. **`MaintenanceService` neu vorgeschlagen:** frischen Boolean lesen und zentral je
   Zugriffsart und Principal entscheiden. Effektive Adminrechte verwenden, damit
   Superadmin seine Ausnahme behält. Lesefehler als technische Störung behandeln,
   nicht still als `false` auslegen.
2. **`MaintenanceView` neu vorgeschlagen:** vorhandene Cardbausteine, DE/EN und Loginzugang.
   Direktnavigation und anonyme neue Sitzungen berücksichtigen. Serverseitigen
   Routeneinstieg schützen; bloß ausgeblendete Links sperren keinen Direktaufruf.
3. Im gemeinsamen Login nach Passwortprüfung, vor Context-/Cookie-/Sitzungsanlage
   normale Konten abweisen. Wartungsablehnung ist kein Passwortfehlversuch.
   Administrativer Login bleibt gegen Fehlversuche und zu viele Sitzungen geschützt.
4. Bereits offene UIs absichern: HTTP-UIDL und Nachrichten auf offenen Push-
   Verbindungen vor Fachcallbacks stoppen. Bestehende Interceptors gezielt ergänzen,
   einschließlich alter anonymer Registrierungs-/Resetformulare. Ein Routenlistener
   allein schützt deren alte Buttons nicht.
5. Zulassung an öffentlich erreichbaren Mutations-Einstiegen von Registrierung,
   Verifikation und Passwortreset ergänzen. Vor Tokenentwertung oder Queueanlage prüfen.
   Hintergrund-/Systemoperationen ausdrücklich getrennt behandeln, damit Scheduler
   nicht als anonyme Benutzer gesperrt werden.
6. Ausnahmen präzise eingrenzen: Eine pauschale Ausnahme für `/VAADIN/**` darf keine
   Fach-RPCs freigeben. Login, Wartungscard, ihre notwendige Kommunikation und Logout
   müssen funktionieren. Die H2-Konsole bekommt keine zusätzliche Freigabe.
7. Nach Config-Commit den nächsten Zugriff gegen den frischen Zustand prüfen.
   Push-Hinweise können offene Tabs schneller aktualisieren; Schutz muss auch ohne
   empfangene Meldung gelten. Abschalten wirkt ebenso ohne Neustart.
8. Konfigurationshilfe und Betriebshinweise um die exakten Ausnahmen ergänzen.
   Kontodaten und Rollen durch Umschalten nicht verändern.

Ein vor Umschalten zugelassener laufender Vorgang darf kontrolliert enden. Der
Schalter beendet keine laufende Datenbanktransaktion rückwirkend. Zulassung und
Änderungszeitpunkt müssen in Tests eindeutig festgelegt werden.

## Tokens und bestehende Links

Zeit läuft während Wartung weiter; keine automatische Verlängerung der Tokenlaufzeit.
Ein gültiger Link wird bei gesperrter Nutzung nicht verbraucht. Ist er nach Freigabe
abgelaufen, gilt der vorhandene Ablauf zum Anfordern eines neuen Links.
Bereits eingereihte Mails werden nicht umgeschrieben oder gelöscht. Bei Konto-/
Maillinks verständlich darauf hinweisen, ohne den Token sichtbar auszugeben.

## Tests und Abnahme

- **JUnit:** `false`/`true`, alle Rollen und anonym, Konfigurationswechsel, Lesefehler,
  erlaubte Systemoperation und abgewiesene Fachaktion.
- **Persistenz:** keine Konto-/Token-/Queueänderung durch Ablehnung; vorab zugelassenen
  laufenden Vorgang von einer erst nach Umschalten gestarteten Aktion unterscheiden.
- **HTTP/Push:** Direkt-URLs, altes anonymes Formular, alter User-Tab, offener WebSocket,
  Ressourcenausnahmen, Dokumentstatus und weiterhin funktionierender Logout.
- **Browser DE/EN:** Admin schaltet ein; User, Gast und anonymer Kontext landen in
  Wartung; normalen Login verweigern; Admin und Superadmin können wieder ausschalten.
- **Regression:** gültiger unverbrauchter Link nach Freigabe, abgelaufener Link,
  Remember-me ohne Umgehung und kein neuer Sitzungsplatz bei Ablehnung.

Fertig ist das Paket, wenn Direktaufrufe und alte Callbacks geschützt sind und die
administrative Wiederfreigabe praktisch nachgewiesen ist. Diese Tests stehen noch aus.

## Grundlage und Anschlussstellen

[ConfigEntry](../../../src/main/java/de/derpeterson/app/model/enums/ConfigEntry.java),
[SecurityConfig](../../../src/main/java/de/derpeterson/app/security/SecurityConfig.java),
[VaadinSecurityFilter](../../../src/main/java/de/derpeterson/app/security/VaadinSecurityFilter.java),
[PushSecurityInterceptor](../../../src/main/java/de/derpeterson/app/security/PushSecurityInterceptor.java),
[Push-/RPC-Regressionen](../../../src/test/java/de/derpeterson/app/service/PushRpcSecurityPersistenceTest.java).
Abstimmen mit [Anmeldeversuchen](login-attempts-limit.md) und [Sitzungslimit](max-sessions-per-user.md).
