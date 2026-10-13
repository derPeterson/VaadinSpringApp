# Browserabdeckung und offene Punkte

**Stand: 13.10.2026 – Loginänderung gezielt geprüft; ursprüngliche Gesamtabdeckung noch unvollständig.**
Dies ist die zentrale Übersicht zum Prüfstand. Bedienung und Aufbau stehen in der
[Browser-Testanleitung](README.md). Historische Run-Berichte werden nicht verändert.

## Aktuelle Cardgestaltung

Die abschließende Anpassung verwendet eine 430 px breite dunkle Glas-Card mit
kompakteren Abständen/Überschrift, konturierten echten Vaadin-Feldern und dezentem
Buttonverlauf. Der Card-Hintergrund wurde anschließend ausdrücklich auf den RGBA-Verlauf der
Vorlage festgelegt; die übrigen Elementfarben kommen aus Lumo. Inter wird lokal
für den Login eingebunden. Seit diesen optischen Feinänderungen wurden keine
JUnit-/Docker-Browserläufe erneut ausgeführt.
Der bestehende Authentifizierungs-Handler wurde gegen den Ausgangscommit verglichen
und ist unverändert; Security und Registrierung wurden nicht angepasst.

Frischer Anwendungs-/Frontend-Build erfolgreich. Die sechs Login-Layoutfälle
UI-03A–F bestehen jeweils auf Deutsch und Englisch auf demselben neuen Image;
Aufbau, Abbau und HTML-Erzeugung erfolgreich. Die übrigen Tests wurden für diese
rein optische Nachbesserung nicht erneut ausgeführt; ältere Nachweise stehen unten.

- [Aktueller DE-Cardbericht](runs/2026-10-12T21-57-49_UTC__2a1dfb7a__1ac35ef9/report.md)
- [Aktueller EN-Cardbericht](runs/2026-10-12T21-59-37_UTC__2a1dfb7a__35065e21/report.md)

## Anschließende Login-Hintergrund-/Layoutänderung

Nach dem unten dokumentierten Gesamtlauf wurde ausschließlich der Login-Hintergrund
auf CSS-Sterne, Nebel und Planeten umgestellt, das Welcome-Bild entfernt und die
feste Karten-Mindestbreite aufgehoben. Bestehende Felder, Buttons, Texte,
Authentifizierungsablauf und Security-Konfiguration bleiben erhalten.

| Prüfung des neuen Stands | Ergebnis |
| --- | --- |
| Anwendung und JaCoCo | 1036/1036; keine Fehler/Fehlschläge/Skips |
| Helper/Reporter | 24/24; keine Fehler/Fehlschläge/Skips |
| `login-checks` Deutsch | 19/19; Aufbau, Abbau und HTML erfolgreich |
| `login-checks` Englisch | 19/19; Aufbau, Abbau und HTML erfolgreich |

- [DE-Loginbericht](runs/2026-10-12T21-36-30_UTC__2a1dfb7a__abaac605/report.md)
- [EN-Loginbericht](runs/2026-10-12T21-38-14_UTC__2a1dfb7a__27317c32/report.md)

Die sechs zusätzlichen Fälle UI-03A–F prüfen fünf Fenstergrößen (320×568,
390×844, 844×390, 360×240, 1280×900), Fehler/Korrektur und reale Anmeldung
sowie reduzierte Bewegung und Begrenzung der CSS-Dekoration auf Login.
Das Dashboard wird nach der tatsächlichen Anmeldung wieder bei Desktopbreite
kontrolliert; seine responsive Gestaltung ist kein Teil dieser Änderung.
Die kombinierten 19 Fälle enthalten außerdem bestehende Anmeldung/Zugriff,
Tastatur-/Präsentations- und Remember-me-Prüfungen.

Beide Läufe nutzten dasselbe frisch gebaute Image mit lokalen Änderungen auf
`main` (Ausgangscommit `2a1dfb7a`). Jetzt sind 76 Fälle
registriert. Für diesen Stand wurde gezielt `login-checks` ausgeführt, kein neuer
76-Fälle-Gesamtlauf. Echte Geräte/Leistungsbenchmarks und ein vollständiges Zoomaudit
werden durch die emulierten Platzverhältnisse nicht ersetzt.

Die anschließend festgelegten Animationszeiten bleiben 30/45 Sekunden für Sterne
und 18–24 Sekunden für Nebel. Der neuere Card-Prüfstand steht oben.

## Letzter vollständiger Prüfstand

Historischer Gesamtlauf vor der anschließenden Loginänderung: Arbeitsbranch `main`,
Ausgangscommit `d0f33d16`, mit den damals dokumentierten lokalen Änderungen.
Die beiden abschließenden Gesamtläufe verwendeten dasselbe frisch gebaute Anwendungsimage
und jeweils eigene frische Docker-/H2-/Mailpit-Daten. Keine Bestandsdaten oder echten Postfächer.

| Prüfung | Ergebnis | Nicht bestandene Fälle |
| --- | --- | --- |
| Anwendung und JaCoCo | 1036/1036, keine Fehler/Fehlschläge/Skips | keine |
| Helper/Reporter | 24/24, keine Fehler/Fehlschläge/Skips | keine |
| Browser Deutsch | 70/70, keine Fehler/Skips | keine |
| Browser Englisch | 70/70, keine Fehler/Skips | keine |

- [Vollständiger DE-Bericht](runs/2026-10-12T20-35-51_UTC__d0f33d16__a6bcc4c8/report.md)
- [Vollständiger EN-Bericht](runs/2026-10-12T20-48-59_UTC__d0f33d16__d7f70c45/report.md)

