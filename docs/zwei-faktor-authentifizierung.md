# Zwei-Faktor-Authentifizierung

Die Anwendung verwendet TOTP nach RFC 6238: sechs Ziffern, SHA-1 und
30-Sekunden-Schritte. Kompatible Apps sind frei wählbar. Google Authenticator
und Microsoft Authenticator sowie 2fast für Windows werden als Beispiele verlinkt.
Die Wahl der App ist unabhängig davon, ob das Portalpasswort, Google oder Microsoft
zur Anmeldung dient.

## Einrichtung und eigene Verwaltung

Der Einrichtungsschritt bietet wahlweise den QR-Code oder einen manuellen
Schlüssel. Die Schlüsselanzeige ist eine native schreibgeschützte Vaadin-TextArea
mit zwei Zeilen. Beim Sprachwechsel bleibt der gewählte Tab erhalten.
Der Browser lädt den Schlüssel geschützt und ohne Zwischenspeicherung direkt in
die Eingabe. Der Flow-Hostwert bleibt leer; der Schlüssel gelangt dadurch nicht
in den dauerhaften Vaadin-UI-Zustand.
Die erste Bestätigung zeigt sechs Kästchen für einen gemeinsamen sechsstelligen
Code. Ein vollständiger Code kann auf einmal eingefügt werden, auch mit
trennenden Leerzeichen. Die Aktivierung erfolgt über den Bestätigungsbutton.

Unter **Mein Konto → Sicherheit → Portalzugang** stehen Einrichtung,
Authenticatorwechsel, neue Ersatzcodes und Entfernen zur Verfügung. Eine
verpflichtende Anmeldung führt vor dem Portalzugriff direkt zur Einrichtung bzw.
Codeabfrage. Die QR-Grafik entsteht lokal. Für die manuelle Eingabe gibt es einen
geschützten Einrichtungsschlüssel.

Bei verpflichtender Ersteinrichtung erklärt zunächst eine kompakte Karte die
Anforderung. „Abmelden“ beendet die Anmeldung, „Einrichten“ führt zur Einrichtung.
Nach einer frischen Portalpasswort-Anmeldung ist keine zweite Passwortabfrage
nötig: Der originale Spring-Passwortnachweis derselben Sitzung gilt dafür weniger
als fünf Minuten und wird beim Start erneut geprüft. Er wird weder durch einen
Seitenaufruf noch durch Refresh verlängert. Bei älteren Sitzungen und Remember-me
folgt die bestehende Identitätsbestätigung. Die Wiederverwendung gilt nur für ein
bisher nie geschütztes Konto mit Einrichtungspflicht, nicht für freiwillige
Einrichtung, frühere Faktoren oder Betreiber-Wiederherstellungen. Ein erster
gültiger TOTP ist in jedem Fall nötig, bevor Portalzugriff freigegeben wird.

Eine Einrichtung beginnt mit dem aktuellen Portalpasswort oder einer frischen,
kontogebundenen Microsoft-Bestätigung. Google bestätigt keine ausreichend frische
aktive Anmeldung. Ausschließlich für die erstmalige Einrichtung eines bisher nie
geschützten Google-Kontos gilt deshalb die vereinbarte Kombination: neuer
kontogebundener Google-Durchlauf, achtstelliger Einmalcode an die bereits bestätigte
Kontaktadresse und erster TOTP. Google-Konto und Mailpostfach können derselben
Sitzung unterliegen. Dieses verbleibende Risiko wird nicht als unabhängige 2FA
von Google dargestellt. Der Mailcode gilt fünf Minuten. Pro Konto sind höchstens
fünf Anfragen in 15 Minuten und eine pro Minute erlaubt.

Ein vorhandener oder früher entfernter Authenticator wird durch diesen Google-/
Mailweg niemals ersetzt. Für den Authenticatorwechsel muss ein aktueller TOTP oder
Ersatzcode bestätigt werden. Der alte Faktor bleibt bis zum bestätigten Abschluss
gültig. Danach sind altes Geheimnis und alte Ersatzcodes ungültig. Das Entfernen
verlangt ebenfalls einen eigenen Faktornachweis und ist bei einer Pflichtregel
gesperrt. Ein reines Google-Konto benötigt vor dem Entfernen zusätzlich ein
Portalpasswort oder eine nutzbare Microsoft-Verbindung, damit eine sichere spätere
Neueinrichtung möglich bleibt. Der direkte geschützte Authenticatorwechsel bleibt
auch für reine Google-Konten verfügbar.

