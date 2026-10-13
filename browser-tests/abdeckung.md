# Browserabdeckung und offene Punkte

**Stand: 13.10.2026 – aktueller Design-/Mailstand vollständig geprüft: 1055 Anwendungstests, 26 Helper-/Reportertests und je 84 Browserfälle auf DE/EN erfolgreich.**
Bedienung und Aufbau stehen in der [Browser-Testanleitung](README.md).
Historische Run-Berichte bleiben unverändert; hier steht der aktuelle Prüfstand.

## Aktueller vollständiger Prüfstand – 13.10.2026

Der aktuelle Stand einschließlich 96×96-Logo, größerem Servicenamen (Lumo XL),
kleiner weicher Platzhalterschrift, angepassten Statusicons und Reset-Erfolgscard
wurde vollständig geprüft. Copyright verwendet den DE-/EN-Standardtext oder den
eigenen DB-Eintrag. Neu erzeugte Verifikations-/Resetmails enthalten ein echtes
CID-Inline-PNG, die Cosmos-Palette und einen sicheren konfigurierbaren Fußtext;
MJML und daraus erzeugtes HTML sind synchron.

| Prüfung | Ergebnis |
| --- | --- |
| Shared-Modul | Clean install erfolgreich, aktuelle Getter verwendet |
| Anwendung / JaCoCo, Java 25 | 1055/1055, 0 Fehler/Fehlschläge/Skips; Bericht erzeugt |
| Browser-Helper/Reporter | 26/26, 0 Fehler/Fehlschläge/Skips |
| Docker-Produktionsbuild | Erfolgreich, frisches Produktions-JAR mit Frontend |
| Browser Deutsch | 84/84, 0 Fehler/Skips |
| Browser Englisch | 84/84, 0 Fehler/Skips, dasselbe App-Image |
| Aufbau, Abbau, Markdown/HTML-Berichte | Beide Läufe erfolgreich; eigene Testumgebungen entfernt |

- [Aktueller vollständiger DE-Bericht](runs/2026-10-13T13-05-10_UTC__a972ffbf__6c6370b4/report.md)
- [Aktueller vollständiger EN-Bericht](runs/2026-10-13T13-23-24_UTC__a972ffbf__eb540930/report.md)

Die Fälle COSMOS-01A–E enthalten fünf Fenstergrößen einschließlich 1280×1200 zur
Prüfung der Zentrierung der langen Registrierung bei ausreichend Platz.
COSMOS-02 prüft die tatsächliche neue Reset-Erfolgscard. COSMOS-03 stellt den
Copyright-Text über die Oberfläche ein und prüft denselben sicheren Text auch in
echten Verifikations- und Resetmails. Mailpit zeigt die Inline-Bilder geladen an;
Screenshots und Traces sind in den Berichten enthalten.

Gezielte MIME-Roundtrips prüfen passende Content-ID, PNG-Bytes, UTF-8-Inhalt und
Logo-Lesefehler vor dem Transport. Bekannte und unklare Versandfehler, Tokenrotation,
Rollback, Kontosperren und die bestehenden Nebenläufigkeitsregressionen bleiben
geprüft. Der Copyright-Formatter prüft den lokalen Jahreswechsel per injizierter
Clock; eigene/blanke DB-Texte werden nicht durch den Standard ersetzt.

Der erste Anwendungslauf enthielt veraltete Mailfixtures/-Erwartungen (Base64,
fester Footer, fehlender neuer Configwert). Diese wurden angepasst; der finale
Clean-Volltest ist grün. Es wurden keine Tests entfernt, deaktiviert oder übergangen.

Arbeitsbranch `main`, Ausgangscommit `a972ffbf`, lokale Änderungen noch nicht
committet. Geprüftes Image: `sha256:be8c5f9b842642ae2e128cc2cfb7034405064dac47da63c43dbb0576e4ddc49e`.
Alle Zustände über Oberfläche und reguläre SMTP-Testmails in frischen eigenen
Docker-/H2-/Mailpit-Daten erzeugt. Bestandsdaten, manuelle Dockerumgebung und echte
Postfächer wurden nicht verändert. Gmail/Outlook-Darstellung, echte mobile Geräte
und die unten genannten Betriebs-/menschlichen Prüfungen bleiben offen.

## Historischer Zwischenstand: Logo-/Zentrierungsanpassung

