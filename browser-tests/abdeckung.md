# Browserabdeckung und offene Punkte

**Stand: 12.10.2026 – alle registrierten Fälle grün, ursprüngliche Gesamtabdeckung noch unvollständig.**
Dies ist die zentrale Übersicht zum Prüfstand. Bedienung und Aufbau stehen in der
[Browser-Testanleitung](README.md). Historische Run-Berichte werden nicht verändert.

## Letzter vollständiger Prüfstand

Arbeitsbranch: `feature/browser_manual_coverage`, Commit `664ad9cc`, mit lokalen Änderungen.
Beide Browserläufe verwendeten dasselbe frisch gebaute Anwendungsimage und jeweils
eigene frische Docker-/H2-/Mailpit-Daten. Keine Bestandsdaten oder echten Postfächer.

| Prüfung | Ergebnis | Nicht bestandene Fälle |
| --- | --- | --- |
| Anwendung und JaCoCo | 1034/1034, keine Fehler/Fehlschläge/Skips | keine |
| Helper/Reporter | 24/24, keine Fehler/Fehlschläge/Skips | keine |
| Browser Deutsch | 44/44, keine Fehler/Skips | keine |
| Browser Englisch | 44/44, keine Fehler/Skips | keine |

- [Vollständiger DE-Bericht](runs/2026-10-12T18-31-45_UTC__664ad9cc__cf19ae2f/report.md)
- [Vollständiger EN-Bericht](runs/2026-10-12T18-38-08_UTC__664ad9cc__8008e05d/report.md)

Aufbau, Abbau und HTML-Erzeugung waren in beiden Läufen erfolgreich.
HTML-Berichte über ihre `html/open-report.bat` öffnen; Screenshots/Traces bleiben im Archiv.
Die erfolgreichen 44 Browserfälle ersetzen nicht alle 44 ursprünglichen manuellen
Fälle: Die beiden Kataloge haben unterschiedliche IDs und Teilabläufe. Fehlende
Journeys, Betriebsprüfungen und menschliche Einschätzungen bleiben unten ausgewiesen.

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

Aktuell sind keine fehlgeschlagenen Fälle unter den 44 registrierten Browserfällen
offen. Die folgenden noch nicht aufgenommenen Abläufe sind weiterhin keine
vollständigen Browsernachweise.

## Weitere fehlende Abläufe und sinnvolle Reihenfolge

1. **Benutzerverwaltung ergänzen:** Sperren/Freigeben, gezieltes Aktivieren
   unverifizierter Konten und zwei tatsächlich offene veraltete Adminformulare.
2. **Darauf aufbauend Sitzungen und alte Links:** Rollenentzug, Kontosperre/Löschung
   bei bestehenden Verbindungen sowie Verifikations-/Resetlinks nach Adminänderungen.
3. **Status/Live-Aktualisierung:** aktuelle Value-Anzeige von gleichlautenden Optionen
   trennen; anschließend manuellen Status und automatische Abwesenheit/Rückkehr prüfen.
4. **Remember-me:** Popover nach Login zuverlässig bedienen, tatsächliche Checkbox,
   Cookieauslieferung, echten Browserneustart und Logout prüfen. Frühere Cookiebeobachtung
   ist ein Hinweis, kein bewiesener Authentifizierungsfehler; keine aktive Suite aufgenommen.
5. **Übrige Konfiguration und Admin-Testmail:** richtige Konfigurationsgruppen,
   Boolean-/Zahlenwerte und Neustartwirkung prüfen. Unicode-Mail wurde früher empfangen;
   Vergleich am regulären SMTP-Zeilenabschluss gescheitert, kein bestätigter Versandfehler.
