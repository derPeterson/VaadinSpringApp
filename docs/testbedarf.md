# Offener Testbedarf

## Browser-Tests

Oberflächenprüfungen in DE und EN auf Desktop und schmaler Ansicht ausführen,
sofern das Szenario keine andere Breite vorgibt. Manuelle Abnahmen gelten weiter.

- **Anbieterkarten – Abstand zwischen Logo und Text — offen.**
  OIDC-12 für Google und Microsoft bei 1280 und 390 px ausführen. Erwartet
  werden 16 bzw. 12 px Abstand und gleich breite Anbieterbuttons.

- **Kontosicherheit – mobile Kartenaktionen an der Textspalte — offen.**
  OIDC-12, ACCOUNT-CARDS-01 und MFA-21 bei 390 und 320 px prüfen.
  Passwort und E-Mail ändern, 2FA-Aktionen sowie Konto deaktivieren und
  löschen beginnen an der Textspalte und zeigen die vollständige Beschriftung.

- **Admin-Benachrichtigungen – mobile Footeraktionen — offen.**
  In `FormLayoutsBrowserTest` bei 390 und 320 px prüfen, dass „Vorschau prüfen“
  und „Benachrichtigung senden“ die Innenbreite ihrer Footer füllen und lesbar bleiben.

- **Admin-Vorgangsliste bei 320 px — offen.**
  Ein Ticket bei aktivem Filter auf einer Folgeseite öffnen und zur Liste
  zurückkehren. Listenseite und Filter bleiben erhalten. Beim Wiederöffnen
  steht der Entwurf weiterhin im Antwortfeld.

- **OTP-Methodenreiter — offen.**
  Tastaturwechsel und ARIA-Zuordnung der aktiven Eingabe in Anmelde- und
  Adminbestätigung sowie in Aktionsdialogen prüfen. In der Adminbestätigung
  zusätzlich die blaue Oberkante und den Anschluss an den Kopf prüfen.
  Beim Portalpasswortdialog die Pixelkante unter beiden Reitern, die seitlichen
  Linienanschlüsse und oberen Rundungen beim Öffnen und nach beiden
  Wechselrichtungen prüfen. Beim Zurückwechseln ist der angefangene App-Code
  leer. Bei Passwortnachweis, bereits bestätigter Identität und beim
  Wiederherstellungsschritt gezielt prüfen, dass der Kopf ohne Reiter bleibt.

- **Badge- und Formularmaße — offen.**
  Quellen-, Lesestatus- und Ticketrelevanz-Badges der Glockenvorschau,
  Ticketstatus in Liste, Kopf und Verlauf sowie Gerätestatus mit 22 px
  prüfen, Sitzungszähler mit 30 px und Sitzungslimit mit 24 px.
  Lange Beschriftungen auf Umbruch und Lesbarkeit prüfen. Bei einzeiligen
  Feldern und Feldhilfeknöpfen 40 px Höhe, Eingabetext mit 13 px und normale
  Buttonschrift mit 12 px stichprobenartig prüfen. Kompakte Seitengrößenauswahl
  und Dauerfelder mit den jeweiligen Größenvarianten vergleichen.

- **Eigene Wiederherstellungsanfragen als VirtualList — offen.**
  Mit 1/2/5/6/20 Tickets Listenhöhe, Begrenzung auf fünf Zeilen und Scrollen bis
  zum letzten Eintrag prüfen. Lange Titel, Badges, linke Markierung und lückenlose
  Auswahlfläche innerhalb der Zeile prüfen. Die ganze Zeile per Maus, Enter und
  Leertaste aktivieren. Strg+Klick hebt die Auswahl auf. Nach Wiederöffnen erscheint
  der Antwortentwurf, nach Nachladen oder Live-Update bleibt eine bewusst leere
  Auswahl erhalten. Sprachwechsel mit ausgewähltem Ticket prüfen. Die einfachen
  Trennlinien des Admin-Grids mit denen der Karten vergleichen.

