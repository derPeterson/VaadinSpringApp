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

- [ ] Den bestehenden Schutz für persönliche Daten, die Speicherformulare der
  Konfiguration einschließlich Anbieter-/2FA-Einstellungen sowie Benutzer
  anlegen/bearbeiten um den nativen Browserhinweis ergänzen.
- [ ] Bei echten ungespeicherten Änderungen auch vor F5/Strg+R, Browser-Refresh,
  Tab-/Fensterschließen und Verlassen über die Adresszeile beziehungsweise externe
  Navigation warnen. Der Benutzer kann das Verlassen bewusst bestätigen.
- [ ] Den Hinweis an die vorhandenen Änderungsvergleiche koppeln. Unveränderte,
  erfolgreich gespeicherte und vollständig auf den Ausgangsstand zurückgesetzte
  Formulare warnen nicht. Nach einem fehlgeschlagenen Speichern bleibt der Schutz
  aktiv, solange die Änderungen noch vorliegen. Beim bewussten Verwerfen und
  Schließen eines Formulars den zugehörigen Browserhinweis wieder entfernen.
- [ ] Den browserseitigen Standarddialog verwenden. Bestehende anwendungsinterne
  Bestätigungen erhalten und doppelte Warnungen für dieselbe Aktion vermeiden.
  Suchfelder, Filter, Codeeingaben und sonstige Aktionen ohne Speicherformular
  bleiben außerhalb dieses Schutzes. Kein automatisches Speichern und keine
  zusätzliche Entwurfswiederherstellung einführen.
- [ ] Die passende öffentliche Vaadin-/Browseranbindung prüfen und die technische
  Zuverlässigkeitsgrenze auf mobilen Geräten berücksichtigen. Kein vollständiges
  Abfangen von Browserabstürzen oder zwangsweisem Beenden versprechen.
- [ ] Die offenen Browserfälle aus [Testbedarf](../testbedarf.md) in der großen
  Testrunde ergänzen und isoliert in DE/EN auf Desktop und Smartphone prüfen.

## 6. Dialoge zum Deaktivieren und endgültigen Löschen angleichen

- [ ] Im Tab „Konto verwalten“ Tabler `user-cog` verwenden. Den Inhaltsbereich
  als gelbe Warnhinweisfläche im bestehenden Design gestalten: Erklärung oben,
  darunter die beiden nativen Aktionsbuttons innerhalb der Fläche gemeinsam
  rechtsbündig nebeneinander anordnen. Den umgebenden
  Bereich „Sicherheit & Zugang“ und die übrigen Tabs unverändert lassen.
- [ ] „Konto deaktivieren“ mit Tabler `user-pause` gelb gestalten, „Konto endgültig
  löschen“ mit Tabler `user-x` rot. Dies gilt für die Übersicht und die jeweilige
  Bestätigungsaktion im Dialog. Vorhandene gemeinsame Warn- und Löschbutton-Stile
  verwenden, keine lokalen Nachbauten ihrer Zustände einführen.
- [ ] Beide bestehenden Dialoge mit dem Untertitel „Identität bestätigen“ im
  Kopf ergänzen, wie bei der Anbieterbestätigung. Gestaltung und Aufbau der
  vorhandenen Dialogbausteine beibehalten; DE/EN berücksichtigen.
- [ ] Für das Deaktivieren Tabler `user-pause` im Dialogkopf und im
  Bestätigungsbutton verwenden. Für das endgültige Löschen Tabler `user-x`
  einsetzen, ebenfalls im Kopf und im Button. Die Personen-Icons stammen aus
  derselben Familie; keine Mischung mit den runden Lucide-Personen-Icons.
  Das Kopf-Icon samt seinem Rahmen und dezenten Hintergrund bei Deaktivierung
  gelb und bei Löschung rot gestalten. Die übrigen Dialogflächen behalten das
  gemeinsame Design.
- [ ] Den bestehenden Text zu den Folgen der Aktion aus dem normalen Fließtext
  in eine gemeinsame Hinweisbox vor der Eingabe übernehmen: Deaktivierung im
  vorhandenen gelben Sicherheitswarnbox-Design. Für die endgültige Löschung eine
  rote Farbvariante derselben Box ergänzen, mit identischen Abständen, Formen
  und Schriftwerten. Icon, fette Einleitung „Sicherheitshinweis:“, Rahmen und
  dezenter Hintergrund verwenden den jeweiligen Warnfarbton; für Rot die
  vorhandenen Fehlerfarb-Tokens nutzen. Der restliche Text bleibt normal hell.
  Abmeldung, Frist und anschließende Löschung beziehungsweise sofortige endgültige
  Löschung klar benennen. Die vorhandene Regel zur Icon-Ausrichtung beachten.
