---
name: umsetzung-starten
description: "Neue Features in VaadinSpringApp mit Feature-Branch und Planung beginnen und nach Freigabe bis zur gemeinsamen Abnahme begleiten. Gilt für Umsetzung starten sowie natürliche Folgeaufträge zum aktiven Feature, zu Paketen und dessen UI. Einzelne unabhängige Korrekturen oder Skill-Erstellung eröffnen keinen neuen Feature-Ablauf."
---

# Umsetzung Starten

Lies zuerst `README.md`, `AGENTS.md` und die für das Feature relevanten Quellen und
Dokumente. Nutze die vorhandene Architektur, Vaadin-Komponenten und das Cosmos-Theme.
OpenCode bleibt unverändert. Beispielhafte Feature-Namen in einer Diskussion oder
beim Erstellen dieses Skills sind keine tatsächlichen Umsetzungsaufträge.

## 1. Einmaliger Einstieg: Branch und Plan

- „Umsetzung starten: FEATURE“ eröffnet den Arbeitsablauf. Git-Stand und vorhandene
  Feature-Pläne prüfen; fremde Änderungen erhalten. Standardmäßig einen eigenen
  `feature/<passender-name>`-Branch vom aktuellen lokalen main anlegen. Einen vom
  Benutzer genannten Branch/Basisstand verwenden; ausdrücklich gewünschte Arbeit
  ohne neuen Branch respektieren. Keine bestehenden Branches zurücksetzen/überschreiben.
- Ist bereits dieses Feature aktiv, seinen Branch/Plan weiterverwenden. Nicht bei
  jeder Folgemeldung erneut abzweigen. Bei einem wirklichen Konflikt nur die fehlende
  Entscheidung klären, nicht pauschal die bereits beauftragte Arbeit erneut freigeben lassen.
- Anforderungen anhand der Beschreibung, bestehender Fach-/Sicherheitsverträge und
  tatsächlicher Aufrufer prüfen. Sinnvolle Arbeitspakete nach Funktion und Abhängigkeit
  bilden; kleine Aufgaben nicht künstlich zerlegen. Annahmen und echte offene Fragen nennen.
- Einen befristeten Plan unter `docs/planung/<feature>.md` anlegen. Knapp festhalten:
  Ziel und Umfang, Branch/Basis, aktuelle freigegebene Arbeit, offene Pakete,
  relevante Entscheidungen, Test-/Abnahmekriterien und gegebenenfalls gemeinsame UI-Abnahme.
  Ein Plan pro Feature; kein paralleler Planstapel und keine Chronik jedes Gesprächsschritts.
- Den Plan zur Diskussion vorlegen und anhalten. Featurestart allein erlaubt die
  Branch-/Plananlage, noch keine Implementierung, Anwendungsstarts oder Browserläufe.
  Bei gleichzeitig ausdrücklich beauftragter Planung und Umsetzung den ganzen
  Benutzerauftrag beachten; eine ausdrücklich verlangte Planabnahme nicht überspringen.

## 2. Mit normalen Nachrichten weiterarbeiten

| Nachricht im aktiven Feature | Bedeutung |
| --- | --- |
| „Am Plan würde ich X ändern; bei Y habe ich eine Frage“ | Erläutern und Plan aktualisieren; noch keine Umsetzung aus bloßer Diskussion ableiten |
| „Setze den Plan komplett um“ | Die freigegebenen Pakete in sinnvoller Reihenfolge bearbeiten; notwendige neue Grundsatzentscheidungen zurückstellen |
| „Erst Schritt 1 und 2, danach schaue ich“ | Genau diesen Umfang umsetzen und danach anhalten |
| „Integriere jetzt die Oberfläche mit unseren Komponenten“ | Freigegebenen UI-Umfang im bestehenden Theme umsetzen; keine freie Neugestaltung |
| „Buttons rechtsbündig; zeige fünf Einträge; ergänze diesen Filter“ | Als konkrete Änderung am aktiven Feature behandeln, nicht als neuen Featurestart |
| „Umsetzung abgeschlossen“ / „Das Feature ist fertig“ | Fachliche Abnahme und Abschlussvorbereitung; allein daraus keinen Merge oder Commit ableiten |
| „Merge das nach main“ | Zugleich fachliche Abnahme und Auftrag zur Abschlussbereinigung/Integration; keine zusätzliche Abschlussformel verlangen |

- Der Benutzer muss den Skill nicht wiederholt aufrufen. Aktiven Zusammenhang aus
  Gespräch, Git-Branch und Feature-Plan erhalten. Bei neuem Chat den Plan zur
  Orientierung lesen; fehlende Freigaben nicht aus einem alten Dateinamen erfinden.
- Rückmeldungen und neue konkrete Wünsche gehören zum laufenden Feature. Nur bei
  wesentlicher Umfangs-/Vertragsänderung Auswirkungen und nötige Entscheidung ansprechen.
  Keine neue Planfreigabe für jede kleine bereits beauftragte UI-Korrektur verlangen.
- Den Plan auf den heutigen offenen Stand bringen. Erledigte technische Schritte
  entfernen; offene gemeinsame Prüfung oder nächste Pakete bleiben erhalten. Nach
  Teilumsetzung berichten, was fertig ist und was auf Rückmeldung wartet.

