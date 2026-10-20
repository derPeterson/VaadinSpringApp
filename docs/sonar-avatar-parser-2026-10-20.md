# Avatar-Parser: Sonar-Paket vom 20.10.2026

Ausgangspunkt: main, Commit `154e3af`, sauberer Arbeitsbaum. Aktueller Export: 58 eindeutige offene Findings. Freigegebener Umfang: Avatar-Parser lesbarer aufteilen und Blockgrenzen, ungültige Metadaten und EXIF-Verarbeitung absichern. Keine Session-/UI-Serialisierung oder fremden Komponenten überarbeitet.

## Änderungen und Finding-Zuordnung

Eine Produktionsdatei geändert: `AvatarImageProcessor.java`. Eine Testklasse ergänzt: `AvatarImageMetadataTest.java`. Alle fünf vorhandenen AvatarImageProcessor-Findings gezielt bearbeitet; Serverergebnis erst nach dem nächsten Benutzer-Scan. Keine Suppressions, Accepted-Entscheidungen, False-Positive-Entscheidungen oder Scans.

[Alle fünf IDs, ursprüngliche Exportzeilen und Begründungen](sonar-avatar-parser-findings-2026-10-20.csv): zwei S3776 (bisher process/orientation), zwei S127 (PNG-Blockzähler), ein S135 (JPEG-Abbrüche).

Die Verarbeitung ist nach tatsächlichen Aufgaben gegliedert: Format-/Uploadprüfung, Reader-/Rasterprüfung, Metadaten, Crop/Transformation und begrenzte PNG-Ausgabe. Kein neuer Service und keine Änderung öffentlicher APIs. Das bisherige Bildrendering, die EXIF-Transformationsmatrizen und TIFF-Feldauswertung bleiben erhalten.

PNG wird einmal statt zweimal durchlaufen. Der gemeinsame Durchlauf prüft weiterhin alle erreichbaren Blöcke auf APNG und merkt ausschließlich Position/Länge des ersten eXIf-Blocks in einem privaten Wertobjekt. Die TIFF-Auswertung erfolgt wie bisher erst nach dem Rasterdecode bei normalize; validate interpretiert TIFF weiterhin nicht. Auch hinter einem ersten EXIF-Block wird APNG weiter abgelehnt. Mehrere EXIF-Blöcke behalten die bestehende Regel: ausschließlich der erste entscheidet, auch wenn dessen TIFF ungültig ist.

JPEG besitzt eine eigene Markersuche mit benannten Prüfungen für längenlose Marker und APP1-EXIF-Kennung. Füllbytes, Standalone-Marker und Segmente bewegen den Cursor ausdrücklich vorwärts. Ungültige Grenzen und SOS/EOI beenden die Metadatensuche mit Orientierung 1. Keine break-/continue-Kette. TIFF-Offsets bleiben relativ zum begrenzten Metadatenblock, unsigned Längen werden vor int-Konvertierung gegen verbleibende Eingabe geprüft.

## Erhaltene Verträge und Grenzen

- 5 MiB Upload, maximal 8192 pro Bildkante beziehungsweise 20 Millionen Pixel; Dimensionsprüfung weiterhin vor vollständigem Rasterdecode.
- Zwei Decoderplätze; Freigabe im finally auch bei Fehlern. Reader.dispose, Bild.flush, Graphics.dispose und begrenzte Outputstreams erhalten. Das Ergebnisbild wird auch bei einem Fehler beim Erzeugen der Graphics-Ressource im finally freigegeben.
- Ergebnis weiterhin 512 × 512, PNG, höchstens 1 MiB; Cropgrenzen, Transparenz und bestehende Interpolation erhalten.
- Format-/Größen-/APNG-Fehler werden weiterhin mit bestehenden Problem-Werten behandelt; ungültige TIFF-Orientierung fällt auf 1 zurück. Keine Rohmetadaten ins Ergebnis kopiert.
- PNG-Vorprüfung und ImageIO haben unterschiedliche Aufgaben. Der Metadatenscanner ist kein neuer vollständiger PNG-/JPEG-Konformitätsvalidator; Fehlerbehandlung und tolerierte Metadaten folgen dem bisherigen Verhalten. Keine zusätzlichen Dateiformate oder Animationen freigegeben.

Keine Änderungen an Design, Übersetzungen, Transaktionen, Login/Logout, Remember-me, Rollen oder Datenformaten. Keine bestehenden Datenbanken, Container oder fremden Prozesse verändert; Anwendung nicht gestartet oder neu gestartet.

## Regressionen und tatsächlich ausgeführte Prüfungen

