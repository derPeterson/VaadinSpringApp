# Gleichzeitige Sitzungen pro Benutzer begrenzen

Stand: 15.10.2026; Ausgangscommit `1d57a5e`. **Umsetzungsplan, noch nicht implementiert.**

Anschlussstand: Die [Begrenzung der Anmeldeversuche](../../login-attempts-contract.md)
ist inzwischen umgesetzt. Darauf aufbauen; dieses weitere Paket bleibt offen.

| Eigenschaft | Wert |
| --- | --- |
| Konfiguration | `max.sessions.per.user` / `ConfigEntry.MAX_SESSIONS_PER_USER` |
| Enum-Standard | `3` |
| Gruppe | Sicherheit |
| Vorgeschlagene Aktivierung | Bei neuer Sitzungszulassung ohne Neustart |
| Priorität | Nach dem gemeinsamen Anmelde-Einstieg |

## Heutiger Stand

Der Wert ist im Adminformular vorhanden, wird aber von der Sicherheitskonfiguration
nicht verwendet. `SecurityConfig` setzt Sessionfixation auf `newSession`; ein Register
angemeldeter Sitzungen oder eine Concurrent-Session-Grenze fehlt. Der eigene Vaadin-
Login speichert den SecurityContext selbst. Ein DSL-Eintrag nur am HTTP-Formlogin
würde diesen Weg nicht vollständig abdecken.

Der bereits vorhandene HTTP-/Push-Schutz bei Sperre oder Rollenentzug bleibt erhalten.

## Empfohlener erster Umfang

**Vorschlag:** Neue Anmeldung ablehnen, wenn bereits N aktive angemeldete HTTP-Sitzungen
bestehen. Ein weiterer Login meldet bestehende Geräte nicht ab.

- Bei Standard 3 sind drei unabhängige Browserprofile erlaubt; ein viertes wird abgewiesen.
- Weitere Tabs im selben Profil teilen gewöhnlich dieselbe Sitzung und zählen einmal.
  Maßgeblich ist die serverseitige Sitzung, nicht die Tabzahl.
- Ein Remember-me-Cookie allein belegt keinen Platz. Erzeugt seine automatische
  Anmeldung eine neue Sitzung, wird diese normal geprüft und gezählt.
- Logout, Sessiontimeout und Invalidierung nach Sperre/Rollenwechsel geben Plätze frei.
  Browserschließen ohne Logout wird erst beim serverseitigen Timeout erkennbar.
- Alle vier Rollen unterliegen derselben Grenze; keine Admin-Sonderplätze.
- Bei Reduzierung unter die aktuelle Zahl bleiben bestehende Sitzungen erhalten.
  Neue sind erst erlaubt, wenn die Zahl wieder unter der Grenze liegt.

Die erste Ausbaustufe gilt für **eine Anwendungsinstanz** mit ihren Servlet-Sitzungen.
Ein lokales Spring-Register begrenzt mehrere Server nicht gemeinsam. Eine spätere
Mehrinstanzlösung benötigt einen geteilten Bestand, etwa Spring Session, und eine
eigene Entscheidung über Speicherung, Betrieb und Migration.

## Sichtbare Änderung

Bei korrekten Zugangsdaten erklärt ein eigener DE/EN-Hinweis, dass die maximale
Sitzungszahl erreicht ist: auf einem anderen Gerät abmelden oder Timeout abwarten.
Die Zahl wird erst nach gültigem Identitätsnachweis gezeigt, nicht für beliebige Adressen.

Geöffnete Geräte arbeiten weiter. Geräteverwaltung und „Andere Sitzungen beenden“
gehören zunächst nicht dazu. Die Feldhilfe erklärt Zählweise, Ablehnung neuer
Sitzungen und die Wirkung einer Reduzierung. Bestehende Gestaltung bleibt erhalten.

## Technischer Umsetzungsweg

1. **`SessionAdmissionService` neu vorgeschlagen:** aktuelles Limit lesen und dieselbe
   Zulassungsregel für Vaadin-Login, HTTP-Formlogin und Remember-me verwenden.
   Geprüfte Frameworkmechanismen integrieren, keine drei unabhängigen Zähler bauen.
2. Spring `SessionRegistry`, geeignete `SessionAuthenticationStrategy`-Bausteine und
   `HttpSessionEventPublisher` prüfen und anbinden. Sessionende und Session-ID-Wechsel
   müssen das Register korrekt aktualisieren. Die Identität stabil über die normalisierte
   Kontoidentität zuordnen, nicht über ein veraltetes Userobjekt.
3. Prüfung plus Registrierung pro Benutzer atomar absichern. Eine Standardprüfung mit
   anschließender ungesicherter Registrierung genügt bei zwei parallelen Logins an
   der letzten freien Stelle nicht. Lokale Verriegelungen begrenzt verwalten; das
   tatsächliche N-Limit und die Eininstanz-Grenze mit Tests belegen.