- **Ticketnachricht – TextArea-Fokusrahmen und Größenänderung — offen.**
  Die Eingabefläche beginnt bei 122 px. Auf Desktop mit dem nativen Griff
  vergrößern, längeren Text eingeben, scrollen, leeren und erneut die Größe
  ändern. Erwartet sind eine stabile Ziehhöhe und ein sichtbarer äußerer
  Fokusrahmen. Leere und zu lange Inhalte beim Absenden prüfen.

- **Einheitliche Schließen-Icons — offen.**
  Kreis-X mit 18 × 18 px im 36-px-Button, Zentrierung, Fokus und Tooltip in
  Glockenvorschau, Nachrichtendialog, Empfänger-Popover, Meldungen, Feldhilfen,
  Benutzerverwaltung und Konto-, 2FA-, Anbieter- und Bestätigungsdialogen prüfen.
  Fehlende Maus- und Tastaturpfade abdecken, insbesondere den Schließen-Button
  im Empfänger-Popover und die Tastaturbedienung weiterer Meldungsvarianten.

- **Dauerfeld-Platzhalter nach Leeren — offen.**
  Die gedämpfte Farbe und unveränderte Zentrierung nach dem Leeren bei
  ganzzahligen Dauerwerten und Zahlenfeldern prüfen. Deaktivierte,
  schreibgeschützte Felder und Felder ohne Löschaktion einbeziehen.

- **Switch-Grundstyling — offen.**
  „Login speichern“, Benutzerformular, Anbieter/2FA, Ersatzcodes,
  Ticketbeobachtung und „Alle Benutzer“ mit den geprüften Sicherheits- und
  SMTP-Switches vergleichen. Bahn, runden Griff, Ein-/Aus- und
  Disabled-/Readonly-Farben, Fokus, Klickfläche, Beschriftung, Untertext,
  Umbruch und Abstände prüfen. Bei Benutzerformularen zusätzlich volle
  Zeilenbreite und oben/unten S bei Schaltern prüfen.

- **Mitteilungskarten – Dichte und Lesestatuslinie — offen.**
  Übersicht und Detail mit L-Innenabstand, Vorschau mit M prüfen. Abstände
  Titel–Nachricht und Nachricht–Metadaten in Übersicht und Detail S/M, in der
  Vorschau XS/S. Zusätzliche Leerzeilen und Abstände nach den Detailmetadaten
  ausschließen. Hohe Werkzeuge rechts oben dürfen den Textabstand nicht
  beeinflussen. Bei langen Titeln Umbruch, bei Titeln ohne Icon den bündigen
  Beginn prüfen. Im Detail bleiben Aktion und lange Texte im Kartenrand.
  In der Vorschau linke Linie für ungelesen (blau) und gelesen (grau) prüfen.
  Statuswechsel und der getrennte Ticketrelevanz-Badge dürfen die Ausrichtung
  nicht verschieben.

- **Empfängervorschau – vollständige Popover-Liste — offen.**
  Öffnen per Klick in den Zwischenraum zwischen Avataren und die
  Namen-Tooltips prüfen. Bei 200 Empfängern bis zum letzten Eintrag scrollen.
  Mauswahl, Strg+Klick auf eine andere Zeile und Erhalt der Auswahl beim
  Scrollen prüfen. Die Versandempfänger bleiben unverändert.
  Dialogkopf, abgesetzte Content-Fläche, 380-px-Wunschbreite mit seitlichem
  L-Abstand und Suchfeld (40/13/14 px für Höhe/Schrift/Lupe) vermessen.
  Overlayhöhe bei 200 Empfängern im Fenster halten. Zentrierten Leerzustand
  mit dem der Vorgangsliste vergleichen. Sekundäre E-Mail, lange Texte,
  vollflächige Auswahl samt Akzentlinie, Hover/Fokus,
  einfache Zeilentrenner und Scrollleisten in Chromium und Firefox prüfen.
  Eine zusätzliche Touch-Geste zum Abwählen wartet auf die Entscheidung im TODO.

