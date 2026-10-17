# Ausgefülltes Lernbeispiel – keine ausgeführte Prüfung

**Alle Beobachtungen, Personen, Belege, Fehler und Korrekturen in dieser Datei sind
frei erfunden. Es wurde dafür weder eine Anwendung gestartet noch ein Fehler behoben.**
Der unten genannte echte Repository-Hash zeigt nur das erwartete Format des Feldes;
er bestätigt keinen dieser fiktiven Tests oder Befunde.

🟦 **Blaue Markierung = ausgefüllter Beispielwert.** Überschriften, Schritte und
Erwartungen entsprechen der [leeren Vorlage](vorlage.md). Die blauen Werte gehören
nicht in ein echtes Protokoll; sie werden durch eigene Beobachtungen ersetzt.
Es werden keine fehlenden Bild-/Logdateien verlinkt: E-01 usw. sind erfundene Belegkennungen.

## Kopf des Protokolls

| Feld | Ausgefüllter Beispielwert |
| --- | --- |
| Protokoll-ID / Anlass | 🟦 BEISPIEL-A; orientierende Prüfung nach einer lokalen Oberflächenänderung |
| Ausgewählte Test-IDs / ausgelassene Varianten | 🟦 Alle 15 Fälle ausgewählt; fehlende Geräte/Clients und ausgelassene Schritte unten einzeln ausgewiesen |
| Beginn / Ende / Zeitzone | 🟦 15.10.2026, 18:00–19:30 Uhr, Europe/Berlin (+02:00); erfundene Uhrzeiten |
| Geprüft von | 🟦 Beispielperson Alex; keine tatsächlich prüfende Person |
| Git-Commit / Branch / lokale Änderungen | 🟦 Formatbeispiel `0695926bcf7fd119b22aa560f644c80bbcf9774d`, Branch main; Lernstand A, fiktive lokale Dialogänderung beschrieben, aber nicht umgesetzt |
| Startart / URL | 🟦 IntelliJ-Konfiguration „Vaadin – Handprüfung“; http://127.0.0.1:18090/ |
| Datenbank / Properties-Datei | 🟦 data/manual-tests/BEISPIEL-A/appdb und data/manual-tests/BEISPIEL-A/manual-tests.properties; erfundene Pfade, Kennwörter/Schlüssel bleiben lokal |
| Browser / Version / Geräte / OS / Zoom | 🟦 Beispielinventar D1: Windows 11, Edge 140; M1: Android 15, Chrome 140; Desktop 1280×900, Mobil 414×896, Simulation 390×844; Zoom 100 %/200 %. Erfundenes Inventar, keine Versionsempfehlung |
| Oberflächensprachen / Mailclients / Hilfsmittel | 🟦 DE und EN; Outlook/Gmail geplant, nicht ausgeführt; Tastatur vorhanden, Screenreader fehlt |
| SMTP-Testziel / verfügbare Betriebsbelege | 🟦 Lokal A 127.0.0.1:18125/Web 18126; B 18127/Web 18128 für AUTO-02; Auth/TLS aus. Logs/Mailpit vorhanden, auditierbare Terminalzustandsbeobachtung fehlt |
| Testkonten | 🟦 manual-admin@example.test (Admin), probe-benutzer@example.test (normal), retry-beispiel@example.test (Registrierung während SMTP-Ausfall); alle erfunden |
| Einrichtung | 🟦 Für lokale UI-/SMTP-Fälle bereit; Screenreader und Auditverfahren fehlen, externes SMTP nicht vorgesehen |
| Belegablage | 🟦 data/manual-tests/BEISPIEL-A/belege; nur Beispielpfad. In dieser Lerndatei keine echten Screenshots, Logs oder Tokenlinks |

## Entscheidungshilfe während des Ausfüllens

| Situation | Beispielentscheidung |
| --- | --- |
| Test noch nicht begonnen und Auswahl noch offen | 🟦 OFFEN; erst nach Ausführung bewerten, hier im abgeschlossenen Beispiel null offene Fälle |
| Erwartung tatsächlich erfüllt | 🟦 BESTANDEN plus konkrete Beobachtung und geprüfte Varianten |
| Titel überdeckt eine Aktion | 🟦 FEHLER, Befund B-01 anlegen; Screenshot/Breite festhalten |
| Screenreader oder belastbare Einsicht fehlt | 🟦 BLOCKIERT; Voraussetzung und nächste Maßnahme benennen, keinen Anwendungsfehler erfinden |
| Test wäre möglich, Zeit reicht nicht | 🟦 NICHT AUSGEFÜHRT; offen gebliebenen Umfang nennen |
| Reiner lokaler Einsatz ohne externes SMTP | 🟦 AUTO-08 NICHT ZUTREFFEND mit dieser fachlichen Begründung |
| Fix vorhanden | 🟦 Neuer Stand und getrennter Nachtest; die alte FEHLER-Zeile bleibt erhalten |

## Prüffälle

### HUM-01 · Visuelle Gesamtabnahme

**Einordnung:** Menschliche Abnahme.

**Warum hier:** Die Layoutprüfungen messen Grenzen und bekannte Styles; sie beurteilen keine gestalterische Stimmigkeit.

**Voraussetzung:** Desktop und kleines Fenster; DE und EN; freigegebener Designstandard als Vergleich.

