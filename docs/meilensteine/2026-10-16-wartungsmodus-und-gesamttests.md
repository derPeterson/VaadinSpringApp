# Wartungsmodus umgesetzt und Gesamtrunde abgeschlossen

Datum: 16.10.2026. Branch `feature/maintenance-mode`, Basis
`05a67e8ca25ac4cacb326096c18b5f89b57a8406`. Die geprüfte Erweiterung liegt
uncommittet vor; der Basiscommit enthält sie noch nicht. Nichts wurde gestaged,
committet, gemergt oder gepusht.

## Ergebnis und Einführung

`maintenance.mode` verwendet die vorhandene Konfiguration, Standard `false`, ohne
neue Schemaerweiterung. Nach Speichern wirkt der Schalter beim nächsten relevanten
Zugriff. Admin und Superadmin behalten Zugang und können ausschalten. Normale Konten,
Gäste und anonyme Besucher erhalten die lokalisierte Cosmos-Wartungscard.

Bestehende Sitzungen werden pausiert, nicht pauschal abgemeldet. Remember-me umgeht
die Wartung nicht; Ablehnung erzeugt keinen neuen Sitzungsplatz. Alte Formulare und
bereits offene Push-Verbindungen führen keine weiteren Fachcallbacks aus. Abgewiesene
Verifikations-/Resetaktionen verbrauchen keinen gültigen Token. Laufzeiten und
Hintergrundjobs laufen weiter. Die öffentliche Registrierung verbindet Konto und
erste Mailvorbereitung transaktional. Globale Styles und bestehende Layout-/Iconstandards
wurden nicht geändert.

Zum ersten Einsatz Shared-Modul und Anwendung bauen und die Anwendung neu starten;
ein Browser-Refresh installiert diese Java-Erweiterung nicht. Spätere Änderungen
des Schalters brauchen keinen Neustart. Für Schema-/Datenarbeiten ist weiterhin ein
normaler Anwendungs-/Worker-Stopp erforderlich. Details:
[Wartungsvertrag](../maintenance-mode-contract.md).

## Tatsächlich ausgeführte Prüfung

- Java 25 bestätigt; unabhängiges Shared-Modul sauber gebaut/installiert.
- Ausgangsbasis: 1171 Anwendungstests, 43 Browserhelfer, 205 Python-Offlinetests
  und vollständiges `all` mit je 109 Fällen DE/EN erfolgreich.
- Endgültiger Stand: **1226 Anwendungstests, 44 Browserhelfer und 205 Python-Offlinetests**
  erfolgreich, ohne Fehler, Fehlschläge oder Skips. Keine Live-Provider-/Modelltests.
- Produktionsbuild einschließlich Anwendungstests und Frontend erfolgreich.
- Vollständiges Browser-`all`: **118/118 DE und 118/118 EN**, je zwei isolierte
  Docker-/H2-/Mailpit-Worker mit identischem Produktionsimage. Gleiche 118 Fallidentitäten,
  keine Doppelungen; alle 34 Fast-Fälle enthalten. Fast wurde nicht separat wiederholt.
- Neun Wartungsfälle einschließlich Rollenmatrix, HTTP 503, erhaltenen Sitzungen,
  alten Registrierungs-/Resetformularen, echten eigenen Testmails und tatsächlichem
  Chromium-Neustart bestanden. HTTP-/Push-/Persistenzregeln zusätzlich in JUnit geprüft.
- Aufbau, Abbau, gemeinsame HTML-Berichte und Coverage in beiden finalen Läufen erfolgreich.
- Google Java Format / Spotless und Ruff-Formatprüfungen erfolgreich. Eigene Änderungen
  formatiert; keine pauschale Umformatierung fremder Änderungen.

## Coverage des endgültigen Stands

| Messung | Zeilen | Zweige |
| --- | ---: | ---: |
| Anwendungs-JUnit | 4624/6425 (71,97 %) | 1340/2023 (66,24 %) |
| Browser all DE | 5413/6425 (84,25 %) | 1325/2023 (65,50 %) |
| Browser all EN | 5415/6425 (84,28 %) | 1328/2023 (65,65 %) |

JUnit verwendet eine frische Messdatei. Browserdaten wurden je Sprache aus beiden
Workern gegen die Klassen des tatsächlich geprüften JARs zusammengeführt, mit
119 analysierten Anwendungs-Java-Klassen. Shared-JAR und Drittbibliotheken sind
nicht Teil dieses Reportumfangs. Browsermessungen enthalten auch Start und
Hintergrundjobs. Dies ist keine CSS-/JavaScript-Coverage; Fallzahlen ergeben keine
Prozentabdeckung sämtlicher Browserfunktionen. JUnit- und Browserprozente wurden
nicht addiert oder als gemeinsame Abdeckung ausgegeben.

## Wichtige Erkenntnisse

1. Vaadins Request-Interceptor läuft auch für normales HTTP. Die zusätzliche
   Socketprüfung muss auf den bestehenden WebSocket-Einstieg begrenzt sein.
2. Eine neu vom Server angelegte UI benötigt ihre erste UIDL zur Routenauflösung.
   Die Ausnahme gilt nur für echte Sitzungs-UIs; alte Fachansichten und gefälschte
   IDs bleiben geschützt. Routen- und Serviceprüfung bilden weitere Grenzen.
3. Ein HTTP-503-Forward von verschachtelten Token-URLs verändert den serverseitigen
   Pfad. Ein lokaler Index-HTML-Listener hält Bootstrap-Ressourcen an der App-Wurzel.
4. Gefälschte Vaadin-Parameter dürfen fremde Servlets wie eine freigeschaltete
   H2-Konsole nicht von Wartung ausnehmen. Transportpfade werden explizit begrenzt.
5. Rollenentscheidungen vor Push-UI-Auflösung müssen an die angefragte Sitzung bzw.
   deren Principal gebunden sein. Ein fremder Threadkontext ist keine Adminfreigabe.
6. Öffentliche Registrierung braucht eine gemeinsame Transaktionsgrenze für Konto
   und erste Mailvorbereitung, damit späte Ablehnungen keine halbfertige Anlage hinterlassen.

Diese Integrationsbefunde wurden korrigiert und passend nachgeprüft. Zwischenläufe
mit Fehlern oder kontrolliert abgebrochene überholte Stände sind keine bestandenen
Abschlussläufe. Assertions und Fehlerprüfungen wurden nicht zur Umgehung abgeschwächt.

## Grenzen und Aufbewahrung

Reguläre Anwendung, echte Datenbanken, Zugangsdaten, Postfächer und fremde Docker-
umgebungen blieben unangetastet. Nur eigene isolierte Teststacks wurden verwendet
und laut Laufprotokollen erfolgreich abgebaut. Die zusätzliche Live-Inventur am
Vormittag war wegen nicht erreichbarer Docker-Linux-API nicht möglich. Keine
Bestandsmigration oder echte Endgeräte-/Screenreader-/PWA-Abnahme.
Service Worker sind im Browserrunner wie bisher blockiert. Der Wartungsschalter
ist kein Datenbank-Offlinebetrieb und verlängert keine Tokens.

Die alten Run-Ordner wurden nicht gelöscht oder archiviert. Dieser Meilenstein ist
ohne lokale Screenshots, Traces, Run-Verzeichnisse und Rohberichte lesbar. Der
umgesetzte Wartungsplan wurde durch den verbindlichen Vertrag ersetzt und entfernt.
Als weiteres Arbeitspaket war der Mail-Workerpool vorgesehen; danach das geplante Kontopanel.
Aktueller Gesamtstand: [Abdeckungsübersicht](../../browser-tests/abdeckung.md).
