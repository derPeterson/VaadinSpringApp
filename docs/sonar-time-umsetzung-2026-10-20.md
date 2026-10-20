# P2-TIME: Eindeutige Aktivitätszeitpunkte

Stand: 20.10.2026, Branch `fix/sonar-activity-time`, Ausgangspunkt `43bde6f`.
Keine Commits, Pushes oder Sonar-Statusänderungen durch diese Arbeit.

## Fachliche Entscheidung

Die Anwendung befindet sich vollständig in Entwicklung und wird ohne historische
Aktivitätsdaten neu aufgesetzt. Deshalb gibt es keine Bestandsmigration,
Legacy-Spalte oder heuristische Umdeutung lokaler Uhrzeiten. Der Auftrag umfasst
die Codeumstellung für eine neu angelegte Datenbank; vorhandene Datenbanken,
Container oder Volumes wurden dafür nicht geöffnet, gelöscht oder zurückgesetzt.

„Vor einer Stunde“ bezeichnet tatsächlich verstrichene Zeit. Sprache und
Laufzeit-Systemzone bestimmen keinen gespeicherten Zeitpunkt. Die Anzeige und
die Kalendereingabe des Aktivitätsfilters verwenden ausdrücklich `Europe/Berlin`.
Geburtsdaten bleiben `LocalDate`; andere fachliche Zeitfelder gehören nicht zu
pauschalen Umstellungen dieses Pakets.

## Änderungen

| Bereich | Ergebnis |
| --- | --- |
| `UserEntity.lastActivity` | `Instant` statt `LocalDateTime`, explizite Hibernate-Zuordnung `TIMESTAMP_UTC`. Builder und neuer Entity-Standard verwenden einen eindeutigen Zeitpunkt. Die unbenutzte Entity-Methode `updateLastActivity()` entfällt; Aktualisierungen erfolgen über den Service. |
| `UserRepository` | Bulkupdate und beide Statusabfragen erhalten `Instant`. Index, Querygrenzen und Versionsverhalten bleiben erhalten. |
| `UserService` | Aktivitätsaktualisierung liest eine testbare `Clock`, im Normalbetrieb `Clock.systemUTC()`. Scheduler-Recheck arbeitet mit demselben Zeitpunkttyp. Bestehende öffentliche Konstruktorabhängigkeiten bleiben erhalten. |
| `UserStatusScheduler` | Abwesenheits- und Rückkehrgrenze entstehen aus tatsächlichen Zeitabständen einer testbaren Clock. Die strikten Vergleiche „älter als“ beziehungsweise „neuer als“, Sperren und getrennten Kandidatentransaktionen bleiben erhalten. |
| `UserActivityTime` | Gemeinsame Anzeigezone und ausdrückliche Umwandlung der lokalen inklusiven Filtergrenze. Eine Uhrzeit in der Frühlingslücke wird abgewiesen. Bei der doppelten Herbststunde beginnt „aktiv seit“ beim ersten Vorkommen, sodass beide Vorkommen eingeschlossen bleiben. |
| `AdminUserActivityFilter` | Bestehende Datum-/Zeitfelder und kompakte Eingabe bleiben. Frühlingslücken werden mit der vorhandenen übersetzten Validierung abgewiesen, auch bei programmatisch gesetzten Werten. Platzhalter verwenden dieselbe Anzeigezone und eine testbare Clock. |
| `AdminUserFilters` / `AdminUserManagementSection` | Angewendete Filter und Sortierung vergleichen Zeitpunkte. Ein ungültiger Entwurf ersetzt keinen angewendeten Filter. Absolute Zeitstempel und Filterchips enthalten zusätzlich den UTC-Offset zur Unterscheidung der Herbststunde. Zukünftige Zeitpunkte werden weiterhin absolut angezeigt. |
| `AdminUserActivityText` | Minuten, Stunden, Tage und Wochen basieren auf verstrichener Dauer; ein Tag entspricht 24 Stunden. Monate und Jahre bleiben bewusst Kalenderabstände in der Anzeigezone. Bestehende DE-/EN-Texte werden wiederverwendet. |

Die Clock-Erweiterung bleibt auf die Aktivitätsverarbeitung begrenzt. Es gibt
keinen neuen globalen Clock-Bean und keine Nebenwirkung auf Mailqueue,
Tokenlaufzeiten, Remember-me oder andere vorhandene Zeitverträge.

## Regressionstests

15 zusätzliche Testausprägungen:

- Zwei DE-/EN-Fälle für tatsächliche Stunden und 23-/25-Stunden-Tage beim
  Frühlings- und Herbstwechsel; bestehende Monats-/Jahresgrenzen laufen weiter.
- Drei Filterfälle für Frühlingslücke, Korrektur, Herbstüberlappung und Zurücksetzen.
  Die vorhandenen Platzhalterprüfungen verwenden jetzt eine feste Clock mit
  abweichender Zone statt die tatsächliche Uhrzeit.
