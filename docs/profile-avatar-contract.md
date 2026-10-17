# Profilbilder und gemeinsame Avatare

Stand: 17.10.2026, `feature/account-avatar-preview`. Die Speicherfunktion ist umgesetzt;
die neue Browserreise wartet auf die ausdrückliche Freigabe und die menschliche Abnahme.

## Bedienung

„Profilbild bearbeiten“ öffnet den bestehenden Cosmos-Dialog. JPG/PNG auswählen,
Ausschnitt ziehen oder per Pfeiltasten verschieben, zwischen 100 und 300 Prozent
zoomen und speichern. Fehler und Erfolg erscheinen als übersetzte Notifications.
Es gibt keine Designvorschau-Meldung und keinen wirkungslosen Speicherbutton mehr.

| Aktion | Ergebnis nach Speichern |
| --- | --- |
| Neues Bild auswählen | Nur den bereinigten Ausschnitt als 512×512-PNG speichern; vorheriges Bild ersetzen |
| Standard/Initialen | Initialen anzeigen; das eine gespeicherte Bild behalten |
| Eigenes Bild | Vorhandenes gespeichertes Bild wieder anzeigen |
| Bild löschen | Bilddaten und Bildrevision entfernen; Initialen aktivieren |
| Abbrechen, Escape, Außenklick oder Schließen | Entwurf verwerfen; gespeicherten Stand behalten |

Ein gespeichertes Bild dient beim erneuten Öffnen zum Umschalten und zur Anzeige.
Für einen neuen Ausschnitt das Original erneut auswählen: das unbeschnittene Original
wird nicht dauerhaft aufgehoben. Während eines laufenden Speichervorgangs sind
weitere Aktionen kurz gesperrt. Speichern ohne gültiges eigenes Bild wird abgelehnt.
Ein zwischenzeitlich geänderter Avatar führt zu einer Konfliktmeldung und wird nicht
überschrieben. Profilfelder und Avatar haben unabhängige Versionen.

## Darstellung und Aktualisierung

`OverlayUserIcon` wird in Home-/Admin-/Konto-Kopfzeile, Benutzermenü,
Kontozusammenfassung, Benutzertabelle und Löschdialog verwendet. Die bisherigen
48/96/72/32-Pixel-Größen bleiben erhalten. Initialen stammen aus je einem Unicode-
Zeichen von Vor- und Nachname; ohne Namen erscheint das neutrale Benutzerzeichen.
Farben, Rahmen und Schimmer stehen in `cosmos-tokens.css` unter `--cosmos-avatar-*`.

Die Avatare oben rechts, im Benutzermenü und in „Mein Konto“ zeigen Haken, Minus, Uhr oder
Kreuz als separate Statusanzeige. Avatarzeichen, Dropdowneinträge und Auswahlpräfix
verwenden dieselben SVG-Kontursymbole ohne farbige Vollfüllung. Kopfzeilenavatar,
Benutzerpopup und Kontobild übernehmen die Statusfarbe auch für Rahmen und kräftigen Glow; die
übrigen Profilbildflächen behalten den neutralen Standardrahmen. Die vier Schatten
stehen zentral in `cosmos-tokens.css`: Variante „Kräftig“ mit 6 px bei 80 %
Deckkraft und 26 px bei 60 %.

Am Kontobild ist das Statuszeichen direkt bedienbar:
Eine native Vaadin-Auswahl liegt mit etwas größerer Klickfläche über dem
Statuszeichen. Die Konturindikatoren sind 30 Prozent der Avatargröße groß
(14/22/29 px bei 48/72/96 px); die drei Statusavatare haben 2 px Rahmenbreite.
Indikator und Klickfläche liegen mit ihrem Mittelpunkt bei 45 Grad auf der Mitte
der Kreislinie; die Position berücksichtigt die Rahmenstärke und eine optische
Korrektur von 1,5 px nach außen. Die SVGs sind ohne Inline-Grundlinienversatz als
Block dargestellt.
Klick bzw. Tastatur öffnen dieselben Statusoptionen wie im Benutzermenü. Der
Avatar-Tooltip ist am interaktiven Kontobild ausgeschaltet; die zugängliche
Beschriftung von Bildgruppe und Auswahl bleibt erhalten.
Beide Statuslisten verzichten auf Vaadins zusätzlichen Auswahlhaken. Gewählter,
gehoverter und fokussierter Eintrag verwenden dieselbe zentrale Hervorhebungsfläche;
der native Auswahlzustand und die Tastaturbedienung bleiben erhalten. Andere
Selects sind davon nicht betroffen.

