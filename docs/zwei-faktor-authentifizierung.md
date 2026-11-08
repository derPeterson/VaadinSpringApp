# Zwei-Faktor-Authentifizierung

Die Anwendung verwendet TOTP nach RFC 6238 mit sechs Ziffern, HMAC-SHA-1 und
30-Sekunden-Schritten. HMAC-SHA-1 ist Teil des gemeinsamen Formats von QR-Code,
manueller Einrichtung und Codeprüfung. Kompatible Apps sind frei wählbar. Google Authenticator
und Microsoft Authenticator sowie 2fast für Windows werden als Beispiele verlinkt.
Die Wahl der App ist unabhängig davon, ob das Portalpasswort, Google oder Microsoft
zur Anmeldung dient.

## Einrichtung und eigene Verwaltung

Die Ersatzcodeausgabe bleibt beim PDF-Download auf derselben Seite. Fehler werden
als Standardmeldung angezeigt. Anzeigen, Kopieren und PDF-Download bleiben bis
„Codes gesichert“ oder bis zum Ende der berechtigten Sitzung möglich. Es gibt für
diese Ausgabe keine zusätzliche Fünf-Minuten-Frist. Herunterladen verbraucht sie nicht.
Die zehn Codes sind beim Betreten bereits aufgeklappt. Der PDF-Dateiname enthält
den auf sichere ASCII-Zeichen bereinigten Servicenamen, beispielsweise
`derPeter-recovery-codes.pdf`. Ohne verwendbaren Namen bleibt es bei `recovery-codes.pdf`.
Erst der Switch „Ich habe meine Ersatzcodes sicher gespeichert“ schaltet den
Abschlussbutton „Codes gesichert“ frei. Die Bestätigung bleibt beim Sprachwechsel für denselben
Codesatz erhalten, gilt aber nicht für einen neuen Codesatz. Sie dokumentiert die
bewusste Bestätigung, kann die tatsächliche sichere Ablage jedoch nicht nachweisen.
Die aktuellen Konto-, Sitzungs-, Faktor- und Berechtigungsprüfungen bleiben erhalten.
Status, Inhaltstyp und PDF-Signatur werden vor dem Dateidownload geprüft.
Eine Fehlerantwort wird nicht als TXT-Datei angeboten. „Codes gesichert“ verwirft
die Ausgabe endgültig und erteilt keinen neuen Sicherheitsnachweis. Abmeldung,
Sitzungsablauf oder Sitzungswiderruf sperren den Zugriff ebenfalls. Die automatische
Bereinigung entfernt bestätigte Ausgaben und Ausgaben ohne aktive Sitzung.
Die fünf Minuten gelten weiterhin für Sicherheitsbestätigungen und Einrichtungsschritte.

Der Einrichtungsschritt bietet wahlweise den QR-Code oder einen manuellen
Schlüssel. Die Schlüsselanzeige ist eine native schreibgeschützte Vaadin-TextArea
mit zwei Zeilen. Beim Sprachwechsel bleibt der gewählte Tab erhalten.
Der Browser lädt den Schlüssel geschützt und ohne Zwischenspeicherung direkt in
die Eingabe. Der Flow-Hostwert bleibt leer; der Schlüssel gelangt dadurch nicht
in den dauerhaften Vaadin-UI-Zustand.
Die erste Bestätigung zeigt sechs Kästchen für einen gemeinsamen sechsstelligen
Code. Ein vollständiger Code kann auf einmal eingefügt werden, auch mit
trennenden Leerzeichen. Sobald alle sechs Ziffern eingegeben, eingefügt oder per
Autofill übernommen sind, wird automatisch geprüft. Das gilt bei Einrichtung und
Anmeldebestätigung; der Bestätigungsbutton bleibt zusätzlich verfügbar. Unvollständige
Codes werden nicht automatisch abgesendet. Nach einem falschen Code kann erneut
eingegeben und bestätigt werden.

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
Nach „Codes gesichert“ oder dem Ende der Sitzung werden sie nicht erneut angezeigt.
Die Anwendung speichert die PDF nicht als Datei. Der Ablauf der Sitzungsausgabe
ändert die Gültigkeit bereits gesicherter Ersatzcodes nicht.

