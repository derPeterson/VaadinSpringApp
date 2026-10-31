# Große Testrunde – 31.10.2026

## Prüfstand und Ergebnis

Geprüft wurde `feature/google-microsoft-anmeldung` auf Basis von
`782a971906b387d3b90ba26f13ba855215131def` einschließlich der uncommittierten
Feature- und QA-Korrekturen. Der lokale Quell-/Testfingerabdruck lautet
`8b891c16484c626451988272522b9cc25c29440e534240e245742511e839265c`.

Vergleichsstand war die bisherige Design-QA zu
`ee4861abdb537246896d18cec95c20a6b7333c2c` samt damaligen uncommittierten
Nachträgen. Dafür lag kein vollständiger Quellfingerabdruck vor. Maßgeblich waren
deshalb der tatsächliche Diff, die neuen Quellen und die aktuellen Testberichte.

Der automatisierte Testumfang ist nach den unten beschriebenen Korrekturen und
gezielten Nachtests bestanden. Keine bestätigten Anwendungsfehler sind offen.
Ein aktueller Sonar-Nachweis für diesen Featurestand fehlt weiterhin. Der Stand
ist damit noch keine vollständige Qualitätsfreigabe oder gemeinsame Featureabnahme.
Es wurde weder committet noch gemergt oder veröffentlicht.

Der aktuelle Arbeitsstand enthält zusätzlich die dokumentierten Startvorgaben in
`.env.example` mit ausdrücklicher Properties-Zuordnung sowie die geführte
Adminverwaltung unter Konfiguration / Anmeldeanbieter. Dafür bestanden **77 gezielte
JUnit-Fälle** für Konfiguration, Persistenz, Rechte, veraltete Formulare, laufende
Anbieteranfragen, Decoderwechsel und Navigation. Der optimierte Testimage-Build,
Spotless und Checkstyle waren erfolgreich. Die Anbietersuite bestand anschließend
mit **19 Browserfällen je DE und EN** auf demselben Image
`sha256:2e1f8357b2cc8970658ffefa8418fa4865b0daa8041451b1436d22c1f42b96d7`.
Die neue Einrichtung wurde auf Desktop und Smartphone geprüft. Auch ein Neustart
mit beiden Anbietern deaktiviert und die erneute Freigabe erhielten die Einstellungen.
Die Testumgebungen wurden erfolgreich abgebaut. Die oben beschriebene große
Gesamtrunde liegt vor diesen Ergänzungen und wird durch die gezielten Prüfungen
nicht ersetzt. Echte Anbieterzugänge, gemeinsame UI-Abnahme und aktueller Sonar-Nachweis
bleiben offen.

## Ausgeführte Prüfungen

| Prüfung | Beobachtetes Ergebnis |
| --- | --- |
| Vollständiger Root-Reactor, Production und Coverage | 2.350 Anwendungstests erfolgreich, keine Fehler, zwei erwartete optionale Skips |
| JUnit-Browser-Helfer | 85 erfolgreich |
| Browser `all`, Deutsch | 187 von 187 erfolgreich, keine Skips |
| Browser `all`, Englisch | 186 erfolgreich, ein Kalender-Testhelferfehler; anschließend gezielt behoben und nachgeprüft |
| Browser `user-filters`, nach Testhelferkorrektur | Deutsch 5/5 und Englisch 5/5 erfolgreich |
| Frontend-JavaScript | 12 erfolgreich |
| Sonar-Offlineprüfungen | 47 erfolgreich |
| Coverage-Offlineprüfungen | 18 erfolgreich |
| Formatierung, Spotless und Checkstyle | Erfolgreich, keine Checkstyle-Verstöße |
| Ruff für die fünf gepflegten Python-Helfer | Formatprüfung und Lint erfolgreich |

Damit sind alle 187 Browser-Szenarien je Sprache am selben Anwendungsimage
abgedeckt. Die Filter-Nachtests wiederholen bestehende Szenarien und sind keine
zusätzlichen zehn unterschiedlichen Fälle. Der englische Gesamtlauf wurde nach
der ausschließlichen Testhelferkorrektur nicht vollständig wiederholt: betroffen
war nur USER-05C, dessen gesamte Filtersuite in beiden Sprachen nachgeprüft wurde.

Das identische Image aller maßgeblichen Browserläufe war
`sha256:69d6c175ddf4190a073c9d2bb50f1e8384942f0f2a49b52d566e66cbefb4503d`.
Die Anwendung blieb während dieser Läufe unverändert. Die Gesamtläufe nutzten vier
klassenweise getrennte Worker mit eigenen Datenbanken und Testdiensten. Innerhalb
eines Workers liefen die Fälle sequenziell. Chromium wurde in Deutsch und Englisch
mit Desktop- und gezielten Smartphonefällen geprüft.

Der vollständige Reactor lief mit `clean verify`, `-Pproduction,coverage` und
`-Dvaadin.productionMode=true` unter JDK 25. Das wirksame Maven-Modell und die
Coverage-Verifikation bestätigten Quellen-, Klassen- und Ausführungsdatenbezug.
Die JaCoCo-Zeilenabdeckung beträgt **87,67 % für App** und **100 % für Shared**.
Es wurde keine zusätzliche serverseitige Browser-Coverage erhoben.

Die zwei erwarteten Skips gehören ausschließlich zu den opt-in IDE-/Caddy-Fällen
in `IdeHttpsProxyTest`. Der eingefrorene OpenCode-Workflow wurde nicht mitgeprüft.

## Behobene Anwendungsfehler

