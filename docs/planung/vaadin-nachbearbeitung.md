# Offene Nachbearbeitungen

Die vereinbarten Badge- und Formulargrößen aus den Punkten 1 und 2 stehen in der
[Theme-Anleitung](../theme.md). Die automatisierten Browsernachtests stehen im
[Testbedarf](../testbedarf.md). Die bisherigen manuellen Abnahmen sind im
[manuellen Katalog](../manual-tests/README.md) dokumentiert.

Die Gesamtumsetzung des [Vaadin-Komponenten-Abgleichs](vaadin-komponenten-abgleich.md)
bleibt offen. Die weitere Arbeit findet auf `codex/vaadin-komponenten-folgearbeiten-6`
statt. Voraussetzung für den Branchwechsel ist ein sauberer lokaler `main`, für
den Sonar null offene Findings und ein erfülltes Quality Gate bestätigt.

## Offene Schritte in vereinbarter Reihenfolge

6. **Als abschließende Prüfung der Umsetzung die große Testrunde auf diesem Folgebranch durchführen.**
   Alle offenen Einträge aus `docs/testbedarf.md` mit der vorhandenen Abdeckung
   abgleichen, fehlende sinnvolle Regressionen ergänzen und vollständig prüfen.
   Den vollständigen Build samt JUnit-, Helper-/Offline- und Stilprüfungen sowie
   Browser DE/EN auf Desktop und Smartphone gemäß QA-Ablauf ausführen. Beide
   Sprachen am selben unveränderten aktuellen Image prüfen. Gefundene Fehler im
   vereinbarten Umfang beheben und passende Nachtests bis zum belegten Endstand
   durchführen. `docs/status.md` aktualisieren; Testbedarf erst nach erfolgreicher
   passender Abdeckung entfernen. Blockierte oder nicht ausgeführte Fälle sichtbar
   erhalten. Keine bereits erteilten manuellen Abnahmen erneut anfordern.
7. **Erst nach der großen Testrunde die Teststrategie ausführlich analysieren.**
   JUnit- und Playwright-Bestand darauf prüfen, welche Funktions-, Sicherheits-
   und responsiven Bedienprüfungen erhalten bleiben müssen, welche übergenauen
   Gestaltungsprüfungen vereinfacht oder entfernt werden können und welche
   repräsentativen gemeinsamen Komponentenprüfungen sinnvoll bleiben.
   Konkrete Empfehlungen mit betroffenen Tests und erhaltenem Schutz vorlegen.
   Dies ist ausschließlich Analyse und Planung: keine Tests löschen, umschreiben
   oder deaktivieren. Ein anschließender Testumbau benötigt die Benutzerentscheidung.

   Schwerpunkte dieser Analyse:

   - Den vom Benutzer beobachteten Speicherbedarf von teils 50–60 GB pro größerem
     Browserlauf untersuchen. Tatsächliche Anteile von Screenshots, Traces,
     Berichten, Logs und sonstigen Laufartefakten messen; zusätzliche Docker-/
     Buildbestände getrennt betrachten. Keine Ursache oder Einsparung vorab behaupten.
   - Unnötige Erfolgsaufnahmen, wiederholte Messwertbelege und gegebenenfalls
     redundante Artefaktkopien identifizieren. Vorschläge für bedarfsgerechte
     Aufzeichnung und Aufbewahrung machen; hilfreiche Fehlerdiagnose erhalten.
     Bestehende Runs nicht ohne gesonderten Löschauftrag entfernen.
   - Den JUnit-Bestand nach fachlicher Aussagekraft prüfen: Funktions-, Sicherheits-,
     Daten-, Rechte- und Konsistenzprüfungen erhalten. Reine Größen-/CSS-Konfiguration,
     wiederholte Komponentenstrukturprüfungen und doppelte Absicherung derselben
     Aussage als Vereinfachungskandidaten konkret benennen.
   - Bei Playwright echte Benutzerabläufe und responsive Bedienbarkeit priorisieren.
     Nicht für jeden Button und jede Einsatzstelle eigene Pixel-, Schrift- oder
     Farbprüfungen vorsehen. Gemeinsame Bausteine bei Bedarf repräsentativ prüfen.
   - Für jede Empfehlung erklären, welche relevante Absicherung erhalten bleibt,
     was entfallen kann und welcher tatsächliche Nutzen bezüglich Wartung,
     Speicher und gegebenenfalls Laufzeit zu erwarten ist. Aus Testzahlen allein
     keine Aussage über Nutzen oder Laufzeit ableiten.