1. Login, Registrierung, Home und alle Adminbereiche ansehen; je eine Erfolgsmeldung und Feldfehlermeldung öffnen.
2. Benutzer bearbeiten und Löschbestätigung öffnen, ohne zu löschen. Tabelle, Filter, Overlays und Dialogkopf/-fuß zusammen beurteilen.
3. Lesbarkeit, Kontraste, Abstände, Ausrichtung, Iconwirkung und verständliche Hervorhebung aktiver/gefährlicher Aktionen bewerten. Auffälligkeiten konkret benennen.

**Erwartet:** Die Oberfläche wirkt zusammengehörig und ruhig; relevante Texte/Aktionen sind gut lesbar und eindeutig. Rein persönlicher Änderungswunsch wird von einem Bedienhindernis unterschieden.

- **Status:** 🟦 FEHLER
- **Beobachtung:** 🟦 Im erfundenen Lernstand A überlagert bei 390 px ein langer englischer Dialogtitel den Schließen-Button. Desktop wirkt stimmig; die schmale Variante verletzt die Erwartung.
- **Varianten / Zeitpunkt:** 🟦 DE/EN; Desktop 1280×900 und simuliert 390×844; 18:05–18:12 +02:00.
- **Beleg oder fehlende Voraussetzung:** 🟦 Fiktiver Beispielbeleg E-01: Screenshotbeschreibung „Titel überdeckt X bei 390 px“. Es gibt dazu keine echte Bilddatei.
- **Befund / Nachtest:** 🟦 B-01; Nachtest BEISPIEL-B unten. Die ursprüngliche Bewertung bleibt FEHLER.

### HUM-02 · Echtes Mobilgerät und Bildschirmtastatur

**Einordnung:** Menschliche Abnahme.

**Warum hier:** Die Browser-Suites simulieren Fenstergrößen; sie ersetzen kein echtes Touchgerät und dessen Tastatur.

**Voraussetzung:** Eigenes Mobilgerät im lokalen Netz; Testanwendung erreichbar, base.url passend; Schritte zur Netzfreigabe in anwendung.md.

1. In Hoch- und Querformat ein längeres Formular ausfüllen. Bildschirmtastatur, Scrollen, Datumsauswahl und Passwortanzeige benutzen.
2. Filterpanel und Date-/TimePicker sowie Benutzerdialog bedienen; Öffnen/Schließen und erreichbare Aktionen bei geöffneter Tastatur beurteilen.
3. Touchziele, Scrollverhalten, Tastaturüberlagerung und Rotation bewerten; Gerät, OS und Browser festhalten.

**Erwartet:** Die Aufgabe lässt sich ohne verdeckte Pflichtfelder oder unerreichbare Aktionen erledigen. Touchziele und Rückmeldungen sind praktisch bedienbar; nach Rotation bleiben Eingaben nachvollziehbar.

- **Status:** 🟦 BESTANDEN
- **Beobachtung:** 🟦 Auf dem Beispielgerät mit 414 px Breite blieben Formularaktionen mit geöffneter Tastatur erreichbar; Rotation erhielt die Eingaben. Die 390-px-Abweichung aus HUM-01 wurde hier nicht nochmals behauptet.
- **Varianten / Zeitpunkt:** 🟦 Eigenes Beispielgerät M1, Android mit Chrome; 414×896, Hoch/Quer, Tastatur eingeblendet; 18:12–18:22.
- **Beleg oder fehlende Voraussetzung:** 🟦 Fiktiver Beispielbeleg E-02: Gerätebeobachtung mit Hoch-/Querformat und Tastatur.
- **Befund / Nachtest:** 🟦 Kein Befund; kein Nachtest in dieser Beispielrunde.

### HUM-03 · Tastatur, Zoom und sichtbarer Fokus

**Einordnung:** Menschliche Abnahme.

**Warum hier:** Login und einzelne Shortcuts sind automatisiert; die durchgehende Bedienbarkeit und menschliche Lesbarkeit sind es nicht.

**Voraussetzung:** Desktop ohne Maus; Browserzoom 100 % und 200 %, zusätzlich eigene Kontrast-/Anzeigeeinstellung sofern genutzt.

1. Mit Tab, Umschalt+Tab, Enter, Leertaste und Escape durch Navigation, Benutzerliste, Filter, Dialoge und Info-Overlays gehen.
2. Bei 200 % dieselbe Aufgabe abschließen; Fokus nach Schließen eines Dialogs/Overlays und Rückkehr in die Tabelle verfolgen.
3. Fokusanzeige, logische Reihenfolge, Erreichbarkeit der Schließen-/Lösch-/Speicheraktionen und Lesbarkeit beurteilen.

**Erwartet:** Fokus bleibt sichtbar und verständlich; alle für die gewählte Aufgabe nötigen Aktionen sind erreichbar. Kein unbeabsichtigtes Speichern, keine Fokusfalle und kein Verlust notwendiger Information durch Zoom.

- **Status:** 🟦 BESTANDEN
- **Beobachtung:** 🟦 Die gewählten Aufgaben ließen sich bei 100 % und 200 % mit sichtbarem Fokus bedienen; Schließen führte nachvollziehbar zum Auslöser zurück.
- **Varianten / Zeitpunkt:** 🟦 Desktop D1, Edge, Zoom 100 %/200 %; Navigation, Filter, Edit/Löschdialog und Info-Overlay; 18:22–18:32.
- **Beleg oder fehlende Voraussetzung:** 🟦 Fiktiver Beispielbeleg E-03: dokumentierte Tabfolge und Beobachtung bei beiden Zoomstufen.
- **Befund / Nachtest:** 🟦 Kein Befund; kein Nachtest in dieser Beispielrunde.

