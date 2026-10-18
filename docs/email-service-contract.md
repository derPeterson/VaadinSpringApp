# Mailversand-Verträge

Standabgleich 15.10.2026: Die beschriebenen Service-/Migrationsverträge bleiben vom
Redesign unverändert. Aktuelle automatisierte Nachweise und Grenzen stehen in der
[Abdeckungsübersicht](../browser-tests/abdeckung.md). Bestandsmigrationen werden
durch Tests in frischen H2-Datenbanken nicht nachgewiesen.

## Kodierung und Nachrichtentyp

`MailConfig.javaMailSender()` erzeugt den tatsächlich injizierten
`JavaMailSenderImpl` und setzt dessen `defaultEncoding` explizit auf UTF-8.
Host, Port, Anmeldedaten und SMTP-Properties behalten ihre bisherigen Quellen.
Dies erfolgt nicht über eine zusätzliche, möglicherweise ungenutzte Boot-Property.

`EmailService.sendEmail` erzeugt UTF-8-HTML. Ohne die bekannte Logo-CID bleibt
es bei genau einem `text/html; charset=UTF-8`-Teil. Verifikations-/Resetvorlagen
referenzieren `cid:service-logo@vaadinspringapp`; dafür erzeugt der Helper
`multipart/related` mit HTML und einem Inline-PNG-Teil (`Content-ID` passend zur CID,
Dateiname `service-logo.png`, Content-Type `image/png`). `setText` erfolgt vor
`addInline`. Es gibt keine beliebigen Benutzeranhänge, externen Bildabrufe oder
einen alternativen Plain-Text-Inhalt.

`EmailLogo` verwaltet CID/Ressourcenpfad. Das PNG wird aus dem Classpath gelesen,
nicht aus einem lokalen Quelldateipfad. Die Produzenten prüfen die Ressource weiterhin
vor Queue-Anlage, damit ein `IOException` die bestehende Tokenrotation zurückrollen
kann. Der Versanddienst liest die Bytes vollständig vor dem Transport; eine
`ByteArrayResource` liefert für spätere MIME-Lesezugriffe jeweils einen neuen Stream.

Die MJML-Quellen und erzeugten HTML-Dateien verwenden dieselbe Cosmos-Palette mit
statischen CSS-Verläufen und festen Grundfarben als Rückfall. Keine Animation,
externen Webfonts oder öffentlich erreichbaren Logo-URLs sind erforderlich.
`{{COPYRIGHT_TEXT}}` wird bei Queue-Anlage aus der DB-Konfiguration aufgelöst:
gemeinsamer `CopyrightTextFormatter`, `{year}` aus der injizierten Clock,
`{serviceName}` aus der Konfiguration, DE-/EN-Standard über den vorhandenen Getter.
Eigene DB-Texte bleiben erhalten und werden für HTML escaped. Bereits gespeicherte
Queue-Inhalte werden nicht umgeschrieben.

`sendAdminEmail` erzeugt weiterhin `SimpleMailMessage`, also Plain Text.
HTML-Markup bleibt Text. Beim realen Spring-Sender läuft die Konvertierung über
`MimeMailMessage(createMimeMessage())`; die vom Sender erzeugte Nachricht trägt
das UTF-8-Default, das der Helper erkennt. Der Dienst ersetzt nicht den Sender
und führt keine eigene Konvertierung oder zusätzliche Versandversuche ein.

## Bestehende Eingaben

| Eingabe | HTML-Methode | Admin-Methode an der Sendergrenze |
|---|---|---|
| User | Nicht-null; null führt zu `NullPointerException` | Kein User-Argument |
| Empfänger/Absender | Nicht-null; genau eine parsebare Adresse, optional Anzeigename | Strings unverändert weitergereicht |
| null-Adresse | `IllegalArgumentException` | Unverändert im `SimpleMailMessage` |
| Leere/ungültige/mehrfache Adresse | `MessagingException` bei Helper-Parsing | Keine eigene Prüfung; realer Adapter kann `MailParseException` liefern |
| Lokale Adresse ohne Domain | Weiterhin akzeptiert; keine strengere Adressvalidierung aktiviert | Keine eigene Domainprüfung |
| Betreff/Inhalt null | `IllegalArgumentException` | Unverändert weitergereicht; der Adapter entscheidet über Verarbeitung |
| Betreff/Inhalt leer | Erhalten | Erhalten |
| Inhalt mit HTML | Als HTML gesetzt; keine HTML-Syntaxprüfung | Als Plain Text gesetzt |

Der konfigurierte `EMAIL_FROM` wird pro Aufruf neu gelesen. Der Dienst trimmt
nicht, ergänzt keine Pflichtfelder und prüft keine Zustellbarkeit. Die Tabelle
beschreibt den bestehenden Vertrag, keine neuen Validierungsregeln. Dass der
Mock-Sender einen Randfall annimmt, bedeutet nicht, dass SMTP ihn akzeptiert.

## Fehler und Zuständigkeiten