Das vorhandene Service-Logo steht über dem Servicenamen. Gleich große obere und
untere Grid-Bereiche halten die Card selbst bei ausreichend Platz exakt in der
Bildschirmmitte, unabhängig von Logo/Überschrift und Copyright. Bei wenig Höhe oder
langer mobiler Registrierung wächst die Seite und bleibt scrollbar; alle Inhalte
sind erreichbar. Keine festen Pixel-Offsets oder JavaScript-Größenberechnung.

Die vorhandenen Cosmos-Fälle prüfen jetzt zusätzlich geladenes Logo, dessen Position
vor dem Servicenamen, überlappungsfreie Anordnung und die Cardmitte. Desktop (1280×900)
muss die Cardzentrierung in allen fünf Ansichten erfüllen; passende kleine Fenster
prüfen sie ebenfalls. Sehr niedrige Fenster bleiben echte Bedienungsprüfungen.

| Gezielte Prüfung des neuen Stands | Ergebnis |
| --- | --- |
| Gemeinsame Komponenten/Copyright – JUnit | 6/6, keine Fehler/Fehlschläge/Skips |
| Cosmos-Design Deutsch / Englisch | Jeweils 7/7, keine Fehler/Skips |
| Loginlayout Deutsch / Englisch | Jeweils 6/6, keine Fehler/Skips |
| Frischer Image-Build, Aufbau, Abbau, HTML | Erfolgreich; danach dasselbe Image verwendet |

- [cosmos-design DE](runs/2026-10-13T10-32-46_UTC__a972ffbf__2c1c1d43/report.md)
- [cosmos-design EN](runs/2026-10-13T10-35-51_UTC__a972ffbf__fec50aee/report.md)
- [login-layout DE](runs/2026-10-13T10-36-57_UTC__a972ffbf__8a9dd80e/report.md)
- [login-layout EN](runs/2026-10-13T10-37-45_UTC__a972ffbf__a2eb5fef/report.md)

Dieser historische Zwischenschritt wurde damals nur gezielt geprüft.
Der aktuelle vollständige Prüfstand steht oben; frühere Run-Berichte bleiben erhalten.

## Historischer vollständiger Prüfstand vor Logo-/Zentrierungsanpassung

Arbeitsbranch `main`, Ausgangscommit `a972ffbf` mit den lokalen,
noch nicht committeten Designänderungen. Die beiden Gesamtläufe verwendeten dasselbe
frisch gebaute Produktions-JAR/Image und jeweils eigene frische Docker-/H2-/Mailpit-Daten.

| Prüfung | Ergebnis |
| --- | --- |
| Anwendung und JaCoCo | 1044/1044, keine Fehler/Fehlschläge/Skips |
| Browser-Helper/Reporter | 26/26, keine Fehler/Fehlschläge/Skips |
| Browser Deutsch | 83/83, keine Fehler/Skips |
| Browser Englisch | 83/83, keine Fehler/Skips |
| Aufbau, Abbau und HTML-Berichte | Jeweils erfolgreich |

- [Vollständiger DE-Bericht](runs/2026-10-13T09-02-52_UTC__a972ffbf__77813902/report.md)
- [Vollständiger EN-Bericht](runs/2026-10-13T09-19-06_UTC__a972ffbf__475d1311/report.md)

HTML-Berichte über die jeweilige `html/open-report.bat` öffnen. Screenshots und
Traces liegen im selben Run-Archiv. Es wurden keine Bestandsdaten oder echten
Postfächer verwendet. Der verpackte Docker-Stand ist geprüft; ein separater aktueller
IntelliJ-/Eclipse-Lauf und echte mobile Geräte wurden nicht geprüft.

## Gemeinsames Design der Kontoansichten

Login, Registrierung, Verifikation, Passwortanfrage und Passwortänderung verwenden
gemeinsame Java-/CSS-Bausteine, die Palette der Silent-Cosmos-Referenz, animierte
CSS-Dekoration, lokales Inter und übersetzte Platzhalter. Inter gilt global; Home/Admin
behalten ihre bisherigen Farben. Copyright wird über Allgemein gespeichert, seine
Platzhalter werden ersetzt und HTML-Zeichen als Text angezeigt. Die vorherigen
Hintergrundbilder sind aus den aktiven Ressourcen entfernt; die versionierten
Originale bleiben über Git nachvollziehbar. Ein lokaler Backupordner ist aktuell
nicht vorhanden.

