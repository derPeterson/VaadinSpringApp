# Vertrag: Eigene Sitzungen und gespeicherte Anmeldungen

> Aktualisierung vom 21.10.2026: Der unten beschriebene frühere Verlust aktiver
> Sitzungen bei Serverneustart ist durch die ausdrücklich beauftragte dauerhafte
> Anmeldung ersetzt. Maßgeblich ist der [aktuelle Neustart-/Speichervertrag](durable-session-contract.md).
> Frühere Prüfzahlen beschreiben den damaligen Stand. Eine gültige Sitzung zählt
> jetzt auch nach Serverneustart weiter; die Vaadin-Oberfläche wird frisch aufgebaut.


Stand: 17.10.2026. Implementierung auf `feature/account-session-management`.
Die freigegebene Gesamtrunde begann am 17.10.2026 nach 08:00 Uhr Europe/Berlin;
JUnit, Produktionsbuild und vollständige Browserläufe DE/EN sind erfolgreich abgeschlossen.
Manuelle Nutzerabnahme und Übernahme in Main stehen noch aus.

## Umfang und Oberfläche

Der bestehende Tab „Geräte & Sitzungen“ zeigt echte Einträge statt der drei
Entwurfszeilen. Quelle aktiver Sitzungen bleibt das bestehende Spring-SessionRegistry.
Anzahl und das aktuelle `max.sessions.per.user` werden beim Laden, beim Tabwechsel,
nach Aktionen und mit „Aktualisieren“ erneut gelesen. Profilentwürfe bleiben erhalten.

Jeder Eintrag nennt die grob erkennbaren Browser-/Betriebssystemangaben,
Anmeldezeit und letzte bekannte Aktivität. Unbekannte Angaben erscheinen neutral.
Aktivität wird bei HTTP-Dokumenten und Vaadin-UIDL aktualisiert; Heartbeats und
Push-Hintergrundverkehr allein simulieren keine menschliche Aktivität. Zeitangaben
verwenden die gewählte Oberflächensprache und die Serverzeitzone. User-Agent-Angaben
sind Näherungen, kein sicherer Gerätenachweis. Keine IP-Ortung oder Fingerprints.

Aktuelle Sitzung und zugehörige Tabs bleiben bei „Alle anderen abmelden“ erhalten.
„Aktuelle Sitzung“ bleibt deaktiviert. Andere aktive Sitzungen lassen sich einzeln
abmelden. Gespeicherte Anmeldungen ohne aktive Sitzung werden separat innerhalb
derselben Liste als solche benannt und lassen sich mit „Zugang widerrufen“ entfernen.
Sie zählen nicht zum aktiven Sitzungslimit. Es gibt keine statischen Beispieldaten mehr.

Bestehende Cosmos-Komponenten, Icons, Badges und Footerflächen werden weiterverwendet.
Die lokale Account-CSS wird auch für die eingeschränkte Loginansicht geladen;
globale Styles und die Stylesheetdateien wurden nicht verändert. UI-Texte und
Notifications liegen im gemeinsamen typisierten DE/EN-Katalog. Hinweise sind pro
Vaadin-UI gespeichert: Eine Aktion oder ein Sprachwechsel in einem anderen
Browserfenster darf den eigenen Hinweis weder schließen noch verändern. Das gilt
auch für mehrere Tabs derselben HTTP-Sitzung. Es gibt keinen anwendungsweiten
veränderlichen Notification-Zustand mehr.

## Gezielter Widerruf und Konkurrenz

`DeviceRememberMeServices` erweitert Springs vorhandene persistente Remember-me-
Authentifizierung; Tokenprüfung, Rotation, Gültigkeit und Diebstahlerkennung bleiben
beim Framework. Eine erfolgreich geprüfte Tokenserie wird serverseitig ihrer
HTTP-Sitzung zugeordnet. Bei einer neuen Passwortanmeldung wird der bestätigte alte
gespeicherte Zugang dieses Browsers ersetzt beziehungsweise bei abgewähltem
„Angemeldet bleiben“ entfernt. Normales Logout widerruft nur den eigenen Zugang.

Die neue JDBC-Tabelle `remembered_device` ergänzt `persistent_logins` um zufällige
öffentliche Kennungen, Browser-/Systemhinweise und Erstellungszeit. Die Spalte
`persistent_logins.username` wird bei Bedarf von Springs ursprünglichen 64 auf mindestens 255 Zeichen
erweitert, passend zum bestehenden E-Mail-Feld. Vorhandene Tokens bleiben erhalten;
bereits längere Spalten werden nicht verkleinert. Sie enthält
keine Passwörter und keine zusätzlichen Authentifizierungstokens. UI-Daten enthalten
keine Servlet-Sitzungskennungen, Tokenserien oder Cookieinhalte. Die zufälligen
Zeilen-/Aktionskennungen berechtigen allein zu keiner Aktion: Jede Abfrage und jeder
Widerruf wird erneut auf das serverseitig bestimmte eigene Konto begrenzt.

