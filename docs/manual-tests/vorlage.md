# Manuelle Sonderprüfungen – Vorlage

Version 2.0 · Stand 15.10.2026. **Leere Vorlage: Hier wurde nichts geprüft.**
Anleitung und Fallauswahl: [Übersicht](README.md). Einrichtung: [anwendung.md](anwendung.md).
Ein vollständig erfundenes Lernprotokoll mit Fehler und Nachtest steht in
[vorlage_ausgefüllt.md](vorlage_ausgefüllt.md).

## Verwendung und Status

Nur passende Fälle auswählen. Normale Anmeldung, Registrierung, Tokenlinks, CRUD,
Filter, Pagination, Status und einzelne Formularfehler werden bereits automatisiert
geprüft und sind hier keine erneute vollständige Checkliste. Ein Konten-/Mailablauf
in den Schritten ist nur Vorbereitung für die jeweils ausdrücklich offene Aussage.

**Menschliche Abnahme** bleibt erforderlich, soweit eine Person oder ein echtes
Gerät beurteilen muss. **Zur Automatisierung vorgesehen** bedeutet: derzeit noch
manuell; künftig Browserfall, Betriebs- oder Integrationsfixture. Es ist eine
Zielmarkierung, keine zugesagte Umsetzung und kein bestandener Test.

| Status | Wann verwenden? |
| --- | --- |
| OFFEN | Noch nicht entschieden oder angefangen. |
| BESTANDEN | Alle ausgewählten Varianten ausgeführt und Erwartung beobachtet. |
| FEHLER | Ausgeführt; konkrete Erwartung verletzt. Befund anlegen. |
| BLOCKIERT | Benötigtes Gerät, Werkzeug, Ziel oder belastbarer Nachweis fehlt. Grund und nächste Maßnahme nennen. |
| NICHT AUSGEFÜHRT | Möglich, aber bewusst ausgelassen; Grund und Restumfang nennen. |
| NICHT ZUTREFFEND | Für den vorgesehenen Einsatz fachlich nicht nötig; warum begründen. |

Varianten getrennt bewerten: Ein geprüftes Gerät bestätigt kein weiteres Gerät.
Ein Teilnachweis bei fehlender Voraussetzung macht den ganzen Fall nicht grün.
Bei einem Fix zuerst den neuen Stand erfassen und einen eigenen Nachtest anlegen;
den ursprünglichen Fehler nicht rückwirkend zu BESTANDEN ändern.

## Kopf des Protokolls

| Feld | Eintrag |
| --- | --- |
| Protokoll-ID / Anlass | … |
| Ausgewählte Test-IDs / ausgelassene Varianten | … |
| Beginn / Ende / Zeitzone | … |
| Geprüft von | … |
| Git-Commit / Branch / lokale Änderungen | Vollständige Ausgabe von git rev-parse HEAD; Branch und lokale Änderungen separat: … |
| Startart / URL | IDE oder JAR; tatsächliche erreichbare Adresse: … |
| Datenbank / Properties-Datei | Nur isolierte Pfade, keine Kennwörter/Schlüssel: … |
| Browser / Version / Geräte / OS / Zoom | Tatsächlich verwendete Kombinationen: … |
| Oberflächensprachen / Mailclients / Hilfsmittel | … |
| SMTP-Testziel / verfügbare Betriebsbelege | Host/Port/Modus ohne Zugangsdaten: … |
| Testkonten | Erfundene E-Mail, Rolle und Zweck; Passwort nur lokal: … |
| Einrichtung | Bereit oder fehlende Voraussetzung mit Grund: … |
| Belegablage | Eigener lokaler Prüfungsordner; alternativ präzise Textbeobachtung: … |

Die Quelle für automatisierte Nachweise ist [browser-tests/abdeckung.md](../../browser-tests/abdeckung.md).
Diesen Nachweis und einen manuellen Status nicht miteinander verwechseln.

## Prüffälle

<a id="hum-01"></a>

### HUM-01 · Visuelle Gesamtabnahme

**Einordnung:** Menschliche Abnahme.

**Warum hier:** Die Layoutprüfungen messen Grenzen und bekannte Styles; sie beurteilen keine gestalterische Stimmigkeit.

**Voraussetzung:** Desktop und kleines Fenster; DE und EN; freigegebener Designstandard als Vergleich.

