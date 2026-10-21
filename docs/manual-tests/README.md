# Manuelle Sonderprüfungen

Dieser Katalog enthält sieben menschliche Abnahmen und acht noch offene
Automatisierungskandidaten. Er beschreibt konkrete heutige Prüfschritte, keine
bestandenen Ergebnisse. Normale automatisierte Journeys stehen im
[Browserkatalog](../../browser-tests/README.md); technische Regeln prüft JUnit.

## Auswahl und Protokoll

Für Design/Text/Bedienung HUM-01–05 auswählen, für Mailprogramme HUM-06 und für
die Entscheidung bei unklarem Versand HUM-07. AUTO-01–08 bleiben gezielte offene
Betriebs-/Browser-/Integrationsprüfungen, bis ein echter automatisierter Nachweis
vorliegt. Keine menschliche Abnahme durch einen Screenshot allein ersetzen.

Das kurze eigene Protokoll nennt Code-/Änderungsstand, Sprache, Gerät/Browser,
ausgewählte IDs, Schritte und tatsächliche Beobachtung. Status pro Variante:
OFFEN, BESTANDEN, FEHLER, BLOCKIERT, NICHT AUSGEFÜHRT oder NICHT ZUTREFFEND mit
Grund. Leere Felder bedeuten nicht bestanden. Fehler mit Erwartung/Ist erfassen;
nach Korrektur einen getrennten Nachtest notieren. Temporäre Belege lokal im
eigenen Testordner halten, relevante aktuelle Ergebnisse in [status.md](../status.md).

## Eigene Testumgebung

Die [Docker-Testumgebung](../docker/README.md) stellt Anwendung und Mailpit bereit.
Für lokale Sonderprüfungen eine eigene IDE-Startkonfiguration und neue Dateidatenbank
verwenden. [properties.example](properties.example) nach
`data/manual-tests/EIGENER-LAUF/manual-tests.properties` kopieren, `__LAUF__`
ersetzen und ausschließlich erfundene Kontodaten sowie einen eigenen Testschlüssel
eintragen. Bestehende Entwicklerdaten und fremde Prozesse erhalten.

Als Programmargument der separaten IDE-Konfiguration verwenden:

```text
--spring.config.import=file:./data/manual-tests/EIGENER-LAUF/manual-tests.properties[encoding=utf-8]
```

Java25 und Projektroot als Arbeitsverzeichnis verwenden. Geerbte DB-/SMTP-Overrides
dürfen die eigene Testdatei nicht übersteuern. Auf frischer Maven-Einbindung zuerst
Shared installieren. Der Dateiimport bleibt verpflichtend, damit ein falscher Pfad
abgebrochen wird. `server.port` und `base.url` müssen zusammenpassen; vorbereitet
ist 18090. SMTP ist für eigenes Mailpit auf 18125 vorbereitet, Weboberfläche 18126.
Nur das selbst bereitgestellte Mailpit stoppen; keine Weiterleitung an reale Postfächer.

Für Superadmin-Funktionen ein eigenes Bootstrapkonto über `app.bootstrap.superadmins`
vorbereiten; ein gewöhnlicher Admin kann sich diese Rolle nicht selbst vergeben.
Für Neustartfälle denselben eigenen Datenbankpfad verwenden. Bei deaktiviertem
Bootstrap muss das nötige Testkonto bereits über die UI angelegt sein. Gespeicherte
Admin-Konfiguration hat Vorrang vor `app.config.defaults.*`; startgebundene
SMTP-Änderungen brauchen einen Neustart. Ausgangswerte vor Teständerungen notieren.

Für ein echtes Mobilgerät muss die eigene Testinstanz gezielt im lokalen Netz
erreichbar sein: Testadresse und `base.url` anpassen, Bindeadresse/Firewall nur für
das benötigte private Testnetz freigeben und danach zurücknehmen. Nicht die
Entwicklungsinstanz oder H2-Konsole allgemein freigeben. Ohne diese Vorbereitung
die Gerätevariante als BLOCKIERT führen.

## Prüffälle

<a id="hum-01"></a>

### HUM-01 · Visuelle Gesamtabnahme

**Einordnung:** Menschliche Abnahme.

**Voraussetzung:** Desktop und kleines Fenster; DE und EN; freigegebener Designstandard als Vergleich.

