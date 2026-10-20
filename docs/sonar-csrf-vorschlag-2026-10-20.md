# P1-CSRF: konkreter Vorschlag und Regressionstestplan

Stand: 20.10.2026, Branch `fix/sonar-validation-session`. **Nur untersucht und geplant; keine CSRF-Implementierung oder zugehörige neue Testklasse angelegt.**

## Bestätigter Ausgangspunkt

`SecurityConfig.securityFilterChain()` deaktiviert Spring-CSRF global. Gleichzeitig bestehen HTTP-Formularlogin unter `/login` und HTTP-Logout unter `/logout`. Zusätzlich authentifiziert `LoginView` über einen Flow-Button direkt beim AuthenticationManager und ruft `SecurityService.storeAuthenticatedUser()` auf. Das Benutzermenü ruft `SecurityService.logout()` als Flow-Callback auf.

`PushRpcSecurityPersistenceTest.csrfProtectionIsActuallyEnabled` prüft Vaadins eigenen UIDL-Token und die Zurückweisung einer falschen UIDL-Sicherheitskennung. Das beweist **nicht**, dass Spring-CSRF für die klassischen HTTP-Endpunkte eingeschaltet ist. Umgekehrt ist die globale Spring-Deaktivierung kein Beleg, dass alle Flow-Callbacks ohne CSRF-Schutz wären.

Mit Vaadin-Plattform 25.3.2 ist lokal Flow/vaadin-spring **25.3.3** im Testklassenpfad aufgelöst. Die zugehörigen lokalen Sources wurden berücksichtigt. `RequestUtil.isFrameworkInternalRequest()` delegiert an HandlerHelper mit dem konfigurierten URL-Mapping. `VaadinSecurityConfigurer.customizeCsrf()` nimmt Framework-Anfragen aus und kann bei konfigurierter formLoginPage außerdem diese Seite ausnehmen. Deshalb **nicht unbesehen den gesamten Configurer übernehmen**, wenn gerade der klassische POST-Login geschützt werden soll.

## Vorgeschlagene kleine Umsetzung

1. Die vorhandene SecurityFilterChain und ihre fachlichen Handler behalten. `.csrf(...disable())` durch aktivierten Spring-Schutz mit `HttpSessionCsrfTokenRepository` und dem normalen maskierenden Request-Handler ersetzen. Keine neue Authentifizierungsarchitektur und kein zusätzliches stateless Tokenmodell einführen.
2. Die CSRF-Pflicht nach HTTP-Methode und tatsächlichem Transport bestimmen: sichere Methoden wie GET/HEAD/OPTIONS ausnehmen; klassische zustandsändernde Endpunkte einschließlich **POST /login und POST /logout immer prüfen**; nur tatsächlich vom Framework behandelte Flow-Anfragen anhand des RequestUtil und des realen Servlet-Mappings ausnehmen. Ein angehängtes `v-r=uidl` darf auf einem klassischen Authentifizierungsendpunkt keine Ausnahme erzeugen. Vor endgültiger Matcher-Festlegung die tatsächlich verwendeten UIDL-/Push-/Upload-URLs im Test erfassen. Kein pauschales `/VAADIN/**`, `/HILLA/**`, keine allgemeine Ausnahme für `/login` oder alle POST-Anfragen.
3. Für weiterhin unterstützte klassische HTTP-Clients einen Tokenbezug vorsehen: gezielt **GET /login** nach dem CsrfFilter um den Responseheader `X-CSRF-TOKEN` mit dem durch Spring bereitgestellten Request-Token ergänzen. Dabei den Sessionbezug erhalten und `Cache-Control: no-store` setzen. Keine Tokens in URL, Logs oder dauerhaften Dateien; keine allgemeine CORS-Freigabe. Andere öffentliche GET-/Asset-Anfragen sollen deswegen keine zusätzliche Session erzeugen. Der Flow-Buttonlogin benötigt weiterhin nur seinen normalen UIDL-Transport, keinen neuen sichtbaren Formularablauf.
4. Die vorhandenen ObjectPostProcessors setzen bei UsernamePasswordAuthenticationFilter und RememberMeAuthenticationFilter ausdrücklich `SessionAuthenticationStrategy = admissions`. Dadurch darf die neu benötigte `CsrfAuthenticationStrategy` nicht versehentlich aus der Strategiekomposition verschwinden. Für diese beiden Anmeldepfade Admission und CSRF-Tokenrotation gezielt zusammensetzen. Auch beim manuellen Flow-Login die Rotation innerhalb des bestehenden Completion-/Rollback-Rahmens genau einmal ausführen. **Nicht** pauschal bei jedem `AdmittedSecurityContextRepository.saveContext()` oder jedem Rechte-Refresh rotieren. SessionAdmissionService führt die Session-Fixation-Behandlung bereits aus; keine doppelte ID-Rotation oder zweite Reservierung einführen.
5. Den klassischen Logout ausdrücklich auf POST festlegen. Vorhandene Remember-me- und Session-Aufräumhandler erhalten und Spring-Token-Cleanup berücksichtigen. GET /logout darf keine Abmeldung mehr bewirken; dies ist eine beabsichtigte Verhaltenskorrektur für diesen HTTP-Endpunkt. Der Flow-Abmeldebutton bleibt unverändert im Ablauf.
6. MissingCsrfTokenException und InvalidCsrfTokenException in der bestehenden Fehlerbehandlung gezielt als **HTTP 403** behandeln. Der bisherige generische AccessDenied-Redirect darf einen CSRF-Fehler nicht als normalen Navigationsfehler verschleiern. Keine Zugangsdaten oder Token im Fehlertext ausgeben.
7. Für eine aktivierte lokale H2-Konsole die Konsequenzen separat prüfen. Kein allgemeines produktives Ausschalten des Schutzes als Reparatur einer Entwicklungsoberfläche. Vorhandene Account-/Verifikations-/Reset-Verträge und Einmallinks nicht als Nebenwirkung ändern.