Einrichtung, Wiederherstellung und 2FA-Hilfe begrenzen den Zugriff der gesamten
aktuellen Sitzung auf den jeweiligen Ablauf. Direktlinks und alte Tabs führen zum
laufenden Schritt zurück. Der reduzierte Seitenkopf zeigt den Kontext der Anmeldung
oder des Kontos und bietet weiterhin die Sprachwahl. Die Aktion im Ablauf führt
bewusst zurück, meldet ab oder bestätigt die Sicherung der Ersatzcodes. Während der
Ersatzcode-Ausgabe öffnet ein Neuladen in derselben gültigen Sitzung die Ausgabe erneut.

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

In den Bestätigungsdialogen für Sicherheitsaktionen führt die Hilfe unter der
Codeeingabe zur eigenen Wiederherstellungsanfrage. Der Dialog schließt dabei und
die begonnene Aktion endet. Nach Verlassen der Hilfe kann sie neu gestartet werden.

Nach dem ersten Anmeldenachweis ist an der 2FA-Abfrage die eigene Hilfsanfrage
erreichbar. Dieser eingeschränkte Zugang öffnet keine anderen Kontobereiche.
Pro Konto ist eine offene Anfrage möglich, höchstens drei neue pro Tag. Beschreibung
und Antworten sind auf 2.000 Zeichen, der Verlauf auf 50 Einträge begrenzt.
Die sichtbare Vorgangsnummer beginnt bei `#10001` und wird in beiden Ansichten
aus der internen ID gebildet.
Geschlossene Anfragen werden nach 180 Tagen entfernt.

Admins und Superadmins sehen die gemeinsame Liste, erhalten eine interne
Benachrichtigung und können einen Vorgang übernehmen. Veraltete oder konkurrierende
Änderungen überschreiben keine Zuständigkeit. Der zuständige Bearbeiter kann
antworten, die vorbereitete Anleitung senden und den Vorgang abschließen.
Antragsteller sehen ihre öffentlichen Antworten und können ihre eigene Anfrage
schließen. Öffentliche Antworten aus der Administration erscheinen als
„Systemadministration“. In der Administration zeigen öffentliche Antworten und
interne Vermerke stattdessen Avatar und Namen des tatsächlichen Autors mit Name
und E-Mail im bestehenden Tooltip. Die Identitätsdaten werden ausschließlich für
die berechtigte Adminansicht ausgegeben. Bei gelöschtem Autorenkonto bleibt die
neutrale Beschriftung erhalten. Die Herkunft wird beim Schreiben gespeichert und bleibt
auch bei einem Rollenwechsel oder gelöschtem Bearbeiterkonto erhalten.
„Vom Nutzer geschlossen“ und „Abgeschlossen“ sperren Antworten und
den Versand der Anleitung. Ein Admin kann einen vom Nutzer geschlossenen Vorgang
abschließen, wenn er ihm zugewiesen oder noch unzugewiesen ist. Bei Bedarf kann
der Antragsteller im Rahmen des Tageslimits eine neue Anfrage eröffnen.

### Ticketzustände, Zähler und Glocke

Die roten Zahlen an Vorgängen zählen ungelesene einzelne Nachrichten für das
jeweilige Konto. Die Glocke zählt dagegen ungelesene, fachlich noch relevante
Benachrichtigungen. Beides ist unabhängig voneinander; ein historischer Hinweis
wird durch die Übernahme nicht automatisch als gelesen markiert.

Der rote Punkt am SideNav-Eintrag „Ticketsystem“ fasst die relevanten ungelesenen
Nachrichten ohne weitere Zahl zusammen. Er folgt denselben Regeln wie die
Ticketzähler und aktualisiert sich auch in anderen Administrationsbereichen.
Der Tooltip lautet „Neue Aktivitäten im Ticketsystem“. Das bloße Öffnen des
Ticketsystems entfernt den Punkt nicht, erst das Lesen oder der Wegfall aller
relevanten ungelesenen Nachrichten.

