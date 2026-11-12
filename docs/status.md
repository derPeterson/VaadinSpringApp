# Große Testrunde vom 12.11.2026

Geprüft wurde der Nachtauftrag auf `codex/vaadin-komponentenanpassungen`,
aufbauend auf `fee0cf64`. Der letzte vollständig zugeordnete QA-/Sonar-Vergleich
bleibt `main` auf `9c0d53e8`. Die Pflichtpakete wurden einzeln umgesetzt, geprüft
und committet. Der finale Anwendungsstand ist `866a33d1`; danach änderten sich
nur die Lade-Synchronisierung des Browser-Page-Objects und die Dokumentation.
Es gab keinen Merge, Push oder echten Mailversand.

## Ergebnisse

| Prüfung | Ergebnis |
| --- | --- |
| Optimierter Maven-Reactor mit Java 25 und Coverage | Bestanden |
| Spotless, Checkstyle und Diff-Prüfung | Bestanden |
| Anwendungstests | 2.908 bestanden, zwei bewusst optionale Caddy-Tests übersprungen |
| Browser-Helfertests | 100 bestanden, nach der letzten Testkorrektur erneut bestanden |
| Frontend-Offlinetests | 20 bestanden |
| Coverage- und Sonar-Offlineprüfungen | 18 und 47 bestanden |
| Wirksames Maven-Modell und Coverage-Prüfer | Bestanden, App 84,51 %, Shared 100 % Zeilenabdeckung |
| Browser `all` DE | 289 von 289 bestanden, keine Fehler oder Skips |
| Browser `all` EN | 288 von 289 bestanden; ein Lade-Synchronisierungsfehler im Test, siehe Nachtest |
| Gezielter Nachtest `remember-me` DE | Alle vier Fälle bestanden |
| Gezielter Nachtest `remember-me` EN | Alle vier Fälle bestanden |
| Sonar für den aktuellen Branchstand | Nicht bestätigt; nur ältere main-Analyse lesbar |

Die beiden Gesamtläufe und die gezielten Nachtests verwendeten dasselbe
unveränderte Image
`sha256:e7ee1d4b2db9ced4ae5fb0afa99cfae7a08c504fd690cb4311b9ac0283fe1eb1`.
Die Gesamtläufe liefen mit je vier getrennten Anwendungs-, H2-, HTTPS- und
Mailpit-Umgebungen. Aufbau und Abbau beider Gesamtläufe und Nachtests waren erfolgreich.
Die Starts und das erweiterte Schema wurden mit frischen isolierten Daten geprüft.
Bestehende Entwicklerdaten dienten nicht als Testfixture und blieben unverändert.

Der vollständige Reactor prüfte den finalen Java-Anwendungscode. Die danach
angepasste Entwicklungskonto-Badgegestaltung wurde im finalen Image neu gebaut
und in beiden Sprachen geprüft. Die letzte reine Testkorrektur baut kein neues
Anwendungsimage; ihre Stil- und Helferprüfungen sind gesondert erfolgreich.

### Ursache und Nachtest des englischen Ladefehlers

`AUTH-07B` erreichte vor der Anmeldung im frisch gestarteten separaten Chromium
bereits `/login`, aber der clientseitig gesetzte Seitentitel war innerhalb des
fünfsekündigen Assertionsfensters noch leer. Die Trace zeigte anschließend das
korrekt geladene Formular. Das war vor dem eigentlichen Remember-me-Neustart,
nicht ein fehlgeschlagener Wiederanmeldevorgang.

`LoginPage.open()` wartet jetzt mit dem bestehenden begrenzten Locator-Timeout
auf das sichtbare E-Mail-Feld. Danach werden URL, Titel, beide Eingaben und
Überschrift unverändert geprüft. Keine Assertion wurde entfernt, kein erneuter
Loginversuch und keine feste Pause eingeführt. Die vier echten Browser- und
Anwendungsneustartfälle sind in DE und EN auf demselben Image erfolgreich nachgeprüft.
Der fehlgeschlagene englische Gesamtlauf wird dadurch nicht rückwirkend als
vollständig grüner Lauf ausgewiesen.

## Ergänzte und korrigierte Abdeckung

- Entwicklungskonten: Kennzeichen bei der Anlage, Superadmin-Rechte, unabhängiger
  E-Mail-Nachweis, zentrale Versandunterdrückung und sichtbare Hinweise. Normale
  ungeprüfte Konten bleiben davon getrennt; Sicherheitsnachweise werden nicht umgangen.
- Mailvorlagen: Alle zehn Vorlagenpaare im Cosmos-Stil auf Deutsch und Englisch,
  Aktions-URL identisch zum Button, vollständig kopierbar und ohne Überlauf auf
  Desktop-/Smartphonebreite. Vier Kontostatus-Mails stammen jetzt aus MJML/HTML.
