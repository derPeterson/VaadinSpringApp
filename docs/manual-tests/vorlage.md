# Manuelle Tests – VaadinSpringApp

> Vorlage, Version 1.4 · erstellt am 10.10.2026.
> Diese Datei bleibt als Übergangscheckliste für die Automatisierung erhalten.
> Sie wird nicht mehr als Vorlage für neue vollständige manuelle Durchläufe erweitert.
> Die früheren Ablauf- und Protokollhinweise bleiben zur Einordnung vorhandener Archive stehen.
> Automatisierungsstatus siehe Abschnitt 18; er ist kein manueller Testerfolg.

## Inhaltsverzeichnis

- [1. Vorlage verwenden und Ergebnisse festhalten](#1-vorlage-verwenden-und-ergebnisse-festhalten)
- [2. Kopf des Testdurchlaufs](#2-kopf-des-testdurchlaufs)
- [3. Vorbereitung und Testkonten](#3-vorbereitung-und-testkonten)
- [4. Start und initiale Konfiguration](#4-start-und-initiale-konfiguration)
- [5. Registrierung](#5-registrierung)
- [6. Verifikation](#6-verifikation)
- [7. Anmeldung und Abmeldung](#7-anmeldung-und-abmeldung)
- [8. Passwortreset](#8-passwortreset)
- [9. Benutzerverwaltung](#9-benutzerverwaltung)
- [10. Rollen und bestehende Sitzungen](#10-rollen-und-bestehende-sitzungen)
- [11. Status, Aktivität und Push](#11-status-aktivität-und-push)
- [12. Konfiguration im Adminbereich](#12-konfiguration-im-adminbereich)
- [13. Mailversand und Warteschlange](#13-mailversand-und-warteschlange)
- [14. Sprache und Darstellung](#14-sprache-und-darstellung)
- [15. Erweiterte Betriebsprüfungen](#15-erweiterte-betriebsprüfungen)
- [16. Fehlerprotokoll und Nachprüfung](#16-fehlerprotokoll-und-nachprüfung)
- [17. Abschluss](#17-abschluss)
- [18. Automatisierte Teilfälle](#18-automatisierte-teilfälle)

## 1. Vorlage verwenden und Ergebnisse festhalten

Empfohlene Ablage im Projekt:

```text
docs/manual-tests/
  vorlage.md
  runs/
    2026-10-10_14-30__56f66ac__app.md
    2026-10-10_14-30__56f66ac__tests.md
    2026-10-12_18-00__anderer-commit__tests.md
  evidence/
    2026-10-10_14-30__56f66ac/
      ... bereinigte Screenshots und Belege ...
```

1. Diese Vorlage einmal als `docs/manual-tests/vorlage.md` ablegen.
2. Für jeden Durchlauf eine **Kopie** in `runs/` anlegen. Dateiname:
   `JJJJ-MM-TT_HH-mm__kurzer-commit__tests.md`; Zeitzone im Kopf erfassen.
   Die Einrichtung separat als `…__app.md` protokollieren und hier verlinken.
   Beide Dateien verwenden denselben Durchlaufnamen.
3. Kopf, Testkonten und Konfiguration ausfüllen. Ausdrücklich freigegebene
   erfundene lokale Testpasswörter dürfen in der Kontentabelle stehen.
   Echte Zugangsdaten, geheime Schlüssel, Sitzungscookies und vollständige
   Tokenlinks nicht eintragen.
4. Tests in der vorgesehenen Reihenfolge bearbeiten. Jeder Test hat eine feste ID.
5. Checkbox erst abhaken, wenn Schritte **und erwartetes Ergebnis** geprüft wurden.
   Ein grüner Test benötigt zusätzlich den Status `BESTANDEN`.
6. Bei Abweichungen Status, tatsächliches Ergebnis, Zeitpunkt und Beleg ergänzen.
   Einen Fehler im Abschnitt [Fehlerprotokoll](#16-fehlerprotokoll-und-nachprüfung)
   genauer beschreiben; dort auch die betroffenen Test-IDs vermerken.
7. Nach Codeänderungen einen **neuen Durchlauf** anlegen. Den alten Fehlversuch
   erhalten; nicht nachträglich so ändern, als wäre er schon damals erfolgreich gewesen.
8. Nach Abschluss Protokoll und geeignete Belege zusammen versionieren oder in einer
   festgelegten Ablage archivieren. Änderungen an der Vorlage gelten für künftige Läufe.

**So entsteht der Dateiname:**

Im Projektordner in PowerShell den aktuell ausgecheckten Commit kurz ausgeben lassen:

```powershell
git rev-parse --short HEAD
```

Lautet die Ausgabe beispielsweise `56f66ac` und beginnt der Test am 10.10.2026
um 14:30 Uhr, heißt die Kopie `2026-10-10_14-30__56f66ac__tests.md`.
Der hintere Teil ist also das Git-Commit-Kürzel des getesteten Code-Stands,
keine zufällige Kennung. Seine Länge kann je nach Git-Konfiguration variieren.
Datum und Uhrzeit gehören zum Beginn dieses Testdurchlaufs; die Zeitzone wird im Kopf
eingetragen. Für einen späteren Durchlauf Datum, Uhrzeit und Commit erneut ermitteln.
Bei lokalen, noch nicht committeten Änderungen diese zusätzlich im Kopf dokumentieren.

**Statuswerte:**

| Status | Bedeutung |
| --- | --- |
| `OFFEN` | Noch nicht bewertet; Ausgangsstatus in dieser Vorlage. |
| `BESTANDEN` | Schritte ausgeführt und erwartetes Ergebnis beobachtet. |
| `FEHLER` | Ausgeführt, aber mindestens ein erwartetes Ergebnis fehlt. |
| `BLOCKIERT` | Voraussetzung fehlt oder ein früherer Fehler verhindert die Prüfung; Grund angeben. |
| `NICHT AUSGEFÜHRT` | Bewusst ausgelassen; Grund angeben. |
| `NICHT ZUTREFFEND` | Für diesen Durchlauf fachlich nicht anwendbar; begründen. |

Die Checkbox allein unterscheidet Fehler nicht von ausgelassenen Tests. Daher den
Status immer ausfüllen. Die Beschreibung einer Beobachtung ist zunächst ein Befund;
ob ein Codefehler, Umgebungsproblem oder Verbesserungsvorschlag vorliegt, wird im
Fehlerprotokoll festgehalten. Mehrere Varianten eines Tests einzeln protokollieren;
`BESTANDEN` gilt erst, wenn alle anwendbaren Varianten erfolgreich waren.

**Umfang:** Abschnitte 4–14 bilden den normalen vollständigen Browserdurchlauf.
Abschnitt 15 enthält gesonderte Betriebs- und technische Prüfungen. Dafür fehlende
Werkzeuge oder Fixtures als `BLOCKIERT` dokumentieren; Browsererfolg ersetzt diese
Prüfungen nicht. Ein gezielter Nachtest darf kürzer sein, muss seinen Umfang nennen.

## 2. Kopf des Testdurchlaufs

| Feld | Eintrag |
| --- | --- |
| Durchlauf-ID / Dateiname | … |
| Einrichtungsprotokoll / Konfigurationsdatei | … |
| Art | Vollständiger Durchlauf / gezielter Nachtest: … |
| Vorlagenversion | 1.4 |
| Beginn / Ende mit Zeitzone | … |
| Geprüft von | … |
| Vollständiger Git-Commit | … |
| Branch | … |
| Arbeitsbaum | sauber / lokale Änderungen: … |
| Bei lokalen Änderungen | Patch/Beleg und Beschreibung: … |
| Vergleichslauf / letzter erfolgreicher Lauf | … |
| Anlass / Änderungen seit Vergleichslauf | … |
| Anwendungs-URL | … |
| Startart | IntelliJ / Maven Wrapper / JAR / Docker: … |
| Java / Vaadin / Spring Boot | … |
| Betriebssystem | … |
| Browser und Version(en) | … |
| Geräte / Bildschirmgrößen / Zoom | … |
| Datenbank | frische Testdatenbank / bestehende Testkopie; Typ und isolierter Speicherort: … |
| Datenbankzustand / Fixture | neu / aus Testlauf … übernommen; Ausgangsdaten: … |
| SMTP-Testumgebung | lokale Mailablage / anderer kontrollierter Testserver: … |
| SMTP-Ziel ohne Zugangsdaten | Host, Port, Auth-/TLS-Modus: … |
| Anzahl Anwendungsinstanzen | … |
| Relevante Konfiguration | siehe Tabelle unten; Dateiwerte und beobachtete gespeicherte Werte unterscheiden |
| Letzter automatischer Testlauf | Datum, Commit, Befehl, Ergebnis und Beleg: … |
| Startzeit / Zeitbasis | Server-Zeitzone, Browser-Zeitzone, Uhrzeit: … |
| Belegablage | … |

Den tatsächlich getesteten Stand erfassen, nicht nur „aktuelle Version“. Hilfreiche
Lesebefehle im Projektverzeichnis:

```powershell
git rev-parse HEAD
git branch --show-current
git status --short
```

Ein Commit allein beschreibt lokale Änderungen oder die Konfiguration nicht.
Diese separat dokumentieren; geheime Werte nur über eine lokale Referenz benennen.

### Relevante Einstellungen

| Einstellung | Ausgangswert / während des Tests / wiederhergestellt |
| --- | --- |
| `service.name`, `base.url` | … |
| Admin-Bootstrap aktiviert? | … |
| H2-Konsole freigegeben? | … |
| `rememberMe.duration` | … |
| `user.auto.absent.timeout` | … |
| `verificationToken.valid.duration` / `.live.duration` | … |
| `passwordResetToken.valid.duration` / `.live.duration` | … |
| `email.queue.max_retry` | … |
| `email.queue.pool.size` / `.capacity` | … |
| `email.queue.sent.live.duration` | … |
| SMTP-Einstellungen, Absender, Admin-Testempfänger | … |
| Weitere Änderungen / Docker-Konfiguration, falls vorhanden | … |

SMTP- und Remember-me-Einstellungen werden beim Start gelesen; Änderungen können
einen Neustart benötigen. Vorgaben für die Erstbefüllung überschreiben keine bereits
gespeicherten Konfigurationswerte. Diese Unterschiede bei einem Fehler mit erfassen.

## 3. Vorbereitung und Testkonten

- [ ] Eine getrennte Testdatenbank festgelegt. „Frisch“ heißt tatsächlich leer;
  alte Anwendungsdaten nicht versehentlich weiterverwenden oder überschreiben.
- [ ] Kontrollierten SMTP-Testserver bereitgestellt. Eine lokale Mailablage prüft
  SMTP-Annahme und Mailinhalt; sie beweist keine Zustellung bei externen Providern.
- [ ] Anwendungs-URL und `base.url` passen zusammen, auch beim Aufruf vom Handy.
  `localhost` in einem Link zeigt auf das Gerät, das den Link öffnet.
- [ ] Vorhandene Startanleitung und Betriebsverträge gelesen; keine Migrationen
  an Bestandsdatenbanken als Teil eines frischen Browserdurchlaufs ausführen.
- [ ] Zwei **getrennte Browserprofile/Sitzungen** eingerichtet: A für den steuernden
  Admin, B für das betroffene Konto. Zwei Tabs desselben Profils teilen meist Cookies
  und reichen für verschiedene Konten nicht. Für weitere Konten ein drittes Profil nutzen.
- [ ] Testzugänge dokumentiert; bei ausdrücklich erlaubten erfundenen lokalen Werten in der Kontentabelle. Testmails und Belege enthalten nur Testdaten.
- [ ] Sicherstellen, dass Bootstrap-Rollen und ein testbarer Adminzugang vorhanden
  sein werden. Ohne aktivierten Bootstrap entsteht kein automatisches Adminkonto.
- [ ] Wartezeiten und ursprüngliche Konfiguration notiert; Änderungen danach zurücksetzen.

Die folgenden Namen sind Rollen im Testplan, keine fest eingebauten Konten:

| Kürzel | Verwendung | Test-E-Mail / Ausgangszustand | Lokales erfundenes Testpasswort, falls freigegeben |
| --- | --- | --- | --- |
| A1 | Steuernder aktiver Admin | … | … |
| A2 | Zweiter aktiver Admin für Rollen-/Letzter-Admin-Tests | … | … |
| U1 | Registrierter und verifizierter normaler Benutzer | … | … |
| U2 | Noch nicht verifiziertes Konto | … | … |
| U3 | Konto für Sperren und alte Links | … | … |
| U4 | Konto für Löschung | … | … |
| X | Syntaktisch gültige, nicht registrierte E-Mail | … | kein Konto |

Konten bei Bedarf erst während des Durchlaufs anlegen. Für unabhängige Varianten
zusätzliche Konten mit eindeutigen Adressen verwenden. Tests, die ein Konto sperren,
sein Passwort ändern oder es löschen, verändern die Voraussetzungen späterer Tests.

## 4. Start und initiale Konfiguration

### START-01 · Erststart mit frischer Datenbank

1. Testdatenbank und SMTP-Testserver bereitstellen; Bootstrap-Einstellung notieren.
2. Anwendung mit der gewählten Startart starten; Startzeit erfassen.
3. `/` und `/login` öffnen. Mit dem vorgesehenen Admin anmelden und `/admin` öffnen.
4. Initiale Konfiguration und Rollen kontrollieren, über UI oder vorhandene
   freigegebene Lesewerkzeuge. Startlogs auf relevante Fehler prüfen.

**Erwartet:** Anwendung und Seiten erreichbar; Tabellen einschließlich Admin-Mailaudit
angelegt; `ROLE_USER` und `ROLE_ADMIN` vorhanden; Konfigurationswerte initialisiert.
Ein Bootstrap-Admin entsteht nur bei aktivierter Anlage mit vollständigen Angaben.
Falls kein Admin entstehen soll, den fehlenden Zugang nicht als Testfehler werten,
sondern die bewusste Konfiguration und den vorgesehenen Zugang dokumentieren.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Zeitpunkt / Beleg:** …

### START-02 · Erneuter Start erhält Daten und Konfiguration

1. Über den Adminbereich einen eindeutig erkennbaren Testwert speichern,
   beispielsweise den Anzeigenamen. Vorherigen Wert notieren.
2. Anwendung beenden und mit **derselben Testdatenbank** erneut starten.
3. Admin anmelden, Konfiguration und vorhandene Konten erneut ansehen.
4. Auch geänderte Erstbefüllungs-Vorgaben dürfen vorhandene Werte nicht überschreiben.
   Bereits durchgeführte Teile aus dem Einrichtungsprotokoll übernehmen;
   nur die dort nicht belegten Schritte zusätzlich prüfen.

**Erwartet:** Gespeicherte Werte und Konten bleiben erhalten; keine doppelten Rollen
oder Bootstrap-Konten. Zuletzt gespeicherter Konfigurationswert weiterhin vorhanden.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Zeitpunkt / Beleg:** …

### START-03 · Erststart und Neustart verwenden die richtige Mailkonfiguration

1. Wirksame SMTP-Einstellungen mit den vorgesehenen Testwerten vergleichen.
2. Im Einrichtungsprotokoll festhalten, ob vor dem vollständigen Neustart
   überhaupt ein Mailversuch durchgeführt wurde. Wenn ausgelassen, nicht
   rückwirkend als bestanden oder fehlgeschlagen darstellen. Nach dem
   vollständigen Neustart den Admin-Testversand aus MAIL-01 prüfen.
3. Falls Einstellungen korrigiert werden müssen, Änderung und Neustart erfassen;
   Test danach wiederholen, ohne den fehlgeschlagenen Erstversuch zu überschreiben.

**Erwartet:** Versand erreicht den vorgesehenen Testserver. Eine abweichende Wirkung
zwischen Erststart und Neustart wird ausdrücklich festgehalten, nicht durch den
erfolgreichen zweiten Versuch verdeckt.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Zeitpunkt / Beleg:** …

## 5. Registrierung

### REG-01 · Gültige Registrierung

1. Anonym `/registration` öffnen und neue Testadresse U1 verwenden.
2. Alle Pflichtfelder gültig ausfüllen. Geburtsdatum in der Vergangenheit wählen.
3. Ein Testpasswort mit mindestens acht Zeichen, Großbuchstaben und Sonderzeichen
   wählen; eine Ziffer ist nach dem aktuellen Vertrag keine zusätzliche Pflicht.
4. Registrierung abschicken, sichtbare Bestätigung und Mailablage prüfen.
5. Konto im Adminbereich nachsehen; vor Verifikation einen Login versuchen.

**Erwartet:** Genau ein Konto mit normaler Benutzerrolle angelegt; Verifikationsmail
vorhanden; Konto vor Verifikation nicht für normale Anmeldung freigegeben. Keine
technischen Exceptiontexte in der Oberfläche. Nach Verifikation als U1 weiterverwenden.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Zeitpunkt / Beleg:** …

### REG-02 · Pflichtfelder und ungültige Eingaben

1. Je Versuch genau einen Fehler erzeugen: blanker Vor-/Nachname, fehlendes oder
   heutiges/zukünftiges Geburtsdatum, fehlerhafte E-Mail, zu kurzes Passwort,
   Passwort ohne Großbuchstaben oder ohne Sonderzeichen, abweichende Bestätigung.
2. Jeweils absenden; Feldmarkierung und Hinweis prüfen.
3. Fehler korrigieren und erneut absenden; für echte Neuregistrierungen neue Adressen verwenden.

**Erwartet:** Ungültige Eingaben verständlich markiert; kein Konto und keine
Verifikationsmail aus dem ungültigen Versuch. Nach Korrektur ist der gültige Ablauf möglich.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Zeitpunkt / Beleg je Variante:** …

### REG-03 · Doppelte E-Mail und Kanonisierung

1. Mit bereits vorhandener Adresse erneut registrieren.
2. Variante mit anderer Groß-/Kleinschreibung und äußeren Leerzeichen versuchen.
3. Meldung und Zahl der vorhandenen Konten prüfen.

**Erwartet:** Keine zweite Identität für dieselbe kanonisierte E-Mail; verständlicher
Hinweis. Keine bereits vorhandenen Kontodaten werden überschrieben.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Zeitpunkt / Beleg:** …

## 6. Verifikation

### VER-01 · Gültiger Link und erneute Verwendung

1. Verifikationsmail von U1 öffnen. Empfänger, Sprache, Betreff, Name,
   Serviceangabe, Footer, Linkziel und nicht ersetzte Platzhalter prüfen.
2. Link in einer anonymen Sitzung öffnen und Aktivierung abschließen.
3. Mit U1 anmelden; anschließend denselben Link erneut anonym öffnen.

**Erwartet:** Gültiger Link aktiviert das Konto; Anmeldung anschließend möglich.
Ein bereits verwendeter Link darf beim weiterhin aktiven Konto erneut Erfolg anzeigen,
ohne den Zustand nochmals zu ändern. Er darf ein später gesperrtes Konto nicht
reaktivieren (siehe VER-04). Keine Rohplatzhalter oder fremden Kontodaten in der Mail.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Zeitpunkt / Beleg:** …

### VER-02 · Ungültiger oder fehlender Token

1. Einen Verifikationslink mit verändertem Token öffnen; Original nicht verbreiten.
2. Verifikationsseite ohne Token öffnen; angebotenen Ablauf beobachten.
3. Ggf. eine unzulässige Eingabe in das dort angebotene Mailformular versuchen.

**Erwartet:** Keine Aktivierung durch ungültigen Token; verständlicher Hinweis oder
vorgesehener Anforderungsablauf. Keine Exception, SQL-Ausgabe oder fremden Kontodaten.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Zeitpunkt / Beleg:** …

### VER-03 · Ablauf und erneuter Versand

1. Für ein eigenes neues U2-Testkonto bei Bedarf vorübergehend eine kurze,
   positive Token-Gültigkeit einstellen; Ausgangswert und Zeitpunkt notieren.
2. Neue Mail anfordern. Auf den tatsächlichen Ablauf dieses neu erzeugten Tokens warten.
3. Alten Link öffnen; vorgesehenen erneuten Versand benutzen.
4. Neuen Link prüfen. Während eine Mail bereits offen in der Queue ist, eine
   weitere Anforderung versuchen und die erzeugten Nachrichten beobachten.
5. Ursprüngliche Konfiguration wiederherstellen.

**Erwartet:** Abgelaufener Link aktiviert nicht; gültiger neuer Link funktioniert.
Verbrauchte/ungültig gemachte Links werden nicht wieder gültig. Offene Nachrichten
desselben Kontos/Typs werden gemäß dem Deduplizierungsvertrag nicht beliebig vervielfacht.
Laufzeitänderungen gelten für neu erzeugte Tokens, nicht rückwirkend für alte.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Zeiten / Mailanzahl / Beleg:** …

### VER-04 · Adminsperre bleibt trotz alter Links bestehen

1. Für U3 einen noch unbenutzten gültigen Verifikationslink vorbereiten.
2. U3 im Adminbereich ausdrücklich sperren; Admin-Sperre von fehlender
   Verifikation unterscheiden. Link öffnen und ggf. erneuten Versand versuchen.
3. Dasselbe mit einem bereits verifizierten, anschließend gesperrten Konto prüfen.
4. Login versuchen; Sperrzustand erneut prüfen.

**Erwartet:** Link und erneuter Versand heben die Adminsperre nicht auf. Kein Login
des gesperrten Kontos. Alte Links werden nicht zur Umgehung der Sperre verwendet.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Zeitpunkt / Beleg je Variante:** …

## 7. Anmeldung und Abmeldung

### AUTH-01 · Erfolgs- und Fehlerfälle

1. Mit verifiziertem U1 und richtigem Passwort anmelden.
2. Abmelden. Anschließend falsches Passwort, unbekannte Adresse X und noch nicht
   verifiziertes U2 ausprobieren; jeweils frischen Ausgangszustand herstellen.
3. Geschützte Seite ohne Anmeldung direkt über die URL öffnen.

**Erwartet:** Nur zulässige Kombination führt zur Anmeldung. Fehler verständlich,
ohne technische Details. Geschützte Seiten sind anonym nicht nutzbar. Eine bewusst
konfigurierte Sperrregel nur dann erwarten, wenn sie tatsächlich implementiert ist;
der bloße Konfigurationsname ist kein Nachweis.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Zeitpunkt / Beleg je Variante:** …

### AUTH-02 · Abmeldung, Zurück und bestehende Tabs

1. Als U1 eine geschützte Seite in zwei Tabs desselben Browserprofils öffnen.
2. In einem Tab abmelden. Im anderen eine Aktion versuchen; Browser-Zurück und
   anschließend Neuladen einer geschützten URL prüfen.

**Erwartet:** Keine neue geschützte Aktion nach Logout möglich; erforderliche
Weiterleitung/erneute Anmeldung. Eine vom Browser gecachte Ansicht allein beweist
keine neue Serverberechtigung; Aktion und Neuladen separat bewerten.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Zeitpunkt / Beleg:** …

### AUTH-03 · Angemeldet bleiben

1. Ein frisches normales Browserprofil ohne bestehende Login-Cookies verwenden.
2. Mit aktivierter „Angemeldet bleiben“-Option anmelden. Browser vollständig
   schließen; ohne Sitzungswiederherstellung erneut öffnen und geschützte Seite besuchen.
3. Explizit abmelden; Browser erneut öffnen und Zugriff versuchen.
4. In einem zweiten frischen Profil ohne diese Option wiederholen. Browser-
   Einstellungen zur Wiederherstellung von Sitzungscookies dokumentieren.

**Erwartet:** Remember-me ermöglicht innerhalb der eingestellten Laufzeit die
vorgesehene Wiederanmeldung; expliziter Logout beendet sie. Ergebnis ohne Option
nicht aus bloßem Schließen eines Tabs ableiten. Falls der Browser Sitzungscookies
wiederherstellt, Variante ohne Option gesondert als nicht eindeutig geprüft markieren.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Browserverhalten / Beleg:** …

## 8. Passwortreset

### RESET-01 · Gültiger Reset

1. Anonym `/forgot-password` öffnen und Reset für aktives U1 anfordern.
2. Mailinhalt wie in VER-01 prüfen; Link öffnen.
3. Neues gültiges Testpasswort eingeben und speichern.
4. In frischer Sitzung neues Passwort testen, danach altes Passwort versuchen.

**Erwartet:** Neues Passwort funktioniert, altes nicht mehr. Reset-Mail korrekt,
Linkziel erreichbar; keine technischen Details in der Oberfläche.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Zeitpunkt / Beleg:** …

### RESET-02 · Anonyme Antwort verrät keine Konten

1. Reset für aktive, unbekannte, unverifizierte und administrativ gesperrte
   Testadresse jeweils mit syntaktisch gültiger Eingabe anfordern.
2. Sichtbaren Text, Form und Verhalten vergleichen.
3. Syntaxfehler separat eingeben, um normale Eingabevalidierung zu prüfen.

**Erwartet:** Gleiche allgemeine Bestätigung für syntaktisch gültige Anfragen;
kein Hinweis, ob das Konto existiert oder tatsächlich eine Mail verschickt wurde.
Ungültiges E-Mail-Format darf als Eingabefehler erscheinen. Dies prüft keinen
Timing-Schutz und keine Rate-Limits.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Zeitpunkt / Beleg je Variante:** …

### RESET-03 · Passwortregeln, ungültige und verbrauchte Links

1. Auf gültiger Resetseite ungültige Passwörter/abweichende Bestätigung versuchen.
2. Danach gültig speichern; denselben Link erneut öffnen und erneut speichern versuchen.
3. Zusätzlich einen Link mit verändertem Token öffnen.

**Erwartet:** Ungültige Eingaben ändern das Passwort nicht. Verwendeter und
ungültiger Link erlauben keine weitere Änderung. Hinweise verständlich und lokalisiert.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Zeitpunkt / Beleg je Variante:** …

### RESET-04 · Ablauf auch bei bereits geöffnetem Formular

1. Kurze positive Reset-Gültigkeit für einen neuen Testtoken einstellen;
   Ausgangswert und Frist notieren.
2. Link vor Ablauf öffnen, Formular offen lassen und Ablauf abwarten.
3. Erst danach ein gültiges neues Passwort absenden; Link nochmals öffnen.
4. Ursprüngliche Einstellung wiederherstellen.

**Erwartet:** Abgelaufener Token wird auch beim Speichern erneut abgelehnt;
kein Passwortwechsel durch das zuvor geöffnete Formular. Exakte Gleichheit mit
der Ablaufgrenze ist manuell nicht zuverlässig prüfbar und bleibt eine automatische Regression.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Ablaufzeit / Absendezeit / Beleg:** …

### RESET-05 · Zwei geöffnete Formulare für denselben Token

1. Denselben neuen Resetlink in zwei getrennten anonymen Sitzungen öffnen.
2. In Sitzung B Passwort ändern. Anschließend im noch offenen Formular der
   anderen Sitzung eine andere Änderung versuchen.
3. Login mit dem erfolgreich gesetzten Passwort kontrollieren.

**Erwartet:** Einmal verwendeter Token kann aus dem alten Formular nicht erneut
verbraucht werden; das zuerst erfolgreich gesetzte Passwort bleibt gültig.
Der manuelle Ablauf belegt keine vollständige Absicherung aller Parallelitätsfälle.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Reihenfolge / Zeitpunkt / Beleg:** …

### RESET-06 · Sperre oder Löschung nach Linkerstellung

1. Für U3 einen gültigen Resetlink erstellen; Konto anschließend als Admin sperren.
2. Reset über alten Link versuchen und gesperrten Login kontrollieren.
3. Mit einem separaten U4-Konto Link erstellen, Konto löschen und alten Link öffnen.

**Erwartet:** Reset hebt die Adminsperre nicht auf; gesperrter Login bleibt verhindert.
Ein Link für ein gelöschtes Konto erlaubt keine Passwortänderung oder Neuanlage.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Zeitpunkt / Beleg je Variante:** …

## 9. Benutzerverwaltung

### USER-01 · Anlegen, bearbeiten und erneut öffnen

1. Als A1 einen normalen Benutzer im Adminbereich mit gültigen Daten anlegen.
2. Daten, Rollen und vorgesehenen Aktivierungszustand nach erneutem Öffnen prüfen.
3. Namen und E-Mail ändern, speichern, Ansicht neu öffnen und aktuelle Loginidentität prüfen.
4. Ungültige Namen/E-Mail/Passwort bzw. vorhandene E-Mail bei Anlage und Änderung versuchen.

**Erwartet:** Zulässige Änderungen gespeichert; Login nutzt aktuelle E-Mail.
Unzulässige Eingaben/Dubletten verständlich markiert, ohne Teiländerungen.
Passwortänderung nur bei bewusstem gültigem neuen Passwort; ein leer gelassenes
optionales Passwortfeld bei Bearbeitung ändert das bestehende Passwort nicht.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Zeitpunkt / Beleg je Variante:** …

### USER-02 · Sperren und gezieltes Freigeben

1. Normalen aktiven Testbenutzer sperren; Status nach erneutem Öffnen und Login prüfen.
2. Konto über vorgesehene Adminaktion freigeben; erneut prüfen.
3. Ein zusätzliches, noch nicht verifiziertes Testkonto ausdrücklich als Admin
   aktivieren; Anmeldung und Zustand nach erneutem Öffnen prüfen.

**Erwartet:** Gespeicherter Zustand entspricht der bewussten Aktion. Gesperrtes Konto
kann sich nicht anmelden. Eine ausdrückliche Admin-Aktivierung erlaubt die Anmeldung
auch ohne vorherigen Verifikationslink und beendet den ausstehenden Verifikationszustand.
Eine Adminsperre darf dagegen durch alte Links oder erneuten Mailversand nicht aufgehoben
werden (siehe VER-04). Automatische Linkaktivierung und bewusste Adminentscheidung
als unterschiedliche Abläufe dokumentieren.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Zeitpunkt / Beleg:** …

### USER-03 · Löschen mit zugehörigen Tokens und Queueeinträgen

1. Für U4 Testkonto, Token und passende offene Testmail vorbereiten.
2. U4 als Admin über Bestätigung löschen. Benutzerliste neu laden.
3. Login und alte Verifikations-/Resetlinks versuchen; Queuezustand mit
   vorhandenen freigegebenen Lesewerkzeugen prüfen.

**Erwartet:** Benutzer entfernt; zugehörige Token/Benutzermails entfernt; keine
spätere Nachricht aus einer noch offenen Queue für das gelöschte Konto.
Auditdaten dürfen gemäß dokumentiertem Vertrag bewusst erhalten bleiben.
Ein bereits begonnener SMTP-Aufruf wird nicht rückwirkend zurückgenommen.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Zeitpunkt / Beleg:** …

### USER-04 · Veraltetes Bearbeitungsformular

1. Dasselbe Konto in zwei getrennten Adminsitzungen zur Bearbeitung öffnen.
2. In A speichern oder sperren. Danach im alten Formular der anderen Sitzung
   eine abweichende Änderung speichern.
3. Frisch laden und den tatsächlichen gespeicherten Zustand prüfen.

**Erwartet:** Veraltete Daten überschreiben nicht unbemerkt aktuelle Kontosperren,
Passwortänderungen oder Versionsstände. Ein Konflikt fordert verständlich zum
erneuten Öffnen/Prüfen auf. Kein SQL-/Exceptiontext für den Benutzer.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Reihenfolge / Zeitpunkt / Beleg:** …

## 10. Rollen und bestehende Sitzungen

### SEC-01 · Direkte URL und normale Benutzerrolle

1. `/admin` anonym direkt öffnen; danach als normaler U1 versuchen.
2. A2 bewusst Adminrolle geben; frische Anmeldung als A2 durchführen.
3. Adminbereich aufrufen, danach alle aktuellen Rollen erneut prüfen.

**Erwartet:** Gast und normaler Benutzer können Adminfunktionen nicht nutzen;
frisch angemeldeter aktiver Admin mit passender Rolle hat den vorgesehenen Zugriff.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Zeitpunkt / Beleg:** …

### SEC-02 · Rollenentzug bei offenem Adminformular / Push

1. Mindestens A1 und A2 als aktive Admins sicherstellen.
2. In B als A2 ein Adminformular öffnen, Eingaben vorbereiten, noch nicht speichern.
3. In A als A1 die Adminrolle von A2 entfernen.
4. **Ohne vorheriges Neuladen** in B die geschützte Aktion auslösen. Danach eine
   weitere Aktion und direkten `/admin`-Aufruf versuchen.
5. Falls Push verbunden war, auch den bestehenden UI-Kanal und dessen Verhalten beobachten.

**Erwartet:** Neue geschützte Aktionen über alte UI/Push-Verbindung werden an der
nächsten Sicherheitsgrenze verhindert; keine unberechtigte Änderung gespeichert.
Bereits vor Entzug begonnene Aktionen werden nicht rückwirkend widerrufen.
Vorhandene Tests für direkte RPC-Aufrufe werden durch diesen Browsertest ergänzt.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Zeit des Entzugs / Aktion / Beleg:** …

### SEC-03 · Sperre und Löschung bei laufender Sitzung

1. U3 in B anmelden und eine geschützte Ansicht offen halten.
2. In A U3 sperren. Ohne Neuladen in B eine Aktion auslösen; anschließend neu laden.
3. Variante mit einem eigenen U4-Konto und Löschung wiederholen.
4. Variante mit Remember-me-Anmeldung prüfen, soweit eingerichtet.

**Erwartet:** Neue geschützte Aktionen trotz alter Sitzung verhindert; erneute
Anmeldung des gesperrten/gelöschten Kontos nicht möglich. Remember-me umgeht den
aktuellen gespeicherten Kontozustand nicht.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Zeitpunkt / Beleg je Variante:** …

### SEC-04 · Letzten aktiven Admin schützen

1. Erst sicherstellen, dass A1 als aktiver Admin erreichbar ist und ein geplanter
   Rückweg besteht. Keine Änderungen außerhalb der Testumgebung.
2. A2 deaktivieren oder dessen Adminrolle entfernen, sodass A1 der einzige aktive Admin ist.
3. Bei A1 Rollenentzug, Sperren und Löschen jeweils über die vorhandene UI versuchen.
4. Alle Kontozustände frisch prüfen; A2 anschließend wieder passend herstellen.

**Erwartet:** Keine dieser Aktionen entfernt den letzten aktiven Admin. Der Grund
wird verständlich angezeigt; A1 bleibt administrativ nutzbar. Falls eine Variante
schon wegen Selbstschutz nicht angeboten wird, diese UI-Prüfung dokumentieren;
sie ersetzt nicht die bestehenden Service-/Parallelitätsregressionen.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Zeitpunkt / Beleg je Variante:** …

## 11. Status, Aktivität und Push

### STATUS-01 · Manueller Status und Aktualisierung

1. Bei normalem Testkonto die vorhandenen Statusoptionen auswählen.
2. Anzeige nach erneutem Öffnen prüfen. Eine zweite Ansicht/Sitzung beobachten,
   soweit dort der Zustand desselben Kontos sichtbar ist.
3. Vorhandene Push-Verbindung offen lassen und Änderung dort beobachten.

**Erwartet:** Gespeicherter und sichtbarer Status stimmen überein; verbundene
Ansichten aktualisieren sich gemäß der vorhandenen Anzeige, ohne dass dafür eine
neue Anmeldung nötig ist. Ein nicht angebotenes Fremdbenutzer-Statusfeld nicht voraussetzen.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Sichtbarkeit / Zeitpunkt / Beleg:** …

### STATUS-02 · Automatische Abwesenheit und Rückkehr

1. `user.auto.absent.timeout` notieren; ggf. vorübergehend eine kurze positive Dauer setzen.
2. Testkonto auf verfügbar stellen und keine Navigation/Aktivitätsereignisse auslösen.
3. Timeout plus nächsten Schedulerlauf abwarten; Wechsel auf abwesend beobachten.
4. Wieder eine Navigation auslösen, die Aktivität erfasst; nächsten Rückkehrlauf abwarten.
5. Manuell auf abwesend setzen und dieselbe Navigation wiederholen.
6. Ursprünglichen Timeout wiederherstellen.

**Erwartet:** Automatische Abwesenheit nach Inaktivität; automatisch abwesendes
Konto nach erfasster Aktivität wieder verfügbar. Bewusst manuell gesetzte Abwesenheit
wird nicht durch diesen Rückkehrlauf aufgehoben. Nicht jede Mausbewegung ist automatisch
ein Aktivitätsereignis; tatsächlich verwendete Navigation notieren.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Timeout / beobachtete Zeiten / Beleg:** …

## 12. Konfiguration im Adminbereich

### CONFIG-01 · Zulässige Werte speichern

1. Ausgangswerte sichern. Einen Textwert, einen vorhandenen Boolean und einen
   erlaubten Zahlen-/Dauerwert über die passenden UI-Felder ändern und speichern.
2. Ansicht erneut öffnen; Werte vergleichen. Bedeutung anhand der jeweiligen
   Funktion prüfen, z. B. neue Tokenlaufzeit oder Anzeigename.
3. Für beim Start eingelesene Einstellungen Neustart dokumentieren und danach prüfen.
4. Ausgangswerte wiederherstellen.

**Erwartet:** Zulässige Werte gespeichert und in der vorgesehenen Funktion wirksam;
keine Verwechslung zwischen Sofortwirkung und Wirkung nach Neustart.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Schlüssel und Werte / Neustart / Beleg:** …

### CONFIG-02 · Ungültige Konfiguration bleibt abgelehnt

1. Anhand der vorhandenen editierbaren Felder ungültige Zahl, ungültiges Dauerformat
   und nicht positive Token-Gültigkeit versuchen; pro Versuch nur einen Fehler setzen.
2. Pflichtfeld leeren; Boolean-Eingabe nur prüfen, wenn die UI freie Texte erlaubt.
3. Jeweils Speichern versuchen, Hinweis prüfen und gespeicherte Werte frisch lesen.
4. Zulässige leere SMTP-Zugangsdaten ohne Authentifizierung nicht als Fehler behandeln.

**Erwartet:** Fachlich unzulässige Werte verständlich abgelehnt; bisher gültige
Konfiguration nicht unbemerkt beschädigt. Erlaubte Leerwerte bleiben zulässig.
Bei unbekanntem Vertrag Beobachtung als Klärungsbedarf festhalten, nicht stillschweigend
eine neue Regel voraussetzen.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Schlüssel / Variante / Beleg:** …

## 13. Mailversand und Warteschlange

### MAIL-01 · Admin-Testmail und Unicode

1. Im Adminbereich gültigen Testempfänger, Betreff und Text eingeben.
2. Umlaute, `ß` und weitere Unicode-Zeichen in Betreff und Inhalt verwenden.
3. Absenden; Meldung sowie tatsächliche Nachricht in der SMTP-Testablage prüfen.
4. Fehlenden/ungültigen Empfänger versuchen und Validierung prüfen.

**Erwartet:** Korrekte Empfänger-/Absenderangaben, Betreff und lesbarer Inhalt;
kein Zeichensalat. Ungültige Eingaben führen nicht zu einer vermeintlichen
Erfolgsmeldung. Admin-Testversand ist ein direkter Versand; seine Notification
beweist nicht die spätere Queue-Zustellung anderer Mails.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Testmail-Referenz / Zeitpunkt / Beleg:** …

### MAIL-02 · Erfolgreiche Queue-Verarbeitung

1. Über eine zulässige Registrierung oder Resetanforderung eine neue Mail erzeugen.
2. Mailablage und, soweit mit vorhandenen Lesewerkzeugen sichtbar, Queue verfolgen.
3. Verarbeitung sowie weitere Schedulerläufe abwarten; Zahl passender Nachrichten prüfen.

**Erwartet:** Fachlich erwartete Mail korrekt; beim beobachteten Erfolgsablauf
kein erneuter Versand nach Abschluss. Queue durchläuft `PENDING`/`IN_PROGRESS`
zu `SENT`; kurze Zwischenzustände müssen in der UI nicht sichtbar sein. `SENT`
belegt den erfolgreichen Senderaufruf/Ergebnisstand, nicht jede externe Endzustellung.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Queue-ID ohne Token / Mailanzahl / Zeiten / Beleg:** …

### MAIL-03 · Nachgewiesener Verbindungsfehler und Wiederholung

1. Den lokalen SMTP-Testserver vor dem Versand stoppen oder kontrolliert
   unerreichbar machen; nur den eigenen Testserver verwenden.
2. Eine neue fachlich zulässige Queue-Mail erzeugen. Fehlerzeit und Queue-ID notieren.
3. Mindestens einen Wiederholungsversuch beobachten; SMTP-Testserver **vor**
   Ausschöpfung des Limits wieder bereitstellen. Bei Sender-Konfigurationsänderung neu starten.
4. Auf den nächsten fälligen Versuch und Nachrichteneingang warten.

**Erwartet:** Nachweislicher Vorversand-Verbindungsfehler erlaubt begrenzte Retries;
Retry-Abstand mindestens eine Minute nach dem gespeicherten Fehlerzeitpunkt, zusätzlich
Scheduler-/Verarbeitungszeit. Nach Wiederherstellung erfolgt zulässiger Versand;
Versuchszahl in Logs stimmt mit dem beobachteten Versuch überein. Kein blindes
Retry-Versprechen für unklare Fehler nach Transportbeginn.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / max_retry / Versuche und Zeiten / Beleg:** …

### MAIL-04 · Dauerhafter Fehler und Adminbeobachtbarkeit

1. Mit einer eigenen neuen Queue-Mail SMTP-Ausfall kontrolliert länger bestehen lassen.
2. Konfiguriertes Retrylimit und Wartezeit berücksichtigen; letzter Versuch ist
   initialer Versuch plus erlaubte Wiederholungen. Nicht unbegrenzt auf ein Ergebnis warten.
3. Terminalen Queuezustand, Logs und vorhandene Admin-Auditaufzeichnung prüfen.
4. SMTP-Testserver wieder bereitstellen; weitere Läufe beobachten.

**Erwartet:** Nach Ausschöpfung `FAILED`; andere Nachrichten/gespeicherte Erfolge
bleiben erhalten. Adminmeldung enthält keine vollständige Benutzerentität oder
Passwort-Hashes. Ein fehlgeschlagener Adminversuch ist kein erfolgreicher Versand.
Admin-Auditstatus `SENT`, `NOT_SENT`, `UNKNOWN` oder offenes `IN_PROGRESS`
entsprechend Beleg bewerten; bei weiterhin unerreichbarem SMTP kann `UNKNOWN`
zulässig sein. Keine automatischen Admin-Retries nach Wiederherstellung.
Fehlende Auditzeile bei Speicher-/Prozessausfall als Beobachtung festhalten,
nicht als Nichtversandbeweis auslegen.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Queue-/Auditstatus / Zeiten / Beleg:** …

## 14. Sprache und Darstellung

### LANG-01 · Sichtbarer Wechsel Deutsch ↔ Englisch

1. Auf Login, Registrierung, Verifikation und Resetseiten die Sprache in beide
   Richtungen wechseln. Überschriften, Felder, Buttons und Hinweise ansehen.
2. Im Adminbereich Benutzerformular und Konfiguration sowie Mailformular öffnen;
   jeweils sichtbare Feldfehler erzeugen und Sprache wechseln.
3. Während einer noch sichtbaren Notification oder eines Dialogs wechseln, soweit
   der vorhandene Sprachschalter erreichbar ist. Auch Titel prüfen.

**Erwartet:** Sichtbare Texte und dynamische Fehlerhinweise wechseln passend;
keine Rohschlüssel, technische englische Exceptions oder abgeschnittenen Texte.
Für wegen Navigation geschlossene Hinweise den neu geöffneten Zustand separat
prüfen; daraus keinen Nachweis für die Aktualisierung einer offenen Notification ableiten.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Seite / Richtung / Beleg je Variante:** …

### LANG-02 · Sprache nach Anmeldung und Mailvorlagen

1. Als Testkonto Sprache wählen, abmelden und in neuer Sitzung erneut anmelden.
2. Gespeicherte Benutzersprache und tatsächlich geladene Oberflächensprache vergleichen.
3. Je einen Verifikations-/Resetablauf mit deutschem und englischem Testkonto auslösen;
   ggf. zusätzliche Konten verwenden. Mailinhalt inklusive Footer und Link prüfen.

**Erwartet:** Dokumentierte Sprachauswahl/Persistenz funktioniert. Beide Mailvorlagen
vollständig und ohne Rohplatzhalter. Die konkrete Sprache der Mail anhand des
gespeicherten Kontos/aktuellen Vertrags bewerten, nicht allein anhand des Browserfensters.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Konto-Sprache / Browser-Sprache / Mail-Sprache / Beleg:** …

### UI-01 · Desktop, Handy und andere Fensterbreiten

1. Wichtige Seiten auf breitem Desktop, schmalem Browserfenster und echtem Handy öffnen.
2. Registrierung, Login, Reset, Adminformulare und Dialoge benutzen; DE und EN prüfen.
3. Handy hoch/quer drehen und Bildschirmtastatur öffnen. Scrollbarkeit,
   Sichtbarkeit des Speichern-Buttons und Feldfehler prüfen.
4. Hintergrundbilder, Icons/Logo, Tabellen, Menüs und Notifications ansehen.

**Erwartet:** Keine unbeabsichtigten Überlagerungen oder unzugänglichen Aktionen;
Formulare bedienbar. Tabellen dürfen bewusst horizontal scrollen. Bildzuschnitt
allein ist kein Funktionsfehler; verdeckte Inhalte oder unlesbare Texte schon.
Die geplante neue Gestaltung ist noch kein Sollzustand dieser Prüfung.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Gerät / Größe / Seite / Beleg:** …

### UI-02 · Tastatur und grundlegende Bedienbarkeit

1. Ohne Maus durch Login/Registrierung und ein Adminformular mit Tab/Shift+Tab navigieren.
2. Fokus, Feldbeschriftungen, Checkboxen und vorhandene Enter-/Escape-Aktionen prüfen.
3. Browserzoom erhöhen, z. B. auf 200 %, und Kernaktionen wiederholen.

**Erwartet:** Sichtbarer Fokus und sinnvolle Reihenfolge; wesentliche Aktionen und
Fehlerhinweise erreichbar/lesbar. Enter löst nicht unbeabsichtigt eine andere
Aktion aus. Dies ist eine grundlegende Bedienprüfung, kein vollständiges Accessibility-Audit.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Seite / Eingabe / Beleg:** …

## 15. Erweiterte Betriebsprüfungen

Diese Fälle separat vorbereiten und ihren Umfang im Kopf nennen. Es gibt keine
automatisch vorausgesetzte Queue-/Recovery-Oberfläche. Ohne vorhandene Testwerkzeuge
oder kontrollierte Fixtures den Fall als `BLOCKIERT` bzw. `NICHT AUSGEFÜHRT` erfassen.
Keine Produktionsdaten, blindes Zurücksetzen von Claims oder Umstellen der Systemuhr.

### OPS-01 · Unklarer Transportausgang bleibt quarantänisiert

1. In einer isolierten Umgebung mit geeigneter SMTP-Testfixture einen Fehler
   **nach möglicher SMTP-Annahme** erzeugen; Fehlertyp und Annahmebelege erfassen.
2. Betroffene Queue-ID und Zustand lesen; weitere Schedulerläufe beobachten.
3. Anwendung kontrolliert neu starten und nochmals beobachten.

**Erwartet:** Unklarer Ausgang bleibt `IN_PROGRESS`; keine automatische erneute
Freigabe durch Timeout, Alter oder Neustart. Fehlende Mail im Postfach ist kein
Nachweis des Nichtversands. SMTP-/DB-Lücke und fehlende Exactly-once-Garantie bleiben
als Betriebsgrenze dokumentiert, auch bei grünem Test.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Fixture / Queue-ID / SMTP-Beleg / Belegablage:** …

### OPS-02 · Aufbewahrung und Bereinigung

1. Mit einer gesonderten Testfixture passende Einträge vor/an/nach der Zeitgrenze
   vorbereiten; Fixture und ursprüngliche Zustände dokumentieren.
2. Bereinigungsaufgaben über vorhandenen Testweg ausführen oder tatsächlichen
   Zeitplan abwarten. Im aktuellen Stand laufen beide Queuebereinigungen sonntags
   um 03:00 Uhr nach der Serverzeitbasis; ein gewöhnlicher Kurztest beweist sie nicht.
3. SENT-, FAILED-, PENDING- und IN_PROGRESS-Zustände sowie Adminaudit vergleichen.
4. Tokenbereinigung gesondert mit alter/junger Tokenfixture prüfen; nicht aus dem
   bestandenen Queuecleanup ableiten.

**Erwartet:** SENT nur mit `createdAt` strikt vor eingestellter Aufbewahrungsgrenze
entfernt; maßgeblich ist die Erstellung, nicht der spätere Versandzeitpunkt.
FAILED nur mit bekanntem `lastRetryAt` strikt älter als 30 lokale Tage entfernt.
Gleichheit bleibt erhalten; fehlende FAILED-Zeit, PENDING und IN_PROGRESS bleiben
erhalten. Adminaudit bleibt unabhängig bestehen. Tokencleanup beachtet den jeweiligen
Tokenvertrag und erhält jüngere Tokens. Grenzpräzision zusätzlich automatisiert prüfen.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Fixture / Auslöseweg / Zeiten / Beleg je Bereinigung:** …

### OPS-03 · Manuellen Recovery-Vertrag nachvollziehen

1. Den bestehenden Vertrag in `docs/email-queue-processing.md` mit einem isolierten
   Testfall durchgehen: betroffene ID und Versandfenster bestimmen, alle Worker
   sicher stoppen, SMTP-Belege zuordnen und Tokenzustand prüfen.
2. Bei unklarem Ausgang **keine** Wiederfreigabe durchführen; Entscheidung dokumentieren.
3. Eine tatsächliche Wiederfreigabe nur als separat geplanten Test bei positiv
   nachgewiesenem Nichtversand und sicher beendeten Workern durchführen, gemäß
   dem dort beschriebenen Lock-/Transaktionsvertrag. Kein pauschales Update aller Claims.

**Erwartet:** Keine Wiederholung bei nachgewiesener SMTP-Annahme oder unklarem
Ausgang. Eine erlaubte Wiederfreigabe umgeht keine Kontosperre, Token-Endzustände,
Retryhistorie oder Fälligkeit. Reine Durchsicht ausdrücklich als solche kennzeichnen;
sie beweist keinen praktisch ausgeführten Recoverylauf.

- [ ] Bestanden
- **Status:** OFFEN
- **Prüfart:** Dokumentdurchsicht / tatsächlich ausgeführter isolierter Test: …
- **Ist / Grund / Entscheidung / Beleg:** …

### OPS-04 · Anonyme Resetantwort bei technischem Fehler

1. In isolierter Umgebung mit einer passenden kontrollierten Fehlerfixture einen
   Fehler bei Konto-/Token-/Queueverarbeitung erzeugen; keinen fremden Dienst stören.
2. Syntaktisch gültige anonyme Resetanfrage ausführen und mit RESET-02 vergleichen.
3. Tokengebundene Validierung/Speicherung mit technischem Fehler separat prüfen.

**Erwartet:** Anonyme Anfrage behält gleiche allgemeine Bestätigung ohne
Kontoinformationen; tokengebundener technischer Fehler zeigt neutralen Hinweis und
erlaubt keinen ungesicherten Reset. Details nur im Log. Ein SMTP-Ausfall allein
erzeugt nicht zwingend einen Fehler in der zuvor abgeschlossenen Queueanlage.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Fixture / betroffene Stelle / Beleg:** …

### OPS-05 · Gebautes JAR und Docker, wenn vorbereitet

1. Gewählte Startvariante, Build-Commit, JAR bzw. Image-Kennung und Konfiguration erfassen.
2. Mit eigener Testdatenbank und SMTP-Testserver starten; Start- und Kernabläufe
   START-01, REG-01, VER-01, RESET-01, MAIL-01, SEC-01 und UI-01 wiederholen.
3. Styles, Icons, Mail-Logo und Vorlagen ohne lokale `src/`-Dateien prüfen.
4. Bei Docker erreichbare Link-URLs, Speicherpersistenz und Verhalten nach
   Containerneustart prüfen. Docker ist im Plan; kein vorhandenes Setup voraussetzen.

**Erwartet:** Geprüfte Funktionen und Ressourcen funktionieren in dieser Startart;
Links erreichbar, bewusst persistente Daten nach Neustart vorhanden. Ein erfolgreicher
IntelliJ-Test allein bestätigt diesen Fall nicht. Startarten separat protokollieren.

- [ ] Bestanden
- **Status:** OFFEN
- **Ist / Grund / Startart / Build-/Image-Kennung / Beleg:** …

## 16. Fehlerprotokoll und Nachprüfung

Für jede eigenständige Auffälligkeit den folgenden Block kopieren. IDs wie `MT-F001`
innerhalb des Durchlaufs eindeutig halten; Test-IDs bleiben über Durchläufe stabil.
Belege mit relativen Links eintragen, damit sie zusammen mit dem Protokoll archiviert
werden können. Keine vollständigen Tokenlinks oder Zugangsdaten in Screenshots/Logs.

### MT-F001 · Kurztitel

| Feld | Eintrag |
| --- | --- |
| Betroffene Test-ID(s) | … |
| Einordnung | bestätigter Fehler / Umgebungsproblem / Klärungsbedarf / Verbesserungsvorschlag |
| Gewicht | blockiert Test / Sicherheits- oder Datenproblem / Funktionsfehler / Darstellung |
| Zeitpunkt mit Zeitzone | … |
| Git-Commit / lokale Änderungen | … |
| Konto-Kürzel / Rolle / Sprache | … |
| Browser / Gerät / Startart | … |
| Ausgangszustand und relevante Konfiguration | … |
| Erwartetes Ergebnis | … |
| Tatsächliches Ergebnis | … |
| Häufigkeit | immer / gelegentlich / bisher einmal; Versuche: … |
| Belege | relativer Screenshot-/Loglink, bereinigt: … |
| Früherer Lauf mit erfolgreichem gleichen Fall | … / unbekannt |
| Issue / OpenCode-Auftrag / Fix-Commit | … |
| Nachtest | Verweis auf neuen Durchlauf und Test-ID; Ergebnis: … |

**Reproduktionsschritte:**

1. …
2. …
3. …

**Abweichungen seit dem letzten erfolgreichen Lauf:** …

**Noch ungeklärt / nächster Schritt:** …

Eine frühere grüne Prüfung und ein späterer Fehler grenzen den Zeitraum ein, beweisen
aber nicht automatisch den verursachenden Commit. Auch Browser, Konfiguration,
Testdaten, SMTP und Startart müssen vergleichbar sein. Einen Fix mit neuem Commit
in einem neuen Protokoll bestätigen und hier nur den Verweis ergänzen.

## 17. Abschluss

### Ergebnisübersicht

| Bereich | Ergebnis / offene Test-IDs / Fehler-IDs |
| --- | --- |
| Start und Erstkonfiguration | … |
| Registrierung und Verifikation | … |
| Anmeldung und Abmeldung | … |
| Passwortreset | … |
| Benutzerverwaltung | … |
| Rollen und laufende Sitzungen | … |
| Status und Push | … |
| Admin-Konfiguration | … |
| SMTP und Queue | … |
| DE/EN und Darstellung | … |
| Erweiterte Betriebsprüfungen | … |

| Zählung | Anzahl |
| --- | --- |
| BESTANDEN | … |
| FEHLER | … |
| BLOCKIERT | … |
| NICHT AUSGEFÜHRT | … |
| NICHT ZUTREFFEND | … |
| Noch OFFEN | … |

- **Gesamtergebnis:** OFFEN / BESTANDEN IM GENANNTEN UMFANG / MIT FEHLERN / UNVOLLSTÄNDIG
- **Tatsächlich geprüfter Umfang:** …
- **Wesentliche ungeprüfte Grenzen:** …
- **Offene Fehler und empfohlene nächste Schritte:** …
- **Nächster Nachtest / Verweis:** …

- [ ] Testwerte und zeitweise verkürzte Fristen wiederhergestellt bzw. Abweichungen dokumentiert.
- [ ] Benötigte Adminzugänge/Rollen für Folgetests wiederhergestellt.
- [ ] SMTP-Testserver wieder im vorgesehenen Zustand; offene Claims/fehlgeschlagene Mails notiert.
- [ ] Test-Endzeit, Commit und ggf. Änderungen während des Durchlaufs dokumentiert.
- [ ] Belege geprüft und um Geheimnisse/Tokenlinks bereinigt.
- [ ] Protokoll archiviert; Vorlage blieb unverändert.

`BESTANDEN IM GENANNTEN UMFANG` nur vergeben, wenn die gewählten Prüfungen vollständig
und ohne offene Fehler bestanden sind. Fehlende Voraussetzungen nicht durch eine
grüne Zusammenfassung verdecken. Dieses Protokoll belegt beobachtete Abläufe zu einem
bestimmten Stand, keine vollständige Fehlerfreiheit oder automatische Deploymentfreigabe.

## 18. Automatisierte Teilfälle

Dieser Abschnitt dient ausschließlich als Übergangscheckliste. Die vorhandenen
Prüfaufträge werden schrittweise durch eigenständige Browserfälle ersetzt.
Es gibt keine gegenseitigen Dokumentationsverweise oder technische Abhängigkeit.

`AUTOMATISIERT` erst abhaken, wenn alle geeigneten Varianten des bisherigen Punktes
abgedeckt sind. `TEILWEISE` bedeutet, dass noch Varianten fehlen. Technische Fälle
gehören in JUnit-/Persistenztests; erforderliche menschliche Bewertungen werden später
in einer neuen, separaten Anleitung für manuelle Sonderprüfungen gesammelt.

| Vollständig automatisiert | Bisheriger Punkt | Stand | Bereits abgedeckt | Noch offen |
| --- | --- | --- | --- | --- |
| [ ] | START-01 / UI-01 | TEILWEISE | Startseite und Bilder bei Desktopgröße | Weitere Erststartprüfungen und Darstellung auf anderen Bildschirmgrößen |
| [ ] | AUTH-01 | TEILWEISE | Formular, Admin und aktiver normaler Benutzer, unverifiziertes Konto, falsches Passwort, unbekannte Adresse, anonymer Zugriff | Gesperrte Konten |
| [ ] | AUTH-02 | TEILWEISE | Abmeldung mit Admin, bestehender Tab, Browser-Zurück und erneuter Aufruf | Alte mutierende Formulare und weitere Kontovarianten |
| [ ] | SEC-01 | TEILWEISE | Gast, vorhandener Admin und normale Benutzerrolle | Nachträgliche Rollenvergabe |
| [ ] | START-03 | TEILWEISE | Mailpit-API erreichbar und tatsächliche Verifikationsmail eingegangen | Weitere Versandarten sowie Erststart-/Neustartvergleich |
| [ ] | REG-01 | TEILWEISE | Gültige Registrierung, Bestätigung, tatsächliche Testmail und gesperrte Anmeldung vor Aktivierung | Vollständige Adminansicht des angelegten Kontos |
| [ ] | REG-02 | TEILWEISE | Leere Pflichtfelder, ungültige E-Mail, abweichende Passwortbestätigung und erfolgreiche Korrektur | Blanke Namen, weitere Passwort-/Bestätigungsvarianten, heutige/zukünftige Geburtstage |
| [ ] | VER-01 | TEILWEISE | Empfänger, Betreff, Name, Service, Platzhalter, echter Mailbutton, Aktivierung, Anmeldung und erneute Linkverwendung | Weitere Sprach-/Footer- und Adminsperrenvarianten |
| [ ] | VER-02 | TEILWEISE | Unbekannter Token und tokenloser Aufruf zur tatsächlichen Startseite geprüft | Fehlerhafte Eingaben im Mailformular fehlen |
| [ ] | Alle weiteren bisherigen Prüfpunkte | OFFEN | Noch keine vollständige Übernahme in Browserabläufe | Vorhandene Varianten nach und nach zuordnen und umsetzen |

Die historischen Test-Häkchen und Statuswerte der Abschnitte 1–17 sowie archivierte
Durchläufe werden dadurch nicht verändert. Diese Übergangscheckliste beschreibt
den Automatisierungsfortschritt, keine Freigabe der Anwendung.
