# Passwortreset-Verträge

## Lokale Zeit und Konfiguration

`PasswordResetService` verwendet wie `VerificationService` eine injizierbare
`Clock`. Der Spring-Standardkonstruktor verwendet `Clock.systemDefaultZone()`;
der vollständige Konstruktor nimmt eine explizite, nicht-null Clock entgegen.
Die Speicherung bleibt `LocalDateTime`: Die Zone der Clock bestimmt die lokale
Zeit. Es erfolgt keine Umstellung bestehender Zeitstempel auf UTC/Instant.

Ein ACTIVE-Token ist nur gültig, wenn `expiryDate > LocalDateTime.now(clock)`.
Bei Gleichheit ist er bereits abgelaufen und wird EXPIRED. Validierung und
Passwortänderung prüfen jeweils erneut nach dem Kontolock; das Rendern des
Formulars reserviert keinen gültigen Token.

`PASSWORD_RESET_TOKEN_VALID_DURATION` und `PASSWORD_RESET_TOKEN_LIVE_DURATION`
müssen parsebare, strikt positive ISO-8601-Dauern sein. Auch positive
Subsekundendauern sind zulässig. Null, leere/ungültige Werte, Null-/Negativdauern
und nicht darstellbare Zeitberechnungen schlagen fehl. Ablaufdatum und Dauer
werden vor Tokeninvalidierung berechnet; es gibt keine stillen Ersatzwerte.
Bereinigung löscht weiterhin ausschließlich Ablaufdaten strikt vor
`now(clock) - Aufbewahrungsdauer`, unabhängig vom Tokenstatus. Gleichheit bleibt
erhalten. Invalidität der Konfiguration darf keine Löschung auslösen.

## Anonyme Anfrage und Fehlerhinweise

Nach einer syntaktisch gültigen E-Mail-Eingabe zeigt `ForgotPasswordView`
dieselbe Bestätigung mit gleicher Darstellung und Dauer für fehlende,
deaktivierte und aktive Konten, vorhandene PENDING/IN_PROGRESS-Mails sowie
IOException und Runtimefehler. Sie verspricht keinen tatsächlich erfolgten
Versand. Der Text nennt einen möglichen Link und allgemeine Schritte, falls
keine Mail eintrifft. Technische Fehler werden mit Stacktrace ausschließlich
geloggt, nicht als kontobezogener Fehlerhinweis angezeigt.

Der boolesche Service-Rückgabewert bleibt ein interner Queue-Ausgang, kein
öffentlicher Kontonachweis. Servicefehler werden nicht verschluckt: Die
Transaktionsgrenze muss IOException und Runtimefehler weiterhin zurückrollen,
bevor die View die identische Bestätigung zeigt. Eingabevalidierung bleibt
erhalten; sie hängt nur von der Eingabe ab, nicht von Kontodaten.

Bei einem tokengebundenen Reset zeigt ein ungültiger/abgelaufener Token den
entsprechenden Hinweis. Technische Fehler bei Tokenvalidierung oder Reset
zeigen dagegen eine neutrale Meldung mit Bitte um späteren Versuch, ohne
Exceptiontext, SQL, Token oder Passwort. Bei fehlgeschlagener Validierung wird
kein Passwortformular angeboten. DE/EN-Texte kommen über typisierte
`MessageProperties`-Getter; sichtbare Notifications verwenden dynamische
Supplier des bestehenden `NotificationHelper`, Kartentexte den bestehenden
Sprachwechselmechanismus.

## Unveränderte Schutzmaßnahmen und Grenzen

Kontolocks, aktuelle Kontosperren, einmaliger Tokenverbrauch, irreversible
Terminalzustände, Open-Mail-Schutz und F8-Flush vor Refresh bleiben erhalten.
Ein Flush ist kein Commit; äußere Rollbacks und Optimistic-Locking-Konflikte
bleiben wirksam. Tokenrotation und Queueanlage rollen bei IOException gemeinsam
zurück. Streams und Classpath-Vorlagen behalten die Absicherungen aus Paket 2.

Die Antwortgleichheit ist kein Timing-Schutz, Rate-Limit oder CAPTCHA und
garantiert keine Gleichheit aller Netzwerk-/Infrastrukturfehler. Tests prüfen
serverseitige Komponenten und isoliertes H2; Browser, SMTP, Bestandsmigrationen,
andere Datenbanken/Isolationsmodi und Mehrinstanzbetrieb bleiben ungeprüft.
