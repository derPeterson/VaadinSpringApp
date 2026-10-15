# Browserabdeckung und offene Punkte

**Runner-Nachprüfung am 15.10.2026: 43 Helper-/Reportertests, 34 Fast-Fälle DE/EN und 100 vollständige DE-Browserfälle mit zwei isolierten Workern erfolgreich.**
Der vorherige Theme-Prüfstand mit 1085 Anwendungs-JUnit-Tests und je 100 DE-/EN-Fällen
bleibt unten als eigener Prüfschritt erhalten.
Bedienung und Aufbau stehen in der [Browser-Testanleitung](README.md).
Wichtige Abschlüsse bleiben unter [Meilensteine](../docs/meilensteine/README.md)
unabhängig von bereinigbaren lokalen Testausgaben erhalten.

## Fast-Modus und isolierte Parallelisierung (15.10.2026)

Branch `feature/browser-tests-fast-parallel`, Ausgangscommit `003f6ac7`;
Runner-/Testmarkierungs- und Dokumentationsänderungen lagen uncommittet vor.
Anwendungsquellen und Styles wurden dafür nicht geändert. Die vorhandenen 100
Browserfälle bleiben erhalten; `fast` ist eine bewusst markierte Teilmenge mit 34 Fällen.
Auswahl, Gründe und Messgrenzen stehen in der [Testanleitung](README.md#schnellprüfung-und-parallele-ausführung).

| Prüfung | Ergebnis |
| --- | --- |
| Java 25 / Helper und Reporter | 43/43; keine Fehler, Fehlschläge oder Skips |
| Fast DE sequenziell | 34/34; Aufbau, Abbau und Bericht erfolgreich |
| Fast DE mit zwei Workern | 34/34; dieselben Fall-/Parameteridentitäten wie sequenziell, keine Doppelungen |
| Fast EN mit zwei Workern und Coverage | 34/34; gemeinsamer JaCoCo-Bericht gegen das geprüfte JAR erfolgreich |
| All DE mit zwei Workern und normalem Image-Build (Cache) | 100/100, 54 bzw. 46 Fälle je Worker; keine Fehler/Skips, identische Image-ID beider Worker, Aufbau/Abbau und gemeinsamer Bericht erfolgreich |
| Absichtlicher Fehler-Probelauf | 1 erwarteter Fehlschlag, Exitcode 1 und roter Gesamtbericht; eigener Abbau erfolgreich |
| Kontrollierter Abbruch nach Aufbau zweier Worker | Exitcode 23 des temporären QA-Helfers; beide eigenen Projekte vollständig abgebaut, `interrupted` samt Endzeit gespeichert |

In der DE-Einzelmessung sank die Zeitspanne aller 34 Fälle von **3:53 auf 2:16 min**;
der gesamte Java-Runner benötigte **4:21 bzw. 2:42 min**. Starter-Vorbereitung davor
ist ausgeschlossen. Der sequenzielle Lauf rief den Image-Build mit Cache auf,
der parallele Vergleich verwendete dasselbe Image mit `-NoBuild`. Dies ist keine
Dauerzusage und kein langfristiger Durchschnitt. Mehr Worker benötigen mehr CPU/RAM.

Geprüft wurde die reale Zwei-Worker-Ausführung. Die Verteilung für 1–4 Worker ist
zusätzlich durch Helpertests abgedeckt; vier gleichzeitige Docker-Umgebungen wurden
hier nicht ausgeführt. Der Abbruchnachweis gilt für kontrollierten JVM-Shutdown,
nicht für einen harten Betriebssystem-/Rechnerabbruch. Die beiden Probeläufe sind
keine bestandenen fachlichen Browserläufe und werden nicht in die Erfolgszahlen addiert.

Der vollständige parallele DE-Lauf benötigte **12:59 min** für den Java-Runner;
die Fallspanne betrug **12:26 min**. Die 100 Fall-/Parameteridentitäten stimmen exakt
mit dem vorherigen vollständigen DE-Lauf überein; alle 34 Fast-Fälle sind enthalten.
Dies ist eine aktuelle Einzelmessung, kein neuer gepaarter Vergleich mit einem
sequenziellen `all`-Lauf. Englisch wurde für den neuen Runner mit `fast` geprüft,
nicht nochmals mit `all`. Nach der Prüfung blieben von den eigenen Testprojekten
keine Container, Netzwerke oder Volumes zurück.

Die Fast-Coverage prüft die technische Zusammenführung serverseitiger Java-Rohdaten.
Es entstehen ein gemeinsamer HTML-/Markdown-Testbericht und nur ein geprüftes JAR,
keine Kopie je Worker. Dies ist keine CSS-/JavaScript-Coverage. Anwendungs-JUnit und
dessen Coverage wurden in dieser Runner-Nachprüfung nicht erneut ausgeführt; der
vorherige vollständige Anwendungsprüfstand folgt unten.

## Vollständige Theme-Prüfung

Branch `feature/theme-css-structure`, Ausgangscommit `e429f86f`;
Theme-/Dokumentationsänderungen lagen bei der Prüfung uncommittet vor.
Zentrale Tokens, Dateiaufteilung und umbenanntes `admin-users.css` sind im frischen
Produktionsimage geprüft. Alle sieben CSS-Dateien werden korrekt ausgeliefert;
ihre geparsten Regeln stimmen trotz Produktionsminifizierung mit dem Quellstand überein.

| Prüfung | Ergebnis |
| --- | --- |
| Java 25.0.4.1 / Shared sauber installiert | Erfolgreich |
| Anwendungs-JUnit | 1085/1085; 0 Fehler, Fehlschläge, Skips |
| Browser-Helper/Reporter | 34/34; 0 Fehler, Fehlschläge, Skips |
| Frischer Produktionsbuild einschließlich JUnit und Frontend | Erfolgreich |
| Browser Deutsch / Englisch | Je 100/100; 0 Fehlschläge oder Skips, identisches App-Image |
| Aufbau, Abbau und Allure-/Markdown-Berichte | Beide vollständig erfolgreich |

JUnit-Coverage: **71,16 % Zeilen** (3689/5184) und
**66,37 % Zweige** (1127/1698); frische Messung.
Gemessen ist serverseitiger Java-Anwendungscode. Browser-Code-Coverage wurde diesmal
nicht erneut instrumentiert; die frühere Messung steht im
[Redesign-Meilenstein](../docs/meilensteine/2026-10-15-redesign-abgeschlossen.md).
Die Fall-Erfolgsquote ist keine CSS-/JavaScript- oder vollständige fachliche Coverage.

[Dauerhafter Theme-Meilenstein](../docs/meilensteine/2026-10-15-theme-struktur-geprueft.md) mit Ergebnissen,
Erkenntnissen und Messgrenzen; [Theme-Anleitung](../docs/theme.md) für die neue Struktur.
Die vollständige Prüfung ist erfolgreich abgeschlossen. Weitere Designkorrekturen
waren in dieser Nachprüfung nicht nötig.

USER-11A/B prüfen Filterentwürfe, Anwenden, einzelne Badges, Datumabhängigkeit und
Scrollen der begrenzten Zeitliste ohne Auswahl. USER-12A/B prüfen die optionale
Passwortbestätigung mit Fehlern erst nach Blur und sichere Schließen-Aktionen.
Beide Sprachläufe verwenden getrennte eigene Docker-/H2-/Mailpit-Umgebungen und Konten.
Die eigenen Container, Netzwerke und Volumes wurden vollständig abgebaut;
IDE-Anwendung und Bestandsdaten waren keine Testziele.

## Gemeinsames Design der Kontoansichten

Login, Registrierung, Verifikation, Passwortanfrage und Passwortänderung verwenden
gemeinsame Java-/CSS-Bausteine, die Palette der Silent-Cosmos-Referenz, animierte
CSS-Dekoration, lokales Inter und übersetzte Platzhalter. Inter gilt global; Home/Admin
verwenden ebenfalls die gemeinsame Cosmos-Palette. Copyright wird über Allgemein gespeichert, seine
Platzhalter werden ersetzt und HTML-Zeichen als Text angezeigt. Die vorherigen
Hintergrundbilder sind aus den aktiven Ressourcen entfernt; die versionierten
Originale bleiben über Git nachvollziehbar. Ein lokaler Backupordner ist aktuell
nicht vorhanden.

| Fälle | Nachweis im vollständigen DE-/EN-Lauf |
| --- | --- |
| COSMOS-01A–E | Alle fünf Ansichten bei 320×568, 390×844, 844×390, 1280×900 und 1280×1200; keine horizontale Abschneidung, gemeinsame Gestaltung, sichtbare Platzhalterschrift und Screenshots |
| COSMOS-02 | Mobile Registrierung, echte Aktivierungsmail, erfolgreiche Verifikation, echte Reset-Mail, Passwortänderung mit Erfolgscard/Häkchen und Anmeldung bei 390×844 |
| COSMOS-03 | Copyright über Admin gespeichert/neu geladen, sicherer Fußtext mit year/serviceName in UI und echten Verifikations-/Resetmails, geladene Inline-Logos und Wiederherstellung; gemeinsame Admin-Palette |
| COSMOS-04 | Schmale Fehlermeldung verwendet Card-Verlauf; Schließen erreichbar, gemeinsame Home-Palette |
| COSMOS-05A–C | Home/Avatar-Popup und Adminnavigation bei drei Größen; Allgemein sowie Benutzerformular innerhalb der Fensterbreite |
| UI-03A–F | Reale Loginbedienung bei fünf Größen einschließlich 360×240, Fehler/Korrektur, Remember-me-Auswahl, Anmeldung und reduzierte Bewegung |

JUnit prüft zusätzlich Copyright-Defaults/Erhalt gespeicherter Werte, sichere Textanzeige,
Notification-Scope sowie wechselnde Registrierungsplatzhalter ohne Verlust der Formulardaten.
Die Anmelde-/Registrierungs-/Tokenaktionen und die Security-Konfiguration wurden gegen
den Ausgangsstand geprüft und blieben unverändert. Der Browser prüft auch die bisherigen
fachlichen Konto-, Admin-, Mail-, Status- und Sicherheitsabläufe.

Sterne behalten 30/45 Sekunden, Nebel 18–24 Sekunden als Animationszeiten.
Reduzierte Bewegung deaktiviert die Animation; kurze Fenster dürfen vertikal scrollen.
Die 100 Browserfälle prüfen konkrete UI-Abläufe. Menschliche Beurteilung und offene
Betriebs-/Integrationsfolgen werden im [neuen Handprüfkatalog](../docs/manual-tests/README.md)
geführt; eine erfolgreiche Browser-Fallzahl bedeutet keine vollständige Betriebsabdeckung.

## Einheitliche Datum-/Uhrzeitbedienung

DatePicker in Registrierung und Benutzerformular sowie Date-/TimePicker im
Aktivitätsfilter verwenden `autoOpen=false`. Direkte Eingabe öffnet kein Overlay;
Icons und die vorgesehenen Pfeiltasten bieten weiterhin Auswahlmöglichkeiten.

Der Geburtstagsplatzhalter entspricht der Registrierung und folgt der Sprache.
Im Aktivitätsfilter ist der Datumsplatzhalter das heutige Datum im DE-/EN-Format;
der Zeitplatzhalter entspricht der aktuellen Uhrzeit, abgerundet auf 15 Minuten.
Die Uhrzeit ist optional und nur bei gültigem Datum bedienbar. Ohne gültiges Datum
wird sie deaktiviert. Öffnen und Scrollen der Zeitliste übernimmt keinen Wert.
Der Geburtstag bleibt beim DE↔EN-Wechsel im Anlegen-/Bearbeiten-Dialog erhalten
(technische Regression).

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
| USER-10 | Gesamtzahl nach Anlage/Löschung, unabhängig von Filtern und mit korrektem Sprachwechsel | Bestanden | Bestanden |
| USER-11A | Filterentwürfe, Anwenden, Panelzustand und einzelnes Badge-X ohne Übernahme anderer Entwürfe | Bestanden | Bestanden |
| USER-11B | Datumabhängige Zeit, aktueller Platzhalter, begrenztes Overlay und Scrollen ohne Auswahl | Bestanden | Bestanden |
| USER-12A | Optionale Passwortbestätigung, exakter Vergleich, Fehler erst nach Blur und Rücksetzen | Bestanden | Bestanden |
| USER-12B | Sichere Schließen-Aktionen, neue Löschtexte und Erhalt des ausgewählten Kontos | Bestanden | Bestanden |

Suche/Pagination, Filter, Anlage, Bearbeitung, Löschabbruch/-bestätigung,
Selbstlöschschutz, Sprachpräferenz und Passwortwechsel bestehen jetzt in beiden
Gesamtläufen. Der Editierbutton wird vor dem echten Mausklick sichtbar gemacht und
fokussiert, damit die native Grid-Scrollbewegung den Klick nicht verliert. Erwartungen,
Speicher-/Anmeldeprüfungen und Sicherheitsregeln bleiben erhalten.

Im aktuellen vollständigen DE-/EN-Lauf sind jeweils alle 100 Browserfälle bestanden.
Noch offene ergänzende Prüfungen stehen im Abschnitt „Ergänzende Handprüfungen“ unten.

## Ergänzte Arbeitspakete

| Paket | Neue UI-Aussagen | Suite / IDs |
| --- | --- | --- |
| 1 | Sperren/Freigeben, gezieltes Aktivieren unverifizierter Konten, Rollenvergabe/-entzug, echte zwei alte Formulare und Selbstschutz | `user-access`: USER-07A–E |
| 2 | Alte privilegierte Callbacks nach Rollenentzug/Sperre/Löschung; alte Verifikations- und Resetbuttons; neutrale gesperrte Anfrage | `session-security`: SESSION-01A–E (einschließlich Remember-me bei Sperre/Löschung); `blocked-links`: SEC-LINK-01A/B und SEC-LINK-02A/B, RESET-03C |
| 3 | Echter Browserneustart mit/ohne Remember-me und nach Logout; Statuswahl, zweiter Tab, automatische Abwesenheit/Rückkehr | `remember-me`: AUTH-07A/B; `user-status`: STATUS-01/02 |
| 4 | Weitere String-/Boolean-/Integerkonfiguration, Unicode-Testmail, echte Tokenabläufe/Resend und Verifikationsanfragefehler | `configuration`: CONFIG-04A–C; `admin-mail`: MAIL-01; `token-expiry`: VER-03A, RESET-06A, VER-02B |

Jeder Fall enthält eigene Schritte und Erwartungen im erzeugten Markdown-Bericht.
Implementierung allein zählt nicht als erfolgreicher Nachweis; Resultate stehen oben.
Die Oberfläche bereitet sämtliche Anwendungszustände vor. Mailpit liefert nur die
eigene tatsächlich versandte SMTP-Mail. Interne Regeln bleiben technische Tests.

## Noch sinnvoll als nächster Schritt

Die vier UI-Pakete sind geprüft. Als Nächstes bieten sich kontrollierte
Betriebsfixtures an: Neustart mit derselben eigenen Testdatenbank, SMTP-Stopp und
Wiederanlauf sowie deren Bericht. Weitere Browserpakete betreffen Sprachwechsel bei sichtbaren Admin-Feldfehlern,
neue Mails desselben Kontos nach Sprachwechsel und unterbrochene Verbindungen. Schmale Home-/Adminansichten sind durch
COSMOS-05A–C, der mobile Konfigurations-Tooltip durch CONFIG-05F geprüft.
Die fünf Kontoansichten einschließlich Reset sind inzwischen responsiv geprüft.
Queue-/Audit-Innenzustände werden dafür nicht in gewöhnliche UI-Tests gepresst.

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

## Ergänzende Handprüfungen

Der aktuelle [Handprüfkatalog](../docs/manual-tests/README.md) enthält ausschließlich
menschliche Abnahmen und noch offene ergänzende Testfolgen. Bereits automatisierte
Normalabläufe werden dort nicht nochmals als Handtest geführt. Eine Mail oder ein
Testkonto darf als Vorbereitung vorkommen; geprüft wird nur die benannte Restlücke.

| Restumfang | Fälle im Handprüfkatalog | Nächster geeigneter Nachweis |
| --- | --- | --- |
| Gestalterische Qualität, echtes Mobilgerät, Tastatur/Zoom, Screenreader, Texte und tatsächliche Mailclients | HUM-01–06 | Menschliche Beurteilung auf den angegebenen Geräten/Hilfsmitteln/Clients |
| Recovery bei unvollständigen Versandbelegen | HUM-07 | Menschliche Schreibtischübung zum Recovery-Vertrag, keine Datenänderung |
| Gleiche Datenbank nach Serverneustart und Wirksamkeit startgebundener SMTP-Änderungen | AUTO-01/02 | Zur Automatisierung vorgesehen – kontrollierte Betriebsfixture |
| Realer SMTP-Stopp/Wiederanlauf, begrenzter dauerhafter Fehler und brauchbare Diagnose | AUTO-03/04 | Zur Automatisierung vorgesehen – Betriebsfixture mit korrelierten Belegen |
| Sichtbare Feldfehler beim Sprachwechsel, neue Mails nach Sprachwechsel desselben Kontos und Offline-/Online-Folge | AUTO-05–07 | Zur Automatisierung vorgesehen – echte UI-/Browserfälle |
| Vorgesehener SMTP-Transport einschließlich Auth/TLS und tatsächlichem Testpostfach | AUTO-08 | Zur Automatisierung vorgesehen – gesonderte freigegebene Integrationsfixture |

Diese Zuordnung ist ein offener Prüfauftrag, kein bestandener Nachweis. Die neue
[leere Vorlage](../docs/manual-tests/vorlage.md) enthält Schritte und Erwartungen;
das [ausgefüllte Lernbeispiel](../docs/manual-tests/vorlage_ausgefüllt.md) enthält
**ausschließlich erfundene** Ergebnisse und Korrekturen. Fehlende Werkzeuge/Belege
werden als BLOCKIERT behandelt, nicht als bestätigter Anwendungsfehler.

Bereits durch JUnit/Persistenz belegte interne Zustände, exakte Ablaufgrenzen,
Cleanupregeln und Konkurrenz werden nicht zu manuellen Datenbankprüfungen dupliziert.
Bestandsmigrationen, Mehrinstanzbetrieb und unklare Transport-/Commitausgänge bleiben
in ihren eigenen technischen/Betriebsanleitungen und benötigen passende Fixtures;
der neue kurze Katalog behauptet dafür keine vollständige Freigabe.

## Belege und Pflege

Hier nur den aktuellen Prüfstand, Fallzuordnungen und offene Grenzen pflegen.
Wichtige Abschlüsse eigenständig lesbar unter [Meilensteine](../docs/meilensteine/README.md)
festhalten: Datum, geprüfter Stand, Ergebnisse, Coverage und wesentliche Erkenntnisse.
Keine konkreten lokalen Run-Pfade oder Listen früherer Testläufe als dauerhafte Belege eintragen.

Lokale Laufordner mit Screenshots, Traces, JARs und HTML-/Coverage-Berichten dürfen
nach Auswertung bereinigt werden. Ihre dauerhafte Aufbewahrung oder vollständige
Sicherung ist nicht erforderlich. Die Links innerhalb eines Run-Berichts gelten nur,
solange der zugehörige Ordner vorhanden ist. Keine automatische Löschung ohne Auftrag.

Die Weiterleitung unter `docs/browser-tests/README.md` verweist weiter auf die
aktuelle Browseranleitung. Die manuelle Ergänzung wird ausschließlich im
[Handprüfkatalog](../docs/manual-tests/README.md) gepflegt, ohne alte Run-Verweise.
