# Manuelle Sonderprüfungen

Stand 15.10.2026. Der Katalog enthält **15 gezielte Fälle**: **7 menschliche Abnahmen**
und **8 derzeit offene Automatisierungskandidaten**. Er ersetzt die alte vollständige
Handcheckliste. Die 100 vorhandenen Browserfälle und technischen JUnit-Regeln werden
hier nicht erneut Schritt für Schritt abgearbeitet.

| Datei | Zweck |
| --- | --- |
| [anwendung.md](anwendung.md) | Nur die benötigte isolierte Testumgebung vorbereiten und starten |
| [properties.example](properties.example) | Minimale lokale Einstellungen; vor Benutzung kopieren und ergänzen |
| [vorlage.md](vorlage.md) | Leeres Protokoll mit Fällen, Erwartungen, Befunden und Nachtest |
| [vorlage_ausgefüllt.md](vorlage_ausgefüllt.md) | Vollständig erfundenes Lernbeispiel; ausgefüllte Werte mit 🟦 markiert |

## Was ist noch von Hand sinnvoll?

| ID | Prüfziel | Einordnung |
| --- | --- | --- |
| [HUM-01](vorlage.md#hum-01) | Visuelle Gesamtabnahme | Menschliche Abnahme |
| [HUM-02](vorlage.md#hum-02) | Echtes Mobilgerät und Bildschirmtastatur | Menschliche Abnahme |
| [HUM-03](vorlage.md#hum-03) | Tastatur, Zoom und sichtbarer Fokus | Menschliche Abnahme |
| [HUM-04](vorlage.md#hum-04) | Screenreader und verständliche Ansagen | Menschliche Abnahme |
| [HUM-05](vorlage.md#hum-05) | Verständlichkeit der deutschen und englischen Texte | Menschliche Abnahme |
| [HUM-06](vorlage.md#hum-06) | Darstellung in tatsächlich verwendeten Mailprogrammen | Menschliche Abnahme |
| [HUM-07](vorlage.md#hum-07) | Recovery-Entscheidung bei unklarem Mailversand | Menschliche Abnahme |
| [AUTO-01](vorlage.md#auto-01) | Serverneustart mit derselben Testdatenbank | Zur Automatisierung vorgesehen – Betriebsfixture |
| [AUTO-02](vorlage.md#auto-02) | SMTP-Vorgaben beim Erststart und Wechsel nach Neustart | Zur Automatisierung vorgesehen – Betriebsfixture |
| [AUTO-03](vorlage.md#auto-03) | Realer SMTP-Ausfall mit anschließendem Wiederanlauf | Zur Automatisierung vorgesehen – Betriebsfixture |
| [AUTO-04](vorlage.md#auto-04) | Dauerhafter SMTP-Ausfall und brauchbare Betriebsdiagnose | Zur Automatisierung vorgesehen – Betriebsfixture |
| [AUTO-05](vorlage.md#auto-05) | Sprachwechsel mit sichtbaren Feldfehlern und Entwürfen | Zur Automatisierung vorgesehen – Browserfall |
| [AUTO-06](vorlage.md#auto-06) | Neue Mails nach Sprachwechsel für dasselbe Konto | Zur Automatisierung vorgesehen – Browserfall |
| [AUTO-07](vorlage.md#auto-07) | Verbindungsunterbrechung während einer Bearbeitung | Zur Automatisierung vorgesehen – Browserfall |
| [AUTO-08](vorlage.md#auto-08) | Vorgesehener SMTP-Transport mit Auth/TLS und Testpostfach | Zur Automatisierung vorgesehen – Integrationsfixture |

**Zur Automatisierung vorgesehen** kennzeichnet eine noch nicht automatisierte
Prüffolge. Der Zusatz unterscheidet Browserfälle von einer **Betriebsfixture**
(eigene Prozesse, Neustarts, Ausfälle) und einer **Integrationsfixture** (eigenes
freigegebenes SMTP-/Postfachziel). Diese Fälle bleiben bis zum tatsächlichen
automatisierten Nachweis hier offen; die Markierung ist keine Testerfolgsanzeige.

## Kurzer Ablauf

1. Passende Fälle auswählen. Für eine Oberflächenabnahme sind vor allem HUM-01–05
   relevant; Geräte/Hilfsmittel und Mailclients ausdrücklich nennen.
2. Die [Einrichtung](anwendung.md) verwenden. Für reine Text-/Recovery-Beurteilung
   ist keine neue Anwendung nötig, falls geeignete Ansichten bzw. Unterlagen vorliegen.
3. Eine Kopie der Vorlage unter dem eigenen lokalen Prüfungsordner ausfüllen.
   Leere Felder bedeuten nicht bestanden. Die [Beispieldatei](vorlage_ausgefüllt.md)
   erklärt Status, Fehleraufnahme, Korrektur und begrenzten Nachtest.
4. Nur tatsächlich beobachtete Ergebnisse eintragen. Wichtige Erkenntnisse dauerhaft
   als kurze Markdown-Zusammenfassung sichern; keine Pflicht zum Behalten aller Läufe.

## Abgrenzung und Pflege

- Automatisierter Stand: [Browserabdeckung](../../browser-tests/abdeckung.md).
  Anmeldung, Registrierung, Linkgültigkeit, CRUD, Filter/Pagination, Status,
  responsive Fenster und einzelne Shortcuts sind dort bereits geprüft.
- JUnit/Persistenz prüft fachliche Regeln, Konkurrenz, exakte Zeitgrenzen sowie
  interne Queue-/Auditverträge. Kein Datenbankinspektionsprogramm für normale Handprüfer.
- Sobald ein AUTO-Fall erfolgreich automatisiert ist, dessen manuelle Dopplung entfernen
  und den Nachweis im Browser-/Integrationstest pflegen. Erforderliche menschliche
  Bewertung bleibt als klar begrenzter HUM-Fall erhalten.
- Neue Betriebsprüfungen benötigen eigene kontrollierte Fixtures. Fehlende Einsicht
  ist BLOCKIERT; kein SQL, Statusrücksetzen oder künstliche Sessionereignisse als Ersatz.
- Für Bestandsmigrationen gelten eigene Anleitungen: [Versionierung](../user-version-migration.md),
  [Identität/Kaskaden](../user-identity-migration.md) und [Verifikationsstatus](../verification-state-migration.md).
  Nur eine ausdrücklich vorbereitete Testkopie verwenden; diese Kurzliste ersetzt keine Migrationserprobung.
- Zum [Designstandard](../design-standard.md) und zum [Recovery-Vertrag](../email-queue-processing.md).

Temporäre Protokolle liegen zusammen mit ihrer lokalen Testkonfiguration unter
`data/manual-tests/` (bereits ignoriert), statt neue Archive im Dokumentationsordner anzulegen.
Bestehende Datenbanken, Konfigurationen oder Browser-Run-Ordner werden dabei nicht bereinigt.
