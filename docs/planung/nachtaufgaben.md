# Gesammelte Entwicklungsaufgaben für die Nacht

Status: **Umsetzung nach ausdrücklichem Startsignal begonnen.**

Der Benutzer hat den vollständigen Nachtlauf ausdrücklich gestartet.
Die Aussage, später schlafen zu gehen, startet keine Arbeiten automatisch.
Die Aufgaben gehören zum bestehenden Branch `codex/vaadin-komponentenanpassungen`.
Vor dem Start die bestätigten Fehler in [BUGS.md](../../BUGS.md) berücksichtigen.
Bestehende offene Arbeiten bleiben in ihren bisherigen Plänen.

## Freigegebener Ablauf nach dem Startsignal

Die folgenden Befugnisse sind für diese Nachtliste erteilt. Sie sind ausdrücklich
**noch kein Startsignal**; bis dahin nur die vereinbarte Planung pflegen.

- Auf dem bestehenden Branch arbeiten und **nach jedem einzelnen Arbeitspaket**
  Review und einen eigenen Commit mit aussagekräftiger deutscher Message
  durchführen. Das gilt auch für Analyseergebnisse und spätere optionale
  Umsetzungen; mehrere Pakete nicht in einem Sammelcommit zusammenfassen.
  Gefundene Fehler im betroffenen Funktionsumfang selbstständig
  beheben und passende Regressionen ergänzen. Kein Merge und kein Push.
- Nach jedem Paket die passenden gezielten Prüfungen ausführen, bei kritischen
  Änderungen am Anmeldeablauf früh einen kurzen isolierten Browsercheck ergänzen.
  Die vollständige Browserprüfung in DE/EN auf Desktop und Smartphone erfolgt
  abschließend mit der großen Testrunde und dem offenen zentralen Testbedarf.
- Eigene isolierte Testanwendungen, Testdatenbanken und Testdienste starten und
  stoppen dürfen. Zusammengehörige Prüfungen können bei sauber getrennten Daten
  dieselbe Umgebung nutzen; kein kompletter Neuaufbau nach jeder Kleinigkeit.
  Builds nacheinander und ohne Imagewechsel während laufender Prüfungen ausführen.
  Automatisierte Regressionen weiterhin mit isolierten Testdaten und Testdiensten
  ausführen; kein echter Mailversand.
- **Auf diese Nachtliste begrenzte Lesbarkeitsausnahme:** Beim Änderungsreview die
  eigenen geänderten Codestellen einschließlich des zum Verständnis nötigen
  Kontexts auf Lesbarkeit prüfen und korrigieren. Keine erneute vollständige
  Lesbarkeitsanalyse oder vorsorgliche Überarbeitung jeder betroffenen Klasse.
  Fachliche Korrektheit, Sicherheit, Aufrufer und Seiteneffekte trotzdem im
  erforderlichen Zusammenhang prüfen. Die allgemeinen Projektregeln in
  `AGENTS.md` und `docs/entwicklung.md` bleiben für andere Aufgaben unverändert.
- Bei unerwarteten Problemen ist die Bedienung des PCs zur Bearbeitung des
  Auftrags erlaubt. Im beauftragten Umfang selbstständig sinnvolle Entscheidungen
  treffen. Zusätzlich ist ausdrücklich erlaubt, die lokale Entwicklungsanwendung
  zu bedienen, zu starten und zu stoppen sowie ihre austauschbaren Daten und
  Sitzungen für nötige Reproduktionen zu untersuchen und zu verändern. Der Benutzer
  beendet sie vor der Nachtarbeit; eine unveränderte Erhaltung ist nicht mehr
  verlangt. Vor einem erforderlichen Datenbankreset den vorhandenen Stand für
  eine mögliche Wiederherstellung sichern. Automatisierte Tests verwenden trotzdem
  eigene isolierte Fixtures. Neue geheime Zugangsdaten oder weitergehende
  Systemfreigaben nicht voraussetzen; konkrete Blocker festhalten und unabhängige
  Aufgaben weiterbearbeiten.