### HUM-04 · Screenreader und verständliche Ansagen

**Einordnung:** Menschliche Abnahme.

**Warum hier:** Es existiert kein vollständiger Screenreader-Nachweis. Automatische Regeln können die Verständlichkeit der Ansagen nur ergänzen.

**Voraussetzung:** Tatsächlich verfügbarer Screenreader; Produkt/Version, Browser und Bedienmodus protokollieren.

1. Login oder Registrierung lesen lassen: Labels, Pflichtfelder, Passworteingabe und Fehlerbezug beurteilen.
2. Benutzerliste, Filterstatus/Badges und Datum-/Uhrzeitauswahl bedienen; Sortierung und deaktivierte Elemente anhören.
3. Bearbeitungs-/Löschdialog und Benachrichtigung öffnen. Titel, Zweck, gefährliche Aktion und Fokuswechsel beurteilen.

**Erwartet:** Zweck, aktueller Zustand und Fehlerhilfe sind hörbar verständlich. Dialoge besitzen einen erkennbaren Kontext; Iconaktionen sind sinnvoll benannt. Ohne Screenreader bleibt der Fall BLOCKIERT, nicht bestanden.

- **Status:** 🟦 BLOCKIERT
- **Beobachtung:** 🟦 Keine Beobachtung möglich: Auf dem Beispielrechner war kein Screenreader für diesen Test verfügbar. Das ist kein nachgewiesener Anwendungsfehler.
- **Varianten / Zeitpunkt:** 🟦 Geplant: Desktop D1 plus Screenreader; um 18:32 BLOCKIERT festgestellt.
- **Beleg oder fehlende Voraussetzung:** 🟦 Voraussetzung fehlt: Screenreader. Nächste Maßnahme: verfügbares Produkt/Version und Termin mit geschulter Person festlegen.
- **Befund / Nachtest:** 🟦 Kein Anwendungsbefund; offene Voraussetzung zuerst herstellen.

### HUM-05 · Verständlichkeit der deutschen und englischen Texte

**Einordnung:** Menschliche Abnahme.

**Warum hier:** Sprachwechsel und konkrete Texte sind technisch geprüft; sprachliche Qualität und Handlungsverständnis erfordern eine Person.

**Voraussetzung:** DE und EN jeweils über den vorhandenen Sprachschalter wählen; Testkonto und absichtliche Formularfehler.

1. Feldhilfen, Konfigurationsinfos, Passwortregeln, Filterbeschreibung und Sicherheits-/Löschtexte in beiden Sprachen lesen.
2. Beurteilen, ob verständlich ist, was geändert wird, wann ein Neustart nötig ist und wie ein Fehler korrigiert werden kann.
3. Uneinheitliche Begriffe, irreführende Aussagen und schwer verständliche Sätze mit konkretem Textvorschlag notieren.

**Erwartet:** Die Texte sind konsistent, verständlich und fachlich passend. Eine korrekte Schlüsselauflösung allein genügt nicht. Anonyme Formularseiten besitzen keinen eigenen Sprachschalter; keine künstlichen Sessionereignisse verwenden.

- **Status:** 🟦 BESTANDEN
- **Beobachtung:** 🟦 DE und EN waren im Beispiel für die gewählten Hilfen/Fehler verständlich; kein konkreter Textbefund. Ein persönlicher Formulierungswunsch wäre separat als Verbesserung zu erfassen.
- **Varianten / Zeitpunkt:** 🟦 DE und EN; Passwort-/Konfigurationshilfen, Filter- und Löschtexte; 18:33–18:43.
- **Beleg oder fehlende Voraussetzung:** 🟦 Präzise Textbeobachtung im Protokoll; kein Screenshot nötig, keine Beanstandung im fiktiven Beispiel.
- **Befund / Nachtest:** 🟦 Kein Befund; kein Nachtest in dieser Beispielrunde.

### HUM-06 · Darstellung in tatsächlich verwendeten Mailprogrammen

**Einordnung:** Menschliche Abnahme.

**Warum hier:** Mailpit prüft SMTP/MIME, Inhalt und Inline-Bilder; Gmail-/Outlook-/Mobilansicht und menschliche Lesbarkeit sind damit nicht belegt.

**Voraussetzung:** Eigene tatsächlich erzeugte Testmail; Mailprogramm benennen. EML-Import nur falls unterstützt; externe Zustellung getrennt unter AUTO-08.

1. Verifikations- und Resetmail in den tatsächlich genutzten Clients öffnen; breite/schmale Ansicht und hell/dunkel soweit verfügbar vergleichen.
2. Logo, Abstände, Text, Footer und erkennbare Link-/Buttonbeschriftung beurteilen. Verhalten bei blockierten Bildern ansehen.
3. Prüfen, ob sich der Handlungszweck ohne externe Bilder verstehen lässt und das Linkziel zur eigenen Testinstanz passt.

**Erwartet:** Die Mail ist lesbar, vertrauenswürdig zuzuordnen und bedienbar; Layout oder Clientfarben verdecken keine Aktion. Nicht verfügbare Clients einzeln als nicht geprüft aufführen; ein Mailpit-Screenshot ersetzt sie nicht.

