# Abgleich der ursprünglichen manuellen Testfälle

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
7. Abschluss: vollständiger DE/EN-Lauf, Anwendungstests und Helper-/Reportertests, Scope-/Diffprüfung.

Für jedes Umsetzungspaket gelten maximal zwei Versuche insgesamt. Unerwartete
Fehler beenden den Paketversuch; der zweite beginnt mit erneuter Bestandsaufnahme.
Erfolgreiche Pakete werden einzeln committed. Gescheiterte Pakete und ungeeignete
Browserprüfungen stehen in `automatisierung-offen.md`. Keine Änderung der alten Vorlage.

## Zuordnung (wird nach geprüften Paketen fortgeschrieben)

| Ursprüngliche ID | Gegenstand | Ausgangsabdeckung / Rest |
|---|---|---|
| START-01 | Erststart mit frischer Datenbank | INFRA-01/02 und AUTH-01A: UI/Bootstrap; Tabellen/Logprüfung separat |
| START-02 | Erneuter Start erhält Daten und Konfiguration | OFFEN – noch kein entsprechender Browsernachweis |
| START-03 | Erststart und Neustart verwenden die richtige Mailkonfiguration | REG-01A und RESET-01A: SMTP beim frischen Start; Neustart fehlt |
| REG-01 | Gültige Registrierung | REG-01A: Registrierung, Mail, Ablehnung vor Aktivierung; Admininspektion fehlt |
| REG-02 | Pflichtfelder und ungültige Eingaben | REG-02A/B: Pflichtfelder, E-Mail und Bestätigung; weitere Einzelvarianten fehlen |
| REG-03 | Doppelte E-Mail und Kanonisierung | OFFEN – noch kein entsprechender Browsernachweis |
| VER-01 | Gültiger Link und erneute Verwendung | VER-01A/B: Aktivierung und Wiederverwendung; Footer nicht ausdrücklich verglichen |
| VER-02 | Ungültiger oder fehlender Token | VER-02A: unbekannter/fehlender Token; Anforderungsformular teilweise |
| VER-03 | Ablauf und erneuter Versand | OFFEN – noch kein entsprechender Browsernachweis |
| VER-04 | Adminsperre bleibt trotz alter Links bestehen | OFFEN – noch kein entsprechender Browsernachweis |
| AUTH-01 | Erfolgs- und Fehlerfälle | AUTH-01A–D, REG-01A und SEC-01A |
| AUTH-02 | Abmeldung, Zurück und bestehende Tabs | AUTH-02A: Logout, Tabnavigation und Zurück; alte Mutation nicht geprüft |
| AUTH-03 | Angemeldet bleiben | OFFEN – noch kein entsprechender Browsernachweis |
| RESET-01 | Gültiger Reset | RESET-01A: Mail, Passwortwechsel, alte/neue Anmeldung |
| RESET-02 | Anonyme Antwort verrät keine Konten | RESET-03A und RESET-02A: aktiv/unbekannt und Syntax; unverifiziert/gesperrt fehlen |
| RESET-03 | Passwortregeln, ungültige und verbrauchte Links | RESET-02B/04A/04B |
| RESET-04 | Ablauf auch bei bereits geöffnetem Formular | OFFEN – noch kein entsprechender Browsernachweis |
| RESET-05 | Zwei geöffnete Formulare für denselben Token | OFFEN – noch kein entsprechender Browsernachweis |
| RESET-06 | Sperre oder Löschung nach Linkerstellung | OFFEN – noch kein entsprechender Browsernachweis |
| USER-01 | Anlegen, bearbeiten und erneut öffnen | OFFEN – noch kein entsprechender Browsernachweis |
| USER-02 | Sperren und gezieltes Freigeben | OFFEN – noch kein entsprechender Browsernachweis |
| USER-03 | Löschen mit zugehörigen Tokens und Queueeinträgen | OFFEN – noch kein entsprechender Browsernachweis |
| USER-04 | Veraltetes Bearbeitungsformular | OFFEN – noch kein entsprechender Browsernachweis |
| SEC-01 | Direkte URL und normale Benutzerrolle | AUTH-01D und SEC-01A; nachträgliche Adminvergabe fehlt |
| SEC-02 | Rollenentzug bei offenem Adminformular / Push | OFFEN – noch kein entsprechender Browsernachweis |
| SEC-03 | Sperre und Löschung bei laufender Sitzung | OFFEN – noch kein entsprechender Browsernachweis |
| SEC-04 | Letzten aktiven Admin schützen | OFFEN – noch kein entsprechender Browsernachweis |
| STATUS-01 | Manueller Status und Aktualisierung | OFFEN – noch kein entsprechender Browsernachweis |
| STATUS-02 | Automatische Abwesenheit und Rückkehr | OFFEN – noch kein entsprechender Browsernachweis |
| CONFIG-01 | Zulässige Werte speichern | OFFEN – noch kein entsprechender Browsernachweis |
| CONFIG-02 | Ungültige Konfiguration bleibt abgelehnt | OFFEN – noch kein entsprechender Browsernachweis |
| MAIL-01 | Admin-Testmail und Unicode | OFFEN – noch kein entsprechender Browsernachweis |
| MAIL-02 | Erfolgreiche Queue-Verarbeitung | REG/VER/RESET: echte Queuezustellung; interne Zustände separat |
| MAIL-03 | Nachgewiesener Verbindungsfehler und Wiederholung | OFFEN – noch kein entsprechender Browsernachweis |
| MAIL-04 | Dauerhafter Fehler und Adminbeobachtbarkeit | OFFEN – noch kein entsprechender Browsernachweis |
| LANG-01 | Sichtbarer Wechsel Deutsch ↔ Englisch | Alle bisherigen Fälle pro Sprache; dynamischer Wechsel mit offenen Fehlern fehlt |
| LANG-02 | Sprache nach Anmeldung und Mailvorlagen | Mailbetreffe/Platzhalter DE/EN geprüft; Präferenzpersistenz fehlt |
| UI-01 | Desktop, Handy und andere Fensterbreiten | Desktop/Infrastrukturbilder; Handy/Rotation/Dialogsituation fehlt |
| UI-02 | Tastatur und grundlegende Bedienbarkeit | OFFEN – noch kein entsprechender Browsernachweis |
| OPS-01 | Unklarer Transportausgang bleibt quarantänisiert | OFFEN – noch kein entsprechender Browsernachweis |
| OPS-02 | Aufbewahrung und Bereinigung | OFFEN – noch kein entsprechender Browsernachweis |
| OPS-03 | Manuellen Recovery-Vertrag nachvollziehen | OFFEN – noch kein entsprechender Browsernachweis |
| OPS-04 | Anonyme Resetantwort bei technischem Fehler | OFFEN – noch kein entsprechender Browsernachweis |
| OPS-05 | Gebautes JAR und Docker, wenn vorbereitet | Runner: frisches Docker-JAR; separater IntelliJ-/JAR-Vergleich fehlt |