| Fälle | Nachweis im vollständigen DE-/EN-Lauf |
| --- | --- |
| COSMOS-01A–E | Alle fünf Ansichten bei 320×568, 390×844, 844×390, 1280×900 und 1280×1200; keine horizontale Abschneidung, gemeinsame Gestaltung, sichtbare Platzhalterschrift und Screenshots |
| COSMOS-02 | Mobile Registrierung, echte Aktivierungsmail, erfolgreiche Verifikation, echte Reset-Mail, Passwortänderung mit Erfolgscard/Häkchen und Anmeldung bei 390×844 |
| COSMOS-03 | Copyright über Admin gespeichert/neu geladen, sicherer Fußtext mit year/serviceName in UI und echten Verifikations-/Resetmails, geladene Inline-Logos und Wiederherstellung; Admin-Palette unverändert |
| COSMOS-04 | Schmale Fehlermeldung verwendet Card-Verlauf; Schließen erreichbar, Home-Palette unverändert |
| UI-03A–F | Reale Loginbedienung bei fünf Größen einschließlich 360×240, Fehler/Korrektur, Remember-me-Auswahl, Anmeldung und reduzierte Bewegung |

JUnit prüft zusätzlich Copyright-Defaults/Erhalt gespeicherter Werte, sichere Textanzeige,
Notification-Scope sowie wechselnde Registrierungsplatzhalter ohne Verlust der Formulardaten.
Die Anmelde-/Registrierungs-/Tokenaktionen und die Security-Konfiguration wurden gegen
den Ausgangsstand geprüft und blieben unverändert. Der Browser prüft auch die bisherigen
fachlichen Konto-, Admin-, Mail-, Status- und Sicherheitsabläufe.

Sterne behalten 30/45 Sekunden, Nebel 18–24 Sekunden als Animationszeiten.
Reduzierte Bewegung deaktiviert die Animation; kurze Fenster dürfen vertikal scrollen.
Die 84 Browserfälle und 44 ursprünglichen manuellen Fälle haben unterschiedliche IDs
und Teilabläufe. Die Vergleichstabelle unten behauptet keine vollständige 100-%-Ablösung
aller Betriebs- und menschlichen Prüfungen.

## Einheitliche Datum-/Uhrzeitbedienung

DatePicker in Registrierung und Benutzerformular sowie Date-/TimePicker im
Aktivitätsfilter verwenden `autoOpen=false`. Direkte Eingabe öffnet kein Overlay;
Icons und die vorgesehenen Pfeiltasten bieten weiterhin Auswahlmöglichkeiten.

Kurze Beispiele im Adminbereich kommen aus dem gemeinsamen typisierten Katalog:
DE `31.12.1990` / `14:30`, EN `12/31/1990` / `2:30 PM`. „Uhrzeit optional“ steht
klein neben der Aktivitätsüberschrift, nicht mehr als Platzhalter. Der Geburtstag
bleibt beim DE↔EN-Wechsel im Anlegen-/Bearbeiten-Dialog erhalten (technische Regression).

UI-01A/B bestätigen direkte Geburtstagseingabe, Kalenderauswahl/Wiederöffnen und
tatsächliche Registrierung bei 390×844 bzw. 844×390. USER-05B/C bestätigen
kombinierte Filter, direktes Datum, Kalenderauswahl, optionale Uhrzeit und Clear.
Morgen liefert keine Treffer, gestern zeigt das eigene Konto; beide Zeitvarianten
werden über echte Bedienung übernommen. Vaadin darf führende Nullen beim Blur entfernen;
entscheidend ist das exakt geprüfte kanonische Datum und das tatsächliche Filterergebnis.

Der projizierte Kalenderinhalt liegt im DOM beim DatePicker. Der Browserhelfer
verwendet zugängliche Tageszellen mit dem öffentlichen `date`-Part; die virtualisierte
Uhrzeitliste wird über echte Mausbewegungen gescrollt. Anwendungszustände werden
nicht über SQL-/Service-/API-Abkürzungen erzeugt. Echte Geräte und Bildschirmtastaturen
bleiben ergänzende Prüfungen.

## Aktuelle Benutzerverwaltungsfälle