1. Login, Registrierung, Home und alle Adminbereiche ansehen; je eine Erfolgsmeldung und Feldfehlermeldung öffnen.
2. Benutzer bearbeiten und Löschbestätigung öffnen, ohne zu löschen. Tabelle, Filter, Overlays und Dialogkopf/-fuß zusammen beurteilen.
3. Lesbarkeit, Kontraste, Abstände, Ausrichtung, Iconwirkung und verständliche Hervorhebung aktiver/gefährlicher Aktionen bewerten. Auffälligkeiten konkret benennen.

**Erwartet:** Die Oberfläche wirkt zusammengehörig und ruhig; relevante Texte/Aktionen sind gut lesbar und eindeutig. Rein persönlicher Änderungswunsch wird von einem Bedienhindernis unterschieden.

<a id="hum-02"></a>

### HUM-02 · Echtes Mobilgerät und Bildschirmtastatur

**Einordnung:** Menschliche Abnahme.

**Voraussetzung:** Eigenes Mobilgerät im lokalen Netz; Testanwendung erreichbar, base.url passend; Hinweise zur Netzfreigabe unten beachten.

1. In Hoch- und Querformat ein längeres Formular ausfüllen. Bildschirmtastatur, Scrollen, Datumsauswahl und Passwortanzeige benutzen.
2. Filterpanel und Date-/TimePicker sowie Benutzerdialog bedienen; Öffnen/Schließen und erreichbare Aktionen bei geöffneter Tastatur beurteilen.
3. Touchziele, Scrollverhalten, Tastaturüberlagerung und Rotation bewerten; Gerät, OS und Browser festhalten.

**Erwartet:** Die Aufgabe lässt sich ohne verdeckte Pflichtfelder oder unerreichbare Aktionen erledigen. Touchziele und Rückmeldungen sind praktisch bedienbar; nach Rotation bleiben Eingaben nachvollziehbar.

<a id="hum-03"></a>

### HUM-03 · Tastatur, Zoom und sichtbarer Fokus

**Einordnung:** Menschliche Abnahme.

**Voraussetzung:** Desktop ohne Maus; Browserzoom 100 % und 200 %, zusätzlich eigene Kontrast-/Anzeigeeinstellung sofern genutzt.

1. Mit Tab, Umschalt+Tab, Enter, Leertaste und Escape durch Navigation, Benutzerliste, Filter, Dialoge und Info-Overlays gehen.
2. Bei 200 % dieselbe Aufgabe abschließen; Fokus nach Schließen eines Dialogs/Overlays und Rückkehr in die Tabelle verfolgen.
3. Fokusanzeige, logische Reihenfolge, Erreichbarkeit der Schließen-/Lösch-/Speicheraktionen und Lesbarkeit beurteilen.

**Erwartet:** Fokus bleibt sichtbar und verständlich; alle für die gewählte Aufgabe nötigen Aktionen sind erreichbar. Kein unbeabsichtigtes Speichern, keine Fokusfalle und kein Verlust notwendiger Information durch Zoom.

<a id="hum-04"></a>

### HUM-04 · Screenreader und verständliche Ansagen

**Einordnung:** Menschliche Abnahme.

**Voraussetzung:** Tatsächlich verfügbarer Screenreader; Produkt/Version, Browser und Bedienmodus protokollieren.

1. Login oder Registrierung lesen lassen: Labels, Pflichtfelder, Passworteingabe und Fehlerbezug beurteilen.
2. Benutzerliste, Filterstatus/Badges und Datum-/Uhrzeitauswahl bedienen; Sortierung und deaktivierte Elemente anhören.
3. Bearbeitungs-/Löschdialog und Benachrichtigung öffnen. Titel, Zweck, gefährliche Aktion und Fokuswechsel beurteilen.

**Erwartet:** Zweck, aktueller Zustand und Fehlerhilfe sind hörbar verständlich. Dialoge besitzen einen erkennbaren Kontext; Iconaktionen sind sinnvoll benannt. Ohne Screenreader bleibt der Fall BLOCKIERT, nicht bestanden.

<a id="hum-05"></a>

### HUM-05 · Verständlichkeit der deutschen und englischen Texte

**Einordnung:** Menschliche Abnahme.

**Voraussetzung:** DE und EN jeweils über den vorhandenen Sprachschalter wählen; Testkonto und absichtliche Formularfehler.

1. Feldhilfen, Konfigurationsinfos, Passwortregeln, Filterbeschreibung und Sicherheits-/Löschtexte in beiden Sprachen lesen.
2. Beurteilen, ob verständlich ist, was geändert wird, wann ein Neustart nötig ist und wie ein Fehler korrigiert werden kann.
3. Uneinheitliche Begriffe, irreführende Aussagen und schwer verständliche Sätze mit konkretem Textvorschlag notieren.