Die Adminliste zeigt standardmäßig 10 Vorgänge pro Seite. Im Footer lassen sich
20 oder 50 Einträge wählen. Die Pfeile wechseln jeweils zur vorherigen oder
nächsten Seite. Kategorie, Status, Zuständigkeit und „Alle / Ungelesen“ filtern
den berechtigten Bestand in der Datenbank. Der Footer zeigt den Bereich und die
Gesamtzahl der Treffer. Die persönliche Liste lädt weiterhin bis zu 20 Anfragen.
Eine Änderung der Kategorie, Filter oder Seitengröße beginnt auf Seite eins.
Aktualisieren und Live-Updates behalten die aktuelle Seite bei, solange sie
vorhanden ist. Beim Blättern bleiben geöffnete Details und Antwortentwurf erhalten.
Der Ticketsprung nimmt die sichtbare Ticketnummer mit oder ohne Raute entgegen, hebt
Listenfilter auf und öffnet die passende Seite. Ein unbekannter Vorgang lässt
die bisherige Auswahl bestehen. Der Zugriff erfordert dieselben Adminrechte
wie die Liste.
Im Filter „Ungelesen“ verschwindet ein gelesener Vorgang aus der Liste. Seine
geöffnete Detailansicht und der Antwortentwurf bleiben zur Bearbeitung erhalten.
Ein geänderter Status- oder Zuständigkeitsfilter kann ihn weiterhin ausblenden.

| Ticketstatus | Ungelesen-Zähler in der Administration | Was noch zu tun ist |
| --- | --- | --- |
| Offen | Für alle berechtigten Admins | Übernehmen und prüfen |
| In Bearbeitung | Für den Bearbeiter und berechtigte Beobachter | Antworten und bearbeiten |
| Vom Nutzer geschlossen, zugewiesen | Für den Bearbeiter und berechtigte Beobachter | Verlauf prüfen und endgültig abschließen |
| Vom Nutzer geschlossen, unzugewiesen | Für alle berechtigten Admins | Prüfen und endgültig abschließen; dabei wird der ausführende Admin zugewiesen |
| Abgeschlossen | Kein Zähler, auch nicht in der Nutzeransicht | Verlauf bleibt lesbar; keine weiteren Nachrichten oder Ticketänderungen |

Eine Beobachtung fremder Vorgänge ist Superadmins vorbehalten. Der Bearbeiter
sieht den Zähler auch ohne aktiviertes „Beobachten“. Ein neuer Admin kann ältere
Nachrichten als ungelesen sehen, sofern der aktuelle Ticketstatus und seine
Zuständigkeit beziehungsweise Beobachtung das vorsehen.

| Ereignis | Glockenhinweis und Empfänger |
| --- | --- |
| Neues Ticket | Alle aktivierten, bestätigten Admins und Superadmins erhalten einen Hinweis mit „Zum Ticket“ |
| Übernahme | Der ursprüngliche Hinweis „Neues Ticket“ erhält „Bereits übernommen“ / „Already assigned“ und entfällt im Glockenzähler |
| Neue öffentliche Nachricht | Aktiv beobachtende, berechtigte Bearbeiter und Superadmins erhalten einen Hinweis; der Absender erhält keinen eigenen Hinweis |
| Interner Vermerk | Keine zusätzliche Glockenbenachrichtigung; bleibt ausschließlich administrativ sichtbar |
| Nutzer schließt ein zugewiesenes Ticket | Bearbeiter auch ohne Beobachtung sowie berechtigte Beobachter erhalten einen eigenen Hinweis zur Prüfung und zum abschließenden Schließen |
| Nutzer schließt ein unzugewiesenes Ticket | Alle aktivierten, bestätigten Admins und Superadmins werden zur abschließenden Prüfung informiert |
| Endgültiger Abschluss | Zugehörige Hinweise erhalten „Abgeschlossen“ / „Closed“ und entfallen im Glockenzähler |

Beim Nutzerabschluss werden ältere Ticket-Hinweise mit „Vom Nutzer geschlossen“
gekennzeichnet und nicht mehr gezählt; der neue Abschlussprüfhinweis bleibt relevant.
Damit zählen frühere Nachrichten und derselbe anschließende Prüfauftrag nicht
nebeneinander als mehrere offene Aufgaben. Eigene Nutzerabschlüsse lösen keinen
zusätzlichen Hinweis an das ausführende Konto aus.

Historische Benachrichtigungen bleiben im Postfach sichtbar. Fachlich erledigt,
persönlich gelesen und persönlich entfernt sind getrennte Zustände. Der Filter
„Ungelesen“ und „Alle als gelesen markieren“ berücksichtigen nur weiterhin relevante
Hinweise; die Gesamtansicht enthält auch erledigte Hinweise.

