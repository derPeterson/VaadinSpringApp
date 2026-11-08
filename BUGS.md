# Offene Fehler

- **2FA-Ersatzcode-Ausgabe lässt sich versehentlich verlassen.** Nach der
  Ersteinrichtung, dem Authenticatorwechsel oder der Neuausgabe von Ersatzcodes
  im Besucherheader auf das Logo oder einen Navigationspunkt klicken, ohne
  „Codes gesichert“ zu bestätigen. Die Navigation kann die Ausgabe verlassen,
  obwohl der Abschluss im Formular noch gesperrt ist. Die laufende Ausgabe
  soll nach Neuladen erreichbar bleiben und der reguläre Portalzugang erst
  nach der Bestätigung weiterführen. Nachtest: Header, Direktlink und zweiter
  Tab bei aktiver Ausgabe, jeweils vor und nach der Bestätigung.

Die bewussten Grenzen der Google-Ersteinrichtung stehen in der
[Sicherheitsdokumentation](docs/externe-anmeldung.md#google-bestätigung-und-geschützte-ersteinrichtung).
Offene automatisierte Ergänzungen und menschliche Abnahmen bleiben im
[Testbedarf](docs/testbedarf.md) und im [2FA-Plan](docs/planung/zwei-faktor-bestaetigung.md).
