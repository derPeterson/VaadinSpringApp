# Anmeldung nach einem Anwendungsneustart

> Aktualisierung vom 21.10.2026: Der unten beschriebene frühere Verlust aktiver
> Sitzungen bei Serverneustart ist durch die ausdrücklich beauftragte dauerhafte
> Anmeldung ersetzt. Maßgeblich ist der [aktuelle Neustart-/Speichervertrag](durable-session-contract.md).
> Frühere Prüfzahlen beschreiben den damaligen Stand. Eine gültige Sitzung zählt
> jetzt auch nach Serverneustart weiter; die Vaadin-Oberfläche wird frisch aufgebaut.


Stand: 17.10.2026. Analyse auf `feature/account-settings-ui`.

## Ergebnis und Einordnung

Die Sitzungsbegrenzung wurde mit einem lokalen `SessionRegistryImpl` eingeführt.
Dessen Inhalt wird beim vollständigen Serverneustart nicht gespeichert. Der
[Sitzungsvertrag](session-limit-contract.md) beschreibt diese Eininstanz-Grenze
seit der Einführung. Eine normale Sitzung ist kein zugesicherter dauerhafter Login.
Das leere Register allein beweist jedoch nicht die Ursache des IDE-Befunds: Eine
vom Servletcontainer wiederhergestellte Sitzung und der lokale Registerbestand
sind getrennte Zustände. Es wurde keine absichtliche Abschaltung der bisherigen
IDE-Sitzungswiederherstellung gefunden.

„Login speichern“ wurde nicht absichtlich abgeschaltet. Der Code verwendet weiterhin
Spring Securitys `PersistentTokenBasedRememberMeServices` und die Datenbanktabelle
`persistent_logins`. Die Checkbox wird pro Anmeldung übergeben. Tokenrepository,
Secret und Laufzeit werden beim Start aus der Konfiguration aufgebaut.

Der neu ergänzte Browsernachweis AUTH-07C/D startet ausschließlich die eigene
Docker-Testanwendung tatsächlich neu, während zwei angemeldete Tabs offen bleiben.
Mit Remember-me kehrt dieselbe Kontoidentität in beiden Tabs zurück; ohne
Remember-me muss in der Produktions-Testumgebung neu angemeldet werden. Auch die
bestehenden echten Chromium-Neustarts mit/ohne Remember-me und nach Logout waren
erfolgreich (vier Fälle der Suite `remember-me`). Keine Cookies oder Tokens wurden
im Test injiziert, ausgetauscht oder als Zugangsdaten ausgegeben.

## Warum IntelliJ davon abweichen kann

Ein IDE-/DevTools-Neustart und der Neustart des gepackten Produktions-JARs sind
verschiedene Fälle. Spring Boot DevTools aktiviert standardmäßig
`server.servlet.session.persistent=true`; die Wiederherstellung setzt aber
kompatible serialisierte Sitzungsobjekte voraus. Vaadin weist bei Live Reload
ausdrücklich auf serialisierbare Sitzungen und die Trennung der Reload-Verfahren hin.
Java-/Komponentenänderungen und Classloader-Wechsel können diese Wiederherstellung
verhindern. Das ist eine mögliche Erklärung, kein nachgewiesener Befund des
konkreten IntelliJ-Laufs.

Die reguläre Anwendung, ihre Datenbank, Cookies und echten Anmeldedaten wurden
für diese Analyse nicht verändert oder ausgelesen. Der konkrete gemeldete Verlust
von Remember-me beim IntelliJ-Neustart ist bisher nicht reproduziert. Er ist damit
nicht als behoben zu betrachten. Dafür wäre ein isolierter Nachweis mit demselben
IDE-Neustartverfahren beziehungsweise ein passender bereinigter Fehlerauszug nötig.
Es wurden keine Sicherheitsprüfungen oder Sitzungsgrenzen gelockert.

## Technische Quellen

- [Spring Boot DevTools: Defaults und Restart-Grenzen](https://docs.spring.io/spring-boot/reference/using/devtools.html)
- [Vaadin: Live Reload und Sitzungsbehandlung](https://vaadin.com/docs/latest/flow/configuration/live-reload)

Die neuen Browserfälle bleiben im vollständigen Lauf und in `remember-me` enthalten.
Die Coverage-Erfassung ergänzt innerhalb derselben frischen Testumgebung ihre Daten
nach einem Serverneustart (`append=true`), statt den vorigen Messabschnitt zu verlieren.
Jeder neue Lauf bekommt weiterhin neue eigene Volumes und unabhängige Messdaten.
