# Abgleich der ursprünglichen manuellen Testfälle

**Abschluss: UNVOLLSTÄNDIG.** 30 aktive Browserfälle (vorher 21); final DE 28/30, EN 29/30.
Einzelsuites grün, Gesamtläufe mit zwei nachstehend dokumentierten Grenzen.

Stand: 12.10.2026. Ausgangscommit `5310516d2ccbade7169aa19023ec21a24a849c22`.
Arbeitsbranch: `feature/browser_manual_coverage`. Die 44 ursprünglichen Fall-IDs
werden hier als Vergleichsgrundlage geführt. Ein Browserfall kann mehrere Schritte
eines manuellen Falls prüfen; seine Existenz beweist nicht automatisch dessen vollständige Abdeckung.

## Arbeitspakete und Prüfungen

1. Bestandsaufnahme und Zuordnung (Dokumentationsprüfung aller 44 IDs).
2. Benutzerverwaltung: Anlage/Änderung, Sperre/Freigabe, Löschung, Validierung und altes Formular; neue Suite `users`, DE/EN.
3. Laufende Sitzungen und Rollen: Vergabe/Entzug, Sperre/Löschung und Selbstschutz; eigene Suite `session-security`, DE/EN.
4. Konfiguration und direkter Mailversand: UI-Speicherung/Validierung und echtes Unicode-SMTP; eigene Suite `admin-settings`, DE/EN.
5. Weitere Kontoabläufe: Dubletten, alte Resetformulare, Sperre/Löschung und neutrale Antworten; eigene Suite `account-lifecycle`, DE/EN.
6. Sprache/Bedienung: Präferenz, dynamischer Sprachwechsel, schmale Formulare; eigene Suite `presentation`, DE/EN.
7. Zusätzliche unabhängige Pakete: Remember-me und manueller Status/Live-Aktualisierung, jeweils DE/EN bei Erfolg.
8. Abschluss: vollständiger DE/EN-Lauf, Anwendungstests und Helper-/Reportertests, Scope-/Diffprüfung.

Für jedes Umsetzungspaket gelten maximal zwei Versuche insgesamt. Unerwartete
Fehler beenden den Paketversuch; der zweite beginnt mit erneuter Bestandsaufnahme.
Erfolgreiche Pakete werden einzeln committed. Gescheiterte Pakete und ungeeignete
Browserprüfungen stehen in `automatisierung-offen.md`. Keine Änderung der alten Vorlage.

## Zuordnung nach den Paketprüfungen