Beim sichtbaren Lesen eines Ticketverlaufs werden nur bereits dargestellte
Nachrichtenbenachrichtigungen und bereits dargestellte Abschlusszustände quittiert.
Eine danach eintreffende Nachricht oder ein späterer Nutzerabschluss bleibt ungelesen.
Ein Hintergrundtab quittiert erst nach dem Sichtbarwerden. Andere geöffnete Tabs
desselben Kontos erhalten den aktualisierten Stand ebenfalls.

„Beobachten“ auszuschalten verhindert weitere normale Nachrichtenhinweise. Es
ändert weder Zuständigkeit noch Lesestatus und unterdrückt für den Bearbeiter
nicht die nötige Abschlussprüfung. Nach Rollenentzug, Kontosperre oder ausstehender
Kontobestätigung werden keine neuen administrativen Ticket-Hinweise zugestellt.
Die Übergabe zwischen Bearbeitern ist vorerst nicht vorgesehen.

Vorgangsänderung, ursprüngliche Hinweise erledigen und neue Hinweise anlegen
gehören zur gleichen Transaktion unter der bestehenden Ticketsperre. Erst nach
Commit werden offene Ansichten und Glocken aktualisiert. Wiederholte Lese- und
Statusmeldungen verändern den Zähler nicht mehrfach; ein zurückgerollter Vorgang
löst keine erfolgreiche Live-Aktualisierung aus.

Geöffnete Anfrageansichten in Wiederherstellung und Administration laden
Änderungen nach erfolgreicher Speicherung per Vaadin-Push nach. „Aktualisieren“
lädt weiterhin auf Wunsch neu. Ticketauswahl und Entwurf bleiben beim Nachladen
erhalten. Entwürfe sind an die jeweilige Anfrage gebunden. Die rote Zahl am
Ticket zeigt ungelesene Nachrichten und wird auch in anderen geöffneten Tabs
desselben Kontos aktualisiert. Sie wird pro
Konto, Ticket und Ansicht geführt und berücksichtigt nur Nachrichten, die in
der jeweiligen Ansicht sichtbar sind.

Die Administration bündelt die Bearbeitung unter „Tickets“. Die Tabs „Alle
Tickets“ und „Wiederherstellung“ verwenden die vorhandene Ticketart. Weitere
Ticketarten sind noch nicht eingerichtet. Auf breiten Fenstern bleiben Liste
und Nachrichtenverlauf nebeneinander sichtbar. Schmale Fenster ordnen sie
untereinander an. Der Verlauf nutzt dieselben Sprechblasen wie die Benutzerhilfe
und kennzeichnet interne Vermerke separat.
Erstellung und tatsächliche Statuswechsel werden in derselben Transaktion als
administrative Systemereignisse im Verlauf gespeichert: vorheriger/neuer Zustand,
Zeitpunkt und handelnde Person. Wiederholtes Übernehmen desselben Tickets erzeugt
keinen weiteren Statuswechsel; ein zurückgerollter Vorgang hinterlässt kein Ereignis.
Die Texte entstehen aus typisierten Zuständen in der aktuellen Oberflächensprache.
Diese Ereignisse sind nur administrativ sichtbar, zählen weder als Nachrichten noch
als ungelesen und lösen keine zusätzlichen Glockenhinweise aus. Die Grenze von
50 Nachrichten pro Ticket gilt weiterhin für Nachrichten und Anleitungen, ohne History.
Liste und Detailkopf zeigen das unveränderte Erstelldatum; die Sortierung berücksichtigt
weiterhin die letzte relevante Aktivität.

Am administrativen Editor wählen native Tabs zwischen öffentlicher Antwort und
internem Vermerk. Entwurfstext und Modus bleiben beim Wechsel zwischen Tickets sowie
bei Live-Aktualisierung erhalten. Interne Vermerke erhalten die violette Systemfläche
und eine ausdrückliche Kennzeichnung. Der Sicherheitshinweis im internen Editor
verbindet das Verbot sensibler Inhalte mit der Information, dass nur Administratoren
diese Vermerke lesen können und die anfragende Person keinen Zugriff darauf hat. Die Nutzeransicht erhält weder diese Vermerke
noch die administrativen Systemereignisse oder deren Personenangaben.

