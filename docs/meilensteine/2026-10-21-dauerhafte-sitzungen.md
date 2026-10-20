# Dauerhafte Anmeldung und neu aufgebaute Vaadin-Oberfläche

Am 21.10.2026 auf main, Ausgangspunkt bbdc6e07b8b4bedf4e4098b6bc814505ca5e3f85,
17 UI-/Session-Finding-IDs bearbeitet. Zusätzlich ausdrücklich beauftragter neuer
Sitzungsvertrag: gültige Anmeldung/Slots/CSRF/Widerruf über Serverneustarts erhalten,
UI und ungespeicherte Entwürfe frisch aufbauen. Spring Session JDBC und feste
versionierte JSON-Mementos, keine serialisierten JPA-/Spring-/Vaadin-Graphen.

Vollständiger Produktions-Clean-Verify mit **2185 Tests**, null Fehler/Skips.
**49 Browser-Helper** grün. Neue und vorhandene Konkurrenz-/Rollback-/Rechte-/
Kontosicherheitstests enthalten; 371 Build-Eingaben mit dem Arbeitsstand per SHA-256
abgeglichen. JaCoCo frisch: **10764/12200 Zeilen (88,23 %)** und **3180/4094 Zweige
(77,67 %)**, 236 ausführbare Anwendungs-Java-Klassen. Spotless/Checkstyle und
Workbox-/Vite-Entwicklungsprüfung erfolgreich, kein Entwicklungsserver gestartet.

Ausdrücklich freigegebene **gezielte** Browserabnahme: **49/49 DE und 49/49 EN**,
null Fehler/Skips. Zwei isolierte Worker je Sprache am identischen Image
`sha256:0ee9176c96bd33ccfdb35152e89e9bc9d6c21acecc91a346ecbcedd6d0277fdd`. Echte Neustarts, Login/Logout/Remember-me, Sitzungslimit/-verwaltung,
Widerruf, alte RPCs, Wartung, Rollen/Sperren und Kontosicherheit geprüft. Eigene
H2-Daten und Mailpit-Mails, fremde Container/Daten unverändert. Cleanup und
Berichterstellung erfolgreich. Kein neuer vollständiger all-Lauf.

| Prüfung | Fälle | Java-Zeilen | Java-Zweige |
| --- | ---: | ---: | ---: |
| Browser gezielt DE | 49/49 | 8826/12200 (72,34 %) | 1976/4094 (48,27 %) |
| Browser gezielt EN | 49/49 | 8825/12200 (72,34 %) | 1976/4094 (48,27 %) |

Browsercoverage misst serverseitiges Java samt Startup/Hintergrundjobs, keine
Frontend- oder vollständige Anforderungsabdeckung. Keine Zusammenrechnung von
Sprachen oder Zwischenläufen. Erforderliche Zwischenkorrekturen sind im Bericht
benannt; nur die abschließenden unveränderten Sprachläufe gelten als Abnahme.

Einführung: neu bauen und Anwendung neu starten; beim ersten Upgrade rein lokale
Sitzungen erneut anmelden. Danach Datenbank und relevante Secrets erhalten.
Keine Bestandsdatenbank-Migration geprüft; kein Cluster/Rolling Upgrade und keine
Wiederaufnahme offener Formulare. Sonar-Nachscan und tatsächlicher Finding-Entfall
stehen aus. Keine Commits, Pushes oder Sonar-Statusänderungen ausgeführt.

[Vollständiger Bericht](../sonar-ui-sessions-2026-10-21.md),
[Betriebs-/Speichervertrag](../durable-session-contract.md).
