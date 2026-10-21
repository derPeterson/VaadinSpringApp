# Anwendung

## Benutzerkonto

- Registrierung mit Vor-/Nachname, E-Mail, bestätigtem Passwort, Geschlecht und Geburtstag.
- Neue Registrierungen benötigen die Aktivierung über eine tatsächlich versandte E-Mail.
- Anmeldung mit E-Mail und Passwort; optional gespeicherte Anmeldung mit Remember-me.
- Abmelden beendet die Sitzung. Die Startseite ist auch anonym erreichbar; Konto und
  Administration verlangen danach eine neue Anmeldung.
- Passwortreset über einen zeitlich begrenzten Einmallink. Anfragen geben keine Konten preis.
- Oberfläche und Systemmails unterstützen Deutsch und Englisch; die Kontosprache ist gespeichert.

## Mein Konto

`/account` enthält die Tabs persönliches Profil, Sicherheit sowie Geräte und Sitzungen.
`?tab=profile`, `?tab=security` und `?tab=sessions` halten den ausgewählten Bereich
in der URL; unbekannte oder mehrdeutige Werte öffnen das Profil.

Im Profil lassen sich Namen, Geburtstag und Sprache speichern. Die E-Mail-Adresse
wird über den Sicherheitsablauf geändert. Parallele Änderungen werden mit einem
Versionskonflikt gemeldet, statt ein anderes geöffnetes Formular zu überschreiben.

Passwortwechsel verlangen eine aktuelle Passwortbestätigung. Die eigene aktuelle
Sitzung bleibt erhalten; andere Zugänge werden widerrufen. Eine E-Mail-Änderung wird
an der neuen Adresse ausdrücklich bestätigt; ein bloßer Linkaufruf übernimmt sie
nicht. Danach sind neue Anmeldung und die Sicherheitsmails an die betroffenen
Adressen vorgesehen. Die Details stehen unter [Sicherheit](sicherheit.md).

Geräte und Sitzungen zeigen aktive Zugänge und gespeicherte Anmeldungen mit
Gerätebeschreibung. Einzelne oder alle anderen Zugänge können widerrufen werden.
Bei erreichtem Sitzungslimit gibt es nach Passwortprüfung einen eingeschränkten
Verwaltungszugang zum Freigeben eines Platzes; er öffnet keine normalen Fachaktionen.

## Profilbild und Verfügbarkeit

Gemeinsame Avatare zeigen Initialen oder ein eigenes Profilbild. Änderungen an Namen,
Bild und Status erscheinen in den angebundenen Ansichten. Statussymbole kennzeichnen
verfügbar, beschäftigt, abwesend und offline; manuelle Wahl und automatische
Abwesenheit haben getrennte Regeln.

Der Bilddialog akzeptiert JPEG und PNG, bietet Ausschnitt und Zoom und speichert
ein bereinigtes 512×512-PNG. Initialen können gewählt werden, ohne das gespeicherte
Bild zu löschen. Löschen entfernt das Bild; Abbrechen verwirft den Entwurf.
Für einen neuen Ausschnitt muss das Original erneut hochgeladen werden.

Die Uploadgrenzen sind 5 MiB, 8192 Pixel je Kante und 20 Megapixel; das Ergebnis
ist auf 1 MiB begrenzt. Originale und Metadaten werden nicht dauerhaft archiviert.
Uploadentwürfe sind zeitlich und mengenmäßig begrenzt. Ein verspäteter Upload darf
einen geschlossenen oder wieder geöffneten Dialog nicht verändern.

## Benachrichtigungen

Die Kopfzeile bietet eine Vorschau; `/notifications` öffnet den vollständigen
Posteingang. Beide haben die Auswahl Alle/Gelesen/Ungelesen. Die Inbox ergänzt Suche,
Seitenwahl, Gesamt-/Ungelesen-Zähler, Aktualisieren und eine Sammel-Leseaktion.

Öffnen ändert den Lesestatus nicht. Jede Karte bietet Öffnen und Als gelesen markieren
als Iconbuttons sowie ein X zum persönlichen Entfernen. Metadaten und Aktionen stehen
gemeinsam in der letzten Zeile. Die vollständige Inbox kann **gelesene** Hinweise
gesammelt entfernen; die Header-Vorschau bietet diese Sammellöschung nicht.

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
zusätzlich Superadmins und den internen Mitteilungsversand. Die letzte aktive
Superadminberechtigung ist geschützt; ein Admin darf sich keine höheren Rechte geben.

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
