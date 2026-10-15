# Begrenzung erfolgloser Passwortanmeldungen

Stand: 15.10.2026. Die Implementierung verwendet die vorhandene Konfiguration
`login.attempts.limit` (`ConfigEntry.LOGIN_ATTEMPTS_LIMIT`, Enum-Standard 10).
Gespeicherte Werte bzw. `app.config.defaults.*` behalten ihre bisherige Rangfolge.

## Verhalten

- Ein Zählfenster dauert 15 Minuten je normalisierter Anmeldekennung. Bei N erlaubten
  Fehlern scheitern N Versuche regulär. Der N-te Fehler setzt eine 15-minütige Sperre;
  nachfolgende Passwortversuche werden ohne erneute Passwortprüfung abgewiesen.
- Weitere blockierte Versuche verlängern die Sperre nicht. Genau am gespeicherten
  Ablaufzeitpunkt ist eine neue Passwortprüfung erlaubt und beginnt mit frischem Zähler.
- Genau am Ende eines ungesperrten Zählfensters beginnt das nächste Fenster neu.
- Erfolgreicher Passwortnachweis setzt den Zähler zurück. Ungültige Zugangsdaten und
  abgelehnte Kontozustände zählen als erfolglose Passwortanmeldung. Die Behandlung
  von unbekannten und bestehenden Kennungen verrät dadurch keinen Kontozustand.
- Technische Authentifizierungs-/Speicherfehler zählen nicht als falsches Passwort
  und dürfen keine erfolgreiche Anmeldung oder gültige Remember-me-Cookies erzeugen.
- Alle vier Rollen unterliegen derselben Regel. `enabled`, Rollen und bestehende
  angemeldete Geräte werden durch die Versuchssperre nicht verändert.
- Remember-me verwendet seinen eigenen Spring-Provider und verbraucht keine
  Passwortversuche. Ein falscher Provider-Schlüssel bleibt abgewiesen.

Der Grenzwert muss mindestens 1 sein. 0 bedeutet nicht unbegrenzt. Änderungen werden
beim nächsten Versuch gelesen. Ein kleineres Limit kann einen noch laufenden Zähler
sofort sperren; ein erhöhtes Limit hebt eine bereits gesetzte Sperre nicht vorzeitig auf.
Bei ungültigem gespeichertem Wert erscheint eine neutrale technische Störungsmeldung.

## Anmeldewege und Anzeige

`SecurityConfig` stellt einen gemeinsamen `ProviderManager` bereit. Sein
`LoginAuthenticationService` schützt Passwortanmeldungen sowohl vom Vaadin-Button als
auch aus dem tatsächlichen HTTP-Formlogin. Ein direkter POST umgeht die Regel nicht.
Remember-me läuft separat über `RememberMeAuthenticationProvider`.

Das bestehende Formular und alle Styles bleiben erhalten. DE/EN unterscheiden
regulären Anmeldefehler, vorübergehende Drosselung und technische Nichtverfügbarkeit,
geben aber keine Adresse, Rolle, Existenzinformation oder internen Zähler aus.
Die Info am Konfigurationsfeld erläutert Fenster, Sperre und Wirkungszeitpunkt.
HTTP-Fehler führen über neutrale Codes zu denselben dynamisch übersetzten Meldungen;
erfolgreicher HTTP-Login führt Admin/Superadmin nach Admin, andere Konten nach Home.

Spring stellt bei Passwortanmeldung zusätzlich typisierte Faktornachweise aus.
Diese beschreiben die Authentifizierungsart und sind keine gespeicherten Rollen:
[DaoAuthenticationProvider](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/dao-authentication-provider.html).
Die laufende Sitzungsprüfung vergleicht deshalb Kontoberechtigungen getrennt von echten
`FactorGrantedAuthority`-Objekten. Der Nachweis bleibt im Kontext erhalten; die Dashboard-
Rollenanzeige zeigt ihn nicht als neue Rolle. Beliebige nachgeahmte `FACTOR_*`-Strings
werden ausdrücklich nicht ausgefiltert. Kontosperren und echte Rollenänderungen
invalidieren weiterhin alte HTTP-/Push-Sitzungen.

## Persistenz, Konkurrenz und Speichergrenze

`LoginAttemptEntity` liegt in `login_attempts`: SHA-256-Schlüssel der über
`EmailIdentity.canonicalize` normalisierten Kennung, Fensterbeginn, letzter aktiver
Versuch, Fehlerzahl und optionaler Sperrzeitpunkt. Zeitwerte sind `Instant` in UTC;
die fachliche Uhr ist in Tests injizierbar. Kein Passwort, Token oder Roh-Request
wird gespeichert. Ein ungesalzener Identitätshash ist keine zugesicherte Anonymisierung.

`login_attempt_guard` hält eine einzige Koordinationszeile. Bei Neuanlage einer
Kennung begrenzt ihr kurzer Datenbanklock die Gesamtzahl auf **10.000 Zähler**.
Bestehende Kennungen werden weiter bearbeitet; neue werden bei voller Kapazität mit
neutraler Nichtverfügbarkeit abgewiesen. Der Schutz wird niemals still ausgelassen.
Der Wert ist eine zentrale Implementierungsgrenze, kein zusätzlicher Adminschalter.

