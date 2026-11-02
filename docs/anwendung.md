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

## Mein Konto

Angemeldete Startseite, Postfach und Konto teilen Header, Identitätskarte und
seitliche Navigation. Die Startseite bietet den Kosmos-Ausblick mit echten
Schnellzugriffen und ungelesener Nachrichtenanzahl, ohne erfundene Statistiken.

`/account` bietet persönliche Daten, Sicherheit sowie Geräte und Sitzungen über eine
seitliche Kontonavigation. Auf Smartphones öffnet sie als Overlay.
`?tab=profile`, `?tab=security` und `?tab=sessions` halten den ausgewählten Bereich
in der URL; unbekannte oder mehrdeutige Werte öffnen das Profil.

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

Im Tab **Portalzugang** lässt sich ein Authenticator einrichten, wechseln oder
entfernen. Neue Ersatzcodes werden einmalig zum Kopieren und als druckbare PDF
angeboten. Eine verpflichtende Einrichtung bzw. Bestätigung erfolgt vor dem
vollständigen Kontozugriff. Auch eine gespeicherte Anmeldung überspringt diesen
Schritt nicht. Details: [Zwei-Faktor-Authentifizierung](zwei-faktor-authentifizierung.md).

Geräte und Sitzungen zeigen aktive Zugänge und gespeicherte Anmeldungen mit
Gerätebeschreibung. Einzelne oder alle anderen Zugänge können widerrufen werden.
Die jeweilige Bestätigung nennt die betroffene Sitzung beziehungsweise gespeicherte
Anmeldung. Abbrechen führt keinen Widerruf aus; die aktuelle Sitzung bleibt geschützt.
Bei erreichtem Sitzungslimit gibt es nach Passwort- oder Anbieterprüfung einen eingeschränkten
Verwaltungszugang zum Freigeben eines Platzes; er öffnet keine normalen Fachaktionen.

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

Inbox- und Historienaufbewahrung sind getrennt konfiguriert: standardmäßig 180 und
365 Tage. Ein leeres Feld bedeutet unbegrenzt; sonst sind positive Tageswerte nötig.
Persönliches Entfernen löscht keine Nachweise oder Hinweise anderer Empfänger.

## Administration

Admins verwalten Benutzer, Konfiguration und den Mailtest. Superadmins verwalten
zusätzlich Superadmins, den internen Mitteilungsversand, Anmeldeanbieter und die
rollenbezogenen Zwei-Faktor-Regeln. Die letzte aktive
Superadminberechtigung ist geschützt; ein Admin darf sich keine höheren Rechte geben.

**Wiederherstellungsanfragen** bieten eine kleine gemeinsame Vorgangsliste mit
Übernahme, öffentlichen Antworten, internen Vermerken und vorbereiteter Anleitung.
Nach dem ersten Anmeldenachweis können ausgesperrte Benutzer ausschließlich ihre
eigenen Anfragen erreichen. Eine Antwort oder ein Ticketabschluss setzt keinen
Authenticator zurück und gibt keinen gesperrten Kontozugriff frei.

Die Benutzerverwaltung bietet sofortige Namens-/E-Mail-Suche, Sortierung und Paging.
Weitere Filter sind Entwürfe bis zum Anwenden; Badges zeigen angewandte Kriterien.
Reset leert Entwürfe und aktive Kriterien. Datum und Uhrzeit bilden einen gemeinsamen
Aktivitätsfilter; Uhrzeit ist ohne gültiges Datum deaktiviert.

Konfigurationsfelder zeigen Standardwert und Regeln im Info-Tooltip. Gespeicherte
Werte haben Vorrang vor Startvorgaben. SMTP, Remember-me und Workerzahl sind
startgebunden; andere fachliche Einstellungen wirken beim nächsten relevanten Zugriff.
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