| ID | Aussage | DE-Nachweis | EN-Nachweis |
| --- | --- | --- | --- |
| USER-05A | Zwölf eigene Konten, Suche, exakte Treffer auf zwei Seiten, Seitengröße und Leerzustand | Bestanden | Bestanden |
| USER-05B | Kombinierte Freigabe-/Status-/Rollenfilter, direktes Datum mit optionaler Zeit, Entfernen/Clear | Bestanden | Bestanden |
| USER-05C | Kalender-Icon, Tagesauswahl/Wiederöffnen und optionale Dropdown-Zeit | Bestanden | Bestanden |
| USER-06A | Zentrale Validierung, Dublettenkorrektur, tatsächliche Anmeldung des neuen Kontos | Bestanden | Bestanden |
| USER-06B | Namen/E-Mail speichern, erneut öffnen, Passwort leer unverändert lassen | Bestanden | Bestanden |
| USER-06C | Löschabbruch, Bestätigung und dauerhaftes Nichtvorhandensein | Bestanden | Bestanden |
| USER-06D | Selbstlöschschutz im eigenen Adminformular | Bestanden | Bestanden |
| USER-06E | Suche und Freigabefilter beim Sprachwechsel in beide Richtungen erhalten | Bestanden | Bestanden |
| USER-06F | Neues Passwort speichern, altes abweisen und mit neuem anmelden | Bestanden | Bestanden |

Suche/Pagination, Filter, Anlage, Bearbeitung, Löschabbruch/-bestätigung,
Selbstlöschschutz, Sprachpräferenz und Passwortwechsel bestehen jetzt in beiden
Gesamtläufen. Der Editierbutton wird vor dem echten Mausklick sichtbar gemacht und
fokussiert, damit die native Grid-Scrollbewegung den Klick nicht verliert. Erwartungen,
Speicher-/Anmeldeprüfungen und Sicherheitsregeln bleiben erhalten.

Im aktuellen vollständigen DE-/EN-Lauf sind jeweils alle 84 Browserfälle bestanden.
Die weiterhin nur teilweise abgedeckten ursprünglichen Fälle sind nachfolgend aufgeführt.

## Ergänzte Arbeitspakete

| Paket | Neue UI-Aussagen | Suite / IDs |
| --- | --- | --- |
| 1 | Sperren/Freigeben, gezieltes Aktivieren unverifizierter Konten, Rollenvergabe/-entzug, echte zwei alte Formulare und Selbstschutz | `user-access`: USER-07A–E |
| 2 | Alte privilegierte Callbacks nach Rollenentzug/Sperre/Löschung; alte Verifikations- und Resetbuttons; neutrale gesperrte Anfrage | `session-security`: SESSION-01A–E (einschließlich Remember-me bei Sperre/Löschung); `blocked-links`: SEC-LINK-01A/B und SEC-LINK-02A/B, RESET-03C |
| 3 | Echter Browserneustart mit/ohne Remember-me und nach Logout; Statuswahl, zweiter Tab, automatische Abwesenheit/Rückkehr | `remember-me`: AUTH-07A/B; `user-status`: STATUS-01/02 |
| 4 | Weitere String-/Boolean-/Integerkonfiguration, Unicode-Testmail, echte Tokenabläufe/Resend und Verifikationsanfragefehler | `configuration`: CONFIG-04A–C; `admin-mail`: MAIL-01; `token-expiry`: VER-03A, RESET-06A, VER-02B |

Jeder Fall enthält eigene Schritte und Erwartungen im erzeugten Markdown-Bericht.
Implementierung allein zählt nicht als erfolgreicher Nachweis; Resultate stehen oben.
Die Oberfläche bereitet sämtliche Anwendungszustände vor. Mailpit liefert nur die
eigene tatsächlich versandte SMTP-Mail. Interne Regeln bleiben technische Tests.

## Noch sinnvoll als nächster Schritt

Die vier UI-Pakete sind geprüft. Als Nächstes bieten sich kontrollierte
Betriebsfixtures an: Neustart mit derselben eigenen Testdatenbank, SMTP-Stopp und
Wiederanlauf sowie deren Bericht. Weitere Browserpakete betreffen offene
Adminfehlerdialoge beim Sprachwechsel und schmale Home-/Adminansichten.
Die fünf Kontoansichten einschließlich Reset sind inzwischen responsiv geprüft.
Queue-/Audit-Innenzustände werden dafür nicht in gewöhnliche UI-Tests gepresst.

## Grenzen der Testarten

- Regeln, Transaktionen, Sperren, Parallelität, exakte Zeitgrenzen und interne
  Queue-/Audit-Zustände bleiben in JUnit-/Persistenztests. Ein UI-Erfolg beweist sie nicht vollständig.
- Neustart mit derselben Datenbank, kontrollierter SMTP-Ausfall/Wiederanlauf,
  Retrylimit, unklarer Versand-/Commitausgang und Recovery benötigen eigene begrenzte
  Betriebsfixtures. Keine SQL-/Service-Abkürzungen zum Vorbereiten von Browserzuständen.