## Regressionstestplan vor Umsetzung abzustimmen

Die folgenden Fälle sind **geplant, noch nicht als neue CSRF-Regressionen implementiert**. Bestehende Sicherheits-JUnits laufen im Gesamtpaket bereits mit.

| ID | Bereich / Testort | Erwartung |
|---|---|---|
| C01 | SecurityChainRegressionTest: POST /login ohne Token, falscher Token, fremder Session-Token | 403; keine Authentifizierung, kein Slot, keine Remember-me-Ausgabe; Passwortzähler nicht als fehlgeschlagene Passwortprüfung erhöhen. |
| C02 | Derselbe Test: GET /login und klassischer POST mit Session + gültigem Header beziehungsweise `_csrf` | Tokenheader vorhanden, no-store; gültiges Passwort meldet normal an. Falsches Passwort behält vorhandene Fehler-/Drosselverträge. |
| C03 | Klassischer Login mit angehängtem v-r=uidl / anderem gefälschten Frameworkmarker | Ohne gültigen Spring-Token weiterhin 403; kein Bypass. |
| C04 | SessionAdmissionTest / SecurityChainRegressionTest | Session-ID wechselt einmal, genau ein Slot; Admission vor Veröffentlichung von Principal/Cookie; Folgefehler räumen nur den neu reservierten Slot auf. |
| C05 | LoginContextPersistenceTest / SessionAdmissionTest | Alter Spring-Token nach HTTP- oder Flow-Login ungültig; neuer Token gültig. Persistierter Context im Folgerequest vorhanden, kein Tokenwechsel bei bloßem Rechte-Refresh. |
| C06 | HTTP-Logout | GET ohne Seiteneffekt; POST mit fehlendem/falschem Token 403 und bestehende Anmeldung bleibt erhalten; gültiger Token meldet ab, gibt Slot frei und bereinigt die eigene Remember-me-Serie/Cookies. |
| C07 | Remember-me / SecurityChainRegressionTest | Gültiger Cookie kann über sicheren GET weiter anmelden; Limits und Wartung gelten; neue Session bekommt eigenen CSRF-Zustand. Ungültiger Cookie und abgelehnte Admission beeinträchtigen keine fremden Serien/Sitzungen. |
| C08 | PushRpcSecurityPersistenceTest und echte Servletkette | Legitime UIDL-Anfrage mit gültigem Flow-Token funktioniert ohne Spring-Formulartoken; falscher Flow-Token führt zu keiner Mutation. Die bestehende reine ServerRpcHandler-Prüfung um einen Test durch die HTTP-Filterkette ergänzen. |
| C09 | Tatsächliche Push-/Upload-Transporte | Legitime Framework-Anfragen funktionieren; WebSocket/RPC mit abgelaufener Sitzung oder widerrufenen Rechten bleiben gesperrt. Nicht nur einen beliebigen POST auf /VAADIN/uidl als echten Transport simulieren. |
| C10 | LoginSessionRecoveryTest / SessionManagementTest | Erreichtes Sessionlimit führt weiter nur zum eingeschränkten Verwaltungszugang; Revoke/RevokeOthers benötigen echten geschützten Flow-Callback. Abbruch oder Fehler hinterlassen keinen Slot/Context. |
| C11 | Wartung und Rollenrefresh | Adminzugang, Ablehnung normaler Konten, erlaubter Logout und bestehende Session-Wiederaufnahme unverändert; Schutz greift auch beim expliziten Relogin. |
| C12 | Öffentliche Ressourcen / Tests ohne HTTP-Session | Statische Dateien und nicht betroffene anonyme GETs erzeugen keinen zusätzlichen CSRF-Sessionzustand; kein Spring-Token in URL/Logs. |
| C13 | Spätere Browserabnahme DE und EN | Flow-Passwortlogin, Remember-me nach Neustart des Browserkontexts, Logout, zweiter Tab, Sessionlimit-Verwaltung, Wartung und Wiederaufnahme am selben Produktionsimage erfolgreich. |

Bestehende tokenlose klassische HTTP-Testhelfer müssen beim späteren CSRF-Paket den echten Tokenbezug verwenden. Sie pauschal weiter auszunehmen oder den Filter in Tests abzuschalten wäre keine Regressionabsicherung. Neue Assertions sollen Verhalten und Seiteneffekte prüfen, nicht nur das Vorhandensein eines Filters.

## Risiken und Freigabekriterium

Hauptrisiko sind die beiden parallelen Loginwege und deren eigene Sessionstrategien. Vor Umsetzung benötigt der Vorschlag Tests für Token-Lebenszyklus, Filterreihenfolge und Rollback, anschließend die tatsächlichen Flow-Transporte. Der Spring-CSRF-Befund bleibt bis dahin offen; aus den bisherigen grünen JUnits wird keine Behebung behauptet. Browserabnahme separat freigeben, bevor sie gestartet wird.

Primärquellen: [Spring-CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html), [Vaadin Security Configurer](https://vaadin.com/docs/latest/flow/security/vaadin-security-configurer), [Vaadin-Schutzmechanismen](https://vaadin.com/docs/latest/flow/security/advanced-topics/vulnerabilities). Die projektspezifischen Aussagen wurden zusätzlich gegen die genannten lokalen Quellstellen und Framework-Sources geprüft.
