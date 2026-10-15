# Ausbau 2: Sitzungsverwaltung in der Kontokonfiguration

Stand: 15.10.2026. **Abgestimmter Folgeauftrag zur Planung; noch nicht implementiert.**

## Reihenfolge und Voraussetzung

Zuerst werden die [noch offenen Konfigurationsfunktionen](../konfiguration/README.md)
umgesetzt. Danach ist das Kontokonfigurationspanel vorgesehen, das bisher noch nicht
existiert. In diesem Panel folgt die Sitzungsverwaltung als **zweite Ausbaustufe** der
[Begrenzung gleichzeitiger Sitzungen](../konfiguration/max-sessions-per-user.md).
Dieser Plan baut weder das Panel noch die Verwaltung bereits ein.

## Ziel und Zählweise

Benutzer sollen ihre angemeldeten Sitzungen sehen und andere Sitzungen abmelden
können, um beispielsweise einen Platz innerhalb ihres Sitzungslimits freizugeben.
Die Oberfläche kann von Geräten sprechen, muss die tatsächliche Zählweise erklären:
Gezählt werden serverseitige Anmeldesitzungen, keine Tabs und keine zuverlässig
identifizierten physischen Geräte. Mehrere Tabs desselben Browserprofils teilen
normalerweise einen Platz; verschiedene Browser können auf demselben Gerät mehrere
Plätze belegen. Das Limit gilt je Benutzerkonto.

## Vorgeschlagener Umfang im künftigen Panel

Bereich **„Angemeldete Sitzungen“** mit folgenden Angaben und Aktionen:

- Browser und Betriebssystem, soweit erkennbar; unbekannte Angaben neutral darstellen.
- Zeitpunkt der Anmeldung und letzte bekannte Aktivität, ohne Scheingenauigkeit.
- Kennzeichnung **„Diese Sitzung“** für die aktuell verwendete Sitzung.
- **„Abmelden“** pro anderer Sitzung.
- **„Alle anderen abmelden“**, wobei die aktuelle Sitzung erhalten bleibt.
- Anzeige der belegten Plätze und des aktuell eingestellten Sitzungslimits.

Bestehende Cosmos-Komponenten, Dialoge, Buttons und DE/EN-Übersetzungen verwenden.
Die genaue Gestaltung und Einordnung werden beim Aufbau der Kontokonfiguration
schrittweise abgestimmt; dieser Plan legt keine neuen globalen Styles fest.

## Erwartetes Verhalten

Eine abgemeldete Sitzung verliert ihren Zugriff und gibt ihren Platz frei. Auch alte
Tabs, Vaadin-RPCs und Push-Verbindungen dürfen danach keine geschützten Aktionen mehr
ausführen. Ihr zugehöriges „Angemeldet bleiben“ muss ebenfalls widerrufen werden,
damit das betroffene Gerät nicht automatisch wieder angemeldet wird. Andere Geräte
und die aktuelle Sitzung bleiben bei einer gezielten Abmeldung erhalten.

Ein bereits beendeter Eintrag wird ohne Fehler entfernt beziehungsweise aktualisiert.
Browser schließen bleibt vom Abmelden verschieden: Ohne erkennbare Abmeldung wird
der Platz weiterhin erst durch Sitzungsablauf frei.

**Grenze:** Wer auf keinem Gerät mehr Zugriff hat und am Limit abgewiesen wird, kann
das geschützte Kontopanel noch nicht öffnen. Zunächst bleibt die Meldung „Auf einem
anderen Gerät abmelden oder den Sitzungsablauf abwarten“. Eine Verwaltung direkt nach
gültigem Passwort trotz erreichtem Limit wäre ein eigener, später abzustimmender
Zugangsweg und ist durch diesen Plan nicht automatisch beauftragt.

## Technischer Entwurf für die spätere Umsetzung

1. Das Register und die atomare Platzfreigabe aus Ausbaustufe 1 wiederverwenden;
   keine unabhängige Geräteliste mit abweichender Zählung einführen.
2. Jede Lese- und Abmeldeaktion serverseitig auf das aktuell angemeldete Konto
   begrenzen. Sitzungskennungen oder Anmeldetokens nicht in der Oberfläche offenlegen.
3. Aktive Sitzung und zugehörige Remember-me-Tokenserie sicher zuordnen. Die heutige
   Tokenverwaltung vor Umsetzung prüfen; falls nötig gezielt erweitern. Niemals als
   Abkürzung alle Remember-me-Tokens des Kontos löschen, wenn nur eine Sitzung endet.
4. Widerruf, Invalidierung und Freigabe so koordinieren, dass eine parallele
   Remember-me-Anmeldung keinen gerade abgemeldeten Zugang wiederherstellt.
5. Nur benötigte Metadaten erfassen; Browserangaben sind Hinweise und kein sicherer
   Gerätenachweis. Keine IP-Ortung oder Geräte-Fingerprints im ersten Umfang.
6. Die Eininstanz-Grenze aus Ausbaustufe 1 beibehalten und dokumentieren. Verhalten
   nach Neustart und Umgang mit Remember-me-Tokens ohne aktive Sitzung vor Umsetzung
   ausdrücklich festlegen; ein Cookie allein belegt weiterhin keinen Sitzungsplatz.

## Geplante Abnahme

- JUnit: nur eigene Sitzungen sichtbar/abmeldbar; fremde Kennungen abweisen;
  gezielte und gemeinsame Abmeldung; wiederholte Abmeldung; paralleler Widerruf,
  Remember-me und neue Anmeldung ohne Überschreitung des Limits.
- Browser DE/EN: mehrere unabhängige Browserkontexte, aktuelle Sitzung markieren,
  andere abmelden, freien Platz erneut nutzen und betroffene Remember-me-Rückkehr
  verhindern; übrige Geräte bleiben angemeldet.
- Sicherheit: alte Tabs, RPC und Push nach Widerruf gesperrt; Konto-/Rollenänderungen
  behalten ihre bisherigen Schutzregeln. Alle vier Rollen verwalten nur eigene Sitzungen.
- Menschliche Abnahme: verständliche Angaben und Bedienung im neuen Kontopanel,
  einschließlich unbekannter Browserdaten und der Meldung beim erreichten Limit.

Dies sind geplante Prüfungen; für diesen Dokumentationsauftrag wurden keine
Anwendungs- oder Browsertests ausgeführt.