4. Im eigenen Login die Zulassung vor Context-/Vaadin-Principal-/Cookie-Speicherung
   ausführen. Session-ID gegen Sessionfixation erneuern; konkrete Strategie mit Vaadin
   testen, damit UI-/Sessionattribute nicht unkontrolliert verloren gehen. Die vorhandene
   `newSession`-Konfiguration beweist diesen Schritt beim manuellen Login nicht.
5. Automatische Remember-me-Anmeldung explizit anschließen. Bei Ablehnung keinen
   Loginloop erzeugen und keine fremden vorhandenen Remember-me-Tokens löschen.
   Restaurieren desselben registrierten Principals zählt nicht erneut.
6. Teilfehler zurückrollen: Fehler nach Reservierung, vor Contextspeicherung oder bei
   `handleLogin` dürfen keinen Geisterplatz und keine halbfertige Anmeldung hinterlassen.
   Registerplätze bei vorhandener Stale-Session-Invalidierung entfernen.
7. Vorhandene HTTP-/Push-Prüfungen ergänzen, falls ungültige Sitzungen sonst alte RPCs
   erreichen könnten. Kontosperren und Rollenentzug behalten Vorrang.

Ein Neustart verliert normale lokale Servlet-Sitzungen. Wiederanmeldung bzw.
Remember-me-Rückkehr registriert neu; alte Plätze dürfen nicht dauerhaft blockieren.

## Einführung und Wechselwirkungen

Für die Eininstanzversion ist keine Änderung an Konten oder Rollen nötig. Die
Laufzeitregister beginnen leer. Wird später Sessionpersistenz aktiviert, muss die
Wiederherstellung vor Zulassung neuer Logins geregelt sein. Minimum 1 erhalten;
0 bekommt keine neue Bedeutung „unbegrenzt“.

Mit [umgesetzten Anmeldeversuchen](../../login-attempts-contract.md) abstimmen: eine Sitzungsablehnung ist kein
falsches Passwort. Mit [Wartungsmodus](maintenance-mode.md) abstimmen: ein dort abgewiesenes
Konto darf keinen Platz belegen. Bestehende Token-Löschregeln bei echtem Logout nicht
beiläufig ändern. Ein geänderter Grenzwert gilt für nachfolgende Zulassungen;
ein bereits begonnener Vorgang verwendet seinen definierten Konfigurationsstand.

## Tests und Abnahme

- **JUnit:** Grenzen 1/3, dieselbe Sitzung erneut, unterschiedliche Principaldarstellungen,
  Erhöhen/Reduzieren und ungültiger gespeicherter Wert.
- **Konkurrenz:** zwei korrekte Logins am letzten Platz; genau einer darf erfolgreich
  registriert werden. Reservierungs-/Speicherfehler geben den Platz frei.
- **Sicherheit:** Session-ID-Wechsel, Logout, Timeout, Sperre, Rollenentzug,
  Remember-me und HTTP-Formlogin verwenden denselben Bestand.
- **Browser DE/EN:** N unabhängige Kontexte, N+1 ablehnen, zusätzliche Tabs behalten,
  nach Logout neu anmelden; geschlossenes Gerät nicht voreilig freigeben.
- **Regression:** bestehende Push-/RPC-Fälle und Admin-/Superadmin-/Gastlogins bestehen.
  Ausschließlich eigene Sitzungen und wegwerfbare Testumgebungen verwenden.

Fertig ist das Paket bei nachgewiesenem Limit auf allen Anmeldewegen unter Konkurrenz
und dokumentierter Eininstanz-Grenze. Die geplanten Tests wurden noch nicht ausgeführt.

## Grundlage und Anschlussstellen

[SecurityConfig](../../../src/main/java/de/derpeterson/app/security/SecurityConfig.java),
[SecurityService](../../../src/main/java/de/derpeterson/app/security/SecurityService.java),
[LoginView](../../../src/main/java/de/derpeterson/app/views/LoginView.java),
[PushSecurityInterceptor](../../../src/main/java/de/derpeterson/app/security/PushSecurityInterceptor.java),
[Sitzungsregressionen](../../../src/test/java/de/derpeterson/app/service/SessionAuthorityPersistenceTest.java).

Spring beschreibt Concurrent Session Control und die ausdrückliche Einbindung der
Sessionstrategie bei eigenen Authentifizierungsmechanismen:
[Session Management](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html),
[ConcurrentSessionControlAuthenticationStrategy](https://docs.spring.io/spring-security/reference/api/java/org/springframework/security/web/authentication/session/ConcurrentSessionControlAuthenticationStrategy.html).
Die Ablehnung neuer Sitzungen und Änderungspolitik sind Vorschläge dieses Plans.
