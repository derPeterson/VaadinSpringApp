# Offene Fehler

- Beim erneuten Anfordern einer Verifikationsmail wird auch ein bereits offener
  Versandauftrag als Erfolg gemeldet. Ein Auftrag mit unklarem Transportergebnis
  bleibt auf `IN_PROGRESS` und verhindert dadurch weitere Anforderungen an dieselbe Adresse.
  Die sichere Behandlung solcher Aufträge und eine eindeutige UI-Rückmeldung sind offen.
  [Versandzustände und Grenzen](docs/mailqueue.md#ergebnisse-und-wiederholung).

- Bei der Registrierung setzt der Browser gespeicherte Anmeldedaten in die
  E-Mail-Bestätigung und das Passwortfeld ein. Autofill-Hinweise der E-Mail- und
  Passwortfelder korrigiert, realer Nachtest steht aus.
  [Reproduktion und Nachtest](docs/manual-tests/README.md#hum-11).

- Beim Bearbeiten des eigenen Kontos setzt der Browser das gespeicherte Passwort
  ein und löst dadurch beim unveränderten Schließen den Änderungsschutz aus.
  Autofill-Kennzeichnung korrigiert, realer Browsernachtest steht aus.
  [Details und Nachtest](docs/status.md#autofill-beim-bearbeiten-des-eigenen-kontos).

Die bewussten Grenzen der Google-Ersteinrichtung stehen in der
[Sicherheitsdokumentation](docs/externe-anmeldung.md#google-bestätigung-und-geschützte-ersteinrichtung).
Offene automatisierte Ergänzungen stehen im [Testbedarf](docs/testbedarf.md).
