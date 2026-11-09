# Manuelle Sonderprüfungen

Dieser Katalog beschreibt menschliche Abnahmen und Prüfanleitungen für besondere
Betriebs-, Browser- und Integrationsfälle. Er ist kein Ergebnisbericht und keine
zweite Aufgabenliste. Offener Automatisierungsbedarf steht ausschließlich im
[Testbedarf](../testbedarf.md). Normale automatisierte Journeys stehen im
[Browserkatalog](../../browser-tests/README.md); technische Regeln prüft JUnit.

## Auswahl und Protokoll

Für Design/Text/Bedienung HUM-01–05 auswählen, für Mailprogramme HUM-06 und für
die Entscheidung bei unklarem Versand HUM-07. Die automatisierten Neustart-,
SMTP-, Sprachwechsel- und Offlinefälle stehen im Browserkatalog. AUTO-08 ist eine
optionale Prüfung eines tatsächlich vorgesehenen externen SMTP-Anbieters.
Keine menschliche Abnahme durch einen Screenshot allein ersetzen.

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

## Mailbeispiele zur Sichtprüfung

[`ManualEmailPreviewIT`](../../app/src/test/java/de/derpeterson/app/manual/ManualEmailPreviewIT.java)
sendet vorhandene Mailvorlagen an genau ein ausdrücklich angegebenes Postfach.
Er ist durch seinen `*IT`-Namen außerhalb des normalen `mvn test`-Laufs und zusätzlich
ohne `-Dmail.preview.send=true` deaktiviert. Kein `@Disabled` entfernen.

In IntelliJ eine lokale JUnit-Startkonfiguration für diese Klasse anlegen, als
VM-Option `-Dmail.preview.send=true` und folgende Umgebungsvariablen setzen:

| Variable | Inhalt |
| --- | --- |
| `MAIL_PREVIEW_TO` | Deine einzelne reale Empfängeradresse |
| `MAIL_PREVIEW_LANGUAGES` | `de`, `en` oder `de,en`; Standard beide |
| `MAIL_PREVIEW_FROM` | Für deinen SMTP-Zugang erlaubte Absenderadresse |
| `MAIL_PREVIEW_SMTP_HOST` | SMTP-Server |
| `MAIL_PREVIEW_SMTP_PORT` | Port; Standard `587` |
| `MAIL_PREVIEW_SMTP_USERNAME` | SMTP-Benutzername |
| `MAIL_PREVIEW_SMTP_PASSWORD` | SMTP-Passwort bzw. freigegebenes App-Passwort |
| `MAIL_PREVIEW_SECURITY` | `starttls` (Standard), `ssl` oder ausdrücklich `none` |
| `MAIL_PREVIEW_SMTP_AUTH` | Standard `true`; `false` nur für einen passenden eigenen Relay/Testserver |

Für direktes TLS typischerweise Port `465` und `ssl` wählen; maßgeblich sind die
Vorgaben des eigenen SMTP-Zugangs. Zugangsdaten und reale Adressen nur lokal in der
Startkonfiguration halten, nicht in Java-Dateien oder versionierten Konfigurationen.
Es werden weder `.env` noch die Anwendungsdatenbank geladen; kein Anwendungsstart nötig.

Alternativ mit denselben lokalen Umgebungsvariablen vom Projektroot starten:

```powershell
.\mvnw.cmd -pl app -am test '-Dtest=ManualEmailPreviewIT' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dmail.preview.send=true'
```

Der Test verwendet den normalen MIME-Versand einschließlich eingebettetem Logo und
die vorhandenen HTML-Vorlagen: Aktivierung, Passwortreset, Passwort geändert,
E-Mail-Änderung bestätigen und E-Mail geändert. Dazu kommen die lokalisierte
Admin-Testmail und einmal die technische, auch in der Anwendung englische
Versandfehlermeldung als Klartext. Ergebnis: **8 Mails bei einer Sprache, 15 bei DE+EN**.
Die Zwei-Faktor-Vorlage enthält ausschließlich einen synthetischen Beispielcode.
Für unveränderte gleiche Inhalte an alter/neuer Adresse gibt es nur ein Beispiel.
`PROMOTION` ist ein Queue-Typ ohne eigenen Versandablauf oder eigene Vorlage.