- Anonyme Login-/Registrierungs-/Tokenformulare bieten derzeit keinen Sprachschalter.
  Dynamischen Wechsel dort nicht durch künstliche Session-/JavaScript-Ereignisse ersetzen.
- Echte Geräte, Bildschirmtastatur, Zoom, Accessibility, visuelle Qualität und
  menschliche Recovery-Entscheidungen benötigen ergänzende Sonderprüfungen.
- Mailpit belegt lokale SMTP-Zustellung, keine externe Postfachzustellung oder
  Darstellung in Outlook/Gmail. Timing-Schutz, Mehrinstanzbetrieb und externer Betrieb sind nicht belegt.

## Zuordnung der 44 ursprünglichen Fälle

Die alte manuelle Vorlage bleibt eine Vergleichsgrundlage; die Browserdokumentation
und Berichte sind eigenständig. Die IDs der beiden Kataloge können unterschiedliche
Teilabläufe bezeichnen. Ein Browserfall kann mehrere Schritte eines ursprünglichen
Falls abdecken. Es wird kein künstlicher Gesamt-Prozentwert berechnet.

**UI-ABGEDECKT** gilt nur für den beschriebenen Ablauf; **TEILWEISE** lässt Schritte offen.
**TECHNISCH** nennt JUnit-/Persistenzprüfungen ohne einen Browser-/Betriebsnachweis.
**OFFEN** bedeutet nicht automatisch einen bestätigten Anwendungsfehler.

