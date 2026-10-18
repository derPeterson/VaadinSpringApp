# Persönliche Mitteilungen

Stand: 18.10.2026, Feature-Branch `feature/user-messages`, auf Grundlage von `7dedccc`.
Der erste Umfang ist implementiert. Browserabnahme und Produktionsbuild stehen noch aus.
Die zuvor abgestimmte [Planung](mitteilungsfunktion-plan.md) bleibt als ursprüngliche
Entscheidungsgrundlage erhalten; dieses Dokument beschreibt den tatsächlichen Vertrag.

## Bedienung

- Angemeldet: Die vorhandene Glocke zeigt die Anzahl ungelesener Mitteilungen,
  oberhalb von 99 als `99+`. Ein Klick öffnet bis zu fünf aktuelle Einträge.
  Anonym bleiben Glocke und Konto-Schnelleinstieg ausgeblendet.
- Alle Mitteilungen anzeigen öffnet `/messages`: 20 Einträge pro Seite, neueste zuerst,
  Filter für ungelesene Einträge, Aktualisieren und Vor-/Zurückblättern.
- Das Öffnen der Glocke markiert nichts als gelesen. Erst das Öffnen eines Eintrags
  markiert diesen. „Alle als gelesen markieren“ verwendet die zuletzt geladene
  Liste der eigenen ungelesenen Empfangs-IDs; später eingetroffene Einträge bleiben ungelesen.
- Detaildialog: Escape, Außenklick, Schließen-Icon und Footer-Schließen. Systemeinträge
  bieten ausschließlich den internen Einstieg in Konto/Sicherheit an.
- Lesestatus gehört zum Konto, unabhängig vom verwendeten Tab oder Gerät.
  Neue Mitteilungen und Leseänderungen lösen nach Commit lokale Push-Aktualisierungen aus.

## Superadmin-Versand

Nur mit ausdrücklich zugewiesener Rolle **Superadmin** erscheint Administration →
Mitteilungen (`/admin?section=messages`). Normale Administratoren erhalten diesen Versandzugang
nicht. Eine unberechtigte direkte Bereichs-URL zeigt das Dashboard; die Serviceoperationen
weisen unberechtigte Aufrufe zusätzlich ab.

Einzelne/mehrere Konten oder alle freigeschalteten bestehenden Konten auswählen, Titel
und Nachricht ausfüllen, Vorschau prüfen und danach senden. Titel maximal 120, Text maximal
4000 Java-/Frontend-Zeichen. Beide Felder sind Pflichtfelder. Inhalte werden ausschließlich
als Text dargestellt, einschließlich Zeichenfolgen wie HTML. Manuelle Mitteilungen bleiben
in der vom Verfasser gewählten Sprache. Es werden keine zusätzlichen E-Mails versandt.

„Alle aktiven Konten“ meint freigeschaltet und verifiziert, unabhängig vom Online-Status:
aktivierte Gäste, Offlinekonten und der Absender sind eingeschlossen. Deaktivierte und
noch nicht verifizierte Konten fehlen. Später registrierte Konten erhalten frühere
Rundmitteilungen nicht nachträglich.

Die Vorschau fixiert die Empfänger-IDs. Vor dem Versand wird die berechtigte Menge erneut
aus skalaren Datenbankwerten bestimmt. Änderungen erfordern eine neue Vorschau; eine
nachträgliche Erweiterung passiert nicht stillschweigend. Bei gezielter Auswahl sind nur
Änderungen innerhalb dieser Auswahl relevant. Kontomutationen und manueller Versand
verwenden dieselbe Admin-Rollensperre und Sperrreihenfolge.