- **Benachrichtigungsentwurf – Vorschauvalidierung — offen.**
  Beim Wechsel zu „Alle Benutzer“ gesperrte, deaktivierte und unbestätigte
  Konten aus der Vorschau ausschließen. Einen zuvor gewählten Empfänger
  nachträglich sperren. Die Vorschau nennt die geänderte Auswahl, während
  eine leere Auswahl die Meldung für fehlende Empfänger erhält.

- **Ticketsystem – Validierung und erneuter Anleitungsversand — offen.**
  Neue Anfrage, öffentliche Antwort und internen Vermerk leer oder zu lang
  absenden. Ohne Absenden keine Markierung, danach konkrete Meldung ohne
  Inline-Text oder Feldnamenzeile. Änderungen löschen die alte Markierung.
  Rechte-, Status- und Versionskonflikte beim Absenden sowie den Zeilenumbruch
  im Hinweis für interne Vermerke prüfen.
  Bei regulären und Entwicklerkonten den unterschiedlichen Untertitel,
  die erneute Freigabe nach 60 Sekunden ohne F5 und das Ende des Nachladens
  beim Verlassen prüfen. Bei regulären Konten blockiert zusätzlich ein
  Mailauftrag in PENDING oder IN_PROGRESS. Sperrtooltip per Maus und Tastatur
  mit fettem „Gesperrt:“ und normalen Markdown-Abständen prüfen. Der offene
  Mailauftrag hat Vorrang vor der Wartezeit. Bei Entwicklerkonten erscheint
  nur die Wartezeit.
  Bei Entwicklerkonten steht der gelbe Hinweis zu Beginn der Vorschau.
  Jeder bestätigte Wiederholungsversand erscheint im Ticket und erzeugt nur
  für reguläre Konten einen Mailauftrag. Sprachwechsel im offenen Dialog prüfen.

- **Konfiguration – Validierung und Sprachwechsel — offen.**
  Vor dem Speichern bleiben die Felder unmarkiert, nach dem Speichern
  werden ungültige Felder markiert. Die Meldung erklärt den ersten Fehler
  ohne zusätzliche Feldnamenzeile. Die offene Meldung soll beim Sprachwechsel mit
  unveränderten Werten übersetzt werden. Nur das bearbeitete Feld verliert
  seine alte Markierung, der Fokus springt beim Fehler nicht.
  Über die bereits geprüften Text-, Ganzzahl- und Dauerbeispiele hinaus
  E-Mail-Werte, Anbieter und Mailqueue-Assistenten sowie weitere Felder
  einbeziehen. Unlesbare Zahlen bei leerem Modellwert dürfen weder
  Standardwerte noch unbegrenzte Aufbewahrung speichern. Echte Leerwerte
  behalten ihren Vertrag. 2FA-Inaktivitätsfehler vor der Identitätsbestätigung
  prüfen, gültiges Speichern bleibt geschützt.

- **Benutzerformular – weitere Grenzfälle beim Speichern — offen.**
  Pflichtrollen und ungültige Geburtsdaten prüfen, insbesondere unlesbare
  Eingabe ohne Modellwert und Änderungsschutz. Die für Pflichtfelder,
  E-Mail und Passwort vorhandenen Prüfungen auf schmaler Ansicht absichern.
  Beim Korrigieren nur die betroffene Markierung entfernen. Zeilenumbruch
  der Passwortregeln und Sprachwechsel bei offener Meldung prüfen.
  Rollen auf Desktop nur links, auf schmaler Ansicht alle Zeilen einspaltig.
  Tastaturfokus der Schalter und Änderungen nur am Entwurf prüfen.
  Gemeinsam genutzte Schaltermaße stehen beim Switch-Nachtest.

