# Freigegebene Frontend-Referenzen

Drei bedienbare HTML-Momentaufnahmen der gemeinsam abgestimmten Gestaltung:

| Bereich | Referenz | Einstieg |
| --- | --- | --- |
| Besucher, ohne Anmeldung | [Public](public.html) | Startseite, Anmeldung und Registrierung |
| Angemeldete Benutzer | [Portal](portal.html) | Startseite, Benachrichtigungen und Mein Konto |
| Administration | [Admin](admin.html) | Dashboard, Benutzerverwaltung, Konfiguration, Mailtest und Benachrichtigungen |

Die HTML-Dateien direkt im Browser öffnen. Den Ordner `assets` daneben belassen.
Alle Ressourcen liegen lokal. Es werden weder Nachrichten noch E-Mails versendet
oder echte Benutzerdaten gespeichert. Formulare, Dialoge, Filter, Auswahl,
Pagination und Feldhilfen verwenden ausschließlich synthetische Beispiele.

Die Besucheransichten sind über die obere Navigation und die Formularlinks
bedienbar. Weitere Link- und Bestätigungszustände lassen sich in den jeweiligen Formularen
über die synthetischen Statusauswahlen darstellen.
Portal und Admin verwenden ihre Seitennavigation. Admin startet ohne Fragment
mit dem Dashboard. Die Referenzen sind Gestaltungsvorlagen, keine zweite Anwendung.

`assets` enthält die lokalen Schriften samt Lizenz, das Dashboardmotiv sowie die
Styles und Controller der drei Oberflächen. Geteilte Tooltips liegen in
`tooltips.css` und `tooltips.js`. Die übrigen Skripte behalten ihre ursprüngliche
Ausführungsreihenfolge. Es gibt keine Generatoren, separaten Viewer, Piloten,
Varianten-, Icon- oder Hintergrundauswahlen mehr. Das feste Kosmosmotiv bleibt.

Für ausdrücklich beauftragte Designänderungen nur die betroffene Referenz
mitziehen. Texte und Verhalten der echten Anwendung bleiben in Vaadin, den
Diensten und dem typisierten Textkatalog maßgeblich. Bei einem neuen Stitch-Auftrag
gezielt die betreffende Seite beziehungsweise einen synthetischen Screenshot und
den [Theme-Standard](../../theme.md) verwenden.

Das gewählte Service-Logo liegt unter
[icons/service-logo.svg](../../../app/src/main/resources/META-INF/resources/icons/service-logo.svg).
Die übrigen Logos sind dort im Ordner `service-logo-alternatives` erhalten.