## 3. Freigegebenen Umfang umsetzen und prüfen

- Kleine zusammenhängende Fehler fachlich beheben. Bei größeren Zusatzfunktionen,
  Auswirkungen auf andere Bereiche oder grundlegenden Sicherheits-/Verhaltensänderungen
  gezielt fragen. Ohne Antwort den betroffenen Teil offen lassen und unabhängige
  freigegebene Arbeit fortsetzen; Schweigen ist keine Zustimmung.
- Neue UI nur soweit im freigegebenen Umfang enthalten erstellen. Bestehende Cosmos-
  Bausteine für eine bedienbare Grundlage nutzen. Keine eigenen globalen Tokens,
  Icons, Theme- oder Layoutumbauten als Nebenwirkung einführen. Grundlegende
  Gestaltung und spätere Verfeinerung gemeinsam mit dem Benutzer abstimmen.
- Backend-only-Aufgaben ohne unnötige UI-Phase bearbeiten. Ein technischer Erfolg
  führt nicht automatisch zu einer zusätzlich erfundenen sichtbaren Funktion.
- Sinnvolle JUnit-/isolierte Regressionstests für das geänderte Verhalten ergänzen
  und passende vorhandene Tests ausführen. Vorgeschriebene Stil-/Formatierungsschritte
  gemäß `AGENTS.md` vor Tests und Abschluss beachten. Keine Implementationskopien als Tests.
- Noch nicht erfolgreich geprüften Browser-/anderen Testbedarf zentral in
  `docs/testbedarf.md` pflegen: Funktion, Szenario, Erwartung, Testart und Grund.
  Vorhandene Aufgaben zusammenführen; eine Lücke nicht nur im Feature-Plan ablegen.
- Keine automatische Browserrunde, große Testrunde oder Sonar-Bereinigung starten.
  Menschliche UI-Durchsicht und JUnit-Ergebnisse nicht als Playwright-Abnahme ausgeben.
  Bereits ausdrücklich beauftragte passende Prüfungen ohne erneute Rückfrage ausführen.
- Bestehende Dokumentation beschreibt das aktuell gültige Verhalten. Keine neue
  Migration, Kompatibilitätsschicht oder Berichtshistorie vorsorglich hinzufügen.

## 4. Gemeinsame Abnahme und Abschluss

- Technisch fertige Pakete als solche melden, nicht eigenmächtig das gesamte Feature
  schließen. Bei UI auf die gemeinsame Funktions-, Bedienungs- und Designabnahme warten.
  Der Feature-Plan bleibt bis dahin mit den tatsächlich offenen Schritten bestehen.
- Ein ausdrücklicher Mergeauftrag nach main gilt bereits als Benutzerabnahme und
  Abschlussauftrag. Nicht zusätzlich „Umsetzung abgeschlossen“ verlangen. Bei einem
  anderen Zielbranch nicht automatisch den endgültigen Featureabschluss annehmen,
  sofern dieser nicht ebenfalls eindeutig beauftragt ist. Tatsächlich abgeschlossen
  erst nach erforderlicher Bereinigung, erfolgreicher Integration und Nachprüfung melden;
  bei offenen Änderungen oder fehlgeschlagenem Merge/Build den Rest ausdrücklich offen halten.
- Bei einem Mergeauftrag zuerst den [Merge-Skill](../branch-lokal-mergen/SKILL.md)
  anwenden, insbesondere dessen Prüfung auf bestehende uncommittierte Änderungen
  vor jeder Abschlussbereinigung. Die Regeln für einen sauberen Arbeitsstand und
  den Benutzercommit gelten auch im Featureablauf.
- Kleine iterative Änderungen bleiben auf demselben Branch. Viele sinnvolle
  Zwischencommits sind möglich; die Zahl bestimmt nicht den fachlichen Abschluss.
  Standardmäßig Commit-Messages liefern. Nur auf entsprechenden Auftrag committen;
  kein automatischer Commit/Merge/Push durch einen Umsetzungs- oder Abnahmeauftrag.
- Nach Benutzerabnahme featurebezogene Provisorien, nachweislich überflüssigen Code,
  Ressourcen und erledigte Plan-/TODO-Verweise bereinigen. Keine pauschale Bereinigung
  fremder Bereiche. Wichtige offene Folgefunktionen nach TODO, Testlücken in den Testbedarf
  übernehmen; danach den erledigten Feature-Plan löschen. Gültige Fach-/Bedienungsdoku aktualisieren.
- Abschlussbereinigung auf dem Feature-Branch durchführen und relevante Nachtests
  ausführen. Bereit zur Integration melden bzw. einen ausdrücklich beauftragten Merge
  durchführen. Nach Merge tatsächlichen Git-Stand und verbleibende Prüfgrenzen nennen.
- Kurz auf Deutsch berichten: fertiger Umfang, wirkliche Prüfungen, offene Abnahmen/
  Testaufgaben, benötigter Build/Neustart/Refresh und gegebenenfalls Commit-Message.
  Kein neuer datierter Feature-Abschlussbericht neben dem aktuellen Plan und Git.
