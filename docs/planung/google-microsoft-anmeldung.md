# Google-/Microsoft-Anmeldung und freiwillige Profilangaben

## Auftrag und Stand

Branch: `feature/google-microsoft-anmeldung`.
Die Umsetzung ist freigegeben, einschließlich externer Registrierung ohne zusätzliches
Portalpasswort und isolierter Browserprüfungen DE/EN auf Desktop und Smartphone.
Ein Zwischencommit des aktuellen Stands ist beauftragt. Merge und Push sind nicht beauftragt. Die separate große Testrunde auf `main`
bleibt offen.

Der Funktionsumfang ist in [Externe Anmeldung](../externe-anmeldung.md) beschrieben.
Dazu gehören Google und Microsoft für private sowie Firmen-/Schulkonten, freiwillige
Profilangaben, ausdrückliche Anbieterzuordnungen und zweckgebundene erneute Nachweise.
Die bisherige Passwortanmeldung und die bestehenden Kontoschutzregeln bleiben erhalten.
Die drei HTML-Referenzen enthalten die entsprechenden Gestaltungselemente.
Der [Vaadin-Komponentenabgleich](vaadin-komponenten-abgleich.md) ist eine separate
spätere Umsetzung. Die abgenommene Analyse beauftragt keinen Bausteinumbau in diesem
Feature; dessen Umsetzung ist keine Voraussetzung für die hiesige Abnahme oder den Abschluss.
Die gezielten Prüfungen und der automatisierte Umfang der großen Testrunde auf dem
Featurestand sind abgeschlossen. Ergebnisse einschließlich gezielter Nachtests und
des noch fehlenden aktuellen Sonar-Nachweises: [Prüfstand](../status.md).

## Noch offen bis zur gemeinsamen Abnahme

- Die freigegebene geführte Einrichtung unter Konfiguration / Anmeldeanbieter
  gemeinsam ansehen: Zugangsdaten, Rücksprungadresse, direkte Aktivierung und
  Deaktivierung für Google/Microsoft. Die isolierten Browserprüfungen DE/EN auf
  Desktop und Smartphone sind bestanden. Echte Anbieterzugänge bleiben Teil von HUM-08.
- Gemeinsam die Anbieterbuttons, ergänzende Registrierung und verbundenen Anmeldungen
  auf Desktop und Smartphone ansehen. Die Registrierung verlangt kein Portalpasswort.
  Die Zwei-Panel-Anordnung der Anmeldung ist abgenommen. Für die Registrierung
  ist derselbe eigene Schnellzugang rechts neben dem unveränderten Formularpanel
  freigegeben, bei wenig Platz darunter. Die Footerlinks folgen denselben Umbruchregeln.
  Grundlage ist der Inhalt aus Variante B im
  [Stitch-Projekt](https://stitch.google.com/projects/43914967159269113).
  Die dunkle Inhaltsfläche und der lokale Footer bleiben erhalten. Diese Anordnung
  der Registrierung gemeinsam ansehen.
- „Login speichern“ als nativen Switch mit gemerkter Schalterstellung gemeinsam
  ansehen. Das separate Einstellungs-Cookie enthält keine E-Mail und kein Passwort
  und ist kein Remember-me-Anmeldenachweis. Die lokale HTML-Referenz bildet die
  Stellung mit getrenntem Vorschau-Speicher ab.
- Mit selbst eingerichteten Google-/Microsoft-Anwendungen die Rücksprungadressen und
  tatsächlichen Anbieterabläufe prüfen. Dafür sind lokale Client-Konfiguration und
  interaktive Kontofreigabe nötig. Prüfanleitung: [HUM-08](../manual-tests/README.md#hum-08).
  Die isolierten OIDC-Tests verwenden keine echten Anbieterzugänge.
- Nach Abnahme den offenen Plan und seinen TODO-Verweis entfernen. Keine automatische
  Zusammenführung oder Veröffentlichung.

Die Kontoidentität bleibt unabhängig von veränderlichen E-Mail-Adressen. Weder
Anbieterrollen noch eine erfolgreiche Anbieteranmeldung umgehen lokale Sperren,
Wartung, Sitzungslimit oder Widerrufe. Nicht konfigurierte Anbieter werden nicht angeboten.
