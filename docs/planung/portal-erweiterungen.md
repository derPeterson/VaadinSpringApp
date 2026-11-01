# Mögliche Erweiterungen des Portals

Stand: 31.10.2026. Entscheidungsvorlage für weitere Funktionen.
Das Designpaket ist abgeschlossen. Die große Testrunde auf `main` bleibt separat
offen und startet erst mit ausdrücklichem Auftrag.

## Gestartetes Funktionspaket

**Anmeldung mit Google und Microsoft; Geschlecht und Geburtsdatum freiwillig.**
Der Featurestart ist beauftragt. Umfang, Entscheidungen, offene Arbeitspakete und
Abnahme stehen ausschließlich im [aktiven Umsetzungsplan](google-microsoft-anmeldung.md).
Microsoft umfasst private sowie Firmen-/Schulkonten. Die Umsetzung ist freigegeben. Zwei externe Anbieter reichen zunächst.

## Weitere Möglichkeiten zur Auswahl

Die Bewertung bezieht sich auf dieses Portal ohne festgelegten fachlichen Inhalt.
„Etabliert“ bedeutet eine übliche Option in passenden Anwendungen, keine allgemeine
Pflicht. Der Nutzen hängt später von Zielgruppe und Inhalt ab.

Umbau: **klein** = wenige begrenzte Stellen; **mittel** = mehrere Komponenten,
Dienste und Tests; **groß** = neue durchgängige Abläufe oder Eingriffe in Anmeldung
und Datenmodell. Das sind relative Einschätzungen, keine Stunden- oder Terminzusagen.