1. Login, Registrierung, Home und alle Adminbereiche ansehen; je eine Erfolgsmeldung und Feldfehlermeldung öffnen.
2. Benutzer bearbeiten und Löschbestätigung öffnen, ohne zu löschen. Tabelle, Filter, Overlays und Dialogkopf/-fuß zusammen beurteilen.
3. Lesbarkeit, Kontraste, Abstände, Ausrichtung, Iconwirkung und verständliche Hervorhebung aktiver/gefährlicher Aktionen bewerten. Auffälligkeiten konkret benennen.

**Erwartet:** Die Oberfläche wirkt zusammengehörig und ruhig; relevante Texte/Aktionen sind gut lesbar und eindeutig. Rein persönlicher Änderungswunsch wird von einem Bedienhindernis unterschieden.

- **Status:** OFFEN
- **Beobachtung:** …
- **Varianten / Zeitpunkt:** …
- **Beleg oder fehlende Voraussetzung:** …
- **Befund / Nachtest:** …

<a id="hum-02"></a>

### HUM-02 · Echtes Mobilgerät und Bildschirmtastatur

**Einordnung:** Menschliche Abnahme.

**Warum hier:** Die Browser-Suites simulieren Fenstergrößen; sie ersetzen kein echtes Touchgerät und dessen Tastatur.

**Voraussetzung:** Eigenes Mobilgerät im lokalen Netz; Testanwendung erreichbar, base.url passend; Schritte zur Netzfreigabe in anwendung.md.

1. In Hoch- und Querformat ein längeres Formular ausfüllen. Bildschirmtastatur, Scrollen, Datumsauswahl und Passwortanzeige benutzen.
2. Filterpanel und Date-/TimePicker sowie Benutzerdialog bedienen; Öffnen/Schließen und erreichbare Aktionen bei geöffneter Tastatur beurteilen.
3. Touchziele, Scrollverhalten, Tastaturüberlagerung und Rotation bewerten; Gerät, OS und Browser festhalten.

**Erwartet:** Die Aufgabe lässt sich ohne verdeckte Pflichtfelder oder unerreichbare Aktionen erledigen. Touchziele und Rückmeldungen sind praktisch bedienbar; nach Rotation bleiben Eingaben nachvollziehbar.

- **Status:** OFFEN
- **Beobachtung:** …
- **Varianten / Zeitpunkt:** …
- **Beleg oder fehlende Voraussetzung:** …
- **Befund / Nachtest:** …

<a id="hum-03"></a>

### HUM-03 · Tastatur, Zoom und sichtbarer Fokus

**Einordnung:** Menschliche Abnahme.

**Warum hier:** Login und einzelne Shortcuts sind automatisiert; die durchgehende Bedienbarkeit und menschliche Lesbarkeit sind es nicht.

**Voraussetzung:** Desktop ohne Maus; Browserzoom 100 % und 200 %, zusätzlich eigene Kontrast-/Anzeigeeinstellung sofern genutzt.

1. Mit Tab, Umschalt+Tab, Enter, Leertaste und Escape durch Navigation, Benutzerliste, Filter, Dialoge und Info-Overlays gehen.
2. Bei 200 % dieselbe Aufgabe abschließen; Fokus nach Schließen eines Dialogs/Overlays und Rückkehr in die Tabelle verfolgen.
3. Fokusanzeige, logische Reihenfolge, Erreichbarkeit der Schließen-/Lösch-/Speicheraktionen und Lesbarkeit beurteilen.

**Erwartet:** Fokus bleibt sichtbar und verständlich; alle für die gewählte Aufgabe nötigen Aktionen sind erreichbar. Kein unbeabsichtigtes Speichern, keine Fokusfalle und kein Verlust notwendiger Information durch Zoom.

- **Status:** OFFEN
- **Beobachtung:** …
- **Varianten / Zeitpunkt:** …
- **Beleg oder fehlende Voraussetzung:** …
- **Befund / Nachtest:** …

<a id="hum-04"></a>

### HUM-04 · Screenreader und verständliche Ansagen

**Einordnung:** Menschliche Abnahme.

**Warum hier:** Es existiert kein vollständiger Screenreader-Nachweis. Automatische Regeln können die Verständlichkeit der Ansagen nur ergänzen.

**Voraussetzung:** Tatsächlich verfügbarer Screenreader; Produkt/Version, Browser und Bedienmodus protokollieren.

