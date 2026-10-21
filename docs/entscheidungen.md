# Technische Entscheidungen

Diese Entscheidungen beschreiben das geltende Modell. Die vollständige
Implementierung steht im Code; begründete Sonar-Ausnahmen stehen zusätzlich am Befund.

| Entscheidung | Vorteil | Grenze / möglicher späterer Bedarf |
| --- | --- | --- |
| Vaadin Flow und Spring Boot | Gemeinsame Java-Fachlogik, Komponenten und Sicherheit | UI-Lebenszyklus und Transportschutz müssen gemeinsam geprüft werden |
| Lokale H2-Datenbank mit aktuellem festem Schema | Einfach neu aufzusetzen und reproduzierbar zu testen | Anderes Datenbanksystem braucht eine eigene Freigabe und Tests |
| Eine App-Instanz | Klare lokale UI-/Kontosperren und Ereignisse | Mehrinstanzbetrieb erfordert verteilte Koordination und einen Ereigniskanal |
| Dauerhafte Sicherheitsdaten, neu aufgebaute Oberfläche | Gültige Anmeldung bleibt bei Serverneustart; keine gespeicherten Service-/Entitygraphen | Offene Formulare und Uploadentwürfe werden nicht wiederhergestellt |
| Frische Rechteprüfung in Diensten | Sperren, Widerruf und Rollenänderungen gelten auch in geöffneten Ansichten | Schutz darf nicht auf sichtbare Buttons beschränkt werden |
| Versionierung plus Kontosperren | Formularkonflikte und konkurrierende Sicherheitsaktionen sind kontrolliert | Mehrzeilige Invarianten wie letzter Superadmin brauchen zusätzliche gemeinsame Sperren |
| Typisierte gemeinsame Übersetzungen | Anwendung und Browsertests nutzen denselben DE-/EN-Vertrag | Fehlende Schlüssel müssen sichtbar fehlschlagen; keine stillen Sprach-Fallbacks |
| Konservative Mailclaims | Unklarer SMTP-Ausgang wird nicht automatisch doppelt versendet | Keine Exactly-once-Garantie; IN_PROGRESS kann manuelle Entscheidung verlangen |
| Gemeinsame Theme-Tokens | Farben und Bausteine zentral austauschbar | CSS-Ladereihenfolge und lokale Geltungsbereiche bleiben Teil des Vertrags |
| Gezielte Logs mit sicheren Metadaten | Diagnose ohne Credential-/Inhaltsflut | Freie Exceptiontexte nicht ungeprüft ausgeben |

## Sonar-Ausnahmen als fachliche Entscheidung

- Die View-Konstruktoren zeigen verwendete Abhängigkeiten ausdrücklich. Ein
  künstliches Service-Bündel oder Setter nur für einen Zählerschwellwert ist nicht vorgesehen.
- Die gemeinsame Vererbung trennt Zugangsschutz, Benutzerverfolgung und
  Aktivitätslebenszyklus über Vaadins AppLayout-Basis. Der Tiefenschwellwert allein
  rechtfertigt keine Vermischung oder Duplizierung dieser Aufgaben.
- Paketprivate Frameworktypen können einen begrenzten Wildcard an einer Bean-Grenze
  erforderlich machen. Framework-APIs können Date verlangen, obwohl intern Instant gilt.
- `ServiceReference` veröffentlicht eine verwaltete Beanreferenz; die referenzierte
  Fachkomponente verantwortet ihre eigene Nebenläufigkeit.
- Der Mailkoordinator merkt einen Interrupt, stoppt weitere Annahme und wartet
  angenommene Tasks ab. Das Interrupt-Flag wird anschließend garantiert wiederhergestellt.
- Datenbank-/Sperr-/Rollbackverträge werden nicht pauschal wegen eines Proxy-Hinweises
  geändert. Entscheidend sind tatsächlicher Eintritt und gewünschte Commitgrenze.

Diese Begründungen bleiben unabhängig von einzelnen lokalen Prüfberichten verständlich.
Ändert sich der fachliche Vertrag, ist auch die jeweilige Ausnahme neu zu bewerten.