6. **Token-/Mailabläufe ergänzen:** Ablauf mit echten Mindestdauern, erneuter Versand,
   ungültige Eingaben im Verifikations-Anforderungsformular und vollständige Mail-Footer.
   Die vier Token-Konfigurationsfälle CONFIG-03A–D bestehen DE/EN; Mindestgrenzen
   sind eine Minute Gültigkeit und zwei Minuten Aufbewahrung nach Ablauf.

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
| START-03 | Erststart und Neustart verwenden die richtige Mailkonfiguration | TEILWEISE | REG/VER/RESET und SMTP-Mailpit | Erststart-Zustellung belegt; Vergleich Erststart/Neustart und Admin-Testmail noch offen |
| REG-01 | Gültige Registrierung | TEILWEISE | REG-01A, VER-01A, SEC-01A | Registrierung/Mail/gesperrter Vorlogin belegt; explizite Admininspektion und genaue Kontozahl fehlen |
| REG-02 | Pflichtfelder und ungültige Eingaben | TEILWEISE / TECHNISCH | REG-02A/B/C/D | Geforderte UI-Eingabevarianten und Korrekturen geprüft; Nichtanlage/Nichtversand jeder einzelnen Variante bleiben gesonderte technische Aussagen |
| REG-03 | Doppelte E-Mail und Kanonisierung | TEILWEISE / TECHNISCH | REG-03A | Exakte/kanonisierte Dublette abgewiesen und Passwort unverändert; genaue Anzahl gespeicherter Identitäten technisch prüfen |
| VER-01 | Gültiger Link und erneute Verwendung | TEILWEISE | VER-01A/B, MailpitInbox | Aktivierung/erneute Verwendung und Mail-Schlüsselangaben geprüft; Footer nicht ausdrücklich mit Getter verglichen |
| VER-02 | Ungültiger oder fehlender Token | TEILWEISE | VER-02A | Unbekannter/fehlender Token geprüft; ungültige Eingabe im tatsächlich angebotenen Anforderungsformular fehlt |
| VER-03 | Ablauf und erneuter Versand | OFFEN – ABHÄNGIG | Technische Clock-/Queue-Persistenztests vorhanden | UI-Speicherung von einer Minute ist jetzt durch CONFIG-03A belegt; Ablauf mit echter Wartezeit wurde noch nicht ergänzt. Erneuten Versand/Deduplizierung nicht als Browsererfolg zählen |
| VER-04 | Adminsperre bleibt trotz alter Links bestehen | OFFEN – ABHÄNGIG | Technische Sperrregressionen vorhanden | Adminbearbeitung jetzt durch USER-06B belegt; spezifische Sperre unverifizierter/verifizierter Konten und Prüfung alter Links noch nicht ergänzt |
| AUTH-01 | Erfolgs- und Fehlerfälle | UI-ABGEDECKT | AUTH-01A–D, REG-01A, SEC-01A, VER-01A | Erfolg/falsches Passwort/unbekannt/unverifiziert/anonym geprüft; zusätzliche hypothetische Sperrregeln nicht behauptet |
| AUTH-02 | Abmeldung, Zurück und bestehende Tabs | TEILWEISE | AUTH-02A | Admin-Tabs, Logout, Zurück und neuer Zugriff belegt; alter geschützter Mutationscallback nicht vollständig geprüft |
| AUTH-03 | Angemeldet bleiben | OFFEN | Zwei archivierte Remember-me-Versuche | Ohne Option zweimal echter Neustart bestanden; gesamte Variante mit Option und Logout nicht zuverlässig abgeschlossen, keine aktive Suite aufgenommen |
| RESET-01 | Gültiger Reset | TEILWEISE | RESET-01A | Echte Mail/neues und altes Passwort belegt; vollständiger Mail-Footer-Vertrag nicht ausdrücklich verglichen |
| RESET-02 | Anonyme Antwort verrät keine Konten | TEILWEISE | RESET-03A/03B, RESET-02A | Aktiv/unbekannt/unverifiziert und Syntax belegt; administrativ gesperrte Adresse bleibt abhängig von Adminbearbeitung offen |
| RESET-03 | Passwortregeln, ungültige und verbrauchte Links | UI-ABGEDECKT | RESET-02B/04A/04B | Fehler/Korrektur, unbekannter und verbrauchter Link ohne weiteres Passwortformular; technische Regeln bleiben JUnit |
| RESET-04 | Ablauf auch bei bereits geöffnetem Formular | OFFEN – ABHÄNGIG | Technische Clock-/Ablaufregressionen vorhanden | UI-Speicherung von einer Minute ist durch CONFIG-03C belegt; benötigt noch den Ablauf mit echter Wartezeit; keine Uhrmanipulation/Testzustandsinjektion als Ersatz |
| RESET-05 | Zwei geöffnete Formulare für denselben Token | UI-ABGEDECKT | RESET-05A | Zwei getrennte anonyme Kontexte: erstes Speichern gewinnt, alter Submit abgewiesen und richtige Loginidentität geprüft; echte Parallelität separat |
| RESET-06 | Sperre oder Löschung nach Linkerstellung | OFFEN – ABHÄNGIG | Technische Sperr-/Löschregressionen vorhanden | Adminbearbeitung/Löschung durch USER-06B/C belegt; gezielte Sperre und tatsächliche alte Resetlinks nach Änderungen noch nicht ergänzt |
| USER-01 | Anlegen, bearbeiten und erneut öffnen | TEILWEISE | USER-05A/06A: Anlage, genaue Suche/Pagination, zentrale Validierung und neue Anmeldung; technische Filter-/Zeitregressionen | Datum/Kalender und optionale Zeit durch USER-05B/C sowie Bearbeitung/erneutes Öffnen durch USER-06B belegt; ausdrückliche Admininspektion aller Stammdaten bleibt ergänzbar |
| USER-02 | Sperren und gezieltes Freigeben | OFFEN | Keine vollständige Browser-Sperre/Freigabe | Auch ausdrückliche Freigabe unverifizierter Konten fehlt |
| USER-03 | Löschen mit zugehörigen Tokens und Queueeinträgen | TEILWEISE / TECHNISCH | Löschkaskaden in Persistenztests; USER-06C: Abbruch, Bestätigung und dauerhafte Abwesenheit des ausgewählten Kontos | UI-Löschung belegt; alte Links fehlen noch, interne Queue-/Tokenkaskaden bleiben technische Aussagen |
| USER-04 | Veraltetes Bearbeitungsformular | OFFEN | Versionskonflikte in Persistenz-/UI-Komponententests | Zwei tatsächliche Adminformulare noch nicht bestätigt |
| SEC-01 | Direkte URL und normale Benutzerrolle | TEILWEISE | AUTH-01D und SEC-01A | Anonym/normal ausgeschlossen und Bootstrapadmin funktioniert; nachträgliche Adminrollenvergabe fehlt |
| SEC-02 | Rollenentzug bei offenem Adminformular / Push | OFFEN – ABHÄNGIG | PushRpcSecurityPersistenceTest, SessionAuthorityPersistenceTest | Adminbearbeitung jetzt belegt; Rollenentzug bei tatsächlich offener geschützter Browserverbindung fehlt noch |
| SEC-03 | Sperre und Löschung bei laufender Sitzung | OFFEN – ABHÄNGIG | Sitzungs-/Sperrregressionen technisch | Reale Aktion nach Sperre/Löschung inklusive Remember-me noch nicht bestätigt |
| SEC-04 | Letzten aktiven Admin schützen | TEILWEISE / TECHNISCH | AdminSecurityPersistenceTest | USER-06D belegt Selbstlöschschutz; Letzter-Admin-Schutz bei Rollen-/Freigabeänderungen fehlt noch als Browserablauf |
| STATUS-01 | Manueller Status und Aktualisierung | OFFEN | Zwei archivierte Statusversuche | Vaadin-Slot- und Optionen-/Value-Locators zweimal gescheitert; kein vollständiger manueller Status-/Live-Nachweis übernommen |
| STATUS-02 | Automatische Abwesenheit und Rückkehr | OFFEN – ABHÄNGIG / TECHNISCH | UserStatusSchedulerTest, UserStatusPersistenceTest | Kurzer Timeout plus echte sichtbare Status-/Rückkehrprüfung benötigen bestätigte Config- und Statushelfer |
| CONFIG-01 | Zulässige Werte speichern | TEILWEISE | CONFIG-03A–D: vier Tokendauern an der Mindestgrenze gespeichert und neu geladen, DE/EN | Weitere Konfigurationsfelder (u. a. Boolean/Zahlen) und Neustartwirkung bleiben offen; früheres Gesamtpaket nicht nachträglich als erfolgreich werten |
| CONFIG-02 | Ungültige Konfiguration bleibt abgelehnt | TEILWEISE – TOKENFEHLER BEHOBEN | CONFIG-03A–D DE/EN: Syntax, Null, negative und zu kurze Dauern abgelehnt; gespeicherter Wert bleibt unverändert | Mindestgrenzen 1 Minute Gültigkeit / 2 Minuten Aufbewahrung nach Ablauf. Weitere Konfigurationsfelder offen; behobene Null-Dauer-Lücke im Archiv belegt |
| MAIL-01 | Admin-Testmail und Unicode | OFFEN | Echte Unicode-Mail im ersten Versuch empfangen | Vergleich scheiterte am regulären abschließenden SMTP-Zeilenumbruch; gesamtes Paket nicht abschließend DE/EN bestätigt |
| MAIL-02 | Erfolgreiche Queue-Verarbeitung | TEILWEISE / TECHNISCH | REG/VER/RESET lesen tatsächlich zugestellte eigene SMTP-Mails | Kein vollständiger Browsernachweis interner PENDING/IN_PROGRESS/SENT-Zustände oder späterer Scheduler-Doppelversandfreiheit |
| MAIL-03 | Nachgewiesener Verbindungsfehler und Wiederholung | OFFEN – BETRIEB / TECHNISCH | EmailConnectionFailureTest, EmailQueueServicePersistenceTest | Realer SMTP-Stopp/Wiederanlauf mit mindestens einminütigem Retry benötigt begrenzte Test-Orchestrierung; keine Queue-Statusoberfläche vorhanden |
| MAIL-04 | Dauerhafter Fehler und Adminbeobachtbarkeit | OFFEN – BETRIEB / TECHNISCH | Queue-/Auditregressionen | Dauerhafter realer SMTP-Ausfall, Retrylimit und Auditbeobachtung nicht im Browser eingerichtet |
| LANG-01 | Sichtbarer Wechsel Deutsch ↔ Englisch | TEILWEISE / PRODUKTGRENZE | LANG-01C, USER-06E: Adminsuche/Freigabefilter bleiben bei Wechsel in beide Richtungen erhalten | Offene Adminfehler/-dialoge noch nicht im Browser bestätigt; anonyme Form-/Tokenseiten haben keinen UI-Schalter |
| LANG-02 | Sprache nach Anmeldung und Mailvorlagen | TEILWEISE | LANG-02A im aktuellen DE/EN-Gesamtlauf bestanden; VER/RESET-Mails pro Sprache | Vorbereitung über exakte Suche jetzt belegt; vollständiger Footer-Vergleich bleibt offen |
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
