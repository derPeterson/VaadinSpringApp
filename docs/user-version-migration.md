# Benutzer-Versionierung (US-002)

Vor dem Deployment muss `users.version` für **alle bestehenden Benutzer** als
`BIGINT NOT NULL` vorliegen; Startwert 0. `@Version` wird ausschließlich von JPA
verwaltet. Neue Entities lassen die Java-Version zunächst null (JPA-Neuanlage).
Hibernate `ddl-auto=update` ersetzt keine geprüfte Bestandsmigration.

## Vorgehen für die bestehende H2-Datenbank

In einem geplanten Wartungsfenster alle schreibenden Instanzen stoppen, Backup
erstellen und die Migration zunächst auf einer Kopie testen. Beispiel für eine
Datenbank, in der die Spalte noch nicht existiert:

```sql
ALTER TABLE users ADD COLUMN version BIGINT DEFAULT 0;
UPDATE users SET version = 0 WHERE version IS NULL;
ALTER TABLE users ALTER COLUMN version SET NOT NULL;
```

Existiert die Spalte bereits, nur den Backfill/NOT-NULL-Schritt nach Prüfung
ihres Typs und ihrer Werte ausführen. Vorhandene nicht-null Versionen **nicht**
zurücksetzen. Keine gemischten alten/neuen Anwendungsversionen betreiben: Alte
Instanzen erhöhen die Version nicht. Alte Sessions/Formulare nach Deployment
verwerfen; niemals eine fehlende Formularversion als aktuelle Version ergänzen.
Diese Befehle wurden nicht auf einer produktiven Datenbank ausgeführt.

## Häufige Änderungen und Konflikte

- Status/Manual-Flag und Profil-/Sicherheitsänderungen werden versioniert.
  Ein unveränderter Status samt unverändertem Flag löst kein Update aus.
- Reine Aktivitätsänderungen sind mit Hibernate `@OptimisticLock(excluded=true)`
  von der Versionserhöhung ausgenommen; `@DynamicUpdate` schreibt nur tatsächlich
  geänderte Spalten. UI-Aktivität wird per ID frisch geladen und geändert, nicht
  als vollständiger Snapshot gemergt.
- Admin-Formulare halten die Version vom Öffnen fest und übertragen nur ihre
  editierbaren Felder. Status, Locale, Aktivität und ein nicht explizit neu
  vergebenes Passwort werden nicht aus dem Formular übernommen.
- Ein echter Status-/Profilkonflikt kann das Formular ungültig machen. Die UI
  fordert zum Neuladen und Prüfen auf. Kein automatischer Retry mit alten Daten.
  Versionierung pro Benutzer behebt **nicht** US-001 (Cross-Row-Admin-Invariante).

Statusnachrichten werden synchron nach erfolgreichem Commit zugestellt.
Mehrere Statuswechsel desselben Benutzers in einer Transaktion werden zum
Netto-Wechsel zusammengefasst; Rückkehr zum ursprünglichen Status sendet nichts.
Listenerfehler werden einzeln protokolliert, aber nicht erneut zugestellt.
Ohne Outbox sind Crash zwischen Commit und Zustellung, prozessübergreifende
Zustellung und globale Reihenfolge konkurrierender Commits nicht abgesichert.