| ID | Gegenstand | Status | Browser-/Techniknachweis | Verbleibender Teil / Grund |
|---|---|---|---|---|
| START-01 | Erststart mit frischer Datenbank | TEILWEISE / TECHNISCH | INFRA-01/02, AUTH-01A, Runner-Aufbau | Frische Docker-Daten, Login/Admin und Rollen bei Anlage sichtbar; Tabellen-/Audit-/Loginspektion kein Browsernachweis |
| START-02 | Erneuter Start erhält Daten und Konfiguration | OFFEN – BETRIEB | Kein Neustartfall mit derselben Datenbank | Runner startet absichtlich jedes Mal frische Daten; benötigt kontrollierte Neustart-Orchestrierung, keine manuelle Umgebung verwenden |
| START-03 | Erststart und Neustart verwenden die richtige Mailkonfiguration | TEILWEISE | REG/VER/RESET, MAIL-01 und SMTP-Mailpit | Erststart-Zustellung und Admin-Testmail belegt; Vergleich Erststart/Neustart bleibt offen |
| REG-01 | Gültige Registrierung | TEILWEISE | REG-01A, VER-01A, SEC-01A | Registrierung/Mail/gesperrter Vorlogin belegt; explizite Admininspektion und genaue Kontozahl fehlen |
| REG-02 | Pflichtfelder und ungültige Eingaben | TEILWEISE / TECHNISCH | REG-02A/B/C/D | Geforderte UI-Eingabevarianten und Korrekturen geprüft; Nichtanlage/Nichtversand jeder einzelnen Variante bleiben gesonderte technische Aussagen |
| REG-03 | Doppelte E-Mail und Kanonisierung | TEILWEISE / TECHNISCH | REG-03A | Exakte/kanonisierte Dublette abgewiesen und Passwort unverändert; genaue Anzahl gespeicherter Identitäten technisch prüfen |
| VER-01 | Gültiger Link und erneute Verwendung | UI-ABGEDECKT | VER-01A/B, MailpitInbox | Echte Mail: eigene Adresse, gewählter Betreff, Name, Service, vollständiger kanonischer DE-/EN-Footer, Platzhalter und lokaler Mailbutton; Aktivierung, Anmeldung und idempotente Wiederverwendung. Externe Mailclients bleiben Sonderprüfungen |
| VER-02 | Ungültiger oder fehlender Token | UI-ABGEDECKT | VER-02A/B | Unbekannter/fehlender Token, Pflichtfeld/Syntaxfehler, Korrektur und erneute echte Mail geprüft |
| VER-03 | Ablauf und erneuter Versand | TEILWEISE / TECHNISCH | VER-03A, CONFIG-03A, Queue-Persistenztests | Echte Minute abgewartet, abgelaufener Mailbutton, erneuter Versand, neue Mail-ID/Link, alter ungültiger Link und Anmeldung geprüft; absichtlich offen gehaltene Queue und konkurrierende Deduplizierung bleiben technisch |
| VER-04 | Adminsperre bleibt trotz alter Links bestehen | UI-ABGEDECKT | SEC-LINK-01A/B (vorher aktiviert: nein/ja) | Unbenutzter wie bereits benutzter Mailbutton und erneuter Versand heben ausdrückliche Adminsperre nicht auf; gespeicherte Freigabe und verweigerter Login geprüft |
| AUTH-01 | Erfolgs- und Fehlerfälle | UI-ABGEDECKT | AUTH-01A–D, REG-01A, SEC-01A, VER-01A | Erfolg/falsches Passwort/unbekannt/unverifiziert/anonym geprüft; zusätzliche hypothetische Sperrregeln nicht behauptet |
| AUTH-02 | Abmeldung, Zurück und bestehende Tabs | TEILWEISE | AUTH-02A | Admin-Tabs, Logout, Zurück und neuer Zugriff belegt; alter geschützter Mutationscallback nicht vollständig geprüft |
| AUTH-03 | Angemeldet bleiben | UI-ABGEDECKT | AUTH-07A/B | Zwei eigene temporäre Profile, tatsächliches Prozessende/Neustart mit und ohne Option; wiederhergestellte Identität, Cookieentfernung und dauerhaftes Logout geprüft. Keine Sitzungscookie-Injektion |
| RESET-01 | Gültiger Reset | UI-ABGEDECKT | RESET-01A, MailpitInbox | Echte eigene Mail, Betreff, Dienst, vollständiger kanonischer DE-/EN-Footer und lokaler Button; neues Passwort funktioniert, altes nicht |
| RESET-02 | Anonyme Antwort verrät keine Konten | UI-ABGEDECKT | RESET-03A/B/C, RESET-02A | Aktive, unbekannte, unverifizierte und administrativ gesperrte Adresse: identischer sichtbarer Anfragehinweis; Syntaxfehler separat. Kein Timing-/Rate-Limit-Nachweis |
| RESET-03 | Passwortregeln, ungültige und verbrauchte Links | UI-ABGEDECKT | RESET-02B/04A/04B | Fehler/Korrektur, unbekannter und verbrauchter Link ohne weiteres Passwortformular; technische Regeln bleiben JUnit |
| RESET-04 | Ablauf auch bei bereits geöffnetem Formular | UI-ABGEDECKT / TECHNISCH | RESET-06A, CONFIG-03C | Formular vor Ablauf geöffnet, echte Mindestdauer abgewartet, alter Submit und neuer Aufruf abgewiesen; neues Passwort abgelehnt, ursprüngliches funktioniert. Exakte Gleichheitsgrenze bleibt JUnit |
| RESET-05 | Zwei geöffnete Formulare für denselben Token | UI-ABGEDECKT | RESET-05A | Zwei getrennte anonyme Kontexte: erstes Speichern gewinnt, alter Submit abgewiesen und richtige Loginidentität geprüft; echte Parallelität separat |
| RESET-06 | Sperre oder Löschung nach Linkerstellung | UI-ABGEDECKT | SEC-LINK-02A/B (Sperre/Löschung) | Echter alter Resetbutton bietet nach Sperre/Löschung kein Passwortformular; Login abgewiesen. Gezielte spätere Freigabe erhält das ursprüngliche Passwort |
| USER-01 | Anlegen, bearbeiten und erneut öffnen | TEILWEISE | USER-05A/06A: Anlage, genaue Suche/Pagination, zentrale Validierung und neue Anmeldung; technische Filter-/Zeitregressionen | Datum/Kalender und optionale Zeit durch USER-05B/C sowie Bearbeitung/erneutes Öffnen durch USER-06B belegt; ausdrückliche Admininspektion aller Stammdaten bleibt ergänzbar |
| USER-02 | Sperren und gezieltes Freigeben | UI-ABGEDECKT | USER-07A/C | Sperren/Freigeben einschließlich frischer Anmeldung und ausdrücklicher Aktivierung unverifizierter Konten geprüft |
| USER-03 | Löschen mit zugehörigen Tokens und Queueeinträgen | TEILWEISE / TECHNISCH | USER-06C, SEC-LINK-02; Löschkaskaden in Persistenztests | UI-Abbruch/Bestätigung und dauerhafte Abwesenheit sowie alter Resetlink nach Löschung geprüft. Interne Token-/Queuekaskaden bleiben technische Aussagen |
| USER-04 | Veraltetes Bearbeitungsformular | UI-ABGEDECKT | USER-07D | Zwei echte gleichzeitig offene Adminformulare: neuere Änderung gespeichert, alter Submit mit eigenem Konflikthinweis abgewiesen, Daten frisch geprüft |
| SEC-01 | Direkte URL und normale Benutzerrolle | UI-ABGEDECKT | AUTH-01D, SEC-01A, USER-07B | Gast/normaler Benutzer ausgeschlossen, Bootstrapadmin sowie tatsächliche Rollenvergabe und späterer Entzug mit neuen Anmeldungen geprüft |
| SEC-02 | Rollenentzug bei offenem Adminformular / Push | TEILWEISE / TECHNISCH | SESSION-01A (Rollenentzug), PushRpcSecurityPersistenceTest | Alter echter privilegierter Browsercallback erhält vor Mutation 401; Probe-Konto fehlt, neuer Adminaufruf ausgeschlossen. Das belegt diesen Flow-Clientpfad, nicht sämtliche WebSocket-Transportvarianten |
| SEC-03 | Sperre und Löschung bei laufender Sitzung | UI-ABGEDECKT / TECHNISCH | SESSION-01B–E (Sperre/Löschung jeweils mit/ohne Remember-me) | Bestehende echte privilegierte Formulare blockiert, Probe-Konto fehlt und erneuter Adminaufruf verlangt Anmeldung. Interne Principal-/Pushvarianten ergänzend technisch |
| SEC-04 | Letzten aktiven Admin schützen | TEILWEISE / TECHNISCH | USER-06D, USER-07E; AdminSecurityPersistenceTest | Eigene Löschung/Sperre/eigener Rollenentzug durch UI-Selbstschutz abgewiesen. Globale Letzter-Admin-Regel und konkurrierende Änderungen bleiben Service-/Persistenzregressionen |
| STATUS-01 | Manueller Status und Aktualisierung | UI-ABGEDECKT | STATUS-01 | Alle angebotenen Statuswerte ausgewählt; sichtbarer aktueller Wert, echte Push-Aktualisierung im zweiten Tab und Speicherung nach Neuladen geprüft |
| STATUS-02 | Automatische Abwesenheit und Rückkehr | UI-ABGEDECKT / TECHNISCH | STATUS-02; UserStatusSchedulerTest/UserStatusPersistenceTest | Konfiguration über UI, echte Inaktivität/Scheduler und Rückkehr nach Reload; manuell beschäftigt sowie manuell abwesend bleiben erhalten. Originaltimeout wiederhergestellt |
| CONFIG-01 | Zulässige Werte speichern | TEILWEISE / BETRIEB | CONFIG-03A–D, CONFIG-04A–C, COSMOS-03, VER-03A, RESET-06A | String/Boolean/Ganzzahl, Copyright-Fußtext und vier Tokendauern gespeichert/neu geladen; geänderter Name tatsächlich auf frischer Startseite, neue Dauern tatsächlich am Mailtoken geprüft; Neustartwirkung startgebundener Einstellungen bleibt offen |
| CONFIG-02 | Ungültige Konfiguration bleibt abgelehnt | TEILWEISE / TECHNISCH | CONFIG-03A–D, CONFIG-04A/C | Syntax, Null, negative/zu kurze Tokendauern, blanker Name, ungültige URL und nicht positive Ganzzahl abgelehnt; Originalwerte erhalten. Freie Boolean-Texte werden nicht angeboten; weitere SMTP-Verträge bleiben technische/ergänzende Prüfungen |
| MAIL-01 | Admin-Testmail und Unicode | UI-ABGEDECKT | MAIL-01, MailpitInbox | Empfänger-/Betreff-/Textfehler korrigiert; eigene echte SMTP-Mail mit UI-konfiguriertem Absender, Unicode-Betreff/Inhalt und genauem regulärem Zeilenabschluss geprüft |
| MAIL-02 | Erfolgreiche Queue-Verarbeitung | TEILWEISE / TECHNISCH | REG/VER/RESET lesen tatsächlich zugestellte eigene SMTP-Mails | Kein vollständiger Browsernachweis interner PENDING/IN_PROGRESS/SENT-Zustände oder späterer Scheduler-Doppelversandfreiheit |
| MAIL-03 | Nachgewiesener Verbindungsfehler und Wiederholung | OFFEN – BETRIEB / TECHNISCH | EmailConnectionFailureTest, EmailQueueServicePersistenceTest | Realer SMTP-Stopp/Wiederanlauf mit mindestens einminütigem Retry benötigt begrenzte Test-Orchestrierung; keine Queue-Statusoberfläche vorhanden |
| MAIL-04 | Dauerhafter Fehler und Adminbeobachtbarkeit | OFFEN – BETRIEB / TECHNISCH | Queue-/Auditregressionen | Dauerhafter realer SMTP-Ausfall, Retrylimit und Auditbeobachtung nicht im Browser eingerichtet |
| LANG-01 | Sichtbarer Wechsel Deutsch ↔ Englisch | TEILWEISE / PRODUKTGRENZE | LANG-01C, USER-06E: Adminsuche/Freigabefilter bleiben bei Wechsel in beide Richtungen erhalten | Offene Adminfehler/-dialoge noch nicht im Browser bestätigt; anonyme Form-/Tokenseiten haben keinen UI-Schalter |
| LANG-02 | Sprache nach Anmeldung und Mailvorlagen | TEILWEISE | LANG-01B, MailpitInbox und vollständige DE-/EN-Gesamtläufe | Gespeicherte Sprache nach Anmeldung und gewählte Mailbetreffe/volle kanonische Footer in beiden Sprachen geprüft. Sprachänderung eines bereits vorhandenen Kontos mit anschließender neuer Mail bleibt ergänzbar |
| UI-01 | Desktop, Handy und andere Fensterbreiten | TEILWEISE / MENSCHLICH | INFRA-01, COSMOS-01A–D/02, UI-03A–F; UI-01A/B im aktuellen DE-/EN-Lauf bestanden: direkte Datumeingabe, Kalenderauswahl/Wiederöffnen und Registrierung | Kontoansichten zusätzlich durch COSMOS-01A–D/COSMOS-02 bei vier Fenstergrößen samt gültigen Maillinks geprüft; Home/Admin, echte Geräte/Tastatur und menschliche Layoutqualität bleiben ergänzende Prüfungen |
| UI-02 | Tastatur und grundlegende Bedienbarkeit | TEILWEISE / MENSCHLICH | UI-02A | Login per Tab/Fokus/Enter belegt; gesamte Fokusreihenfolge, 200-%-Zoom und menschliche Lesbarkeit/Accessibility nicht vollständig geprüft |
| OPS-01 | Unklarer Transportausgang bleibt quarantänisiert | TECHNISCH / BETRIEBSNACHWEIS OFFEN | Queue-Service-/Persistenzregressionen | Fehler nach möglicher SMTP-Annahme und Neustart erfordern kontrollierte Transportfixture; fehlende Mail ist kein Nichtversandbeweis |
| OPS-02 | Aufbewahrung und Bereinigung | TECHNISCH / BETRIEBSNACHWEIS OFFEN | Queue-/Token-Cleanup mit Clock/H2 | Alte Zustände/Grenzfixture dürfen nicht über Browser-SQL erzeugt werden; tatsächlicher Sonntags-Schedulerlauf ist kein Kurztest |
| OPS-03 | Manuellen Recovery-Vertrag nachvollziehen | BETRIEB / MENSCHLICHE ENTSCHEIDUNG | Dokumentierter Recovery-Vertrag | Keine Recovery-Oberfläche; Versandbelege und sicher gestoppte Worker müssen geprüft werden. Kein blindes Wiederfreigeben |
| OPS-04 | Anonyme Resetantwort bei technischem Fehler | TECHNISCH / NETZWERK-NACHWEIS OFFEN | PasswordResetViewsTest und Servicefehlerregressionen | Kontrollierter technischer Fehler bei neutraler Antwort braucht geeignete Fehlerfixture; reguläre UI-Anfrage ersetzt ihn nicht |
| OPS-05 | Gebautes JAR und Docker, wenn vorbereitet | TEILWEISE / BETRIEB | Runner: verpacktes Docker-JAR und frische Daten | Separater Start desselben Stands außerhalb Docker/IntelliJ und vollständiger Vergleich nicht ausgeführt |

## Belege und Pflege

Hier nur den aktuellen Stand und verbleibende Voraussetzungen pflegen. Abgeschlossene
Versuchshistorien gehören in die Run-Archive. Die vorherigen Dokumentfassungen inklusive
Paketversuchen wurden vollständig und überprüft gesichert:

- [Dokumentationsstand vor der Zusammenfassung](runs/dokumentationsarchiv/2026-10-12_14-39-02_UTC__vor-zusammenfassung.zip)

Historische Run-Berichte, die manuelle Vorlage, die Weiterleitung unter
`docs/browser-tests/README.md` und der nützliche SQL-Skriptordner bleiben erhalten.
Es gibt weiterhin keine vollständige 100-%-Ablösung aller ursprünglichen Fälle.
