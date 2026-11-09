---
name: java-lesbarkeit
description: "Java-Klassen in VaadinSpringApp gezielt oder projektweit auf Lesbarkeit prüfen und auf Auftrag überarbeiten. Für ausdrückliche Lesbarkeitsprüfungen und Lesbarkeitsumbauten verwenden. Für tägliche Codearbeit gilt direkt der Codestil des Projekts."
---

# Java-Lesbarkeit

Der [Codestil in der Entwicklungsanleitung](../../../docs/entwicklung.md#codestil)
ist der Maßstab. Lies ihn und kläre den Umfang aus dem Auftrag. Verwende die
dortigen Projektbeispiele als Orientierung beim Lesen des betroffenen Codes.

## Prüfen

Verfolge konkrete Abläufe vom Einstieg bis zum Ergebnis, etwa von einem Klick
zur Fachaktion und ihrer Rückmeldung. Lies Aufrufstellen mit. Halte fest, an
welcher konkreten Zeile Namen, Gliederung oder Kontrollfluss den Leser zum
Nachschlagen zwingen. Beschreibe pro Befund, was schwer zu erkennen ist und wie
eine gezielte Änderung das Lesen erleichtert. Ein reiner Prüfauftrag endet mit
diesen Fundstellen und Vorschlägen.

## Überarbeiten

- Bei projektweitem Auftrag die versionierten Java-Quellen in `app`, `shared`
  und `browser-tests` einschließlich Testcode paketweise sichten. Generierte
  Quellen auslassen. Bei einer Unterbrechung die gesichteten und verbleibenden
  Pakete im Arbeitsstand festhalten.
- Nur Stellen ändern, an denen die Überarbeitung das Lesen erleichtert.
  Aufrufer und bestehende Verträge einschließlich Framework-Einstiegen prüfen.
  Im Diff besonders auf dieselben Fallunterscheidungen, dieselbe Reihenfolge
  und dieselben Seiteneffekte achten. Erfordert ein Vorschlag eine fachliche
  Änderung oder einen grundlegenden Umbau, diesen Punkt separat zur Entscheidung
  nennen und mit den unabhängigen Stellen fortfahren.
- Pro Arbeitspaket den Diff prüfen und die vorgeschriebenen Stilprüfungen aus
  der [Entwicklungsanleitung](../../../docs/entwicklung.md#java-formatieren-und-prüfen)
  ausführen. Bei umgegliederten Abläufen passende vorhandene Tests gezielt nutzen.
  Browserläufe, Sonar-Scans und Git-Abschlüsse folgen ihrem jeweiligen Auftrag.
- Zum Abschluss den gesichteten Umfang, einige aussagekräftige Vorher-nachher-
  Stellen, ausgeführte Prüfungen und offene Entscheidungen nennen.
