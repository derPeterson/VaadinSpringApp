# SMTP-SocketFactory gezielt anbinden

Stand: 15.10.2026; Ausgangscommit `1d57a5e`. **Umsetzungsplan, noch nicht implementiert.**

| Eigenschaft | Wert |
| --- | --- |
| Konfiguration | `mail.smtp.socketFactory.class` / `ConfigEntry.MAIL_SMTP_SOCKETFACTORY_CLASS` |
| Enum-Standard | `javax.net.ssl.SSLSocketFactory` |
| Gruppe | SMTP |
| Vorgeschlagene Aktivierung | Mit dem SMTP-Sender beim Start; Änderung benötigt Neustart |
| Priorität | Nachrangig; Spezialbedarf und Bedeutung zuerst bestätigen |

## Heutiger Stand

Das Feld prüft die Schreibweise eines Java-Klassennamens; leer ist zulässig. Existenz
und Kompatibilität werden nicht geprüft. `MailConfig.javaMailSender()` liest den Wert
nicht. SMTP funktioniert über Host/Port/Auth, SSL bzw. STARTTLS und Trust bereits.
`ConnectionAwareJavaMailSender` markiert nachgewiesene Vorversandfehler für die
bestehende konservative Retrybehandlung.

## Entscheidung vor Umsetzung

Der Schlüssel klingt nach allgemeiner SocketFactory, die Feldhilfe bezeichnet aber
SSL/TLS und der Standard ist eine SSL-Factory. Diese Mehrdeutigkeit bewusst auflösen:
Einfaches Weiterreichen des bisherigen Standards an eine allgemeine Socket-Property
könnte funktionierendes Klartext-SMTP verändern.

**Mein Vorschlag:** Feld als optionale erweiterte **TLS-SocketFactory** definieren,
bisherigen DB-Schlüssel aus Kompatibilitätsgründen erhalten und die Hilfe präzisieren.
Eine allgemeine, auch Klartext verändernde Factory bräuchte einen anderen Vertrag.

Leer und der bisherige Standardname bedeuten „Systemstandard verwenden“ und setzen
keine neue Factory-Property. Eine abweichende explizite TLS-Factory wird nur für
TLS-Verbindungen übergeben. Bestehende Installationen behalten so ihr Transportverhalten.

## Gewünschtes Verhalten

| Transportkonfiguration | Vorgeschlagene Wirkung |
| --- | --- |
| Ohne SSL/STARTTLS | Keine TLS-Factory auf Klartext-Sockets anwenden; Feldhilfe erklärt dies |
| SSL aktiv | Explizite TLS-Factory für verschlüsselten Transport verwenden |
| STARTTLS aktiv | Explizite TLS-Factory erst beim TLS-Wechsel verwenden |
| Leer/bisheriger Standardname | Bisherige Provider-/Systemauswahl erhalten |
| Abweichende ungültige Klasse | Im Formular ablehnen; kein Ausweichen auf Klartext |

Bestehende Komponenten und Stile bleiben. Beschreibung, semantische Prüfung und
ehrlicher Neustarthinweis kommen hinzu. Der Mail-Test verwendet nach Neustart denselben
Sender wie Queue und Adminmail.

## Technischer Umsetzungsweg

1. Die tatsächlich aufgelöste Angus-/Jakarta-Mail-Version vor Implementierung prüfen.
   Die Zuordnung des bisherigen DB-Schlüssels zur TLS-Provider-Property
   `mail.smtp.ssl.socketFactory.class` als Adapterentscheidung dokumentieren.
   Kein ungeprüftes 1:1-Durchreichen an `mail.smtp.socketFactory.class`.
2. Den Wert in `MailConfig` lesen. Leer/Standard belassen den aktuellen Zustand.
   Für abweichende TLS-Klassen die SSL-spezifische Property setzen und stillen Factory-
   Fallback gezielt unterbinden. Andere SMTP-Werte unverändert übernehmen.
3. Gemeinsame Klassenvalidierung neu vorschlagen: syntaktisch gültiger Name, Klasse
   im Anwendungsklassenpfad vorhanden und mit `SSLSocketFactory` kompatibel. Den
   Factory-Erzeugungsvertrag des konkreten Providers prüfen. `SSLSocketFactory` ist
   nicht einfach eine normal instanziierbare konkrete Klasse. Bei bloßer Prüfung
   keine beliebigen statischen Initialisierungen auslösen.