- Selbst getroffene wesentliche Entscheidungen paketbezogen kurz mit Grund und
  betroffenem Commit festhalten und dem Benutzer in der morgendlichen
  Gesamtzusammenfassung mitteilen. Keine technische Rücknahme durch destruktive
  Git-Befehle vorwegnehmen; die getrennten Commits ermöglichen spätere gezielte
  Korrekturen nach Benutzerentscheidung.

### Optionale Umsetzung von Punkt 3 und 4

Beide Punkte beginnen weiterhin als Analyse. Sind die Pflichtpakete samt nötigen
Prüfungen abgesichert und besteht noch Zeit, gilt für diesen Nachtlauf:

- **Vor 10:00 Uhr, Zeitzone Europe/Berlin:** Die Umsetzung darf optional ohne
  erneute Rückfrage beginnen, sofern die Analyse eine klare Lösung im vereinbarten
  Umfang ergibt. Unkomplizierte, reversible Detailentscheidungen, beispielsweise
  eine eindeutige Pfadbezeichnung, darf der Agent selbst treffen und berichten.
- **Ab 10:00 Uhr:** Vor Beginn einer optionalen Umsetzung den Benutzer fragen und
  die Antwort abwarten. Eine ausbleibende Antwort ist keine Freigabe. Bereits
  zulässig begonnene Arbeit sicher abschließen; keine neue optionale Umsetzung
  nach dem Stichtag ohne Zustimmung beginnen.
- Keine Verpflichtung, diese Zusatzumsetzungen noch zu beginnen. Offene
  Grundsatzentscheidungen zu Fachverhalten, Berechtigungen, Speicherformaten oder
  Architektur bleiben beim Benutzer. Eine technisch erforderliche neue eigene
  Ersatzkomponente bleibt ebenfalls erklärungs- und freigabepflichtig. In solchen
  Fällen nur die Analyse mit konkretem Vorschlag liefern.
- Analyse und optionale Umsetzung jeweils getrennt reviewen und committen.
  Umsetzungen passend testen und in die abschließende große Testrunde einbeziehen;
  bereits ausgeführte Prüfungen bei relevanten Änderungen gezielt ergänzen.

## 1. Entwicklungskonten ausdrücklich kennzeichnen

Umgesetzt und mit 287 gezielten Tests geprüft. Das Merkmal ist bei der Anlage
festgelegt und unabhängig von E-Mail-Nachweis und Rollen. Mailabhängige Nachweise
bleiben gesperrt, statt Sicherheitsprüfungen zu umgehen. Die isolierte UI-Prüfung
steht zentral im [Testbedarf](../testbedarf.md) für die abschließende Testrunde.

## 2. Vollständige Aktionslinks in E-Mails zum Kopieren anbieten

Die vorhandenen sechs Vorlagenpaare enthalten gemeinsame DE/EN-Kopierhinweise
und dieselbe vollständige URL wie ihr Button. 160 gezielte Tests sind erfolgreich.
Die Kontostatus-Vorlagen folgen in Paket 7. Der visuelle Mailpit-Nachtest bleibt
zentral im [Testbedarf](../testbedarf.md).

## 3. Zahlenfeld mit integrierter Einheitenauswahl analysieren

Analyse abgeschlossen: [Empfehlung, Feldzuordnung und offene Umsetzung](einheitenauswahl.md).
Native Zahlenfelder mit Select im Suffix genügen als Zusammensetzung. Tagesfelder
behalten ihre fachliche Einschränkung. Noch keine optionale UI-Umsetzung begonnen.

## 4. Einheitliche Seitenpfade und Breadcrumb-Navigation analysieren

