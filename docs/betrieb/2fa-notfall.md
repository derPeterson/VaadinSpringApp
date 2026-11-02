# Betreiber-Notfallweg bei verlorenem Superadmin-Faktor

Diese Anleitung richtet sich an den Betreiber mit direktem Server- und
Datenbankzugriff. Sie ist keine Benutzer-Wiederherstellung und kein Mailtext.
Regulär gelten Authenticator und Ersatzcodes. Supportanfragen können keinen Faktor
zurücksetzen. Es gibt keinen versteckten Notfallbenutzer.

Das Projekt läuft lokal mit austauschbaren Entwicklungsdaten. Vor einem späteren
Livebetrieb müssen Sicherung, Datenerhalt, Identitätsprüfung und Verantwortlichkeiten
erneut vereinbart werden.

1. Anwendung und zugehörige Instanzen stoppen. Unabhängig vom offenen Ticket
   feststellen, welches konkrete eigene Superadminkonto repariert werden soll.
   Kontokennung, Aktivierung, bestätigte E-Mail und explizite Superadminrolle prüfen.
   Keine Reparatur anhand eines frei angegebenen Namens oder eines Tickettexts.
2. Eine Sicherung des aktuellen Datenbestands und des getrennten Verschlüsselungs-
   schlüssels anlegen. Die passende lokale Datenbank mit dem H2-Werkzeug öffnen.
   Keine laufende Anwendung oder Browserkonsole als Reparaturwerkzeug verwenden.
3. Innerhalb einer Transaktion für genau dieses Konto die Zugangsversion und
   Faktorversion erneuern. Verschlüsseltes Faktor-Geheimnis löschen, die Historie
   erhalten und `operator_enrollment=true` setzen. Fehlversuchssperren zurücksetzen.
   Ersatzcodes und offene MFA-Vorgänge des Kontos entfernen. Bestehende Sitzungen
   und gespeicherte Anmeldungen widerrufen. Die folgenden Anweisungen zeigen die
   Operationen mit gebundenen Parametern, kein ausführbares Skript mit Standardkonto:

   ```sql
   UPDATE users SET access_version = CAST(RANDOM_UUID() AS VARCHAR), version = version + 1
     WHERE id = :gepruefteBenutzerId;
   UPDATE two_factor_accounts SET encrypted_secret = NULL, revision = RANDOM_UUID(),
     previously_enrolled = TRUE, operator_enrollment = TRUE, accepted_step = -1,
     failed_attempts = 0, attempt_window = NULL, blocked_until = NULL, version = version + 1
     WHERE user_id = :gepruefteBenutzerId;
   DELETE FROM two_factor_recovery_codes WHERE user_id = :gepruefteBenutzerId;
   DELETE FROM two_factor_challenges WHERE user_id = :gepruefteBenutzerId;
   DELETE FROM persistent_logins WHERE username = :bestaetigteKontoadresse;
   DELETE FROM remembered_device WHERE username = :bestaetigteKontoadresse;
   DELETE FROM spring_session WHERE principal_name = :bestaetigteKontoadresse;
   ```

   Beide Updates müssen genau eine vorhandene Zeile betreffen. Andernfalls
   abbrechen und Kontozuordnung bzw. aktuellen Faktorstand prüfen. Kein pauschales
   Löschen aller Benutzer, Regeln oder Sitzungen. Falls ein Passwort ebenfalls
   verloren ist, nur einen mit dem aktuellen Anwendungsencoder erzeugten Hash
   setzen. Keine Klartextpasswörter oder fest eingebauten Standardpasswörter.
4. Transaktion prüfen und abschließen, Datenbankwerkzeug schließen und Anwendung
   mit dem unveränderten Verschlüsselungsschlüssel starten. Das reparierte Konto
   muss sich regulär mit seinem ersten Faktor anmelden. Der markierte Zustand
   erzwingt eine neue Authenticator-Einrichtung, auch wenn die Rollenregel aus ist.
   Bei einem externen Erstfaktor bleibt der vollständige kontogebundene OIDC-
   Durchlauf notwendig. Der Betreiberzustand ist keine direkte Portal-Freigabe.
5. Einrichtung mit erstem TOTP abschließen und neue Ersatzcodes sichern. Alte
   Sitzungen, Ersatzcodes und Freigaben dürfen danach nicht funktionieren. Erst
   die erfolgreiche Einrichtung entfernt den besonderen Wiederherstellungszustand.

Den Eingriff mit interner Kontokennung, Grund und Ergebnis außerhalb sensibler
Zugangsdaten dokumentieren. Niemals Schlüssel, Codes oder Passworthashes protokollieren.
