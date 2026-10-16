# Vertrag: Gleichzeitige Sitzungen pro Benutzer

Stand: 15.10.2026. Erste Ausbaustufe von `max.sessions.per.user`.

## Geltung und sichtbares Verhalten

- Enum-Standard ist **3**; gespeicherte Konfiguration beziehungsweise konfigurierte
  Anwendungsdefaults haben wie bisher Vorrang. Erlaubt sind positive Ganzzahlen.
- Das Limit gilt je normalisierter Kontoidentität für angemeldete HTTP-Sitzungen,
  gleichermaßen für Superadmin, Administrator, Benutzer und Gast.
- Mehrere Tabs desselben Browserprofils teilen normalerweise eine Sitzung und damit
  einen Platz. Zwei Browser auf demselben Gerät können zwei Plätze belegen.
- Bei belegten N Plätzen wird eine weitere Anmeldung mit gültigem Passwort abgelehnt.
  Bestehende Sitzungen werden nicht verdrängt. Der DE/EN-Hinweis erklärt, dass man
  sich anderswo abmelden oder den Ablauf einer Sitzung abwarten muss.
- Falsche Passwörter behalten ihren bisherigen neutralen Fehler und den
  [Anmeldeversuchsschutz](login-attempts-contract.md). Eine Sitzungsablehnung ist kein
  falsches Passwort und erhöht dessen Fehlerzähler nicht.
- Änderungen am Limit gelten bei der nächsten neuen Zulassung ohne Neustart.
  Eine Reduzierung meldet bestehende Geräte nicht ab; zusätzliche Anmeldungen sind
  erst wieder unter der Grenze erlaubt. Wiederherstellen derselben Sitzung zählt nicht neu.
- Logout, Timeout und Invalidierung geben den Platz frei. **Browser schließen allein
  ist kein Logout**; der Server hält den Platz bis zur Invalidierung oder zum Timeout.
- Technisch nicht lesbare oder unzulässige Grenzen erzeugen keinen unbegrenzten
  Ersatzwert. Neue Anmeldungen werden mit dem neutralen Nicht-verfügbar-Hinweis abgelehnt.

## Gemeinsame Zulassung und Konkurrenz

`SessionAdmissionService` ist die gemeinsame `SessionAuthenticationStrategy` für
HTTP-Passwortlogin und Remember-me; der eigene Vaadin-Login verwendet denselben Dienst.
`AdmittedSecurityContextRepository` sichert die tatsächliche Speicherung ebenfalls ab.
Ein reiner Aufruf des `AuthenticationManager` prüft die Identität und belegt noch keinen
Platz; erst die Zulassung einer konkreten HTTP-Sitzung zählt.

Ein Spring-`SessionRegistryImpl` speichert die kanonisierte Kontoidentität als Principal.
Es speichert keine kopierten Rollenobjekte als Zählidentität. 256 feste Sperrobjekte
begrenzen den Speicherbedarf der Verriegelung. Prüfung und Registrierung sind je
Identität atomar: Zwei gleichzeitige Logins können den letzten Platz nicht beide erhalten.
Ein gemeinsamer Sitzungsmutex verhindert außerdem, dass parallele Anmeldungen
verschiedener Konten im selben Browser ihre Zuordnung gegenseitig überschreiben.
Eine begonnene Reservierung zählt bereits, bis sie erfolgreich abgeschlossen oder
zurückgenommen wurde. Es gibt keine unabhängig zählenden Loginwege.

Vor Veröffentlichung eines neuen Principals oder eines Remember-me-Cookies wird die
Zulassung geprüft. Die Sitzungs-ID wird bei einer neuen Anmeldung gegen Sessionfixation
erneuert; vorhandene Vaadin-/UI-Attribute bleiben erhalten. Dieselbe bereits registrierte
Sitzung behält ihre ID bei bloßer Kontextwiederherstellung.

## Freigabe und Teilfehler

Ein an die HTTP-Sitzung gebundener Listener entfernt den Registereintrag bei
Invalidierung. Zusätzlich ist Springs `HttpSessionEventPublisher` als Servlet-Listener
angebunden; das Register verarbeitet Sitzungsende und spätere ID-Wechsel.

Fehler bei der Anmeldung dürfen keinen Geisterplatz hinterlassen:

- Vaadin schließt Status-/Aktivitätsaktualisierung, Speicherung des Principals und
  Cookie-Ausgabe in die Rücknahme einer neuen Reservierung ein. Solange die Antwort
  noch nicht abgeschlossen ist, wird eine begonnene Cookie-Ausgabe bei einem Fehler
  ebenfalls abgebrochen. Die Statusänderung
  erfolgt weiterhin über eine echte Transaktionsgrenze im `UserService`.
- Bei gescheiterter neuer Anmeldung werden ihre HTTP-Sitzung und der Threadkontext
  verworfen; der Vaadin-Principal wird entfernt. Bereits angemeldete andere Geräte
  bleiben erhalten.
- `LoginCompletionFilter` bereinigt neue Reservierungen auch bei späteren technischen
  Fehlern innerhalb der HTTP-/Remember-me-Filterkette.
- Bestehende Regeln gegen Sperre, Löschung und Rollenentzug gelten weiterhin für HTTP,
  alte Vaadin-RPCs und Push. Ihre Invalidierung entfernt ebenfalls den Registerplatz.

## Remember-me