MIME-Aufbaufehler der HTML-Methode (`MessagingException`) werden weitergegeben.
Parse-, Authentifizierungs- und Versandfehler bleiben ohne Fallback oder eigenen
Retry. Die vorhandene gezielte Ausnahme bleibt: Ein vom
`ConnectionAwareJavaMailSender` für genau diese Nachricht nachgewiesener Fehler
vor Versand wird als `MailPreparationException` mit Originalursache weitergegeben.
Unmarkierte `MailSendException` und unklare Versand-/Close-Ausgänge bleiben unverändert.
Ein `IOException` beim Lesen des Inline-Logos wird ebenfalls vor dem Transport als
`MailPreparationException` weitergegeben. Konfigurations- und andere Runtimefehler
propagieren unverändert. Nach einem Aufbau-/Konfigurations-/Logolesefehler wird der
Sender nicht aufgerufen.

Der Spring-Admin-Adapter kapselt Adress-Parsingfehler in `MailParseException`;
die Methode führt davor keine zusätzliche Prüfung durch. Retries gehören
weiterhin zum Aufrufer, insbesondere `EmailQueueService`, nicht zum Versanddienst.
Die Admin-Oberfläche behält ihre bisherige Eingabevalidierung und Fehleranzeige.
Transaktionen, Queue-Status, Retrygrenzen und Empfängerauswahl werden nicht geändert.

## Prüfgrenzen

Tests prüfen echte Nachrichten mit Byte-Roundtrip und den realen
`MimeMailMessage`-Adapter auf einer vom tatsächlichen Bean-Factory-Sender
erzeugten Nachricht. Die Adapterkonvertierung wird ohne `send` auf dem realen
Sender, ohne `testConnection` und ohne Transport ausgeführt. Konfiguration ist
vollständig gemockt; keine Bestandsdaten oder echten Anmeldedaten erforderlich.
SMTP-Zustellung, Darstellung in Mailclients und Anwendungslaufzeit bleiben ungeprüft.

Der CID-/Cosmos-/Copyright-Feinschliff vom 13.10.2026 ist abschließend geprüft:
1055 Anwendungstests und 26 Browser-Helper-/Reportertests sowie je 84 Browserfälle
auf DE/EN erfolgreich; frisch gebautes Docker-Produktions-JAR, eigene Umgebungen
abgebaut. MIME-Roundtrips prüfen Content-ID, Inline-PNG-Bytes und UTF-8; ein
Logo-Lesefehler stoppt nachweislich vor Transport. Unmarkierte Versandfehler bleiben
unklar und werden vom Maildienst nicht wiederholt. Die Service-/Persistenzregressionen
erhalten Tokenrotation, Rollback und Kontosperren. Browserjourneys prüfen geladene
Mailbilder und den über die Oberfläche gespeicherten sicheren DB-Fußtext in beiden
Mailtypen. MJML wurde erfolgreich mit strikter Validierung in HTML übersetzt.
Gmail/Outlook-Darstellung und reale Zustellung bleiben separat ungeprüft.
Berichte: [Browserabdeckung](../browser-tests/abdeckung.md).


## Adressfeste Konto-Sicherheitsmails (17.10.2026)

`sendEmailTo(recipient, subject, html)` verwendet denselben MIME-/CID-/Transportablauf
mit explizit gespeichertem Empfänger. Der vorhandene `sendEmail(user, ...)`-Vertrag
bleibt einschließlich MIME-Erstellung und Validierungsreihenfolge erhalten.
Neue Konto-Sicherheitsmails werden als NOTIFICATION mit `email_queue.recipient`
gespeichert. Null bei älteren Einträgen erhält die bisherige Empfängerauswahl.
Siehe [Kontosicherheitsvertrag](account-security-contract.md) für Vorlagen, Transaktionen,
Sitzungsfolgen und die weiterhin offene tatsächliche Browser-/SMTP-Abnahme.

## Manueller Mail-Test im Wartungsmodus (19.10.2026)

Die SMTP-Kennung im Mail-Test zeigt denselben Statuspunkt wie der interne
Benachrichtigungsversand: grün bei offenem Betrieb, rot bei Wartung oder nicht
lesbarer Wartungskonfiguration. Das gesamte Mail-Testformular einschließlich
Info- und Versandbuttons ist bei gesperrtem Betrieb deaktiviert. Entwürfe bleiben
erhalten und sind nach Ende der Wartung wieder bearbeitbar.

Konfigurationsänderungen im selben Adminbereich aktualisieren den Status direkt.
Eine geöffnete Mail-Testansicht fragt ihn mit Vaadins nativem Polling spätestens
beim nächsten Poll erneut ab (angefordert: höchstens fünf Sekunden; Browser können
Hintergrundtabs verzögern). Beim Verlassen wird der eigene Listener entfernt und
der vorherige Pollingabstand wiederhergestellt. Der Versandcallback prüft den
Status zusätzlich frisch, bevor er den Maildienst aufruft. Ein bereits laufender
SMTP-Aufruf wird nicht abgebrochen; es gibt keine Transaktionssperre gegen einen
gleichzeitigen Wartungswechsel. Automatische Mails und deren Queue behalten ihr
bestehendes Verhalten.

Gezielte Prüfung: 70 JUnit-Fälle erfolgreich, keine Fehler oder Skips, davon neun
neue Mail-Testfälle für DE/EN, Wartung, unlesbare Konfiguration, Entwurfserhalt,
frische Versandprüfung und Polling-Lebenszyklus. Maildienst und Konfiguration sind
gemockt; keine Anwendung, reale Datenbank oder SMTP-Verbindung wurde gestartet.
Keine neuen Browserprüfungen oder Produktionsbuilds; sichtbare Geometrie und
Mailzustellung sind damit nicht nachgewiesen.
