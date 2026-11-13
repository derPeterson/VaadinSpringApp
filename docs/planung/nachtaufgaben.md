# Gesammelte Nachtaufgaben

## Freigabe

Die gesammelten Pakete sind mit dem ausdrücklichen Startauftrag freigegeben.
Paket 1 und 5 werden umgesetzt, Paket 2 bis 4 ausschließlich analysiert und
geplant. Paket 6 bleibt an die unten vereinbarten Voraussetzungen und Uhrzeiten
gebunden. Die Gesamtumsetzung bleibt bis zur gemeinsamen Abnahme offen.

## Ablauf nach der Startfreigabe

- Nach jedem vereinbarten Arbeitspaket die Änderungen reviewen, festgestellte
  Fehler im Umfang beheben und einen eigenen Commit mit aussagekräftiger
  deutscher Message in abgeschlossener Form erstellen. Das gilt bereits für
  das erste Paket zur E-Mail-Queue und anschließend für die weiteren Pakete.
- Ausschließlich für diese Nachtaufgabe die Lesbarkeit der **geänderten Bereiche**
  prüfen und gegebenenfalls korrigieren, nicht jeweils die kompletten Klassen
  erneut überarbeiten. Die allgemeinen Entwicklungs- und Reviewregeln werden
  dadurch nicht dauerhaft geändert.

## Offener Prüf- und Sichtungsstand

Paket 1 ist technisch umgesetzt. 62 gezielte JUnit-Fälle sowie die erneute
Konfigurationsregression sind bestanden. Die vier Queue-Browserfälle sind in DE
und EN auf demselben Image bestanden, einschließlich Desktop/Smartphone und
frischem Schema. Im Nachtest wurden eine Kartenbreite und die Synchronisierung
der Zeiteinheiten verbessert. Die gemeinsame Sichtung der fertigen Karte bleibt offen.

Für Paket 2 liegen [Release-Ablauf und Versionierung](releases.md) sowie
[Was ist neu?](neuigkeiten.md) zur gemeinsamen Durchsicht vor. Versionsnummern,
Skills und Anwendung wurden für diese beiden Planungen nicht verändert.

Für Paket 3 liegt die [Passkey-Planung](passkeys.md) vor. Empfehlung: vorerst
zurückstellen; eine spätere Alternative muss Anmeldung und sensible Aktionen
mit den vorhandenen Sicherheitsverträgen verbinden. Noch keine Umsetzung.

Für Paket 4 liegt die [Planung für öffentlichen Testbetrieb](oeffentlicher-testbetrieb.md)
mit Kostenvergleich, Mail/CI und konkreten Vorarbeiten vor. Keine Buchung oder
Bereitstellung wurde durchgeführt.

## Abarbeitungsliste

### 1. E-Mail-Queue in zwei Tabs gliedern

- [ ] Vor der Gestaltung vorhandene vergleichbare Oberflächen in der Anwendung
  ansehen und die zuständigen Vaadin-/Cosmos-Bausteine prüfen. Bestehende
  Konfigurationskarten, Tabs, Felder, Hinweise und Aktionsbereiche als konkrete
  Vorlage verwenden. Die vorhandenen Standard-Styles übernehmen.
- [ ] Unter dem bestehenden Header zwei native Vaadin-Tabs einfügen. Gestaltung
  und Anordnung exakt wie bei den Anmeldeanbietern Google/Microsoft übernehmen,
  gemäß dem beigefügten Screenshot. Keine segmentierte OTP-Auswahl verwenden.
- [ ] Im ersten Tab die fünf bisherigen allgemeinen Queue-Einstellungen bündeln:
  gleichzeitige Versandaufgaben, E-Mails pro Verarbeitungsdurchlauf, maximale
  Wiederholungen, Wartezeit bis zum erneuten Versand und Aufbewahrungsdauer
  gesendeter E-Mails. Den Tab **Allgemein** nennen.
- [ ] Auch **Allgemein** erhält sein Icon: Lucide `sliders-horizontal`.
- [ ] Den zweiten Tab **Assistenten** nennen. Dort die drei bisherigen Einstellungen
  der Bereinigung, die neue Historien-Aufbewahrung und die zugehörigen Bedienelemente, Warnung und Ergebnisanzeige
  zusammenführen. Die Mehrzahl lässt Platz für spätere weitere Assistenten,
  ohne jetzt zusätzliche Funktionen einzubauen.
- [ ] Für den Tab **Assistenten** das bereits vorhandene Lucide-Icon `bot`
  (`AppIcon.BOT`) verwenden. Im Content-Bereich den Bereinigungsassistenten
  mit Lucide `brush-cleaning` kennzeichnen. Die Icons haben unterschiedliche
  Aufgaben: Assistentenbereich und konkrete Bereinigung.
- [ ] Den Content-Bereich farblich wie die bestehenden fünf allgemeinen
  Queue-Einstellungen gestalten. Der aktuelle Screenshot ist die Referenz für
  Hintergrund, Feldflächen, Textfarben und die vorhandenen Cosmos-Hinweise und
  Aktionen. Beide Tabinhalte verwenden diesen bestehenden Standard.
- [ ] Im Tab **Assistenten** den Bereinigungsassistenten nach **Stitch-Variante A**
  als eigene innere Konfigurationskarte aufbauen. Den bestehenden äußeren
  E-Mail-Queue-Rahmen erhalten. Die innere Karte übernimmt den Aufbau unserer
  Konfigurationskarten: Header mit `brush-cleaning`, Titel und Sekundärtext,
  darunter eine Trennlinie und der bestehende schwarz-dunkelblaue Content-Bereich.
