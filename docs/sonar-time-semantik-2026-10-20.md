# P2-TIME: Speicher- und Zeitzonensemantik

Stand: 20.10.2026. **Nur Analyse; keine Zeitberechnung, Spalte oder historische Zeile geändert. Keine bestehende Datenbank geöffnet.**

## Bestehender Vertrag

| Stelle | Tatsächliches Verhalten |
|---|---|
| UserEntity.lastActivity / updateLastActivity | LocalDateTime ohne gespeicherten Offset und ohne ZoneId; Initialisierung und Aktualisierung mit LocalDateTime.now(). |
| UserService.updateLastActivity und UserRepository.updateLastActivity | Schreiben diesen lokalen Wert direkt per JPA-Bulkupdate. Kein expliziter UTC-Konverter an der Stelle. |
| Standard-Hibernate-Zuordnung | LocalDateTime entspricht einem TIMESTAMP ohne eigenständige Zeitzoneninformation. Im Modell keine abweichende Zeitspalten-/Konverterdefinition gefunden. Produktive JDBC-Metadaten wurden nicht gelesen. |
| AdminUserManagementSection / AdminUserActivityText | Vergleicht gespeicherte lokale Aktivität mit LocalDateTime.now(); Duration nutzt damit Wandzeit. Monate/Jahre werden bewusst als Kalenderabstände berechnet. |
| AdminUserActivityFilter / Repositoryfilter | Filtern und sortieren nach denselben lokalen Werten; Datum und Zeit werden ohne Benutzer-ZoneId erfasst. |
| UserStatusScheduler | Bildet lokale Cutoffs für AVAILABLE/ABSENT. Das ist ein weiterer Nutzer derselben Speichersemantik; eine spätere Migration darf ihn nicht auslassen. Daraus wurde kein zusätzlicher ungeprüfter Sicherheitsfehler abgeleitet. |
| compose.yaml | Konfiguriert Europe/Berlin für den App-Container. |
| Anwendung außerhalb Compose | Die Laufzeit-Systemzone bestimmt now(). Keine allgemeine Zeitzonenfestlegung oder hibernate.jdbc.time_zone in den geprüften Anwendungsproperties gefunden. Historische Laufzeit-Zonen sind damit nicht aus der Zeile selbst rekonstruierbar. |
| Bevorzugte Sprache / Geburtsdatum | Locale ist keine Zeitzone. Ein Geburtsdatum bleibt fachlich LocalDate und gehört nicht in eine pauschale Instant-Umstellung. |

## Konkretes Problem und Grenzen einer kleinen Reparatur

Die isolierte Java-Prüfung zeigt für Europe/Berlin am 29.03.2026 von 01:30 bis 03:30: lokale Duration **2 h**, tatsächlich verstrichene Zeit **1 h**. Das betrifft die behauptete relative Aktivitätsdauer, wenn verstrichene Zeit gemeint ist.

Eine pauschale Umdeutung aller vorhandenen LocalDateTime-Werte mittels atZone(Europe/Berlin) löst die historische Mehrdeutigkeit nicht: Am 25.10.2026 existiert 02:30 zweimal; am 29.03.2026 existiert 02:30 in dieser Zone nicht. Ohne gespeicherten Offset kann man die ursprüngliche tatsächliche Aktivität nicht immer bestimmen. Auch ein Wechsel der Systemzone auf UTC ändert die Interpretation neuer Werte und ist keine neutrale Anzeigeänderung.

## Fachliche Entscheidung vor Umsetzung

**Option A: ausdrücklich lokale Kalender-/Wandzeitanzeige.** Den bestehenden Vertrag dokumentieren und die Sonar-Abweichung begründet zur Entscheidung zurückstellen. Wenn die UI hingegen „vor einer Stunde“ als tatsächliche Dauer verspricht, ist diese Option fachlich nicht gleichwertig und darf nicht stillschweigend gewählt werden.

**Option B: tatsächliche verstrichene Aktivitätsdauer.** Für neue Aktivitätsereignisse einen eindeutigen Zeitpunkt (Instant beziehungsweise eindeutig persistierten Offset) verwenden, eine explizite Clock wählen und die Anzeigezone separat bestimmen. Historische lokale Werte erhalten, separat kennzeichnen oder nur nach ausdrücklich beschlossener Herkunfts-/Mehrdeutigkeitsregel migrieren. Bei einer Übergangsphase einen gesonderten eindeutigen Zeitwert mit klar definiertem Legacy-Verhalten erwägen. Datenmigration und Status-/Filteranpassung benötigen einen eigenen abgestimmten Umfang.

Empfehlung: Zuerst entscheiden, ob die Anzeige verstrichene Zeit garantieren soll und welche Zeitzone historische Bestände tatsächlich verwendeten. Keine automatische Schemaänderung durch einen bloßen Java-Typwechsel bei aktiviertem ddl-auto=update auslösen.

## Geplante Nachtests

- Frühlingswechsel: 01:30 bis 03:30 ergibt bei Zeitpunkten eine Stunde.
- Herbstwechsel: zwei Aktivitäten mit derselben lokalen Uhrzeit und verschiedenen Offsets bleiben unterscheidbar.
- Expliziter Umgang mit mehrdeutigen oder nicht existierenden historischen lokalen Zeiten; keine stillschweigende Offsetauswahl.
- Gemeinsame Semantik für Aktivitätsanzeige, Sortierung, Filter und Abwesenheits-Cutoff; bestehende Sperr-/Versionsgrenzen erhalten.
- Fixierte Clock und ausgewählte Laufzeit-Zonen, kein globales Setzen der Systemzone in parallel laufenden Tests.
- DE/EN-Anzeige, Minuten-/Stunden-/Tagesgrenzen sowie bewusst kalenderbezogene Monats-/Jahresgrenzen erhalten.

Die vorhandenen sechs Ausprägungen von AdminUserActivityTextTest laufen weiterhin unverändert. Sie prüfen die bisherigen Grenzen in DE/EN, aber noch keine behobene Sommerzeitsemantik. P2-TIME bleibt fachlich offen.