Bei verlorener App nach der regulären Anmeldung **Authenticator wiederherstellen**
wählen, bevor ein Ersatzcode zur reinen Anmeldung verbraucht wird. Ein unbenutzter
Ersatzcode bereitet ausschließlich den geschützten Wechsel vor. Auch der letzte
verbliebene Code genügt. Während des gültigen Vorgangs bleibt diese Sitzung auf
die Einrichtung beschränkt. Weder eine allgemeine Anmeldebestätigung noch eine
Freigabe für Passwort- oder andere Kontoänderungen entsteht dadurch.

Alter Faktor und Ersatzcode bleiben bis zum ersten gültigen TOTP der neuen App
erhalten. Erst der erfolgreiche Abschluss ersetzt den Faktor und widerruft die
alten Codes atomar. Ein abgebrochener oder nach fünf Minuten abgelaufener Versuch
verbraucht den Ersatzcode nicht. Wird er zwischenzeitlich für eine andere Anmeldung
verwendet, darf der vorbereitete Wechsel nicht mehr abschließen. Von zwei
gleichzeitigen Wechseln mit demselben Code kann nur einer gewinnen. Danach werden
neue Ersatzcodes ausgegeben und andere Sitzungen widerrufen.

Bei der Einrichtung und beim Erzeugen neuer Ersatzcodes werden zehn zufällige
Codes einmalig angeboten. Alle Codes kopieren oder als PDF herunterladen und
ausdrucken. Das Dokument enthält Logo, Servicename, Konto, Datum und Erläuterungen.
Die Codes gelten jeweils einmal. Neue Codes widerrufen sämtliche alten Codes.
Das Erneuern der Codes erhält Art und Zeitpunkt des vorhandenen Sitzungsnachweises.
Nach dem Schließen der Ausgabe oder nach fünf Minuten werden sie nicht erneut
angezeigt. Die Anwendung speichert die PDF nicht als Datei.

## Konfiguration und Standardwerte

**Administration → Konfiguration → Zwei-Faktor-Authentifizierung** ist ausschließlich
für Superadmins zugänglich. Das gilt auch für Lesen, Neuladen, direkte Dienstaufrufe
und die Google-/Microsoft-Anbieterkonfiguration. Die reguläre Anmeldung bei diesen
Anbietern benötigt selbstverständlich keine Administratorrechte.

Für jede explizit zugewiesene Rolle und jede Funktion gilt eine der Regeln:

| Regel | Bedeutung |
| --- | --- |
| Aus | Keine zusätzliche 2FA-Anforderung dieser Funktion. Andere Identitätsprüfungen bleiben erhalten. |
| Falls eingerichtet | Mit vorhandenem Authenticator verbindlich. Ohne eingerichteten Faktor keine Einrichtungspflicht. |
| Verpflichtend | Vor dem Zugriff einrichten und bestätigen. |

Bei mehreren Rollen gilt die strengste Regel. Ein Konto ohne aktuelle Rolle erhält
keine MFA-Ausnahme. Rollen- und Konfigurationsänderungen wirken auch auf bereits
offene Ansichten. Ein neuer Regelstand macht bestehende Freigaben ungültig.

Voreinstellung: Admin und Superadmin müssen 2FA verwenden. Für Benutzer und Gäste
gilt „Falls eingerichtet“. Der zusätzliche Schutz allein für den Eintritt in den
Administrationsbereich ist zunächst aus. Anmeldung und sensible Funktionen sind
getrennt konfigurierbar: Passwort, E-Mail-Adresse, Anbieter verbinden/trennen,
Faktor- und Ersatzcodeverwaltung sowie Sicherheitskonfiguration. Änderungen der
Sicherheitskonfiguration werden nach der vorherigen Regel bestätigt. Ein noch
nicht gespeicherter Wert kann seine eigene Bestätigung nicht abschalten.

Die zusätzliche Inaktivitätsfrist ist standardmäßig aus. Bei Bedarf sind Minuten,
Stunden oder Tage einstellbar, insgesamt eine Minute bis 365 Tage. Ausschalten
erhält den Zeitwert. Nur echte Eingaben zählen als Aktivität. Hintergrundanfragen,
Heartbeat, Push und Polling verlängern die Frist nicht.
Auch sitzungsgebundene Benachrichtigungszugriffe auf Push-Threads prüfen diesen
Nachweis. Nach Ablauf werden keine neuen Vorschauinhalte geladen. Eine offene
Vorschau wird geschlossen und ist erst nach erneuter Bestätigung wieder nutzbar.

In einer gültigen Sitzung lösen Navigation, Refresh und weitere Tabs keine neue
Anmeldebestätigung aus. Eine neue Anmeldung, ein abgelaufener Nachweis oder eine
geänderte Regel verlangen sie erneut. „Login speichern“ ersetzt nur den ersten
Anmeldenachweis. Es überspringt keine geltende 2FA-Regel. Sensible Aktionen verlangen
nach der Voreinstellung jeweils eine eigene, einmalige Bestätigung. Eine solche
Freigabe gilt höchstens fünf Minuten für genau Konto, Sitzung, Faktor-, Zugangs-
und Regelstand und den angeforderten Zweck.