- Drei Servicefälle mit derselben Instant-Clock in UTC, Berlin und New York.
- Drei echte Hibernate-/H2-Fälle mit verschiedenen Datenbank-Sitzungszeitzonen:
  beide Vorkommen von 02:30 bleiben getrennt, Typmetadaten bestätigen
  `TIMESTAMP WITH TIME ZONE`, Rücklesen und Repository-Cutoffs bleiben korrekt.
- Zwei echte Scheduler-/Persistenzfälle über beide Zeitwechsel: keine verfrühte
  Abwesenheit, unveränderte Gleichheitsgrenze, Wechsel nach Ablauf und Rückkehr
  durch neue Aktivität.
- Zwei DE-/EN-Komponentenfälle für relative Texte, verschiedene absolute Offsets,
  chronologische Sortierung, inklusiven Filter, Beibehaltung bei ungültigem
  Entwurf und zukünftige Zeitpunkte.

Bestehende konkurrierende Profil-/Status-/Aktivitätsfälle werden mit dem neuen
Zeitpunkttyp unverändert fachlich geprüft. Keine globale JVM-Zeitzone wird im
laufenden Testprozess umgestellt; H2-Verbindungen gehören nur zu flüchtigen
Testdatenbanken.

## Prüfergebnis

| Prüfung | Ergebnis |
| --- | --- |
| Erste gezielte Aktivitäts-/Status-/Filterrunde | 187/187 erfolgreich |
| Abschließende Komponenten-Nachprüfung nach Filtervalidierung und DE-/EN-Erweiterung | 28/28 erfolgreich |
| Vollständiger Maven-Testlauf mit Produktionsprofil und `verify` | **1971/1971 erfolgreich**, keine Fehler oder Skips; ausführbares Produktions-JAR gebaut |
| Browser-Helfertests ohne Browser-/Anwendungsstart | **48/48 erfolgreich**, keine Fehler oder Skips |
| Root Spotless apply/check und Checkstyle | Erfolgreich, keine Stilabweichungen |
| Abschließende Diff-/Whitespaceprüfung | Erfolgreich |

Gegenüber dem Ausgangsstand sind 15 Testausprägungen hinzugekommen. Die gezielten
Runden sind Teilmengen der 1971 Anwendungstests und werden nicht zusätzlich als
neue eindeutige Tests gezählt.

Frische JaCoCo-Messung ausschließlich aus dem vollständigen aktuellen Testlauf:
**10198/11627 Zeilen = 87,71 %**, **2957/3848 Zweige = 76,85 %**,
219 analysierte Anwendungsklassen. `UserActivityTime` und `AdminUserActivityText`
sind in Zeilen und Zweigen vollständig abgedeckt. Scheduler: 33/33 Zeilen,
7/8 Zweige. Die übrigen Lücken betreffen weiterhin auch andere Anwendungsbereiche;
die Gesamtquote ist kein Nachweis einer vollständigen Browser- oder Betriebsabnahme.

Der Build verwendet JDK 25.0.4.1, Maven Wrapper 3.9.16 und die unveränderten
Projektabhängigkeiten. Die Stilprüfungen wurden nach den letzten Java-Korrekturen
vor dem vollständigen Testlauf erfolgreich wiederholt.

## Einführung und Grenzen

Shared ist unverändert. Anwendung mit dem aktuellen Stand bauen und vollständig
neu starten; ein Browser-Refresh allein übernimmt die Java-Änderung nicht.
Für den vereinbarten Neuaufbau ist ein frisches Datenbankschema vorgesehen.
Ein Start gegen ein altes Schema mit `ddl-auto=update` wäre keine fachlich
validierte Datenübernahme und ist kein Bestandteil dieser Umsetzung.

Keine neue Browser-UI-Abnahme und kein neues Dockerimage für dieses Paket.
Die neuen UI-Nachweise sind serverseitige Komponenten-JUnits, keine Browserfälle.
Die früheren CSRF-Browserergebnisse belegen ihren damaligen Stand. Browserläufe
benötigen gemäß AGENTS.md eine eigene ausdrückliche Freigabe. Ein erneuter
Sonar-Scan und eine Bewertung im Sonar-Server wurden nicht durchgeführt.

## Grundlagen

- [Hibernate ORM 7.4: Instant und zeitliche Typen](https://docs.hibernate.org/orm/7.4/userguide/html_single/#basic-datetime-instant)
  ordnet `Instant` dem Typ `TIMESTAMP_UTC` zu; das tatsächliche H2-Schema wird
  zusätzlich im Test über JDBC-Metadaten geprüft.
- [Java 25: ZonedDateTime](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/time/ZonedDateTime.html)
  beschreibt Lücken und Überlappungen. Die Anwendung verlässt sich nicht auf
  das automatische Vorwärtsschieben einer nicht existierenden Filterzeit.
- [Ursprüngliche Analyse](sonar-time-semantik-2026-10-20.md) dokumentiert den
  vorherigen lokalen Speichervertrag; die Entscheidung zum vollständigen
  Neuaufbau ersetzt dort die noch offene Frage einer historischen Übernahme.