Die Auswahl speichert sofort über dessen vorhandene Aktion, unabhängig vom
Profilformular. Fehler stellen den vorherigen Status wieder her und verwenden die
bestehenden übersetzbaren Notifications. Menü, Kopfzeile und Kontobild synchronisieren
sich auch bei externen Statusänderungen. Die Profilbildquelle bleibt dabei erhalten.

Profilbilddialog, Benutzertabelle und Löschdialog zeigen weiterhin
nur Bild bzw. Initialen. Das interaktive Kontobild verwendet eine zugängliche Gruppe
mit beschriftetem Select; der übrige Bildbereich öffnet keine Statusauswahl. Nach erfolgreichem Commit verteilt
`UserProfileBroadcaster` Bild-/Modus- und gespeicherte Namensänderungen über den
bestehenden `UI.access`-/Push-Ansatz. Listener werden beim Verlassen abgemeldet.
Konto-Profilentwürfe werden dabei nicht durch neue Formularwerte überschrieben.
Der Broadcaster gilt für eine Anwendungsinstanz; für mehrere Instanzen wäre später
ein gemeinsamer Ereigniskanal nötig. Nach Refresh kommt der Stand aus der Datenbank.

## Daten und Sicherheitsregeln

Die additive Tabelle `user_avatar` enthält genau eine Zeile je betroffenem Konto:
Konto-Fremdschlüssel, eigene JPA-Version, INITIALS/CUSTOM, Bildvorhandensein,
Bildrevision und höchstens ein PNG-BLOB. Bestandskonten ohne Zeile verwenden
Initialen. Gewöhnliche UserEntity-/Statusabfragen laden keine Bilddaten; die
Benutzertabelle lädt Metadaten für ihre sichtbare Seite gesammelt. Die Datenbank-
Fremdschlüsselregel entfernt den Avatar auch bei Kontolöschung. Keine Originale,
Bildarchive, Dateinamen, EXIF-/GPS-Daten oder zusätzlichen Statusbildkopien speichern.

`UserAvatarService` bestimmt das Zielkonto aus der aktuellen Anmeldung. Vor dem
Upload und beim eigentlichen Speichern werden Sitzung, Kontofreigabe und Wartung
neu geprüft; Bildverarbeitung erfolgt vor der kurzen gesperrten Schreibtransaktion.
Gäste und normale Konten dürfen ihr eigenes Profilbild verwalten. Fremde Bilder
sind nur für bereits berechtigte Admin-/Superadmin-Anmeldungen lesbar.

`AvatarImageResource` verwendet Vaadins sitzungsgebundene DownloadHandler und prüft
bei jedem tatsächlichen Abruf erneut die Berechtigung und Bildrevision. Alte URLs
sind keine Berechtigung. Antworten: `image/png`, fester Inline-Name `avatar.png`,
`Cache-Control: private, no-store`, `nosniff` und restriktive CSP. Keine Bilddaten bei
401/403/404/503. Bei Ladefehlern fällt der Avatar auf Initialen zurück. Bereits
empfangene Bilder lassen sich nicht aus privaten Kopien des Empfängers entfernen.

## Grenzen und Upload-Lebenszyklus

- JPG/PNG, maximal 5 MiB tatsächlich gelesene Bytes, 8192 Pixel je Kante und 20 Megapixel.
- Kein SVG, GIF, animiertes PNG, WebP oder HEIC/HEIF; keine automatische HEIC-Konvertierung.
- RAW-Upload über Vaadin; der Handler begrenzt den Stream selbst und vertraut weder
  Dateiname, Browserfilter, MIME-Behauptung noch deklarierter Länge.
- Bildabmessungen vor vollständigem Decode prüfen, EXIF-Orientierung berücksichtigen,
  Ausschnitt/Zoom serverseitig validieren und Raster neu als PNG codieren; maximal 1 MiB Ergebnis.
- Höchstens zwei gleichzeitige Bildverarbeitungen und zwei Upload-Streams. Pro Entwurf
  nur ein Transfer; globale Kapazität maximal 16 Entwürfe mit jeweils einem Original.