**70 neue öffentliche API-Testausprägungen**, mit echten PNG-/JPEG-Fixtures, ohne private Methoden per Reflection zu testen:

- Alle acht EXIF-Ausrichtungen für beide Formate und beide TIFF-Byte-Reihenfolgen (32 Fälle), vier unterschiedliche Farbquadranten als unabhängige sichtbare Erwartung.
- 14 ungültige/unsupported TIFF-Varianten in beiden Formaten (28 Fälle): leerer/kurzer Header, falsche Byte-Reihenfolge/Magic, unsigned oder blocküberschreitende IFD-Offsets, leere/abgeschnittene/große Verzeichnisse, falscher Tag/Typ/Count sowie Orientierung 0/9.
- Jeweils beide Formate mit mehreren EXIF-Blöcken und ungültigem ersten TIFF (4 Fälle).
- APNG hinter EXIF, unbekannte und leere PNG-Chunks, JPEG-Füllbytes/Nicht-EXIF-APP1 sowie drei übergroße PNG-Längen (6 Fälle).

Vor Produktionsänderung bestanden **84/84 Charakterisierungsfälle** (70 neue + 14 bestehende Bildtests), null Failures/Errors/Skips. Nach Überarbeitung bestanden **104/104 gezielte Bild-/Draft-/Avatar-Persistenzfälle**, null Failures/Errors/Skips. Diese sind Teil des vollständigen Laufs und werden nicht zusätzlich als eindeutige Gesamtmenge gezählt.

Nach der letzten Java-Anpassung Spotless apply, Spotless check und Checkstyle vor dem Testlauf ausgeführt: erfolgreich, null Stilabweichungen. JDK 25.0.4.1/Maven Wrapper 3.9.16 geprüft.

Finaler vollständiger Produktions-verify: **2.070/2.070 Anwendungstests**, null Failures, Errors oder Skips. Surefire-XML unabhängig aufsummiert und mit Maven abgeglichen. Produktions-JAR gebaut und repackaged; beendet am 20.10.2026 um 22:51:09 +02:00.

Frische JaCoCo-Daten ausschließlich dieses vollständigen Laufs:

| Umfang | Zeilen | Zweige |
| --- | --- | --- |
| Gesamte Anwendung, 220 analysierte Klassen | 10.250/11.660 = **87,91 %** | 2.996/3.866 = **77,50 %** |
| AvatarImageProcessor | 147/155 = **94,84 %** | 124/158 = **78,48 %** |

Keine vollständige Branchabdeckung behauptet: seltene Decoder-/Ausgabe-/Ressourcenfehler und weitere Binärvarianten bleiben ergänzbare Testlücken. Kein neuer Browser-UI-Lauf und keine Browsercoverage. Keine eigenständige neue Browser-Helfer-/Frontendtestsuite; vorhandene DE-/EN-Komponentenfälle im Maven-Lauf enthalten. Keine realen E-Mails versendet; Tests verwenden isolierte Fixtures.

## Frontend-Nachkontrolle und Einführung

Nach dem Produktionsbuild Paketmanifeste mit dem unmittelbar vorher gesicherten Stand abgeglichen; keine Änderungen an package.json oder package-lock.json im Diff. Deklarierte Abhängigkeiten ohne Installationsskripte abgeglichen: npm meldete bereits aktuellen Bestand. Alle fünf Workbox-Imports aufgelöst und die Entwicklungs-Vite-Konfiguration erfolgreich geladen, ohne einen Entwicklungsserver zu starten. Damit ist der zuvor aufgetretene fehlende Workbox-Import separat nachgeprüft; dies ersetzt keine Browserabnahme.

Reviewbare Änderungen liegen uncommittet auf main. Keine Commits, Pushes, Merges, Sonar-Scans oder Statuswechsel. Einführung über neuen Java-Build beziehungsweise das gebaute Produktions-JAR und Anwendungsneustart; Browser-Refresh allein übernimmt den geänderten Servercode nicht. Es ist keine sichtbare Designänderung beabsichtigt.

Nach Benutzer-Commit einen neuen Scan/Export abgleichen. Die tatsächliche Finding-Reduktion und mögliche Folgehinweise sind erst dort messbar. Andere Komplexitäts-, Binder-/Sessionstruktur- und Serialisierungshinweise bleiben außerhalb dieses Pakets.

Formatgrundlagen: [W3C PNG-Spezifikation](https://www.w3.org/TR/png-3/) und [JPEG ITU-T T.81](https://www.w3.org/Graphics/JPEG/itu-t81.pdf). Die konkreten Grenzen und Fallbacks oben beruhen auf dem bisherigen Anwendungscode und der öffentlichen Charakterisierung.
