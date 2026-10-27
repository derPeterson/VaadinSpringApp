# Benutzerdialoge und Feldicons – Vorschlag zur Sichtung

Nach Benutzerentscheidung sind die Feldicon-Regel in allen drei HTML-Referenzen
und die getrennten Admin-Passworthinweise übernommen. Die übrige Dialoganordnung
bleibt beim bestehenden Stand. Platzhalter verwenden den Anwendungskatalog,
der Rollenhinweis steht direkt bei der Auswahl. Das grüne Badge „Dein Benutzer“
steht unter dem Namen und übernimmt den Stil von „Dieses Gerät“ mit Punkt.
Maßgeblich sind `admin.html` und `admin-users.html`. Der Löschdialog wurde
nicht verändert.

Der aktuelle, skriptfreie Dialogstand wurde mit ausschließlich synthetischen Daten
in das private Stitch-Projekt importiert:
[Ist-Stand](https://stitch.google.com/projects/14011270174485926515?node-id=5973307835346129426).
Darauf basiert der neue
[Vorschlag für Anlegen/Bearbeiten und Iconvergleich](https://stitch.google.com/projects/14011270174485926515?node-id=0c0884945554444490a1bd57df765563).
Metadaten wurden unabhängig erneut abgerufen, Original-HTML und Bild heruntergeladen
und tatsächlich geprüft. Bereinigter Import, Prompt, Rohcode/Bild und begründete
Antwort liegen temporär unter `target/designkritik-stitch/portal/stitch/user-editor-*`.
Es wird kein neuer Inspirationsordner angelegt.

Die lokale Sichtung unter `preview/user-editor-vorschlag.html` bietet Hinzufügen,
Bearbeiten und Grau/Cyan als getrennte Ansichten mit Desktop-/Smartphonegrößen.
Die Dialoge stammen aus Stitchs Original-HTML. Lokal sind nur die vorhandene Inter-
Schrift eingebettet, die äußere Vorschau ergänzt und kleine Bildschirmränder
angepasst. Ein nicht belegter Berechtigungstooltip wurde entfernt; eine erfundene
Systemversionszeile gehört nicht zur Vorschau. Der Icontext wurde nach Stitchs
anschließender Klärung präzisiert. Die unveränderte Originalausgabe bleibt erhalten.

Stitch behält die wesentliche Formularanordnung bei. Passwort-Hinweise stehen
direkt bei den jeweiligen Eingaben; der Sicherheitshinweis wird in einer eigenen
Box gruppiert. Beim Anlegen nennt die Primary-Aktion die konkrete Aktion.
Alle vorhandenen Felder und vier Rollen sind enthalten. Das neue Konto startet
inaktiv und ohne Rolle; Passwortbestätigung ist zunächst deaktiviert. Im
Bearbeitungsbeispiel bleiben Alex Berger, Divers, Geburtsdatum, Aktivierung und
Rolle Benutzer erhalten. Superadmin bleibt in beiden Beispielen deaktiviert.

Seine geklärte Empfehlung für Feldicons lautet: neutral grau im Ruhezustand,
heller bei Hover, Cyan bei Fokus oder geöffnetem Picker, gedämpft bei Disabled.
Die bereits etablierten cyanfarbenen Icons normaler Secondary-Aktionen bleiben
davon ausdrücklich ausgenommen. Genau diese Feldregel ist inzwischen für die
HTML-Referenzen freigegeben und eingepasst; keine Änderung am Anwendungstheme.

Grenzen: statischer Layoutvorschlag. Native Selects, nicht angebundene Kalender-
und Passwortaktionen sowie die konkreten Close-/Buttonglyphen ersetzen nicht
die abgestimmten Referenzkomponenten. Vor einer Übernahme müssen diese aus dem
Bestand verwendet und die tatsächlichen Validierungs-/Rechteverträge erhalten
werden. Keine neuen Fachregeln aus generierten Texten übernehmen.

Geprüft: Auswahl aller drei Ansichten bei Desktop, 390×844 und 844×390 ohne
horizontalen Seitenüberlauf. Markierungen und Ausgangsbeispiele wurden aus dem
HTML kontrolliert. Keine Funktions- oder Designabnahme der vorgeschlagenen Dialoge.

## Abgestimmte Entscheidung: linke Feldsymbole

Stitch wurde zusätzlich gezielt zu den Präfixen der tatsächlichen Registrierung
befragt (Person, Briefumschlag, Schloss, Personengruppe, Kalender). Seine klare
Empfehlung ist, rein dekorative linke Symbole in beschrifteten Formularfeldern
durchgängig wegzulassen. Rechte Bedienicons für Passwortanzeige, Kalender und
Auswahllisten bleiben erhalten. Suchfelder behalten die Lupe als Suchkennzeichen.
Ein dekorativer Kalender links zusätzlich zum Kalenderknopf rechts wäre doppelt.
Falls Präfixe gewählt würden, müssten sie auch bei vorhandenen Werten sichtbar
bleiben. Frage/Originalantwort liegen unter `stitch/field-prefix-*` in der
temporären Spielwiese. Die Benutzerfreigabe ist inzwischen in allen drei
Referenzen umgesetzt: elf dekorative Brief-/Schlosssymbole entfernt,
Textabstände bereinigt; Suchlupen und rechte Bedienicons bleiben erhalten.