1. Login oder Registrierung lesen lassen: Labels, Pflichtfelder, Passworteingabe und Fehlerbezug beurteilen.
2. Benutzerliste, Filterstatus/Badges und Datum-/Uhrzeitauswahl bedienen; Sortierung und deaktivierte Elemente anhören.
3. Bearbeitungs-/Löschdialog und Benachrichtigung öffnen. Titel, Zweck, gefährliche Aktion und Fokuswechsel beurteilen.

**Erwartet:** Zweck, aktueller Zustand und Fehlerhilfe sind hörbar verständlich. Dialoge besitzen einen erkennbaren Kontext; Iconaktionen sind sinnvoll benannt. Ohne Screenreader bleibt der Fall BLOCKIERT, nicht bestanden.

- **Status:** OFFEN
- **Beobachtung:** …
- **Varianten / Zeitpunkt:** …
- **Beleg oder fehlende Voraussetzung:** …
- **Befund / Nachtest:** …

<a id="hum-05"></a>

### HUM-05 · Verständlichkeit der deutschen und englischen Texte

**Einordnung:** Menschliche Abnahme.

**Warum hier:** Sprachwechsel und konkrete Texte sind technisch geprüft; sprachliche Qualität und Handlungsverständnis erfordern eine Person.

**Voraussetzung:** DE und EN jeweils über den vorhandenen Sprachschalter wählen; Testkonto und absichtliche Formularfehler.

1. Feldhilfen, Konfigurationsinfos, Passwortregeln, Filterbeschreibung und Sicherheits-/Löschtexte in beiden Sprachen lesen.
2. Beurteilen, ob verständlich ist, was geändert wird, wann ein Neustart nötig ist und wie ein Fehler korrigiert werden kann.
3. Uneinheitliche Begriffe, irreführende Aussagen und schwer verständliche Sätze mit konkretem Textvorschlag notieren.

**Erwartet:** Die Texte sind konsistent, verständlich und fachlich passend. Eine korrekte Schlüsselauflösung allein genügt nicht. Anonyme Formularseiten besitzen keinen eigenen Sprachschalter; keine künstlichen Sessionereignisse verwenden.

- **Status:** OFFEN
- **Beobachtung:** …
- **Varianten / Zeitpunkt:** …
- **Beleg oder fehlende Voraussetzung:** …
- **Befund / Nachtest:** …

<a id="hum-06"></a>

### HUM-06 · Darstellung in tatsächlich verwendeten Mailprogrammen

**Einordnung:** Menschliche Abnahme.

**Warum hier:** Mailpit prüft SMTP/MIME, Inhalt und Inline-Bilder; Gmail-/Outlook-/Mobilansicht und menschliche Lesbarkeit sind damit nicht belegt.

**Voraussetzung:** Eigene tatsächlich erzeugte Testmail; Mailprogramm benennen. EML-Import nur falls unterstützt; externe Zustellung getrennt unter AUTO-08.

1. Verifikations- und Resetmail in den tatsächlich genutzten Clients öffnen; breite/schmale Ansicht und hell/dunkel soweit verfügbar vergleichen.
2. Logo, Abstände, Text, Footer und erkennbare Link-/Buttonbeschriftung beurteilen. Verhalten bei blockierten Bildern ansehen.
3. Prüfen, ob sich der Handlungszweck ohne externe Bilder verstehen lässt und das Linkziel zur eigenen Testinstanz passt.

**Erwartet:** Die Mail ist lesbar, vertrauenswürdig zuzuordnen und bedienbar; Layout oder Clientfarben verdecken keine Aktion. Nicht verfügbare Clients einzeln als nicht geprüft aufführen; ein Mailpit-Screenshot ersetzt sie nicht.

- **Status:** OFFEN
- **Beobachtung:** …
- **Varianten / Zeitpunkt:** …
- **Beleg oder fehlende Voraussetzung:** …
- **Befund / Nachtest:** …

<a id="hum-07"></a>

### HUM-07 · Recovery-Entscheidung bei unklarem Mailversand

**Einordnung:** Menschliche Abnahme.

**Warum hier:** Die konservative Quarantäne ist technisch geprüft; die Bewertung unvollständiger Betriebsbelege ist eine menschliche Entscheidung.