- **Status:** 🟦 NICHT AUSGEFÜHRT
- **Beobachtung:** 🟦 Keine Clientdarstellung bewertet. Nur Mailpit war geöffnet; dessen Ansicht zählt hierfür ausdrücklich nicht als Ersatz.
- **Varianten / Zeitpunkt:** 🟦 Geplant: Outlook und Gmail; um 18:43 bewusst ausgelassen.
- **Beleg oder fehlende Voraussetzung:** 🟦 Grund: Zeit für die tatsächlich vorgesehenen Mailclients fehlte. Mailpit-Nachrichten stehen als Testmaterial bereit; Darstellung bleibt ungeprüft.
- **Befund / Nachtest:** 🟦 Im Restumfang vermerkt; kein erfolgreicher Nachweis.

### HUM-07 · Recovery-Entscheidung bei unklarem Mailversand

**Einordnung:** Menschliche Abnahme.

**Warum hier:** Die konservative Quarantäne ist technisch geprüft; die Bewertung unvollständiger Betriebsbelege ist eine menschliche Entscheidung.

**Voraussetzung:** Nur eine Schreibtischübung mit erfundenen Versandbelegen; Recovery-Vertrag: docs/email-queue-processing.md im Projekt.

1. Szenario A: SMTP-Annahme belegt, Datenbank-Ergebnis fehlt. Szenario B: nur Timeout/fehlende Postfachmail. Szenario C: Nichtversand positiv belegt.
2. Je Szenario Entscheidung begründen: kein erneuter Versand bei Annahme; bei unklarem Ausgang Quarantäne/Eskalation; bei belegtem Nichtversand erst autorisierte und kontrollierte Wiederfreigabe prüfen.
3. Benötigte Queue-ID, Versandfenster, beendete Worker, Providerbelege und Tokenzustand benennen. Einen widersprechenden oder fehlenden Beleg bewusst berücksichtigen.

**Erwartet:** Entscheidung und Begründung folgen dem Recovery-Vertrag. Fehlende Mail und alter Claim gelten nicht als Nichtversandbeweis. Diese Übung verändert keine Daten und führt keinen Retry aus.

- **Status:** 🟦 BESTANDEN
- **Beobachtung:** 🟦 Die prüfende Person ordnete Annahme zu kein Retry, Timeout zu Quarantäne/Eskalation und belegten Nichtversand zu erst autorisierter Wiederfreigabe zu. Es erfolgte keine Datenänderung.
- **Varianten / Zeitpunkt:** 🟦 Alle drei beschriebenen Szenarien A/B/C; 18:44–18:51.
- **Beleg oder fehlende Voraussetzung:** 🟦 Schriftliche Szenarioentscheidung mit Begründung; kein Queue-Zugriff und keine Wiederfreigabe.
- **Befund / Nachtest:** 🟦 Kein Befund; kein Nachtest in dieser Beispielrunde.

### AUTO-01 · Bestandsabgleich nach Neustart ohne Bootstrap

**Einordnung:** Zur Automatisierung vorgesehen – Betriebsfixture.

**Warum hier:** Echte Serverneustarts mit derselben eigenen Datenbank sind inzwischen
für Anmeldung und gespeicherte Sitzungszugänge automatisiert. Hier bleibt nur der
zusätzliche Betriebsfall mit deaktiviertem Bootstrap und einem ausdrücklichen
Vorher-/Nachher-Abgleich von Kontenzahl, Rollen/Freigabe und Konfigurationswert offen.
Die Anmeldung nach Neustart allein ist kein Nachweis dieses gesamten Datenbestands.

**Voraussetzung:** Eigene Dateidatenbank, ein über die UI angelegtes Testkonto und ein gespeicherter Konfigurationswert; Bootstrap deaktiviert.

1. Konto über die Oberfläche wiederfinden; E-Mail, Freigabe/Rolle, Kontenzahl und einen nicht geheimen Konfigurationswert als Ausgangsstand notieren.
2. Nur die eigene Anwendung ordentlich stoppen und mit exakt derselben Properties-Datei/Datenbank neu starten; keine neue Datenbank wählen.
3. Frisch anmelden und die notierten Werte erneut prüfen. Ein normales Sitzungsende durch den Serverneustart ist kein Datenverlust.

**Erwartet:** Konto und gespeicherte Einstellungen bleiben erhalten; keine zusätzlichen Bootstrapkonten oder Datenverluste. Alte offene Browseransichten nicht als alleinigen Persistenznachweis verwenden.

- **Status:** 🟦 BESTANDEN
- **Beobachtung:** 🟦 Ein eigenes Testkonto, seine Freigabe/Rolle, die Kontenzahl und der gespeicherte Servicename blieben nach geordnetem Serverneustart mit derselben DB erhalten. Frische Anmeldung funktionierte.
- **Varianten / Zeitpunkt:** 🟦 IDE-Stop/Start; unveränderter Pfad data/manual-tests/BEISPIEL-A/appdb; 18:52–18:58.
- **Beleg oder fehlende Voraussetzung:** 🟦 Fiktive Vorher-/Nachhernotiz mit eigenen UI-Werten; ursprüngliche DB/Datei beibehalten.
- **Befund / Nachtest:** 🟦 Kein Befund; kein Nachtest in dieser Beispielrunde.

### AUTO-02 · SMTP-Vorgaben beim Erststart und Wechsel nach Neustart

**Einordnung:** Zur Automatisierung vorgesehen – Betriebsfixture.

**Warum hier:** Lokale Mails sind geprüft; die gezielte Erststart-/Konfigurationswechsel-/Neustartfolge ist nicht automatisiert.

**Voraussetzung:** Für die Erststartvariante eigene leere DB; Mailpit A auf 18125/18126 und optional B auf 18127/18128. Andere Ports nur mit dokumentierter Anpassung.