- [ ] Dieselben Anpassungen für die Passwort- und die 2FA-Variante verwenden.
  Über dem Passwortfeld ergänzen: „Geben Sie Ihr aktuelles Portalpasswort ein:“
  sowie die englische Entsprechung, in regulärer Textfarbe mit 12 px und Gewicht
  400. Vorhandene App-Code-/Ersatzcode-Hinweise bleiben im gemeinsamen Baustein.
- [ ] Bestehende Identitäts-, Rechte- und Letzter-Superadmin-Prüfungen erhalten.
  Die Kontoaktion weiterhin erst durch bewussten Klick auf den Bestätigungsbutton
  ausführen, auch bei vollständig eingegebenem App-Code.
- [ ] Nach erfolgreicher Deaktivierung beziehungsweise Löschung auf der
  Anmeldeseite einmalig eine passende Erfolgsnotification anzeigen:
  „Ihr Konto wurde erfolgreich deaktiviert.“ beziehungsweise „Ihr Konto wurde
  endgültig gelöscht.“ Die Rückmeldung über die Abmeldung hinweg erhalten,
  ohne die alte Sitzung weiterzuverwenden. Erfolg nur nach bestätigtem
  Transaktionsabschluss melden, nicht allein aufgrund der Weiterleitung.
- [ ] Den Mailhinweis am tatsächlichen Ergebnis ausrichten: Bei Queueaufnahme
  „Die Bestätigungs-E-Mail wurde zum Versand vorgemerkt.“ ergänzen, keine
  erfolgte Zustellung behaupten. Für Entwicklungskonten ohne Mailversand keinen
  Versandhinweis anzeigen. Einen Versandfehler getrennt vom bereits bestätigten
  Kontostatus benennen; er macht eine erfolgte Kontoänderung nicht rückgängig.
- [ ] Bei einem technischen Fehlschlag der Kontoaktion auf der Anmeldeseite eine
  Fehlernotification anzeigen: „Ihr Konto konnte nicht deaktiviert werden.
  Bitte wenden Sie sich an die Administration.“ beziehungsweise „Ihr Konto
  konnte nicht gelöscht werden. Bitte wenden Sie sich an die Administration.“
  Keine Erfolgsrückmeldung und keine Behauptung einer Kontoänderung ausgeben.
  Ist der Ausgang unklar, dies ausdrücklich sagen. Falsche Passwörter/Codes und
  fachliche Ablehnungen weiterhin direkt im Dialog erklären. Alle Meldungen
  auch auf Englisch und mit den vorhandenen Notification-Bausteinen umsetzen.
- [ ] Die Darstellungen und Aktionen mit und ohne 2FA in der abschließenden
  Testrunde isoliert in DE/EN auf Desktop und Smartphone prüfen. Nur eigene
  Testkonten verwenden; der offene Browsernachtest steht im [Testbedarf](../testbedarf.md).

## 7. Kontostatus-E-Mails in das bestehende Cosmos-Maildesign überführen

- [ ] Die bisherigen schlichten HTML-Inhalte für Deaktivierung, Löschwarnung
  und Löschbestätigung durch MJML-Vorlagen und daraus erzeugte HTML-Dateien
  ersetzen. Bestehende Kontomails als Vorlage für Logo, Farben, Typografie,
  Karten, Buttons, Abstände und Footer verwenden; keine zweite Mailgestaltung
  einführen. Alle Vorlagen auf Deutsch und Englisch bereitstellen und über den
  vorhandenen Vorlagen-/Versandweg laden.