Analyse abgeschlossen: [Bestandsaufnahme, native API und Zielzuordnung](seitenpfade.md).
Vaadin 25.3.5 enthält Breadcrumbs. Empfohlen ist der manuelle Modus für unsere
Queryparameter. Gesperrte Abläufe bleiben ohne Links. Noch keine Umsetzung begonnen.

## 5. Formularschutz um Browseraktionen erweitern

Umgesetzt: Derselbe Änderungsvergleich schützt persönliche Daten, allgemeine
Konfiguration, Anbieter-/2FA-Einstellungen und Benutzerformulare auch bei nativen
Browseraktionen. Ein sauberer oder geschlossener Bereich entfernt nur seinen
Schutz. Suchfelder, Filter und Codeeingaben bleiben ausgenommen.

Der gezielte deutsche Browserlauf hat alle 12 Fälle auf Desktop/Smartphone bestanden.
Der englische Nachlauf und ergänzende Grenzfälle der großen Runde bleiben im
[Testbedarf](../testbedarf.md). Mobile Betriebssystemabbrüche sind keine zuverlässig
abfangbare Browseraktion; es wird kein automatisches Speichern versprochen.
## 6. Dialoge zum Deaktivieren und endgültigen Löschen angleichen

Umgesetzt: Tabler-Personenicons, gemeinsam rechts angeordnete Übersichtaktionen,
gelbe Deaktivierung und rote Löschung, passende Sicherheitswarnboxen sowie der
Untertitel zur Identitätsbestätigung. Passwort und 2FA bleiben bewusste Klickaktionen.

Nach bestätigtem Commit wird die alte Sitzung beendet. Eine einmalige Rückmeldung
liegt ausschließlich in einer neuen anonymen Sitzung. Unklarer technischer Ausgang
wird ausdrücklich nicht als bestätigte Löschung oder Deaktivierung gemeldet.
Entwicklungskonten erhalten keine Versandzusage. Diese konservative Behandlung
verhindert eine falsche Aussage bei einem Fehler während des Transaktionsabschlusses.
Der gezielte Browsernachtest folgt mit den Kontomails; siehe [Testbedarf](../testbedarf.md).
## 7. Kontostatus-E-Mails in das bestehende Cosmos-Maildesign überführen

Vier Vorlagenpaare in MJML/HTML und Kontosprache sind umgesetzt. Der Java-Service
setzt kein eigenes Mail-HTML mehr zusammen. Deaktivierung und Erinnerung haben
Reaktivierungsbutton und identische kopierbare URL; Lösch- und
Reaktivierungsbestätigung bleiben ohne Aktion. Die Reaktivierungsbestätigung
entsteht erst mit gültigem einmaligem Tokenverbrauch in derselben Transaktion.
Entwicklungskonten bleiben ohne Mail.

13 gezielte Vorlagen-/Persistenztests sind erfolgreich. Der visuelle Mailpit-Nachtest
und die anschließende Gesamtprüfung bleiben im [Testbedarf](../testbedarf.md).
Echte Mailclient-Abnahmen bleiben im manuellen Katalog, nicht als Browsererfolg.
## 8. Selbst deaktivierte Konten nach der Anmeldung gezielt zur Reaktivierung führen

Umgesetzt: Der Loginlink entfällt. Gültiges Portalpasswort oder Anbieterbeweis
führen auf eine separate gelbe Hinweisseite mit Frist, Abmelden und Senden.
Der zeitlich begrenzte Nachweis erteilt keine Portalrechte. Sperren, Fristablauf,
Kontorevision, Versandwartezeit und offene Queueaufträge werden serverseitig geprüft.
Die bestehenden Linkbestätigungen bleiben ausdrücklich. Browserprüfung folgt
gezielt und in der Gesamtrunde; der offene Umfang steht im [Testbedarf](../testbedarf.md).

## 9. Vollständige Kontolöschung mit umfangreich verknüpften Testkonten absichern

