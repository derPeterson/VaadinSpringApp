# Mitteilungsfunktion: Machbarkeit und Umsetzungsplan

Stand: 18.10.2026. Geprüfte Codebasis: `main`, `7dedccc`.
Ursprüngliche Planung vor der anschließenden Umsetzungsfreigabe. Die beschriebenen
Ausgangsbefunde und Klassennamen sind daher als damalige Analyse/Vorschläge zu lesen.
Der erste Umfang ist inzwischen auf `feature/user-messages` umgesetzt; der
[aktuelle Funktions- und Prüfvertrag](user-messages.md) beschreibt gewählte Standardwerte,
tatsächliche Paketaufteilung und noch offene Browser-/Betriebsprüfungen.

## 1. Ziel und Machbarkeit

Die Funktion passt zur bestehenden Vaadin-/Spring-/JPA-Anwendung. Kein externer
Messagingdienst und kein neuer Frontendstack sind für den ersten Umfang erforderlich.
Dauerhafte Mitteilungen bilden einen persönlichen Posteingang hinter der vorhandenen
Glocke. Sie bleiben bei Reload, Abmeldung, Offlinezeit und Anwendungsneustart erhalten.

Drei vorhandene bzw. geplante Kanäle haben unterschiedliche Aufgaben:

| Kanal | Zweck |
| --- | --- |
| Bestehender `NotificationHelper` | Kurze UI-Rückmeldung auf die gerade ausgeführte Aktion, einschließlich Validierungsfehlern |
| Neue Mitteilungen | Gespeicherte Information für ein Benutzerkonto mit eigenem Lesestatus |
| Bestehende E-Mails | Externe Bestätigung und sicherheitsrelevante Information auch ohne Zugriff auf die Anwendung |

Eine Passwortänderung darf weiterhin eine unmittelbare Erfolgsnotification anzeigen,
eine gespeicherte Mitteilung erzeugen und die bestehende Sicherheitsmail einreihen.
Die Inbox ersetzt keine bestehenden Sicherheitsmails oder Verifikationslinks. Der neue
SMTP-Workerpool ist für SMTP zuständig; Inboxeinträge werden nicht darüber zugestellt.
Kein Chat, keine Antworten zwischen Benutzern und keine Browser-/Betriebssystem-Pushmeldungen
im ersten Umfang. Vaadin-Server-Push aktualisiert nur eine bereits geöffnete Anwendung.

## 2. Tatsächliche Anschlussstellen

- `ui/components/AccountHeaderActions`: gemeinsame Glocke in Home, Konto und Admin;
  derzeit Platzhalter ohne Klickfunktion. Anonym sind die Konto-Schnelleinstiege verborgen.
- `views/AdminView`: vorhandene SideNav und URL-Wiederherstellung über `section`.
  `AdminSection` ist aktuell ein privates Enum in dieser View, kein vorhandener Shared-Typ.
- `Application`: `@Push` ist bereits aktiviert. `websocket/UserProfileBroadcaster`
  liefert ein vorhandenes Muster für Aktualisierungen nach dem Datenbankcommit;
  registrierte UI-Listener müssen bei Detach wieder entfernt werden.
- `service/AccountSecurityService`: Passwortwechsel, E-Mail-Anfrage und bestätigte
  Adressänderung besitzen klar abgegrenzte Transaktionen und bestehende Mail-Outboxeinträge.
- `service/PasswordResetService`: erfolgreich verbrauchter Reset ist ein weiterer
  fachlicher Einstiegspunkt. Nicht die bloße Anforderung eines Resetlinks verwenden.
- `service/UserService`: tatsächlich geänderte Rollen/Freigaben können später gezielt
  angebunden werden. Der reine Passwort-Hash-Setter ist kein erfolgreicher Fachabschluss.
- `security/SessionManagementService`: spätere Sitzungsereignisse haben eigene,
  teilweise getrennte Transaktions-/Widerrufsgrenzen und brauchen eine gesonderte Prüfung.