**Voraussetzung:** Nur eine Schreibtischübung mit erfundenen Versandbelegen; Recovery-Vertrag: docs/email-queue-processing.md im Projekt.

1. Szenario A: SMTP-Annahme belegt, Datenbank-Ergebnis fehlt. Szenario B: nur Timeout/fehlende Postfachmail. Szenario C: Nichtversand positiv belegt.
2. Je Szenario Entscheidung begründen: kein erneuter Versand bei Annahme; bei unklarem Ausgang Quarantäne/Eskalation; bei belegtem Nichtversand erst autorisierte und kontrollierte Wiederfreigabe prüfen.
3. Benötigte Queue-ID, Versandfenster, beendete Worker, Providerbelege und Tokenzustand benennen. Einen widersprechenden oder fehlenden Beleg bewusst berücksichtigen.

**Erwartet:** Entscheidung und Begründung folgen dem Recovery-Vertrag. Fehlende Mail und alter Claim gelten nicht als Nichtversandbeweis. Diese Übung verändert keine Daten und führt keinen Retry aus.

- **Status:** OFFEN
- **Beobachtung:** …
- **Varianten / Zeitpunkt:** …
- **Beleg oder fehlende Voraussetzung:** …
- **Befund / Nachtest:** …

<a id="auto-01"></a>

### AUTO-01 · Serverneustart mit derselben Testdatenbank

**Einordnung:** Zur Automatisierung vorgesehen – Betriebsfixture.

**Warum hier:** Der Runner verwendet frische Daten; der Chromium-Neustart von remember-me ist kein Serverneustart mit derselben Datenbank.

**Voraussetzung:** Eigene Dateidatenbank, ein über die UI angelegtes Testkonto und ein gespeicherter Konfigurationswert; Bootstrap deaktiviert.

1. Konto über die Oberfläche wiederfinden; E-Mail, Freigabe/Rolle, Kontenzahl und einen nicht geheimen Konfigurationswert als Ausgangsstand notieren.
2. Nur die eigene Anwendung ordentlich stoppen und mit exakt derselben Properties-Datei/Datenbank neu starten; keine neue Datenbank wählen.
3. Frisch anmelden und die notierten Werte erneut prüfen. Ein normales Sitzungsende durch den Serverneustart ist kein Datenverlust.

**Erwartet:** Konto und gespeicherte Einstellungen bleiben erhalten; keine zusätzlichen Bootstrapkonten oder Datenverluste. Alte offene Browseransichten nicht als alleinigen Persistenznachweis verwenden.

- **Status:** OFFEN
- **Beobachtung:** …
- **Varianten / Zeitpunkt:** …
- **Beleg oder fehlende Voraussetzung:** …
- **Befund / Nachtest:** …

<a id="auto-02"></a>

### AUTO-02 · SMTP-Vorgaben beim Erststart und Wechsel nach Neustart

**Einordnung:** Zur Automatisierung vorgesehen – Betriebsfixture.

**Warum hier:** Lokale Mails sind geprüft; die gezielte Erststart-/Konfigurationswechsel-/Neustartfolge ist nicht automatisiert.

**Voraussetzung:** Für die Erststartvariante eigene leere DB; Mailpit A auf 18125/18126 und optional B auf 18127/18128. Andere Ports nur mit dokumentierter Anpassung.

1. Beim allerersten Start mit vorbereiteter Properties-Datei eine eindeutige Admin-Testmail senden; Empfang in A prüfen, bevor ein zusätzlicher Neustart stattfindet.
2. Im Adminbereich mail.port auf 18127 speichern. Vor dem Prozessneustart eine zweite eindeutig benannte Testmail senden: der bestehende SMTP-Sender nutzt weiterhin A.
3. Dieselbe Anwendung/DB neu starten, dritte eindeutige Mail senden und Empfang in B prüfen. mail.port danach zurücksetzen und nochmals neu starten.

**Erwartet:** Konfigurierte Defaults gelten bereits bei leerer DB. Eine gespeicherte Änderung des startgebundenen SMTP-Ports wirkt erst nach Neustart; bestehende DB-Werte werden nicht durch app.config.defaults.* überschrieben. B fehlt: Wechselvariante BLOCKIERT.

- **Status:** OFFEN
- **Beobachtung:** …
- **Varianten / Zeitpunkt:** …
- **Beleg oder fehlende Voraussetzung:** …
- **Befund / Nachtest:** …

