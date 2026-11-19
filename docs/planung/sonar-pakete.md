# Sonar-Bereinigung der Vaadin-Folgearbeiten

Die Ausgangsanalyse `8cc5be6d-fcd9-4b54-8dae-f8dde91dcba9` betrifft den lokalen
main-Stand `16c87047`. Sonar meldet 18 offene Wartbarkeitsbefunde und keine
Sicherheits-Hotspots. Die 18 Befunde verletzen die Gate-Bedingung für neuen Code.

## Vaadin-Komponentencode

Neun Befunde betreffen Empfänger-Popover, Ticketliste, Pagination, Dauerfeld und
Konfigurationsvalidierung. Die Ausrichtung nutzt den deklarierenden Vaadin-Typ.
Zeilenbegrenzung, Darstellung der Listen und Aktualisierung der Seitenbuttons
werden lesbarer. Der Wert für eine ungültige Dauer erhält eine Konstante. Die
Konfiguration validiert alle Felder und meldet weiterhin den ersten Fehler.
Die bestehenden Komponenten-, Ticket- und Konfigurationsprüfungen sichern die
Verträge ab. Das Paket ist bis zum passenden Sonar-Nachscan offen.

## Regressionstests

Neun Befunde betreffen Ticket-Service- und View-Tests sowie die
Empfängerauswahl in Komponenten- und Browserprüfungen. Ausnahmetests bereiten
ihre Argumente vor der geprüften Aktion vor. Verdichtete Tests erhalten klar
abgegrenzte Szenarien oder fachlich benannte Prüfgruppen. Unbenutzte
Testvariablen entfallen. Ausnahmen, Auswahl, Suche, Tastatur und bisherige
Assertions bleiben abgesichert. Die Browserausführung folgt in der großen Testrunde.