`AdminView` ist auch für normale Administratoren zugänglich. Superadmin gewährt
zusätzlich Adminrechte, die Umkehrung gilt nicht. Die neue Versandmaske braucht daher
zusätzlichen Superadminschutz. Ein globaler Method-Security-Schalter ist im geprüften
Anwendungscode nicht vorhanden; allein hinzugefügte `@PreAuthorize`-Annotationen wären
kein vollständiger Schutzplan.

## 3. Empfohlener erster Funktionsumfang

### Empfängeransicht

- Glocke mit Anzahl ungelesener Mitteilungen, bei großen Zahlen beispielsweise `99+`.
- Ein natives Vaadin-Popover zeigt die neuesten Einträge; eine eigene Übersicht bietet
  paginierte Historie und Filter für alle/ungelesene Mitteilungen.
- Titel, Text, Zeitpunkt, erkennbare Herkunft und gegebenenfalls eine interne Aktion.
- Gelesen wird ein Eintrag durch bewusstes Öffnen seines Inhalts; das bloße Öffnen
  der Glocke markiert nicht die gesamte Inbox als gelesen.
- Zusätzlich einzelne Einträge und alle zum Aufruf erfassten ungelesenen Einträge
  als gelesen markieren. Danach neu eingetroffene Mitteilungen bleiben ungelesen.
- Lesestatus ist kontoweit und wird zwischen Tabs/Geräten synchronisiert.
- Leerer Zustand sowie Lade-/Fehlerzustand. Nach Offlinezeit/Neustart wird aus der DB gelesen.
- Erste Fassung ohne Antworten, Anhänge, freie HTML-Inhalte, Ton oder automatische Toastflut.
  Individuelles Ausblenden/Archivieren kann später als eigene Funktion hinzukommen.

### Superadminmaske

Neuer Bereich **Mitteilungen** unter beispielsweise `/admin?section=messages`.
Einzeln bzw. mehrfach auswählbare Benutzer oder **alle aktiven Konten**, Titel,
Nachrichtentext und eine Vorschau vor dem Versand. Die Oberfläche erhält vorhandene
Cosmos-Cards, Buttons, Footer und Tokens; keine neue Palette oder globale CSS-Änderung.
Konkrete Schriftgrößen, Abstände und Borders werden später gemeinsam abgestimmt.

Vorgeschlagene Regeln:

- Nur Superadmin darf manuell senden; normale Admins und andere Rollen nicht.
- „Alle“ meint die zum Sendevorgang festgehaltenen aktivierten Bestandskonten,
  einschließlich Offlinekonten und des Absenders. Aktivierte Gastkonten sind eingeschlossen.
  Noch unbestätigte Registrierungen und gesperrte Konten sind nicht eingeschlossen.
- Neue Konten erhalten frühere Rundmitteilungen nicht automatisch. Der manuelle
  Verfügbarkeitsstatus „Offline“ ist keine Kontosperre und kein Ausschlusskriterium.
- Empfängerwahl serverseitig neu prüfen. Weicht die Menge von der bestätigten Vorschau
  ab, erneut bestätigen statt still an zusätzliche oder andere Konten zu senden.
- Doppelklick und Wiederholung desselben Sendeauftrags erzeugen keine doppelten Einträge.
- Erfolg erst nach Datenbankcommit; die Rückmeldung nennt die tatsächliche Empfängerzahl.
  „Zugestellt“ bedeutet hier gespeichert und zugeordnet, nicht vom Menschen gelesen.
- Als einfache Anfangsgrenzen: Titel bis 120 und Text bis 4000 Zeichen; Pflichttext und
  serverseitige Prüfung. Diese Werte sind Vorschläge, keine bestehenden Projektregeln.
- Ein kompakter Verlauf eigener administrativer Sendungen kann Auftrag, Zeitpunkt,
  Zielauswahl und Empfängerzahl zeigen. Daraus entsteht kein Zugriff auf die persönliche
  Sicherheits-Inbox anderer Benutzer und keine Überwachung individueller Lesebestätigungen.
- Nach Versand keine stille Bearbeitung bereits zugestellter Inhalte. Korrekturen
  erfolgen als neue Mitteilung. Rückruf/geplanter Versand gehören nicht zur ersten Fassung.

## 4. Welche Aktionen sind sinnvoll?

