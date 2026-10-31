---
name: stitch-entwurf
description: "Eine konkrete Seite von VaadinSpringApp mit Google Stitch gestalten oder Varianten daraus erstellen und mit den bestehenden Frontend-Referenzen zur gemeinsamen Sichtung aufbereiten. Für ausdrücklich gewünschte Stitch-Entwürfe, nicht für reine CSS-Korrekturen oder eine bereits freigegebene Vaadin-Übernahme."
---

# Stitch-Entwurf

## Ausgangspunkt und Umfang

README, AGENTS, docs/theme.md und den aktiven Featureplan lesen. Die tatsächlich
betroffene Vaadin-View, ihre Aufrufer, Texte, Zustände und Berechtigungen sowie die
passende freigegebene HTML-Referenz unter docs/design/frontend-referenzen prüfen.
Bei später ausgelagerten Referenzen den in deren README dokumentierten Ablageort
verwenden. Fehlende Vorlagen benennen, keine vermeintlich aktuellen Screens erfinden.

Den Benutzerauftrag in einen konkreten Gestaltungsauftrag übersetzen: Zielseite,
Probleme, gewünschter Spielraum und Anzahl der Varianten. Bestehende Funktionen
und Pflichtfelder inventarisieren. Keine Kennzahlen, Charts oder Fachaktionen als
vorhandene Funktion erfinden. Platzhalter und zusätzliche Muster ausdrücklich markieren.
Nur wesentliche ungeklärte Entscheidungen erfragen, sonst mit dem vorhandenen Stand arbeiten.

## Zusammenarbeit mit Stitch

Die vorhandene Stitch-CLI für Projektverwaltung, Uploads, Generierung und Downloads
verwenden. Vor Aufrufen bei Bedarf die CLI-Hilfe beziehungsweise Payloadschemas prüfen.
Fehlende Anmeldung oder ein CLI-Fehler erlauben keinen stillen Wechsel zur
Webseitensteuerung. Bei einer tatsächlich fehlenden CLI-Funktion den nötigen
Browserfallback begründen und vorher mit dem Benutzer abstimmen.
Auf Wunsch die CLI-Anmeldung selbst mit `stitch login` starten. Kontoauswahl und
interaktive Freigaben führt der Benutzer durch. Danach den Canvas-Zugriff mit einem
lesenden Projektaufruf prüfen. Eine erfolgreiche Google-Anmeldung allein belegt ihn
nicht. Fehlende Canvas-Zugangsdaten konkret benennen, keine Zugangsdaten auslesen oder
im Chat anfordern. Unabhängige Vorbereitung fortsetzen, bis der Zugriff vorhanden ist.
Keine lokalen Eigenentwürfe als Stitch-Ergebnis ausgeben.

Stitch einen gezielten Screenshot der synthetischen Ausgangsseite und die relevanten
Gestaltungsregeln geben: dunkler Cosmos-Hintergrund, gemeinsame Tokens, Inter,
Service-Logo, vorhandene Panels/Controls/Dialoge und ausdrücklich gesetzte Lucide-Icons.
Konkrete Maße/Farben aus docs/theme.md beziehungsweise den zuständigen CSS-Quellen
übernehmen, keine zweite feste Werteliste im Skill führen. Daten für Beispiele
synthetisch halten. Den gewählten Projekt-/Seitenkontext vor dem Absenden prüfen.
Der ausdrückliche Stitch-Auftrag umfasst die Übermittlung dieser synthetischen
Gestaltungsinformationen an Stitch, keine echten Konto- oder Konfigurationsdaten.

Stitch Spielraum im angefragten Bereich geben. Gewünschte Varianten sollen sich
sichtbar unterscheiden, die vereinbarten Fachverträge und das Referenzsystem aber
bewahren. Bestehende Referenzen nicht im selben Schritt ersetzen.

## Ergebnis zur gemeinsamen Sichtung

Vor der Bewertung die tatsächlich gerenderten Entwurfsbilder herunterladen und ansehen.
Prompt, Titel oder Metadaten allein reichen für einen Favoriten nicht aus. Prüfen, ob
fertige Entwürfe auf der Projektfläche vorhanden sind. Falls die CLI sie nur als
Generierungsergebnis liefert, die unveränderten exportierten HTML-Ergebnisse über
`stitch upload screen` im selben Projekt ablegen und die Links dieser Knoten verwenden.
Die echten Stitch-Ergebnisse mit direkten Projekt-/Entwurfslinks zeigen. Unterschiede,
eigenen Favoriten und Gründe kurz erklären. Bei einer lokalen bedienbaren Vorschau
vorhandene Referenzbausteine und Controller verwenden, klar als Übertragung des
Stitch-Vorschlags kennzeichnen und sichtbare Abweichungen zum Original nennen.
Keinen neuen Ordner mit vielen fast identischen vollständigen Referenzseiten anlegen.
Die Referenzsammlung enthält nur `public.html`, `portal.html`, `admin.html` und
deren lokale Assets. Keine entfernten Generatoren, Piloten oder Viewer voraussetzen.
Neue Varianten zunächst getrennt und temporär aufbereiten; die drei festen
Referenzen erst nach ausdrücklicher Auswahl und Übernahmeauftrag verändern.

Browserprüfung und Starts nach den geltenden Projektfreigaben durchführen. Desktop,
schmale Darstellung und relevante Zustände gezielt prüfen, keine große QA automatisch
starten. Belege sichern und tatsächlich ausgeführte Prüfungen nennen.

Eine Entwurfswahl ist keine automatische Umsetzung oder Designabnahme. Erst auf
Übernahmeauftrag die ausgewählte Variante in den vorgesehenen Referenzstand oder
die echte Vaadin-Anwendung einarbeiten. Globale Themeänderungen gesondert abstimmen.
Offene Entscheidungen im aktiven Plan erhalten, keine Review-Berichtsserien anlegen.
