# Prüfumfang und Grenzen

Die [Browseranleitung](README.md) enthält die aktuelle Suite-/Fallauswahl.
[docs/status.md](../docs/status.md) ist die einzige Zusammenfassung der tatsächlich
abgeschlossenen Prüfungen. Einzelne Schritte und Erwartungen stehen direkt bei
jedem Test und werden in den Laufbericht übernommen.

| Ebene | Zuständigkeit |
| --- | --- |
| Anwendungs-JUnit und H2 | Regeln, Null-/Grenzwerte, Transaktionen, Rollback, Parallelität, Schema und Serialisierung |
| Browser DE und EN | Sichtbare Journeys: Registrierung/Mails, Konto/Avatar, Login/Remember-me, Neustart/Sitzungen, Rollen/Wartung, Administration, Benachrichtigungen und Theme |
| Helfertests | Suiteauswahl, Worker, Texte, Reporter und Diagnose ohne App-Container |
| Frontend-Offlinetests | Bildeditor und kompakte Datums-/Zeiteingaben |
| Manuelle Sonderprüfungen | Echte Geräte, Bediengefühl, Hilfsmittel, Mailclients und individuelle Designbewertung |

Browser prüfen Chromium und definierte Fenstergrößen. Das belegt keine vollständige
Mehrbrowser-, Mobilgeräte-, Screenreader- oder externe SMTP-Abnahme. Mailpit belegt
die tatsächliche Annahme im eigenen Testnetz. Headless-Screenshots ersetzen kein
menschliches Designurteil. Technische Sitzungs-/Transaktionsrennen werden gezielt
unter JUnit kontrolliert, nicht durch zufälliges Browser-Timing erzeugt.

Die [manuellen Sonderprüfungen](../docs/manual-tests/README.md) ergänzen nur diese
Lücken. Neue Automatisierung soll eine konkrete Lücke schließen und keine schon
vollständig geprüfte Journey duplizieren. Offene automatisierte Testaufgaben stehen
zentral im [Testbedarf](../docs/testbedarf.md); geplante Erweiterungen in [TODO](../TODO.md).
