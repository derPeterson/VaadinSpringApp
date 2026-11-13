# Release-Ablauf und Versionierung

## Empfehlung und Umfang

Noch nicht umgesetzt. Erst nach gemeinsamer Durchsicht freigeben. Die heutigen
Projektversionen bleiben unverändert. Ein Release bezeichnet einen geprüften,
eindeutig wiederherstellbaren Softwarestand, nicht automatisch eine Veröffentlichung
im Internet. Das Deployment wird getrennt in der Hostingplanung behandelt.

Für dieses gemeinsam entwickelte Portal empfehlen sich kleine, fachlich sinnvolle
Releases nach abgenommenen Umsetzungen. Zwischencommits und Layoutkorrekturen
erzeugen keine eigene Version. Eine ausdrücklich beauftragte Fehlerkorrektur kann
ebenfalls als kleiner Release abgeschlossen werden.

## Versionen statt Commitzähler

Empfohlener erster bewusst versionierter Entwicklungsrelease: **0.1.0**. Das ist
eine vorgeschlagene neue Ausgangsbasis, keine Behauptung über bereits veröffentlichte
Stände. Aktuell haben Root, Shared, App und Browsermodul `1.0-SNAPSHOT`; als einziger
vorhandener Git-Tag ist `archiv/html-referenzen` erkennbar. Er ist kein App-Release.

| Stand oder Änderung | Projektregel / Beispiel |
| --- | --- |
| Laufende Arbeit am nächsten Funktionspaket | `0.2.0-SNAPSHOT`; beliebig viele Commits, konkrete Revision zusätzlich über Git-Hash |
| Abgenommenes neues Funktionspaket | `0.1.0` → `0.2.0` |
| Kleine Fehlerkorrektur ohne neue Funktion | `0.2.0` → `0.2.1` |
| Stabilitätszusage für regulären Betrieb | Bewusster Übergang zu `1.0.0`, nicht bloß weil eine Versionszahl erreicht wurde |
| Danach inkompatible Änderung eines zugesagten Vertrags | Major-Version, etwa `1.4.2` → `2.0.0` |

Das ist ein SemVer-orientierter Projektvertrag. SemVer selbst bezieht sich auf eine
definierte öffentliche API. Für unser Portal müssen zusätzlich zugesagte Anmeldewege,
URLs, gespeicherte Daten und die Bedienung bestehender Konten betrachtet werden.
Vor `1.0.0` kennzeichnen neue beziehungsweise inkompatible Funktionsstände die
Minor-Version. Die Patch-Stelle verwenden wir bereits dort für reine Korrekturen.
Größe des Diffs oder Anzahl der Commits bestimmen die Version nicht.

Nach einem Release wird die geplante nächste Entwicklungsversion gesetzt, etwa
`0.2.0-SNAPSHOT`. Ist später ausschließlich ein Patch der bisherigen Linie nötig,
ist der Kandidat bewusst auf `0.1.1-SNAPSHOT` festzulegen. Ein bereits vergebener
Release-Tag und dessen Artefakt werden niemals nachträglich umgedeutet.

Maven-Snapshots sind bewegliche Entwicklungsstände. Beim Veröffentlichen in ein
Maven-Repository versieht Maven sie mit Zeitstempel/Buildnummer. Ein lokaler
`package`-Build wird dadurch nicht automatisch zu einer solchen Veröffentlichung.
Für uns reichen zunächst Entwicklungsversion plus Git-Hash; Jenkins oder ein
Artefakt-Repository sind dafür keine Voraussetzung.

## Konkrete Projektstellen

- Root aggregiert die Module, `app` erbt separat von Spring Boot. Eine Änderung
  nur der Root-Version erreicht daher nicht automatisch alle Projektversionen.
- Gemeinsame Version in den vier POMs sowie Parent-/Shared-Abhängigkeitsverweise
  konsistent ändern. Externe Bibliotheks- oder Boot-Versionen dabei nicht ersetzen.
- Das Dockerfile kopiert derzeit ausdrücklich `app-1.0-SNAPSHOT.jar`. Vor dem
  ersten Release den Artefaktpfad unabhängig von der Versionsnummer machen,
  beispielsweise über einen festen Maven-`finalName` für das ausführbare App-JAR.
- Entsprechende Skripte, Tests, Dokumentationsbeispiele und Image-/Coverage-
  Verbraucher gezielt nach Projektartefakt-Namen durchsuchen und nachziehen.
- Build-Metadaten mit Projektversion und Quellrevision in das Artefakt übernehmen.
  Ein Dirty-Build darf nicht als unveränderter Release-Tag ausgegeben werden.
- Daten-/Schemaverträge vor öffentlichem Betrieb neu bewerten. Ein Git-Rollback
  ist kein verlässlicher Datenbank-Rollback. Die lokale Entwicklung bleibt bis
  dahin beim aktuellen Schema und austauschbaren isolierten Testdaten.

## Ein künftiger Release-Auftrag