**Erwartet:** Die Texte sind konsistent, verständlich und fachlich passend. Eine korrekte Schlüsselauflösung allein genügt nicht. Anonyme Formularseiten besitzen keinen eigenen Sprachschalter; keine künstlichen Sessionereignisse verwenden.

<a id="hum-06"></a>

### HUM-06 · Darstellung in tatsächlich verwendeten Mailprogrammen

**Einordnung:** Menschliche Abnahme.

**Voraussetzung:** Eigene tatsächlich erzeugte Testmail; Mailprogramm benennen. EML-Import nur falls unterstützt; externe Zustellung getrennt unter AUTO-08.

1. Verifikations- und Resetmail in den tatsächlich genutzten Clients öffnen; breite/schmale Ansicht und hell/dunkel soweit verfügbar vergleichen.
2. Logo, Abstände, Text, Footer und erkennbare Link-/Buttonbeschriftung beurteilen. Verhalten bei blockierten Bildern ansehen.
3. Prüfen, ob sich der Handlungszweck ohne externe Bilder verstehen lässt und das Linkziel zur eigenen Testinstanz passt.

**Erwartet:** Die Mail ist lesbar, vertrauenswürdig zuzuordnen und bedienbar; Layout oder Clientfarben verdecken keine Aktion. Nicht verfügbare Clients einzeln als nicht geprüft aufführen; ein Mailpit-Screenshot ersetzt sie nicht.

<a id="hum-07"></a>

### HUM-07 · Recovery-Entscheidung bei unklarem Mailversand

**Einordnung:** Menschliche Abnahme.

**Voraussetzung:** Nur eine Schreibtischübung mit erfundenen Versandbelegen; Recovery-Vertrag: docs/mailqueue.md im Projekt.

1. Szenario A: SMTP-Annahme belegt, Datenbank-Ergebnis fehlt. Szenario B: nur Timeout/fehlende Postfachmail. Szenario C: Nichtversand positiv belegt.
2. Je Szenario Entscheidung begründen: kein erneuter Versand bei Annahme; bei unklarem Ausgang Quarantäne/Eskalation; bei belegtem Nichtversand erst autorisierte und kontrollierte Wiederfreigabe prüfen.
3. Benötigte Queue-ID, Versandfenster, beendete Worker, Providerbelege und Tokenzustand benennen. Einen widersprechenden oder fehlenden Beleg bewusst berücksichtigen.

**Erwartet:** Entscheidung und Begründung folgen dem Recovery-Vertrag. Fehlende Mail und alter Claim gelten nicht als Nichtversandbeweis. Diese Übung verändert keine Daten und führt keinen Retry aus.

<a id="auto-01"></a>

### AUTO-01 · Bestandsabgleich nach Neustart ohne Bootstrap

**Einordnung:** Zur Automatisierung vorgesehen – Betriebsfixture.

**Voraussetzung:** Eigene Dateidatenbank, ein über die UI angelegtes Testkonto und ein gespeicherter Konfigurationswert; Bootstrap deaktiviert.

1. Konto über die Oberfläche wiederfinden; E-Mail, Freigabe/Rolle, Kontenzahl und einen nicht geheimen Konfigurationswert als Ausgangsstand notieren.
2. Nur die eigene Anwendung ordentlich stoppen und mit exakt derselben Properties-Datei/Datenbank neu starten; keine neue Datenbank wählen.
3. Frisch anmelden und die notierten Werte erneut prüfen. Ein normales Sitzungsende durch den Serverneustart ist kein Datenverlust.

**Erwartet:** Konto und gespeicherte Einstellungen bleiben erhalten; keine zusätzlichen Bootstrapkonten oder Datenverluste. Alte offene Browseransichten nicht als alleinigen Persistenznachweis verwenden.

<a id="auto-02"></a>

### AUTO-02 · SMTP-Vorgaben beim Erststart und Wechsel nach Neustart

**Einordnung:** Zur Automatisierung vorgesehen – Betriebsfixture.

**Voraussetzung:** Für die Erststartvariante eigene leere DB; Mailpit A auf 18125/18126 und optional B auf 18127/18128. Andere Ports nur mit dokumentierter Anpassung.

