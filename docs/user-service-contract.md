# Öffentliche UserService-Verträge

Alle Regeln stammen aus dem bestehenden Formular-/Entityvertrag. Die reinen
Prüfroutinen liegen in `validation/UserInputRules`; `ValidationHelper` übernimmt
nur Komponenten/Notifications und delegiert seine bisherigen Prüfmethoden.

| Methode | Voraussetzungen / Verhalten |
| --- | --- |
| `save`, `saveUser` | Nicht-null Entity; Vor-/Nachname nicht blank, gültige Mail, Geschlecht, Geburtsdatum strikt vor heute, nicht-null Rollen mit nicht-null Enumname. Leere Rollen erlaubt. Bereits **kodiertes**, nicht-blank Passwort, nicht-null Locale/Status. Mail wird kanonisiert. Flush/letzter-Admin-Prüfung in derselben Transaktion. |
| `updateAdminUser` | Entity mit ID und beim Formularöffnen fixierter Version; dieselben editierbaren Feldregeln. Null/blank Rohpasswort lässt den Hash unverändert; sonst bestehende Passwortregel. Nicht editierbare Status-/Aktivitäts-/Locale-Felder bleiben erhalten. |
| `updatePassword` | Nicht-null Entity und Rohpasswort nach unveränderter Regel (trim-basiert: mindestens 8 Zeichen, Großbuchstabe, Sonderzeichen). Kodiert das unveränderte Rohpasswort; speichert **nicht** selbst. |
| `updateUserLocale` | Gültige Mail und nicht-null Locale, keine neue Whitelist. Fehlendes Konto: keine Änderung. |
| `updateUserStatus` | Entity mit ID, nicht-null Status; lädt gespeicherten Benutzer statt Snapshot zu mergen. Statusmeldung best-effort erst nach Commit. |
| `updateLastActivity` | Nicht-null ID; gezielte Aktivitätsänderung ohne Snapshotmerge. |
| `updateScheduledStatus` | Nicht-null ID/Cutoff, Ziel nur ABSENT/AVAILABLE; eigene Transaktion, Sperre und Recheck. Fehlende/ungeeignete Kandidaten: keine Änderung. |
| `deleteUser` | Entity mit ID; Versions-/letzter-Admin-Schutz. Benutzer, Tokens und Mailqueue atomar gelöscht (migrierte FKs vorausgesetzt). |
| `findByEmail`, `emailExistsForOtherUser` | Gemeinsame E-Mail-Kanonisierung. Exists schließt optional eigene ID aus; null/blank liefert false ohne Query. Nur Preflight, die DB entscheidet parallele Dubletten. |
| `findAllUsers` | Vollständige Benutzerabfrage; keine Eingabe. |
| `canDeleteUser` | Unverbindlicher Preflight; null/fehlende ID liefert false. |
| `wouldRemoveLastEnabledAdmin` | Nicht-null Rollen mit gültigen Elementen; null ID bedeutet Neuanlage. Preflight ersetzt keine Mutationsprüfung. |

## Exceptions und Transaktionen

- `IllegalArgumentException`: ungültige oben genannte Eingabe.
- `IllegalStateException`: letzter aktiver Admin würde entfernt, oder Benutzer
  für Status-/Aktivitätsupdate fehlt.
- `OptimisticLockingFailureException`: veralteter/gelöschter Adminsnapshot oder
  Versionskonflikt beim Speichern/Löschen.
- `DataIntegrityViolationException`: DB-Constraintverletzung, insbesondere
  E-Mail-Dublette. Auch andere Spring-Persistenz-/Sperrfehler und Encoderfehler
  werden weitergegeben. Nach Fehlern komplette Transaktion zurückrollen und
  fehlgeschlagenen PersistenceContext nicht weiterverwenden.

Die Registrierung revalidiert Formularwerte bei einer Service-Argumentexception.
Nur tatsächlich ungültige Felder erhalten Eingabehinweise; unerklärte Exceptions
werden nicht verschluckt. Die E-Mail-Unique-Verletzung wird ausschließlich am
benannten `uk_users_email` und SQLState 23505 erkannt. Andere Constraints, FK-/
Check-/Verbindungsfehler sind **keine** E-Mail-Eingabefehler. Der DB-Nachweis gilt
für H2; andere Dialekte müssen die Constraintdiagnostik gesondert prüfen.

`PasswordResetService.resetPassword` verlangt ebenfalls ein Rohpasswort nach
`UserInputRules`; ungültige Werte werfen vor Encoding/Tokenverbrauch eine
`IllegalArgumentException`. Sonstige Token-/Resetverträge bleiben unverändert.