- **Native Löschkreuze — offen.**
  In Benutzereditor, externem Registrierungsabschluss, Kontodialogen,
  Anmeldeanbietern, Mail-Test, Benachrichtigungen, Tickets und Ersatzcodefeld
  stichprobenartig Sichtbarkeit und Leeren per X/Escape prüfen. Leeren muss
  die Änderungs-/Dirty-Erkennung auslösen, auch für den gesamten TextArea-Wert.
  Bei leerem, deaktiviertem oder schreibgeschütztem Feld bleibt das X
  verborgen. OTP-Ziffern und E-Mail-Code mit dem Ersatzcodefeld vergleichen,
  einschließlich Methodenwechsel. Anzeige- und Kopierfelder bleiben
  schreibgeschützt. Escape zuerst am gefüllten Feld prüfen, über den bereits
  geprüften E-Mail-Dialog hinaus.

- **Persönliche Daten – Pflichtsprache — offen.**
  Bei fehlender Sprache bleibt das Feld bis zum Speichern unmarkiert.
  Speichern meldet das Pflichtfeld. Eine gültige Auswahl hebt die
  Markierung auf.

- **Pflichtfeldindikatoren — offen.**
  Rotes natives Sternchen direkt hinter der Beschriftung in Benutzereditor,
  Konfiguration und 2FA/Tickets prüfen. Optionale Felder bleiben ohne
  Indikator. Interne Vermerke verwenden keine separate violette Markierung.
  Disabled, Readonly und Fehlermarkierung bleiben optisch unterscheidbar.

- **Login – Markierungen bei Rückmeldungen zur Anmeldung — offen.**
  Wartung, Drosselung, Sitzungslimit, technische Fehler und Rückmeldungen nach
  Weiterleitungen prüfen. Die Eingabefelder bleiben dabei unmarkiert.

- **ComboBox-Overlays – Außen- und Eintragsabstände — offen.**
  Außen 8 px Scrollerabstand und zwischen Auswahlflächen 2 px prüfen,
  insbesondere unter der letzten ausgewählten Zeile. Geschlecht/Sprache,
  Benutzer- und Konfigurationsfelder, Rollen- und Empfänger-Mehrfachauswahl
  sowie Seitengrößenauswahl in Benutzerverwaltung, Inbox und Versandhistorie
  einbeziehen. Filterung, Scrollen, Tastaturauswahl und Auswahlhaken prüfen,
  soweit die Varianten diese Funktionen anbieten.

- **Checkboxen – weiße Auswahlmarkierung — offen.**
  Weiße Haken und Teilauswahlstriche in Einzelcheckboxen, Gruppen und
  Tabellen-Gesamtauswahl prüfen. Auswahlfläche, Fokus und Hover sowie
  native Unterscheidbarkeit bei Disabled und Readonly prüfen.

- **DatePicker-Overlay – Aktionen und Jahresspalte — offen.**
  Hover von „Heute“ und „Abbrechen“ sowie die native seitliche Markierung
  der Jahresspalte prüfen. Auf dem Smartphone Jahres- und Datumswechsel
  in der nativen Auswahl ohne Überlauf prüfen.

- **Globale Notifications – Platzbedarf und Inhalt — offen.**
  Rechts 24 px Abstand mit und ohne vertikale Scrollleiste vergleichen.
  Lange Meldung bei geringer Höhe, nativen Innenabstand, gelbes Warndreieck,
  rotes Kreis-X, gemeinsame Titel ohne Ausrufezeichen und lesbare Links prüfen.

- **Link-Statusseiten und Sicherheitshinweise – weitere Iconvarianten — offen.**
  Abgelaufene und ungültige Links sowie technische Fehler für
  Verifikation und E-Mail-Änderungsbestätigung prüfen. Beim Passwort-Reset
  ist zusätzlich das Icon für technische Fehler offen. Das Warndreieck
  und seine Farbe in Hinweisen bei Kontoaktionen, Reaktivierung,
  Benutzereditor, Neustarthilfe, Wiederherstellung und Tickets prüfen.

## JUnit-Tests

Kein gesonderter offener JUnit-Testbedarf.

Menschliche Sonderprüfungen und Abnahmen stehen im
[manuellen Katalog](manual-tests/README.md#hum-01).