Ein UUID-Vorgangsschlüssel bleibt bei einem fehlgeschlagenen/unklar beantworteten
Versuch erhalten. Gleicher Absender, Inhalt, Auswahlart und Empfängersnapshot liefern bei
Wiederholung dieselbe Sendung. Abweichende Wiederverwendung wird abgewiesen. Die DB erzwingt
zusätzlich einen eindeutigen Schlüssel und eindeutige Kombinationen aus Nachricht und Empfänger.
Ein neuer Entwurf ist eine neue Sendung. Die Wiederholungserkennung gilt innerhalb der
Aufbewahrung; bereits gelöschte Empfänger können eine alte Wiederholung zum Konflikt machen.

Eigene gesendete Nachrichten erscheinen mit Versandzeit, Inhalt und ursprünglicher
Empfängeranzahl, seitenweise zu 20 Einträgen. Es gibt keine fremden privaten System-Inboxen,
individuellen Leselisten, nachträgliche Bearbeitung oder Rücknahme der Sendung.

## Automatische Ereignisse und Transaktionen

| Erfolgreiche Fachaktion | Neuer Inbox-Typ | Bestehende Kanäle |
| --- | --- | --- |
| Passwortwechsel im Konto | `PASSWORD_CHANGED` | UI-Rückmeldung und bestehende Sicherheitsmail bleiben erhalten |
| Passwortreset über gültigen Link | `PASSWORD_RESET` | Bestehender Resetablauf bleibt erhalten; keine zusätzliche neue Reset-Bestätigungsmail eingeführt |
| Bestätigte E-Mail-Änderung | `EMAIL_CHANGED` | Bestehende Hinweise an alte und neue Adresse bleiben erhalten |

Eine bloß angeforderte E-Mail-Änderung erzeugt noch keinen Erfolgseintrag. Verwendete,
ungültige, abgelaufene oder erfolglose Aktionen erzeugen keine Erfolgsmitteilung.

`UserMessageService.recordSecurityEvent` verlangt eine bestehende Transaktion. Fachaktion,
Nachricht und Empfangseintrag werden gemeinsam gespeichert. Scheitert die Pflichtmitteilung,
rollt auch die Fachaktion einschließlich ihrer Mail-Outbox-Einträge zurück. Der bisherige
Sitzungswiderruf beim Konto-Passwortwechsel/bei E-Mail-Bestätigung erfolgt weiter nach Commit.
Für den Passwortreset wurde keine zusätzliche Änderung der bisherigen Sitzungsregeln eingeführt.

Systeminhalte speichern nur Typ und Erstellungszeit; Empfänger ist die stabile Benutzer-ID.
Keine Passwörter, Hashes, Tokenlinks, alten/neuen E-Mail-Adressen oder Sitzungs-IDs in den
Mitteilungsinhalten. Vorlagen werden beim Anzeigen über den typisierten DE-/EN-Katalog
übersetzt. Die Benutzer-ID hält den Eintrag auch nach Änderung der Anmeldeadresse beim Konto.

## Zugriff, Datenmodell und Aufbewahrung

`SecurityService.getSessionUser` prüft die konkrete HTTP-Sitzung, Widerruf, aktuellen
Kontostatus und frisch geladene Berechtigungen. Push übernimmt weder Thread-Authentifizierung
noch einen Request-Proxy. Ein eingeschränkter Verwaltungszugang bei erreichtem Sitzungslimit
ist keine vollständige Anmeldung und eröffnet keine Inbox. Wartungsmodus gilt auch hier:
normale Konten pausieren, administrative Rollen bleiben nach bestehenden Regeln zugelassen;
unlesbare Wartungskonfiguration sperrt den Zugriff.

`user_messages` hält unveränderlichen Inhalt, Typ, Zeit, Vorgangsschlüssel und optionalen
Absender. `user_message_recipients` hält Benutzerbezug und `read_at`. Benutzerlöschung entfernt
nur seine Empfangseinträge; Senderlöschung setzt den Senderbezug auf NULL. Gemeinsame Inhalte
anderer Empfänger bleiben erhalten. Index auf Empfänger/Lesestatus/ID sowie Zeitindex und
Unique-Constraints werden durch das JPA-Schema angelegt. Empfangslisten lesen skalare
Projektionen; Leseupdates benötigen kein Leeren fremder Änderungen im Persistence Context.