- [ ] Die bisherige HTML-Zusammensetzung in `AccountLifecycleService` vollständig
  entfernen, einschließlich ausschließlich dafür verwendeter Hilfen/Importe.
  Keine alten HTML-Strings oder parallelen Versandpfade als Fallback behalten.
  Den Service selbst mit seiner Fachlogik und den Sicherheits-/Transaktionsgrenzen
  erhalten. Die feste [E-Mail-Vorgabe](../entwicklung.md#e-mail-vorlagen-und-cosmos-design)
  bei Umsetzung und Review anwenden.
- [ ] „Konto deaktiviert“: Den bestätigten Kontostatus, das konkrete Löschdatum
  und die Möglichkeit zur rechtzeitigen Reaktivierung erklären. Ein Button
  „Konto reaktivieren“ führt zum vorhandenen geschützten Reaktivierungsablauf.
  Auch beim erneuten Anfordern eines Reaktivierungslinks dieselbe Vorlage mit
  passendem Text verwenden.
- [ ] „Konto wird bald gelöscht“: Erinnerung mit konkretem Löschdatum und
  Reaktivierungsbutton gestalten. Die bestehenden konfigurierbaren Fristen
  beibehalten: Standardaufbewahrung 30 Tage und Erinnerung sieben Tage vorher,
  kein fest im Mailtext versprochener Wochenabstand. Die vorhandene Behandlung
  kurzer Fristen und einer deaktivierten Erinnerung erhalten.
- [ ] „Konto endgültig gelöscht“: Die abgeschlossene Löschung bestätigen, sowohl
  nach bewusster Sofortlöschung als auch nach Ablauf der Deaktivierungsfrist.
  Kein Reaktivierungsbutton und keine Wiederherstellbarkeit versprechen. Die
  bestehende Versandmöglichkeit nach Entfernung des Kontodatensatzes erhalten.
- [ ] „Konto wieder aktiviert“ als zusätzliche Bestätigung nach erfolgreicher
  Reaktivierung ergänzen. Erst mit bestätigter Kontoänderung einreihen, nicht
  schon beim Öffnen des Links oder nach ungültigen/abgelaufenen Links. Keine
  automatische Anmeldung oder Umgehung von Passwort/2FA einführen.
- [ ] Tatsächliche Kontosprache, konfigurierte Basisadresse und konkrete
  Fristdaten verwenden. Die Regeln aus Nachtaufgabe 1 für Entwicklungskonten
  auch bei allen vier Vorlagen beachten; keine Mail für solche Konten senden.
  Vorhandene Transaktions-, Token-, Queue- und Widerrufsgrenzen erhalten.
- [ ] Mit Nachtaufgabe 2 abstimmen: Bei vorhandenen Aktionsbuttons die vollständige
  tatsächliche Ziel-URL darunter zum Kopieren anzeigen. Reine Bestätigungsmails
  erhalten keine unnötigen Aktionslinks oder leeren Linkbereiche.
- [ ] Vorlagen-/Versandtests für alle vier Ereignisse ergänzen und die erzeugten
  Mails in der abschließenden Testrunde isoliert über Mailpit in DE/EN bei
  Desktop- und Smartphonebreite prüfen. Keine echten E-Mails versenden. Den
  bestehenden manuellen Mailclient-Katalog um die neuen Vorlagen ergänzen und
  echte Clientabnahmen als solche offen halten. Ergebnisse und offene Prüfungen
  im zentralen [Testbedarf](../testbedarf.md) pflegen.

## 8. Selbst deaktivierte Konten nach der Anmeldung gezielt zur Reaktivierung führen

- [ ] Den zusätzlichen Link „Konto reaktivieren“ aus dem Anmeldeformular
  entfernen. Stattdessen nach gültigem erstem Anmeldenachweis für ein selbst
  deaktiviertes Konto eine eigene Hinweisseite öffnen. Die Gestaltung der
  bestehenden Seite für ausstehende E-Mail-Bestätigung übernehmen: gemeinsamer
  Seitenrahmen, gelber Status und Hinweis, native Vaadin-/Cosmos-Komponenten.
  Keine Vermischung der fachlichen Zustände Verifikation und Deaktivierung.
- [ ] Erklären, dass das Konto deaktiviert ist und über den Link in der
  Deaktivierungs-E-Mail rechtzeitig reaktiviert werden kann. Das tatsächliche
  Löschdatum anzeigen und auf Spam-/Junkordner hinweisen. Die Aktionen sind
  „Abmelden“ und „Senden“, gemeinsam rechtsbündig und gleich
  breit wie auf der E-Mail-Bestätigungsseite. Texte und Zustände in DE/EN pflegen.
- [ ] Erneuten Versand an die gespeicherte Kontoadresse mit den vorhandenen
  Wartezeit-, Deduplizierungs- und Tokenregeln ermöglichen. Keine automatische
  Mail bei jedem Anmeldeversuch und keine frei änderbare Empfängeradresse auf
  der Hinweisseite. Queueaufnahme und Versandfehler ehrlich rückmelden.
  Die Mailvorlage aus Nachtaufgabe 7 und den kopierbaren Link aus Punkt 2 nutzen.
- [ ] Die Weiterleitung für Portalpasswort und verbundene Anmeldeanbieter
  berücksichtigen. Kontostatus erst nach erfolgreichem Nachweis offenlegen;
  eine bekannte E-Mail-Adresse allein genügt nicht. Der beschränkte Zustand
  erlaubt keine normale Konto-/Adminnutzung, keine Remember-me-Anmeldung und
  keine Umgehung der 2FA. Refresh und direkte URLs dürfen ihn nicht freischalten.
- [ ] Administrativ deaktivierte Konten weiterhin von der Selbstreaktivierung
  ausschließen. Gelöschte Konten und abgelaufene Reaktivierungsfristen eröffnen
  keinen Reaktivierungsweg. Entwicklungskonten bleiben gemäß Nachtaufgabe 1
  vom Mailversand ausgeschlossen; keine falsche Versandzusage anzeigen.
- [ ] Den bestehenden Reaktivierungslink und seine bewusste Bestätigung erhalten.
  Erst nach gültiger Bestätigung wird das Konto wieder aktiv; die Hinweisseite
  oder eine erneute Linkanforderung allein aktiviert es nicht. Anschließend
  regulär anmelden und die vorhandenen Sicherheitsprüfungen durchlaufen.
- [ ] Passende Regressionen ergänzen und in der abschließenden Testrunde
  isoliert in DE/EN auf Desktop und Smartphone prüfen. Offene automatische
  Prüfungen zentral im [Testbedarf](../testbedarf.md) erhalten. Noch keine Umsetzung.

## 9. Vollständige Kontolöschung mit umfangreich verknüpften Testkonten absichern

- [ ] Vorhandene Löschtests mit dem aktuellen H2-Schema und allen fachlichen
  Kontoverknüpfungen abgleichen. Der bestehende Persistenztest belegt Teilfälle
  für Konto, persönliche Queue-/Versanddaten, eine Benachrichtigung und eine
  gespeicherte Sitzung; er ist kein vollständiger Löschungsnachweis. Eine kurze
  fachliche Übersicht der Löschregeln in der aktuellen Kontodokumentation pflegen,
  keinen zusätzlichen dauerhaften QA-Bericht oder Parallelkatalog anlegen.
- [ ] Vorhandene Persistenztests um ein repräsentatives, umfangreich verknüpftes
  Konto und unabhängige Kontrollkonten erweitern. Reale isolierte Datenbankdaten
  für Profil/Avatar, Rollenverknüpfungen, Google-/Microsoft-Identitäten,
  2FA einschließlich Ersatzcodes und Challenges, Verifikations-/Passwortreset-/
  E-Mail-Änderungstoken, gespeicherte Anmeldungen, Sitzungen samt Attributen,
  Anmeldeversuche, Mailqueue/Versandhistorie, Benachrichtigungen/Empfänger und
  Tickets/Antworten/Beobachter aufbauen, soweit diese Beziehungen existieren.
  Nicht nur Fremdschlüssel prüfen: auch Verknüpfungen über E-Mail, Hashwerte,
  Ticketnummern und gespeicherte Momentaufnahmen berücksichtigen.
- [ ] Sofortige Selbstlöschung, automatische Löschung nach Deaktivierungsfrist
  und administrative Löschung prüfen. Jeweils nach tatsächlichem Commit in
  einem frischen Lesekontext die betroffenen Tabellen und Beziehungen abfragen.
  Die Bereinigung gespeicherter Anmeldungen, Sitzungseinträge und Mailaufträge
  für diesen Nachweis nicht nur durch simulierte Dienste bestätigen lassen.
- [ ] Mischfälle mit fremden Daten abdecken: eigene Tickets mit fremden Antworten,
  eigene Antworten/Vermerke in fremden Tickets, übernommene fremde Vorgänge,
  ausschließlich persönliche und gemeinsam adressierte Benachrichtigungen.
  Eigene personenbezogene Daten und Inhalte gemäß vereinbartem Löschvertrag
  entfernen; fremde Konten und unabhängige Daten erhalten. Freigegebene fremde
  Vorgänge müssen fachlich konsistent wieder übernehmbar sein. Gemeinsam genutzte
  Rollen und globale Konfiguration sind keine zu löschenden Kontodaten.
- [ ] Die bewusst gewünschte Löschbestätigung gesondert prüfen: Ihr minimaler
  Versandauftrag enthält vorübergehend die Empfängeradresse, obwohl das Konto
  bereits gelöscht ist. Entfernung nach erfolgreichem Versand beziehungsweise
  endgültigem Versandfehler prüfen. Den bestehenden Sonderfall eines unklaren
  Transportausgangs ausdrücklich mit dem offenen Mailqueue-Bug abgleichen und
  verbleibende Daten ehrlich benennen, nicht als vollständig gelöscht melden.
- [ ] Alte Sitzungen, Remember-me-Nachweise, Anbieterzuordnungen und Aktionslinks
  dürfen nach Kontolöschung keinen Zugriff oder eine Wiederherstellung erlauben.
  Fehlgeschlagene Löschungen und Transaktionsrollback dürfen keine unbemerkte
  Teilbereinigung oder falsche Erfolgsbestätigung erzeugen. Die zeitlichen Grenzen
  nachgelagerter Sitzungsbereinigung prüfen und dokumentieren.
- [ ] Bei den Prüfungen gefundene echte Löschlücken im beauftragten Umfang
  beheben, mit Regressionen absichern und die geänderten Stellen gemäß der
  Lesbarkeitsausnahme dieses Nachtauftrags reviewen. Bestehende Tests erweitern
  statt parallele Testimplementierungen
  aufzubauen. Passende Löschabläufe in der abschließenden isolierten großen
  Testrunde mitprüfen. Offene Nachweise im [Testbedarf](../testbedarf.md) führen;
  für automatische Regressionen ausschließlich eigene isolierte Testkonten nutzen.
  Eine nötige Reproduktion in der freigegebenen lokalen Entwicklungsanwendung
  getrennt von diesen Regressionen behandeln.

## 10. Konfigurationsfelder für Deaktivierungsfristen vereinheitlichen

- [ ] Die Beschriftungen auf „Aufbewahrung nach Deaktivierung“ und
  „Löscherinnerung“ kürzen, ohne Klammerzusätze. Beide Zahlenfelder wie die
  Aufbewahrungsfelder unter Benachrichtigungen mit dem vorhandenen nativen
  Feld-/Suffixbaustein gestalten: Einheit „Tage“ rechts im Feld mit Trennlinie.
  „Tage vorher“ nicht als zusätzliche Feldbeschriftung oder Suffix verwenden;
  der Bezug der Erinnerung zum Löschtermin steht in der Feldhilfe. DE/EN pflegen.
- [ ] Beide Feldhilfen in den gemeinsamen Aufbau bringen beziehungsweise darauf
  prüfen. Ganze Tage, tatsächliche Grenzen, 0 als abgeschaltete Erinnerung und
  den effektiven Vorlauf bei kurzen Aufbewahrungsfristen verständlich erklären.
  Unter „Wann gilt die Änderung?“ festhalten: ohne Neustart für künftige
  Selbstdeaktivierungen; bereits festgelegte Lösch-/Erinnerungstermine bleiben
  erhalten. Keine abweichenden Fristberechnungen allein für die Darstellung.
- [ ] Wie freigegeben eine eigene Konfigurationsrubrik „Kontoverwaltung“ anlegen
  und die beiden Fristfelder aus „Sicherheit & Sitzungen“ dorthin verschieben.
  Vorhandene Gruppen-/Navigationsmechanik und den typisierten Konfigurationskatalog
  verwenden; zugehörige aktuelle Dokumentationsverweise nachziehen. „Benutzerstatus“
  bleibt für Anwesenheit/Abwesenheit zuständig.
- [ ] Die neue Konfigurationsrubrik ausdrücklich auf Superadmins beschränken:
  Nur sie dürfen die anwendungsweiten Fristen sehen und ändern. Admins und
  normale Benutzer erhalten keinen Zugriff, auch nicht über direkte URLs oder
  direkte Aufrufe der zuständigen Speicherdienste. Passende serverseitige
  Berechtigungsprüfungen und Regressionen ergänzen.
  Die neue Konfigurationsrubrik nicht mit „Mein Konto → Sicherheit → Konto
  verwalten“ verwechseln; dort verwaltet jeder berechtigte Kontoinhaber nur sein
  eigenes Konto. Keine Ausweitung der Kontozugriffe durch die neue Rubrik.
  Die [Rollenübersicht](../rollen.md) nach Umsetzung vom geplanten auf den
  tatsächlich geltenden Stand aktualisieren.
- [ ] Die freigegebenen Feld- und Hilfetextänderungen in der abschließenden
  isolierten Testrunde in DE/EN auf Desktop und Smartphone prüfen. Native
  Zahlenfeldbedienung, Suffix, Fokusdarstellung und unveränderte Wertebereiche
  berücksichtigen. Offene Prüfungen im [Testbedarf](../testbedarf.md) führen.

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