8. **Die noch offenen Vaadin-Komponentenkandidaten ausführlich bewerten.**
   Erst nach der großen Testrunde, anschließend an die Teststrategie-Analyse,
   den Komponentenplan mit dem dann aktuellen Code abgleichen. Für jeden
   verbleibenden Kandidaten konkret erklären:

   - Welches tatsächliche Problem der vorhandene Aufbau verursacht und welche
     passende öffentliche Vaadin-Komponente oder API es lösen könnte.
   - Welcher Gewinn durch weniger eigenen Code, einfachere Wartung, Bedienbarkeit,
     Barrierefreiheit oder robustere Funktion zu erwarten ist. Reine optische
     Abweichung oder die Verwendung nativer Komponenten allein rechtfertigen
     keinen weiteren Umbau.
   - Welcher Implementierungs-, Anpassungs-, Test- und Abnahmeaufwand sowie welches
     Regressionsrisiko entstehen. Bereits passende Kompositionen nativer
     Komponenten ausdrücklich als mögliche endgültige Lösung bewerten.
   - Eine begründete Empfehlung geben: notwendig, klar lohnend, optional oder
     ohne ausreichenden Nutzen. Notwendiges von persönlichem Gestaltungswunsch
     und technischen Vorlieben trennen.

   Ziel ist eine konkrete Abschlussentscheidung für den seit Wochen laufenden
   Vaadin-/Gestaltungsumbau: Welche wenigen Arbeiten wären noch gerechtfertigt,
   und welche Kandidaten können bewusst entfallen oder späteren eigenständigen
   Aufgaben vorbehalten bleiben? Keine neue Designrunde, keinen Piloten und keine
   weiteren Komponentenumbauten aus dieser Analyse automatisch beginnen.
   Nutzen und Aufwand nachvollziehbar einschätzen; keine unbelegten Prozentwerte
   für Einsparungen oder Garantien zur Fehlerfreiheit nennen.