| Auslöser | Empfehlung | Wichtige Grenze |
| --- | --- | --- |
| Manuelle Information durch Superadmin | Erster Umfang | Wartungsankündigungen, organisatorische Informationen, gezielte Hinweise |
| Passwort erfolgreich geändert | Erster automatischer Auslöser | Erst bei erfolgreichem Fachcommit; keine Passwörter/Hashes im Inhalt |
| Passwort erfolgreich zurückgesetzt | Erster Umfang | Nur nach tatsächlicher Änderung; eine Resetanforderung beweist keine Aktion des Kontoinhabers |
| Neue E-Mail-Adresse erfolgreich bestätigt | Erster Umfang | Beim bloßen Antrag noch nicht behaupten, die Adresse sei geändert; wegen Abmeldung nach erneutem Login sichtbar |
| E-Mail-Änderung angefordert | Optional anschließend | Hinweis „Bestätigung ausstehend“; kein Token/Bestätigungslink in der Inbox und keine Umgehung des E-Mail-Nachweises |
| Rollen/Berechtigungen durch Administration geändert | Sinnvoller zweiter Schritt | Nur tatsächliche Änderungen; immer klar als administrative Änderung kennzeichnen |
| Freigabe/Sperre eines Kontos geändert | Zweiter Schritt nach Bedarf | Gesperrte Benutzer können die Inbox nicht öffnen; dafür kein Ersatz externer Kommunikation |
| Andere Sitzungen/gespeicherte Anmeldungen widerrufen | Optionaler zweiter Schritt | Ein Hinweis pro abgeschlossener Aktion, keine Meldung je Tab; keine neue E-Mail allein wegen einer Sitzungsabmeldung |
| Willkommen nach Kontoaktivierung | Optional, niedrige Priorität | Einmalig; keine wiederholte Willkommensserie nach jedem Login |
| Neuer/unbekannter Gerätezugriff | Spätere eigene Sicherheitsfunktion | Erfordert zuvor definierte Geräteidentität und Erkennungsregeln; vorhandene Gerätebezeichnungen reichen nicht als Beweis |
| Profil speichern, Bild wechseln, Status/Abwesenheit, normale An-/Abmeldung | Nicht dauerhaft melden | Vorhandene Sofortrückmeldung genügt; sonst entsteht unnötige Nachrichtenmenge |
| Einzelne fehlgeschlagene Logins, Validierungs-/SMTP-/Technikfehler | Nicht in die normale Inbox schreiben | Verhindert Angreifer-Spam und Offenlegung technischer Details; Logs und vorhandener Mailqueue-Adminpfad behalten ihre Aufgaben |
| Kontolöschung | Keine Nachricht an das gelöschte Konto | Das Konto und seine Empfangseinträge sind danach nicht mehr nutzbar |

## 5. Vorgeschlagenes Datenmodell

Zwei Tabellen trennen gemeinsamen Inhalt und individuellen Lesestatus. Eine Rundmitteilung
wird einmal gespeichert und jedem Empfänger zugeordnet; jeder liest unabhängig.
Auch eine einzelne Systemmitteilung verwendet dieselben Strukturen.

| Neuer Typ | Geplante Daten |
| --- | --- |
| `UserMessageEntity` | ID, Herkunft/Typ, Erstellungszeit als `Instant`, optionaler Absenderbezug, manueller Titel/Text ODER Systemvorlage mit kleinen validierten Ereignisdaten, stabile Vorgangs-ID gegen Duplikate |
| `UserMessageRecipientEntity` | ID, Message-FK, Empfänger-FK auf stabile Benutzer-ID, `readAt` nullable; Unique-Constraint auf Mitteilung und Empfänger |
| `UserMessageType` | Zum Beispiel `ADMIN_MESSAGE`, `PASSWORD_CHANGED`, `PASSWORD_RESET`, `EMAIL_CHANGED`; keine beliebigen vom Browser gewählten Systemtypen |

