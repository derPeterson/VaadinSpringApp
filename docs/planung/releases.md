# Release-Versionierung – offene Abschlussarbeiten

Branch: `codex/release-versionierung`, Basis main `5ba5a879b0cc5bc26b9e32699952c76851fbd0bc`.
Der Benutzer hat Umsetzung, große Testrunde und einen bedingten kleinen lokalen
Release freigegeben. Ziel: `0.1.0`, danach `0.1.1-SNAPSHOT`. Keine Veröffentlichung.

## Bereits vorbereitete Grundlage

Projektversionen und interne Abhängigkeiten sind konsistent, der JAR-Pfad ist
versionsunabhängig, Build-Metadaten enthalten Version/Revision/Dirty-Zustand.
Changelog, Release-Helfer und Skill sind angelegt. Die 14 gezielten Release-Tests
bestehen; der echte Maven-Build liefert die richtige Git-Revision. Die gültige
Bedienung und Grenzen stehen in [Releases](../releases.md).

## Offene Arbeitspakete

- [ ] Abschließendes Review der Implementierung, Kompilierung und Browser-Helfertests.
  Dann den technischen Stand committen und mit dem neuen Helfer den Kandidaten
  `0.1.0` samt ehrlichen DE-/EN-Notizen vorbereiten und separat committen.
- [ ] Den geprüften Kandidaten lokal nach main integrieren, damit der vorgesehene
  geschützte Sonar-Gesamtscan eindeutig auf main läuft. Kein Push. Quellbranch erhalten.
- [ ] Große Testrunde: vollständiger optimierter Reactor und gemeinsame Coverage,
  Frontend-/Offlineprüfungen einschließlich Release-Helfer sowie Browser `all` in
  DE und EN am selben unveränderten Image. Den gesamten [Testbedarf](../testbedarf.md)
  einbeziehen, besonders die korrigierte Mailqueue-Meldungsprüfung und den
  Dauerfeld-Nachtest aus [BUGS](../../BUGS.md). Neue Aura-Fälle prüfen deaktivierte
  Felder, Kalender/Zeitauswahl und die Hintergrund-Geometrie in vier Breiten.
- [ ] Erforderliche Korrekturen im bestehenden Fach-/Designvertrag beheben und
  passend nachprüfen. Kein automatischer Umbau weiterer Funktionen. Sonar und
  Testerfolg getrennt belegen; nur bei insgesamt grünem Ergebnis fortfahren.
- [ ] Die exakte getestete JAR-Version mit Revision und SHA-256 qualifizieren,
  lokal mit Manifest aufbewahren und unveränderlich als `v0.1.0` markieren.
  Der getestete Releasecommit darf nicht durch nachträgliche Redaktion umgedeutet werden.
- [ ] Nach erfolgreicher QA die Aura-Umstellung abschließen: bestandene Bug-/Testbedarf-
  Einträge, ihren TODO und den erledigten [Aura-Plan](aura-umstellung.md) samt Verweisen
  entfernen. Nicht geprüfte oder wartende Aufgaben erhalten. Die separate
  Vaadin-Komponenten-Gesamtumsetzung bleibt ausdrücklich offen.
- [ ] Nächsten Patch-Snapshot `0.1.1-SNAPSHOT` separat committen. Die rein redaktionelle
  QA-/Planungsbereinigung darf in diesem Folgecommit liegen; dabei die erfolgreiche
  QA weiterhin eindeutig dem Releasecommit und dessen Image zuordnen.
- [ ] Diesen erledigten Plan samt Verweisen entfernen; Bedienung in `docs/releases.md`,
  echte Prüfergebnisse in `docs/status.md`, Releaseartefakt/Manifest lokal erhalten.

## Freigabegrenzen

Eigene isolierte Dienste und frische Testdaten verwenden. Keine vorhandene Datenbank,
keine echten Mails, kein Push, keine Registry-Veröffentlichung und kein Deployment.
Fehlende Nachweise verhindern den Release; sie werden nicht durch gelöschte Aufgaben
oder erfundene QA-Einträge ersetzt. Die Neuigkeiten-Oberfläche bleibt ein eigener Auftrag.
