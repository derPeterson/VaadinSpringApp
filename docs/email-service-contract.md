# Mailversand-Verträge

## Kodierung und Nachrichtentyp

`MailConfig.javaMailSender()` erzeugt den tatsächlich injizierten
`JavaMailSenderImpl` und setzt dessen `defaultEncoding` explizit auf UTF-8.
Host, Port, Anmeldedaten und SMTP-Properties behalten ihre bisherigen Quellen.
Dies erfolgt nicht über eine zusätzliche, möglicherweise ungenutzte Boot-Property.

`EmailService.sendEmail` erzeugt genau einen `text/html; charset=UTF-8`-Teil.
Der Helper erzeugt den Content-Type; es gibt keinen manuellen Header und keine
Multipart-Hüllen. Der aktuelle Dienst unterstützt weder Attachments noch
Inline-Ressourcen oder einen alternativen Plain-Text-Inhalt.

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
`MailException`, etwa Parse-, Authentifizierungs- und Versandfehler des Senders,
werden nicht abgefangen, verändert oder durch einen Fallback ersetzt.
Konfigurations- und andere Runtimefehler propagieren ebenfalls unverändert.
Nach einem Aufbau-/Konfigurationsfehler wird der Sender nicht aufgerufen.

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