Aufbau, Abbau und HTML-Erzeugung waren erfolgreich. HTML-Berichte über ihre
`html/open-report.bat` öffnen; Screenshots/Traces bleiben im jeweiligen Archiv.
Die 70 Browserfälle und die 44 ursprünglichen manuellen Fälle haben unterschiedliche
Teilabläufe und IDs. Ihr Vergleich steht unten; erfolgreiche Browserläufe bedeuten
keine vollständige 100-%-Ablösung aller Betriebs- und menschlichen Prüfungen.

Der neue Remember-me-Ablauf hat eine fehlende Spring-Kontextspeicherung nach dem
eigenen Vaadin-Login aufgedeckt: das zunächst gesetzte Cookie wurde durch erneute
Remember-me-Anmeldungen paralleler Folgeanfragen wieder gelöscht. Der Login speichert
den Kontext jetzt ausdrücklich über das vorhandene `SecurityContextRepository`,
bevor das Cookie ausgegeben wird. Zwei technische Varianten lesen den Kontext aus
einer unabhängigen Folgeanfrage; der Browser bestätigt Cookie, echten Prozessneustart
und Logout mit erneutem Neustart. Die bestehende Sperr-/Rollenprüfung bleibt erhalten.
Die fehlgeschlagenen Zwischenläufe sind historische Belege, keine offenen Fehler
dieses abschließenden Prüfstands.

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

Im bisherigen vollständigen 70-Fälle-Lauf waren keine Fälle fehlgeschlagen.
Der gezielte Prüfstand der anschließenden Loginänderung steht oben. Die ergänzten Pakete und die weiterhin nur teilweise abgedeckten ursprünglichen
Fälle sind nachfolgend getrennt aufgeführt.

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
Fehlerdialoge beim Sprachwechsel und schmale Admin-/Resetansichten.
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
| CONFIG-01 | Zulässige Werte speichern | TEILWEISE / BETRIEB | CONFIG-03A–D, CONFIG-04A–C, VER-03A, RESET-06A | String/Boolean/Ganzzahl und vier Tokendauern gespeichert/neu geladen; geänderter Name tatsächlich auf frischer Startseite, neue Dauern tatsächlich am Mailtoken geprüft; Neustartwirkung startgebundener Einstellungen bleibt offen |
| CONFIG-02 | Ungültige Konfiguration bleibt abgelehnt | TEILWEISE / TECHNISCH | CONFIG-03A–D, CONFIG-04A/C | Syntax, Null, negative/zu kurze Tokendauern, blanker Name, ungültige URL und nicht positive Ganzzahl abgelehnt; Originalwerte erhalten. Freie Boolean-Texte werden nicht angeboten; weitere SMTP-Verträge bleiben technische/ergänzende Prüfungen |
| MAIL-01 | Admin-Testmail und Unicode | UI-ABGEDECKT | MAIL-01, MailpitInbox | Empfänger-/Betreff-/Textfehler korrigiert; eigene echte SMTP-Mail mit UI-konfiguriertem Absender, Unicode-Betreff/Inhalt und genauem regulärem Zeilenabschluss geprüft |
| MAIL-02 | Erfolgreiche Queue-Verarbeitung | TEILWEISE / TECHNISCH | REG/VER/RESET lesen tatsächlich zugestellte eigene SMTP-Mails | Kein vollständiger Browsernachweis interner PENDING/IN_PROGRESS/SENT-Zustände oder späterer Scheduler-Doppelversandfreiheit |
| MAIL-03 | Nachgewiesener Verbindungsfehler und Wiederholung | OFFEN – BETRIEB / TECHNISCH | EmailConnectionFailureTest, EmailQueueServicePersistenceTest | Realer SMTP-Stopp/Wiederanlauf mit mindestens einminütigem Retry benötigt begrenzte Test-Orchestrierung; keine Queue-Statusoberfläche vorhanden |
| MAIL-04 | Dauerhafter Fehler und Adminbeobachtbarkeit | OFFEN – BETRIEB / TECHNISCH | Queue-/Auditregressionen | Dauerhafter realer SMTP-Ausfall, Retrylimit und Auditbeobachtung nicht im Browser eingerichtet |
| LANG-01 | Sichtbarer Wechsel Deutsch ↔ Englisch | TEILWEISE / PRODUKTGRENZE | LANG-01C, USER-06E: Adminsuche/Freigabefilter bleiben bei Wechsel in beide Richtungen erhalten | Offene Adminfehler/-dialoge noch nicht im Browser bestätigt; anonyme Form-/Tokenseiten haben keinen UI-Schalter |
| LANG-02 | Sprache nach Anmeldung und Mailvorlagen | TEILWEISE | LANG-01B, MailpitInbox und vollständige DE-/EN-Gesamtläufe | Gespeicherte Sprache nach Anmeldung und gewählte Mailbetreffe/volle kanonische Footer in beiden Sprachen geprüft. Sprachänderung eines bereits vorhandenen Kontos mit anschließender neuer Mail bleibt ergänzbar |
| UI-01 | Desktop, Handy und andere Fensterbreiten | TEILWEISE / MENSCHLICH | INFRA-01; UI-01A/B im vollständigen aktuellen DE-/EN-Lauf bestanden: direkte Datumeingabe, Kalenderauswahl/Wiederöffnen und Registrierung | Echte Geräte/Tastatur, übrige Formulare und menschliche Layoutqualität fehlen |
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
