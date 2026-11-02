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
