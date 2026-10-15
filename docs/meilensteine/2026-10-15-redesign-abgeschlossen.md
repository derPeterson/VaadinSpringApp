# Redesign abgeschlossen – 15.10.2026

Das Redesign wurde als Standard für die weitere Entwicklung bestätigt.
Die anschließende vollständige QA ist erfolgreich abgeschlossen. Dieser Bericht
bewahrt die Ergebnisse unabhängig von bereinigbaren lokalen Testausgaben.
Geprüfter Ausgangscommit: `c5c3d089b0bc60db6f52f287cf284157445dda25` auf `main`;
die ergänzenden Test- und Dokumentationsänderungen lagen bei der Prüfung uncommittet vor.

## Ergebnis und Umfang

Kontoansichten, Home, Administration, Tabellen, Filter und Benutzerdialoge verwenden
die abgestimmte gemeinsame Gestaltung. Verbindliche Werte und Abläufe stehen im
[Designstandard](../design-standard.md). Weitere Designänderungen erfolgen schrittweise
im jeweils ausdrücklich angefragten Umfang.

| Ausgeführte Prüfung | Ergebnis |
| --- | --- |
| Shared-Modul, sauberer Installationslauf | Erfolgreich |
| Anwendungs-JUnit | 1082 Tests; 0 Fehler, Fehlschläge oder Skips |
| Browser-Helper und Reporter | 34 Tests; 0 Fehler, Fehlschläge oder Skips |
| Frischer Docker-Produktionsbuild einschließlich Tests und Frontend | Erfolgreich |
| Vollständige Browserprüfung Deutsch | 100/100 bestanden |
| Vollständige Browserprüfung Englisch | 100/100 bestanden, identisches App-Image |
| Aufbau, Abbau, Coverage und Berichtserzeugung beider Browserumgebungen | Erfolgreich |

Umgebung: Java 25.0.4.1, Maven 3.9.16, Chromium 153.0.8010.12.
Die Browserprüfungen verwendeten eigene Docker-Projekte, isolierte H2-/Mailpit-Daten,
erfundene Konten und regulär über SMTP empfangene Testmails. Zustände entstanden
über die Oberfläche. Die IDE-Anwendung und Bestandsdaten waren keine Testziele.
Alle eigens erstellten Testcontainer, Netzwerke und Volumes wurden entfernt.

## Gemessene Code-Coverage

| Messung | Anweisungen | Zeilen | Zweige |
| --- | ---: | ---: | ---: |
| JUnit | 71,09 % (18795/26439) | 70,87 % (3689/5205) | 66,22 % (1127/1702) |
| Browser Deutsch + Englisch vereinigt | 85,70 % (22657/26439) | 86,19 % (4486/5205) | 65,63 % (1117/1702) |

JUnit wurde mit einer frischen Ausführungsdatei gemessen. Für die Browsermessung
wurden die Daten beider vollständigen Sprachläufe vereinigt und ausschließlich
Klassen aus dem tatsächlich getesteten identischen Produktions-JAR analysiert.
Lokale Windows-Klassen und Containerklassen wurden nicht vermischt.

JaCoCo misst hier serverseitigen Java-Anwendungscode. Die Browsermessung enthält
auch Start und Hintergrundaufgaben. Dies ist weder CSS-/JavaScript-Coverage noch
eine vollständige fachliche Abdeckung. 100/100 ist die Erfolgsquote der vorhandenen
Browserfälle, keine Behauptung einer Code-Coverage von 100 %.

## Ergänzte Regressionen

15 Anwendungs-JUnit-Fälle, vier Browserfälle und sechs Helper-/Reporterfälle wurden ergänzt.
Die neuen Browserfälle wurden vollständig in beiden Sprachen ausgeführt:

| Fall | Gesichertes Verhalten |
| --- | --- |
| USER-11A | Filterentwürfe wirken erst nach Anwenden; Panel erhält Entwürfe; Badge-X entfernt nur das gewählte angewandte Kriterium |
| USER-11B | Uhrzeit nur bei gültigem Datum; Viertelstunden-Platzhalter und Scrollen ohne Wertübernahme; Overlay maximal 300px; Datum löschen deaktiviert Uhrzeit |
| USER-12A | Passwortbestätigung optional und abhängig vom ersten Feld; exakter Vergleich; Fehler erst nach Verlassen des Felds; Leeren setzt Bestätigung zurück |
| USER-12B | Beide Dialoge sicher schließen; Auswahl und zugrunde liegendes Bearbeitungsformular bleiben erhalten; Löschinhalt korrekt |

Bestehende Prüfungen zu Anmeldung, Remember-me mit echtem Browserneustart,
Rollen/Sperren, veralteten Formularen, Mailtokens, Konfiguration, Unicode-Mails,
Sprachwechsel und mobilen Fenstergrößen blieben erfolgreich.

## Wichtige Erkenntnisse

Die Abschlussrunde erforderte keine Änderung am Anwendungs- oder Designcode.
Veraltete Tests erwarteten noch sofort wirkende Filter, feste Platzhalter und
einfache Checkboxtexte. Diese Erwartungen wurden an das bestätigte Verhalten angepasst.

Bei Vaadin zählen unsichtbare Restknoten nicht als sichtbare aktive Badges.
Vor erneutem Öffnen des Filterpanels muss dessen tatsächliches Schließen abgewartet
werden. Die virtualisierte Zeitliste wird mit echter Mausbewegung über dem sichtbaren
Overlay gescrollt; unsichtbare Pufferzeilen sind dafür ungeeignet. Zeitwerte werden
aus dem sichtbaren lokalisierten Text gelesen.

Aus der vorausgehenden Adminprüfung bleiben zwei wichtige Fehlerkorrekturen erhalten:
Enter an der Konfigurationsnavigation darf keine ungespeicherten Werte speichern;
Enter im Mailtext erzeugt einen Zeilenumbruch statt vorzeitig zu senden.
Die Regressionen CONFIG-05A–F und MAIL-02/03 sichern diese Bedienung mit ab.
Beim SMTP-Vergleich werden reguläre CRLF-/LF-Unterschiede berücksichtigt;
Unicode, Leerzeichen und zusätzliche Zeilen bleiben inhaltlich prüfpflichtig.

## Verbleibende Prüfgrenzen

Echte Mobilgeräte und Bildschirmtastaturen, vollständige Accessibility-/Zoomprüfung,
Darstellung und Zustellung in externen Mailprogrammen sowie kontrollierte
Betriebsfälle wie SMTP-Ausfall/Wiederanlauf, Recovery und Bestandsmigrationen sind
hierdurch nicht vollständig nachgewiesen. Regeln zu Transaktionen, Nebenläufigkeit,
exakten Zeitgrenzen und internen Queuezuständen bleiben Aufgabe technischer Tests.
Details stehen in der [aktuellen Abdeckungsübersicht](../../browser-tests/abdeckung.md).

Erneute Ausführung und optionale Coverage beschreibt die
[Browser-Testanleitung](../../browser-tests/README.md). Ein späterer neuer Lauf
ersetzt die damaligen Messergebnisse dieses Meilensteins nicht rückwirkend.