Die gemeinsame Bereinigung gilt für Selbstlöschung, Fristablauf und administrative
Löschung. Die zuvor fehlende zusätzliche administrative Bereinigung sowie die
Empfängerzahl gemeinsam erhaltener Nachrichten sind korrigiert. Reale isolierte
Persistenztests decken Anbieter/Avatar/Rollen, 2FA/Token, Remember-me, Sitzungen mit
Attributen, Anmeldeversuche, Queue/Versandhistorie und eigene/fremde Ticket- und
Benachrichtigungsdaten ab. Rollback und die Entfernung der minimalen
Löschbestätigung nach Erfolg beziehungsweise endgültigem Versandfehler sind geprüft.
Der unklare SMTP-Ausgang bleibt gemäß [Mailqueue](../mailqueue.md) als bestehender
Fehler zur administrativen Klärung offen; keine automatische Wiederholung.

- [ ] Die passenden Oberflächenabläufe in der abschließenden großen Testrunde
  mitprüfen. Der dauerhafte Löschvertrag steht in [Sicherheit](../sicherheit.md#eigenes-konto-deaktivieren-und-löschen).

## 10. Konfigurationsfelder für Deaktivierungsfristen vereinheitlichen

Umgesetzt: Die eigene Rubrik „Kontoverwaltung“ verwendet die vorhandenen nativen
Tagesfelder mit Suffix und kurzen Beschriftungen. Feldhilfen erklären ganze Tage,
Grenzen, ausgeschaltete Erinnerung und unveränderte bereits festgelegte Termine.
Nur Superadmins sehen und ändern die Fristen. Die Dienstprüfung lädt aktuelle
Rollen; die einmalige Startanlage kann vorhandene Werte nicht überschreiben.
Navigation, Speichern, Rollenentzug und die Feldhilfen sind gezielt geprüft.
Die ergänzten Browserfälle werden in der abschließenden Testrunde ausgeführt;
der offene Nachweis bleibt im [Testbedarf](../testbedarf.md).

## Vorerst zurückgestellt

Die Umstellung der Benachrichtigungsfilter und die Kennzahlenkarten werden
aktuell nicht bearbeitet. Sie bleiben
im [Komponentenplan](vaadin-komponenten-abgleich.md) erhalten und sind kein Auftrag
für diese Nachtliste.

## Abschließender Punkt: große Testrunde

Dieser Punkt bleibt am Ende der Liste. Weitere gesammelte Entwicklungsaufgaben
werden davor eingefügt. Die Runde beginnt nach den vorherigen Aufgaben und erst
mit dem ausdrücklichen Startsignal für die Nachtarbeit.

- [ ] Den dann aktuellen Gesamtstand nach dem bestehenden Ablauf für die große
  Testrunde prüfen, einschließlich Build, JUnit-, Frontend- und Browserprüfungen.
- [ ] [Offenen Testbedarf](../testbedarf.md) mit den vorhandenen Tests abgleichen,
  fehlende sinnvolle Regressionen ergänzen und tatsächlich ausführen. Auch die
  offenen Nachtests aus den vorherigen Arbeiten berücksichtigen.
- [ ] Browserprüfungen isoliert in Deutsch und Englisch durchführen; betroffene
  Oberflächen auf Desktop- und Smartphonebreite prüfen. Die Freigabe für diese
  abschließende Runde umfasst diese Browserläufe. Dafür eigene isolierte Testdaten
  und Testdienste verwenden; kein echter Mailversand. Für zusätzliche lokale
  Reproduktionen gilt die oben festgehaltene erweiterte Freigabe.
- [ ] Gefundene Fehler im vereinbarten Umfang beheben, eigene Änderungen auf
  Lesbarkeit prüfen und passende Nachtests ausführen. Menschliche Abnahmen und
  wegen fehlender Entscheidungen wartende Prüfungen ausdrücklich offen lassen.
- [ ] Den Prüfstand und verbleibenden Testbedarf aktualisieren. Zum Abschluss
  die Ergebnisse aller Nachtaufgaben und noch offenen Punkte zusammenfassen.