Für Systemtexte werden Typ und unveränderliche, erlaubte Ereignisdaten gespeichert,
keine Java-Getter-Namen, beliebigen Übersetzungsschlüssel oder beliebigen JSON-Objekte
vom Client. Konkretes schlankes Speicherformat bei Umsetzung festlegen. Wo keine
Zusatzdaten nötig sind, nur Typ und Zeitpunkt speichern. Keine Tokens, Passwörter,
Hashes, Session-/Remember-me-Handles oder umfangreichen Geräte/IP-Datensätze.

Indizes für Empfänger/Lesestatus und paginierte Abfrage nach Erstellungszeit plus ID.
Keine komplette `UserEntity`-Sammlung mit allen historischen Nachrichten laden.
Die Zuordnung über Benutzer-ID bleibt bei einer E-Mail-Adressänderung erhalten.

Bei Benutzerlöschung dessen Empfangseinträge entfernen. Das Löschen eines administrativen
Absenders darf keine Rundmitteilungen anderer Empfänger kaskadierend löschen:
Absender-FK gegebenenfalls auf null setzen, Anzeige weiterhin „Administration“.
Verwaiste Inhalte ohne Empfänger können gezielt bereinigt werden. Die neue Funktion
ist kein unveränderliches Sicherheits-Auditarchiv.

## 6. Service- und Transaktionsregeln

1. **Vertrauenswürdige Erzeugung:** Fachservices rufen den internen `UserMessageService`
   auf. Separater administrativer Versand prüft den aktuellen Superadmin serverseitig.
   Ein Benutzer kann weder Absender noch Quelle „System“ oder Empfänger-ID einer Inboxabfrage bestimmen.
2. **Gemeinsamer Commit:** Bei den ersten SQL-basierten Sicherheitsaktionen werden
   Fachänderung, Nachricht und Empfangseintrag in derselben bestehenden Transaktion
   gespeichert. Keine eigene unabhängige Transaktion für die Nachricht. Sonst wäre
   eine Erfolgsmeldung trotz fachlichem Rollback möglich.
3. **Fehlerpolitik:** Schlägt das Speichern einer verpflichtenden Sicherheitsmitteilung
   fehl, rollt auch die Fachänderung zurück. Das folgt dem bisherigen Muster der
   mitgespeicherten Sicherheitsmail-Outbox. Dies ist ein bewusst zu bestätigender
   Vorschlag. Soll die Fachaktion davon unabhängig bleiben, ist stattdessen ein
   dauerhafter Ereignis-/Outboxschritt mit Wiederverarbeitung nötig, kein stilles Ignorieren.
4. **Duplikatschutz:** Ein manueller Auftrag hat einen stabilen Requestschlüssel.
   Systemmeldungen verwenden die ID eines konkreten Vorgangs, z. B. die einmalige
   Tokenbestätigung bzw. Credentialänderung. Nicht bloß `(Benutzer, Nachrichtentyp)`
   deduplizieren, sonst würden spätere legitime Änderungen verschwinden.
5. **Push erst danach:** Nur nach Commit eine kurze Änderungshinweis-ID an die
   betroffenen aktiven UIs senden. Ein reines After-Commit-Event ist keine dauerhafte
   Nachrichtenzustellung; die gespeicherte DB-Zuordnung ist bereits vorhanden.
6. **Pushfehler:** Keine bereits committierte Fachänderung wegen eines defekten
   UI-Listeners nachträglich als gescheitert melden. Nach Reload/Navigation/erneutem
   Öffnen kommt der richtige Zustand aus der DB. Kein UI-Zugriff unter Datenbanklock.
7. **Sitzungsereignisse:** Wegen vorhandener getrennter Widerrufsgrenzen nicht einfach
   eine Meldung an einen Buttonklick hängen. Vor deren Anbindung muss ein verlässlicher
   Abschluss-/Ereignisvertrag feststehen; deshalb nicht im ersten automatischen Paket.

Spring unterstützt commitgebundene Listener; diese lösen jedoch nicht automatisch
die dauerhafte Speicherung bzw. Wiederverarbeitung von Ereignissen:
[Spring: Transaction-bound Events](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html).

## 7. Berechtigungen und Live-Aktualisierung

- Normale Benutzer, aktivierte Gäste, Admin und Superadmin lesen nur ihre eigene Inbox.
  Auch Superadmin erhält dadurch keinen allgemeinen Lesezugriff auf fremde Mitteilungen.