- Kontoaktionen: gelbe Deaktivierung, rote Löschung, Passwort-/2FA-Bestätigung,
  bewusster Klick, einmalige Rückmeldung nach Abmeldung und ehrliche Mailzusage.
  Fehler vor Commit dürfen keine teilweise Löschung oder falsche Erfolgsmeldung ergeben.
- Reaktivierung: erster Passwort-/Anbieternachweis, eingeschränkte Hinweisseite,
  Frist und Wartezeit, erneuter Versand, einmalige Linkbestätigung, direkte URLs
  und Refresh. Administrative Sperren bleiben von Selbstreaktivierung ausgeschlossen.
- Löschintegrität: umfangreich verknüpfte Konten über Selbstlöschung, administrative
  Löschung und Fristablauf. Reales H2-Schema, Sitzungsattribute, Nachweise, Tickets,
  Benachrichtigungen und Versanddaten einschließlich Rollback. Daten anderer Konten
  bleiben erhalten; gemeinsame Empfängerzahlen und offene Zuweisungen werden korrigiert.
- Fristen: eigene Rubrik „Kontoverwaltung“, Tagesfelder, DE-/EN-Feldhilfen,
  Zugriff ausschließlich für Superadmins, auch im Dienst und bei direkten URLs.
- Formularschutz: native Browserwarnung bei geändertem Profil, Konfiguration und
  Benutzerformularen. Neuladen, Tab-Schließen, externe Navigation, Zurücknehmen,
  Speichern und fehlgeschlagenes Speichern sind abgedeckt. Filter bleiben ausgenommen.
- Bestehender offener UI-Testbedarf: Verifikationsseite und automatischer Versand
  nach administrativer Adressänderung, Benutzer-Anmeldungsbereich, Identitätstexte,
  Headerlinks, Cursor und Hover in beiden Sprachen und bei beiden Bildschirmbreiten.

Während der Runde wurden ein requestgebundener Zugriff bei Ticket-Pushaktualisierung,
eine falsche Versandzusage bei noch offenem Auftrag und die rote Dialoggestaltung
korrigiert. Veraltete Testannahmen zu Kontofreigabe, Verifikationssperre und gültigen
Reaktivierungslinks wurden mit den bestehenden Sicherheitsverträgen abgeglichen.
Die Remember-me-Auswahl wartet im Test auf das tatsächlich gespeicherte Auswahlcookie.
Reviews umfassten fachliche Grenzen und die Lesbarkeit der eigenen Änderungen
entsprechend der ausdrücklich begrenzten Nachtfreigabe.

## Grenzen und offene Arbeiten

### Autofill beim Bearbeiten des eigenen Kontos

