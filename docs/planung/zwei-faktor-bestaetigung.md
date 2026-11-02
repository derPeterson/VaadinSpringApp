# Zwei-Faktor-Authentifizierung – offene Abnahme

## Auftrag und Stand

Branch: `codex/zwei-faktor-authentifizierung`, Basis:
`9be7038f3d0a3bd986d3733777a22d216650fa3d` auf lokalem `main`.
Die acht vereinbarten Pakete sind technisch umgesetzt. Review und gezielte
automatisierte Nachprüfungen sind abgeschlossen. Die gemeinsame fachliche und
gestalterische Abnahme steht aus.
Der geprüfte technische Stand ist als Zwischencommit gesichert. Die fachliche Abnahme,
der Featureabschluss sowie Merge und Push stehen noch aus.

Das aktuelle Verhalten einschließlich Standardwerten und Grenzen steht in der
[Fachdokumentation](../zwei-faktor-authentifizierung.md). Dort sind TOTP,
Einrichtung, einmalige Ersatzcodes/PDF, Rollen-/Funktionsregeln, Sitzungsfristen,
Entwicklungsoptionen und Wiederherstellungsanfragen beschrieben. Der manuelle
[Betreiber-Notfallweg](../betrieb/2fa-notfall.md) bleibt von Benutzerhilfe getrennt.
Die Google-/Microsoft-Verwaltung ist im selben Paket auf Superadmins beschränkt.

## Noch offen

1. Einrichtung, Bestätigung und Verwaltung im bestehenden Cosmos-Stil gemeinsam
   durchsehen: Mein Konto → Sicherheit → Portalzugang, eingeschränkte Anmeldung,
   Ersatzcodeausgabe, Hilfeanfrage und deren Adminbearbeitung. DE/EN und
   Desktop/Smartphone berücksichtigen. Eine spätere Stitch-Verfeinerung ist
   noch keine Freigabe für eine neue Gestaltung.
2. Die Superadmin-Konfiguration einschließlich Feldhilfen, Zeiteinheiten und
   Rollenregeln sowie den Ausschluss normaler Admins aus beiden geschützten
   Konfigurationsgruppen gemeinsam abnehmen.