Die Zuständigkeit erscheint nur in der Administration als Bearbeiteravatar mit
Name und E-Mail im Tooltip. Im Detailkopf stehen diese Angaben bei fremden
Bearbeitern auch sichtbar daneben. Eigene Zuweisungen tragen das grüne „Sie“-/
„You“-Badge; unzugewiesene Vorgänge zeigen „Nicht Zugewiesen“ kursiv.
In der Benutzeransicht werden keine Bearbeiternamen oder -adressen ausgegeben.

Administratoren können an eigenen zugewiesenen, offenen Tickets „Beobachten“
aktivieren. Superadmins können auch andere offene
Tickets abonnieren. Neue öffentliche Nachrichten lösen für berechtigte Abonnenten
außer dem Absender eine interne Benachrichtigung mit der Vorgangsnummer aus.
Sie verweist auf den Nachrichtenverlauf in der Administration. Interne Vermerke
sind nur in der Administration lesbar und lösen keine Abo-Benachrichtigung aus.
Ticketbezogene Benachrichtigungen speichern die öffentliche Vorgangsnummer.
Ihre Sprungziele verwenden beispielsweise `/admin?section=support&ticket=10003`:
den Bereich und die sichtbare Ticketnummer ohne Raute. Die Reihenfolge der Parameter
ist beliebig. Dieselbe URL lässt sich direkt eingeben. Die Ticketansicht wählt
ausschließlich einen für den angemeldeten Administrator zugänglichen Vorgang aus;
eine unbekannte Nummer hebt keine Rechte auf.
Beim Wechsel des Ticket-Tabs oder der Auswahl aktualisiert die Ansicht die URL
ohne Seitennavigation: `tab=all` beziehungsweise `tab=recovery` und, bei Auswahl,
`ticket=10003`. Ein Browser-Refresh stellt diesen Zustand wieder her. Ohne oder
mit ungültigem Tabparameter gilt „Alle Tickets“; ohne Ticketnummer bleibt die
Detailauswahl leer. Die native History-API ersetzt den aktuellen Eintrag, statt
für jeden Auswahlwechsel einen zusätzlichen Zurück-Schritt anzulegen.
„Zum Ticket“ öffnet für berechtigte Administratoren den zugehörigen Eintrag
in der bestehenden Ticketliste; Lesen und Übernehmen bleiben eigene Aktionen.

„Anleitung senden“ zeigt die Anleitung in der aktuellen Oberflächensprache vorab.
Ein Sprachwechsel aktualisiert auch die bereits geöffnete Vorschau. Beim Senden
wird die Anleitung in der gespeicherten Kontosprache des Empfängers als öffentliche
Antwort abgelegt und eine E-Mail an die bestehende
bestätigte Kontaktadresse in die Mailqueue aufgenommen. Queueaufnahme ist keine bestätigte
Zustellung. Ein fehlgeschlagener Queueauftrag bleibt wiederholbar. Die Anleitung
enthält keine neuen Codes. Freier Tickettext, Name, Geburtsdatum und ein E-Mail-Link
sind keine Wiederherstellungsnachweise. Ohne Authenticator und gültigen Ersatzcode
wird die Sperre nicht über das Ticketsystem aufgehoben.

Die Vorschau gliedert die Anleitung in vier nummerierte Schritte, den gelben
Sicherheitshinweis und eine separate Information zu den Grenzen der Hilfe.
Im Nachrichtenverlauf erscheint sie als kompakte Sprechblase mit aufklappbaren
Details. Der Öffnungszustand bleibt beim Aktualisieren erhalten. Der Verlauf zeigt
den tatsächlich versendeten und gespeicherten Text in der Kontosprache des Empfängers;
die Vorschau folgt unabhängig davon der aktuellen Sprache des bearbeitenden Admins.
Die Nachrichtenart `RECOVERY_INSTRUCTIONS` kennzeichnet ausschließlich die vom
Dienst versendete Anleitung; kopierter Text in einer normalen Antwort bleibt
eine normale Nachricht. Das aktuelle Schema enthält dafür `support_replies.kind`.

Der [Betreiber-Notfallweg](betrieb/2fa-notfall.md) betrifft ausschließlich den
Betreiber mit Server-/Datenbankzugriff. Er ist keine Supportantwort und wird nicht
an Benutzer versendet. Es existiert weder ein versteckter Benutzer noch eine zweite
Reparaturoberfläche.
