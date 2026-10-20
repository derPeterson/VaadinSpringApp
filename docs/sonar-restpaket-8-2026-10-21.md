# Sonar-Restpaket: acht Hinweise und vier bestehende Ausnahmen – 21.10.2026

## Ergebnis

Ausgangspunkt: `main`, Commit `6e1df854155352a990a3bb4e2fad519694bf1123`, sauberer Arbeitsstand.
Benutzerfreigabe: die acht verbleibenden Konstruktor-/Vererbungsfälle bearbeiten und
begründete Ausnahmen direkt in SonarQube einordnen, einschließlich der vier bereits
im [Nachscan-Paket](sonar-nachscan-28-2026-10-21.md) geprüften Fälle.

- **Ein Finding im Code korrigiert:** AdminUserFilterBar, S107.
- **Sieben der acht Hinweise akzeptiert:** vier View-Konstruktoren und drei Vererbungstiefen.
- **Die vier vorherigen Ausnahmen eingeordnet:** zwei Accepted, zwei False Positive.
- Damit **neun Accepted und zwei False Positive** am Server mit je einer konkreten
  Begründung gespeichert; der jeweilige Status und Activity-Eintrag wurden in der
  angemeldeten Oberfläche geprüft. Keine Regeln deaktiviert und keine Suppressions eingeführt.

Die [vollständige Zuordnung der zwölf IDs](sonar-restpaket-8-findings-2026-10-21.csv)
enthält Pfad, Exportzeile, Ergebnis und individuelle Begründung.

## Gezielte Codeänderung

Die vier Filterfelder für Aktivierung, Benutzerstatus, Rolle und letzte Aktivität gehören
fachlich zusammen. Sie werden in einem lokalen typisierten `FilterControls`-Record
übergeben. Toolbaraktionen, Suchfeld, Texte und Anwendungscallback bleiben separat.
Der Konstruktor hat dadurch **sechs statt neun Parameter**. Der Record ist nur ein
Übergabeobjekt und wird nicht als neuer persistierter UI-/Sessionzustand gespeichert.

Geändert wurden nur `AdminUserFilterBar`, dessen Aufbau in `AdminUserManagementSection`
und der bestehende Aufruf im `UiSerializationContractTest`. Dieselben Komponenten,
ihre Reihenfolge, Listener, Filteranwendung, Resetlogik und Callbackverträge bleiben erhalten.
Für diese strukturelle Bereinigung wurden vorhandene aussagekräftige Filter-/Paging-
und Serialisierungsregressionen verwendet; keine bloßen Implementierungstests ergänzt.

## Begründete Abweichungen

| Gruppe | Entscheidung |
| --- | --- |
| UserNotificationsView: 8 Parameter | Sechs Abhängigkeiten benötigt die gemeinsame authentifizierte Basis; Inbox und Konfiguration kommen hinzu. Alle genutzt. |
| AccountView: 10 Parameter | Sechs gemeinsame Basisabhängigkeiten sowie Konfiguration, Profil-, Kontosicherheits- und Sessiondienst verbinden bestehende Kontoaktionen. Fachlogik und Transaktionen bleiben bei Services. |
| AdminView: 10 Parameter | Sechs gemeinsame Basisabhängigkeiten plus Konfiguration, Wartung, Mail und Rollen für bereits getrennte Admin-Komponenten. |
| RegistrationView: 8 Parameter | Alle Abhängigkeiten für Formular-/Konfliktvalidierung, Standardrolle, Kodierung, transaktionale Registrierung, Zugangsschutz, Request, Konfiguration und Texte gebraucht. Keine Umverteilung des Sicherheits-/Rollbackablaufs nur für die Schwelle. |
| Drei Vererbungshinweise | Die Kette über IsAuthenticatedBaseView, TrackedUserAppLayout und UserActivityAwareView bis zu Vaadin AppLayout/Component erhält gemeinsamen Zugangsschutz und Profil-/Sprach-/Avatar-/Aktivitätslebenszyklus. Keine Ebene allein für eine generische Tiefenschwelle entfernt. |
| S2143 / Date | Akzeptierte API-Grenze des SessionInformation-Konstruktors; interne Instant- und Speichersemantik erhalten. |
| S1452 / Wildcard | Akzeptierte Kapselung des paketprivaten JdbcSession-Typs ohne Rawtype/Cast oder künstliche neue Repositoryarchitektur. |
| S3077 / volatile | False Positive: sichere Veröffentlichung einer verwalteten Service-Referenz; die Referenzklasse mutiert keinen Servicezustand. |
| S2142 / Interrupt | False Positive: Flag wird bis zum Drain gemerkt und garantiert in process.finally wiederhergestellt. Bestehender kontrollierter Nebenläufigkeitstest schützt den Vertrag. |

