# Offene Befunde aus den Browsertests

## UI-VER-001 · Verifikation ohne Token zeigt eine leere Ansicht statt der Startseite

- **Status:** OFFEN, reproduziert im Chromium-Browsertest.
- **Priorität:** Mittel. Ungültige oder unvollständige Linkaufrufe können Benutzer in einer leeren Ansicht zurücklassen.
- **Betroffener Browserfall:** VER-02A.
- **Geprüfter Ausgangsstand:** `ae4454b2` mit den ergänzten Browsertests; Anwendungscode unverändert.

### Reproduktion

1. Die Anwendung anonym öffnen.
2. `/verification` ohne Token aufrufen.
3. URL, sichtbare Ansicht und Seitentitel prüfen.

**Erwartet:** Die tatsächliche Startseite wird angezeigt: Serviceanzeige und Benutzermenü
sichtbar, URL `/home`, Titel `Home`.

**Beobachtet:** Die URL wird zu `/home`, aber sichtbar bleibt nur der Hintergrund der
Verifikationsansicht. Serviceanzeige und Benutzermenü fehlen; der Titel lautet weiterhin
`Verification`. Beim direkten Aufruf von `/home` funktioniert die Startseite.

### Einordnung und vorgeschlagene Nachbesserung

`VerificationView.setParameter(...)` startet bei fehlendem Token eine zweite Navigation
mit `UI.getCurrent().navigate(HomeView.class)` während die erste Navigation noch läuft.
Das ist die anhand des Codes naheliegende Ursache für die inkonsistente Ansicht.
Die Umleitung sollte über das aktuelle Navigationsereignis erfolgen, wie bereits in
`MainView.beforeEnter(...)` mit `event.forwardTo(HomeView.class)`.

Der Browserfall bleibt aktiv und schlägt fehl; der Bericht wird nicht als vollständig
bestanden ausgegeben. Keine Umgehung durch Nachladen von `/home`, keinen Skip und keine
schwächere Erwartung einbauen. Nach einer gezielten Korrektur die Verifikationssuite
erneut prüfen. Tokenvalidierung, Adminsperren und Serviceverträge bleiben davon getrennt.

Der ähnliche tokenlose Navigationspfad in `ResetPasswordView` wurde im Rahmen dieses
Pakets noch nicht im Browser geprüft; für ihn liegt hier kein bestätigter Befund vor.