- Jede Listen-, Zähler-, Detail- und Gelesen-Operation prüft die serverseitige Identität;
  Datenbankoperationen verwenden Empfänger-ID und Mitteilungs-ID gemeinsam als Bedingung.
- Anonyme, gesperrte und nur eingeschränkt zur Sitzungsverwaltung zugelassene Zugriffe
  bekommen keinen Inboxzugang. Die bestehenden Wartungsregeln werden nicht umgangen.
- Neue Adminmaske nur für Superadmin aufbauen/anzeigen; Direktlink und Backend-Versand
  ebenfalls prüfen. Rollenänderungen während einer geöffneten Maske müssen wirksam sein.
  Ein manipulierter Client darf die Berechtigung nicht über UI-Komponenten umgehen.
- Method Security wäre möglich, müsste aber ausdrücklich aktiviert und gegen die
  tatsächlich eingesetzten Versionen geprüft werden. Für den ersten Umfang ist ein
  gezielter frischer Service-Guard passend zur bestehenden Architektur ausreichend.
  [Spring Security: Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- Eigener `UserMessageBroadcaster` mit zielbezogenen Registrierungen. In Events keine
  Inhalte oder vollständigen Empfängerlisten an alle UIs senden.
- UI bei Attach registrieren, bei Detach abmelden; kurze Updates über `UI.access` bzw.
  die passende Vaadin-25-API, ohne aufrufenden Request oder Benutzerkontext eines anderen
  Threads zu übernehmen. Die Gültigkeit der Zielanmeldung ist auch im Pushpfad zu prüfen.
  [Vaadin: UI-Updates per Server Push](https://vaadin.com/docs/latest/building-apps/server-push/updates)
- Lesestatusänderungen aktualisieren alle aktuell verbundenen Tabs/Sitzungen dieses Kontos.
- Erste Version: unmittelbare Pushhinweise innerhalb einer laufenden Anwendungsinstanz.
  Persistenz funktioniert über Neustarts; ein verlorener Hinweis verliert keine Nachricht.
  Instanzübergreifendes sofortiges Push benötigt später einen Broker oder abgestimmtes
  Nachladen/Polling. Das vorhandene lokale Broadcastermuster leistet dies nicht automatisch.

## 8. Sprache, Inhalt und Darstellung

Automatische Vorlagen über den gemeinsamen typisierten `MessageTexts`-Katalog DE/EN
abbilden und beim Anzeigen in der gewählten UI-Sprache rendern. Gespeicherte Typen und
Ereignissnapshots bleiben stabil; Datum/Uhrzeit werden für die Anzeige lokalisiert.
Bei späteren Templateänderungen deshalb historische Aussagen nicht umdeuten; wichtige
Vorlagen bei Bedarf versionieren.

Manueller Freitext bleibt genau der vom Superadmin verfasste Text. Die App übersetzt ihn
nicht automatisch. Empfehlung für den ersten Umfang: ein Titel/Text für alle Empfänger;
zweisprachige manuelle Varianten nur bei ausdrücklichem Bedarf ergänzen. Bedienoberfläche
und automatische Mitteilungen sind unabhängig davon in DE/EN verfügbar.

Texte als Text rendern, keine freie HTML-Auswertung über den bestehenden Toast-Helfer.
Interne Aktionen aus erlaubten Typen ableiten, z. B. Konto/Sicherheit; keine frei
hinterlegbaren externen URLs und keine Verifikations-Tokens in gespeicherten Links.
Ein Link umgeht keine Zielseitenberechtigung und bestätigt keine E-Mail-Adresse.

Bestehende Cosmos-Komponenten und Tokens wiederverwenden. Glockenfläche und Icons nicht
nebenbei umskalieren. Popover mit Escape/Außenklick, verständlichen Namen, Tastaturfokus
und sinnvoller schmaler Darstellung. Detailgestaltung kommt gemeinsam nach der Funktion.

## 9. Typische zusätzliche Entscheidungen

| Thema | Empfehlung / noch festzulegen |
| --- | --- |
| Aufbewahrung | Vor Einführung verbindlich festlegen; Vorschlag 180 Tage für gelesene und ungelesene Inhalte, danach transparente Bereinigung. Kein unbegrenztes Archiv. Erst nach Freigabe als wirklich verwendete Konfiguration ergänzen |
| Gelesen vs. Bestätigung | Gelesen ist UI-Status, kein Beweis menschlicher Kenntnisnahme. Verpflichtende Bestätigung ist eine eigene spätere Funktion |
| Ablauf einer Ankündigung | Optional später; kann eine Nachricht als veraltet markieren, ohne Historie heimlich umzuschreiben |
| Ausblenden/Archivieren | Optional später mit eigenem Zustand; kein fremdes Löschen und keine gemeinsame Löschung einer Rundmitteilung |
| Manuelle Sendungen nachträglich ändern | Im ersten Umfang nicht; Korrektur als neue Mitteilung |
| Kanal-/Kategoriepräferenzen | Optional später; sicherheitsrelevante E-Mails nicht automatisch durch eine stummgeschaltete Inbox ersetzen |
| Massenversand | Kleine aktuelle Benutzerzahl: eine transaktionale Empfängerzuordnung. Bei großer Menge eigener dauerhafter Fan-out-Auftrag mit Fortschritt/Retry; nicht einfach HTTP abbrechen oder den SMTP-Pool dafür verwenden |
| Betrieb | Fehler mit Vorgangs-/Mitteilungs-ID protokollieren, keine Bodies/Tokens. Keine neuen Nutzungs-/Lesetracking-Dashboards ohne Auftrag |
| Datenbankeinführung | Zwei neue Tabellen mit FK/Unique/Indizes; neue und künstliche alte H2-Strukturen prüfen. Vor Bestandsbetrieb Schema/Backupverfahren beachten, keine reale DB im Entwicklungstest benutzen |

## 10. Paketaufteilung im bestehenden Projekt

Alle folgenden Namen sind Vorschläge für neue Typen. Bestehende Pakete beibehalten,
keine zusätzliche Architektur oder generische Benachrichtigungsplattform aufbauen.

| Paket / Ort | Aufgabe |
| --- | --- |
| `model/` | `UserMessageEntity`, `UserMessageRecipientEntity` |
| `model/enums/` | `UserMessageType` und nur tatsächlich benötigte Zustände |
| `repository/` | Zwei Repositories: Inhalte und empfängergebundene Listen/Zähler/Read-Updates; Indizes/Constraints |
| `service/` | `UserMessageService` für interne Erzeugung, `UserMessageInboxService` für eigene Inbox, `AdminUserMessageService` für autorisierten manuellen Versand; immutable Item-/Snapshot-DTOs wie bei vorhandenen Services |
| `security/` | Kleiner gemeinsamer Zugriffs-Guard für aktuelle Identität und expliziten Superadmin; bestehende Authentifizierung wiederverwenden |
| `i18n/` | `UserMessageTextRenderer` bildet erlaubte Systemtypen auf typisierte Übersetzungsgetter ab |
| `websocket/` | `UserMessageBroadcaster` für zielbezogene Hinweise nach Commit und Lesestatusänderungen |
| `ui/components/` | Glocken-/Popoverkomponente; Anschluss an `AccountHeaderActions` und bestehende gemeinsame Kopfzeile |
| `views/` | `UserMessagesView` für die eigene paginierte Historie; Routenvertrag in bestehenden Konstanten ergänzen |
| `views/admin/` | `AdminUserMessagesSection`; Einbindung und Superadmin-URL-Schutz in `AdminView` |
| `scheduler/` | Gezielte Aufbewahrungsbereinigung nach abgestimmter Regel |
| `shared/…/i18n/MessageTexts` und Ressourcen | Neue Getter sowie DE-/EN-Texte; Shared bleibt unabhängig von Spring/Vaadin |
| `src/test/java/` | Rechte, Persistenz, Transaktionen, Duplikate, Zeitgrenzen, konkurrierender Lesestatus und gelöschte Benutzer |
| `browser-tests/` | Später ausdrücklich freigegebene DE-/EN-Journeys, bestehende Page-Helper und Katalog erweitern |
| `docs/` | Nach Umsetzung ein verbindlicher Mitteilungsvertrag und tatsächlich ausgeführte Nachweise |

## 11. Sinnvolle Umsetzungsschritte und Aufwand

1. Produktregeln bestätigen: Zielgruppe „alle“, Lesestatus, manuelle Sprache,
   Aufbewahrung und Fehlerpolitik bei automatischen Sicherheitsmitteilungen.
2. Datenmodell, eigene Inbox und serverseitige Rechte mit isolierten JUnit-/H2-Tests.
3. Gemeinsame Glocke, Popover, paginierte Übersicht und lokale Push-Synchronisierung.
4. Superadminmaske mit Empfängervorschau, idempotentem Versand und eigenem Verlauf.
5. Passwortwechsel, erfolgreicher Reset und bestätigte E-Mail-Änderung gezielt anbinden.
6. Aufbewahrung, Schemaeinführung, Dokumentation und Regressionen abschließen.
7. Nach separater Freigabe Browserjourneys DE/EN und gemeinsame Designabnahme.

Insgesamt ein mittleres bis größeres Feature, keine reine Icon-Erweiterung.
Der Schwerpunkt liegt auf Rechteprüfung, gemeinsamen Transaktionen, Lesestatus und
Lebenszyklus. Es ist in den genannten Schritten gut umsetzbar; zusätzliche Infrastruktur
ist für die heutige Eininstanz-Anwendung nicht nötig. Kein geschätzter Zeitbedarf angegeben.

## 12. Geplante Prüfungen

- Superadmin darf senden; Admin/Benutzer/Gast/anonym und eingeschränkte Sitzungen nicht.
  Direktlink, gefälschte IDs/Quelle und Rollenentzug bei geöffneter Maske abweisen.
- Jeder sieht/markiert nur eigene Empfangseinträge; Lesestatus anderer Empfänger bleibt gleich.
- Einzel- und Rundmitteilungen, Offlinekonten, aktivierte Gäste, ausgeschlossene Konten,
  stabile Zielmenge und kein nachträglicher Empfang durch neu angelegte Benutzer.
- Wiederholter Auftrag/Doppelklick idempotent; neue legitime Sicherheitsaktion erneut zustellbar.
- Rollback erzeugt keine Mitteilung/Pushmeldung; Nachrichtenspeicherfehler folgt der
  bestätigten Fehlerpolitik. Ausgefallener UI-Listener verliert keine persistierte Nachricht.
- Paralleles Markieren in zwei Tabs ist idempotent; „alle gelesen“ verschluckt keine
  später eintreffenden Mitteilungen. Reihenfolge und Pagination bleiben stabil.
- Passwort-/E-Mail-/Resetfälle nur nach tatsächlichem Erfolg; keine Verifikationsumgehung,
  Token-/Passwortdaten oder irreführenden Erfolge bei abgewiesenen Änderungen.
- Neustart und Wiederanmeldung erhalten Inbox/Lesestatus; E-Mail-Wechsel behält Zuordnung.
  Benutzerlöschung löscht Empfangseinträge, aber keine fremden Rundmitteilungen.
- DE/EN, sichere Textausgabe auch bei HTML-ähnlichem Freitext, interne Links und Zugangsschutz.
- Cleanupgrenzen mit injizierbarer Clock; keine Löschung vor Ablauf, keine Live-DB.
- Später im Browser: Superadmin sendet an A/B, A markiert gelesen, B bleibt ungelesen,
  Tab-/Gerätesynchronisierung, anonymer Zugang, Superadmin-URL, schmale Ansichten und Fokus.

## 13. Prüfstatus dieses Dokuments

Quellcode-/Dokumentationsanalyse und Abgleich der grundlegenden Frameworkmechanismen
mit den oben verlinkten offiziellen Dokumentationen. Keine Implementierung, Builds,
JUnit-/Browserläufe oder externen Sendungen ausgeführt. Die konkreten APIs und
Schemaoperationen sind vor Umsetzung gegen die dann tatsächlich aufgelösten
Projektversionen zu prüfen. Die vorhandenen Testergebnisse gelten nicht als Nachweis
einer bereits vorhandenen Mitteilungsfunktion.