1. Beim allerersten Start mit vorbereiteter Properties-Datei eine eindeutige Admin-Testmail senden; Empfang in A prüfen, bevor ein zusätzlicher Neustart stattfindet.
2. Im Adminbereich mail.port auf 18127 speichern. Vor dem Prozessneustart eine zweite eindeutig benannte Testmail senden: der bestehende SMTP-Sender nutzt weiterhin A.
3. Dieselbe Anwendung/DB neu starten, dritte eindeutige Mail senden und Empfang in B prüfen. mail.port danach zurücksetzen und nochmals neu starten.

**Erwartet:** Konfigurierte Defaults gelten bereits bei leerer DB. Eine gespeicherte Änderung des startgebundenen SMTP-Ports wirkt erst nach Neustart; bestehende DB-Werte werden nicht durch app.config.defaults.* überschrieben. B fehlt: Wechselvariante BLOCKIERT.

<a id="auto-03"></a>

### AUTO-03 · Realer SMTP-Ausfall mit anschließendem Wiederanlauf

**Einordnung:** Zur Automatisierung vorgesehen – Betriebsfixture.

**Voraussetzung:** Nur eigenes Mailpit; Queue-Retrylimit größer null und notierter Ausgangswert. Diese Prüfung registriert ein neues Konto, statt den direkten Admin-Mailtest als Queue-Test zu verwenden.

1. Eigenes Mailpit vor der Anfrage stoppen. Über Registrierung eine neue eindeutige Testadresse verwenden und den erwarteten Verbindungsfehler im Anwendungslog zuordnen.
2. Mailpit wieder starten; mindestens den Retryabstand von einer Minute und weitere Schedulerzeit abwarten. Ausgang nur anhand des eigenen Empfängers/Zeitfensters zuordnen.
3. Empfang und verwendbaren Link prüfen; einen weiteren Retryzeitraum beobachten und dokumentieren, ob eine weitere identische Zustellung erscheint.

**Erwartet:** Bei nachgewiesenem Verbindungsfehler vor SMTP-Annahme kann die wartende Mail nach Wiederanlauf zugestellt werden. Keine vorschnelle Bewertung nach wenigen Sekunden. Unklarer Transportausgang ist ein anderer Fall; keine Queuezeilen manuell zurücksetzen.

<a id="auto-04"></a>

### AUTO-04 · Dauerhafter SMTP-Ausfall und brauchbare Betriebsdiagnose

**Einordnung:** Zur Automatisierung vorgesehen – Betriebsfixture.

**Voraussetzung:** Isolierte Umgebung, ursprüngliches Retrylimit notiert, für diesen Fall z. B. auf 1 reduziert; eigene Logs und vorgesehenes Betriebsbeobachtungsverfahren verfügbar.

1. Mailpit vor Registrierung einer neuen eindeutigen Adresse stoppen und bis zum initialen Versuch plus konfigurierten Wiederholungen warten; mindestens eine Minute Retryabstand beachten.
2. Anhand zusammengehöriger Logs/Auditbelege feststellen, ob die Verarbeitung begrenzt endet und ein Betreiber Empfängerbezug, Zeitpunkt und nächste Maßnahme zuordnen kann.
3. Mailpit wieder starten und länger als einen Retryabstand beobachten; die terminal fehlgeschlagene Nachricht darf nicht automatisch neu versandt werden. Einstellungen wiederherstellen.

**Erwartet:** Die reale Verarbeitung endet begrenzt und ist diagnostizierbar. Bei ausgefallenem SMTP ist auch die Admin-Benachrichtigung nicht garantiert; fehlende Audit-/Sendebelege bedeuten keinen Erfolg. Ohne belastbare Einsicht BLOCKIERT; keine neue Admin-Queueoberfläche voraussetzen.

<a id="auto-05"></a>

### AUTO-05 · Sprachwechsel mit sichtbaren Feldfehlern und Entwürfen

**Einordnung:** Zur Automatisierung vorgesehen – Browserfall.

**Voraussetzung:** Admin-Konfiguration; ein ungültiger unkritischer Entwurf, z. B. ungültige Basis-URL. Ursprünglichen Wert notieren.

1. Ungültigen Entwurf eingeben und Speichern auslösen; sichtbare Feld-/Benachrichtigungsfehler dokumentieren.
2. Über den tatsächlich erreichbaren Benutzermenü-Sprachschalter DE nach EN und zurück wechseln. Labels, noch sichtbare Fehlertexte und Erhalt des Entwurfs prüfen.
3. Entwurf korrigieren oder verwerfen; danach die gespeicherte Originalkonfiguration prüfen.

**Erwartet:** Weiterhin sichtbare Texte folgen der gewählten Sprache; der ungültige Wert wird nicht gespeichert und der Entwurf nicht still übernommen. Bereits geschlossene Meldungen sind nicht nachträglich prüfbar. Keine Sprachwahl hinter einem modalen Dialog erzwingen.