- Uploadentwürfe liegen im begrenzten Anwendungsspeicher, höchstens zehn Minuten.
  Die minütliche Bereinigung entfernt abgelaufene Einträge. Schließen/Abbruch,
  erfolgreiches Speichern und UI-Detach geben den Entwurf frei. Keine Tempdateiarchive.
- Dialogtoken und Bildkennung binden Upload/Ausschnitt zusammen. Ein verspäteter oder
  abgebrochener Upload darf einen geschlossenen oder neu geöffneten Dialog nicht ändern.

Eine Datenbankdatei schrumpft nicht zwingend sofort nach dem Ersetzen oder Löschen
von BLOBs; bestehende Datenbankbackups bleiben vom normalen Backupbetrieb abhängig.
Es werden keine Datenbanken komprimiert oder Backups als Nebenwirkung gelöscht.

## Übernehmen und prüfen

Shared-Modul bauen, Java und Ressourcen übernehmen und die Anwendung neu starten;
Hibernate ergänzt beim regulären Start die neue Tabelle. Danach Strg+F5. Hier wurde
keine reale Anwendungsdatenbank geöffnet und keine reguläre Anwendung gestartet.

Die gezielten Nachweise umfassen echte Rasterdateien und EXIF-Ausrichtungen,
Byte-/Pixel-/Ausschnittgrenzen, Transparenz, Animationsausschluss, befristete Uploads,
Parallelität/Abbruch, reale H2-Transaktionen, konkurrierende erste Speicherungen,
Versionskonflikte, alle Rollen, Wartung/Sperren, Kontolöschung, HTTP-Status/Headers,
Uploadzuordnung, Status-/Profilereignisse und Listenerabbau. Die Tests verwenden
frische H2-Datenbanken und eigene erzeugte Bilder, keine echten Kontodaten.

Tatsächlicher Prüfstand: 512 gezielte JUnit-Fälle ohne Fehler oder Skips in der
abschließenden Regression. Nach der RAW-Upload-Einstellung, den Ablehnungs-
Notifications und der expliziten PNG-Speicherausgabe wurden 48 betroffene Fälle
nochmals erfolgreich geprüft. Spotless Apply/Check, Shared-Build, Kompilierung,
CSS-Struktur und `git diff --check` sind erfolgreich. Die vier JavaScript-Fälle
wurden ebenfalls erfolgreich ausgeführt; keine kostenpflichtigen/externen Provider.

Nach einem manuellen Uploadfehler wurde der tatsächliche RAW-Handler zusätzlich mit
realer VaadinSession und strenger Sperrprüfung über den Speicherdienst gegen frisches
H2 geprüft. Beide Varianten (mit Thread-Anmeldung bzw. HTTP-Sitzungsfallback) lehnten
vor der Korrektur den Upload ab: Der JPA-Standardwert von `UserEntity.preferredLocale`
las die ungesperrte UI-Sitzung; der Anmeldungsfallback hatte denselben Sperrfehler.
Das Modell verwendet jetzt einen UI-unabhängigen Standard. Registrierung und
Admin-Neuanlage setzen die gewählte Sprache ausdrücklich; gespeicherte Sprachen
bleiben erhalten. Der Anmeldungsfallback liest Vaadin-Attribute nur bei gehaltener
Sperre und verwendet ansonsten die überprüfte HTTP-Sitzung. Auch der Widerruf eines
ungültigen Kontos benötigt keine UI-Sperre. Unerwartete Avatarfehler werden protokolliert.

Die anschließende gezielte Regression bestand mit **210/210 JUnit-Fällen**, ohne
Fehler oder Skips. Sie umfasst Upload, Speichern, tatsächlichen DownloadHandler,
401 ohne Bild nach Kontosperre, Spracherhalt bei Registrierung/Admin-Neuanlage DE/EN,
Avatargrenzen und die betroffenen Anmeldungs-/Push-/Kontosicherheitsprüfungen.
Das vom Benutzer bereitgestellte JPEG wurde separat nur im Speicher erfolgreich
validiert und normalisiert; es wird nicht als Testfixture oder im Repository abgelegt.
Dies ist weiterhin kein Browsernachweis und keine neue vollständige Gesamttestrunde.

Zusätzlich laufen vier isolierte JavaScript-Tests über Node:
`node --test src/test/frontend/account-avatar-editor.test.mjs`. Sie prüfen die
Steuerung mit simulierten DOM-/Canvas-Schnittstellen, keine echte Browserdarstellung.

