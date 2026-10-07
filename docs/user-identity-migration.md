# E-Mail-Identität und Benutzerlöschung (US-006/007)

> Anleitung, nicht ausgeführte Migration. Nur bei einer leeren Neuinstallation
> erzeugt Hibernate die Constraints/FKs aus den Mappings. `ddl-auto=update` ist
> kein Ersatz für die folgenden geprüften Bestandsänderungen. Die bestehende
> [Versionsmigration](user-version-migration.md) bleibt erforderlich.

## Vertrag

- Jede gespeicherte E-Mail ist `trim().toLowerCase(Locale.ROOT)`. Login,
  gesperrter Login-Lookup, Reset/Verifikation, Dublettenprüfung und Session-
  Authorities verwenden dieselbe Identität. Die Darstellung ist ebenfalls
  kanonisch; Groß-/Kleinschreibung des lokalen Teils unterscheidet keine Konten.
- `users.email` bleibt NOT NULL und UNIQUE. Zusätzlich verhindert
  `ck_users_canonical_email` (Kleinschreibung und keine Randzeichen bis U+0020) nichtkanonische
  direkte DB-Schreibzugriffe. Damit kann kein Schreiber den Unique-Constraint
  durch eine Großschreibungs-/Leerzeichenvariante umgehen. Die gezielte Prüfung
  gegen `email` ist nur ein Preflight; parallele Änderungen entscheiden die DB.
- Java `trim` entfernt Randzeichen bis U+0020; SQL `trim` entfernt gewöhnliche
  Leerzeichen. Deshalb prüft der H2-Check zusätzlich Länge und Zeichencode am
  Anfang/Ende, damit auch Tab-/Steuerzeichenvarianten kein zweites Konto erlauben.
  Java `Locale.ROOT` und DB `lower` sind für die hier verwendeten
  ASCII-Mailadressen gleich. Unicode-/IDN-Bestände müssen anhand der tatsächlichen
  Java-Kanonisierung geprüft werden; nicht blind SQL-lower als identisch annehmen.
  Keine transliterierten Adressen, Gmail-Punkt-/Plusregeln oder Providerregeln.
- Alle Benutzer-FKs von `verification_token`, `password_reset_token` und
  `email_queue` müssen ON DELETE CASCADE verwenden. Löschung entfernt sämtliche
  Tokenzustände und zugehörige Warteschlangeneinträge in derselben DB-Transaktion.
  Andere Benutzer, Rollen und deren Tokens bleiben erhalten. Ein Rollback
  stellt Benutzer und abhängige Zeilen gemeinsam wieder her.
- Keine JPA-REMOVE-Kaskade vom Token zum Benutzer oder zu Rollen. DB-Kaskaden
  können bereits geladene Kinder in einem PersistenceContext veraltet lassen;
  nach Benutzerlöschung solche Objekte nicht weiterverwenden. Nach einem
  fehlgeschlagenen Flush/Versionkonflikt die gesamte Transaktion zurückrollen.
  Keine Wiederverwendung eines gescheiterten PersistenceContexts.
- Zu löschende Benutzer werden wie bisher gegen den letzten aktiven Admin und
  optimistische Versionskonflikte geschützt. Bereits vom Mailworker an SMTP
  übergebene Nachrichten lassen sich nicht zurückrufen; es gibt keine neue
  Zustellungs-/Outboxgarantie.

## Offline-Vorgehen für bestehende H2-Datenbanken

1. Wartungsfenster: alle schreibenden Instanzen, Mailworker und Scheduler stoppen;
   vollständiges Backup erstellen und zunächst mit einer isolierten Kopie proben.
   Keine alten/neuen Instanzen parallel betreiben. Keine automatische Migration
   beim Start oder auf produktiven Daten durch diesen Auftrag.
2. Tatsächliche Spalten-/Constraintnamen in `INFORMATION_SCHEMA` prüfen; physische
   Namen können vom Naming-Strategy-/Schema-Stand abhängen. NULL/Leerwerte und
   ungültige Adressen identifizieren und fachlich bereinigen. Jede Adresse mit
   der Java-Kanonisierung prüfen, nicht allein mit DB-Collation vergleichen.
3. Potenzielle ASCII-Dubletten zunächst **nur lesen**:

   ```sql
   SELECT LOWER(TRIM(email)) AS identity, COUNT(*) AS accounts
   FROM users GROUP BY LOWER(TRIM(email)) HAVING COUNT(*) > 1;
   SELECT id, email FROM users WHERE email IS NULL OR TRIM(email) = '';
   ```

   Bei Treffern stoppen. Niemals automatisch Konten zusammenlegen/löschen oder
   Adminrollen/Passwörter/Tokens zwischen Identitäten übertragen. Verantwortliche
   müssen Konflikte einzeln entscheiden, danach erneut prüfen.