3. [HUM-09](../manual-tests/README.md#hum-09) mit echter frei gewählter
   Authenticator-App durchführen. QR/manuelle Einrichtung, TOTP, Ersatzcode,
   PDF-Download/Druck, verständliche Texte und Tastatur-/Mobilbedienung beurteilen.
4. Nach ausdrücklicher Abnahme aktuelle Fachdokumentation und Aufgabenlisten
   abschließend bereinigen und diesen erledigten Plan entfernen. Git-Schritte
   nur auf entsprechenden Auftrag ausführen.

## Gestaltung der verpflichtenden Ersteinrichtung

### QR-Einrichtungsschritt: Variante B mit Ergänzungen

Drei gesichtete Stitch-Entwürfe für „Authenticator-App einrichten“ liegen im
selben Projekt. Panelkopf und Aktionsfooter bleiben bei einer Übernahme Grundlage;
„Abmelden“ verwendet die bestehende rote Aktionsvariante. Der Inhalt darf neu
angeordnet werden. Ausgewählt ist B mit Umschaltung QR/manueller Schlüssel aus C,
den drei App-Empfehlungen und den blauen Tabler-Schrittüberschriften wie in der
Anbietereinrichtung. Die erste Codeeingabe verwendet das freigegebene OTP-Add-on
mit sechs sichtbaren Kästchen und gemeinsamem Einfügen. Der Content verwendet
die Standardfarbe. Vaadin und HTML sind angepasst; die gemeinsame Sicht- und
Bedienprüfung ist auf Wunsch bis zum Ende der Oberflächenanpassungen zurückgestellt.

- [A: Schritte untereinander](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=7593142201349970462)
- [B: QR und Code nebeneinander](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=271433440045913590)
- [C: QR/manueller Schlüssel mit Codefeldern](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=15548966782208944965)

Die Entwürfe enthalten ausschließlich synthetische QR-/Schlüsselbeispiele.
Zusätzliche von Stitch erfundene Seitenstatus und technische Erklärungen sind
keine abgenommenen Anforderungen. Die Windows-App 2fast ist bereits als weitere
frei wählbare App-Empfehlung in Vaadin und der HTML-Referenz ergänzt.

### Bereits ausgewählter Einstieg

Im eigenen Stitch-Projekt
[VaadinSpringApp – Zwei-Faktor-Authentifizierung](https://stitch.withgoogle.com/projects/1332538028618138858)
liegen die drei gesichteten Entwürfe:

- [A: Zentrierte Schlosskarte](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=0767d365b62e4df296209ad37bc924bd)
- [B: Geteiltes Panel mit Schildmotiv](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=c3bcc6e662e34ccba6f0abca4a51b89f)
- [C: Sachliches Panel mit Kosmos-Motiv](https://stitch.withgoogle.com/projects/1332538028618138858?node-id=c0fce938cba44dfb9fee508cb71f340b)

Variante A ist ausgewählt und in Vaadin sowie der Public-Referenz übernommen.
Die gemeinsame Sichtprüfung der Übernahme steht aus. Zusätzliche von Stitch
erfundene Statusangaben und Schrittzahlen werden nicht übernommen.

Vereinbarter Einstieg nach einer frischen Portalpasswort-Anmeldung: Überschrift
„Zwei-Faktor-Authentifizierung erforderlich“, Erklärung „Für Ihr Konto ist eine
zusätzliche Absicherung erforderlich. Verbinden Sie jetzt eine Authenticator-App,
um die Anwendung weiter zu nutzen.“ sowie „Abmelden“ und „Einrichten“.
„Abmelden“ beendet die Anmeldung; Schließen oder Überspringen darf die Pflicht
nicht umgehen. QR-Code, Codeeingabe und Ersatzcodes folgen erst danach.

Die verpflichtende Ersteinrichtung verwendet den echten Passwortnachweis derselben
Sitzung, solange er weniger als fünf Minuten alt ist. Die Prüfung erfolgt auch beim
Start der Einrichtung. Alte Sitzungen, dauerhafte Anmeldung und externe Nachweise
gelten nicht als frischer Passwortnachweis. Ohne passenden Nachweis folgt die
bestehende Identitätsprüfung. Der erste gültige TOTP bleibt Voraussetzung für den
Abschluss. Frühere Faktoren und Betreiber-Wiederherstellungen nutzen diese Ausnahme
nicht.

## Geltende Umfangsgrenzen

- Ein neuer Google-Durchlauf allein bestätigt im normalen Betrieb keine sensible
  Kontoaktion. Ausschließlich bei echter erstmaliger Einrichtung gilt die
  vereinbarte Kombination aus kontogebundenem Google-Durchlauf, Code an die bereits
  bestätigte Kontaktadresse und erstem TOTP. Das akzeptierte Risiko einer gemeinsam
  übernommenen Google-/Mailsitzung ist dokumentiert. Der Weg ersetzt keinen
  vorhandenen, entfernten oder verlorenen Faktor.
- Keine fest eingebaute unveränderliche Adminpflicht. Die strengste tatsächlich
  konfigurierte explizite Rolle gilt. Änderungen werden nach der vorherigen Regel
  bestätigt und wirken auch auf offene bzw. dauerhaft angemeldete Sitzungen.
- Entwicklungs-Ausnahmen sind standardmäßig aus und ohne ausdrückliche
  Startfreigabe unwirksam. Die Startfreigabe allein schwächt keine Prüfung ab.
- Die kleine Ticketgrundlage ist Kontakt und Unterstützung. Weder Adminantwort,
  Anleitung noch Abschluss erzeugt einen Kontonachweis oder Faktorreset.
  Ohne Authenticator/Ersatzcode keine automatische Freischaltung.
- Kein vollständiges Ticketsystem, keine Feedback-Funktion, kein versteckter
  Notfallbenutzer und keine zweite Reparaturoberfläche. Die separate
  Vaadin-Komponentenübernahme gehört nicht zu dieser Umsetzung.
