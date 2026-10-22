# Offene Sonar-Pakete nach dem HTTPS-Merge

Status: Paket 1 ist ausdrücklich zur Umsetzung einschließlich lokaler Integration,
begründeter Statusentscheidungen und Nachscan freigegeben. Paket 2 ist nicht
beauftragt. Das HTTPS-Feature bleibt offen.

Grundlage: sauberes `main`, Revision `e5b6e681320885f44b5902b1fa9b037eb4d4c690`.
Analyse `bfb3a47f-be62-4c71-b604-04c39a303df8` vom 22.10.2026, 10:06:37 UTC;
Serverauftrag `4528517b-21da-4410-8dcb-a23fed416dba` erfolgreich abgeschlossen.
Quality Gate **ERROR**: 21 neue offene Issues, zulässig 0. Neue Coverage 82,7 %
und neue Duplikation 0 % bestehen. Gesamtcode und New Code enthalten dieselben
21 offenen Issues; 26 Accepted und 7 False positives bleiben unverändert.
Keine Security Hotspots. Details: [Prüfstand](../status.md).

## Paket 1: IDE-Starter und Prozessaufsicht (14 Issues)

Code auf `codex/sonar-ide-https` umgesetzt; Formatierung und Stilprüfungen bestanden.
21 unterschiedliche gezielte JUnit-Fälle sind erfolgreich nachgewiesen, darunter
die beiden echten Caddy-/Anwendungsstartfälle ohne Windows-Trustimport.
Zwei Accepted- und zwei False-positive-Entscheidungen sind am Server begründet und
nachgelesen. Lokale Integration und zugeordneter Code-Nachscan stehen noch aus.

Priorität: zuerst die temporäre Java-Argumentdatei prüfen und absichern; danach
Fehlergrenzen, Ressourcen und normale Diagnose im selben abgegrenzten IDE-Paket.
Aufwand mittel; Risiko mittel wegen Prozesslebensdauer und Startprotokoll.

| Regel / Datei und Zeile | Finding-ID |
| --- | --- |
| S5443, IdeHttpsProxy:35 | `f3387e40-28a4-4ccf-b582-b7196a0114ca` |
| S2093, IdeHttpsProxy:45 | `c2e554c8-2e5c-4d36-bd41-28042739ebf3` |
| S112, IdeHttpsProxy:34 | `de1732d7-137e-440d-b618-e02395676321` |
| S112, IdeHttpsProxy:203 | `c36484b4-550d-4019-b156-7f1a74dcfdb8` |
| S112, HttpsDevelopmentApplication:60 | `e72af5c8-4366-40b1-a394-892ee28945f3` |
| S1181, HttpsDevelopmentApplication:54 | `ee2204db-13c1-46b8-acbd-70fa43a5cae1` |
| S106, HttpsDevelopmentApplication:53 | `58365d23-4c3b-468e-9dfe-3001240b03af` |
| S106, IdeHttpsProxy:108 | `7e69fe2d-3784-44dc-a375-a917cb927db4` |
| S106, IdeHttpsProxy:174 | `e6e8bfa9-1f72-42fb-8068-fd573de79635` |
| S106, IdeHttpsProxy:184 | `4c471a10-0dc1-41a2-958a-820464bc6cad` |
| S106, IdeHttpsProxy:185 | `5412c8e3-542e-4ab0-8238-9d845d65ebdf` |
| S7467, IdeHttpsProxy:157 | `fd884c8e-dc85-4c64-a227-3e0ff54b76f1` |
| S7467, IdeHttpsProxy:175 | `63287f4d-4fa2-4304-8350-e60724a1f985` |
| S7467, IdeHttpsProxy:214 | `74a1ecb9-9ebf-4ddd-962a-4d39532760b7` |

Fachliche Einordnung und Grenzen:

- S5443 ist von Sonar als kritische Vulnerability eingestuft: `Files.createTempFile`
  legt eine ausführungsrelevante Argumentdatei im Standard-Tempverzeichnis an.
  Zufälliger Dateiname und anschließendes Löschen sind vorhanden; eine ausnutzbare
  Fremdschreibbarkeit ist damit weder belegt noch ausgeschlossen. Expliziten eigenen
  geschützten Ablageort und Zugriffsrechte samt Fehlerabbau nachweisen.
- S2093 verweist auf das kurzlebige `new IdeHttpsProxy(supervisor, -1)` im Fehlerpfad.
  Ressourcenbesitz klar ausdrücken; den auf Erfolg zurückgegebenen Supervisor nicht
  vorzeitig schließen. Die Argumentdatei wird bereits in `finally` entfernt.
- `Throwable` wird für Proxyabbau gefangen und unverändert weitergeworfen.
  Bei engeren Fehlergrenzen den Abbau auch bei Startfehlern erhalten; keine Fehler
  verschlucken. Generische deklarierte Exceptions passend konkretisieren.
- stdout mit `READY <pid>` und anschließendem Flush ist das notwendige
  Maschinenprotokoll. Dieses nicht durch formatierte Loggerausgabe ersetzen.
  Normale Diagnose gemäß Logging-Vertrag behandeln; Protokoll-Findings fachlich
  begründet entscheiden. Keine pauschale Unterdrückung oder Statusänderung.
- Unbenutzte Catch-Variablen nach bestehendem Java-25-Stil bereinigen.

Passende Prüfungen: vorhandene `IdeHttpsProxyTest` wiederverwenden; erfolgreiche
Start-/Stopzyklen, falsche/ausbleibende Bestätigung, harter Besitzerabbruch,
unerwarteter Proxyexit und Pfade mit Leerzeichen erhalten. Regression für geschützte
Argumentdateien samt Abbau bei Fehler ergänzen. Kein erneuter verpflichtender
menschlicher IDE-/HotSwap-Abnahmelauf. Behebung erst durch passenden Nachscan bestätigen.

## Paket 2: Aussagekräftige Start- und Konfigurationstests (7 Issues)

Priorität: zwei von Sonar als Bugs eingestufte Assertions zuerst; keine bestätigten
Anwendungsfehler. Aufwand klein bis mittel, Risiko gering bis mittel bei Prozesssynchronisation.
Kann vorbereitet werden, abschließend gegen den finalen Stand von Paket 1 prüfen.

| Regel / Datei und Zeile | Finding-ID |
| --- | --- |
| S5783, IdeHttpsProxyTest:60 | `ebb9d062-1812-421b-b081-c44d540bd802` |
| S5783, IdeHttpsProxyTest:72 | `612dca03-5b37-4afc-a913-7858c29c8070` |
| S5778, HttpsTransportConfigurationTest:123 | `e899d830-081f-4c1f-8ad1-b42f1ee92292` |
| S7467, IdeHttpsProxyTest:238 | `95919171-07f6-4bf2-87a3-c2aba21aa023` |
| S2925, IdeHttpsProxyTest:243 | `638b48b2-57f6-433d-a86d-1e2f990bc1fb` |
| S2925, IdeHttpsProxyTest:389 | `df3171dc-0e8f-4d20-baa9-2108c0d60fc1` |
| S2925, IdeHttpsProxyTest:403 | `f6f6928c-0186-4819-b58c-cf5e64315cdb` |

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