Einzelwiderruf entfernt zunächst die passende persistente Tokenserie samt Metadaten;
erst bei erfolgreicher Datenbanktransaktion werden die zugehörigen anderen aktiven
Sitzungen als widerrufen markiert. Bei „Alle anderen“ bleibt ein ausschließlich
zur aktuellen Sitzung gehörender gespeicherter Zugang erhalten. Teilen mehrere
Sitzungen ausnahmsweise dieselbe Tokenserie, wird diese gemeinsam widerrufen, damit
die andere Sitzung nicht automatisch zurückkehren kann; die aktuelle Sitzung bleibt
trotzdem nutzbar. Aktive Zeilen behalten auch dann verschiedene öffentliche Kennungen.

Eine in der Oberfläche noch sichtbare gespeicherte Anmeldung kann inzwischen wieder
aktiv geworden sein. Ihre Aktion verwendet deshalb eine stabile öffentliche
Zuordnung zur gespeicherten Anmeldung und erfasst auch die inzwischen zugehörigen
anderen Sitzungen. Fremde und bereits entfernte Kennungen liefern denselben
neutralen aktualisierten Zustand; die aktuelle Sitzung wird nicht über ihren
Abmeldebutton beendet.

Anmeldung, automatische Rückkehr und manuelle Abmeldung verwenden die bestehenden
Kontosperren der atomaren Sitzungszulassung. Zwischen Cookieprüfung und Zulassung
wird die fortbestehende Tokenserie nochmals geprüft. Ein paralleler Widerruf darf
einen gerade geprüften gespeicherten Zugang nicht wiederherstellen.
Widerrufene Einträge geben ihren Limitplatz sofort frei; HTTP, alte Formulare,
Vaadin-RPC und Push werden vor Fachaktionen über die bestehende Sicherheitsprüfung
zurückgewiesen. Die Browserdarstellung muss sich dabei nicht ohne weiteren Verkehr
sofort ändern.

Passwort-/E-Mail-Änderungen behalten ihren strengeren
[Sicherheitsvertrag](account-security-contract.md): Passwortwechsel widerruft alle
gespeicherten Anmeldungen und andere Sitzungen; bestätigte E-Mail-Änderung alle
bisherigen Zugänge. Die neue Tokenrepository-Implementierung entfernt dabei auch
ihre Metadaten innerhalb derselben Datenbanktransaktion.

## Eingeschränkter Zugang bei erreichtem Limit

Nach korrektem Passwort und Ablehnung wegen des Sitzungslimits öffnet der
Vaadin-Login eine eingeschränkte Variante derselben Sitzungsliste.
Auch der bestehende HTTP-Formlogin legt bei dieser Ablehnung den eingeschränkten
Zugang an. Die Passwortprüfung verwendet den vorhandenen persistenten
Anmeldeversuchsschutz; sie wird für den eingeschränkten Zugang erneut ausgeführt.

Der Nachweis ist ausschließlich serverseitig an die konkrete HTTP-Sitzung gebunden,
gilt fünf Minuten ohne Verlängerung und speichert weder das eingegebene Passwort
noch einen authentifizierten Principal. Die anonyme Sitzungskennung wird nach
Passwortprüfung gegen Sitzungsfixierung gewechselt. Ein Hash der aktuellen
Passworthash-/Rollenwerte bindet den Nachweis an den geprüften Sicherheitsstand.
Ablauf, geändertes Passwort, geänderte Rollen, Deaktivierung oder fehlendes Konto
verhindern weitere Verwaltungsaktionen. Wartungsschutz wird ebenfalls erneut geprüft.

Der eingeschränkte Zugang belegt keinen authentifizierten Sitzungsplatz und gewährt
keinen Zugriff auf Account, Admin oder andere geschützte Fachaktionen. Er erlaubt
nur eigene Sitzungen/Zugänge anzusehen und zu widerrufen sowie die Anmeldung
fortzusetzen oder abzubrechen. Mit „Anmeldung fortsetzen“ läuft die normale atomare
Zulassung einschließlich aktuellem Limit und Wartungsschutz. Erst dann werden
Principal, Sitzungskontext und gegebenenfalls Remember-me-Cookie angelegt.
Bleibt das Limit erreicht, bleibt die eingeschränkte Ansicht nutzbar.
Nach Erfolg oder Abbruch wird der Nachweis entfernt; bei Ablauf wird eine erneute
Anmeldung verlangt. Die UI erklärt diesen Umfang und die zeitliche Begrenzung.

## Neustart, Lebensdauer und Einführung

Der aktive Bestand bleibt ausdrücklich auf eine Anwendungsinstanz beschränkt.
Es wurde keine persistente Servlet-Sitzungsspeicherung, Sitzungsreplikation oder
Clusterverwaltung eingeführt. Die bestehende Entwicklungs-Neustartbehandlung
bleibt erhalten; verlorene aktive Registrierungen werden nicht aus gespeicherten
Gerätemetadaten als vermeintlich aktive Sitzungen rekonstruiert.

