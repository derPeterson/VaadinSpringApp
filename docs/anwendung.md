# Anwendung

## Benutzerkonto

- Registrierung mit Vor-/Nachname, E-Mail und bestätigtem Passwort. Geschlecht und Geburtstag sind freiwillig.
- Lokale Registrierungen benötigen die Aktivierung über eine tatsächlich versandte E-Mail.
  Bei externen Registrierungen gelten die anbieterspezifischen Adressnachweise.
- Anmeldung mit E-Mail/Passwort sowie bei konfigurierten Anbietern mit Google oder Microsoft.
  Externe Registrierung benötigt kein zusätzliches Portalpasswort. Superadmins richten
  die Anbieter unter Konfiguration → Anmeldeanbieter ein und geben sie dort frei.
  Details und Einrichtung:
  [Externe Anmeldung](externe-anmeldung.md). Optional bleibt die gespeicherte Anmeldung.
  Der Switch „Login speichern“ merkt sich die letzte Wahl in diesem Browser für
  ein Jahr. Gespeichert wird nur die Schalterstellung, keine E-Mail und kein Passwort.
- Abmelden beendet die Sitzung. Die Startseite ist auch anonym erreichbar; Konto und
  Administration verlangen danach eine neue Anmeldung.
- Passwortreset über einen zeitlich begrenzten Einmallink. Anfragen geben keine Konten preis.
- Oberfläche und Systemmails unterstützen Deutsch und Englisch; die Kontosprache ist gespeichert.

Bei korrekter Anmeldung und noch ausstehender E-Mail-Bestätigung erscheint eine
eigene Hinweisseite. Sie nennt die Adresse, den Spamordner und die Möglichkeit,
die Bestätigung erneut anzufordern oder zur Anmeldung zurückzukehren. Bis zur
Bestätigung öffnet sich das Benutzerportal nicht.

Die Benutzerverwaltung legt Konten standardmäßig mit Bestätigungsmail an.
Die Checkbox „Verifikation“ ist standardmäßig ausgewählt und erklärt den Ablauf
mit einem Hilfetext. Nur Superadmins können sie bei der Anlage abwählen und das
Konto ausdrücklich ohne E-Mail-Nachweis bereitstellen. In den Benutzerdetails stehen Anlageweg und E-Mail-Nachweis
getrennt. Berechtigte Admins können dort Bestätigungsmails und für geeignete Konten
Passwortresetmails an die gespeicherte Adresse anfordern. Beim Speichern einer
geänderten Adresse fordert die Benutzerverwaltung automatisch einen neuen
Verifikationslink an. Konten mit „Nicht geprüft“ behalten ihre Ausnahme und erhalten
keine Verifikationsmail; administrative Sperren bleiben wirksam. Unveränderte Adressen
lösen keinen Versand aus. Der manuelle Button verwendet weiterhin die gespeicherte
Adresse. Ein Mailauftrag ist keine Zustellbestätigung.

