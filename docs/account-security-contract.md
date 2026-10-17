# Eigene Passwort- und E-Mail-Änderungen

Stand: 17.10.2026. Implementiert im bestehenden Konto-/Cosmos-Design, DE und EN.

## Passwortwechsel

Die Zielidentität stammt ausschließlich aus der aktuellen Anmeldung. Der Server prüft
Pflichtfelder, dieselbe Passwortregel wie Registrierung/Reset und exakte Bestätigung.
Die erneute Prüfung des aktuellen Passworts verwendet den vorhandenen persistenten
Loginversuchsschutz mit `login.attempts.limit`, Fenster und Sperrzeit. Ein bereits
angemeldeter Benutzer umgeht diese Begrenzung bei Sicherheitsänderungen nicht.
Vor der Änderung wird der gespeicherte Account in einer frischen Transaktion gesperrt
und das aktuelle Passwort nochmals gegen den aktuellen Hash geprüft.

Neuer BCrypt-Hash, Ungültigmachen offener Passwortreset-/E-Mail-Änderungslinks,
Bestätigungsmail und Löschen persistenter Remember-me-Einträge werden gemeinsam
committet. Kann die Mail nicht in die Outbox aufgenommen werden, bleibt der alte
Zustand erhalten. Erst nach dem Commit werden andere bekannte Sitzungen widerrufen;
die aktuelle HTTP-/Vaadin-Sitzung mit allen ihren Tabs bleibt erhalten. Das Verhalten
wurde vom Benutzer ausdrücklich gewählt. Die aktuelle Sitzung behält keinen neuen
dauerhaften Remember-me-Zugang: „Angemeldet bleiben“ wird für das Konto widerrufen.

Die Profildraft-Version wird nur dann fortgeschrieben, wenn sie noch der Version vor
dem Passwortwechsel entspricht. Andernfalls bleibt die Konfliktprüfung beim späteren
Profilspeichern wirksam. Unbeteiligte Felder, Rollen und Kontofreigabe ändern sich nicht.

## Änderung der Anmeldeadresse

Die Anfrage verlangt neues E-Mail-Feld, Bestätigung und aktuelles Passwort. Der Server
normalisiert die Adresse zentral und prüft belegte bzw. identische Adressen. Eine Anfrage
ändert keine Anmeldeadresse. Eine Wiederholungsbegrenzung von einer Minute pro Konto
verhindert unmittelbare Mailserien; ein späterer neuer Antrag macht alte Anträge ungültig.
Dies ist keine zusätzliche globale Provider-/IP-Drosselung.

Der Link enthält 32 kryptografisch zufällige Bytes in Base64url. In `email_change_token`
steht nur dessen SHA-256-Hash, mit Accountbezug, vorheriger/neuer Adresse, Fingerabdruck
des Passwort-Hashes, Anlage/Ablauf und terminalem Tokenstatus. Anonyme Aufrufe können
keine eigene Zieladresse oder Account-ID unterschieben. Hash, Adresse und Kontofreigabe
werden beim Bestätigen unter Accountlock erneut geprüft.

Die bestehende Konfiguration `verificationToken.valid.duration` bestimmt die Gültigkeit;
die reale Enum-Zuordnung ist `VERIFICATION_TOKEN_VALID_DURATION`. Bei Gleichheit mit dem
Ablaufzeitpunkt ist der Link abgelaufen. Die Bereinigung verwendet entsprechend die
bestehende Verifikations-Aufbewahrungsdauer. Ungültige Konfiguration führt nicht zu
stillen Ersatzwerten oder zur Löschung beliebiger Tokens.

`/confirm-email-change/<token>` zeigt zunächst nur die Bestätigungsseite. GET, Vorschau,
Reload und ein möglicher Mailscanner verbrauchen den Token nicht. Erst der native
Vaadin-Buttonklick mit bestehendem CSRF-/Transportsschutz führt die Änderung aus.
Einmaliger Verbrauch, Adressänderung, Outboxeinträge an alte und neue Adresse und
Remember-me-Widerruf sind eine Transaktion. Die eindeutige Datenbankconstraint bleibt
bei konkurrierenden Bestätigungen die letzte Schranke. Andere Integritätsfehler werden
nicht pauschal als belegte Adresse verschluckt.

Ein veränderter Passwort-Hash, eine abweichende aktuelle Adresse oder eine aktuelle
Kontosperre verhindern die Bestätigung mit dem alten Link.
Wartung schützt Anfrage, Inspektion und Bestätigung gemäß bestehender Admin-/Superadmin-
Ausnahme. Eine wartungsbedingt abgewiesene Bestätigung verbraucht keinen Token.
Nach erfolgreicher Bestätigung werden alle bisherigen Sitzungen widerrufen; die nächste
Anmeldung verwendet die neue Adresse. Der Anmeldebutton wechselt direkt im Browser.
Ein GET des Login-Dokuments darf nach Bereinigung des alten Sicherheitskontexts weiterlaufen;
alte RPC-/Push- und Fachaufrufe bleiben gesperrt. Andere Tabs und Geräte aktualisieren sich nicht
still auf die neue Identität. Der Link ist nach Verwendung ungültig.