<a id="auto-03"></a>

### AUTO-03 · Realer SMTP-Ausfall mit anschließendem Wiederanlauf

**Einordnung:** Zur Automatisierung vorgesehen – Betriebsfixture.

**Warum hier:** Retry und Fälligkeit sind JUnit-abgedeckt; reales Stoppen/Starten des eigenen SMTP-Ziels mit laufender Anwendung fehlt im Browserlauf.

**Voraussetzung:** Nur eigenes Mailpit; Queue-Retrylimit größer null und notierter Ausgangswert. Diese Prüfung registriert ein neues Konto, statt den direkten Admin-Mailtest als Queue-Test zu verwenden.

1. Eigenes Mailpit vor der Anfrage stoppen. Über Registrierung eine neue eindeutige Testadresse verwenden und den erwarteten Verbindungsfehler im Anwendungslog zuordnen.
2. Mailpit wieder starten; mindestens den Retryabstand von einer Minute und weitere Schedulerzeit abwarten. Ausgang nur anhand des eigenen Empfängers/Zeitfensters zuordnen.
3. Empfang und verwendbaren Link prüfen; einen weiteren Retryzeitraum beobachten und dokumentieren, ob eine weitere identische Zustellung erscheint.

**Erwartet:** Bei nachgewiesenem Verbindungsfehler vor SMTP-Annahme kann die wartende Mail nach Wiederanlauf zugestellt werden. Keine vorschnelle Bewertung nach wenigen Sekunden. Unklarer Transportausgang ist ein anderer Fall; keine Queuezeilen manuell zurücksetzen.

- **Status:** OFFEN
- **Beobachtung:** …
- **Varianten / Zeitpunkt:** …
- **Beleg oder fehlende Voraussetzung:** …
- **Befund / Nachtest:** …

<a id="auto-04"></a>

### AUTO-04 · Dauerhafter SMTP-Ausfall und brauchbare Betriebsdiagnose

**Einordnung:** Zur Automatisierung vorgesehen – Betriebsfixture.

**Warum hier:** Retrylimit und Auditverträge sind technisch abgedeckt; dauerhafter realer Ausfall und praktische Beobachtbarkeit sind nicht durch Browser belegt.

**Voraussetzung:** Isolierte Umgebung, ursprüngliches Retrylimit notiert, für diesen Fall z. B. auf 1 reduziert; eigene Logs und vorgesehenes Betriebsbeobachtungsverfahren verfügbar.

1. Mailpit vor Registrierung einer neuen eindeutigen Adresse stoppen und bis zum initialen Versuch plus konfigurierten Wiederholungen warten; mindestens eine Minute Retryabstand beachten.
2. Anhand zusammengehöriger Logs/Auditbelege feststellen, ob die Verarbeitung begrenzt endet und ein Betreiber Empfängerbezug, Zeitpunkt und nächste Maßnahme zuordnen kann.
3. Mailpit wieder starten und länger als einen Retryabstand beobachten; die terminal fehlgeschlagene Nachricht darf nicht automatisch neu versandt werden. Einstellungen wiederherstellen.

**Erwartet:** Die reale Verarbeitung endet begrenzt und ist diagnostizierbar. Bei ausgefallenem SMTP ist auch die Admin-Benachrichtigung nicht garantiert; fehlende Audit-/Sendebelege bedeuten keinen Erfolg. Ohne belastbare Einsicht BLOCKIERT; keine neue Admin-Queueoberfläche voraussetzen.

- **Status:** OFFEN
- **Beobachtung:** …
- **Varianten / Zeitpunkt:** …
- **Beleg oder fehlende Voraussetzung:** …
- **Befund / Nachtest:** …

<a id="auto-05"></a>

### AUTO-05 · Sprachwechsel mit sichtbaren Feldfehlern und Entwürfen

**Einordnung:** Zur Automatisierung vorgesehen – Browserfall.

**Warum hier:** Entwürfe/Navigation und Infos sind geprüft; bereits ausgelöste Admin-Feldfehler beim Sprachwechsel bleiben offen.

**Voraussetzung:** Admin-Konfiguration; ein ungültiger unkritischer Entwurf, z. B. ungültige Basis-URL. Ursprünglichen Wert notieren.

