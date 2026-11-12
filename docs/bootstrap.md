# Startkonten (Bootstrap)

`development-account` hat den Standardwert **`false`**, auch ohne Angabe.
Kontobezogene E-Mails sind damit grundsätzlich zugelassen. Mit **`true`** ist
das Konto ein Entwicklungskonto und versendet keinerlei Kontomails, einschließlich
Sicherheits-, Deaktivierungs- und Löschmails.

Die Beispielkonten in [`.env.example`](../.env.example) setzen `true` ausdrücklich.
Entwicklungskonten dürfen nicht gleichzeitig `email-verification-required=true`
verwenden. Bei aktiviertem Bootstrap bricht diese Kombination den Anwendungsstart
mit einem Konfigurationsfehler ab. Kein Wert wird automatisch überschrieben.
Rollen- und 2FA-Prüfungen gelten weiterhin.

Die Einstellung wird nur bei der ersten Kontoanlage beim Anwendungsstart
übernommen. Änderungen an den Startvorgaben ändern keine bestehenden Konten.

Weitere Optionen und Beispiele stehen in der
[Entwicklungsanleitung](entwicklung.md#e-mail-bestätigung-für-startkonten).
