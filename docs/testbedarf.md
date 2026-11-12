# Offener Testbedarf

## Browser-Tests

Die Prüfung der offenen Fälle im freigegebenen Umfang gehört zur abschließenden
Nacht-Testrunde. Der Nachtlauf wurde ausdrücklich gestartet;
die Freigabe allein bedeutet keine bereits ausgeführten Prüfungen.

- Entwicklungskonten, **offen, Umsetzung vorhanden**: In DE/EN auf Desktop und
  Smartphone Superadmin-Schalter, Standardwerte, deaktivierte Verifikationsoption,
  Badge und Hinweise bei mailabhängigen Aktionen prüfen. Normale Admins können
  das Merkmal nicht setzen. Keine Versandzusage oder echte Mail für Testkonten
  mit diesem Merkmal; reguläre ungeprüfte Konten behalten ihre eigene Behandlung.

- Kopierbare Aktionslinks, **offen, alle Vorlagen umgesetzt**: Die betroffenen Mails
  in DE/EN isoliert in Mailpit auf Desktop-/Smartphonebreite öffnen. Original-URL
  unter dem Button lesbar und vollständig kopierbar, korrektes Ziel, kein Überlauf
  und keine leeren Linkbereiche in Mails ohne Aktion. Keine echten E-Mails senden.