1. Beim allerersten Start mit vorbereiteter Properties-Datei eine eindeutige Admin-Testmail senden; Empfang in A prüfen, bevor ein zusätzlicher Neustart stattfindet.
2. Im Adminbereich mail.port auf 18127 speichern. Vor dem Prozessneustart eine zweite eindeutig benannte Testmail senden: der bestehende SMTP-Sender nutzt weiterhin A.
3. Dieselbe Anwendung/DB neu starten, dritte eindeutige Mail senden und Empfang in B prüfen. mail.port danach zurücksetzen und nochmals neu starten.

**Erwartet:** Konfigurierte Defaults gelten bereits bei leerer DB. Eine gespeicherte Änderung des startgebundenen SMTP-Ports wirkt erst nach Neustart; bestehende DB-Werte werden nicht durch app.config.defaults.* überschrieben. B fehlt: Wechselvariante BLOCKIERT.

- **Status:** 🟦 BESTANDEN
- **Beobachtung:** 🟦 Erste Testmail kam vor zusätzlichem Neustart in A an. Nach gespeichertem Portwechsel ging die zweite weiterhin an A; nach Neustart traf die dritte in B ein. Rückstellung auf A wurde bestätigt.
- **Varianten / Zeitpunkt:** 🟦 SMTP A 18125/Web 18126, B 18127/Web 18128; Mailbetreffe mit Schritten 1/2/3 unterschieden; 18:59–19:08.
- **Beleg oder fehlende Voraussetzung:** 🟦 Fiktive Empfangsnotizen mit drei Betreff-/Zeitkennungen und zugehörigem Mailpit A/B; keine Tokenlinks ins Protokoll kopiert.
- **Befund / Nachtest:** 🟦 Kein Befund; kein Nachtest in dieser Beispielrunde.

### AUTO-03 · Realer SMTP-Ausfall mit anschließendem Wiederanlauf

**Einordnung:** Zur Automatisierung vorgesehen – Betriebsfixture.

**Warum hier:** Retry und Fälligkeit sind JUnit-abgedeckt; reales Stoppen/Starten des eigenen SMTP-Ziels mit laufender Anwendung fehlt im Browserlauf.

**Voraussetzung:** Nur eigenes Mailpit; Queue-Retrylimit größer null und notierter Ausgangswert. Diese Prüfung registriert ein neues Konto, statt den direkten Admin-Mailtest als Queue-Test zu verwenden.

1. Eigenes Mailpit vor der Anfrage stoppen. Über Registrierung eine neue eindeutige Testadresse verwenden und den erwarteten Verbindungsfehler im Anwendungslog zuordnen.
2. Mailpit wieder starten; mindestens den Retryabstand von einer Minute und weitere Schedulerzeit abwarten. Ausgang nur anhand des eigenen Empfängers/Zeitfensters zuordnen.
3. Empfang und verwendbaren Link prüfen; einen weiteren Retryzeitraum beobachten und dokumentieren, ob eine weitere identische Zustellung erscheint.

**Erwartet:** Bei nachgewiesenem Verbindungsfehler vor SMTP-Annahme kann die wartende Mail nach Wiederanlauf zugestellt werden. Keine vorschnelle Bewertung nach wenigen Sekunden. Unklarer Transportausgang ist ein anderer Fall; keine Queuezeilen manuell zurücksetzen.

- **Status:** 🟦 BESTANDEN
- **Beobachtung:** 🟦 Nach Mailpit-Stopp wurde ein eindeutiger Verbindungsfehler protokolliert. Nach Wiederanlauf traf die eigene Queue-Mail nach dem Retryabstand ein; im weiteren beobachteten Zeitraum erschien keine identische zweite Mail.
- **Varianten / Zeitpunkt:** 🟦 Neue eigene Registrierungsadresse; Mailpit vollständig gestoppt/wieder gestartet; zwei Retryzeiträume beobachtet; 19:09–19:14.
- **Beleg oder fehlende Voraussetzung:** 🟦 Fiktiver Ausschnitt eines zugeordneten Verbindungsfehlers und Empfangszeitpunkt; keine Behauptung über Exactly-once für beliebige Ausfälle.
- **Befund / Nachtest:** 🟦 Kein Befund; kein Nachtest in dieser Beispielrunde.

### AUTO-04 · Dauerhafter SMTP-Ausfall und brauchbare Betriebsdiagnose

**Einordnung:** Zur Automatisierung vorgesehen – Betriebsfixture.

**Warum hier:** Retrylimit und Auditverträge sind technisch abgedeckt; dauerhafter realer Ausfall und praktische Beobachtbarkeit sind nicht durch Browser belegt.

**Voraussetzung:** Isolierte Umgebung, ursprüngliches Retrylimit notiert, für diesen Fall z. B. auf 1 reduziert; eigene Logs und vorgesehenes Betriebsbeobachtungsverfahren verfügbar.

1. Mailpit vor Registrierung einer neuen eindeutigen Adresse stoppen und bis zum initialen Versuch plus konfigurierten Wiederholungen warten; mindestens eine Minute Retryabstand beachten.
2. Anhand zusammengehöriger Logs/Auditbelege feststellen, ob die Verarbeitung begrenzt endet und ein Betreiber Empfängerbezug, Zeitpunkt und nächste Maßnahme zuordnen kann.
3. Mailpit wieder starten und länger als einen Retryabstand beobachten; die terminal fehlgeschlagene Nachricht darf nicht automatisch neu versandt werden. Einstellungen wiederherstellen.