1. Ungültigen Entwurf eingeben und Speichern auslösen; sichtbare Feld-/Benachrichtigungsfehler dokumentieren.
2. Über den tatsächlich erreichbaren Benutzermenü-Sprachschalter DE nach EN und zurück wechseln. Labels, noch sichtbare Fehlertexte und Erhalt des Entwurfs prüfen.
3. Entwurf korrigieren oder verwerfen; danach die gespeicherte Originalkonfiguration prüfen.

**Erwartet:** Weiterhin sichtbare Texte folgen der gewählten Sprache; der ungültige Wert wird nicht gespeichert und der Entwurf nicht still übernommen. Bereits geschlossene Meldungen sind nicht nachträglich prüfbar. Keine Sprachwahl hinter einem modalen Dialog erzwingen.

- **Status:** OFFEN
- **Beobachtung:** …
- **Varianten / Zeitpunkt:** …
- **Beleg oder fehlende Voraussetzung:** …
- **Befund / Nachtest:** …

<a id="auto-06"></a>

### AUTO-06 · Neue Mails nach Sprachwechsel für dasselbe Konto

**Einordnung:** Zur Automatisierung vorgesehen – Browserfall.

**Warum hier:** Getrennte DE-/EN-Läufe und Kontosprache sind geprüft; wechselnde neue Mails desselben bestehenden Kontos mit vorheriger Mailhistorie bleiben ergänzbar.

**Voraussetzung:** Ein bereits aktiviertes Testkonto; eigenes Mailpit; für jede Mail Zeitpunkt/Empfänger und Sprache der auslösenden Sitzung notieren.

1. Mit dem vorhandenen Konto die Sprache wechseln und abmelden. Auf Home den angebotenen Sprachschalter bewusst auf EN setzen, dann die Passwortanfrage öffnen und eine neue Mail anfordern.
2. Nach Zustellung auf Home DE wählen und für dasselbe Konto erneut anfordern. Die zwei neuen Nachrichten anhand von Zeitpunkt/Mail-ID unterscheiden, nicht eine alte Mail wieder auswerten.
3. Betreff, Inhalt und Footer vergleichen; die vorher empfangene englische Nachricht erneut ansehen.

**Erwartet:** Neue Mails folgen der Sprache der auslösenden Sitzung; gespeicherte Kontosprache allein bestimmt eine anonyme Anfrage nicht. Frühere Mails ändern ihre Sprache nicht nachträglich. Es werden ausschließlich neue zulässige Anfragen benutzt.

- **Status:** OFFEN
- **Beobachtung:** …
- **Varianten / Zeitpunkt:** …
- **Beleg oder fehlende Voraussetzung:** …
- **Befund / Nachtest:** …

<a id="auto-07"></a>

### AUTO-07 · Verbindungsunterbrechung während einer Bearbeitung

**Einordnung:** Zur Automatisierung vorgesehen – Browserfall.

**Warum hier:** Die vorhandenen Sicherheits-/Pushprüfungen decken keine reale Offline-/Online-Bedienfolge ab.

**Voraussetzung:** Eigenes unkritisches Testkonto; gespeicherter Ausgangswert; Browser-Entwicklerwerkzeuge mit Offline-Schalter oder andere begrenzte Unterbrechung nur für diesen Browser.

1. Bearbeitungsformular öffnen, einen Namen ändern und die Verbindung nur für diesen Browser unterbrechen.
2. Speichern versuchen; Rückmeldung/Verbindungsanzeige festhalten. Verbindung wiederherstellen und angebotene Wiederverbindung bzw. bewusstes Neuladen durchführen.
3. Konto frisch über die Suche öffnen und gespeicherten Stand feststellen; bei nötig erneutem Speichern genau einmal bewusst auslösen.

**Erwartet:** Es erscheint keine irreführende Erfolgsbestätigung für einen nicht verarbeiteten Vorgang. Nach Wiederverbindung lässt sich der tatsächliche gespeicherte Stand eindeutig feststellen und weiterarbeiten. Verlorene Entwürfe/unklare Bedienung gesondert als Befund erfassen.

- **Status:** OFFEN
- **Beobachtung:** …
- **Varianten / Zeitpunkt:** …
- **Beleg oder fehlende Voraussetzung:** …
- **Befund / Nachtest:** …

<a id="auto-08"></a>

### AUTO-08 · Vorgesehener SMTP-Transport mit Auth/TLS und Testpostfach

**Einordnung:** Zur Automatisierung vorgesehen – Integrationsfixture.