Prüfung, Passwortnachweis und Ergebnisfortschreibung sind je Kennung mit einem
pessimistischen Zeilenlock serialisiert. Es gibt keinen globalen Passwortprüfungslock.
Jede Passwortprüfung besitzt eine eigene `REQUIRES_NEW`-Transaktion; Fehlversuche
werden unabhängig von einem möglichen Aufruferrollback committet. Credentialfehler
werden erst nach erfolgreichem Zähler-Commit wieder geworfen. Technische Fehler
rollen die Verarbeitung zurück bzw. verweigern bei unklarem Commit die Anmeldung.

Die Passwortabfrage nutzt eine skalare Repositoryprojektion mit `MANDATORY` innerhalb
dieser bereits frischen Transaktion. Sie benötigt keine zweite Datenbankverbindung.
Die bestehende separate Abfrage zur Sitzungs-/Remember-me-Revalidierung bleibt erhalten;
ungeprüfte, noch nicht committete Privilegien eines Aufrufers werden nicht übernommen.

Bereinigung läuft stündlich, je Lauf höchstens 1.000 Kandidaten: `lastActivity` muss
strikt älter als 24 Stunden sein und eine Sperre bereits abgelaufen. Unter Lock wird
frisch geprüft. Genau auf der Altersgrenze bleibt ein Eintrag erhalten. Bei großem
Rückstand dauert vollständige Bereinigung mehrere Läufe. Eine zwischen Vorprüfung
und Lock gelöschte alte Zeile wird bei der Anmeldung einmal kontrolliert neu angelegt.

## Passwortreset

Eine angeforderte Mail oder bloß geöffnete Resetseite hebt die Sperre nicht auf.
Erst `PasswordResetService.resetPassword` nach gültigem, aktivem, unverbrauchtem Token
und gültigem neuen Passwort ruft die Zählerrücksetzung auf. Diese tritt mit
`Propagation.MANDATORY` derselben Transaktion bei. Rollback erhält alten Passwort-Hash,
Tokenzustand und Versuchssperre gemeinsam. Ungültige/abgelaufene/verbrauchte Tokens
setzen nichts zurück. Es wird dabei kein neuer Zähler für ein bisher ungezähltes Konto erzeugt.

## Einführung und Betrieb

Mit dem vorhandenen `spring.jpa.hibernate.ddl-auto=update` werden die beiden neuen
Tabellen und der Aktivitätsindex beim Start ergänzt. Bestehende Benutzer, Hashes,
Token- und Rollenzuweisungen werden nicht migriert oder umgeschrieben. Guardzeile
und Zähler entstehen bei Bedarf. Das Schema-Upgrade ist auf einer künstlich alten
H2-Struktur geprüft; ein Test mit einer Bestandsdatenbank wurde nicht durchgeführt.

Die Tabellen gehören zum normalen persistenten Datenbankbestand. Neustarts löschen
keine Sperren; mehrere Instanzen mit derselben Datenbank verwenden dieselben Locks.
Hostuhren müssen ausreichend synchron laufen. Bei deaktivierter automatischer
Schemaerstellung ist vor Aktivierung ein gesondertes Schema-Deployment erforderlich.
Ein Neustart der Anwendung in der IDE ist für diese Java-/Schemaänderung nötig;
danach wirken Änderungen des vorhandenen Versuchslimits ohne weiteren Neustart.

Temporäre Sperren können durch Dritte ausgelöst werden. Zusätzliche Begrenzung pro
IP/Ingress und weitere Schutzstufen sind nicht enthalten. Eine gefüllte Zählertabelle
kann Anmeldungen neuer Kennungen vorübergehend verhindern; Administration über eine
bestehende Sitzung bleibt möglich. Bei unklarem Zähler-Commit wird vorsorglich keine erfolgreiche Anmeldung bestätigt.

## Automatisierte Nachweise und Grenzen

Die Anwendungstests prüfen Fenster-/Sperrgrenzen, N=1, geänderte/ungültige Limits,
normalisierte/unbekannte Kennungen, alle Rollen, Erfolg/Fehler unter Konkurrenz,
begrenzte Neuanlage, Bereinigung, Rollbacks und den vollständigen Passwortreset.
Hinzu kommen künstliches Bestandsupgrade, neu erzeugte Persistenzkontexte, die echte
HTTP-Filterkette, Remember-me-Provider und ein echter Hikari-Pool mit genau einer Verbindung.

Die Browserfälle `AUTH-LIMIT-01` bis `AUTH-LIMIT-03` sind über
`browser-tests/run.ps1 -Suite login-attempts -Language de` bzw. `en` anwählbar.
Die Suite enthält zusätzlich die sechs bestehenden Authentifizierungsfälle und zwei
Remember-me-Fälle mit echtem Browserneustart, insgesamt elf Fälle. Die neuen Fälle
prüfen sichtbare Sperre, erhaltene Sitzung, dynamische Reduzierung, neutrale
Meldungen und Entsperrung über eine tatsächlich versandte Resetmail. Zeitgrenzen
werden kontrolliert in JUnit geprüft; der Browser wartet keine 15 Minuten ab.
Aktuelle tatsächlich ausgeführte Ergebnisse: [Browserabdeckung](../browser-tests/abdeckung.md).