UI-ABGEDECKT bezeichnet den konkret beschriebenen Oberflächenablauf, keine
vollständige technische Prüfung. TEILWEISE bedeutet, dass mindestens ein
ursprünglich erwarteter Schritt oder Nachweis noch fehlt. TECHNISCH nennt
vorhandene JUnit-/Persistenzprüfungen, ohne einen ausgeführten Betriebs-/Browser-
nachweis zu behaupten. Es wird kein künstlicher Gesamt-Prozentwert berechnet.

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
| VER-03 | Ablauf und erneuter Versand | OFFEN – ABHÄNGIG | Technische Clock-/Queue-Persistenztests vorhanden | Browserablauf benötigt verlässliche UI-Änderung der kurzen Tokenlaufzeit; Konfigurationspaket gescheitert. Erneuten Versand/Deduplizierung nicht als Browsererfolg zählen |
| VER-04 | Adminsperre bleibt trotz alter Links bestehen | OFFEN – ABHÄNGIG | Technische Sperrregressionen vorhanden | Benötigt bestätigte Adminbearbeitung für unverifiziertes und verifiziertes Konto; Benutzerverwaltungspaket gescheitert |
| AUTH-01 | Erfolgs- und Fehlerfälle | UI-ABGEDECKT | AUTH-01A–D, REG-01A, SEC-01A, VER-01A | Erfolg/falsches Passwort/unbekannt/unverifiziert/anonym geprüft; zusätzliche hypothetische Sperrregeln nicht behauptet |
| AUTH-02 | Abmeldung, Zurück und bestehende Tabs | TEILWEISE | AUTH-02A | Admin-Tabs, Logout, Zurück und neuer Zugriff belegt; alter geschützter Mutationscallback nicht vollständig geprüft |
| AUTH-03 | Angemeldet bleiben | OFFEN – PAKET GESCHEITERT | Zwei archivierte Remember-me-Versuche | Ohne Option zweimal echter Neustart bestanden; gesamte Variante mit Option und Logout nicht zuverlässig abgeschlossen, keine aktive Suite aufgenommen |
| RESET-01 | Gültiger Reset | TEILWEISE | RESET-01A | Echte Mail/neues und altes Passwort belegt; vollständiger Mail-Footer-Vertrag nicht ausdrücklich verglichen |
| RESET-02 | Anonyme Antwort verrät keine Konten | TEILWEISE | RESET-03A/03B, RESET-02A | Aktiv/unbekannt/unverifiziert und Syntax belegt; administrativ gesperrte Adresse bleibt abhängig von Adminbearbeitung offen |
| RESET-03 | Passwortregeln, ungültige und verbrauchte Links | UI-ABGEDECKT | RESET-02B/04A/04B | Fehler/Korrektur, unbekannter und verbrauchter Link ohne weiteres Passwortformular; technische Regeln bleiben JUnit |
| RESET-04 | Ablauf auch bei bereits geöffnetem Formular | OFFEN – ABHÄNGIG | Technische Clock-/Ablaufregressionen vorhanden | Benötigt kontrolliert kurze UI-Konfiguration und echte Wartezeit; keine Uhrmanipulation/Testzustandsinjektion als Ersatz |
| RESET-05 | Zwei geöffnete Formulare für denselben Token | UI-ABGEDECKT | RESET-05A | Zwei getrennte anonyme Kontexte: erstes Speichern gewinnt, alter Submit abgewiesen und richtige Loginidentität geprüft; echte Parallelität separat |
| RESET-06 | Sperre oder Löschung nach Linkerstellung | OFFEN – ABHÄNGIG | Technische Sperr-/Löschregressionen vorhanden | Verlässliche Adminsperre/Löschung für tatsächliche alte Links benötigt; gescheitertes User-Paket |
| USER-01 | Anlegen, bearbeiten und erneut öffnen | OFFEN – PAKET GESCHEITERT | Bisherige Anlage dient SEC-01A/Sprachpräferenz als Vorbereitung | Bearbeitung/Validation/erneutes Öffnen des Gesamtfalls nach zwei User-Versuchen nicht bestätigt |
| USER-02 | Sperren und gezieltes Freigeben | OFFEN – PAKET GESCHEITERT | Keine vollständige Browser-Sperre/Freigabe | Auch ausdrückliche Freigabe unverifizierter Konten fehlt |
| USER-03 | Löschen mit zugehörigen Tokens und Queueeinträgen | OFFEN – PAKET GESCHEITERT / TECHNISCH | Löschkaskaden in Persistenztests | Bestätigte UI-Löschung/alte Links fehlen; interne Queue-/Tokenkaskaden sind technische Aussagen |
| USER-04 | Veraltetes Bearbeitungsformular | OFFEN – PAKET GESCHEITERT | Versionskonflikte in Persistenz-/UI-Komponententests | Zwei tatsächliche Adminformulare noch nicht bestätigt |
| SEC-01 | Direkte URL und normale Benutzerrolle | TEILWEISE | AUTH-01D und SEC-01A | Anonym/normal ausgeschlossen und Bootstrapadmin funktioniert; nachträgliche Adminrollenvergabe fehlt |
| SEC-02 | Rollenentzug bei offenem Adminformular / Push | OFFEN – ABHÄNGIG | PushRpcSecurityPersistenceTest, SessionAuthorityPersistenceTest | Reale offene Browserverbindung plus Rollenentzug benötigt bestätigte Adminbearbeitung |
| SEC-03 | Sperre und Löschung bei laufender Sitzung | OFFEN – ABHÄNGIG | Sitzungs-/Sperrregressionen technisch | Reale Aktion nach Sperre/Löschung inklusive Remember-me noch nicht bestätigt |
| SEC-04 | Letzten aktiven Admin schützen | OFFEN – ABHÄNGIG | AdminSecurityPersistenceTest | Letzter-Admin-/Selbstschutz muss über erreichbare Formular-/Löschaktionen geprüft werden |
| STATUS-01 | Manueller Status und Aktualisierung | OFFEN – PAKET GESCHEITERT | Zwei archivierte Statusversuche | Vaadin-Slot- und Optionen-/Value-Locators zweimal gescheitert; kein vollständiger manueller Status-/Live-Nachweis übernommen |
| STATUS-02 | Automatische Abwesenheit und Rückkehr | OFFEN – ABHÄNGIG / TECHNISCH | UserStatusSchedulerTest, UserStatusPersistenceTest | Kurzer Timeout plus echte sichtbare Status-/Rückkehrprüfung benötigen bestätigte Config- und Statushelfer |
| CONFIG-01 | Zulässige Werte speichern | OFFEN – PAKET GESCHEITERT | Konfigurationsdienste/Komponenten technisch geprüft | UI-Test suchte Boolean in falscher Gruppe; zweiter Versuch beim Dateieinlesen gescheitert; Wiederladen/Neustartwirkung nicht vollständig bestätigt |
| CONFIG-02 | Ungültige Konfiguration bleibt abgelehnt | OFFEN – BESTÄTIGTER FEHLER | Archivierter CONFIG-02A | PT0S wird derzeit gespeichert, obwohl Tokendienste positive Dauern verlangen; nicht behoben. Weitere UI-Varianten offen |
| MAIL-01 | Admin-Testmail und Unicode | OFFEN – PAKET GESCHEITERT | Echte Unicode-Mail im ersten Versuch empfangen | Vergleich scheiterte am regulären abschließenden SMTP-Zeilenumbruch; gesamtes Paket nicht abschließend DE/EN bestätigt |
| MAIL-02 | Erfolgreiche Queue-Verarbeitung | TEILWEISE / TECHNISCH | REG/VER/RESET lesen tatsächlich zugestellte eigene SMTP-Mails | Kein vollständiger Browsernachweis interner PENDING/IN_PROGRESS/SENT-Zustände oder späterer Scheduler-Doppelversandfreiheit |
| MAIL-03 | Nachgewiesener Verbindungsfehler und Wiederholung | OFFEN – BETRIEB / TECHNISCH | EmailConnectionFailureTest, EmailQueueServicePersistenceTest | Realer SMTP-Stopp/Wiederanlauf mit mindestens einminütigem Retry benötigt begrenzte Test-Orchestrierung; keine Queue-Statusoberfläche vorhanden |
| MAIL-04 | Dauerhafter Fehler und Adminbeobachtbarkeit | OFFEN – BETRIEB / TECHNISCH | Queue-/Auditregressionen | Dauerhafter realer SMTP-Ausfall, Retrylimit und Auditbeobachtung nicht im Browser eingerichtet |
| LANG-01 | Sichtbarer Wechsel Deutsch ↔ Englisch | TEILWEISE / PRODUKTGRENZE | LANG-01C und feste DE/EN-Suites | Home-Sprachschalter belegt; anonyme Form-/Tokenseiten haben keinen UI-Schalter. Offene Adminfehler/-dialoge noch nicht bestätigt |
| LANG-02 | Sprache nach Anmeldung und Mailvorlagen | TEILWEISE | LANG-02A einzeln DE/EN bestanden; im Gesamtlauf Vorbereitung fehlgeschlagen, VER/RESET-Mails pro Sprache | Kontopräferenz noch nicht zuverlässig im Gesamtstand belegt; vollständiger Footer-Vergleich bleibt offen |
| UI-01 | Desktop, Handy und andere Fensterbreiten | TEILWEISE / MENSCHLICH | INFRA-01 und UI-01A einzeln bestanden; mobiler Datumsschritt im DE-Gesamtlauf fehlgeschlagen | Schmale Registrierung noch nicht zuverlässig im Gesamtstand; echte Geräte/Tastatur, übrige Formulare/Dialogsituationen und Layoutqualität fehlen |
| UI-02 | Tastatur und grundlegende Bedienbarkeit | TEILWEISE / MENSCHLICH | UI-02A | Login per Tab/Fokus/Enter belegt; gesamte Fokusreihenfolge, 200-%-Zoom und menschliche Lesbarkeit/Accessibility nicht vollständig geprüft |
| OPS-01 | Unklarer Transportausgang bleibt quarantänisiert | TECHNISCH / BETRIEBSNACHWEIS OFFEN | Queue-Service-/Persistenzregressionen | Fehler nach möglicher SMTP-Annahme und Neustart erfordern kontrollierte Transportfixture; fehlende Mail ist kein Nichtversandbeweis |
| OPS-02 | Aufbewahrung und Bereinigung | TECHNISCH / BETRIEBSNACHWEIS OFFEN | Queue-/Token-Cleanup mit Clock/H2 | Alte Zustände/Grenzfixture dürfen nicht über Browser-SQL erzeugt werden; tatsächlicher Sonntags-Schedulerlauf ist kein Kurztest |
| OPS-03 | Manuellen Recovery-Vertrag nachvollziehen | BETRIEB / MENSCHLICHE ENTSCHEIDUNG | Dokumentierter Recovery-Vertrag | Keine Recovery-Oberfläche; Versandbelege und sicher gestoppte Worker müssen geprüft werden. Kein blindes Wiederfreigeben |
| OPS-04 | Anonyme Resetantwort bei technischem Fehler | TECHNISCH / NETZWERK-NACHWEIS OFFEN | PasswordResetViewsTest und Servicefehlerregressionen | Kontrollierter technischer Fehler bei neutraler Antwort braucht geeignete Fehlerfixture; reguläre UI-Anfrage ersetzt ihn nicht |
| OPS-05 | Gebautes JAR und Docker, wenn vorbereitet | TEILWEISE / BETRIEB | Runner: verpacktes Docker-JAR und frische Daten | Separater Start desselben Stands außerhalb Docker/IntelliJ und vollständiger Vergleich nicht ausgeführt |