- [ ] **Automatische Bereinigung** als eigenes reguläres Konfigurationsfeld mit
  einem nativen Switch im selben Stil wie die anderen True/False-Einstellungen
  der Konfiguration darstellen, nicht im Header oder in der Beschriftung
  eines anderen Felds. Im zweispaltigen Feldbereich steht dieses Feld in der
  ersten Zeile links, rechts daneben die Aufbewahrung der Bereinigungshistorie.
  In der zweiten Zeile
  stehen Prüfintervall links und Mindestalter rechts, jeweils als bestehendes
  Zeitfeld mit Feldhilfe. Beide Zeitfelder sind nur bei aktivierter automatischer
  Bereinigung bedienbar, andernfalls disabled. Vorhandene Werte dabei erhalten.
  Auf schmalen Ansichten die Felder passend untereinander anordnen.
- [ ] In der ersten Zeile rechts **Aufbewahrung der Bereinigungshistorie** als
  vorhandenes Zeitfeld mit fester Einheit Tage und regulärer Feldhilfe ergänzen.
  Vorgabe: **30 Tage**. Das Feld bleibt auch bei
  ausgeschalteter Bereinigungsautomatik bedienbar, weil manuelle Läufe ebenfalls
  eine Historie erzeugen. Kein zusätzlicher Ein-/Aus-Schalter für die Historie.
  Die Feldhilfe erklärt Frist, nächsten täglichen Löschlauf und Auswirkungen auf
  bereits vorhandene Historieneinträge. Speichern über den gemeinsamen Footer.
- [ ] Keinen zusätzlichen sichtbaren Erklärungssatz zur Freischaltung der beiden
  Zeitfelder unter den Eingaben anzeigen. Die bestehenden Feldhilfen bleiben.
- [ ] Darunter den bestehenden gelben Warnhinweis anzeigen. Anschließend den
  Status des letzten Bereinigungslaufs in einem eigenen abgesetzten Hintergrund
  wie in Variante A anzeigen. Links stehen Statusüberschrift und Ergebnis.
  Rechts den Aktualisieren-Button direkt neben **Jetzt bereinigen** platzieren.
  Links das festgelegte Tabler-Icon `terminal-2` für die Statusausgabe verwenden.
  Die eigentliche Ergebnisausgabe mit Laufzeitpunkt und Anzahl bereinigter Aufträge
  in der vorhandenen Monospace-Schrift darstellen. Überschriften und Buttons
  behalten ihre Standardtypografie. Vorhandene fachliche Grenzen und den
  Bestätigungsablauf erhalten. Den Aufbau mit unseren
  Vaadin-/Cosmos-Bausteinen und Styles umsetzen, nicht die abweichenden Maße
  oder Farben der Stitch-Vorlage übernehmen.
- [ ] Beim Warnhinweis die bestehende Ausrichtungsregel verwenden: Beginnt der
  Text mit **Sicherheitshinweis:** in Fettschrift, steht das Icon oben an der
  ersten Textzeile wie bei den Ticketwarnungen.
- [ ] Bewusst ausgelöstes Laden mit sichtbarer Erfolgs- oder Fehlermeldung
  quittieren, sowohl beim Status-Aktualisieren als auch bei **Werte neu laden**.
  Keine Erfolgsmeldung bei fehlgeschlagenem Laden und keine Meldungsflut beim
  initialen Aufbau der Ansicht erzeugen.
- [ ] **Werte neu laden** und **Konfiguration speichern** ausschließlich im
  gemeinsamen Footer der äußeren E-Mail-Queue-Karte belassen. Sie gelten für
  die gesamte Queue-Konfiguration einschließlich beider Tabs und künftiger
  Assistenten. Keine zusätzlichen Speicherbuttons in der inneren Karte anlegen.
  Status aktualisieren und Bereinigung ausführen bleiben lokale Aktionen des
  jeweiligen Assistenten. Direkt dort erklären, dass der manuelle Lauf gespeicherte
  Einstellungen verwendet. Er bleibt auch bei ausgeschalteter Automatik möglich,
  unter Beibehaltung des gespeicherten Mindestalters und der Sicherheitsprüfungen.
- [ ] Diesen Hinweis zum manuellen Lauf unterhalb des Ausführungsbereichs in
  unserer bestehenden dezenten Info-Box darstellen: dunkle Fläche, dezenter
  Rahmen und blaues Info-Icon. Die zentralen Abstände und die bestehende Regel
  zur Icon-Ausrichtung übernehmen. Die gelbe Sicherheitswarnung bleibt separat.
- [ ] Den gesamten Tab **Assistenten** ausschließlich Superadmins anzeigen.
  Admins sehen nur den Tab **Allgemein** mit den fünf allgemeinen
  Queue-Einstellungen. Die fachlichen Berechtigungsprüfungen für die Bereinigung
  bleiben zusätzlich zur Sichtbarkeit erhalten.
- [ ] Neue Beschriftungen wie üblich auf Deutsch und Englisch bereitstellen.
- [ ] Texte eigenständig passend zur aktuellen Fachlogik formulieren und mit dem
  tatsächlichen Verhalten abgleichen. Stitch dient als gestalterische Anregung,
  seine Texte werden nicht ungeprüft übernommen. Die genaue Formulierung darf
  innerhalb dieses fachlichen Rahmens selbstständig gewählt werden.
