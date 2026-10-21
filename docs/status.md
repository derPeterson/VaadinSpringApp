# Aktueller Stand

Stand: 21.10.2026. Ausschließlich lokale Entwicklung mit austauschbaren Testdaten.

## Funktionsstand

- Kontoanlage, Aktivierung, Passwortlogin, Reset und verifizierte E-Mail-Änderung.
- Rollen, letzter aktiver Superadmin, Wartung und Anmeldeversuchsschutz.
- Dauerhafte HTTP-Anmeldung, Sitzungslimit, Geräteverwaltung und Remember-me.
- Profil, Profilbilder, Verfügbarkeit und Live-Aktualisierung.
- Persönliche Benachrichtigungen und Superadmin-Versand mit getrenntem Archiv.
- Deutsches/englisches Cosmos-Theme, Administration, Mailpit und Mailqueue.
- Festes aktuelles H2-Schema mit JPA-Validierung.

Die [Anwendungsbeschreibung](anwendung.md) erklärt die Bedienung; [Entscheidungen](entscheidungen.md)
halten die wesentlichen technischen Grenzen fest.

## Qualitätsstand

SonarQube laut zuletzt bestätigtem Benutzerscan: **0 offene Issues**, **28 begründete
Ausnahmen**. Die Begründungen stehen eigenständig verständlich am Server. Nach dieser
Bereinigung wurde kein neuer Sonar-Scan gestartet; der Zähler ersetzt keine Tests.

Die automatisierte QA der aktuellen Bereinigung ist abgeschlossen. Es gibt keine
ungeklärten Fehler aus den folgenden Prüfungen:

| Prüfung | Aktuelle Runde |
| --- | --- |
| Java 25, Spotless und Checkstyle | grün |
| Vollständige JUnit-Anwendungstests | 2.200 Tests, 0 Fehler, 0 übersprungen |
| Optimierter Build | grün, einschließlich aller Anwendungstests |
| Browser-Helfertests | 49 Tests grün |
| Browser all DE | alle 155 Fälle erfolgreich abgedeckt; Gesamtlauf plus Anmeldenachtest |
| Browser all EN | 155/155 bestanden, 0 Fehler, 0 übersprungen |
| Frontend-JavaScript | 12 Tests grün |
| Workflow-JavaScript | 14 Tests grün |
| Python-Offlineprüfungen | 205 Tests grün |

Der DE-Gesamtlauf hatte 154 bestandene Fälle und eine falsche neue Testannahme:
Logout muss von der öffentlichen Startseite nicht zum Login weiterleiten. Der
korrigierte Test prüft den erneuten Anmeldeschutz des Kontobereichs. Die vollständige
Anmeldegruppe bestand anschließend mit **7/7**; damit ist jeder der 155 deutschen
Fälle erfolgreich geprüft, ohne die unveränderten erfolgreichen Fälle erneut auszuführen.

Beide Sprachen wurden am identischen Produktionsimage geprüft:
`sha256:d554d29604ee270f448a4d714478c5c41c14f025046460042d9f9c8181e5dd81`.
Die eigenen isolierten Testumgebungen sind abgebaut. Bestehende Entwicklungsdaten
und fremde Container wurden nicht verändert. Keine zusätzliche Coverageauswertung.

## Geprüfte Schwerpunkte

- Das aktuelle Schema passt zu allen JPA-Modellen und Rollen-/Benachrichtigungstypen.
  Wiederholte Initialisierung und Neustart erhalten Konten, Versionen und Anmeldesperren.
- Die erlaubte E-Mail-Länge von 255 Zeichen gilt durchgängig für Konto, Remember-me
  und dauerhafte Sitzung. Der dabei gefundene Speichergrenzfehler ist behoben;
  ungültige längere Identitäten bleiben abgewiesen.
- Beschädigte Bilduploads werden abgelehnt. Danach lässt sich im selben Dialog ein
  gültiges Bild speichern und erneut anzeigen.
- Die vollständigen Browserfälle prüfen außerdem Rechte, Wartung, Tokenabläufe,
  Sitzungsneustart/-widerruf, Konfiguration und Benachrichtigungen auf DE und EN.
- Die aktuellen Markdown-Verweise sind geprüft und führen auf vorhandene Dateien.

## Offene Punkte

Die automatisierte Runde ersetzt keine menschliche Abnahme. Echte Geräte,
Screenreader, Textverständlichkeit und Mailclients bleiben gesondert zu prüfen.
Weitere Testlücken und die konkreten Schritte stehen in den
[manuellen Sonderprüfungen](manual-tests/README.md); geplante Arbeiten in [TODO](../TODO.md).

Für die lokale Übernahme neu bauen und mit einer frischen Testdatenbank starten.
Vorhandene Entwicklerdaten wurden während dieser Aufgabe nicht zurückgesetzt.
