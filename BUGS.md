# Offene Fehler

- Bei der Registrierung setzt der Browser gespeicherte Anmeldedaten in die
  E-Mail-Bestätigung und das Passwortfeld ein. Autofill-Hinweise der E-Mail- und
  Passwortfelder korrigiert, realer Nachtest steht aus.
  [Reproduktion und Nachtest](docs/manual-tests/README.md#hum-11).

- Beim Bearbeiten des eigenen Kontos setzt der Browser das gespeicherte Passwort
  ein und löst dadurch beim unveränderten Schließen den Änderungsschutz aus.
  Autofill-Kennzeichnung korrigiert, realer Browsernachtest steht aus.
  [Details und Nachtest](docs/status.md#autofill-beim-bearbeiten-des-eigenen-kontos).

- MultiSelect-Auswahlrahmen werden oben und seitlich abgeschnitten.
  Quellkorrektur vorhanden, erfolgreicher Nachtest steht noch aus.
  [Reproduktion und Nachtest](docs/status.md#offener-multiselect-darstellungsfehler).

Die bewussten Grenzen der Google-Ersteinrichtung stehen in der
[Sicherheitsdokumentation](docs/externe-anmeldung.md#google-bestätigung-und-geschützte-ersteinrichtung).
Offene automatisierte Ergänzungen stehen im [Testbedarf](docs/testbedarf.md).