- [ ] Die vorhandenen Feldhilfen in **Allgemein** und **Assistenten** im bisherigen
  gemeinsamen Stil und mit ihrem fachlichen Informationsumfang erhalten.
  Auch die Assistentenfelder verwenden unsere regulären Feldhilfe-Buttons und
  Hilfedarstellung. Stitch-Hilfetexte unter Eingabefeldern ersetzen diese nicht.

#### Technische Laufhistorie und Aufbewahrung

- [ ] Zusätzlich zum bisherigen letzten Stand jeden tatsächlichen automatischen
  oder manuellen Bereinigungsversuch dauerhaft als eigenen Datensatz erfassen:
  Zeitpunkt, Auslöser, Anzahl bereinigter Aufträge und Ergebnis einschließlich
  Blockierung durch laufenden Versand. Auch abgeschlossene Läufe mit null
  bereinigten Aufträgen erfassen. Reine Scheduler-Prüfungen ohne fälligen Lauf
  erzeugen keine Einträge. Keine Empfänger oder Mailinhalte in die Historie kopieren.
- [ ] Datenmodell und aktuelles H2-Schema gemeinsam ergänzen. Bestehende
  Transaktions- und Sperrgrenzen erhalten. Die bisherige Anzeige des letzten
  Laufs und die Intervallsteuerung bleiben auch bei Historienbereinigung erhalten.
- [ ] Historieneinträge täglich um **03:15 Uhr Serverzeit** gemäß gespeicherter
  Aufbewahrungsfrist automatisch löschen. Uhrzeit als festen Entwicklerwert
  vorsehen, keine zusätzliche Oberflächenkonfiguration. Der Historien-Löschlauf
  ist unabhängig vom Schalter für die Bereinigung ungeklärter Mailaufträge.
  Eine geänderte Frist gilt ohne Neustart beim nächsten täglichen Löschlauf
  auch für bereits gespeicherte Historieneinträge.
- [ ] Gezielte Regressionen für manuelle und automatische Historieneinträge,
  Nullergebnisse, blockierte Läufe, reine Scheduler-Prüfungen und die
  Aufbewahrungsgrenze ergänzen. Fristen und Wirkung in DE/EN dokumentieren.
- [ ] Noch keine sichtbare Verlaufsliste, Detailansicht oder Navigation zur History
  umsetzen. Die Daten sind zunächst nur in der Datenbank abrufbar. Den späteren
  gemeinsamen UI-Entwurf als eigene offene Arbeit in [TODO](../../TODO.md) erhalten.

### 2. Release-Ablauf und „Was ist neu?“ planen

Dieses Paket ist ausschließlich eine Analyse- und Planungsaufgabe. Noch keinen
Release erstellen, keine Versionsnummern ändern, keinen Skill anlegen und keine
Neuigkeiten-Oberfläche umsetzen.

- [ ] Einen zum Projekt passenden Release-Ablauf als Markdown-Plan ausarbeiten.
  Eine abgenommene, abgeschlossene Umsetzung soll zu einer neuen Version führen.
  Außerdem soll ein Release später ausdrücklich nach einer bestimmten Änderung
  beauftragt werden können. Nicht jeder Zwischencommit ist ein Release.
- [ ] Versionierung, Abschlusskriterien, Build-/Testnachweise, Release-Artefakt und
  gegebenenfalls Git-Tag im Ablauf klären. Bestehende Umsetzungs-, Review-, Commit-
  und Merge-Abläufe berücksichtigen. Den Inhalt eines künftigen Release-Skills
  einschließlich seiner Eingaben und Grenzen planen.
- [ ] Maven-Snapshots, Buildkennzeichnung und Releases anhand offizieller Quellen
  recherchieren und für unseren lokalen Maven-Reactor bewerten. Keine neue
  Versionsnummer nach jedem Commit vorsehen. Den konkreten Quellstand über
  Git-Commit und gegebenenfalls Buildkennung nachvollziehbar machen.
- [ ] Als Ausgangsvorschlag Entwicklung auf einer kommenden `x.y.z-SNAPSHOT`-
  Version, geprüften Release ohne `-SNAPSHOT` und anschließend die nächste
  Entwicklungsversion planen. Zeitgestempelte Snapshot-Veröffentlichungen in
  einem Artefakt-Repository von bloßen lokalen Builds unterscheiden. Jenkins
  oder ein Repository nicht ohne tatsächlichen Bedarf einführen.
- [ ] Ein verständliches Major-/Minor-/Patch-Schema für dieses Portal empfehlen:
  Fehlerkorrektur, neue Funktion und inkompatible Änderung fachlich abgrenzen.
  Die Bedeutung von Kompatibilität für unsere Anwendung festlegen, statt die
  SemVer-Regeln für öffentliche Bibliotheks-APIs unbesehen zu übertragen.
  Eine anfängliche `0.x.y`-Entwicklungsphase gegenüber `1.0.0` bewerten und
  konkrete Beispiele für kleine Korrektur- und Funktionsreleases liefern.
- [ ] Gemeinsame Versionsführung von Root, Shared, App und Browsertests prüfen.
  Noch keine Änderung an den bisherigen `1.0-SNAPSHOT`-Versionen vornehmen.
