---
name: release-erstellen
description: "Einen ausdrücklich gewünschten lokalen VaadinSpringApp-Release vorbereiten, prüfen, taggen und auf den nächsten Snapshot weiterstellen. Nicht bei Zwischencommits, bloßen Versionsfragen, Skill-Erstellung oder automatischer Featureabnahme starten."
---

# Release erstellen

README, AGENTS, BUGS, docs/testbedarf.md, docs/status.md und die
[Release-Anleitung](../../../docs/releases.md) lesen. Tatsächlichen Branch,
sauberen Arbeitsstand und vorhandene `vX.Y.Z`-Tags prüfen. Fremde Änderungen erhalten.

## Umfang und Vorschau

Der ausdrückliche Release-Auftrag erlaubt die nötigen eigenen lokalen
Kandidaten-/Snapshotcommits und einen neuen annotierten Release-Tag. Keine Tags
verschieben oder ersetzen. Push, Registry-Veröffentlichung, Deployment und Änderungen
an bestehenden Daten sind gesonderte Aufträge. Ein Releaseauftrag allein ersetzt
keine fachliche Abnahme und erlaubt nicht automatisch die große Browserrunde.

1. `scripts/release/release.py preview` ausführen. Letzten echten Release,
   unveröffentlichte funktionale Änderungen und abgeschlossene Featurepläne gemeinsam
   beurteilen; Commitzahl und Layoutkorrekturen bestimmen keine Versionsnummer.
2. Vorgeschlagene Version und Begründung nennen. Explizite Versionswünsche beachten;
   bei einem neuen wesentlichen Kompatibilitätsbruch die Entscheidung erfragen.
   Nach dem ersten `0.1.0` kleine Korrekturen als Patch behandeln.
3. Bei passendem sauberen Snapshotstand `prepare --version X.Y.Z` ausführen.
   Der Helfer aktualisiert alle Projekt-POMs und DE-/EN-Notizen; er committet nicht.
   Den tatsächlichen Diff reviewen und den Kandidaten lokal committen.

## Prüfung und Markierung

4. Den exakten Kandidaten mit dem Root-Reactor und passenden Prüfungen absichern.
   Bereits erfolgreiche unveränderte Belege nicht grundlos wiederholen. Falls eine
   Gesamt-QA nötig, aber nicht freigegeben ist, die fehlende Freigabe benennen.
   Keinen Ersatzrunner anlegen und keine Prüfungen aus einer bloßen Versionsfrage ableiten.
5. `verify` prüft das ausführbare JAR auf Version, saubere Git-Revision und SHA-256.
   Der vollständige QA-Zustand muss die Releasequalifikation gemäß Anleitung belegen:
   echte Ergebnisse, beide Sprachen am gleichen Image, Coverage und grünes Sonar-Gate.
   Den Nachweis erst nach tatsächlich erfolgreichen Prüfungen eintragen. `verify`
   allein bestätigt keine bestandenen Tests. Keine falschen Freigaben zum Entsperren schreiben.
6. Erst dann `tag` ausführen: genau das geprüfte JAR wird lokal aufbewahrt und seine
   Revision annotiert markiert. Tag, Commit, SHA-256 und Manifest nachlesen.
   Bei Teilerfolg prüfen, ob nur das Artefakt vorbereitet oder der Tag schon erzeugt
   wurde; keine fremden Dateien löschen und keinen bestehenden Tag überschreiben.
7. Direkt am getaggten Stand `next` ausführen und die nächste Patch-Snapshotversion
   separat committen. Der Release bleibt unverändert wiederherstellbar.

Erledigte Featurepläne und Testeinträge erst nach den jeweiligen Nachweisen entfernen.
Dokumentationsabschluss und nächster Snapshot dürfen nach dem getesteten Releasecommit
folgen; nicht behaupten, dass dieser neue Entwicklungsstand bereits dieselbe QA hatte.
Kurz Version, Tag, Artefakt/Prüfsumme, wirkliche Prüfungen und verbleibende Grenzen nennen.