**Warum hier:** Mailpit ohne Auth/TLS ist kein Nachweis für das spätere SMTP-Ziel oder externe Postfachzustellung.

**Voraussetzung:** Nur falls dieser Transport eingesetzt werden soll: ausdrücklich freigegebenes eigenes SMTP-Testziel und Empfängerpostfach; Zugangsdaten ausschließlich lokal.

1. Für die isolierte Anwendung Host, Port, Absender, Auth und TLS gemäß dem eigenen Testziel einstellen und vollständig neu starten; bisherige lokale Werte notieren.
2. Eine eindeutige Admin-Testmail sowie einen Queue-Mailablauf an das freigegebene Testpostfach auslösen. Transportannahme und tatsächliche Postfachzustellung getrennt dokumentieren.
3. Server-/Postfachbelege zuordnen; Links müssen auf die eigene erreichbare Testinstanz zeigen. Danach lokale SMTP-Werte wiederherstellen und neu starten.

**Erwartet:** Der tatsächlich vorgesehene Transport arbeitet mit den vereinbarten Auth-/TLS-Einstellungen; beide Nachrichten treffen zuordenbar ein. Nur SMTP-Annahme genügt nicht als Postfachnachweis. Ohne freigegebenes Ziel BLOCKIERT, bei rein lokalem Einsatz NICHT ZUTREFFEND.

- **Status:** OFFEN
- **Beobachtung:** …
- **Varianten / Zeitpunkt:** …
- **Beleg oder fehlende Voraussetzung:** …
- **Befund / Nachtest:** …

## Befund und Korrektur

Diesen Block je eigenständigem Problem kopieren. Es wird ein tatsächlich beobachteter
Fehler dokumentiert, nicht aus einem fehlenden Werkzeug auf einen Anwendungsfehler geschlossen.

| Feld | Eintrag |
| --- | --- |
| Befund-ID / betroffene Test-IDs | z. B. B-01; … |
| Einordnung / Gewicht | Fehler / Umgebungsproblem / Verbesserungsvorschlag; konkrete Auswirkung: … |
| Ausgangsstand / Umgebung / Zeitpunkt | … |
| Schritte zum Wiederholen | … |
| Erwartet / beobachtet | … |
| Beleg | … |
| Nächste Maßnahme / zuständig | … |
| Korrekturstand | Tatsächlicher Fix-Commit oder ausdrücklich lokale Änderung: … |
| Nachtest-ID / Ergebnis | Neues Protokoll oder klar getrennte Nachtestsektion: … |

## Nachtest nach einer Korrektur

| Feld | Eintrag |
| --- | --- |
| Nachtest-ID / zugehöriger Befund | … |
| Neuer Commit / lokale Änderungen | … |
| Zeit / Person / Geräte | … |
| Wiederholte Test-IDs / Varianten | Ursprüngliche Fehlerfolge und betroffene Nachbarfunktionen: … |
| Beobachtung / Status / Beleg | … |
| Weiterhin ungeprüfter Umfang | … |

## Abschluss

| Zählung im ursprünglichen Protokoll | Anzahl |
| --- | ---: |
| BESTANDEN | … |
| FEHLER | … |
| BLOCKIERT | … |
| NICHT AUSGEFÜHRT | … |
| NICHT ZUTREFFEND | … |
| OFFEN | … |
| Summe ausgewählter Fälle | … |

- **Ergebnis:** OFFEN / BESTANDEN IM GENANNTEN UMFANG / MIT FEHLERN / UNVOLLSTÄNDIG.
- **Geprüfter Umfang und Restgrenzen:** …
- **Offene Befunde und nächste Schritte:** …
- **Wiederherstellung:** Verbindung online, Test-SMTP und Konfigurationswerte im notierten Ausgangszustand; …
- **Ablage:** Wichtige Erkenntnisse ggf. eigenständig in docs/meilensteine/ zusammengefasst; …

Nachtests separat zählen. BLOCKIERT, OFFEN oder ausgelassene erforderliche Varianten
bleiben ungeprüft; NICHT ZUTREFFEND braucht eine Begründung. Ein bestandener Teilnachtest
ist keine vollständige Freigabe. Lokale Protokolle/Belege dürfen nach Auswertung
bereinigt werden; Datenbanken und lokale Konfiguration nicht unbemerkt löschen.
