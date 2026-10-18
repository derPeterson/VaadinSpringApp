# Eigene Passwort- und E-Mail-Änderungen

Stand: 18.10.2026. Implementiert im bestehenden Konto-/Cosmos-Design, DE und EN.

Ergänzung: Erfolgreicher Passwortwechsel, Passwortreset und bestätigte E-Mail-Änderung
speichern jetzt auch eine [persönliche Mitteilung](user-notifications.md) in derselben
Fachtransaktion. Kann diese Pflichtmitteilung nicht gespeichert werden, rollt die
Fachaktion zurück. Bestehende Mail-/Sitzungsregeln bleiben erhalten; eine bloße
E-Mail-Änderungsanfrage erzeugt noch keinen Inbox-Erfolgseintrag.

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
Adressangaben in den Anfrage-/Änderungsmails sind ausdrückliche `mailto:`-Links
in Cosmos-Primary-Textfarbe `#a9c0fb` mit Unterstreichung. Auch der innere Text trägt
diese Inline-Styles, damit er beim Entfernen der äußeren Linkformatierung erkennbar
bleibt. Die Darstellung in konkreten Gmail-/GMX-Versionen bleibt eine Mailclient-Abnahme.
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
Datenbank. Die anschließende [Sitzungsverwaltung](session-management-contract.md) nutzt diesen Schutz und erweitert ihn um gezielte Abmeldeaktionen; ihr vollständiger Prüfstand ist dort getrennt dokumentiert.

## Ausgeführte Prüfungen

Die abschließende vollständige QA vom 17.10.2026 bestand **1.424 JUnit-Fälle**, 47
Browserhelfer-/Reporterprüfungen, 205 Python- und 14 JavaScript-Offlinetests sowie
Shared- und Produktionsbuild. Spotless Apply/Check und Ruff-Formatprüfung sind erfolgreich.
Alle Tests bestanden ohne Fehler oder Skips. Die sechs DE/EN-MJML-Vorlagen wurden
zuvor mit Version 5.4.1 und strikter Validierung nach HTML übersetzt.

Die vollständige Browser-Suite `all` bestand **je 136/136 Fälle DE/EN** am selben
gepinnten Produktionsimage mit je zwei isolierten Docker-/H2-/Mailpit-Workern.
ACCOUNT-SEC-01–04 prüfen Feldvalidierung, Passwortwechsel mit echter Bestätigungsmail,
Erhalt der aktuellen und Widerruf anderer Sitzungen sowie den vollständigen E-Mail-Wechsel:
Anfrage, tatsächliche Mail an die neue Adresse, nicht verbrauchender GET/Reload,
ausdrückliche Bestätigung, Sicherheitsmails an beide Adressen, Anmeldung mit neuer
Adresse und Ablehnung der Wiederverwendung. Mails werden über den echten lokalen
SMTP-Eingang gelesen, nicht durch Aufruf der Mailgenerierung vorbereitet.

Die sieben bestehenden Account-Fälle und die fünf neuen Sitzungsverwaltungsfälle
sind in beiden Gesamtrunden enthalten. JUnit ergänzt exakte Zeitgrenzen, Konkurrenz,
Rollback und HTTP-/RPC-/Push-Schutz; Browsererfolg allein belegt diese Regeln nicht vollständig.
[Gemessene Coverage und Grenzen](../browser-tests/abdeckung.md),
[dauerhafter Meilenstein](meilensteine/2026-10-17-sitzungsverwaltung-und-kontosicherheit.md).

Es wurden nur eigene isolierte Testanwendungen und abgefangene Testmails verwendet.
Keine echte Anwendungsdatenbank, reguläre Anwendung oder fremde Dockerumgebung wurde
verändert. Reale externe SMTP-Provider, Postfachzustellung, Mailclientdarstellung und
Migration einer produktiven Bestandsdatenbank sind weiterhin nicht nachgewiesen.
Die manuelle Nutzerabnahme vor Main steht aus.