4. Die Prüfung beim Speichern verwenden. Ungültige alte, außerhalb der UI gespeicherte
   Werte müssen vor tatsächlichem Versand einen klaren Konfigurationsfehler liefern.
   Die Adminoberfläche soll zur Korrektur erreichbar bleiben. Kein stiller Fallback
   und kein Erfolgshinweis ohne Verwendung der ausgewählten Factory.
5. `mail.smtp.ssl.trust` ausdrücklich mitprüfen: Eine eigene Factory kann eigene Trust-
   Entscheidungen treffen. Die UI darf dann nicht behaupten, die bisherige Trust-Liste
   werde sicher zusätzlich angewendet. Auch deshalb beim bisherigen Standard keine
   explizite neue Factory setzen.
6. `ConnectionAwareJavaMailSender` erhalten. Vorversandfehler behalten ihren Nachweis;
   unklare Fehler nach SMTP-Annahme lösen keinen neuen automatischen Versand aus.
7. DE/EN-Hilfe, Mailvertrag und isolierte SMTP-Tests ergänzen. Speicherung allein
   ändert den vorhandenen Sender nicht; kein Live-Umbau während aktiver Sendungen.

Eine neue Factory darf Zertifikatsprüfung nicht pauschal abschalten. Ob STARTTLS
zukünftig zwingend statt opportunistisch sein soll, ist eine separate Entscheidung.
Dieses Paket ändert diese Regel nicht beiläufig.

## Tests und Abnahme

- **JUnit:** echte Sender-Properties für leer/Standard/explizite Klasse; SSL, STARTTLS
  und Klartext getrennt; ungültige, fehlende oder inkompatible Klasse.
- **`MailConfigTest`:** genaue Propertyanzahl erst nach Prüfung der neuen bedingten
  Properties bewusst anpassen; Assertions nicht nur entfernen.
- **Transportfixture:** eigene Test-Factory belegt ihren Aufruf; Klartext-Mailpit
  funktioniert weiterhin. STARTTLS/SSL über eine eigene TLS-fähige SMTP-Fixture
  einschließlich Zertifikats-/Handshakefehlern nachweisen. Mailpit-Erreichbarkeit
  allein belegt den TLS-Vertrag nicht.
- **Queue-Regression:** Vorversandfehler, Authentifizierung, unklarer Ausgang und
  Adminmail behalten Retry-/Auditverträge.
- **Browser DE/EN:** Feldhilfe, ungültige Klasse, Speicherung und Neustarthinweis.
  Ergänzende Betriebsprüfung belegt die Factory nach Senderneustart.

Fertig ist das Paket bei nachweislich verwendeter expliziter Test-Factory,
unverändertem Standardversand und geklärter Feldbedeutung. Die Prüfungen stehen aus.
Falls kein Spezialbedarf besteht, wäre die spätere bewusste Entfernung des Felds
die einfachere Alternative. Diese Produktentscheidung vor Umsetzung treffen.

## Grundlage und Anschlussstellen

[MailConfig](../../../src/main/java/de/derpeterson/app/config/MailConfig.java),
[ConnectionAwareJavaMailSender](../../../src/main/java/de/derpeterson/app/config/ConnectionAwareJavaMailSender.java),
[MailConfigTest](../../../src/test/java/de/derpeterson/app/config/MailConfigTest.java),
[Mailvertrag](../../email-service-contract.md), [Queuevertrag](../../email-queue-processing.md).

Angus unterscheidet allgemeine und SSL-spezifische SocketFactory-Properties:
[SMTP-Provider-Dokumentation](https://eclipse-ee4j.github.io/angus-mail/docs/api/org.eclipse.angus.mail/org/eclipse/angus/mail/smtp/package-summary.html).
Der Provider berücksichtigt SSL-Factories beim STARTTLS-Wechsel:
[SocketFetcher-Quellcode](https://github.com/eclipse-ee4j/angus-mail/blob/master/core/src/main/java/org/eclipse/angus/mail/util/SocketFetcher.java).
Die Alias-/Standardbehandlung oben ist mein Kompatibilitätsvorschlag und noch keine Appfunktion.
