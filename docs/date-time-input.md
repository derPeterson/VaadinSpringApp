# Datum und Uhrzeit ohne Trennzeichen

Die vorhandenen Vaadin-Felder akzeptieren zusätzlich kompakte ASCII-Ziffernfolgen.
Übernommen und formatiert wird beim Verlassen des Feldes oder mit Enter, nicht bei jedem Tastendruck.
Kalender, normale Datumsformate, Pflichtfelder und bestehende Bereichsprüfungen bleiben erhalten.

## Datum

Gilt für Geburtstag bei Registrierung, Mein Konto und Benutzer anlegen/bearbeiten
sowie für das Datum im Aktivitätsfilter der Benutzerverwaltung.

| Eingabe | Deutsch | Englisch |
| --- | --- | --- |
| 4 Ziffern | Tag, Monat jeweils einstellig + zweistelliges Jahr | Monat, Tag jeweils einstellig + zweistelliges Jahr |
| 6 Ziffern | TTMMJJ | MMTTJJ |
| 8 Ziffern | TTMMJJJJ | MMTTJJJJ |

Beispiele: DE `260486` und `26041986` ergeben `26.04.1986`, DE `4586` ergibt
`04.05.1986`. EN `042686` und `04261986` ergeben `4/26/1986`, EN `4586` ergibt
`4/5/1986`. Die Reihenfolge folgt also der Sprache des Feldes.
Zweistellige Jahre liegen fest zwischen 1950 und 2049 (`50` → 1950, `49` → 2049).
Dafür ist auch Vaadins Referenzdatum auf 01.01.2000 gesetzt; normale getrennte
Eingaben mit zweistelligem Jahr verwenden dieselbe Regel.

Andere kompakte Längen werden nicht geraten. `11086` ist beispielsweise ungültig;
bitte mit führenden Nullen oder Trennzeichen eingeben. Ungültige Kalendertage,
Monate und Jahr 0000 werden abgelehnt. Bestehende Einschränkungen für Geburtstage
(z. B. keine Zukunftsdaten) gelten weiterhin.

## Uhrzeit

Der Aktivitätsfilter akzeptiert zusätzlich `HMM` und `HHMM`, zum Beispiel `930` →
09:30 und `1430` → 14:30. Die kompakte Eingabe verwendet in beiden Sprachen
24-Stunden-Werte. Die Anzeige bleibt sprachabhängig: Deutsch 14:30,
Englisch 2:30 PM. Normale Eingaben mit Doppelpunkt und AM/PM verarbeitet weiterhin Vaadin.

Stunden müssen zwischen 0 und 23, Minuten zwischen 0 und 59 liegen. Die vorhandene
Dropdown-Liste in 15-Minuten-Abständen und die Pfeiltasten-Schrittweite bleiben erhalten.
Das ist kein Eingaberaster: Vaadin erlaubt auch manuell eingegebene andere Minutenwerte.

## Umsetzung und Prüfung

`CompactDateInput` verwendet Vaadins serverseitigen Zusatzparser. `CompactTimePicker`
ergänzt die öffentliche `i18n.parseTime`-Funktion und installiert sie nach Anbindung
und Sprachwechsel erneut. Es gibt keine zusätzliche Bibliothek oder Eingabemaske.
Grundlagen: [Vaadin-Datumsformate](https://vaadin.com/docs/latest/components/date-picker/date-formats)
und [TimePicker](https://vaadin.com/docs/latest/components/time-picker).

Gezielte JUnit-Prüfungen decken Kurzformen, Sprachreihenfolge, Schaltjahre,
Jahrhundertgrenze, ungültige Eingaben, native Bereichsvalidierung und die Weitergabe
an den Aktivitätsfilter ab. Vier JavaScript-Offlinetests prüfen Uhrzeiten,
native Formate und die erneute Installation nach Sprachwechsel.
Ergebnis am 18.10.2026: 128 gezielte JUnit-Tests und vier JavaScript-Offlinetests
erfolgreich, jeweils ohne Fehler oder übersprungene Fälle. Shared `clean install`,
Spotless `apply`/`check` und JavaScript-Syntaxprüfung erfolgreich.
Browserprüfung und tatsächlicher Frontend-Modulladevorgang sind noch offen;
Browsertests wurden für diese Änderung nicht gestartet.

Shared-Modul und Anwendung neu bauen, das neue Frontend-Modul einbinden und die
Anwendung neu starten. Danach die Seite neu laden, gegebenenfalls mit Strg+F5.
Ein vorhandenes Produktions-Frontend benötigt einen neuen Frontendbuild.
