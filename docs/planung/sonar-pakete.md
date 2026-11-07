# Offene Sonar-Pakete

Ausgangsanalyse `849be68b-9387-42d7-abae-82b79617ccae` auf `839e9ace` mit
173 offenen Findings. Das Quality Gate scheitert an den offenen Findings im neuen
Code. Abdeckung und Duplikate erfüllen die Gate-Grenzen.

| Paket | Umfang | Nachweis |
| --- | --- | --- |
| Ticketzugriff und 2FA-Sicherheit | Transaktionsgrenzen im Ticket-Livezugriff, Dateinamen für Ersatzcode-PDFs und die Einordnung von HMAC für den bestehenden TOTP-Vertrag | JUnit zu Ticketzugriff, PDF-Dateinamen und TOTP, anschließend vollständige Main-Analyse |
| 2FA- und Ticketoberflächen | Verzweigungen, Dialoge und technische Sonar-Hinweise innerhalb der bestehenden Bedien- und Rechteverträge | JUnit zur Navigation und Autorisierung, danach DE/EN in der beauftragten großen Testrunde |
| Testpräzision | Fehlertests mit genau einer zu prüfenden Operation sowie zusammengehörige Browser- und JUnit-Assertions | Betroffene Testklassen, anschließend vollständige Main-Analyse |

Findings gelten nach einem zur Korrektur passenden Nachscan als behoben. Offener
automatisierter Testbedarf steht in [Testbedarf](../testbedarf.md).