**Erwartet:** Die reale Verarbeitung endet begrenzt und ist diagnostizierbar. Bei ausgefallenem SMTP ist auch die Admin-Benachrichtigung nicht garantiert; fehlende Audit-/Sendebelege bedeuten keinen Erfolg. Ohne belastbare Einsicht BLOCKIERT; keine neue Admin-Queueoberfläche voraussetzen.

- **Status:** 🟦 BLOCKIERT
- **Beobachtung:** 🟦 Der dauerhafte Ausfall wurde im Beispiel nicht weiter ausgeführt: Ein belastbares, freigegebenes Audit-/Betriebsbeobachtungsverfahren fehlte. Ausbleibende Mail allein wäre kein Nachweis für den terminalen Zustand.
- **Varianten / Zeitpunkt:** 🟦 Geplanter Nachweis bei Retrylimit 1; um 19:14 wegen fehlender Belege blockiert. Kein Retrywert verändert.
- **Beleg oder fehlende Voraussetzung:** 🟦 Voraussetzung fehlt: belastbare Zustands-/Auditkorrelation. Zuständig: betreuende Person für Betriebsfixture; kein SQL als Abkürzung.
- **Befund / Nachtest:** 🟦 Kein Anwendungsbefund; offene Voraussetzung zuerst herstellen.

### AUTO-05 · Sprachwechsel mit sichtbaren Feldfehlern und Entwürfen

**Einordnung:** Zur Automatisierung vorgesehen – Browserfall.

**Warum hier:** Entwürfe/Navigation und Infos sind geprüft; bereits ausgelöste Admin-Feldfehler beim Sprachwechsel bleiben offen.

**Voraussetzung:** Admin-Konfiguration; ein ungültiger unkritischer Entwurf, z. B. ungültige Basis-URL. Ursprünglichen Wert notieren.

1. Ungültigen Entwurf eingeben und Speichern auslösen; sichtbare Feld-/Benachrichtigungsfehler dokumentieren.
2. Über den tatsächlich erreichbaren Benutzermenü-Sprachschalter DE nach EN und zurück wechseln. Labels, noch sichtbare Fehlertexte und Erhalt des Entwurfs prüfen.
3. Entwurf korrigieren oder verwerfen; danach die gespeicherte Originalkonfiguration prüfen.

**Erwartet:** Weiterhin sichtbare Texte folgen der gewählten Sprache; der ungültige Wert wird nicht gespeichert und der Entwurf nicht still übernommen. Bereits geschlossene Meldungen sind nicht nachträglich prüfbar. Keine Sprachwahl hinter einem modalen Dialog erzwingen.

- **Status:** 🟦 BESTANDEN
- **Beobachtung:** 🟦 Der ungültige Basis-URL-Entwurf blieb beim Wechsel DE nach EN und zurück erhalten; sichtbare Fehlermeldung/Labels wechselten. Nach Verwerfen war die ursprüngliche URL gespeichert.
- **Varianten / Zeitpunkt:** 🟦 DE→EN→DE; ungespeicherter URL-Fehler und Rückkehr zum ursprünglichen Wert; 19:15–19:21.
- **Beleg oder fehlende Voraussetzung:** 🟦 Fiktive Notiz zu Entwurf, Fehlersprache und erneut geprüftem Originalwert.
- **Befund / Nachtest:** 🟦 Kein Befund; kein Nachtest in dieser Beispielrunde.

### AUTO-06 · Neue Mails nach Sprachwechsel für dasselbe Konto

**Einordnung:** Zur Automatisierung vorgesehen – Browserfall.

**Warum hier:** Getrennte DE-/EN-Läufe und Kontosprache sind geprüft; wechselnde neue Mails desselben bestehenden Kontos mit vorheriger Mailhistorie bleiben ergänzbar.

**Voraussetzung:** Ein bereits aktiviertes Testkonto; eigenes Mailpit; für jede Mail Zeitpunkt/Empfänger und Sprache der auslösenden Sitzung notieren.

1. Mit dem vorhandenen Konto die Sprache wechseln und abmelden. Auf Home den angebotenen Sprachschalter bewusst auf EN setzen, dann die Passwortanfrage öffnen und eine neue Mail anfordern.
2. Nach Zustellung auf Home DE wählen und für dasselbe Konto erneut anfordern. Die zwei neuen Nachrichten anhand von Zeitpunkt/Mail-ID unterscheiden, nicht eine alte Mail wieder auswerten.
3. Betreff, Inhalt und Footer vergleichen; die vorher empfangene englische Nachricht erneut ansehen.

**Erwartet:** Neue Mails der anonymen Passwortanfrage folgen der Sprache der auslösenden Sitzung; gespeicherte Kontosprache allein bestimmt eine anonyme Anfrage nicht. Sicherheitsmails zu angemeldeten Passwort-/E-Mail-Änderungen folgen dagegen bewusst der gespeicherten Kontosprache; das ist ein eigener automatisierter Vertrag. Frühere Mails ändern ihre Sprache nicht nachträglich. Es werden ausschließlich neue zulässige Anfragen benutzt.

- **Status:** 🟦 BESTANDEN
- **Beobachtung:** 🟦 Für dasselbe vorhandene Konto wurde zuerst in englischer, anschließend in deutscher auslösender Sitzung jeweils eine neue Mail empfangen. Betreff/Inhalt/Footer folgten der jeweiligen Sitzung; die erste Mail blieb englisch.
- **Varianten / Zeitpunkt:** 🟦 EN- und DE-Anfrage an dasselbe aktivierte Konto; zwei neue Nachrichten M-01/M-02; 19:22–19:28.
- **Beleg oder fehlende Voraussetzung:** 🟦 Fiktive Mailkennungen M-01/M-02 und beobachtete Sitzungssprachen; vollständige Links und Mailinhalte nicht vervielfältigt.
- **Befund / Nachtest:** 🟦 Kein Befund; kein Nachtest in dieser Beispielrunde.