Browser-, Produktions- und vollständige Gesamttests wurden für dieses Feature nicht
neu gestartet. Offen bleiben die reale Upload-/Crop-Bedienung DE/EN, echte Handyfotos,
Touch/Screenreader, mehrere Browserfenster und ein echter Anwendungsneustart mit Bild.
Die vorherige Browserabdeckung ist kein Nachweis dieser neuen Profilbildreise.

Persönliche Avatarfarben bleiben eine spätere Erweiterung:
[offener Farbplan](umsetzungsplaene/kontokonfiguration/avatarfarben.md).
Die verwendeten Schnittstellen wurden mit den lokalen Vaadin-25.3.0-Klassen und der
[Upload-Dokumentation](https://vaadin.com/docs/latest/components/upload/file-handling)
sowie [Download-Dokumentation](https://vaadin.com/docs/latest/flow/advanced/downloads)
abgeglichen.

## Archivierte alte Bildressourcen

`user_icon.png` und `icons/clock_circle.svg` sind aus den ausgelieferten Ressourcen
entfernt und im externen Design-Backup unter denselben relativen Pfaden archiviert.
Der ungenutzte alte Status-Renderer und die alte Avatarpfad-Konstante sind entfernt.
Die aktuellen Kontursymbole werden direkt erzeugt und benötigen keine SVG-Datei.
`UserStatusResourceTest` prüft alle Status-/Größenvarianten und den Renderer aus
einem eigenen JAR ohne die alte Bildressource; Avatar- und Kontotests ergänzen dies.

## Dialogmodus beim Wiederöffnen

Der Server setzt Radiobutton und Editor-Modus aus dem gespeicherten Zustand.
Das clientseitige Zurücksetzen der Bilddaten verändert diesen Modus nicht:
Flow kann den Aufruf erst nach der Übertragung der aktuellen Attribute ausführen.
So bleiben „Eigenes Bild“ und der sichtbare Bildbereich beim Öffnen synchron;
auch Initialen mit behaltenem Bild werden nicht ungewollt umgeschaltet.
Zwei JavaScript-Regressionstests stellen diese Aufrufreihenfolge für beide Modi nach.

## Gestaltung der Bildauswahl

Der Dialog verwendet eine native Radiogruppe als zusammenhängende Auswahl mit
„Aa“-/Bildsymbol und Primary-Fläche für den aktiven Modus. „Darstellung“ bleibt
als zugänglicher Gruppenname erhalten. Die vollständig anklickbare Uploadfläche
ist weiterhin der native Upload-Button; sie bündelt Icon, Titel und Dateihinweis.
Der Hinweis zum Behalten des Bildes steht in einer eigenen Cosmos-Fläche mit
Info-Icon. Header, Footer, Speichern und Bildverarbeitung bleiben unverändert.
Die Gestaltung ist auf `.account-avatar-dialog` begrenzt und verwendet vorhandene
Farb-/Flächentokens. Avatarvorschau weiterhin 96 px, ohne Statuszeichen.
Gezielte Dialogtests prüfen auch lokalisierte Komponentenlabels und Upload-Namen;
die visuelle Browserabnahme dieser Gestaltung steht noch aus.

## Kompakte Upload-Variante zur Ansicht

Nach erfolgreicher neuer Bildauswahl verschwindet die große Uploadfläche zugunsten
von Ausschnitt, Zoom und einem lokalen „Abbrechen“-Button. Dieser verwirft nur die
neue Auswahl, lässt den Dialog offen und zeigt wieder die Uploadfläche. Ein bereits
gespeichertes Bild bleibt erhalten und wird erneut geladen; es findet kein Speichern
oder Löschen statt. Der Footer-Abbrechen-Button schließt weiterhin den Dialog.
Dateihinweise nennen nun ausdrücklich JPG/JPEG und PNG.

Der vorherige Dialogstand ist im taskbezogenen lokalen Arbeitsordner als
`avatar-dialog-before-compact-upload-20261018` gesichert. Bei Rücknahme nur die
kompakte Variante zurücknehmen und zwischenzeitliche Änderungen erhalten;
die JPG/JPEG-Beschriftung kann unabhängig bleiben. Keine Git-Sicherung erstellt.