Standardaufbewahrung: **180 Tage**, gelesen und ungelesen. Der tägliche Job um 03:20 in
der Server-Zeitzone entfernt ältere Inhalte samt Empfangseinträgen sowie Inhalte ohne
Empfänger. Die eigene Versandhistorie ist deshalb kein dauerhaftes Auditarchiv. Gelesen
bedeutet keine nachgewiesene menschliche Kenntnisnahme.

Die tatsächlich verwendete Spring-Betriebseinstellung
`app.user-messages.retention-days` erlaubt positive ganze Tage; Standard 180. Anzeige und
Bereinigung verwenden dieselbe `UserMessagePolicy`. Ungültige nichtpositive Werte verhindern
den Start. Dies ist zunächst kein zusätzliches Feld der Admin-Konfigurationsmaske; dort
bleiben die bisherigen 28 Felder unverändert. Bereinigungsänderungen erscheinen beim nächsten
Laden/Aktualisieren; der Job versendet keine individuellen Livehinweise über entfernte Einträge.

## Aufteilung und Einführung

| Bereich | Implementierung |
| --- | --- |
| Datenmodell / JPA | `UserMessageEntity`, `UserMessageRecipientEntity`, `UserMessageType`, zwei Repositories |
| Fachlogik und Zugriff | `UserMessageService` mit getrennten öffentlichen Operationen für eigene Inbox, geprüften Superadmin-Versand und interne Sicherheitsaktionen; der gemeinsame Guard ist privat |
| Sprache / Push | `UserMessageTexts`, Shared-`MessageTexts`, `UserMessageBroadcaster` |
| Oberfläche | `UserMessageBell`, `UserMessagesPanel`, `UserMessagesView`, `AdminUserMessagesSection`; gemeinsame Header-Anbindung über Spring |
| Aufbewahrung | `UserMessagePolicy`, `UserMessageCleanup` |

Für den kleinen gemeinsamen Persistenzablauf ist ein Service umgesetzt, anstelle der im
Plan vorgeschlagenen drei separaten Serviceklassen. Es gibt keine neue Method-Security-
Infrastruktur, keinen externen Broker und keine Kopplung an den SMTP-Workerpool.

Shared neu bauen/installieren, dann Anwendung mit dem aktuellen Stand bauen und vollständig
neu starten. HotSwap allein registriert die neuen Beans, JPA-Tabellen und Route nicht
zuverlässig. Die vorhandene Hibernate-Einstellung `ddl-auto=update` legt die neuen Tabellen
beim regulären Start an. Es wurde keine bestehende Benutzer-Datenbank geöffnet oder verändert.
Neue Tabellen/FKs sind mit frischem H2 getestet; der Start gegen eine künstliche alte
Struktur und ein echter Prozessneustart sind noch separat zu prüfen.

Persistenz überlebt Neustarts durch die Datenbank; unmittelbare Push-Aktualisierungen gelten
zunächst für eine laufende Anwendungsinstanz. Bei mehreren Instanzen, Verbindungsunterbrechung
oder verlorenem Push liefert erneutes Laden den verbindlichen DB-Stand. Keine Zustellgarantie
an geschlossene Browser, keine Desktop-Benachrichtigungen und kein serverseitiger Broadcast
zwischen Instanzen. Abonnements werden beim Detach abgemeldet.

## Nachweise vom 18.10.2026

- Java 25.0.4.1, Shared erfolgreich neu installiert, Anwendungs- und Testcode kompiliert.
- Vollständiger Anwendungs-JUnit-Lauf: **1613/1613**, 0 Fehler, 0 Fehlschläge, 0 Skips.
  Gegenüber dem Ausgangsstand 58 zusätzliche Fälle; bestehende Sicherheits- und Routingfälle
  um die Inbox erweitert. Neue reine Mitteilungsklassen: 38 Persistenz- und 15 Komponentenfälle.