## Sicherheitsmails und Oberfläche

`password_changed`, `email_change_request` und `email_changed` existieren jeweils als
MJML und streng kompiliertes HTML auf Deutsch und Englisch. Layout, Inline-CID-Logo,
Copyright, Typografie und Farben stammen aus den vorhandenen Konto-Mails. Die feste
Kontosprache wird explizit gewählt; der Scheduler benötigt keine Vaadin-Sitzung.
Konfigurations-/Adressplatzhalter werden HTML-escaped. Passwörter und deren Hashes
stehen niemals in diesen Mails. Command-Objekte geben in `toString` keine Eingaben aus.

Sicherheitsmails verwenden einen gespeicherten, normalisierten Queue-Empfänger. Die
Bestätigung geht deshalb auch dann an die angeforderte neue Adresse, wenn sich der
Account später ändert. Bestehende Queueeinträge ohne Snapshot behalten ihren bisherigen
Empfängervertrag. Sicherheitsereignisse werden nicht als beliebige bereits offene
NOTIFICATION unterdrückt; sie nutzen den bestehenden NOTIFICATION-Typ ohne Enum-Migration.
Retries, unklare SMTP-Ausgänge und Administratorfehlerhinweise bleiben beim bestehenden
Queue-Vertrag. Auch die Queue-Diagnose nennt bei neuen Einträgen den festen Empfänger.

Infohinweise erklären den Sitzungswiderruf vor dem Absenden; die Sicherheitsmails
wiederholen ihn. Feldfehler erzeugen rote Markierungen und vorhandene ERROR-Notifications.
Erfolg erscheint erst nach abgeschlossenem Serviceaufruf. Die Meldung sagt „zum Versand
vorgemerkt“; tatsächliche SMTP-Zustellung ist kein Bestandteil der Datenbanktransaktion.
Dynamische Übersetzungssupplier erlauben den bestehenden Notification-Sprachwechsel.

## Datenbank, Betrieb und Prüfgrenzen

Neue Tabelle: `email_change_token`; zusätzliche nullable Spalte `email_queue.recipient`.
Für den neuen Stand Shared-Modul und Anwendung bauen und die Anwendung neu starten.
Die bestehende JPA-Schema-Konfiguration muss die additive Erweiterung anwenden dürfen;
bei extern verwalteten Schemata sind Tabelle/Spalte vor Inbetriebnahme entsprechend
zu migrieren. Es wurden keine Bestandsdatenbanken oder echten Postfächer angefasst.
Tests verwenden frische eigene H2-Datenbanken, echte Transaktionen, JDBC-Remember-me-
Einträge und einen gemockten SMTP-Sender. Sie sind kein Nachweis für die Migration einer
bestehenden Produktionsdatenbank oder die Darstellung in externen Mailclients.

Widerrufene Registry-Sitzungen werden vor HTTP-/Vaadin-RPC-/Push-Aktionen zurückgewiesen
und belegen sofort keinen neuen Sitzungslimit-Platz mehr. Die Registry bleibt wie beim
bestehenden Sitzungslimit an eine Anwendungsinstanz gebunden; clusterweiter aktiver
Sitzungswiderruf ist nicht implementiert. Persistente Remember-me-Einträge liegen in der
Datenbank. Die später geplante Geräteverwaltung bleibt unabhängig und derzeit ein Entwurf.

Browser-/Mailpit-Ende-zu-Ende-Prüfungen und reale SMTP-/Mailclient-Abnahme wurden für diese
Funktionen noch nicht ausgeführt. Browserläufe erfolgen nur nach ausdrücklicher Freigabe.
Die bestehenden sieben Account-Browserfälle belegen Profilspeichern und Dialogschließen,
keine erfolgreiche Passwort- oder E-Mail-Änderung. Die frühere Browser-/JaCoCo-Abdeckung
wird deshalb nicht als Nachweis der neuen Funktion weiterverwendet.


## Ausgeführte Prüfungen

Der vollständige JUnit-Lauf bestand 1.375 Fälle ohne Fehler oder Skips. Zuvor gefundene
Abweichung in der Null-Argument-Reihenfolge des Maildienstes wurde im Code korrigiert,
der bestehende Test unverändert erhalten. Anschließend wurden die letzte Login-
Übergangskorrektur sowie Konto, HTTP-/Push-Schutz und Sitzungszulassung mit 149 gezielten
Fällen erneut ohne Fehler oder Skips geprüft. Diese Fälle überschneiden sich mit dem
Gesamtlauf und werden nicht als zusätzliche 149 Gesamtfälle addiert.
Die sechs neuen MJML-Vorlagen wurden mit der vorhandenen Version 5.4.1 und strikter
Validierung nach HTML übersetzt. Google Java Format/Spotless und Diffkontrolle sind geprüft.
Es wurde keine Anwendung gestartet und kein Browser-/SMTP-Test durchgeführt.
