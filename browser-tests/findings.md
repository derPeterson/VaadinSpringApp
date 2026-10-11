# Befunde aus den Browsertests

## UI-VER-001 · Verifikation ohne Token zeigt eine leere Ansicht statt der Startseite

- **Status:** BEHOBEN: Weiterleitung über das laufende Navigationsereignis; VER-02A bleibt als unveränderte Regression aktiv.
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

### Ursache und Korrektur

`VerificationView.setParameter(...)` startete bei fehlendem Token eine zweite Navigation
mit `UI.getCurrent().navigate(HomeView.class)` während die erste Navigation noch läuft.
Das ist die anhand des Codes naheliegende Ursache für die inkonsistente Ansicht.
Die Umleitung erfolgt jetzt über das aktuelle Navigationsereignis, wie bereits in
`MainView.beforeEnter(...)` mit `event.forwardTo(HomeView.class)`. Dadurch wird die
Zielnavigation samt Inhalt und Titel abgeschlossen, statt eine verschachtelte Navigation
zu starten. Tokenvalidierung und Sicherheitsregeln wurden nicht geändert.

Der Browserfall bleibt unverändert aktiv: keine Umgehung durch Nachladen von `/home`,
kein Skip und keine schwächere Erwartung. Verifikationssuite und vollständiger Browserlauf
prüfen die sichtbare Startseite und den Titel; die jeweiligen Laufarchive enthalten die
tatsächlichen Ergebnisse und Belege. Ergänzende JUnit-Regressionen prüfen null/leeren
Token ohne Serviceaufruf sowie unveränderte gültige, abgelaufene und unbekannte Tokens.
Adminsperren und Serviceverträge bleiben davon getrennt.

Der ähnliche tokenlose Navigationspfad in `ResetPasswordView` wurde im Rahmen dieses
Pakets noch nicht im Browser geprüft; für ihn liegt hier kein bestätigter Befund vor.