Die vier Konstruktorentscheidungen sind bewusst **Accepted**, nicht False Positive:
Die Parameterzahlen stimmen; der zusätzliche Umbau wäre hier nicht verhältnismäßig.
Keine Sammel-Bean, Service-Locator oder neue Setterinjektion zur Warnungsberuhigung.

## Tatsächliche Prüfungen

- Tatsächliches JDK **25.0.4.1**, Maven Wrapper **3.9.16** geprüft.
- Im echten Projektroot nach der Änderung `spotless:apply`, `spotless:check` und
  `checkstyle:check` erfolgreich; **0 Checkstyle-Verstöße**.
- Isolierte bestehende Buildkopie mit **372 SHA-256-geprüften Eingaben** am
  aktuellen Stand synchronisiert; keine .env-, Datenbank- oder fremden Laufzeitdateien kopiert.
- Gezielt **27 Tests** in AdminUserManagementFilterTest und UiSerializationContractTest:
  **0 Fehler, 0 Fehlschläge, 0 Skips**.
- Abschließend `clean verify -Dvaadin.productionMode=true`: **2.205 Anwendungstests**,
  **0 Fehler, 0 Fehlschläge, 0 Skips**; Produktionsfrontend, ausführbares Spring-Boot-JAR
  und frische JaCoCo-Messung erfolgreich. Die gezielten Tests sind darin enthalten.
- Frische Coverage: Zeilen **10.778/12.209 (88,28 %)**, Verzweigungen **3.195/4.100 (77,93 %)**;
  **237 Klassen** im Messumfang, einschließlich des neuen FilterControls-Records.
- `git diff --check` erfolgreich. Keine neue Anwendungs-Browserrunde ausgeführt;
  dafür liegt für dieses Paket keine eigene Freigabe vor. Sonar-Oberflächenbedienung
  dient der freigegebenen Befundeinstufung und ist keine Browserabnahme der Anwendung.

## Nächster Scan und Einführung

Nach den elf serverseitigen Einstufungen zeigte die offene Sonar-Liste **25 Findings**.
Diese sind noch der Stand des bisherigen Scans: die 24 im vorherigen Paket adressierten
Hinweise plus der jetzt korrigierte Filterkonstruktor. Sie wurden nicht manuell als
behoben geschlossen. Der nächste Benutzer-Scan muss den tatsächlichen Entfall und
mögliche Folgehinweise bestätigen. Keine Zusage eines Nullbefunds ohne neuen Scan.

`sonar-findings.json` bleibt der vorhandene Export und wurde nicht mit manuell geänderten
Statuswerten überschrieben. Nach Commit und eigenem Scan bitte neu exportieren.
Keinen Scan, Commit, Push oder Merge ausgeführt.

Übernahme der Java-Änderung durch Build und Anwendungsneustart; keine sichtbare
Designänderung und keine Datenmigration. Die Sonar-Statusänderungen sind bereits
serverseitig wirksam. Keine bestehenden Container, realen Datenbanken oder
Anmeldedaten verändert und keine realen E-Mails versandt.