<a id="auto-06"></a>

### AUTO-06 · Neue Mails nach Sprachwechsel für dasselbe Konto

**Einordnung:** Zur Automatisierung vorgesehen – Browserfall.

**Voraussetzung:** Ein bereits aktiviertes Testkonto; eigenes Mailpit; für jede Mail Zeitpunkt/Empfänger und Sprache der auslösenden Sitzung notieren.

1. Mit dem vorhandenen Konto die Sprache wechseln und abmelden. Auf Home den angebotenen Sprachschalter bewusst auf EN setzen, dann die Passwortanfrage öffnen und eine neue Mail anfordern.
2. Nach Zustellung auf Home DE wählen und für dasselbe Konto erneut anfordern. Die zwei neuen Nachrichten anhand von Zeitpunkt/Mail-ID unterscheiden, nicht eine alte Mail wieder auswerten.
3. Betreff, Inhalt und Footer vergleichen; die vorher empfangene englische Nachricht erneut ansehen.

**Erwartet:** Neue Mails der anonymen Passwortanfrage folgen der Sprache der auslösenden Sitzung; gespeicherte Kontosprache allein bestimmt eine anonyme Anfrage nicht. Sicherheitsmails zu angemeldeten Passwort-/E-Mail-Änderungen folgen dagegen bewusst der gespeicherten Kontosprache; das ist ein eigener automatisierter Vertrag. Frühere Mails ändern ihre Sprache nicht nachträglich. Es werden ausschließlich neue zulässige Anfragen benutzt.

<a id="auto-07"></a>

### AUTO-07 · Verbindungsunterbrechung während einer Bearbeitung

**Einordnung:** Zur Automatisierung vorgesehen – Browserfall.

**Voraussetzung:** Eigenes unkritisches Testkonto; gespeicherter Ausgangswert; Browser-Entwicklerwerkzeuge mit Offline-Schalter oder andere begrenzte Unterbrechung nur für diesen Browser.

1. Bearbeitungsformular öffnen, einen Namen ändern und die Verbindung nur für diesen Browser unterbrechen.
2. Speichern versuchen; Rückmeldung/Verbindungsanzeige festhalten. Verbindung wiederherstellen und angebotene Wiederverbindung bzw. bewusstes Neuladen durchführen.
3. Konto frisch über die Suche öffnen und gespeicherten Stand feststellen; bei nötig erneutem Speichern genau einmal bewusst auslösen.

**Erwartet:** Es erscheint keine irreführende Erfolgsbestätigung für einen nicht verarbeiteten Vorgang. Nach Wiederverbindung lässt sich der tatsächliche gespeicherte Stand eindeutig feststellen und weiterarbeiten. Verlorene Entwürfe/unklare Bedienung gesondert als Befund erfassen.

<a id="auto-08"></a>

### AUTO-08 · Vorgesehener SMTP-Transport mit Auth/TLS und Testpostfach

**Einordnung:** Zur Automatisierung vorgesehen – Integrationsfixture.

**Voraussetzung:** Nur falls dieser Transport eingesetzt werden soll: ausdrücklich freigegebenes eigenes SMTP-Testziel und Empfängerpostfach; Zugangsdaten ausschließlich lokal.

1. Für die isolierte Anwendung Host, Port, Absender, Auth und TLS gemäß dem eigenen Testziel einstellen und vollständig neu starten; bisherige lokale Werte notieren.
2. Eine eindeutige Admin-Testmail sowie einen Queue-Mailablauf an das freigegebene Testpostfach auslösen. Transportannahme und tatsächliche Postfachzustellung getrennt dokumentieren.
3. Server-/Postfachbelege zuordnen; Links müssen auf die eigene erreichbare Testinstanz zeigen. Danach lokale SMTP-Werte wiederherstellen und neu starten.

**Erwartet:** Der tatsächlich vorgesehene Transport arbeitet mit den vereinbarten Auth-/TLS-Einstellungen; beide Nachrichten treffen zuordenbar ein. Nur SMTP-Annahme genügt nicht als Postfachnachweis. Ohne freigegebenes Ziel BLOCKIERT, bei rein lokalem Einsatz NICHT ZUTREFFEND.


Die [Mailqueue](../mailqueue.md), der [Theme-Standard](../theme.md) und die
[Sicherheitsverträge](../sicherheit.md) beschreiben die jeweiligen Erwartungen.