Reproduktion: Mit gespeichertem Browserpasswort in der Benutzerverwaltung das eigene
Konto öffnen und ohne Eingabe schließen. Der Browser hatte das Passwortfeld schon
gefüllt, sodass der Änderungsschutz einen tatsächlich geänderten Formularwert sah.
Die Kennzeichnung ist auf `autocomplete="new-password"` korrigiert, das E-Mail-Feld
verwendet `email`. Unverändertes Schließen, echte Eingaben und Zurücknehmen sind
gezielt per JUnit abgesichert. Der Nachtest mit dem echten Passwortmanager bleibt
unter [HUM-11](manual-tests/README.md#hum-11) offen, ebenso das dort beschriebene
Autofill bei der Registrierung. Ein isoliertes leeres Testprofil ersetzt ihn nicht.

### Versand mit unklarem Ergebnis

Ein Auftrag mit unklarem SMTP-Ausgang bleibt bewusst auf `IN_PROGRESS`. Die erneute
Anforderung wird jetzt ehrlich abgelehnt, statt einen neuen Versand zu behaupten.
Eine sichere administrative Auflösung bleibt offen; ein möglicherweise laufender
Versand wird nicht automatisch zurückgesetzt. Details stehen in
[BUGS](../BUGS.md) und im [Mailqueue-Vertrag](mailqueue.md#ergebnisse-und-wiederholung).

### Sonar, optionale Umbauten und menschliche Sichtung

Die lesende Sonar-Abfrage war erfolgreich: Analyse vom 10.11.2026 für `9c0d53e8`,
null offene Findings, null offene Hotspots und Gate OK. Diese ältere Analyse
passt nicht zum aktuellen Feature-Branch. Kein neuer Scan oder Statuswechsel
wurde ausgeführt; ein grünes Gate für den Nachtstand ist damit nicht belegt.
Die aktuelle QA-Arbeitsablage bleibt deshalb getrennt von der alten Erfolgsbaseline.

Die integrierte Einheitenauswahl und die einheitlichen Seitenpfade sind umgesetzt.
Die Gestaltung steht im [Theme-Standard](theme.md); die gemeinsame Sichtung der
[Seitenpfade](planung/seitenpfade.md) bleibt offen. Ihre gezielte Suite
`breadcrumbs` ist in DE/EN mit jeweils drei erfolgreichen Browserfällen geprüft,
einschließlich Formularschutz, Sprachwechsel und gesperrter Sicherheitsabläufe.

### Aktueller gezielter Komponenten-Nachtest

Der reguläre Testimage-Build ist mit **2.940 bestandenen Anwendungstests** und zwei
bewusst optionalen, übersprungenen Caddy-Tests erfolgreich. Veraltete Testannahmen
zur administrativen Sperre und E-Mail-Freigabe wurden getrennt korrigiert;
Sperr-, Konkurrenz- und Mindestrollenprüfungen bleiben erhalten. Fehlende
Vaadin-Testkontexte und die Erwartung an den getrennten Seitenpfad sind berichtigt.
**104 Browser-Helfertests**, Spotless und Checkstyle sind erfolgreich.
Die abschließenden reinen Namens- und Gliederungskorrekturen sind zusätzlich mit
180 gezielten Anwendungstests und erneut mit den 104 Browser-Helfertests geprüft;
das im Browser geprüfte fachliche Verhalten bleibt unverändert.

Die Suites `component-follow-up` und `segmented-selection` decken zusammen
**43 unterschiedliche Browserfälle je Sprache** ab. DE und EN sind erfolgreich
auf demselben unveränderten Image
`sha256:e83bf4e59cf2a63f6f2d803355759e4a8708e5f467aced3fb66db749d432fba0`
geprüft, einschließlich Desktop- und Smartphonebreite. Ein deutscher Test verwendete
nach dem Sprachwechsel noch die vorherige Feldbeschriftung. Nach Korrektur dieses
Locators sind alle vier Fälle der betroffenen Klasse erneut erfolgreich; der erste
DE-Lauf wird dadurch nicht rückwirkend als vollständig grün ausgewiesen.

Abgedeckt sind Zeitfeldvarianten einschließlich Tastatur, Fehlern, Disabled,
Sprachwechsel und genauer Speicherung, Details ohne Hover, administrative Sperre
gegenüber Selbstdeaktivierung, Verifikations-/Reaktivierungshinweise, Konto-Cards
und Footer, sekundäre Passworthinweise sowie beide Kontotypen. Ein echter
Darstellungsfehler wurde behoben: Die Einheitenauswahl ist vier Pixel breiter,
damit „Sekunden“ vollständig sichtbar bleibt.

Die segmentierten Umschalter verwenden native Tabs im gemeinsamen OTP-Stil:
Profilbild, Posteingang, Glockenvorschau sowie Benutzer- und Ticket-Nachrichtenfilter.
Filterwirkung, Zähler, Auswahl beim Sprachwechsel, Tastatur und Handzeiger sind
geprüft; Vorher-/Nachheraufnahmen wurden verglichen. Normale Seitenreiter und
vertikale Radioauswahl bleiben unverändert.

Alle acht Aktionsmailvorlagen sind als kanonische HTML-Vorschau in beiden Sprachen
und Breiten mit langer vollständig auswählbarer URL geprüft. Die zwei Vorlagen
ohne Aktion enthalten keine leere Linkbox. Empfangene Mails aus den tatsächlichen
Verifikations-, Wiederherstellungs- und Kontoabläufen werden zusätzlich über das
isolierte Mailpit geprüft. Die Vorlagenprüfung ersetzt weder reale Mailprogramme
noch einen eigenen vollständigen SMTP-Gesamtlauf für jede Vorlage.

Aufbau und Abbau der isolierten Umgebungen sind erfolgreich; die laufende
Entwicklungsanwendung und ihre Daten blieben unverändert. Dies ersetzt keinen neuen
vollständigen Browser-Gesamtlauf oder Sonar-Scan. Erledigter automatisierter
[Testbedarf](testbedarf.md) ist entfernt; die gemeinsame Sichtung und die übrigen
zurückgestellten Pakete bleiben im
[Komponentenplan](planung/vaadin-komponenten-abgleich.md).

Browsernachweise verwenden Chromium bei Desktop- und Smartphonebreite; echte
Mobilgeräte, Passwortmanager und Mailprogramme bleiben im
[manuellen Katalog](manual-tests/README.md). Der native Formularschutz ist kein
Autosave und schützt nicht zuverlässig vor Absturz oder erzwungenem Prozessende.
Für die aktuellen Umschalter sind Build, Anwendungsneustart und Browser-Refresh
nötig; sie benötigen keine Datenbankänderung. Die gemeinsame Sichtung der neuen
Komponenten bleibt offen.
