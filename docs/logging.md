# Betriebs- und Diagnosemeldungen

## Zweck und Grundpegel

Normalbetrieb verwendet `INFO`. Die gemeinsame Konfiguration steht in
`src/main/resources/logback-spring.xml`; Spring-Boot-Pegel stehen in
`application.properties` und lassen sich wie andere Spring-Einstellungen überschreiben.
Für eine Untersuchung gezielt einen Dienst aktivieren, beispielsweise
`logging.level.de.derpeterson.app.service.UserNotificationService=DEBUG`.
Nach der Untersuchung wieder auf den normalen Pegel zurückstellen.

`AdmittedSecurityContextRepository` erbt den Logger aus Spring Security. Dessen
Konstruktor verwendet die konkrete Unterklasse als Loggernamen. Deshalb erreichte
die frühere Paketfreigabe `de.derpeterson.app=DEBUG` auch die Frameworkmeldung
`Retrieved SecurityContextImpl`, trotz `org.springframework.security=INFO`.
Im bereitgestellten Auszug betraf das 348 von 666 Zeilen. Der Abruf gehört zu normalen
HTTP-/UI-Anfragen und ist für sich kein Fehler. Der vollständige Kontext enthält
Principal- und Authentifizierungsdaten und wird nicht für Betriebsdiagnose benötigt.

Die beiden geerbten Logger `AdmittedSecurityContextRepository` und
`DeviceRememberMeServices` behalten ausdrücklich `INFO`, auch wenn das Elternpaket
auf DEBUG gestellt wird. WARN/ERROR bleiben sichtbar. Die vorhandene Dateirotation
(25 MB pro Teil, tägliche Aufteilung, `maxHistory=1`) bleibt erhalten.

## Welche Meldung erhält welchen Pegel?

| Pegel | Einsatz |
| --- | --- |
| INFO | Neue Konten, Adminänderungen/-löschungen, Rollenänderungen, Sicherheitsereignisse, Benachrichtigungsversand, neue Sitzungszulassung und ausdrücklicher Gerätewiderruf; Mail vom Transport angenommen; tatsächliche Bereinigung; Startzusammenfassung |
| DEBUG | Profil-/Avataränderung, einzelne Lese-/Entfernungsaktionen, Queueaufnahme, deduplizierte Anforderung oder wiederverwendeter Versandnachweis, geänderter Konfigurationsschlüssel und Formularvalidierung |
| WARN | Übergang in eine Loginversuchssperre, behandelbarer Mailfehler mit geplanter Wiederholung, geschützter letzter Superadmin oder verzögerte UI-Aktualisierung |
| ERROR | Endgültig fehlgeschlagener/unklarer Versand oder fehlgeschlagene fachliche Verarbeitung mit sicherer Fehlerdiagnose |

Keine Meldungen für jeden Methodenaufruf, jede Abzweigung, Polls, Heartbeats,
Inboxabfragen, laufende Aktivität oder unveränderte Statuswerte hinzufügen.
Bereinigungen ohne Treffer bleiben still. Ein bereits aktiver Loginblock löst nicht
bei jedem weiteren Versuch erneut WARN aus. Beim Start ersetzt eine Zusammenfassung
die bisherigen Meldungen für jeden schon vorhandenen Konfigurationseintrag.

## Wahrheitsgemäße Ergebnisse

Innerhalb einer Transaktion Erfolg über `AfterCommitLog.info/debug/warn` anmelden.
Die Ausgabe erfolgt erst nach erfolgreichem Commit; Rollback und Commitfehler
unterdrücken sie. Die Argumente müssen unveränderliche Metadaten sein, etwa IDs,
Anzahlen, Booleans oder Enums. Keine Entities oder veränderlichen Formulare übergeben.
Der Helfer kopiert das Argumentarray. Ohne laufende Transaktion erfolgt die Ausgabe
sofort; ihn dort nur nach bereits abgeschlossenen unabhängigen Operationen verwenden.