1. **Kontosprache nach externer Registrierung:** Nach einem Sprachwechsel vor dem
   Abschicken wurde die anfängliche statt der zuletzt gewählten Sprache gespeichert.
   Die Registrierung übernimmt jetzt die aktuelle Sitzungssprache. OIDC-01 prüft
   die gespeicherte Sprache einschließlich erneutem Öffnen auf Desktop und Smartphone
   in beiden Ausgangssprachen. Ein Portalpasswort bleibt bei externer Registrierung unnötig.
2. **Sitzungsrennen bei Abmeldung oder Widerruf:** Ein später HTTP-Abschluss und das
   Lesen eines bereits geladenen, inzwischen gelöschten Anmeldekontexts konnten HTTP 500
   auslösen. Außerdem konnte die direkte Zerstörung der laufenden Vaadin-UI den
   Antwortaufbau beschädigen. Der abschließende Commit schreibt eine gelöschte Sitzung
   nicht zurück. Ein verschwundener Anmeldekontext wird gezielt verworfen, alte
   geschützte Aktionen werden abgewiesen und die Anmeldung bleibt erreichbar.
   Die UI wird über Vaadins nativen Binding-Lifecycle nach der laufenden Antwort abgebaut.
3. **Gespeicherte Anmeldung nach Tokenabweichung:** Bei Sperre/Löschung und auslaufenden
   Remember-me-Anfragen konnte Springs `CookieTheftException` als HTTP 500 weitergereicht
   werden. Der vollständige Tokenwiderruf und die Cookieentfernung bleiben erhalten.
   Es wird keine Anmeldung wiederhergestellt. Danach übernimmt die normale Zugriffskontrolle
   den erneuten Anmeldeweg. Eine Tokenabweichung wird ohne sensible Inhalte protokolliert.

Die letzten beiden Fehler wurden auch durch Kontrollen der eigenen Testserver entdeckt,
obwohl sichtbare Browserabläufe teilweise bereits bestanden hatten. Die entsprechenden
Fehler traten in den Kontrollen am korrigierten Image nicht mehr auf.

## Regressionen und Teststabilität

Sieben zusätzliche JUnit-Szenarien sichern verspäteten HTTP-Commit, fehlende Wiederbelebung,
den laufenden Vaadin-Antwortabschluss, gelöschte Anmeldekontexte an HTTP-/Socket-Grenzen
und vollständigen Tokenwiderruf bei Cookie-Wiederverwendung ab. Ein gezielter Lauf mit
133 Sicherheits-/Sitzungsfällen sowie der vollständige Reactor waren erfolgreich.
SESSION-01A–E prüfen zusätzlich, dass Rechtewechsel, Sperre und Löschung keine
HTTP-500-Antworten erzeugen. OIDC-01 enthält den Sprachwechsel-Nachweis.

Zwei Testabläufe wurden ohne Wiederholungsversuche oder schwächere Assertions stabilisiert:

- SESSIONS-04 trennt den dauerhaften Sitzungsnachweis von konkurrierenden alten
  Push-Navigationen. Die laufende Seite wird vor dem Serverneustart verlassen.
  Erhaltene Sitzungen und Widerruf werden weiterhin nach beiden Neustarts geprüft.
  SESSIONS-06 prüft den Live-UI-Neuaufbau separat und bestand ebenfalls in DE/EN.
- USER-05C bestätigte eine per Pfeiltasten gewählte Kalenderzelle per Mausklick,
  während sich am Monatswechsel das Gitter noch bewegte. Der Helfer bestätigt jetzt
  mit Enter. Exaktes Datum, geschlossenes Overlay, Wiederöffnen der Auswahl und
  tatsächliche Trefferfilterung bleiben geprüft. Der Helfer hat nur diesen einen Aufrufer.

Die Fehlversuche wurden nicht ausgeblendet. Ihre lokalen Diagnosedaten bleiben erhalten;
konkrete Runzuordnungen und Quellfingerabdrücke stehen im ignorierten QA-Arbeitszustand.

## Sonar und verbleibende Grenzen

Sonar wurde ausschließlich lesend geprüft. Verfügbar war die main-Analyse
`f388685c-cc43-4e6d-9e70-69658e34193d` vom 31.10.2026 zur Revision
`35205f7b255e413e205e2cb687b1a5c1eecca164`: damaliges Gate OK, keine offenen Issues
oder Hotspots. Diese Revision entspricht weder dem lokalen main noch dem aktuellen
Featurestand. Daraus folgt kein aktuelles grünes Gate für dieses Paket. Auf dem
uncommittierten Featurebranch wurde kein Sonar-Upload ausgeführt und keine Ausnahme geändert.

Die OIDC-Fälle verwenden einen eigenen Anbieter-Simulator. Die tatsächliche Google- und
Microsoft-Anmeldung mit selbst eingerichteten Clients, privaten sowie Firmen-/Schulkonten
und echter interaktiver Freigabe bleibt unter [HUM-08](manual-tests/README.md#hum-08)
offen. Ebenfalls offen bleiben die gemeinsame UI-/Textabnahme, echte Mobilgeräte,
Bildschirmtastatur, Zoom und Hilfsmittel gemäß [manuellem Katalog](manual-tests/README.md).
Externe SMTP-Provider und echter Mail-Sichtversand gehören nicht zu dieser Runde.

Alle eigenen Browser-Testumgebungen wurden erfolgreich abgebaut. Entwicklerdaten und
fremde Prozesse wurden nicht verwendet. Versionierte generierte Frontenddateien wurden
nicht übernommen. Die Korrekturen sind im getesteten Build enthalten. Eine laufende
Entwicklerinstanz benötigt einen Neustart mit dem neuen Build.

Offene Aufgaben stehen in [TODO](../TODO.md), bestätigte Anwendungsfehler in
[BUGS](../BUGS.md) und automatisierter Ergänzungsbedarf im [Testbedarf](testbedarf.md).