Ein dauerhaftes Cookie allein zählt nicht als Sitzung. Seine automatische Anmeldung
benötigt aber einen freien Platz; sie durchläuft dieselbe Grenze wie ein Passwortlogin.

Spring rotiert bei gültigem Cookie zunächst den Token und setzt ein neues Cookie, bevor
die Sitzungsstrategie läuft. Scheitert die Zulassung, löscht der bestehende Fehlerpfad
das Cookie im betroffenen Browser wieder. Der Nutzer erhält den Sitzungs- beziehungsweise
Nicht-verfügbar-Hinweis auf der Loginseite; es entsteht keine automatische Login-Schleife.
Die Ablehnung löscht keine Tokenserien anderer Geräte. Die bisherige normale Logout-
und Token-Diebstahlbehandlung wird dadurch nicht geändert.

Beim Vaadin-Login wird die Remember-me-Auswahl pro Anfrage übergeben, statt die
`alwaysRemember`-Eigenschaft des gemeinsam genutzten Dienstes umzuschalten. Dadurch
können parallele Anmeldungen ihre Checkbox-Auswahl nicht gegenseitig verändern.

## Einführung und Betriebsgrenze

Diese Version gilt für **eine Anwendungsinstanz mit lokalen Servlet-Sitzungen**.
Es gibt keine neue Datenbanktabelle oder Migration. Einmaliger Anwendungsneustart
ist für die neuen Java-Komponenten erforderlich; danach wirkt das konfigurierte Limit
ohne weiteren Neustart.

Ein vollständiger Produktionsneustart verwirft die lokalen Sitzungen und ihren Registerbestand. Neue Logins und
Remember-me-Rückkehr zählen anschließend neu. Sitzungsreplikation, persistierte Servlet-
Sitzungen und mehrere Server benötigen einen gemeinsamen Bestand mit eigener Einführung;
das lokale Register bietet hierfür ausdrücklich keine globale Grenze.

Die [Geräte-/Sitzungsverwaltung im künftigen Kontokonfigurationspanel](umsetzungsplaene/kontokonfiguration/sitzungsverwaltung.md)
bleibt **Ausbaustufe 2**, nachdem die fehlenden Konfigurationsfunktionen umgesetzt sind.
Die [Kontoseite](account-settings-ui.md) besitzt inzwischen eine visuelle Vorschau;
die echte Geräteverwaltung ist weiterhin offen. Bei erreichtem Limit gibt es zunächst
keinen eigenen Zugangsweg zu einer Geräteverwaltung vor der Anmeldung.

## Prüfungen

JUnit prüft Grenzwerte, Normalisierung, alle Rollen, dynamische Änderungen, mehrere
Tabs, ID-Wechsel, Timeout-/Invalidierungsfreigabe, Konkurrenz und Rücknahme bei
Speicher-/Status-/Cookiefehlern. Die echte HTTP-Filterkette prüft denselben Bestand
mit Vaadin und Remember-me, Token-Erhalt und Freigabe nach Logout.

Die Browser-Suite `session-limits` umfasst vier neue Fälle SESSION-LIMIT-01–04 sowie
bestehende Login-, Anmeldeversuchs-, Remember-me-, Rollen- und Sitzungsregressionen:
**29 Fälle**, einschließlich der beiden ergänzten Serverneustartvarianten. Sie nutzt
ausschließlich eigene Docker-/H2-/Mailpit-Umgebungen.
Browserschließen und Remember-me werden mit echten separaten Chromium-Prozessen
geprüft. Timeout-/Konkurrenzregeln gehören in JUnit; der Browser wartet kein produktives
Sitzungstimeout ab. Ausgeführte Ergebnisse und Grenzen stehen in
[der Browserabdeckung](../browser-tests/abdeckung.md).

Frameworkgrundlagen, passend zum verwendeten Spring Security 7.1.1:
[Session Management](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html),
[RememberMeAuthenticationFilter](https://docs.spring.io/spring-security/reference/api/java/org/springframework/security/web/authentication/rememberme/RememberMeAuthenticationFilter.html),
[ChangeSessionIdAuthenticationStrategy](https://docs.spring.io/spring-security/reference/api/java/org/springframework/security/web/authentication/session/ChangeSessionIdAuthenticationStrategy.html).

## Zusammenspiel mit Wartung

Der [Wartungsmodus](maintenance-mode-contract.md) prüft neue und ausdrücklich
erneute Anmeldungen vor Registrierung/Abschluss. Abgewiesene Benutzer/Gäste
belegen keinen neuen Platz. Bestehende registrierte Sitzungen dürfen ihren
Context wiederherstellen und behalten ihren Platz; ihre Fachaktionen sind
während Wartung gesperrt. Ausschalten macht weiterhin gültige Sitzungen wieder
nutzbar, ohne die Zählung oder andere Geräte zurückzusetzen.

## Neustartnachweis und Entwicklungsbetrieb

AUTH-07C/D prüfen seit 16.10.2026 zusätzlich einen tatsächlichen Neustart der eigenen
Testanwendung mit zwei offenen Tabs: Remember-me stellt die Identität wieder her,
ohne Remember-me entsteht keine dauerhafte Anmeldung. Die Besonderheiten des
IntelliJ-/DevTools-Betriebs und die Grenze der konkreten Fehlerreproduktion stehen
in der [Neustart-Analyse](login-restart-analysis.md).