Nach `TransactionTemplate.execute` darf ein bereits committetes Ergebnis direkt
protokolliert werden. Die Konfiguration speichert jeden Schlüssel unabhängig;
ein Gruppenfehler kann daher bereits gespeicherte Einzelwerte hinterlassen und
benennt das ausdrücklich. Kein pauschaler Gruppenerfolg bei einem Teilfehler.

Queueaufnahme, Annahme durch SMTP und Lesestatus sind unterschiedliche Ergebnisse.
Eine Reset-/Verifikationsmail ist nach dem Einreihen noch nicht gesendet.
`Email accepted by transport` bestätigt die erfolgreiche Rückkehr des Transports
und den gespeicherten Queueausgang, keine Zustellung im Posteingang des Empfängers.
Idempotente Benachrichtigungswiederholung erzeugt keine zweite Versand-Erfolgsmeldung.

## Inhalte und Fehlerdiagnose

Erlaubte Inhalte sind interne numerische IDs, Typen, Anzahlen, Versuchsnummern,
Konfigurationsnamen und unkritische Zustände. Keine Namen, E-Mail-Adressen,
Nachrichtenbetreffe/-texte, Formularwerte, Kennwörter/-hashes, Principalobjekte,
Sessionkennungen, Cookies, Tokenserien, Tokenwerte oder Token-Digests ausgeben.

Auch `exception.getMessage()` und ungefilterte Exceptions können SQL-Bindewerte,
SMTP-Antworten oder Links enthalten. Anwendungslogger verwenden daher
`LogSafety.safeFailure`: Eine separate diagnostische Kopie erhält ursprüngliche
Exceptiontypen als Meldung, Stackframes sowie Ursache-/Suppressed-Verknüpfungen;
freie Exceptiontexte entfallen. Die ursprüngliche Exception für Fehlerbehandlung,
Klassifizierung und erneutes Werfen bleibt unverändert. Tiefe, Zahl und Frames der
Kopien sind begrenzt; Zyklen bleiben druckbar. Die feste Logmeldung benennt zusätzlich
Operation und unkritischen Bezug, beispielsweise Queue-ID und Versuchsanzahl.

Bestehende technische Frameworkwarnungen werden durch die Änderung nicht pauschal
unterdrückt. Die Anwendungsregeln ersetzen keine Prüfung von separat aktivierten
Bibliotheks-Debug-/SQL-/Transportausgaben.

## Einführung und Nachweis

Die Änderungen benötigen einen vollständigen Anwendungsneustart mit neu gebauter
Anwendung bzw. neuem Produktionsimage. Ein Browser-Refresh allein übernimmt sie
nicht. Die laufende Entwicklungsanwendung wird für die Prüfung nicht angehalten.

Gezielte Tests prüfen effektive Loggerpegel auch bei Paket-DEBUG, weiterhin sichtbare
WARN/ERROR, Commit/Rollback/Commitfehler, keine privaten Mail-/Fehlertexte und einen
idempotenten Versand über echte H2-Transaktionen. Die ursprüngliche Gesamt-QA und
Coverage vor dieser Überarbeitung stehen im
[QA-Meilenstein](meilensteine/2026-10-20-gesamtqa-und-benachrichtigungsredesign.md).
Der [Nachteststand dieser Überarbeitung](meilensteine/2026-10-20-gezielte-logging-ueberarbeitung.md)
wird getrennt von dieser vorherigen Gesamt-QA dokumentiert.

Abschluss 20.10.2026: 1843/1843 Anwendungs-JUnits und DE/EN je 8/8 gezielte
Produktions-Browsernachtests erfolgreich, keine Fehler/Skips. Vollständige
Testserverlogs bestätigen das Ausbleiben der geprüften Context-/Principal-/
Credentials-Dumps und Testzugangsdaten. Weitere Details im verlinkten Nachweis.