4. Für geprüfte ASCII-Bestände kann folgender Backfill verwendet werden. Bereits
   vorhandene Versionswerte nicht zurücksetzen; Bestandsänderungen erhöhen die
   Version. Für Unicode-Bestände stattdessen einen explizit geprüften Export/
   Import der Java-kanonisierten Werte vorbereiten.

   ```sql
   UPDATE users SET email = LOWER(TRIM(email)), version = version + 1
   WHERE email <> LOWER(TRIM(email));
   ALTER TABLE users ADD CONSTRAINT ck_users_canonical_email
     CHECK (email = LOWER(TRIM(email)) AND LENGTH(email) > 0
            AND ASCII(LEFT(email, 1)) > 32 AND ASCII(RIGHT(email, 1)) > 32);
   ```

   Bestehenden NOT-NULL-/UNIQUE-Constraint bestätigen (und nötigenfalls nach
   Dublettenprüfung wiederherstellen). Für verständliche Registrierungsfehler
   muss der E-Mail-Unique-Constraint ausdrücklich `uk_users_email` heißen.
   Den tatsächlichen bisherigen Unique-Constraint (nicht den Check/FK) auf der
   Offlinekopie ermitteln und unter H2 umbenennen, z. B.:

   ```sql
   ALTER TABLE users RENAME CONSTRAINT <bisheriger_email_unique_name> TO uk_users_email;
   ```

   H2 meldet bei Unique-Verletzungen den Constraint samt Backing-Index;
   maßgeblich ist der eindeutige Constraintname. Alternativ im Wartungsfenster
   den alten E-Mail-Unique-Constraint entfernen und einen gleichwertigen
   `ADD CONSTRAINT uk_users_email UNIQUE(email)` anlegen; danach Eindeutigkeit
   und Fehlerdiagnostik auf der Kopie prüfen. Keine parallelen Schreiber und
   keinen Deploymentbetrieb ohne Unique-Schutz. Ohne diese Anpassung werden
   unbekannte Constraintfehler bewusst nicht als E-Mail-Dublette ausgegeben.
   Ist der Check bereits vorhanden, nicht
   ungeprüft ein zweites Mal anlegen. SQL-Versionsbackfill setzt die bereits
   abgeschlossene Versionsmigration voraus.
   Der SQL-Backfill entfernt keine Rand-Tabs/Steuerzeichen. Solche Adressen
   vorher explizit mit dem Java-Vertrag bereinigen; nicht den Check abschwächen.
5. Tatsächliche Benutzer-FKs der drei Tabellen ermitteln, jeweils alten FK
   entfernen und durch eine gleichwertige CASCADE-Variante ersetzen. Platzhalter
   nicht wörtlich ausführen:

   ```sql
   ALTER TABLE verification_token DROP CONSTRAINT <alter_fk_name>;
   ALTER TABLE verification_token ADD CONSTRAINT fk_verification_user
     FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
   ALTER TABLE password_reset_token DROP CONSTRAINT <alter_fk_name>;
   ALTER TABLE password_reset_token ADD CONSTRAINT fk_reset_user
     FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
   ALTER TABLE email_queue DROP CONSTRAINT <alter_fk_name>;
   ALTER TABLE email_queue ADD CONSTRAINT fk_queue_user
     FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
   ```

   Andere FKs, insbesondere Rollen, nicht ändern. Keine verwaisten Kinder
   erzeugen. H2-DDL kann implizit committen: nicht auf ein transaktionales
   Rollback sämtlicher DDL-Schritte vertrauen; Backup-/Restoreplan vorbereiten.
6. Alte Sessions/UI-Formulare verwerfen und Remember-me-Tokens in der tatsächlich
   vorhandenen Tabelle `persistent_logins` im Wartungsfenster widerrufen, statt
   alte Username-Identitäten ungeprüft umzuordnen. Auch gespeicherte Warteschlangen-
   empfänger und externe Identitätsintegrationen prüfen. Erst dann neue Version
   starten. Geänderte Mailadressen müssen den Benutzern fachlich bekannt sein.
7. Auf der Testkopie Canonical-/Unique-Verstöße (auch parallele Updates), Login/
   Reset-Lookups, erfolgreiche und zurückgerollte Benutzerlöschungen mit allen
   Tokenzuständen/Warteschlangeneinträgen und Letzter-Admin-Schutz prüfen. Constraints
   anschließend erneut aus `INFORMATION_SCHEMA` verifizieren.

Die automatisierten Regressionen dieser Änderung prüfen nur frisch erzeugte,
isolierte H2-Schemata. Eine vorhandene Datenbank, Migration, SMTP oder Anwendung
wurde nicht gestartet. Andere DB-Systeme und deren Collation/DDL benötigen
eigene Prüfung; keine Übertragbarkeit der H2-SQL-Beispiele ohne Verifikation.