Ein **Entwicklungskonto** ist davon unabhängig ausdrücklich gekennzeichnet. Nur
Superadmins können dieses Merkmal bei der Kontoanlage setzen; beim Anwendungsstart
ist es je Startkonto konfigurierbar. Es unterdrückt sämtliche kontobezogenen Mails,
auch Sicherheits- und Löschmails. Es ersetzt keine Rollen- oder Identitätsprüfung.
Mailabhängige Nachweise bleiben gesperrt. Für vollständige Mailabläufe ein reguläres
Testkonto verwenden. „Nicht geprüft“ allein unterdrückt diese Mails nicht.
Details: [Startkonten](entwicklung.md#e-mail-bestätigung-für-startkonten) und
[Versandvertrag](mailqueue.md#auftrag-und-versand).

## Mein Konto

Angemeldete Startseite, Postfach und Konto teilen Header, Identitätskarte und
seitliche Navigation. Die Startseite bietet den Kosmos-Ausblick mit echten
Schnellzugriffen und ungelesener Nachrichtenanzahl, ohne erfundene Statistiken.

`/account` bietet persönliche Daten, Sicherheit sowie Geräte und Sitzungen über eine
seitliche Kontonavigation. Auf Smartphones öffnet sie als Overlay.
`section=profile`, `section=security` und `section=sessions` halten den ausgewählten
Bereich in der URL. Unter Sicherheit merkt sich `tab=portal`, `tab=providers` oder
`tab=lifecycle` zusätzlich den Reiter, auch nach dem Neuladen. Ohne eindeutige
Auswahl öffnen sich das Profil beziehungsweise der Portalzugang. Alte Links mit `tab=security` und
den anderen bisherigen Bereichswerten bleiben nutzbar.

Im Profil lassen sich Namen, Geschlecht, Geburtstag und Sprache speichern.
Geschlecht und Geburtstag dürfen leer bleiben oder wieder geleert werden. Die E-Mail-Adresse
wird über den Sicherheitsablauf geändert. Parallele Änderungen werden mit einem
Versionskonflikt gemeldet, statt ein anderes geöffnetes Formular zu überschreiben.

Passwortwechsel verlangen den nach der aktuellen Regel erforderlichen,
zweckgebundenen Identitätsnachweis. Bei eingerichteter 2FA ist dies standardmäßig
ein aktueller Authenticator- oder Ersatzcode. Ohne entsprechende 2FA-Anforderung
bleiben die geschützten Passwort-/Anbieterwege erhalten. Ein Google-Durchlauf allein
genügt im normalen Betrieb nicht. Die eigene aktuelle
Sitzung bleibt erhalten; andere Zugänge werden widerrufen. Eine E-Mail-Änderung wird
an der neuen Adresse ausdrücklich bestätigt; ein bloßer Linkaufruf übernimmt sie
nicht. Danach sind neue Anmeldung und die Sicherheitsmails an die betroffenen
Adressen vorgesehen. Die Details stehen unter [Sicherheit](sicherheit.md).

Neben der E-Mail-Adresse zeigt ein natives Badge ihren Nachweis: „Verifiziert“
in Grün, „Bestätigung ausstehend“ in Gelb oder „Nicht geprüft“ in Grau. Der graue
Zustand bezeichnet ausdrücklich ohne Nachweis bereitgestellte Konten und erklärt
dies im Tooltip. Dieselben Angaben stehen in den administrativen Benutzerdetails.

Im Tab **Portalzugang** lässt sich ein Authenticator einrichten, wechseln oder
entfernen. Neue Ersatzcodes werden einmalig zum Kopieren und als druckbare PDF
angeboten. Eine verpflichtende Einrichtung bzw. Bestätigung erfolgt vor dem
vollständigen Kontozugriff. Auch eine gespeicherte Anmeldung überspringt diesen
Schritt nicht. Details: [Zwei-Faktor-Authentifizierung](zwei-faktor-authentifizierung.md).

Der Tab **Konto verwalten** bietet die vorübergehende Deaktivierung und die
endgültige Löschung. Beide verlangen einen ausdrücklichen Klick nach dem
Identitätsnachweis. Nach erfolgreichem Abschluss erscheint auf der Anmeldeseite
eine einmalige Rückmeldung. Für selbst deaktivierte Konten führt eine gültige
Anmeldung stattdessen zur Reaktivierungshilfe mit festem Löschdatum. Administrative
Sperren erlauben keine Selbstreaktivierung.
In der Benutzerverwaltung heißt der unabhängige administrative Schalter
„Zugang gesperrt“. Unter dem standardmäßig eingeklappten Bereich „Anmeldung“
zeigen selbst deaktivierte Konten ein Status-Badge und ihren endgültigen Löschtermin.
Die Anzeige ist schreibgeschützt. Aufklappen verändert keine Formulardaten.
Fristen, Mailversand und Löschumfang stehen unter
[Kontodeaktivierung und Löschung](sicherheit.md#eigenes-konto-deaktivieren-und-löschen).

Beim Passwort zeigt ein natives Badge „Eingerichtet“ in Grün oder „Nicht eingerichtet“
in Blau. Ein ausschließlich über Google oder Microsoft angelegtes Konto hat zunächst
kein Portalpasswort. Die Aktion heißt dann „Einrichten“, sonst „Ändern“. Sie führt
durch den bestehenden zweckgebundenen Identitätsnachweis. Das Passwort beim Anbieter
wird dadurch nicht verändert. Erst nach erfolgreichem Speichern wechseln Badge und
Aktion zum eingerichteten Zustand.

Geräte und Sitzungen zeigen aktive Zugänge und gespeicherte Anmeldungen mit
Gerätebeschreibung. Einzelne oder alle anderen Zugänge können widerrufen werden.
Die jeweilige Bestätigung nennt die betroffene Sitzung beziehungsweise gespeicherte
Anmeldung. Abbrechen führt keinen Widerruf aus; die aktuelle Sitzung bleibt geschützt.
Bei erreichtem Sitzungslimit gibt es nach Passwort- oder Anbieterprüfung einen eingeschränkten
Verwaltungszugang zum Freigeben eines Platzes; er öffnet keine normalen Fachaktionen.

## Ungespeicherte Formularänderungen

Persönliche Daten, die Speicherformulare der Konfiguration einschließlich
Anmeldeanbietern und 2FA sowie die Benutzerformulare warnen vor dem Verlust
geänderter Eingaben. Innerhalb der Anwendung erscheint der gemeinsame
Bestätigungsdialog. Beim Neuladen, Schließen oder externen Verlassen übernimmt
der Browser seinen eigenen Warnhinweis. Dessen Wortlaut und Aussehen bestimmt
der Browser.

Unveränderte, erfolgreich gespeicherte oder vollständig zurückgesetzte Formulare
warnen nicht. Ein fehlgeschlagener Speicherversuch entfernt den Schutz nicht,
solange Änderungen bestehen. Suchfelder, Filter und Codeeingaben gehören nicht
zu diesem Schutz. Er ist kein automatisches Speichern und greift nicht zuverlässig
bei Browserabsturz, Verbindungsverlust oder erzwungenem Beenden auf Mobilgeräten.

## Profilbild und Verfügbarkeit

Gemeinsame Avatare zeigen Initialen oder ein eigenes Profilbild. Änderungen an Namen,
Bild und Status erscheinen in den angebundenen Ansichten. Statussymbole kennzeichnen
verfügbar, beschäftigt, abwesend und offline; manuelle Wahl und automatische
Abwesenheit haben getrennte Regeln.

Der Bilddialog akzeptiert JPEG und PNG per Dateiauswahl oder Drag-and-drop, bietet Ausschnitt und Zoom und speichert
ein bereinigtes 512×512-PNG. Initialen können gewählt werden, ohne das gespeicherte
Bild zu löschen. Bestätigtes Löschen wird beim Speichern übernommen; Abbrechen verwirft den Entwurf.
Für einen neuen Ausschnitt wählen Sie erneut eine Bilddatei vom Gerät aus.

Die Uploadgrenzen sind 5 MiB, 8192 Pixel je Kante und 20 Megapixel; das Ergebnis
ist auf 1 MiB begrenzt. Originaldateien, Originalnamen und EXIF-Daten werden nicht dauerhaft archiviert.
In `user_avatar` liegen neben dem bereinigten PNG der serverseitige Uploadzeitpunkt
(`uploaded_at`) und die erkannten Originalangaben: `original_format` (JPEG/PNG),
`original_size_bytes`, `original_width` und `original_height`. Die Rastermaße
beschreiben das Original vor Ausschnitt und EXIF-Ausrichtung. Diese Angaben
werden erst mit dem erfolgreichen Speichern des Bildes dauerhaft übernommen.
Der Wechsel zu Initialen erhält sie. Ersetzen aktualisiert sie, Löschen entfernt sie
zusammen mit dem Bild. Es gibt keine Bild- oder Metadatenhistorie. Eine Datenbankprüfung
verhindert unvollständige Bildmetadaten. Größe und Format des gespeicherten Ergebnisses
sind weiterhin 512×512/PNG, seine Bytegröße ergibt sich aus `image_data`.
Uploadentwürfe sind zeitlich und mengenmäßig begrenzt. Ein verspäteter Upload darf
einen geschlossenen oder wieder geöffneten Dialog nicht verändern.

## Benachrichtigungen

Die Kopfzeile bietet eine Vorschau; `/notifications` öffnet den vollständigen
Posteingang. Beide haben die Auswahl Alle/Ungelesen/Gelesen mit tatsächlichen Zählern. Die Inbox ergänzt Suche,
Seitenwahl, Gesamt-/Ungelesen-Zähler, Aktualisieren und eine Sammel-Leseaktion.

Öffnen ändert den Lesestatus nicht. Jede Karte bietet Öffnen und Als gelesen markieren
als Iconbuttons sowie einen Papierkorb zum persönlichen Löschen. Metadaten und Aktionen
stehen in der Inbox gemeinsam über dem Nachrichteninhalt, in der kompakten Vorschau
unter dem Text. Lesen und Löschen verlangen eine ausdrückliche Bestätigung mit dem
betroffenen Kontext. Die vollständige Inbox kann **gelesene** Hinweise
gesammelt entfernen; die Header-Vorschau bietet diese Sammellöschung nicht.
Die Sammel-Lesebestätigung verwendet die beim Öffnen erfassten ungelesenen IDs;
zwischenzeitlich neue Hinweise werden davon nicht erfasst.

Superadmins versenden Hinweise an ausgewählte Konten oder alle berechtigten Empfänger.
Vorschau, ausdrückliche Bestätigung und Vorgangsschlüssel sichern den Versand gegen
unbeabsichtigte Wiederholung. Das Versandarchiv ist von der persönlichen Inbox getrennt.
Interne Mitteilungen werden innerhalb der Anwendung versendet, nicht automatisch per E-Mail.

Passwortwechsel, Passwortreset, Rollenänderung und bestätigte E-Mail-Änderung können
persönliche Systemhinweise erzeugen. Die Inhalte enthalten keine Kennwörter,
Tokenlinks, Adressen oder Sitzungskennungen. Systemtexte werden beim Anzeigen übersetzt.
Der Detaildialog bietet den Sprung zur Kontosicherheit für diese
Kontosicherheitsmeldungen. Bei Ticketmeldungen und administrativen Mitteilungen
erscheint dieser Button nicht.
„Als gelesen markieren“ steht direkt an der Nachricht. Im Footer öffnet
„Weitere Aktionen“ eine native Popover-Buttonliste für die vollständige Übersicht
und das persönliche Löschen. „Schließen“ ist neutral. Ticketmeldungen mit einer
Vorgangsnummer bieten berechtigten Administratoren „Zum Ticket“ als Hauptaktion,
die das passende Ticket in der Administration auswählt. Eine nicht verfügbare
Nummer führt zu einem Hinweis und wählt kein anderes Ticket aus. Auf schmalen
Displays liegt die Zielaktion in einer eigenen vollen Zeile.

Inbox- und Historienaufbewahrung sind getrennt konfiguriert: standardmäßig 180 und
365 Tage. Ein leeres Feld bedeutet unbegrenzt; sonst sind positive Tageswerte nötig.
Persönliches Entfernen löscht keine Nachweise oder Hinweise anderer Empfänger.

## Administration

Admins verwalten Benutzer, Konfiguration und den Mailtest. Superadmins verwalten
zusätzlich Superadmins, den internen Mitteilungsversand, Anmeldeanbieter und die
rollenbezogenen Zwei-Faktor-Regeln. Die letzte aktive
Superadminberechtigung ist geschützt; ein Admin darf sich keine höheren Rechte geben.

Die vollständige Einordnung steht unter [Rollen und Berechtigungen](rollen.md):
zusätzliche Rechte pro Rolle, gemeinsame Kontofunktionen und geltende Ausnahmen.

**Wiederherstellungsanfragen** bieten eine kleine gemeinsame Vorgangsliste mit
Übernahme, öffentlichen Antworten, internen Vermerken und vorbereiteter Anleitung.
Nach dem ersten Anmeldenachweis können ausgesperrte Benutzer ausschließlich ihre
eigenen Anfragen erreichen. Eine Antwort oder ein Ticketabschluss setzt keinen
Authenticator zurück und gibt keinen gesperrten Kontozugriff frei.

Die Benutzerverwaltung bietet sofortige Namens-/E-Mail-Suche, Sortierung und Paging.
Weitere Filter sind Entwürfe bis zum Anwenden; Badges zeigen angewandte Kriterien.
Reset leert Entwürfe und aktive Kriterien. Datum und Uhrzeit bilden einen gemeinsamen
Aktivitätsfilter; Uhrzeit ist ohne gültiges Datum deaktiviert.

Unter „Wann gilt die Änderung?“ erklärt jede Feldhilfe in der Konfiguration den
Zeitpunkt und relevante Folgen für bestehende Sitzungen, Einträge oder laufende
Vorgänge. Das gilt auch für Anmeldeanbieter und Zwei-Faktor-Regeln. Der Schlüssel
für gespeicherte Anmeldungen braucht einen Neustart. Die gelbe Feldhilfe erklärt
das. Nach einer Änderung nennt die Speichermeldung die betroffenen Felder.
Der Mailtest sendet eine echte SMTP-Testmail; in der lokalen Docker-Umgebung fängt
Mailpit sie ab. [Mailqueue](mailqueue.md) und [Docker](docker/README.md).

Adminbereiche stehen in der URL, z. B. `/admin?section=user_management` oder
`/admin?section=configuration&group=security`. Refresh sowie Zurück/Vorwärts stellen
den Bereich wieder her. Berechtigungsprüfungen gelten bei jedem Zugriff.

## Datumseingabe

Datumsfelder akzeptieren normale Formate sowie vier, sechs oder acht kompakte Ziffern.
DE verwendet Tag/Monat, EN Monat/Tag. Zweistellige Jahre liegen zwischen 1950 und 2049.
Übernahme erfolgt mit Tab/Enter oder beim Verlassen; ungültige Tage werden abgelehnt.
Geburtstage müssen vor dem heutigen Tag liegen. Der Aktivitätsfilter akzeptiert
`930` als 09:30 und `1430` als 14:30; angezeigt wird sprachabhängig.
