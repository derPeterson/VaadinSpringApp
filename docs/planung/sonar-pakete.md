# Offene Sonar-Bereinigung

Der aktuelle Main-Scan `b94ed53d-91be-4f3b-8b4b-6a9eff01e8ea` gehört zu
`3abb9139`. Er enthält zwölf offene Codequalitätshinweise, keine Bugs,
Sicherheitslücken oder offenen Hotspots. Das Quality Gate ist wegen dieser
zwölf Hinweise rot. Bestehende begründete Ausnahmen bleiben erhalten.

## Paket: Nachtänderungen verständlicher und analysierbar machen

Auf `codex/sonar-vaadin-folgearbeiten` die zwölf Hinweise bearbeiten:

- Mailqueue: nullable Ergebnisse korrekt kennzeichnen, garantierte
  Paketergebnisse klar behandeln und automatische Vormerkung aus dem
  verschachtelten Fehlerblock lösen; Transaktionen und Sperren erhalten.
- Dialoge und Hintergrund: Initialfokus-Skript aus der Lambda ziehen,
  verschachtelte Löschbestätigung zusammenfassen und wiederholte Werte benennen.
- Tests: Berechtigungsassertion eindeutig abgrenzen, Queryparameter ohne
  unnötiges Regex-Backtracking abwarten und Hilfstab-Kommentar klar formulieren.

Die zwölf Finding-IDs und der vollständige Export liegen im lokalen
Sonar-Arbeitsstand. Stilprüfung, 171 gezielte Anwendungstests und 113
Browser-Helfertests sind bestanden; die Änderungen sind reviewt. Lokal nach `main`
integrieren und den geschützten vollständigen Nachscan bis zum Serverergebnis
verfolgen. Ziel: null offene Findings und bestandenes Quality Gate.

Die abschließende große Browserrunde folgt danach am endgültigen Image.