9. **CSS-Struktur und Wiederverwendung gründlich analysieren; keine dauerhafte Umstellung.**
   Nach den Punkten 7 und 8 untersuchen, wie gemeinsame Grundgestaltung, ausdrücklich
   benannte Varianten und lokale Anordnung klar getrennt werden können. Ziel ist
   ein belastbarer Vorschlag für eine spätere reine Strukturbereinigung ohne
   sichtbare oder funktionale Änderungen. Ausgangspunkt für deren Vergleich wäre
   der dann aktuelle, abgestimmte Stand nach den Größenpaketen 1 und 2.

   Die Analyse muss insbesondere klären:

   - Wo Grundregeln für Eingaben, Buttons, Schließen-Icons, Badges, Panels und
     andere wiederverwendete Komponenten mehrfach oder widersprüchlich definiert
     sind. Konkrete Fundstellen, Aufrufer und tatsächlich wirksame Regeln benennen.
   - Echte Doppelungen von absichtlichen Varianten und lokalen Layoutregeln
     unterscheiden. Abstände einer ganzen Komponente zu ihrer Umgebung dürfen
     lokal bleiben; ihr Grundaussehen soll aus der gemeinsamen Gestaltung kommen.
   - Ladefolge, Spezifität, Vererbung, Custom Properties, Aura-Regeln und öffentliche
     Vaadin-Parts berücksichtigen. Gleiche Eigenschaftsnamen oder ähnlich aussehende
     Deklarationen allein beweisen keine gefahrlose Zusammenführung.
   - Vorhandene gemeinsame Dateien und Tokens als Grundlage bewerten. Nicht
     vorschnell alle CSS-Regeln in eine riesige Base-Datei verschieben und keine
     neue parallele Gestaltungsschicht neben den bestehenden Standards schaffen.
   - Für jeden vorgeschlagenen Schritt erklären, wohin eine Regel gehört, welche
     lokalen Kopien entfallen könnten und weshalb das bisherige Ergebnis erhalten
     bleibt. Begründete Ausnahmen sichtbar machen; wiederholte Grunddefinitionen
     nicht ohne Untersuchung als technisch unvermeidlich hinnehmen.
   - Ein schrittweises Vorgehen mit klar begrenzten Paketen und einem geeigneten
     ersten Baustein vorschlagen. Keine fachlichen Änderungen, neuen Größen,
     Farbkorrekturen oder Designverbesserungen mit einer späteren Auslagerung mischen.
   - Einen belastbaren Nachweis für unverändertes Aussehen und Verhalten erbringen:
     vergleichbare Vorher-/Nachher-Aufnahmen und bei Bedarf Pixelvergleiche an
     identischen Ansichten, Zuständen, Viewports und Sprachen unter gleichen
     Browser-/Schriftbedingungen. Fokus, Hover, Auswahl, Disabled/Readonly, Fehler,
     Overlays, Scrollen und Umbruch berücksichtigen; gezielte statt flächendeckend
     duplizierter permanenter Pixeltests. Das Vorgehen mit Punkt 7 abstimmen.
   - Dafür ausdrücklich freigegebene isolierte Machbarkeitsversuche durchführen:
     repräsentative echte Doppelungen auswählen, den unveränderten Referenzstand
     sichern und eine begrenzte temporäre Zusammenführung erproben. Vorher und
     nachher in DE/EN auf Desktop und Smartphone unter gleichen Bedingungen
     vergleichen; relevante Zustände, gemessene Geometrie und bei Bedarf
     Pixelunterschiede dokumentieren. Den Stichprobenumfang begründen und
     ungeprüfte Bereiche erkennbar lassen. Die notwendigen gezielten Browserläufe
     sind für diesen Analysepunkt ausdrücklich freigegeben; keine erneute
     Browserfreigabe dafür anfordern.
   - Grenzen ehrlich benennen: Aus einer statischen CSS-Prüfung keine allgemeine
     Garantie für pixelgleiche Darstellung aller möglichen Zustände ableiten.
     Aufwand, nicht verlässlich nachweisbare Bereiche und nötige Entscheidungen
     nennen. Falls kein vertretbar sicherer Weg erkennbar ist, dies begründen,
     statt weitere unkontrollierte Korrekturschleifen zu beginnen.

   Ergebnis ist eine verständliche Bestandsbewertung samt Vorgehen, Nutzen,
   Aufwand, Risiken, tatsächlichen Vergleichsbelegen und überprüfbaren
   Abnahmekriterien. Temporäre Versuchsänderungen dienen nur dem Nachweis in einer
   eigenen isolierten Testumgebung mit synthetischen Daten. Die laufende Anwendung
   und den regulären Anwendungsstand nicht umstellen; keine Versuchsumstellung
   als fertige Lösung übernehmen. Die dauerhafte Umsetzung wird danach separat
   mit dem Benutzer entschieden.

Die große Testrunde folgt auf die Implementierung und Sonar-Bereinigung;
anschließend erfolgen ausschließlich die drei Analysen zu Teststrategie,
verbleibenden Vaadin-Kandidaten und CSS-Wiederverwendung, keine weitere Umsetzung.
Die bisherigen manuellen Abnahmen
gelten weiter. Die Vaadin-Gesamtumsetzung auch nach dieser Testrunde nicht
eigenmächtig als abgeschlossen markieren.