- [ ] Im geplanten Release-Skill die Unterscheidung von Entwicklungssnapshot,
  Patch-, Minor- und Major-Release ausdrücklich verankern. Der Ablauf soll die
  Änderungen seit dem letzten Release selbst prüfen, einen passenden Versionstyp
  samt nächster Nummer vorschlagen und kurz begründen. Der Benutzer muss diese
  Einordnung nicht bei jedem Auftrag selbst vorgeben. Unklare fachliche
  Kompatibilitätsbrüche gezielt zur Entscheidung vorlegen, statt allein aus
  Commit-Titeln oder Änderungsumfang eine Version abzuleiten.
- [ ] Die fertige Planung zuerst dem Benutzer zur Durchsicht vorlegen und
  gemeinsam besprechen. Skill, Versionsautomatik, Release-Erstellung und
  Neuigkeiten-Oberfläche erst nach einem späteren Umsetzungsauftrag einführen.
- [ ] Eine zentrale Markdown-Datei für fortlaufend gepflegte Release-Notizen
  vorsehen. Die Pflege soll verbindlich in den Arbeitsablauf eingehen, vor allem
  beim Abschluss einer Umsetzung. Einträge redaktionell aus dem fertigen
  Funktionsumfang ableiten, nicht ungefiltert aus Commit-Messages erzeugen.
- [ ] Inhaltlich kurze Stichpunkte zu wesentlichen neuen oder geänderten
  Funktionen, neuen Bereichen und ihren Nutzungsmöglichkeiten aufnehmen.
  Kleinteilige Layoutkorrekturen, Buttonverschiebungen, wiederholte Designanläufe
  und bloße technische Commit-Chroniken weglassen. Sinnvolle Zusammenfassung
  mehrerer Zwischenänderungen und einen noch unveröffentlichten Abschnitt prüfen.
- [ ] Darauf aufbauend eine gesonderte Planung für einen späteren **Was ist neu?**-
  Bereich in unseren Vaadin-/Cosmos-Oberflächen ausarbeiten. Die vorhandene
  Startseite nicht ersetzen. Platzierung, unaufdringliche Erreichbarkeit,
  Darstellung mehrerer Versionen sowie DE/EN berücksichtigen. Kein automatisches
  Popup-Verhalten aus dem Chrome-Beispiel ungefragt übernehmen.
- [ ] Eine gemeinsame Inhaltsquelle für Entwicklernotizen und sichtbare
  Neuigkeiten bevorzugen. Markdown-Auswertung und Darstellung mit unseren
  bestehenden Vaadin-Bausteinen gegen eine generierte HTML-Ausgabe abwägen.
  Keine manuell doppelt gepflegte Markdown-/HTML-Sammlung vorsehen. Den konkreten
  Aufbau und erforderliche technische Schritte erst in dieser Planung festlegen.
- [ ] Den bereits vorhandenen Punkt **Hilfe und Neuerungen** in der
  [Portal-Erweiterungsplanung](portal-erweiterungen.md) einbeziehen und passende
  Verweise zwischen den offenen Planungen setzen.