Auch Passwort- und Anbieterbestätigungen sind an den Faktor- und Regelstand
gebunden. Beim Verbinden eines Anbieters wird die Freigabe erst nach dem
OIDC-Rücksprung unter dem Kontolock geprüft und zusammen mit der tatsächlichen
Verbindung verbraucht. Der ausgehende Aufruf verlängert die ursprüngliche Frist
nicht. Zwischenzeitliche Konto-, Faktor- oder Regeländerungen verhindern den Abschluss.

## Schlüssel und Entwicklungsoptionen

`APP_SECURITY_TOTP_ENCRYPTION_KEY` ist ein Base64-kodierter zufälliger
32-Byte-Schlüssel. Er gehört außerhalb der Datenbank und außerhalb von Git in die
Startkonfiguration. Er muss beim Neustart gleich bleiben und separat gesichert
werden. Die Erzeugung ist in [.env.example](../.env.example) beschrieben. Ohne
Schlüssel ist eine Neueinrichtung nicht verfügbar. Es wird kein flüchtiger Ersatz
erzeugt. Ein verlorener Schlüssel macht vorhandene TOTP-Geheimnisse unlesbar.

`APP_SECURITY_DEVELOPMENT_OPTIONS_ENABLED=false` ist die Voreinstellung. Nur nach
Start mit `true` erscheinen zwei getrennte Superadmin-Optionen: MFA-Prüfung für
Entwicklung aussetzen und die schwache Google-Bestätigung sensibler Aktionen
zulassen. Die Startfreigabe allein schaltet nichts ab. Die Optionen werden gewarnt
angezeigt. Ohne Startfreigabe sind auch früher gespeicherte Ausnahmen unwirksam.
Eine Ausnahme erzeugt keinen angeblich erfolgreich geprüften zweiten Faktor.

TOTP-Geheimnisse sind mit AES-GCM und Kontobindung verschlüsselt. Ersatzcodes werden
kontogebunden gehasht gespeichert. Kurzlebige Einrichtung und Codeausgabe sind
verschlüsselt und sitzungsgebunden. QR, manueller Schlüssel und Codes gelangen über
eigene geschützte Endpunkte zum Browser und nicht als Klartext in den dauerhaften
Vaadin-/JDBC-Sitzungszustand. Antworten sind nicht cachebar. Wiederverwendung von
TOTP-Zeitschritten und Ersatzcodes wird unter Kontosperre verhindert. Fünf ungültige
Versuche sperren die Bestätigung 15 Minuten, unabhängig von Dialog oder Sitzung.

## Verlust und Kontakt

Nach dem ersten Anmeldenachweis ist an der 2FA-Abfrage die eigene Hilfsanfrage
erreichbar. Dieser eingeschränkte Zugang öffnet keine anderen Kontobereiche.
Pro Konto ist eine offene Anfrage möglich, höchstens drei neue pro Tag. Beschreibung
und Antworten sind auf 2.000 Zeichen, der Verlauf auf 50 Einträge begrenzt.
Geschlossene Anfragen werden nach 180 Tagen entfernt.

Admins und Superadmins sehen die gemeinsame Liste, erhalten eine interne
Benachrichtigung und können einen Vorgang übernehmen. Veraltete oder konkurrierende
Änderungen überschreiben keine Zuständigkeit. Nur der Bearbeiter antwortet, sendet
die vorbereitete Anleitung oder schließt den Vorgang. Antragsteller sehen eigene
öffentliche Antworten. Interne Vermerke bleiben intern.

„Anleitung senden“ zeigt den konkreten Text in der gespeicherten Kontosprache vorab,
legt ihn als öffentliche Antwort ab und nimmt eine E-Mail an die bestehende
bestätigte Kontaktadresse in die Mailqueue auf. Queueaufnahme ist keine bestätigte
Zustellung. Ein fehlgeschlagener Queueauftrag bleibt wiederholbar. Die Anleitung
enthält keine neuen Codes. Freier Tickettext, Name, Geburtsdatum und ein E-Mail-Link
sind keine Wiederherstellungsnachweise. Ohne Authenticator und gültigen Ersatzcode
wird die Sperre nicht über das Ticketsystem aufgehoben.

Der [Betreiber-Notfallweg](betrieb/2fa-notfall.md) betrifft ausschließlich den
Betreiber mit Server-/Datenbankzugriff. Er ist keine Supportantwort und wird nicht
an Benutzer versendet. Es existiert weder ein versteckter Benutzer noch eine zweite
Reparaturoberfläche.