Der geplante Skill soll aus „Release erstellen“ zunächst den tatsächlich letzten
Release und die Änderungen seit diesem Stand ermitteln. Er liest zusätzlich die
abgenommenen Funktionspakete und noch unveröffentlichten Notizen, statt allein
Commit-Titel auszuwerten. Ausgabe vor dem Versionsschritt: vorgeschlagener Typ,
Nummer und kurze Begründung. Ein expliziter Versionswunsch wird auf Konsistenz
geprüft. Unklare Kompatibilitätsbrüche bleiben eine gezielte Benutzerentscheidung.

Vorgesehene Abfolge nach späterer Freigabe des Skills:

1. Auftrag, Quellbranch, sauberen Arbeitsstand und fachliche Abnahme prüfen.
   Ein Zwischenmerge wie im Nachtauftrag ist ausdrücklich keine Release-Abnahme.
2. Releaseumfang und Nummer bestimmen, passende aktuelle Prüfnachweise zuordnen.
   Offene Bugs oder notwendige nicht ausgeführte Tests sichtbar behandeln.
3. Projektversion und Release-Notizen am Kandidaten konsistent aktualisieren,
   reviewen und committen. Den Kandidaten mit dem vorhandenen Root-Reactor bauen
   und prüfen, isolierten Start und benötigte Browser-/Sonar-Nachweise absichern.
   Keine erfolgreichen unveränderten Nachweise ohne Grund nochmals erzeugen.
4. Erst nach Erfolg den exakten Kandidaten lokal als `vX.Y.Z` taggen und das
   passende JAR beziehungsweise Container-Image mit Prüfsumme/Revision zuordnen.
   Bei Fehlern keinen fertigen Release oder erfolgreichen Tag behaupten.
5. Nächste Entwicklungsversion mit `-SNAPSHOT` setzen und separat committen.
   Quellstand des Releases und Weiterentwicklungsstand unterscheidbar halten.
6. Ergebnis mit Version, Tag, Artefakt, tatsächlichen Prüfungen und offenen
   Grenzen nennen. Push, Registry-Veröffentlichung und öffentliches Deployment
   nur als ausdrücklich im späteren Auftrag enthaltene Schritte ausführen.

Der neue Skill und nötige Helfer werden noch nicht angelegt. Seine späteren
Git-Befugnisse müssen ausdrücklich zu diesem Ablauf passen. Er darf nicht
unbeabsichtigt durch eine allgemeine Commit-Message-Anfrage ausgelöst werden.

## Gepflegte Release-Notizen

Eine künftig eingeführte `CHANGELOG.md` bildet die gemeinsame redaktionelle Quelle.
Oben stehen noch unveröffentlichte Änderungen. Bei Abnahme einer Umsetzung werden
deren wesentliche nutzbare Funktionen dort zusammengefasst, nicht jeder Zwischenstand.
Mit dem Release erhält der Abschnitt Version und tatsächliches Freigabedatum.

Geeignet: „Konten lassen sich vorübergehend deaktivieren und per E-Mail wieder
aktivieren.“ Ungeeignet: „Button dreimal verschoben, Klassen neu formatiert.“
Nennenswerte Fehlerkorrekturen mit erkennbarem Nutzereffekt sind sinnvoll.
Reine technische Wartung gehört in technische Dokumentation/Commits und muss nicht
als Benutzerneuigkeit erscheinen. Nachträglich nicht die gesamte Git-Geschichte
automatisch als angebliche Release-Historie importieren. Die erste Basisversion
bekommt eine kurze ehrliche Zusammenfassung der dann vorhandenen Funktionen.

DE und EN werden im selben versionierten Quelldokument klar getrennt geführt.
Funktionale Einträge werden beim fachlichen Abschluss gepflegt; der Review prüft
Vollständigkeit und streicht reine Kosmetik. Für die verbindliche Pflege sind
später der Umsetzungsabschluss und Release-Skill anzupassen, keine zeitgesteuerte
Automation. Die sichtbare Nutzung folgt der [Neuigkeitenplanung](neuigkeiten.md).

## Offene Umsetzungspakete

- [ ] Startversion und die hier vorgeschlagenen Regeln gemeinsam abnehmen.
- [ ] Gemeinsame Projektversion und versionsunabhängigen JAR-/Dockerpfad vorbereiten.
- [ ] Changelog-Struktur, DE/EN-Abschnitte und Abschlussregel einführen.
- [ ] Release-Helfer und Skill mit sicherer Vorschau, Fehlerfällen und
  ausdrücklichem Git-/Veröffentlichungsumfang implementieren und gezielt prüfen.
- [ ] Ersten Release bewusst beauftragen und danach den folgenden Snapshotstand
  nachweisen. Die jetzige Nachtintegration erzeugt diesen Release noch nicht.

## Grundlagen

[Maven: Release-Ablauf](https://maven.apache.org/guides/mini/guide-releasing.html),
[Maven: tatsächliche Snapshot-Veröffentlichungen](https://maven.apache.org/repositories/remote.html),
[Semantic Versioning](https://semver.org/lang/de/),
[Keep a Changelog](https://keepachangelog.com/en/2.0.0/).