- Deaktivierungsfristen in der Konfiguration, **offen, Umsetzung vorhanden** von
  [Nachtaufgabe 10](planung/nachtaufgaben.md#10-konfigurationsfelder-für-deaktivierungsfristen-vereinheitlichen):
  In DE/EN auf Desktop und Smartphone kurze Beschriftungen ohne Klammerzusätze,
  rechts integriertes Tages-Suffix wie bei Benachrichtigungen und die Feldhilfen
  prüfen. Erinnerung erklärt den Abstand vor dem Löschdatum und 0 als aus;
  „Wann gilt die Änderung?“ nennt künftige Selbstdeaktivierungen und unveränderte
  bestehende Termine. Die Felder stehen in der freigegebenen eigenen Rubrik
  „Kontoverwaltung“; nur Superadmins haben Zugriff, Admins und normale Benutzer nicht.

- Reaktivierungshinweis nach Anmeldung, **offen, Umsetzung vorhanden** von
  [Nachtaufgabe 8](planung/nachtaufgaben.md#8-selbst-deaktivierte-konten-nach-der-anmeldung-gezielt-zur-reaktivierung-führen):
  In DE/EN auf Desktop und Smartphone den entfernten Link im Loginformular und
  die eigene gelbe Hinweisseite nach gültigem ersten Anmeldenachweis prüfen.
  Löschdatum, Aktionen, erneuten Versand mit Wartezeit und Mailpit-Linkbestätigung
  abdecken. Passwort-/Anbieterwege, Refresh und direkte URLs prüfen: kein Zugriff
  auf geschützte Seiten und keine Aktivierung ohne gültigen Link. Administrative
  Sperren und abgelaufene Fristen erlauben keine Selbstreaktivierung.

- Kontostatus-E-Mails im Cosmos-Design, **offen, Umsetzung vorhanden** von
  [Nachtaufgabe 7](planung/nachtaufgaben.md#7-kontostatus-e-mails-in-das-bestehende-cosmos-maildesign-überführen):
  Deaktivierung, Löschwarnung, endgültige Löschung und erfolgreiche Reaktivierung
  isoliert in Mailpit auf Deutsch und Englisch bei Desktop-/Smartphonebreite
  prüfen. Erwartet sind der gemeinsame Mailstil, lesbare konkrete Fristen,
  Reaktivierungsbuttons bei Deaktivierung/Erinnerung sowie kopierbare Ziel-URLs
  ohne Überlauf. Löschbestätigungen bieten keine Reaktivierung an. Erneut
  angeforderte Links verwenden ebenfalls die neue Vorlage. Keine echten Mails.

- Konto deaktivieren/löschen – Übersicht und Bestätigungsdialoge, **offen, Umsetzung vorhanden**
  von [Nachtaufgabe 6](planung/nachtaufgaben.md#6-dialoge-zum-deaktivieren-und-endgültigen-löschen-angleichen):
  In DE/EN auf Desktop und Smartphone jeweils Passwort- und 2FA-Variante prüfen.
  In der Übersicht Tabler `user-cog` am Tab und die gelbe Warnhinweisfläche mit
  beiden gemeinsam rechtsbündig angeordneten Aktionen prüfen. Deaktivierung
  verwendet gelbe, Löschung rote Buttons und Icons; die jeweiligen Kopf-Icons
  im Dialog erhalten passende Rahmen und
  dezente Hintergründe bei unveränderten übrigen Dialogflächen.
  Erwartet sind der Untertitel zur Identitätsbestätigung, Tabler-Personen-Icons
  im Kopf und Bestätigungsbutton sowie die vollständigen Folgen der Aktion in
  der gelben beziehungsweise roten Hinweisbox. Der Passwort-Eingabehinweis endet
  mit einem Doppelpunkt und verwendet die regulären Textwerte. Abbrechen erhält
  das Konto; die Kontoaktion erfolgt weiterhin erst nach expliziter Bestätigung.
  Nach erfolgreicher Aktion erscheint auf der Anmeldeseite einmalig die richtige
  Erfolgsnotification. Technische Fehler zeigen dort eine Fehlernotification,
  keinen falschen Erfolg. Mailhinweise unterscheiden Queueaufnahme, Fehler und
  unterdrückten Versand bei Entwicklungskonten; Eingabefehler bleiben im Dialog.

- Erklärungstexte bei Identitätsbestätigungen, **offen, Prüfung mit der Nacht-Testrunde freigegeben**:
  In DE/EN auf Desktop und Smartphone prüfen, dass der einleitende Passwort-/
  Identitätshinweis die Standardtextgröße mit regulärem Schriftgewicht verwendet.
  Der Untertitel im Dialogkopf bleibt unverändert.
  Beim Einstieg in die Authenticator-Einrichtung erklärt der Paneluntertitel
  den vorgeschalteten Identitätsnachweis. Bei vorhandenem Portalpasswort fordert
  der Text über dem Feld zur Passworteingabe auf und endet mit einem Doppelpunkt.
  Die übergeordnete Seitenbeschreibung behält den allgemeinen Zweck der 2FA.

- E-Mail-Bestätigungsseite nach Stitch-Variante A, **offen, Prüfung mit der Nacht-Testrunde freigegeben**:
  Bestehende Suite `email-verification` in DE/EN bei Desktop- und Smartphonebreite
  nutzen. Vollständigen Seitenkopf mit Bereichszeile, Seitentitel und Beschreibung
  sowie die normale Panelposition wie auf den anderen öffentlichen Seiten erhalten.
  Die Seitenbeschreibung erklärt den Zweck der Bestätigung, der Paneluntertitel
  das erneute Anfordern des Links. Beide Texte folgen dem Sprachwechsel.
  Service-Logo und Sprachwechsel, Adresse
  samt Status-Badge, gelben Link-/Spamhinweis und statischen Wartezeithinweis prüfen.
  Der Linkhinweis und der ergänzende Spamhinweis stehen in zwei Absätzen.
  „Spam- oder Junkordner“ ist in beiden Sprachen fett hervorgehoben und folgt
  dem Sprachwechsel ebenso wie der übrige Text.
  Lange Adressen und Aktionsbeschriftungen müssen vollständig sichtbar bleiben.
  Beide gleich breiten Footerbuttons stehen rechtsbündig: „Abmelden“ rot mit
  Sign-out-Icon und „Senden“ blau mit Paperplane, auch nach Sprachwechsel.
  Abmelden mit Rückkehr zur Anmeldung, erneuten Versand mit tatsächlicher Wartezeit und die
  bestehende Portal-/Adminsperre einschließlich Refresh und Sprachwechsel erhalten.
  Keine unbestätigten Versand- oder Zustellungszeitpunkte anzeigen.

- Anmeldungsbereich im Benutzer-Bearbeitungsdialog, **offen, Prüfung mit der Nacht-Testrunde freigegeben**:
  In DE/EN auf Desktop und Smartphone Informationen mit bestehenden Status-Badges
  oberhalb der Trennlinie prüfen. Darunter stehen Bestätigungsmail und Passwortreset
  mit unveränderten Freigaben sowie ein blaues Info-Icon mit kurzer Erklärung
  beider Aktionen und Hinweis auf die gespeicherte Adresse.
  Buttons und lange Beschriftungen müssen bei Platzmangel sauber umbrechen.
  Die vorhandene Suite `email-verification` für ausstehenden, bestätigten und
  ausdrücklich ungeprüften Nachweis sowie Konten ohne Portalpasswort verwenden.
  Beim Speichern einer neuen E-Mail-Adresse automatisch eine Verifikationsmail an
  genau diese Adresse erwarten. „Nicht geprüft“ bleibt ohne Versand erhalten;
  unveränderte Adressen erzeugen keine neue Mail. Den manuellen Versand weiter prüfen.

- Headerlinks, **offen, Prüfung mit der Nacht-Testrunde freigegeben**:
  In Portal und Administration in DE/EN bei Desktop- und Smartphonebreite prüfen,
  dass der Profilbereich bei Hover transparent bleibt. Menü per Klick und Tastatur
  öffnen, sichtbaren Tastaturfokus erhalten und Menüaktionen weiterhin bedienen.
  Der Logo-Link zeigt bei Hover keinen Unterstrich, behält seine Textfarbe und
  verwendet wie im Besucherbereich den Handzeiger.
  Handzeiger auch über Header-Iconbuttons, Profilavatar, Pagination und den
  Benachrichtigungsfiltern einschließlich ihrer Zähler prüfen. Deaktivierte
  Seitenbuttons und reine Identitätsavatare dürfen keinen Handzeiger erhalten;
  Textfelder behalten den Textzeiger. Native Auswahlaktionen folgen demselben
  zentralen Cursorwert.
  Bei den als Segmente gestalteten Radiobuttons „Alle / Ungelesen / Gelesen“
  jeweils die freie Fläche, die Beschriftung und das Zahlen-Badge prüfen.

- Segmentierte Benachrichtigungsfilter, **wartend auf die geplante Umstellung**:
  Nach dem Wechsel von `RadioButtonGroup` zu nativen `Tabs` im Posteingang und
  in der Glockenvorschau Filterwirkung, Badge-Zähler, erhaltene Auswahl bei
  Textaktualisierung und korrekten Seitenwechsel prüfen. Vorher-/Nachher-Vergleich
  in DE/EN auf Desktop und Smartphone, einschließlich Tastatur, zugänglichen
  Beschriftungen und Handzeiger über Fläche, Text und Badge.
  [Geplantes Paket](planung/vaadin-komponenten-abgleich.md#segmentierte-benachrichtigungsfilter-vereinheitlichen).

- Formularschutz bei Browseraktionen, **offen, Umsetzung vorhanden** aus dem
  [Komponentenplan](planung/vaadin-komponenten-abgleich.md#ungespeicherte-formularänderungen).
  Persönliche Daten und die Speicherformulare der Konfiguration einschließlich
  Anbieter-/2FA-Einstellungen sowie das Benutzerformular mit ungespeicherten
  Eingaben prüfen. Szenarien sind F5/Strg+R, Browser-Aktualisieren, Tab-/Fenster-
  Schließen und Verlassen über Adresszeile oder externe Navigation.
  Erwartung: Eine Warnung erlaubt das Abbrechen mit erhaltenem Entwurf oder
  bewusstes Verlassen. Unveränderte und erfolgreich gespeicherte Formulare
  warnen nicht. Nach vollständigem Zurücknehmen der Änderungen ebenfalls nicht
  warnen; nach fehlgeschlagenem Speichern bei weiterhin geändertem Formular schon.
  Doppelte Warnungen beim anwendungsinternen Verwerfen oder Schließen vermeiden.
  Gezielt in DE/EN bei Desktop- und Smartphonebreite prüfen. Die Umsetzung ist
  als [Nachtaufgabe 5](planung/nachtaufgaben.md#5-formularschutz-um-browseraktionen-erweitern)
  umgesetzt; der vollständige DE/EN-Nachweis steht noch aus.

## JUnit-Tests

Keine offenen Ergänzungen aus den vereinbarten Nachtpaketen. Die Fach-,
Persistenz-, Vorlagen- und Rückmeldungstests sind im vollständigen Reactor bestanden.

Menschliche Abnahmen bleiben im
[manuellen Katalog](manual-tests/README.md).