### AUTO-07 · Verbindungsunterbrechung während einer Bearbeitung

**Einordnung:** Zur Automatisierung vorgesehen – Browserfall.

**Warum hier:** Die vorhandenen Sicherheits-/Pushprüfungen decken keine reale Offline-/Online-Bedienfolge ab.

**Voraussetzung:** Eigenes unkritisches Testkonto; gespeicherter Ausgangswert; Browser-Entwicklerwerkzeuge mit Offline-Schalter oder andere begrenzte Unterbrechung nur für diesen Browser.

1. Bearbeitungsformular öffnen, einen Namen ändern und die Verbindung nur für diesen Browser unterbrechen.
2. Speichern versuchen; Rückmeldung/Verbindungsanzeige festhalten. Verbindung wiederherstellen und angebotene Wiederverbindung bzw. bewusstes Neuladen durchführen.
3. Konto frisch über die Suche öffnen und gespeicherten Stand feststellen; bei nötig erneutem Speichern genau einmal bewusst auslösen.

**Erwartet:** Es erscheint keine irreführende Erfolgsbestätigung für einen nicht verarbeiteten Vorgang. Nach Wiederverbindung lässt sich der tatsächliche gespeicherte Stand eindeutig feststellen und weiterarbeiten. Verlorene Entwürfe/unklare Bedienung gesondert als Befund erfassen.

- **Status:** 🟦 NICHT AUSGEFÜHRT
- **Beobachtung:** 🟦 Die Offline-/Online-Folge war möglich, wurde im Beispiel aber aus Zeitgründen auf den nächsten Termin verschoben. Sie ist nicht als bestanden bewertet.
- **Varianten / Zeitpunkt:** 🟦 Geplante Offline-Variante im Desktopbrowser; um 19:28 verschoben.
- **Beleg oder fehlende Voraussetzung:** 🟦 Grund: ausgewählter Fall aus Zeitgründen nicht begonnen. Nächster Termin: nach dieser Beispielrunde gesondert planen.
- **Befund / Nachtest:** 🟦 Im Restumfang vermerkt; kein erfolgreicher Nachweis.

### AUTO-08 · Vorgesehener SMTP-Transport mit Auth/TLS und Testpostfach

**Einordnung:** Zur Automatisierung vorgesehen – Integrationsfixture.

**Warum hier:** Mailpit ohne Auth/TLS ist kein Nachweis für das spätere SMTP-Ziel oder externe Postfachzustellung.

**Voraussetzung:** Nur falls dieser Transport eingesetzt werden soll: ausdrücklich freigegebenes eigenes SMTP-Testziel und Empfängerpostfach; Zugangsdaten ausschließlich lokal.

1. Für die isolierte Anwendung Host, Port, Absender, Auth und TLS gemäß dem eigenen Testziel einstellen und vollständig neu starten; bisherige lokale Werte notieren.
2. Eine eindeutige Admin-Testmail sowie einen Queue-Mailablauf an das freigegebene Testpostfach auslösen. Transportannahme und tatsächliche Postfachzustellung getrennt dokumentieren.
3. Server-/Postfachbelege zuordnen; Links müssen auf die eigene erreichbare Testinstanz zeigen. Danach lokale SMTP-Werte wiederherstellen und neu starten.

**Erwartet:** Der tatsächlich vorgesehene Transport arbeitet mit den vereinbarten Auth-/TLS-Einstellungen; beide Nachrichten treffen zuordenbar ein. Nur SMTP-Annahme genügt nicht als Postfachnachweis. Ohne freigegebenes Ziel BLOCKIERT, bei rein lokalem Einsatz NICHT ZUTREFFEND.

- **Status:** 🟦 NICHT ZUTREFFEND
- **Beobachtung:** 🟦 Der beispielhafte Einsatz nutzt ausschließlich lokales Mailpit; ein externes SMTP-/Postfachziel ist für diesen Einsatz nicht vorgesehen. Bei späterem externem Einsatz muss der Fall neu ausgewählt werden.
- **Varianten / Zeitpunkt:** 🟦 Nur lokaler Einsatz gemäß protokolliertem Zweck; um 19:29 als nicht zutreffend eingeordnet.
- **Beleg oder fehlende Voraussetzung:** 🟦 Fachliche Begründung: bewusst reiner lokaler Einsatz. Kein freigegebenes externes SMTP-Ziel und keine externe Zustellung Teil dieses Umfangs.
- **Befund / Nachtest:** 🟦 Im Restumfang vermerkt; kein erfolgreicher Nachweis.

## Befund B-01 und beispielhafte Korrektur

