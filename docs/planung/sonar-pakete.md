# Offene Sonar-Pakete nach dem HTTPS-Merge

Status: Ausschließlich Paket 2 bleibt offen und ist nicht zur Umsetzung beauftragt.
Paket 1 ist lokal integriert und durch Nachscan bestätigt. Das HTTPS-Feature
bleibt ausdrücklich bis zur gemeinsamen Gesamt-Abnahme offen.

Code-Nachweis: `main`, Revision `bebd699853aa786dbf5a32ee082761aaef793146`.
Analyse `0cf8f070-524a-461d-b7a4-7187461a527a` vom 22.10.2026, 10:35:12 UTC;
Serverauftrag `3ee95a03-a8f6-47e3-9a4e-b161164742f6` erfolgreich abgeschlossen.
Quality Gate **ERROR**: sieben offene neue Issues, zulässig 0. Neue Coverage 82,4 %
und neue Duplikation 0 % bestehen. Gesamtcode und New Code enthalten dieselben
sieben offenen Issues: zwei Bugs in Test-Assertions und fünf Code Smells.
Keine offenen Vulnerabilities oder Security Hotspots. 28 Accepted und 9 False
positives; neue begründete Entscheidungen stehen direkt an den betroffenen Findings.
Ergebnisse und Grenzen: [Prüfstand](../status.md).

## Paket 2: Aussagekräftige Start- und Konfigurationstests (7 Issues)

Priorität: zwei von Sonar als Bugs eingestufte Assertions zuerst; keine bestätigten
Anwendungsfehler. Aufwand klein bis mittel, Risiko gering bis mittel bei Prozesssynchronisation.
Gegen den integrierten und nachgeprüften Stand von Paket 1 umsetzen.

| Regel / Datei und Zeile | Finding-ID |
| --- | --- |
| S5783, IdeHttpsProxyTest:103 | `ebb9d062-1812-421b-b081-c44d540bd802` |
| S5783, IdeHttpsProxyTest:116 | `612dca03-5b37-4afc-a913-7858c29c8070` |
| S5778, HttpsTransportConfigurationTest:123 | `e899d830-081f-4c1f-8ad1-b42f1ee92292` |
| S7467, IdeHttpsProxyTest:283 | `95919171-07f6-4bf2-87a3-c2aba21aa023` |
| S2925, IdeHttpsProxyTest:288 | `638b48b2-57f6-433d-a86d-1e2f990bc1fb` |
| S2925, IdeHttpsProxyTest:440 | `df3171dc-0e8f-4d20-baa9-2108c0d60fc1` |
| S2925, IdeHttpsProxyTest:454 | `f6f6928c-0186-4819-b58c-cf5e64315cdb` |

Fachliche Einordnung:

- In beiden S5783-Lambdas kann bereits die Fixtureerstellung `fakeCommand` eine
  Exception werfen. Vorbereitung vor die Assertion ziehen und den erwarteten
  Startfehler gezielt belegen; bestehende Prozessabbau-Assertions erhalten.
- Die Originprüfung konstruiert in der Assertion zusätzlich ihre Konfiguration.
  Vorbereitung trennen; alle ungültigen Origins weiterhin vollständig prüfen.
- Die kurze Sleep-Schleife wartet begrenzt auf echte Betriebsbereitschaft;
  die beiden langen Sleeps halten absichtlich den künstlichen Kindprozess am Leben.
  Das sind unterschiedliche Aufgaben. Vorhandene JDK-Synchronisation für einen
  gleichwertigen Nachweis nutzen oder den jeweiligen Befund begründet entscheiden;
  keine neue Abhängigkeit und keine abgeschwächten Zeit-/Abbaugrenzen.

Passende Prüfungen: gezielte bestehende JUnit-Klassen und nötige isolierte
Prozessprüfungen, vorgeschriebene Stilprüfungen, anschließend bestätigender Nachscan.
Weitere Tests erst nach konkreter Umsetzung zentral in `docs/testbedarf.md` erhalten,
falls sie nicht im selben Auftrag erfolgreich nachgewiesen werden können.

## Prüfgrenzen

Unveränderter Maven-Analyseumfang: Hauptmodul der Anwendung mit Java-/XML-Quellen;
der separate Browser-Testmodulcode wird durch diesen Hauptprojektscan nicht zusätzlich
analysiert. Der Scanner meldet Warnungen zu unaufgelösten Typen/Imports und erkannten
Preview-Merkmalen bei konfiguriertem Java 25 ohne Preview. Ursachen vor einer
belastbaren abschließenden Bewertung der Analysegrenzen gezielt untersuchen.
Keine neue vollständige Browserrunde und keine Ausführung von Paket 2 beauftragt.