Gespeicherte Tokenserien und ihre Metadaten überleben Neustarts in derselben Datenbank.
Eine gültige automatische Rückkehr wird erneut zum aktuellen Limit zugelassen und
erst dann als aktiv gezählt. Vorhandene ältere Tokenserien bleiben gültig und erhalten
bei der ersten Anzeige neutrale Metadaten; die historische Anmeldezeit lässt sich
nachträglich nicht rekonstruieren, der gespeicherte letzte Nutzungszeitpunkt dient
hier als Ausgangspunkt.

Abgelaufene Tokenserien und verwaiste Metadaten werden beim Laden des eigenen
Bestands sowie stündlich bereinigt. Maßgeblich ist dieselbe beim Start geladene
Remember-me-Dauer wie bei der Cookieprüfung. Eine nur gespeicherte Änderung von
`rememberMe.duration` darf deshalb bis zum Neustart keine noch gültigen Zugänge
ausblenden oder löschen. Die neue Dauer gilt nach Neustart für Prüfung und
Bereinigung gemeinsam. Ein Cookie allein hält keine HTTP-Sitzung aktiv;
Browserschließen bleibt vom ausdrücklichen Abmelden verschieden.

Shared-Modul bauen, Anwendung neu bauen beziehungsweise in der IDE neu starten und
Browser aktualisieren. Die zusätzliche Metadatentabelle wird wie der bestehende
JDBC-Tokenbestand beim Start angelegt. Dabei wird auch eine zu kurze Username-Spalte
des Tokenbestands erweitert. Bei extern verwaltetem Schema müssen die Metadatentabelle
und die ausreichende Spaltenlänge vor Inbetriebnahme bereitgestellt werden. Keine Bestandsdatenbank wurde
für diese Entwicklung geöffnet oder verändert. Migration und Neustart gegen eine
bestehende Produktionsdatenbank sind dadurch nicht nachgewiesen.

## Prüfstand

Gezielte JUnit-Prüfungen verwenden frische eigene H2-Datenbanken, echte
JDBC-Transaktionen/Remember-me-Cookies, das bestehende Sitzungsregister,
SecurityService und die tatsächliche HTTP-Sicherheitskette. Sie decken Eigentum,
aktuellen Zugang, Einzel-/Sammelwiderruf, gespeicherte Zugänge, Neustart, ID-Wechsel,
Rollback, Cleanup, konkurrierende Rückkehr, HTTP/RPC/Push und den eingeschränkten
Login mit persistentem Anmeldeversuchsschutz ab. Vaadin-Komponententests prüfen
DE/EN, Kennzeichnungen, Aktionen, Notifications, Abbruch, Ablauf und Fortsetzen.

Der abschließende vollständige JUnit-Lauf bestand **1.424 Fälle** ohne Fehler oder
Skips. Shared-Build, Spotless Apply/Check, 47 Browserhelfer-/Reporterprüfungen,
205 Python- und 14 JavaScript-Offlinetests sowie der Produktionsbuild waren erfolgreich.
Die vollständige Browser-Suite `all` bestand **je 136/136 Fälle DE und EN** mit zwei
isolierten Workern am selben gepinnten Produktionsimage. Darin enthalten sind
SESSIONS-01–05 für echte Listen, Tabs, Einzel-/Sammelwiderruf, Serverneustart und
eingeschränkten Zugang bei Limit 1 sowie ACCOUNT-SEC-01–04 für Sicherheitsänderungen.
Mehrfensterprüfungen erhalten die Meldungen der jeweils anderen UI.

Die Prüfung fand und behob außerdem die frühere 64-Zeichen-Grenze des JDBC-
Tokenbestands, den globalen Notification-Zustand und die vorzeitige Bereinigung nach
nur gespeicherter Änderung der startgebundenen Remember-me-Dauer. Die Fehler wurden
gezielt reproduziert; die abschließenden Gesamtrunden prüfen den korrigierten Stand.

| JaCoCo-Messung | Zeilen | Zweige |
| --- | ---: | ---: |
| Vollständige Anwendungs-JUnit-Suite | 6910/8157 (84,71 %) | 1833/2507 (73,12 %) |
| Browser all DE | 6949/8157 (85,19 %) | 1622/2507 (64,70 %) |
| Browser all EN | 6958/8157 (85,30 %) | 1623/2507 (64,74 %) |

Gemessen wurden 152 ausführbare Anwendungs-Java-Klassen; Shared und Drittbibliotheken
sind ausgeschlossen. Browserwerte messen serverseitiges Java einschließlich Start und
Hintergrundjobs, keine CSS-/JavaScript-Abdeckung oder vollständig geprüften Anforderungen.
Sprachen werden getrennt ausgewiesen, Zwischenläufe nicht addiert.
[Eigenständiger Meilenstein](meilensteine/2026-10-17-sitzungsverwaltung-und-kontosicherheit.md),
[Prüfstand und verbleibende Grenzen](../browser-tests/abdeckung.md).

Framework-Grundlage:
[Spring Security 7.1 Remember-me-Erweiterungspunkte](https://docs.spring.io/spring-security/reference/7.1/api/java/org/springframework/security/web/authentication/rememberme/PersistentTokenBasedRememberMeServices.html).