| Feld | Ausgefüllter Beispielwert |
| --- | --- |
| Befund-ID / betroffene Test-IDs | 🟦 B-01; HUM-01, Nachtest zusätzlich HUM-03 |
| Einordnung / Gewicht | 🟦 Erfundenes Darstellungsproblem mit Bedienauswirkung: Schließen-Aktion teilweise verdeckt |
| Ausgangsstand / Umgebung / Zeitpunkt | 🟦 Lernstand A, simuliert 390×844, EN, 15.10.2026 18:10 +02:00 |
| Schritte zum Wiederholen | 🟦 1. Eigenes Konto bearbeiten. 2. Englische Oberfläche bei 390 px ansehen. 3. Titel und Schließen-Aktion vergleichen. |
| Erwartet / beobachtet | 🟦 Beide Elemente getrennt les-/bedienbar; im Szenario überdeckte der Titel den Button. Kein tatsächlicher Befund zur aktuellen Anwendung |
| Beleg | 🟦 E-01, fiktive Screenshotbeschreibung; im echten Protokoll hier eigenen bereinigten Beleg verlinken |
| Nächste Maßnahme / zuständig | 🟦 Beispielperson Pat grenzt die lokale Dialoganordnung ein; keine pauschale Änderung des gesamten Themes |
| Korrekturstand | 🟦 Lernstand B: im Szenario kann der Titel umbrechen und die Schließen-Fläche bleibt frei. Kein echter Fix/Commit ausgeführt; reale Prüfung benötigt tatsächlichen Fix-Commit oder beschriebene lokale Änderungen |
| Nachtest-ID / Ergebnis | 🟦 BEISPIEL-B; HUM-01/HUM-03 in den unten genannten Varianten im Lernbeispiel bestanden |

**So wäre der echte Ablauf:** Befund mit reproduzierbaren Schritten an die zuständige
Person geben. Nach der Umsetzung neuen Code-/Konfigurationsstand erfassen. Bei einer
Java-Änderung IDE kompilieren/neu starten; bei Ressourcen sicherstellen, dass die IDE
die aktuelle Datei kopiert hat, dann aktualisieren. Erst jetzt ursprüngliche Fehlerfolge
und betroffene Nachbarfunktionen nachprüfen. Die automatisierte Nachprüfung passend
zur tatsächlichen Änderung zusätzlich ausführen und ihr echtes Ergebnis getrennt notieren.
Die Beispieldatei behauptet keinen automatisierten Lauf nach einem fiktiven Fix.

## Getrennter Nachtest BEISPIEL-B

| Feld | Ausgefüllter Beispielwert |
| --- | --- |
| Nachtest-ID / zugehöriger Befund | 🟦 BEISPIEL-B / B-01 |
| Neuer Commit / lokale Änderungen | 🟦 Derselbe Basis-Hash als Formatbeispiel; Lernstand B mit beschriebener fiktiver lokaler Titelanordnung, nicht tatsächlich geändert |
| Zeit / Person / Geräte | 🟦 15.10.2026, 19:35–19:50 +02:00; Beispielperson Alex, D1; 390×844 und 1280×900, DE/EN, Zoom 100 %/200 % |
| Wiederholte Test-IDs / Varianten | 🟦 HUM-01: Dialogtitel/Schließen in schmal/breit und DE/EN. HUM-03: Fokus, Escape und Tab bei beiden Zoomstufen |
| Beobachtung / Status / Beleg | 🟦 In der erfundenen Nachprüfung getrennte Titel-/Schließenflächen, sichtbarer Fokus und bedienbare Aktionen; beide gewählten Teilprüfungen BESTANDEN. Fiktive Notiz E-04 |
| Weiterhin ungeprüfter Umfang | 🟦 Screenreader, echte Mailclients, Terminalzustandsbeobachtung und Offline-Folge; keine komplette Wiederholung der ursprünglichen 15 Fälle |

🟦 **Bewertung:** B-01 wäre damit im angegebenen Umfang nachgeprüft. Der ursprüngliche
Lauf A bleibt MIT FEHLERN dokumentiert. Nachtest B ist nur BESTANDEN IM GENANNTEN UMFANG;
alle übrigen offenen Voraussetzungen und ausgelassenen Fälle bleiben offen. Für eine
Gesamtabnahme müssen benötigte Restprüfungen erst nachgeholt werden.

## Abschluss des ursprünglichen Beispiels A

| Zählung im ursprünglichen Protokoll | Anzahl |
| --- | ---: |
| BESTANDEN | 🟦 9 |
| FEHLER | 🟦 1 |
| BLOCKIERT | 🟦 2 |
| NICHT AUSGEFÜHRT | 🟦 2 |
| NICHT ZUTREFFEND | 🟦 1 |
| OFFEN | 🟦 0 |
| Summe ausgewählter Fälle | 🟦 15 |

- **Ergebnis:** 🟦 MIT FEHLERN; zusätzlich unvollständige Pflichtvoraussetzungen und ausgelassene Varianten.
- **Geprüfter Umfang und Restgrenzen:** 🟦 Neun Fälle im Lernbeispiel bestanden; keine Screenreader-/Mailclient-/Offline-/Terminalzustandsfreigabe. Nachtest B zählt separat zwei begrenzte Teilprüfungen.
- **Offene Befunde und nächste Schritte:** 🟦 B-01 mit Verweis auf Nachtest B geschlossen; Screenreader/Auditverfahren organisieren, Mailclients und Offline-Folge terminieren. Externen SMTP-Fall bei später geändertem Einsatzzweck neu auswählen.
- **Wiederherstellung:** 🟦 SMTP wieder A auf 18125, Verbindung online, Basis-URL 127.0.0.1:18090, Bootstrap deaktiviert; eigene Testprozesse nach Ende beendet. Alles nur als erfundenes Vorgehensbeispiel.
- **Ablage:** 🟦 Im echten Projekt wichtige Erkenntnisse als kurze Markdown-Zusammenfassung sichern; lokale Belege nur solange nötig behalten. Hier gibt es keine echten Prüfdaten zum Archivieren.