| Möglichkeit | Mehrwert jetzt / später | Umbau und betroffene Bereiche | Wichtigkeit und Einordnung |
| --- | --- | --- | --- |
| **Zwei-Faktor-Anmeldung** | Hoch für Kontoschutz, besonders bei Administratoren. Vom Benutzer als interessant benannt. | **Groß:** Einrichtung per Authenticator-App, zweite Anmeldestufe, sicherer Wechsel/Entfernen und Wiederherstellung mit Ersatzcodes; Zusammenspiel mit externen Logins. | **Hohe Sicherheitspriorität**, besonders vor einem späteren Livebetrieb. Etablierte Sicherheitsfunktion; mehr Aufwand als nur ein zusätzlicher Code beim Login. Als nächste separate Umsetzung zur Ablösung der Google-Zwischenlösung vereinbart, siehe [2FA-Bestätigung](zwei-faktor-bestaetigung.md). |
| **Benachrichtigungseinstellungen** | Jetzt mittel, später hoch bei mehreren Arten freiwilliger Hinweise. | **Mittel:** gespeicherte Präferenzen und deren Berücksichtigung beim Erzeugen/Zustellen; Oberfläche und Texte. | Sinnvoll, sobald Benutzer tatsächlich eine Auswahl benötigen. Freiwillige Hinweise und notwendige Sicherheitsmeldungen getrennt behandeln. |
| **Eigene Kontoaktivitäten** | Mittel: verständliche Chronik zu Anmeldung, Sicherheitsänderungen und verbundenen Anbietern. | **Mittel:** begrenzte Ereignisdaten, Aufbewahrung, eigene Ansicht und Zugriffsschutz. | Etablierte Vertrauensfunktion. Ergänzt vorhandene Geräteansicht und Systemhinweise; nicht lediglich dieselben Informationen erneut anzeigen. |
| **Konto selbst löschen / eigene Daten herunterladen** | Mittel, mit echten Inhalten hoch: Benutzer können ihr Konto selbst verwalten und verlassen. | **Mittel bis groß:** sichere Bestätigung, vollständiger Export beziehungsweise Löschung abhängiger Daten, Sitzungswiderruf und Sonderfall letzter Superadmin. | Sinnvolle Kontofunktion. Vor einem Livebetrieb konkret bewerten; Umfang hängt von den später gespeicherten Daten ab. |
| **Warnung vor ungespeicherten Änderungen** | Mittel bis hoch: versehentliches Verlassen verwirft Eingaben nicht unbemerkt. | **Mittel:** konsistente Erkennung von Änderungen und Behandlung von Navigation, Abbruch und erfolgreichem Speichern. | Praktischer Bedienstandard für bearbeitbare Formulare; ein überschaubareres Paket als die externe Anmeldung. |
| **Entwürfe wiederherstellen** | Jetzt eher gering; später hoch bei längeren Eingaben. Für Admin-Mitteilungen bereits denkbar. | **Mittel bis groß:** Ablage, Wiederherstellung, Bereinigung und Umgang mit mehreren Tabs; keine Passwort-/Tokenentwürfe speichern. | Komfortfunktion, sobald längere Texte wichtig werden. Derzeit nachrangig gegenüber einer Warnung vor Datenverlust. |
| **Feedback / Fehler melden** | Mittel bis hoch während der Entwicklung: Benutzer können Probleme verständlich melden. | **Mittel:** Formular, Zustellung oder Ablage, Bearbeitung und Missbrauchsbegrenzung. Ein vollständiges Ticketsystem wäre deutlich größer. | Nützlich auch ohne weitere Portalinhalte, aber kein notwendiger Portalstandard. Klein beginnen. |
| **Persönliche Anzeigeeinstellungen** | Jetzt eher gering bis mittel; abhängig von konkreten Bedürfnissen. | **Klein bis mittel pro Einstellung:** etwa Zeitzone oder reduzierte Animationen, Speicherung und konsistente Anwendung. Ein zusätzliches helles Theme wäre ein eigener größerer Designumfang. | Gezielt sinnvoll, pauschal viele Schalter eher Spielerei. DE/EN-Sprachwahl ist bereits vorhanden. |
| **Hilfe und Neuerungen** | Jetzt mittel: bestehende Kontofunktionen erklären und Änderungen sichtbar machen. | **Klein** bei überschaubaren statischen Inhalten; **mittel** mit redaktioneller Verwaltung. Inhalt muss gepflegt werden. | Sinnvoll, wenn konkrete Fragen auftreten. Keine umfangreiche Hilfeplattform ohne entsprechenden Inhalt bauen. |
| **Passkeys** | Technisch ein möglicher Komfort- und Sicherheitsgewinn; derzeit vom Benutzer nicht gewünscht. Geräte-PIN ist ebenfalls möglich, nicht nur Fingerabdruck. | **Groß:** Registrierung und Verwaltung mehrerer Zugangsmittel, Wiederherstellung und Integration in sensible Aktionen. | Etablierte passwortlose Alternative, keine Spielerei. **Aktuell zurückgestellt; nicht Teil der gewünschten Umsetzung.** |

## Empfehlung für die weitere Auswahl

Zuerst das gestartete Google-/Microsoft-Paket mit freiwilligen Profilangaben gemeinsam
abnehmen. Die [2FA-Bestätigung](zwei-faktor-bestaetigung.md) ist als nächste separate Umsetzung vereinbart. Für ein
kleineres Komfortpaket bietet sich der Schutz vor ungespeicherten Änderungen an.
Benachrichtigungspräferenzen und Entwürfe gewinnen mit tatsächlichen Portalinhalten
an Wert. Die übrigen Ideen bleiben Auswahlmöglichkeiten, keine zugesagte Roadmap.

## Technische Grundlagen

- [Spring Security: OAuth2-/OpenID-Connect-Anmeldung](https://docs.spring.io/spring-security/reference/7.0/servlet/oauth2/login/index.html)
- [Google: dauerhafte Kontoidentität statt E-Mail als Identitätsschlüssel](https://developers.google.com/identity/openid-connect/openid-connect)
- [Microsoft: private sowie Firmen-/Schulkonten](https://learn.microsoft.com/en-us/entra/identity-platform/v2-protocols-oidc)
- [OWASP: Mehrfaktor-Anmeldung und Wiederherstellung](https://cheatsheetseries.owasp.org/cheatsheets/Multifactor_Authentication_Cheat_Sheet.html)
- [FIDO Alliance: Passkeys](https://fidoalliance.org/passkeys/)
