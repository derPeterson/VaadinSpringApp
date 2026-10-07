# Verifikationsstatus und Konto-Sperren

## Vertrag

- `users.enabled` bleibt das wirksame Login-/Administrationsflag. Die bestehenden Rollen-, Versions-, Last-Admin-, Remember-me- und Sitzungsschutzregeln bleiben bestehen.
- Neu: `users.verification_pending BOOLEAN NOT NULL`. Neue, deaktivierte Registrierungen erhalten durch das Java-Modell `true`. Nur `enabled=false` **und** `verification_pending=true` erlauben Verifikation, Tokenanlage und Verifikations-Mailaufträge.
- Eine explizite `setEnabled(...)`-Entscheidung beendet die ausstehende Verifikation dauerhaft (`verification_pending=false`), auch bei einer Admin-Deaktivierung eines bisher nicht freigeschalteten Kontos. Der Admin-Editor übernimmt dieses Flag nicht aus dem Formular, sondern trifft die Entscheidung am gespeicherten Konto. Ein bereits aktiv gespeichertes Konto wird auch bei Anlage über den Builder als nicht mehr ausstehend gespeichert.
- Im Adminbereich neu angelegte Konten erhalten unabhängig vom Enabled-Schalter ausdrücklich `verification_pending=false`. Ein administrativ deaktiviert angelegtes Konto ist keine ausstehende Selbstregistrierung und darf sich nicht über einen Maillink freischalten.
- Daher bleiben nach **Adminfreigabe → Admin-Deaktivierung** alte unbenutzte Links ungültig, selbst wenn ihr Tokenstatus noch ACTIVE ist. Das Flag wird weder durch erneuten Mailversand noch durch einen alten/detached Benutzer-Snapshot zurückgesetzt. Eine spätere Freigabe erfolgt nur durch den Admin, nicht durch Selbstverifikation.
- Ein bereits USED-Link meldet bei weiterhin aktiviertem Konto idempotent `true`, ohne erneut zu aktivieren, Tokens zu ändern oder eine weitere Benutzer-Version zu erzeugen. Bei gesperrtem Konto meldet auch dieser Link `false`. Andere ungültige Links melden `false`; technische Datenbank-/Sperrfehler werden weiterhin geworfen, nicht pauschal abgefangen.
- Terminale Tokenstatus dürfen nicht reaktiviert oder in andere terminale Zustände umgeschrieben werden. `setTokenStatus` erlaubt nur ACTIVE → INACTIVE/USED/EXPIRED; Wiederholung desselben terminalen Zustands ist ein No-op. ACTIVE als Ziel ist unzulässig.

## Nebenläufigkeit und Transaktionen

Verifikationsabläufe und Queue-Produzenten sperren dieselbe Benutzerzeile mit PESSIMISTIC_WRITE bis zum Transaktionsende. Eine skalare ID-Abfrage mit FlushMode COMMIT erwirbt die Sperre, anschließend wird das gespeicherte Konto explizit refreshed. Damit entscheidet nicht ein stale Objekt aus dem Persistence Context oder Formular über die Berechtigung. Uncommittete Änderungen am übergebenen Objekt sind kein Kontoentscheid und werden nicht übernommen; solche Formularänderungen müssen über den vorhandenen versionierten Adminpfad gespeichert werden.

Adminänderungen behalten die Reihenfolge Rollen-/Last-Admin-Sperre → Benutzersperre und den erwarteten Formularversionsvergleich. Eine vor der Verifikation geöffnete Adminmaske kann nach konkurrierender Aktivierung weiterhin mit einem Versionskonflikt scheitern; sie muss neu geöffnet werden. Eine erfolgreich gespeicherte Deaktivierung wird niemals durch Verifikation rückgängig gemacht. Technische Sperr- und Versionskonflikte außerhalb des idempotenten Linkablaufs bleiben sichtbar.

Die drei `sendVerificationEmailBy*`-Methoden prüfen die offene Mailqueue unter der Kontosperre, rotieren erst danach und persistieren Token und Queueauftrag in derselben Transaktion. Eine bereits PENDING/IN_PROGRESS-Verifikationsmail führt zu Erfolg ohne Rotation. Eine eigenständige `createToken`-Anlage wird bei offener Verifikationsmail abgelehnt, damit deren Link nicht ungültig wird. Checked `IOException` und RuntimeException führen weiterhin zum Rollback.

Alle Queue-Produzenten müssen den Service verwenden. `addEmailToQueue` sperrt ebenfalls die Kontenzeile und dedupliziert nach Kontonummer/Mailtyp; bei Verifikationsmails prüft es den gespeicherten Registrierungsstatus. Verifikationslink-Erzeugung und Queueeintrag gehören in **einen** `sendVerificationEmailBy*`-Aufruf: Die generische Queue-API interpretiert keine beliebigen Bodytexte und kann keine vom Aufrufer außerhalb dieser Transaktion zusammengesetzten Links garantieren. SMTP-Verarbeitung und Scheduling werden durch diese Änderung nicht neu gestaltet.

Die Garantie „höchstens ein aktiver Token“ gilt für über den Service erzeugte Tokens, nicht für direkte SQL-Schreibzugriffe oder ungeprüfte Altbestände. UUID-Eindeutigkeit ersetzt keine Kontosperre. Es wird kein zusätzliches Token-/Queue-Schema eingeführt.

## Erforderliche Bestandsmigration – nur manuell, nicht automatisch ausgeführt

Vor Deployment: Backup, Wartungsfenster, **alle** alten Instanzen und Scheduler stoppen; die Umstellung nicht gemischt mit alten schreibenden Instanzen betreiben. Hibernate `ddl-auto=update` ist kein sicherer Ersatz für diese Datenentscheidung.

Für bestehende H2-Datenbanken nach Prüfung auf einer isolierten Kopie sinngemäß:

```sql
ALTER TABLE users ADD COLUMN verification_pending BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE users ALTER COLUMN verification_pending DROP DEFAULT;
```

**Fail-closed:** Alle bestehenden Konten erhalten zunächst `false`, einschließlich deaktivierter Konten. Aus `enabled=false` oder vorhandenen aktiven Tokens lässt sich eine frühere Adminsperre nicht zuverlässig rekonstruieren. Deshalb **keine** pauschale Rückbefüllung deaktivierter Konten auf `true`. Nur fachlich geprüfte, tatsächlich noch ausstehende Registrierungen dürfen kontrolliert explizit auf `true` gesetzt werden. Gesperrte und jemals administrativ freigegebene Konten bleiben `false`.

Vor Freigabe auditierte pending-Konten auf mehrere aktive Tokens sowie veraltete offene Queueeinträge prüfen und eine konsistente Bereinigung/erneute Mailanforderung planen. Bestehende Links und Jobs nicht ungeprüft weiterverwenden. Das aktive Flag, Rollen und bestehende Benutzer-Versionen nicht zurücksetzen. Für andere Datenbanken sind DDL und Lockverhalten gesondert zu prüfen.

Keine dieser Bestandsoperationen wurde im Rahmen des Auftrags ausgeführt. Die automatischen Regressionen verwenden ausschließlich frische, zufällig benannte H2-In-Memory-Datenbanken und echte Token-/Queue-Persistenz, ohne Anwendung, SMTP oder Scheduler zu starten.
