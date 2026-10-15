# Umsetzungspläne für bisher wirkungslose Konfiguration

Stand: 15.10.2026. Geprüfter Ausgangscommit: `1d57a5e`.
Status: **Planung; diese Dokumente implementieren oder aktivieren keine Funktion.**

Die fünf Einzelpläne beschreiben Iststand, empfohlenen ersten Umfang, sichtbare
Verhaltensänderungen, technische Schritte, Einführung und Abnahmetests. Empfehlungen
sind Vorschläge für die nächste Umsetzung, keine bereits abgestimmten Produktregeln.
Als neu vorgeschlagen bezeichnete Klassen existieren noch nicht.

## Bestandsaufnahme

Alle **29 Werte** aus `ConfigEntry` wurden mit Produktionsverbrauchern abgeglichen.
Anzeige, Übersetzung, Validierung und Speicherung allein gelten dabei nicht als
Umsetzung einer Fachfunktion. Dies ist eine Quellcodeprüfung; Bestandsdatenbanken,
Zugangsdaten und laufende Anwendungen wurden nicht untersucht.

| Eintrag | Enum-Standard | Heutige Wirkung | Plan |
| --- | --- | --- | --- |
| `login.attempts.limit` | `10` | Anzeige/Speicherung; keine Drosselung | [Anmeldeversuche](login-attempts-limit.md) |
| `max.sessions.per.user` | `3` | Anzeige/Speicherung; kein Sitzungskontingent | [Gleichzeitige Sitzungen](max-sessions-per-user.md) |
| `maintenance.mode` | `false` | Anzeige/Speicherung; keine Zugangsbeschränkung | [Wartungsmodus](maintenance-mode.md) |
| `mail.smtp.socketFactory.class` | `javax.net.ssl.SSLSocketFactory` | `MailConfig` reicht den Wert nicht weiter | [SMTP-SocketFactory](mail-smtp-socketfactory-class.md) |
| `email.queue.pool.size` | `5` | Executor konfiguriert; Mailqueue verwendet ihn nicht | [Mail-Workerpool](email-queue-pool-size.md) |

Das sind Enum-Standards, keine ausgelesenen Installationseinstellungen. Gespeicherte
Werte haben Vorrang; fehlende Werte können aus `app.config.defaults.*` kommen.

### Bereits verwendete Werte

| Konfiguration | Nachgewiesener Verbraucher / Wirkung |
| --- | --- |
| `service.name`, `copyright.text` | Konto-/App-Komponenten und neu erzeugte Mails |
| `base.url` | Links neuer Verifikations-/Passwortresetmails |
| `rememberMe.duration`, `rememberMe.secret.key` | Aufbau des Remember-me-Dienstes in `SecurityConfig` |
| `user.auto.absent.timeout` | `UserStatusScheduler` |
| `email.from` | Absender in `EmailService` |
| `email.admin` | Tatsächlicher Adminversand nach terminalem Queuefehler |
| `verificationToken.valid.duration`, `verificationToken.live.duration` | Verifikation und Tokenbereinigung |
| `passwordResetToken.valid.duration`, `passwordResetToken.live.duration` | Passwortreset und Tokenbereinigung |
| `mail.host`, `mail.port`, `mail.username`, `mail.password` | Aufbau des SMTP-Senders |
| `mail.smtp.auth`, `mail.smtp.starttls.enable`, `mail.smtp.ssl.enable`, `mail.smtp.ssl.trust`, `mail.debug` | SMTP-Properties in `MailConfig` |
| `email.queue.capacity` | Höchstzahl fälliger Kandidaten pro Lauf; zusätzlich Pufferwert des ungenutzten Executors |
| `email.queue.max_retry` | Wiederholungen nach nachgewiesenem Nichtversand |
| `email.queue.sent.live.duration` | Bereinigung versandter Einträge nach Erstellungsdatum |

`email.admin` ist bereits angebunden. `email.queue.capacity` ist ebenfalls wirksam;
nur seine zweite Bedeutung als Executor-Puffer gehört zum offenen Workerpool.
Für diese aktiven Einträge gibt es keinen Neuimplementierungsplan. Der Workerplan
beschreibt, wie die wirksame Kapazitätsgrenze erhalten werden soll.

## Empfohlene Reihenfolge

1. **Anmeldeversuche:** gemeinsamen serverseitigen Anmelde-Einstieg schaffen.
2. **Gleichzeitige Sitzungen:** dieselben Anmeldewege einschließlich Remember-me einbinden.
3. **Wartungsmodus:** auf den geklärten Login-/Sitzungsvertrag aufsetzen; alte UIs und Push beachten.
4. **Mail-Workerpool:** eigenes Paket mit belastbaren Konkurrenztests.
5. **SocketFactory:** Spezialfall; Bedarf und genaue Bedeutung des bisherigen Felds zuerst abstimmen.

Jeder Plan ist ein eigenes Arbeitspaket. Gemeinsame Anmeldelogik schrittweise
verbessern, ohne nebenbei sämtliche Sicherheitsklassen umzubauen.

## Gemeinsame Leitplanken

- Superadmin behält sämtliche Adminrechte. Benutzer und Gast erhalten die gleichen
  hier vorgeschlagenen Grenzen; keine versteckten Rollenausnahmen.
- Bedeutung, Eingaberegel und Aktivierungszeit jedes neuen Verbrauchers dokumentieren.
  Die Einzelpläne unterscheiden sofortige Wirkung von startgebundenen Werten.
- DE/EN-Texte über den gemeinsamen `MessageTexts`-Katalog und `MessageProperties` führen.
- Bestehende Cosmos-Komponenten und den [Designstandard](../../design-standard.md)
  verwenden. Gemeinsame Styles und bestehende Icons nicht beiläufig ändern.
- Benötigte Schemaerweiterungen vor Verwendung der Funktion liefern und auf künstlichen
  alten H2-Strukturen testen. Bestehende Konten nicht still umstufen.
- JUnit prüft Fachregeln, Zeitgrenzen und Konkurrenz. Browserfälle prüfen echte
  Klickwege in isoliertem Docker/H2/Mailpit. Resultate erst nach Durchführung dokumentieren.
- Keine dauerhafte Abhängigkeit von lokalen Test-Run-Verzeichnissen schaffen.
- Konkrete Framework-APIs vor Umsetzung gegen die dann aufgelösten Projektversionen
  prüfen. Verlinkte Primärquellen belegen Konzepte, keine bereits getestete Verdrahtung.

## Prüfung dieser Unterlagen

Die Aufgabe ändert ausschließlich Markdown. Keine Builds, JUnit- oder Browserläufe
wurden hierfür gestartet. Geprüft werden Quellbezüge, interne Links, Zuordnung aller
29 Einträge und der Diff. Zum Lesen ist kein Anwendungsneustart oder Refresh nötig.