Quellen für die weitere Ausarbeitung:
[Maven: Snapshots und Remote-Repositories](https://maven.apache.org/repositories/remote.html),
[Maven: Release-Ablauf](https://maven.apache.org/guides/mini/guide-releasing.html),
[Semantic Versioning](https://semver.org/lang/de/).

### 3. Passkeys im Zusammenspiel mit bestehender 2FA analysieren und planen

Dieses Paket ist ausschließlich eine Analyse- und Planungsaufgabe. Die bestehende
Anmeldung und Authenticator-Absicherung bleiben unverändert. Noch keine Passkeys
implementieren, Verfahren entfernen oder Sicherheitsregeln abschwächen.

- [ ] Den aktuellen Anmelde- und Sicherheitsstand gezielt untersuchen:
  Portalpasswort, Google/Microsoft, TOTP, Ersatzcodes, Rollenregeln,
  Identitätsbestätigungen, Sitzungen und Wiederherstellungsabläufe. Passkeys
  anhand aktueller offizieller Quellen zu WebAuthn/FIDO und den tatsächlich
  verwendeten Framework-Versionen bewerten.
- [ ] Eine klare Empfehlung **jetzt sinnvoll oder später zurückstellen** mit
  konkretem Komfort-/Sicherheitsgewinn, zusätzlicher Komplexität, Aufwand und
  Grenzen ausarbeiten. Berücksichtigen, dass der Benutzer mit dem bestehenden
  Verfahren zufrieden ist. Die bloße Verbreitung der Technik begründet noch
  keinen Einbau.
- [ ] Einen einfachen Anmeldefluss vorschlagen: Passkey als mögliche Alternative
  statt zusätzlicher Pflichtschritt neben Passwort und TOTP. Festlegen, welche
  serverseitig geprüften Eigenschaften einschließlich lokaler Benutzerverifikation
  eine vorhandene 2FA-Anforderung erfüllen könnten. Kein pauschales Überspringen
  unserer Rollen- oder Aktionsregeln vorsehen. Fallback-Wege und ihre Auswirkungen
  auf die Gesamtsicherheit ausdrücklich berücksichtigen.
- [ ] Einrichtung aus einem bereits abgesicherten Konto, Bestätigung beim
  Hinzufügen, mehrere Passkeys, Benennung, Übersicht und Widerruf planen.
  Gerätegebundene und synchronisierte Passkeys, Geräteverlust, Gerätewechsel
  und Nutzung auf PC/Laptop/Handy einordnen.
- [ ] Sicherheitsrelevante Folgeabläufe vollständig abgleichen: sensible
  Kontoänderungen, Passwort-/E-Mail-Wechsel, Anbieter verbinden/trennen,
  Authenticator-Wechsel und Entfernung, Ersatzcodes, Wiederherstellung/Hilfe,
  administrative Sperre, Selbstdeaktivierung/Reaktivierung und Kontolöschung.
  Sitzungswiderruf, erneute Identitätsbestätigung und den Schutz gegen Aussperren
  beziehungsweise unberechtigte Kontowiederherstellung berücksichtigen.
- [ ] Die technischen Anforderungen und betroffenen Bereiche beschreiben:
  Credential-Daten und aktuelles Schema, Challenge-Verwaltung und Einmaligkeit,
  RP-ID/Origin/HTTPS einschließlich unserer lokalen und LAN-Testzugänge,
  Browseranbindung sowie passende vorhandene Vaadin-/Spring-Bausteine. Nötigen
  Eigenanteil und mögliche neue Abhängigkeiten nur benennen und bewerten.
- [ ] Auswirkungen auf Anmeldewege-/Sitzungsanzeigen, Konfiguration, bestehende
  Feldhilfen, DE/EN-Texte, Dokumentation und sinnvolle Regressionen planen.
  Benötigte reale Geräte-/Passkey-Provider-Prüfungen von automatisierbaren Tests
  unterscheiden. Noch keine solchen Tests oder Kontoeinrichtungen ausführen.
- [ ] Eine eigenständige Markdown-Planung mit Empfehlung, offenen Entscheidungen
  und geordneten Umsetzungspaketen erstellen. Vor einer späteren Umsetzung
  den Plan erneut gegen Code, Sicherheitsverträge und dann aktuelle offizielle
  APIs prüfen. Er ist ein wiederverwendbarer Ausgangspunkt, keine dauerhafte
  Aussage über unveränderte technische Voraussetzungen und keine Startfreigabe.
- [ ] Die [Portal-Erweiterungsplanung](portal-erweiterungen.md) passend verlinken.
  Umsetzung erst nach gemeinsamer Durchsicht und gesondertem Auftrag.

### 4. Öffentlichen Testbetrieb und komfortables Deployment planen

Dieses Paket ist ausschließlich eine Analyse- und Planungsaufgabe. Noch nichts
buchen, keine öffentliche Instanz bereitstellen, keine DNS-/Anbieterzugänge ändern
und keine lokalen Entwicklungsdaten veröffentlichen. Ziel ist ein öffentlich
erreichbarer Testbetrieb für Freunde und Bekannte mit echten Konten und den
regulären Registrierungs- und Anmeldeabläufen, kein ungeprüfter Produktivstart.

- [ ] Besucher sollen sich über die normale Oberfläche selbst registrieren,
  ihre E-Mail-Adresse bestätigen und anschließend regulär anmelden können.
  Passwort, Google/Microsoft sowie die jeweils geltenden 2FA-Regeln in der
  Planung berücksichtigen. Kein Demo-Sofortlogin, keine gemeinsam verwendeten
  Demo-Konten und keine vereinfachte Anmeldung für diesen Testbetrieb bauen.
  Ziel ist ehrliches Feedback zum vollständigen Zugang und dessen Bedienbarkeit.

- [ ] Etablierte Hostinganbieter mit aktuellen Preisen und Bedingungen vergleichen.
  Zielbudget ungefähr **5–10 Euro monatlich**, geringer Einrichtungs-/Betriebsaufwand
  und zugleich Lernmöglichkeit für den Benutzer. Einen etablierten deutschen
  Anbieter bevorzugen, wenn Ausstattung und Preis passen. Keine Lösung mit
  ungefähr 100 Euro laufenden Kosten vorsehen. Gesamtkosten transparent aufteilen:
  Server, Mehrwertsteuer, IPv4, Domain, Backups, Mailversand, CI-/Registry-Nutzung
  und gegebenenfalls nutzungsabhängige Zusatzkosten. Verfügbarkeit und benötigte
  RAM-/CPU-/Speicherausstattung anhand unserer Anwendung bewerten.
- [ ] Den Ressourcenbedarf des konkreten Projekts abschätzen: Größe des
  Anwendungs-JARs und Container-Images, Arbeitsspeicher der laufenden JVM,
  CPU-/Speicherbedarf von Vaadin-Sitzungen, Datenbank, Avataren, Mailqueue und
  Logs bei wenigen gleichzeitigen Testbenutzern. Vorhandene Build-/Testnachweise
  verwenden und Schätzungen als solche kenntlich machen. Den zeitweise höheren
  Bedarf beim Bauen/Testen vom dauerhaften Anwendungsbetrieb trennen und CI,
  Jenkins oder Monitoring nicht ohne Budgetierung auf denselben kleinen Server
  legen. Keine tausenden Benutzer oder Mails als Zielgröße annehmen.
- [ ] Als Ausgangskandidat einen kleinen Hetzner-Cloudserver in Deutschland mit
  bestehendem Docker-/Caddy-Aufbau und automatisiertem Deployment prüfen. Eine
  komfortable verwaltete Container-Plattform als Alternative vergleichen, etwa
  Railway. Laufende Kosten, Persistenz, Wartung und Unterstützung unseres
  Spring-/Vaadin-Aufbaus abwägen. Empfehlung begründen, noch keinen Anbieter festlegen.
- [ ] Echten transaktionalen E-Mail-Versand ausdrücklich mitplanen. Einen zum
  kleinen Testbetriebs-Volumen passenden SMTP-/Transaktionsmail-Anbieter vergleichen,
  der unsere bestehende Mailqueue und SMTP-Anbindung verwenden kann. Aktuellen
  Versandweg hinsichtlich externer Erreichbarkeit und Zustellung bewerten, nicht
  ungeprüft übernehmen. Absenderadresse mit eigener Domain, Domainnachweis,
  SPF/DKIM/DMARC, Versandlimits und Kosten berücksichtigen. Eine Antwortadresse
  beziehungsweise ein empfangendes Postfach bei Bedarf getrennt einordnen.
  Noch keinen Dienst buchen, DNS ändern oder echte Testmails versenden.
- [ ] Prüfen, ob ein Hosting-/Domainangebot bereits geeigneten SMTP-Versand oder
  ein Postfach enthält und ob es für Bestätigungs-/Resetmails passt. Mailpit als
  mögliche isolierte Test-/Diagnoseumgebung einordnen: abgefangene Testmails sind
  keine echte Zustellung an Freunde. Einen eventuellen Relay-Betrieb hinsichtlich
  tatsächlichem Nutzen, Zugriffsschutz und zusätzlicher Komplexität bewerten,
  nicht als kostenlose Alternative zu einem zustellenden Maildienst darstellen.
- [ ] Ein möglichst wiederholbares Build-/Test-/Deployment-Verfahren planen,
  beispielsweise GitHub Actions und ein versioniertes Container-Image. Den
  tatsächlichen Repository-/CI-Stand berücksichtigen. **Jenkins ausdrücklich als
  Alternative und Lernoption** gegen gehostete CI vergleichen: Wartung, Plugins,
  Build-Ressourcen, Trennung vom Anwendungsbetrieb, Zugänge und Kosten. Nicht
  automatisch beide Systeme oder einen zusätzlichen CI-Server einführen.
  Auslöser, Releasebezug, Rückkehr zu einem bekannten
  Stand, Gesundheitsprüfung und verständliche Fehlerdiagnose beschreiben.
- [ ] Eine komfortable Loganzeige und angemessene Überwachung mitplanen.
  **Grafana mit Loki** beziehungsweise **Grafana Cloud** als etablierte Kandidaten
  gegen eine einfachere Log-/Statusanzeige vergleichen. Selbsthosting und
  verwaltete kostenlose Kontingente hinsichtlich RAM, Speicher, Aufbewahrung,
  tatsächlicher Gesamtkosten und Datenstandort bewerten. Grundbedarf: Logs gezielt
  durchsuchen, Erreichbarkeit und Ressourcen beurteilen, Fehler erkennen. Den
  bestehenden Logging-Vertrag erhalten, keine Geheimnisse oder Mailinhalte in
  externe Logdienste übertragen. Optionale Ausbaustufen vom kleinen Einstieg trennen.
- [ ] Planen, wie der Agent nach einmaliger Einrichtung über geeignete CLI-/API-
  oder Deployment-Zugänge weitgehend selbstständig arbeiten kann. Wiederkehrende
  Klickanleitungen vermeiden. Zugangsrechte passend begrenzen, Geheimnisse sicher
  hinterlegen und nötige einmalige Benutzeraktionen konkret aufführen. Keine
  dauerhafte Erreichbarkeit des Agenten außerhalb eines laufenden Auftrags versprechen.
- [ ] Die Bereitschaft der bestehenden Anmeldung und Sicherheitsabläufe für eine
  öffentliche Demo anhand des aktuellen Codes prüfen. Konkrete Lücken und notwendige
  Änderungen benennen: öffentliche Basisadresse/HTTPS, Cookies/Proxy-Konfiguration,
  Google-/Microsoft-Redirects und Anbietereinstellungen, Passwort-/2FA-/Recovery-
  Abläufe, Mailzustellung, Registrierung, Missbrauchsgrenzen und privilegierte
  Konten. Nicht pauschal behaupten, der lokale Stand sei bereits internetbereit.
- [ ] Datenspeicherung und Betriebsgrenzen für den nun öffentlich erreichbaren
  Einsatz mit echten Benutzerkonten ausdrücklich neu bewerten: H2-Eignung und Alternativen, persistente
  Ablage, Backup/Wiederherstellung, Versionswechsel und Daten-/Schemaverträge.
  Die lokale Regel austauschbarer Testdaten nicht ungeprüft auf fremde Konten
  übertragen. Den Umgang mit echten Konten und mögliche Testdaten klar festlegen.
- [ ] Serverwartung, Updates, Zugriffsschutz, Ressourcenüberwachung und benötigte
  Datenschutz-/Kontaktinformationen als angemessenen Teil des öffentlichen Testbetriebs
  einordnen. Öffentliche Erreichbarkeit von einer bloßen privaten LAN-Vorschau
  unterscheiden, ohne vorsorglich eine große Betriebsplattform zu bauen.
- [ ] Den normalen öffentlichen Registrierungs-/Anmeldeweg nicht durch eine
  zusätzliche Demo-Anmeldung oder vorgeschaltete gemeinsame Zugangsdaten ersetzen.
  Eine bewusst ausgelöste vorübergehende Betriebspause bleibt als Betriebsoption
  planbar. Keine Sicherheit daraus ableiten, dass ein öffentlicher Link noch
  niemandem genannt wurde. Öffentliche Web-Erreichbarkeit, geschützten
  Verwaltungszugang und vollständiges Abschalten unterscheiden. Benötigte Wartung
  eines weiterlaufenden Servers und fortbestehende Kosten bei bloß gesperrtem
  oder gestopptem Dienst erläutern.
- [ ] Die vom Benutzer gewünschte Pause ausdrücklich berücksichtigen: Nach dem
  Vorführen kann die gesamte VM ausgeschaltet bleiben. Dann laufen dort keine
  Dienste, laufende Systemwartung ist in dieser Phase nicht nötig. Vor erneuter
  öffentlicher Freigabe Updates und Zustandsprüfung einplanen. Anbieterabhängig
  fortlaufende Gebühren für die ausgeschaltete VM, Speicher, IP, Domain und
  Backups vom tatsächlichen Löschen/Neuaufsetzen unterscheiden.
- [ ] Eine eigenständige Markdown-Planung mit Anbieterempfehlung, Kostenübersicht,
  notwendigen Vorarbeiten, klaren Einrichtungsschritten und späterem Deployment-
  Ablauf erstellen. Mit der Release-Planung aus Punkt 2 verbinden. Der Benutzer
  liest und bespricht sie vor jeder später beauftragten Umsetzung.
- [ ] Den Anbietervergleich als verständliche Gegenüberstellung mit Vor-/Nachteilen
  und groben monatlichen Gesamtpreisen liefern, nicht centgenau. Einmalige Kosten,
  feste laufende Gebühren, kostenlose Kontingente und mögliche Mehrkosten getrennt
  nennen. Eine kleine empfohlene Einstiegslösung und optionale Ausbaustufen für
  Lernzwecke wie Jenkins/Grafana ausweisen, ohne sie ungefragt umzusetzen.

Erste Quellen für den späteren Vergleich:
[Hetzner Cloud](https://www.hetzner.com/cloud/cost-optimized/),
[Hetzner-Preise](https://docs.hetzner.com/general/infrastructure-and-availability/price-adjustment/),
[GitHub Deployments](https://docs.github.com/en/actions/concepts/workflows-and-actions/deployment-environments),
[Jenkins-Installation und Ressourcen](https://www.jenkins.io/doc/book/installing/linux/),
[Grafana-Angebote](https://grafana.com/pricing/).

### 5. Ablageort der Browser-Testläufe konfigurierbar machen

Dieses Paket nach der Startfreigabe umsetzen. Ziel ist die Entlastung der
Windows-Systemplatte durch ein frei konfigurierbares Laufverzeichnis, etwa
`D:\VaadinSpringApp\browser-runs`. Noch keine vorhandenen Läufe verschieben oder
löschen. Gewünschtes Zielverzeichnis und Umgang mit bestehenden Läufen vor einer
tatsächlichen Bestandsverschiebung festlegen.

- [ ] Einen zentralen lokalen Konfigurationsweg für den Ablageort der Testläufe
  einführen und mit Beispiel dokumentieren. Ein absoluter Pfad außerhalb des
  Projektordners muss unterstützt werden. Keine Junction-/`mklink`-Lösung als
  erforderliche Bedienung vorsehen. Ohne explizite Konfiguration den bisherigen
  Standard eindeutig beschreiben, gesetzte ungültige Pfade nicht still auf C:
  zurückfallen lassen.
- [ ] Starter, Java-Runner, Worker, Metadaten, Allure-Ergebnisse und HTML-Berichte,
  Screenshots, Traces, TLS-Testmaterial und optionale Coverage-Ausgaben auf
  denselben konfigurierten Run-Stamm abstimmen. Die bisherigen Sicherheitsgrenzen
  der Worker-Pfade erhalten und an den gewählten Stamm anpassen, nicht entfernen.
  Getrennte Lauf-/Worker-Verzeichnisse und klassenweise Parallelisierung erhalten.
- [ ] Letzten Bericht öffnen, bestimmten Bericht öffnen, Trace-Viewer und lokale
  Berichtslauncher auch vom externen Ablageort unterstützen. `latest.txt`, relative
  Anhänge und Links sowie nachgelagerte Berichts-/Coverage-Verbraucher prüfen.
  Neue Läufe dürfen nicht weiterhin große Belege im bisherigen C:-Runs-Ordner
  duplizieren. Bestehende Berichte sollen weiterhin gezielt aufrufbar bleiben.
- [ ] Vor einem Lauf Zielpfad, Schreibbarkeit und nötige Verzeichnisse prüfen.
  Pfade mit Leerzeichen und Windows-Laufwerksbuchstaben unterstützen. Benötigte
  Docker-Pfadfreigaben prüfen. Bei nicht erreichbarer D:-Platte verständlich
  abbrechen, statt Ausgaben unbemerkt auf der Systemplatte zu erzeugen.
- [ ] Buildausgaben, Maven-/Browser-Caches und Docker-Speicher vom Runs-Verzeichnis
  unterscheiden. Die Änderung verlagert die Laufartefakte, nicht automatisch alle
  übrigen Entwicklungsdaten. Größenverteilung bei Bedarf gezielt anhand von
  Dateimetadaten prüfen, ohne sensible Inhalte aus Logs oder Datenbanken zu lesen.
- [ ] Vorhandene Läufe nicht ungefragt löschen und keine automatische
  Aufbewahrungs-/Löschregel als zusätzliche Funktion einbauen. Ein vereinbarter
  Bestandsumzug muss nachvollziehbar und ohne Überschreiben erfolgen. Die
  Pfadkonfiguration gehört zum Testwerkzeug, nicht zur Anwendungskonfiguration.
- [ ] Passende Helper-/Runner-/Reportingtests erweitern: externer Stamm,
  Pfadbegrenzung, Report-/Trace-Auflösung und Fehler bei ungültigem Ziel.
  Den tatsächlichen isolierten Ablauf samt Berichtszugriff am neuen Pfad nach
  passender Browserfreigabe gezielt prüfen, keine unnötigen zusätzlichen
  vollständigen Testläufe allein für diese Pfadänderung starten.
- [ ] Testanleitung, Konfigurationsbeispiel und betroffene Aufrufe aktuell halten.
  Anschließend Review der Änderungen einschließlich Lesbarkeit nur der geänderten
  Bereiche und eigener Commit gemäß dem vereinbarten Nachtablauf.

### 6. Optionaler Zwischenmerge, Sonar-Bereinigung und große Testrunde

Erst nach den fünf vereinbarten Paketen und nur bei ausreichendem verbleibendem
Zeitfenster. Alle Zeiten beziehen sich auf **Europe/Berlin** am folgenden Morgen.
Etwa **06:00–07:00 Uhr** ist der gewünschte Orientierungspunkt für den Beginn
dieses optionalen Abschlussblocks. Die große Testrunde muss **spätestens um
09:00 Uhr gestartet** sein, sie muss nicht bis dahin fertig sein. Nicht eigens
bis zu einer Uhrzeit warten, wenn die Voraussetzungen bereits früher erfüllt sind.

- [ ] Vorher aktuellen Testbedarf und bestätigte Bugs erneut prüfen. Beim Sammeln
  dieses Auftrags waren beide Listen ohne offene gezielte Aufgaben beziehungsweise
  bestätigte Fehler. Das ersetzt keine Prüfung der neu entstandenen Änderungen.
  Nötige offene Nachtests zentral erhalten und Fehler vor der Integration behandeln.
- [ ] Paket 5 vor dem optionalen großen Browserlauf fertigstellen und den neuen
  Ablageort gezielt prüfen. Ausreichenden Speicherplatz am gewählten Ziel prüfen,
  keine weitere große Runde mit unbemerkter Ablage auf C: starten.
- [ ] Nach Review und Commits einen sauberen Stand von
  `codex/vaadin-komponentenanpassungen` **lokal nach `main` zwischenintegrieren**.
  Quelle erhalten, kein Push. Build, Tests und isolierten Start gemäß Mergeablauf
  prüfen. Eigene fachlich klare Korrekturen sind vom Nachtauftrag mit Review und
  Commit umfasst; fremde Änderungen nicht ungefragt einbeziehen oder verwerfen.
- [ ] **Dieser Merge bedeutet ausdrücklich keine Abnahme und keinen Abschluss
  der laufenden Vaadin-/Portalumsetzung.** Aktive Pläne, offene manuelle Sichtungen
  und vereinbarte Folgearbeiten erhalten. Kein Release aus Punkt 2 erzeugen.
- [ ] Auf dem tatsächlichen `main` einen vollständigen Sonar-Scan über den
  bestehenden Projekteinstieg ausführen, Serverergebnis und Findings prüfen,
  berechtigte Findings beheben. Zusammengehörige Korrekturen reviewen und
  committen, gezielt prüfen und durch vollständigen Nachscan bestätigen.
  Ziel: keine berechtigten offenen Findings und grünes Quality Gate. Keine
  Prüfregeln abschalten oder Findings allein für eine Null ausblenden.
- [ ] Danach auf einen sauberen Folge-Feature-Branch vom geprüften `main` wechseln,
  alternativ den bisherigen Feature-Branch sauber auf den geprüften Stand bringen.
  Einen freien Namen unter `codex/` verwenden und den tatsächlichen Branch im
  Ergebnis nennen. Kein Umschreiben der bestehenden Branchgeschichte.
- [ ] Uhrzeit und verbleibende Voraussetzungen erneut prüfen. Wenn bis spätestens
  09:00 Uhr sinnvoll startbar, die **große Testrunde** mit passender Testergänzung,
  JUnit-/Helper-/Frontendprüfungen und isolierten Browserläufen **DE und EN** auf
  demselben aktuellen Image durchführen. Der Gesamtauftrag umfasst die erforderlichen
  Testkorrekturen, Reviews und eigenen Commits. Bestehenden noch relevanten Testbedarf
  mit einbeziehen und passende unveränderte Nachweise wiederverwenden.
- [ ] Wird das Zeitfenster zu knapp oder bleiben notwendige Voraussetzungen offen,
  keinen unvollständigen Stand als fertig darstellen. Den optionalen Rest mit Grund
  offen halten. Ein bereits rechtzeitig gestarteter Lauf darf über 09:00 Uhr hinaus
  weiterlaufen. Keine Erfolgsaussagen vor tatsächlichem Abschluss.
- [ ] Ergebnisse in den bestehenden passenden Dokumentationen und im Prüfstand
  festhalten. Zur gemeinsamen morgendlichen Sichtung einen knappen Überblick über
  **alle fünf Pakete und den optionalen Abschlussblock** liefern: umgesetzt/geplant,
  wichtige Entscheidungen, tatsächliche Tests, Sonar, Commits/Branch und offene
  Punkte. Die laufende Gesamtumsetzung weiterhin nicht als abgenommen schließen.

Weitere genannte Aufgaben werden hier in der vereinbarten Reihenfolge ergänzt.

Bestehende offene Arbeiten werden nicht automatisch zu Nachtaufgaben.
