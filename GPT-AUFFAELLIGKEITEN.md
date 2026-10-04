# UserService: Auffälligkeiten für spätere Verbesserungen

Dieses Backlog basiert auf Codeanalyse und isolierten Unit-Tests. Es enthält keine
Produktionsänderungen; Datenbank-, Sicherheits- und Nebenläufigkeitsverhalten wurden
nicht durch einen laufenden Spring-Kontext geprüft.

Java-Pfade relativ zu `src/main/java/de/derpeterson/app/`.

## 1. Statusereignis kann dem tatsächlichen Status widersprechen — hoch
- **Betroffen:** `service/UserService.java`, `updateUserStatus`; `model/UserEntity.java`, `setAutomaticStatus`.
- **Befund:** Ein automatisch angeforderter Wechsel kann bei manuell gesetztem
  `EMPLOYED` oder `OFFLINE` abgelehnt werden. Der Service speichert den unveränderten
  Benutzer, sendet aber trotzdem `newStatus.name()` als neuen Status.
- **Risiko:** UI-Empfänger können einen anderen Status anzeigen als die Datenbank.
- **Später:** Nach dem Entity-Aufruf den tatsächlichen Status für das Ereignis
  verwenden und klären, ob bei unverändertem Status überhaupt gesendet werden soll.
  Der aktuelle Widerspruch ist explizit als Charakterisierung getestet, nicht als Soll-Verhalten.

## 2. Letzter-Admin-Schutz liegt außerhalb der Mutation — hoch
- **Betroffen:** `UserService.canDeleteUser`, `wouldRemoveLastEnabledAdmin`, `deleteUser`,
  `saveUser`/`save`; Aufrufer `views/admin/AdminUserManagementSection.java`.
- **Befund:** Prüfen und Ändern sind getrennte Aufrufe. `deleteUser` prüft selbst
  keine Adminbedingung. In der UI liegt die Löschprüfung sogar vor dem Bestätigungsdialog.
- **Risiko:** Ein anderer Aufrufer kann die Prüfung umgehen; parallele Änderungen
  könnten beide dieselbe positive Prüfung sehen und alle aktiven Admins entfernen.
- **Später:** Die Invariante in einem transaktionalen Anwendungsfall durchsetzen,
  mit geeignetem Locking/Isolation und anschließenden Nebenläufigkeitstests.

## 3. E-Mail-Normalisierung und Eindeutigkeit sind nicht zentral — mittel bis hoch
- **Betroffen:** `emailExistsForOtherUser`, `findByEmail`, `saveUser`/`save`,
  `repository/UserRepository.java`, `model/UserEntity.java`.
- **Befund:** Die Duplikatprüfung trimmt und ignoriert Groß-/Kleinschreibung.
  Suchen/Speichern normalisieren nicht. Die Prüfung ist von der Speicherung getrennt;
  ein `unique`-Attribut allein beweist keine passende case-insensitive Datenbankregel.
- **Risiko:** Unterschiedliche Identitätsregeln und Rennen zwischen Prüfung und
  Speicherung; die tatsächliche Datenbank-Kollation bleibt zu prüfen.
- **Später:** Eine einheitliche Normalisierung festlegen, passende DB-Eindeutigkeit
  sichern und Konflikte beim Speichern fachlich behandeln.

## 4. Vollständige Benutzerlisten für Einzelprüfungen — mittel
- **Betroffen:** `emailExistsForOtherUser`, `canDeleteUser`, `wouldRemoveLastEnabledAdmin`.
- **Befund:** Jede Prüfung liest über `findAll()` alle Benutzer und filtert in Java.
  Die Rollenprüfung kann zusätzlich Lazy-Loading-Zugriffe auslösen.
- **Risiko:** Aufwand steigt mit der Nutzerzahl; mögliche zusätzliche DB-Abfragen.
- **Später:** Gezielte Repository-Existenz-/Zählabfragen verwenden und anhand echter
  SQL-Abfragen prüfen. Die Unit-Tests messen keine Datenbankperformance.

## 5. Null-/Sprachwerte sind nicht gegen den Persistenzvertrag geprüft — mittel
- **Betroffen:** `updateUserLocale`; `UserEntity.preferredLocale` (`nullable=false`).
- **Befund:** Der Service setzt auch `null` oder `Locale.ROOT`. Der bisherige
  Null-Test und der überarbeitete Test charakterisieren dies. Die UI unterstützt
  Deutsch/Englisch, der Service nimmt beliebige Locales an.
- **Risiko:** Null kann erst beim DB-Schreiben scheitern; nicht unterstützte Sprachen
  können an anderer Stelle unerwartete Fallbacks erzeugen.
- **Später:** Erlaubte Sprachwerte und Null-Verhalten definieren, früh validieren und
  einen Persistenztest ergänzen; nicht allein aufgrund des Mocks DB-Kompatibilität behaupten.

## 6. Passwortänderung hat einen uneindeutigen Verantwortungsumfang — mittel
- **Betroffen:** `updatePassword`; Aufrufer `AdminUserManagementSection`.
- **Befund:** Die Methode encodiert und ändert nur das übergebene Entity. Sie speichert
  nicht und prüft keine Passwortregel. Auch leere Eingaben gelangen zum Encoder;
  ein Null-Benutzer führt zu einer `NullPointerException`.
- **Risiko:** Neue Aufrufer könnten eine persistierte, validierte Änderung erwarten.
- **Später:** Den Vertrag dokumentieren oder einen klaren transaktionalen
  Passwort-Anwendungsfall mit Validierung und definierter Fehlerbehandlung anbieten.
  Die aktuellen Tests erwarten ausdrücklich keinen Repository-Aufruf.

## 7. Speichern und Benachrichtigen sind nicht atomar — mittel
- **Betroffen:** `updateUserStatus`, `websocket/UserStatusBroadcaster.java`.
- **Befund:** Erst wird gespeichert, dann synchron gesendet. Ein Listenerfehler wird
  weitergegeben, obwohl `save` bereits aufgerufen wurde. Bei Save-Fehlern ist das
  übergebene Entity bereits verändert. Keine Service-Transaktion umfasst den Statusablauf.
- **Risiko:** Aufrufer erhalten einen Fehler trotz möglicher Persistierung; einzelne
  Listener können weitere Benachrichtigungen verhindern. DB-Rollback und In-Memory-
  Zustand sind unterschiedliche Garantien.
- **Später:** Transaktionsgrenze und After-Commit-Ereignisse prüfen, Listenerfehler
  isolieren und das gewünschte Wiederholungs-/Fehlerverhalten definieren.

## 8. Zwei gleichartige Speichermethoden — niedrig
- **Betroffen:** `saveUser` und `save`.
- **Befund:** Beide delegieren unverändert an `userRepository.save`.
- **Risiko:** Unklare Wahl für Aufrufer; zukünftige Regeln könnten nur in einem Alias landen.
- **Später:** Verantwortungen dokumentieren oder die API gezielt vereinheitlichen.
  Beide öffentlichen Methoden bleiben unverändert und werden geprüft.
