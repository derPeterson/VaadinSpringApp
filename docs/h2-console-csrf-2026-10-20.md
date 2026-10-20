# H2-Konsole nach Aktivierung von Spring-CSRF

Stand 20.10.2026, Nachtrag auf `fix/sonar-csrf`, Ausgangspunkt
`6e8aa610dda9d452b6f97959645fef964f9d4479`. Eigene Änderungen uncommittet;
keine Commits, Pushes, Sonar-Scans oder Sonar-Statusänderungen.

## Ergebnis und Entscheidung

Die ausdrücklich freigeschaltete native H2-Konsole ist für Browser-URLs über
`localhost`, `127.0.0.1` und `[::1]` wieder bedienbar. Sie verwendet einen eng
begrenzten Ursprungsschutz; die Spring-Tokenprüfung für klassische Endpunkte der
Anwendung einschließlich `/login` und `/logout` bleibt aktiv. Es wird kein
Spring-Token in H2-HTML, JavaScript oder native Sitzungs-URLs hineingeschrieben.

Die H2-Oberfläche unterstützt die Spring-Formulartokens nicht. Die
[Spring-Boot-Dokumentation](https://docs.spring.io/spring-boot/reference/data/sql.html#data.sql.h2-web-console)
beschreibt deshalb eine separate Entwicklungsregel für die Konsole und empfiehlt
`SAMEORIGIN` für deren Frames. Hier wird die Konsole zusätzlich mit einer strikten
Herkunftsprüfung abgesichert. Die Wahl und die abweisende Behandlung fehlender
Angaben orientieren sich an den
[OWASP-Empfehlungen zu Fetch Metadata und Origin-Prüfung](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html).
Die tatsächlichen APIs und nativen Abläufe wurden gegen Boot 4.1.1,
Spring Security 7.1.1 und H2 2.4.240 lokal geprüft.

## Umsetzung

- `security/H2ConsoleOriginFilter.java` erkennt ausschließlich das tatsächlich
  registrierte native `JakartaWebServlet`: qualifizierte `h2Console`-Registrierung,
  aktivierter Registrierungseintrag, Servletname und tatsächliches
  `HttpServletMapping` müssen übereinstimmen. Ein URL-Präfix oder `v-r` genügt nicht.
- Ohne `app.security.expose-h2-console` bleibt auch eine vorhandene Konsole an
  einem anderen registrierten Pfad gesperrt. Ohne aktiviertes natives Servlet
  entsteht keine Sonderbehandlung. Root-Mapping und die klassischen Login-/Logout-
  Routen erhalten keine H2-Ausnahme.
- Gültige `Origin`-/`Referer`-Angaben müssen exakt Schema, Host und wirksamen Port
  der Anfrage treffen. `Sec-Fetch-Site: same-origin` wird berücksichtigt.
  `cross-site`, `same-site`, widersprüchliche/mehrfache Header und ungültige oder
  undurchsichtige Origins werden abgewiesen. HTTP-Clients dürfen Header selbst
  setzen; dies ist ein CSRF-Schutz für Browser und keine Netzwerkauthentifizierung.
- Die Konsolen-URL ist auf die genannten Loopback-Hostnamen begrenzt, damit kein
  beliebiger Hostheader als vertrauenswürdiges Ziel dient. Der Client-IP-Test von
  H2 selbst bleibt erhalten; Docker-NAT braucht keine eigenen IP-Ausnahmen.
- Ohne Herkunftsangaben ist nur der harmlose GET-/HEAD-Einstieg ohne Parameter
  zugelassen. SQL- und andere H2-Aktionen brauchen eine vertrauenswürdige Quelle,
  **auch bei GET**: H2 akzeptiert zustandsändernde `.do`-Aktionen darüber ebenfalls.
- `SecurityConfig` schaltet den Filter vor den CsrfFilter. Nur die passend
  identifizierte und erfolgreich geprüfte Konsole verwendet ihren Ursprungsschutz
  anstelle des Spring-Tokens. Alle übrigen Anwendungsregeln bleiben bestehen.
- `X-Frame-Options: SAMEORIGIN` ersetzt die bisherige globale Abschaltung bei
  freigeschalteter Konsole. `Cache-Control: no-store` wird auch gegen die eigenen
  Cacheheader des H2-Servlets erhalten; `Vary` berücksichtigt die Herkunftsheader.
- Keine neue Abhängigkeit und kein neues Profil. Bestehende Konfigurationsdateien,
  `.env`, Datenbanken und fremde Prozesse wurden nicht verändert. Keine neuen Logs
  mit Origins, Referern, Sitzungskennungen oder SQL-Inhalten.

## Bedienung und Einführung

Nach Neubau und Neustart der vorgesehenen Anwendung die bestehende Konsolenadresse
**direkt** über einen der Loopback-Hostnamen öffnen, beispielsweise
`http://localhost:18080/h2/` in der lokalen Docker-Testumgebung. Für die Oberfläche
bleibt JavaScript wie bei der nativen H2-Konsole erforderlich. Der bestehende
Freigabeschalter muss die Konsole weiterhin aktivieren. Ein einfacher Browser-
Refresh übernimmt die neue Java-Implementierung nicht.

Andere Hostnamen, LAN-URLs und ein Einstieg aus fremden Seiten sind nicht Teil
dieser lokalen Entwicklungsfreigabe. Veraltete Browser oder Clients ohne
Herkunftsangaben werden bei Aktionen abgewiesen. Reverse Proxies dürfen solche
Angaben nicht entfernen; die Anwendung interpretiert keine ungeprüften
`X-Forwarded-*`-Header selbst. Eine andere Zielumgebung braucht eine gesonderte
bewusste Regel. Die Konsole bleibt ein Entwicklungswerkzeug.

## Tatsächliche Prüfungen

| Prüfung | Ergebnis |
| --- | --- |
| Neue Ursprungsschutz-Testausprägungen | 35 erfolgreich |
| Neue echte HTTP-Fälle mit H2-Servlet und vollständiger SecurityFilterChain | 9 erfolgreich |
| Gezielter Sicherheitslauf einschließlich bestehender CSRF-/Loginregressionen | 119 erfolgreich |
| Vollständiger Mavenlauf `-Pproduction verify` | 1956 erfolgreich, Produktions-JAR gebaut |
| Unabhängige Browser-Helfer | 48 erfolgreich |
| Spotless apply/check, Checkstyle, Whitespace | erfolgreich |

Alle genannten Testläufe ohne Failures, Errors oder Skips. Die 44 neuen H2-Fälle
kommen zu den 51 zuvor ergänzten CSRF-Ausprägungen hinzu.

Die HTTP-Tests verwenden den realen H2-Servletcode und einen eigenen Tomcat mit
zufälligem Loopback-Port und temporärem Verzeichnis. Anmeldung, INSERT/SELECT und
Logout laufen ausschließlich gegen eine eigene flüchtige In-Memory-Datenbank.
Abmeldung verfolgt den nativen Verweis zurück zur Loginseite und verhindert eine
weitere SQL-Mutation. Fremder Ursprung beziehungsweise fehlende Herkunft blockieren
GET- und POST-Aktionen. Mit bekanntem H2-Sitzungsschlüssel versuchtes fremdes SQL
ändert keine Daten; derselbe aktive Zugang bleibt danach bedienbar. Zusätzlich
werden App-Login/Logout mit Konsolenheadern und gefälschtem `v-r` weiterhin ohne
Spring-Token mit 403 abgewehrt. Cache- und Frameheader werden über HTTP geprüft.

Im Test verhindert `properties=null` das Lesen und Schreiben persönlicher
H2-Einstellungen und der Abfragehistorie. Server, Client und eigene Verbindungen
werden geschlossen; vorübergehende Catalina-Systemeigenschaften wiederhergestellt.
Keine bestehende Datenbank oder laufende Anwendung wurde für diese Tests geöffnet.

## Coverage und Grenzen

Frische JaCoCo-Messung des vollständigen JUnit-Laufs, ohne alte Messdaten:
**10145/11583 Zeilen (87,59 %)** und **2947/3844 Zweige (76,66 %)** bei
218 Anwendungsklassen. `H2ConsoleOriginFilter` und sein Cacheheader-Wrapper zusammen:
**86/90 Zeilen (95,56 %)** und **101/112 Zweige (90,18 %)**. Nicht jeder
Header-/Fehlerzweig und keine vollständige Sicherheitsabdeckung wird behauptet.

Es gab für diesen Nachtrag **keinen neuen Browser-UI-Lauf oder Dockerimage-Build**.
Die echten HTTP-JUnits prüfen die nativen Requests und Antworten; sie ersetzen
keine visuelle Abnahme oder einen Nachweis für eine andere Proxy-/Hostumgebung.
Die vorherigen 44/44 Browserfälle je DE/EN und deren Image-/JAR-Kennungen gelten
für den Stand **vor** dieser H2-Ergänzung. P2-TIME und andere Sonar-Pakete bleiben
unverändert zurückgestellt.
