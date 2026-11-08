# Große Testrunde vom 08.11.2026

Geprüft wurden die Folgearbeiten auf `codex/2fa-folgearbeiten`, ausgehend vom
letzten vollständig belegten Stand `3afa7366` auf `main`.
Der Anwendungsstand ist `606e6955`, der Browser-Teststand einschließlich
des Nachtests für die Benachrichtigungsvorschau `df4e1938`.

## Ergebnisse

| Prüfung | Ergebnis |
| --- | --- |
| Optimierter Maven-Reactor unter JDK 25 | Am aktuellen Stand bestanden, einschließlich geprüftem Maven-Modell |
| Spotless und Checkstyle | Am aktuellen Stand bestanden |
| Anwendungstests | 2.772 bestanden, zwei vorgesehene HTTPS-Opt-in-Fälle übersprungen |
| Gezielter Java-Nachtest der frühen Sperre | 38 bestanden, einschließlich Passwort- und E-Mail-Aktion |
| Browser-Helfertests und Frontend | 96 und 20 bestanden |
| Coverage- und Sonar-Offlineprüfungen | 18 und 47 bestanden |
| JaCoCo-Zeilenabdeckung | Anwendung 86,28 %, Shared 100 % |
| Browser DE | 255 verschiedene Fälle nachgewiesen: 254 im Gesamtlauf, der Desktop-Vorschautest im gezielten Nachlauf |
| Browser EN | 255 von 255 im Gesamtlauf bestanden |
| Früher Sperrbeginn im korrigierten Image | Schneller Direktlink ohne Portalpasswort auf Desktop und Smartphone in DE und EN bestanden |
| SonarQube | Quality Gate auf `main` grün; keine Analyse des Feature-Branches |

Beide endgültigen Browserläufe und der deutsche Vorschau-Nachlauf nutzten das
Image
`sha256:81c49883af4c0db852d0014b5ffb13702ac5f511c7932df28cf65767324e233a`.
Je vier Worker prüften die Gesamtsuite mit getrennten Anwendungs-, H2- und
Mailpit-Umgebungen. Aufbau und Abbau waren erfolgreich. Die frühere
Sonar-Analyse `d7d9b331-4e0d-402c-9df2-de1b39ac0a28` bezieht sich auf
`main`, nicht auf diese Folgearbeiten.

## Befunde und Nachtests

- Die auf den früheren Ablauf ausgerichteten Tests wurden auf den ausdrücklichen
  Rückweg aus der Hilfe, unvollständige OTP-Eingaben und die nativen Tooltips
  angepasst. Kontonavigation nach Refresh, der Hilfeeinstieg aus Konto- und
  Anbieterdialogen, die 2FA-Konfiguration und Ticketfilter sind in DE und EN
  nachgewiesen.
- Der englische Lauf hat einen zu späten Sperrbeginn reproduziert: Führte eine
  Passwort- oder E-Mail-Änderung zur Einrichtung, konnte ein Direktlink vor dem
  Laden der Ansicht noch ins Konto führen. Die Kontoaktion speichert die Sperre
  jetzt vor der Weiterleitung. Die Reihenfolge ist für beide Aktionen mit JUnit
  geprüft. Der Browsernachweis am korrigierten Image ist auf Desktop und
  Smartphone in DE und EN bestanden.
- Der Schutz der einmaligen Ersatzcode-Ausgabe, der Rücksprung aus alten Tabs
  und der PDF-Abruf nach der kurzen Sicherheitsfrist bestanden in DE und EN
  auch am korrigierten Image.
- Im deutschen Gesamtlauf am korrigierten Image brach der Desktop-Vorschautest
  beim erneuten Öffnen der Admin-Konfiguration wegen einer konkurrierenden
  Weiterleitung ab. Der Smartphone-Fall bestand. Der Nachtest öffnet die
  Konfiguration in einem neuen Tab derselben Admin-Sitzung. Die ältere
  Benachrichtigungsvorschau in der Nutzersitzung bleibt Teil der Prüfung.
  Beide Bildschirmgrößen bestanden im Nachlauf auf demselben Image.

## Offene Abnahme

Die gemeinsame Sichtabnahme für [2FA und Admin-Tickets](planung/zwei-faktor-bestaetigung.md)
bleibt offen. Echte Authenticator-Apps, Anbieter-Sonderfälle, Mobilgeräte,
Bildschirmtastatur, Zoom, Screenreader und PDF-Druck stehen im
[manuellen Katalog](manual-tests/README.md). Weitere automatisierte Szenarien
stehen im [Testbedarf](testbedarf.md).