Die Betreffzeilen beginnen mit `[SICHTTEST][DE/EN]`. Alle Kontodaten sind erfunden;
Links zeigen absichtlich auf `https://preview.invalid/` und enthalten keine gültigen
Tokens. Das prüft Aussehen und SMTP-Annahme, keine Registrierung oder Kontomutation.
Optisch nach [HUM-06](#hum-06) beurteilen. Ein grüner JUnit-Test bestätigt weder
Posteingangszustellung noch gutes Layout. Bei Versandfehler stoppt der Test ohne
Retry; vor erneutem Start bereits eingegangene Beispiele beachten.

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

**Voraussetzung:** Eigenes Mobilgerät im lokalen Netz; Testanwendung erreichbar, base.url passend; Hinweise zur Netzfreigabe im Abschnitt „Eigene Testumgebung“ beachten.

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

**Voraussetzung:** Beispiele aus dem JUnit-Sichttest oder eigene tatsächlich erzeugte Testmails; Mailprogramm benennen. EML-Import nur falls unterstützt; technische Zustellung getrennt unter AUTO-08.

1. Alle vorhandenen Mailvorlagen in den tatsächlich genutzten Clients öffnen; breite/schmale Ansicht und hell/dunkel soweit verfügbar vergleichen.
2. Logo, Abstände, Text, Footer und erkennbare Link-/Buttonbeschriftung beurteilen. Verhalten bei blockierten Bildern ansehen.
3. Prüfen, ob sich der Handlungszweck ohne externe Bilder verstehen lässt. Sichttestbeispiele verwenden bewusst `preview.invalid`; funktionale Testmails müssen zur eigenen Testinstanz führen.

**Erwartet:** Die Mail ist lesbar, vertrauenswürdig zuzuordnen und bedienbar; Layout oder Clientfarben verdecken keine Aktion. Nicht verfügbare Clients einzeln als nicht geprüft aufführen; ein Mailpit-Screenshot ersetzt sie nicht.

<a id="hum-07"></a>

### HUM-07 · Recovery-Entscheidung bei unklarem Mailversand

**Einordnung:** Menschliche Abnahme.

**Voraussetzung:** Nur eine Schreibtischübung mit erfundenen Versandbelegen; Recovery-Vertrag: docs/mailqueue.md im Projekt.

1. Szenario A: SMTP-Annahme belegt, Datenbank-Ergebnis fehlt. Szenario B: nur Timeout/fehlende Postfachmail. Szenario C: Nichtversand positiv belegt.
2. Je Szenario Entscheidung begründen: kein erneuter Versand bei Annahme; bei unklarem Ausgang Quarantäne/Eskalation; bei belegtem Nichtversand erst autorisierte und kontrollierte Wiederfreigabe prüfen.
3. Benötigte Queue-ID, Versandfenster, beendete Worker, Providerbelege und Tokenzustand benennen. Einen widersprechenden oder fehlenden Beleg bewusst berücksichtigen.

**Erwartet:** Entscheidung und Begründung folgen dem Recovery-Vertrag. Fehlende Mail und alter Claim gelten nicht als Nichtversandbeweis. Diese Übung verändert keine Daten und führt keinen Retry aus.

<a id="auto-08"></a>

### AUTO-08 · Vorgesehener SMTP-Transport mit Auth/TLS und Testpostfach

**Einordnung:** Optionale Anbieterabnahme; bei rein lokalem Mailpit-Betrieb nicht erforderlich.

SMTP-Authentifizierung, STARTTLS und direktes TLS sind vorhanden. Dieser Fall prüft
die konkrete Anbieteranbindung einschließlich der Anwendungs-Mailqueue; er beschreibt
keine fehlende Funktion. Der Mail-Sichttest prüft Vorlagen und SMTP-Annahme separat.

**Voraussetzung:** Nur falls dieser Transport eingesetzt werden soll: ausdrücklich freigegebenes eigenes SMTP-Testziel und Empfängerpostfach; Zugangsdaten ausschließlich lokal.

1. Für die isolierte Anwendung Host, Port, Absender, Auth und TLS gemäß dem eigenen Testziel einstellen und vollständig neu starten; bisherige lokale Werte notieren.
2. Eine eindeutige Admin-Testmail sowie einen Queue-Mailablauf an das freigegebene Testpostfach auslösen. Transportannahme und tatsächliche Postfachzustellung getrennt dokumentieren.
3. Server-/Postfachbelege zuordnen; Links müssen auf die eigene erreichbare Testinstanz zeigen. Danach lokale SMTP-Werte wiederherstellen und neu starten.

**Erwartet:** Der tatsächlich vorgesehene Transport arbeitet mit den vereinbarten Auth-/TLS-Einstellungen; beide Nachrichten treffen zuordenbar ein. Nur SMTP-Annahme genügt nicht als Postfachnachweis. Ohne freigegebenes Ziel BLOCKIERT, bei rein lokalem Einsatz NICHT ZUTREFFEND.


Die [Mailqueue](../mailqueue.md), der [Theme-Standard](../theme.md) und die
[Sicherheitsverträge](../sicherheit.md) beschreiben die jeweiligen Erwartungen.

<a id="hum-08"></a>

### HUM-08 · Eigene Google-/Microsoft-Anwendungen

**Einordnung:** Anbieterintegration und menschliche Abnahme. Keine automatische Anmeldung
mit echten Konten. Die isolierten OIDC-Regressionen ersetzen diesen Durchlauf nicht.

Die einmalige Einrichtung erfolgt unter Administration → Konfiguration → Anmeldeanbieter.
Dort die Rücksprungadresse kopieren, beim jeweiligen Anbieter hinterlegen und die
erhaltene Client-ID samt Client-Secret eintragen. Die Freigabe gilt nach dem Speichern
für neu aufgerufene Anmeldeseiten. Gespeicherte Geheimnisse bleiben im Formular leer.

**Voraussetzung:** Eigene getrennte Testinstanz, bewusst eingerichtete Clients gemäß
[Einrichtungsanleitung](../externe-anmeldung.md), passende HTTPS-Rücksprungadressen und
ausdrückliche Freigabe für die eigenen Konten. Zugangsdaten ausschließlich lokal halten.
Für tatsächlich benötigte Portal-Verifikationsmails ein ausdrücklich freigegebenes
Testpostfach verwenden. Keine bestehenden Entwicklerkonten als Fixture benutzen.

1. Google sowie Microsoft mit privatem Konto und Firmen-/Schulkonto prüfen. Anbieterzustimmung,
   Abbruch, Rücksprung und benötigte Organisationsfreigaben beachten.
2. Neues externes Konto ohne Portalpasswort registrieren. Fehlende Namen/Kontaktadresse ergänzen.
   Google-Adressnachweis beziehungsweise eigene Portalaktivierung prüfen.
3. Zum bestehenden Konto anmelden und einen weiteren Anbieter bewusst verknüpfen.
   Gleiche Kontaktadressen allein dürfen kein bestehendes Konto öffnen.
4. Eine konkrete Sicherheitsaktion mit geltender 2FA-Regel auswählen und einen
   eigenen aktuellen TOTP oder Ersatzcode verwenden. Anbieterbestätigung und
   Portalpasswort allein dürfen sie nicht freigeben. Die erstmalige Einrichtung
   mit Google verlangt zusätzlich den begrenzten Einmalcode an die bestätigte
   Portaladresse. Sie ersetzt keinen früheren oder verlorenen Faktor. Eine frische
   Microsoft-Bestätigung benötigt weiterhin `auth_time`. Kontowechsel beim Anbieter
   darf keinen fremden Nachweis ergeben. Ablehnung kehrt mit passender Meldung zur
   Kontosicherheit zurück und startet keine Registrierung.
5. Optional ein Portalpasswort einrichten, erneutes Anmelden und bewusste gespeicherte Anmeldung
   prüfen. Einen weiteren Anbieter trennen und die letzte Anmeldemöglichkeit geschützt lassen.
6. DE/EN, Desktop und Smartphone sowie Darstellungs-/Tastaturverhalten der Anbieterbuttons
   und der ergänzenden Registrierung beurteilen.

**Erwartet:** Nur die geprüfte Anbieteridentität führt zum zugeordneten lokalen Konto.
Portalrollen, Sperren und Sicherheitsnachweise bleiben maßgeblich. Anbieter-/Mandantenregeln
und nicht ausgeführte Varianten ausdrücklich dokumentieren. Keine Geheimnisse protokollieren.

<a id="hum-09"></a>

### HUM-09 · Eigener Authenticator und Ersatzcode-Dokument

**Einordnung:** Manuelle Prüfung der 2FA-Bedienung mit echter Hardware und einer
Authenticator-App. Die automatisierten RFC-, Zeit- und Replayprüfungen ergänzen sie.

Auf einer eigenen isolierten Testinstanz einen neuen Authenticator mit einer frei
gewählten TOTP-App am echten Smartphone einrichten. QR und alternativ den manuellen
Schlüssel verwenden, ersten Code eingeben und die Sicherheitsmail beurteilen.
Ersatzcodes vollständig kopieren und das PDF in DE/EN herunterladen. Logo,
Kontozuordnung, Erklärung, Lesbarkeit und Druck prüfen. Nur synthetische Konten und
Codes verwenden, keine echten Geheimnisse in Abnahmeunterlagen aufnehmen.

Nach Abmeldung erneut anmelden und einen TOTP sowie einen noch unbenutzten Ersatzcode
erproben. Navigation, Refresh und zweiter Tab innerhalb derselben gültigen Sitzung
benötigen keinen neuen Anmeldenachweis. Sensible Aktionen verlangen nach der Regel
ihre eigene Bestätigung. Authenticatorwechsel mit vorhandenem Nachweis durchführen,
neue Codes sichern und verlorenen Zugriff über die eingeschränkte Hilfsanfrage
durchgehen. Ein Ticket oder die versandte Anleitung darf die Sperre nicht aufheben.

**Autofill auf echten Geräten.** In Einrichtung und Anmeldebestätigung die
Autofill-Funktion des verwendeten Browsers oder Passwortmanagers in DE/EN
auf Desktop und Smartphone prüfen. Ein vollständiger Code löst genau eine
Prüfung aus. Ein Teilcode löst keine aus und lässt das Formular geöffnet.
Nach einem abgelehnten Code erneut automatisch ausfüllen und den
Bestätigungsbutton zusätzlich mit einer manuellen Eingabe prüfen.
Gerät, Betriebssystem, Browser oder Passwortmanager und Ergebnis festhalten.

Zusätzlich vor der normalen Codeeingabe **Authenticator wiederherstellen** wählen
und einen unbenutzten Ersatzcode für genau diesen Wechsel verwenden. Bis zum ersten
Code der neuen App darf die Sitzung keinen anderen Kontobereich öffnen. Abbruch
und erneuten Einstieg beurteilen. Erst der erfolgreiche Wechsel verbraucht den
Ersatzcode und ersetzt die alte App. Die automatisierte Prüfung deckt auch den
Fall mit nur einem verbliebenen Code ab.

Mit Superadmin die Rollenregeln, Feldhilfen und Fristeneinheiten beurteilen. Ein normaler
Admin darf weder 2FA-Regeln noch Anmeldeanbieter verwalten. Bedienung und Texte in DE/EN
auf Desktop und Smartphone gemeinsam abnehmen. Die Startfreigabe für Entwicklungs-
Ausnahmen bleibt bei dieser regulären Abnahme ausgeschaltet.

<a id="hum-10"></a>

### HUM-10 · Systemscrollleisten und Kontrastmodus

**Offene Geräteprüfung.** In einer eigenen Testumgebung die systemseitig
dauerhafte Anzeige von Scrollleisten aktivieren. Auf Desktop und Smartphone in
DE/EN in Ticketverlauf, Dialog und langer Auswahlliste bis zum Ende scrollen.
Text, Auswahl und Aktionen müssen vollständig sichtbar und bedienbar bleiben.
Anschließend im tatsächlichen Kontrastmodus des Geräts ein langes Auswahlmenü
öffnen. Die Scrollleiste, der Tastaturfokus und der ausgewählte Eintrag müssen
erkennbar bleiben. Pro Gerät Betriebssystem, Browser und die verwendeten
Systemeinstellungen festhalten.