- Echte isolierte H2-Transaktionen prüfen Empfängergrenzen, Rollenverlust, deaktivierte/
  abgelaufene Sitzungen, Thread-Kontext-Trennung, Wartung, konkurrierende Wiederholung,
  Vorschaukonflikte, kontoweite Leseänderung, Snapshots, Cacheeffekte, Rollbacks, Fremd-IDs,
  Sender-/Empfängerlöschung, Aufbewahrung und defekte Push-Abonnenten.
- Die vorhandenen Passwort-/E-Mail-Integrationstests prüfen zusätzlich die echten Inbox-
  Einträge und simulierte Pflichtmitteilungsfehler mit Rollback der Fachaktion. UI-Tests
  prüfen Textdarstellung, DE/EN, Pagination, Dialogkonfiguration, Glockenzähler, Detach,
  Vorschauvalidierung und gleiche Vorgangs-ID beim Wiederholen. Header-/Admin-Anbindung
  wird über echte Spring-Autowire-Verarbeitung geprüft; HTTP-Filtertests umfassen `/messages`.
- JaCoCo: Zeilen **8410/9871 (85,20 %)**, Zweige **2354/3225 (72,99 %)** über 197 ausführbare
  Anwendungs-Java-Klassen. Die neuen Mitteilungsklassen einschließlich ihrer inneren
  Klassen erreichen **540/654 (82,57 %)** Zeilen und **126/187 (67,38 %)** Zweige.
  Messung aus einem frischen vollständigen Lauf, ohne Shared-/Test-/Bibliothekscode.
- Spotless Apply/Check erfolgreich; 482 eindeutige Schlüssel in DE und EN,
  Sprachparität und bestehender Latin-1-Dateicodec erhalten.

Keine Browserausführung, kein Produktionsbuild, keine CSS-/JavaScript-Abdeckungsmessung,
keine Prüfung mit Screenreader oder realem Gerät. Die bestehende Gesamtsuite wurde nicht
als Nachweis der neuen UI-Journeys ausgegeben. Keine realen Postfächer, Zugangsdaten,
Bestandsdatenbanken oder Nutzerprozesse verwendet. JUnit-Fixtures wurden beendet.

### Noch auszuführende Browser-/Betriebsprüfungen

| Geplanter Fall | Sichtbarer Ablauf / Erwartung | Status |
| --- | --- | --- |
| MSG-01 | Superadmin sendet an zwei über UI angelegte Konten; beide sehen die Nachricht, ein drittes nicht | Browser DE/EN offen |
| MSG-02 | Glocke öffnen lässt ungelesen bestehen; Detail öffnen aktualisiert Zähler und zweiten Tab | Browser DE/EN offen |
| MSG-03 | Alle/ungelesen, Pagination und „alle gelesen“ mit währenddessen eintreffender Nachricht | Browser DE/EN offen |
| MSG-04 | Vorschau, Feldänderung, Doppelversand, deaktivierter Empfänger und direkte Admin-URL mit normalem Admin | Browser DE/EN offen; technische Regeln JUnit geprüft |
| MSG-05 | Passwortwechsel/-reset und E-Mail-Bestätigung über echte UI-/Mailpit-Journeys erzeugen jeweils den eigenen Eintrag | Browser DE/EN offen; Fachtransaktionen JUnit geprüft |
| MSG-06 | Sprachwechsel, lange Texte/HTML-Zeichen, schmale Ansichten, Tastaturfokus, Popover und Detaildialog | Browser DE/EN offen; Text-/Komponentenregeln JUnit geprüft |
| MSG-07 | Inhalt/Lesestatus nach echtem Prozessneustart; künstliches bisheriges H2-Schema erweitern | Eigene isolierte Betriebsfixture offen |

Das sind geplante Prüfungen, keine bereits implementierten Browserfälle oder Testergebnisse.
Designfeinschliff erfolgt nach der ersten Benutzeransicht im bestehenden Cosmos-Theme.
