# Offene Sonar-Pakete

Die Analyse `e16ed8fa-5002-489c-9ba3-498d41223feb` auf `3d37fa23` meldet
97 offene Findings. Das Quality Gate scheitert an den offenen Findings im neuen
Code. Abdeckung und Duplikate erfüllen die Gate-Grenzen.

| Paket | Umfang | Nachweis |
| --- | --- | --- |
| 2FA-Sicherheit | Einordnung von HMAC für den bestehenden TOTP-Vertrag | TOTP-Tests und bestätigender Sonar-Nachscan |
| 2FA- und Ticketoberflächen | Korrekturen an Schutzfiltern, Dialogen und Hilfetexten umgesetzt | Vollständige Main-Analyse und beauftragte DE-/EN-Testrunde stehen aus |
| Testpräzision | Zusammengehörige Browser- und JUnit-Assertions gegliedert | Vollständige Main-Analyse steht aus |

Findings gelten nach einem zur Korrektur passenden Nachscan als behoben. Offener
automatisierter Testbedarf steht in [Testbedarf](../testbedarf.md).
