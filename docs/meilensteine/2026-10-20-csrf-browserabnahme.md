# CSRF-Schutz und gezielte Browserabnahme

20.10.2026, Branch `fix/sonar-csrf`, Ausgangscommit
`6e8aa610dda9d452b6f97959645fef964f9d4479`, Änderungen uncommittet.

Spring-CSRF schützt klassische HTTP-Mutationen einschließlich Passwortlogin und
Logout. Echte Flow-Transporte bleiben durch Flow geschützt. Ein gefälschter
Frameworkparameter auf klassischen Endpunkten erzeugt keine Ausnahme. Tokenbezug
und Rotation sind für HTTP-Login, manuellen Flow-Login und Remember-me abgesichert;
Admission, Wartung, Sessionlimits, Rechte-Refresh und Rollback bleiben erhalten.

## Tatsächliche Prüfungen

| Prüfung | Ergebnis |
| --- | --- |
| Vollständige Anwendungs-JUnits und Produktionsbuild | 1912 Tests erfolgreich, keine Fehler/Skips |
| Neue Testausprägungen im Paket | 51 |
| Browser-Helfer | 48 erfolgreich, keine Fehler/Skips |
| Browser DE: Anmeldung und Sitzungen / Wartung | 35/35 und 9/9 erfolgreich |
| Browser EN: Anmeldung und Sitzungen / Wartung | 35/35 und 9/9 erfolgreich |
| Spotless apply/check, Checkstyle, Whitespace | erfolgreich |

Die 44 Fälle je Sprache umfassen Flow-Login, Remember-me nach echtem Browser- und
Anwendungsneustart, Logout, zusätzliche Tabs, eigenes Sitzungsmanagement,
Rechteentzug, Wartung und Wiederaufnahme. Es wurden keine 152er-Gesamtrunden
wiederholt. Der Browserauftrag war ausdrücklich freigegeben.

Alle erfolgreichen Runden prüften dasselbe unveränderliche Produktionsimage:
`sha256:b343230aad715efb2f32cbf8e2dbb8243f6d797cafc10924bcef426ed18ed61a`.
Die JAR-Prüfsumme war in allen vier Runden identisch:
`ca531a1867805710d607e91181f93168bfce070ae5c361ff46c49404172a71f1`.
Nur isolierte Testdaten/Container wurden verwendet und danach abgebaut;
bestehende Anwendung und fremde Datenbestände blieben erhalten.

## Erkenntnisse und Coverage

Ein Dockerbuild deckte ein instabiles Wartungsansichts-Testfixture auf:
CurrentInstance hält seine Sitzung nur schwach. Eine starke Referenz im Test
verhindert den Locale-Rückfall; fünf gezielte Fälle mit englischer System-Locale
und der anschließende Dockerbuild waren erfolgreich. Keine Produktivänderung dafür.
Ein Browserlauf wurde während des Dockerupdates mit 35 Verbindungsfehlern
unterbrochen. Dieser Lauf zählt nicht als fachlicher Nachweis; nach dem Update
wurden die betroffenen Fälle vollständig erfolgreich wiederholt.

Frische JaCoCo-JUnitmessung: **10055/11490 Zeilen (87,5 %)**,
**2843/3730 Zweige (76,2 %)**. Zusammengeführte Browsermessung ausschließlich der
vier erfolgreichen Runden: **7860/11490 Zeilen (68,41 %)**,
**1735/3730 Zweige (46,51 %)**, jeweils 216 Anwendungsklassen.
Die Quoten betreffen verschiedene Fallauswahlen; sie belegen keine vollständige
Sicherheits- oder Transportabdeckung und sind keine neue Gesamt-QA aller Ansichten.

## Offene Punkte

- Beim Abschluss dieser Browserrunden war die H2-Kompatibilität noch offen.
  Der anschließende [H2-Nachtrag](../h2-console-csrf-2026-10-20.md) schließt die
  lokale Bedienintegration über Ursprungsschutz und echte HTTP-Regressionsfälle.
  Aktuell 1956 Anwendungs-JUnits und 48 Helfer grün. Die 44er-Browserrunden und
  ihre Image-/JAR-Kennungen oben gehören zum vorherigen Stand.
- Manipulierte echte Multipart-Uploads und WebSocket-Upgrades bleiben außerhalb
  dieser gezielten Browserrunde. Die technischen RPC-/Ressourcenschutztests sind
  im [Implementierungsbericht](../sonar-csrf-umsetzung-2026-10-20.md) genau abgegrenzt.
- Keine Commits/Pushes, kein neuer Sonar-Scan und keine Sonar-Statusänderungen.
  Übernahme des Servercodes erfordert Neubau und Neustart der vorgesehenen Anwendung.
