# Offener Testbedarf

## Browser-Tests

- **2FA-Konfiguration (wartend auf Umsetzung):** Rollen-/Funktionsregeln,
  effektive Anforderungen, Aus/optional/Pflicht und Wiederholungsfristen auf
  Desktop/Smartphone DE/EN prüfen. Nur Superadmins dürfen die Konfiguration öffnen,
  laden und ändern. Alte Ansichten nach Rollenentzug und Änderungen nach zuvor
  geltender Regel absichern. Beim Wechsel von freiwillig auf Pflicht müssen auch
  bestehende Konten ab dem nächsten geschützten Zugriff Einrichtung oder Bestätigung
  abschließen. Offene und dauerhaft angemeldete Sitzungen umgehen dies nicht.
  Pflicht auch für Admins und Superadmins auf freiwillig oder Aus umstellen können.
  Die Verwaltungsberechtigung bleibt dabei ausschließlich bei Superadmins.
- **2FA-Anmeldung und Kontoaktionen (wartend auf Umsetzung):** Portalpasswort,
  Google, Microsoft, dauerhafte Anmeldung, neue Sitzungen, Registrierung und
  Sitzungsgrenzen mit/ohne Faktor prüfen. Direkte Rücksprünge oder alternative
  Zugänge dürfen die effektive Regel nicht umgehen.
  Innerhalb einer gültigen aktiven Sitzung bei Navigation, Refresh und weiterem Tab
  keine erneute Anmelde-2FA verlangen. Die zusätzliche Inaktivitätsabfrage ist
  zunächst deaktiviert. Einschalten mit gültigem Zeitraum, Einheitenwechsel und
  Ausschalten unter Beibehaltung des Zeitwerts prüfen. Nach Abmeldung, Sitzungsablauf
  oder Ablauf einer eingeschalteten Inaktivitätsregel erneut bestätigen.
  Hintergrundabfragen und Heartbeats verlängern die
  Aktivitätsfrist nicht. Abgelaufenen Faktorstatus von vollständig abgelaufener Sitzung unterscheiden.
  Bei aktiver 2FA sensible Änderungen standardmäßig jeweils frisch bestätigen.
  Anmeldebestätigung oder Freigabe einer anderen Aktion darf nicht genügen.
  Abweichende Konfiguration je Rolle/Funktion berücksichtigen. Pflicht ohne Faktor
  führt zur gesicherten Einrichtung, nicht zur unbestätigten Änderung.
- **Google-Ausnahme und Wiederherstellung (wartend auf Umsetzung):** Bekannten
  Umgehungsweg im sicheren Betrieb abweisen. Entwicklungs-Ausnahme nur ausdrücklich
  freigegeben und gewarnt zulassen. Einrichtung, verlorenen Faktor, Ersatzcodes,
  Wechsel, Entfernen und abgesicherte Hilfe samt letztem Superadmin prüfen.
  Geheimnisse dürfen nicht in dauerhaftem UI-/Sitzungszustand verbleiben.
  Entwicklungsoptionen ohne Startfreigabe auch für Superadmins weder anbieten noch
  serverseitig anwenden. Mit Startfreigabe die einzelnen Ausnahmen und Warnungen
  prüfen. Gespeicherte Ausnahmen nach Neustart ohne Freigabe bleiben unwirksam.
- **Erste Google-2FA-Einrichtung (wartend auf Umsetzung):** Erneute Bestätigung
  des zugehörigen Google-Kontos, einmaligen Code an die hinterlegte bestätigte Adresse,
  ersten TOTP, Ersatzcode-Ausgabe und Sicherheitsmail auf Desktop/Smartphone DE/EN prüfen.
  Fehler, Ablauf und Wiederholungsgrenzen verständlich behandeln. Kein Ersatz für
  bereits eingerichtete/verlorene 2FA oder Freigabe anderer sensibler Aktionen.
- **Superadmin-Anbieterverwaltung, P6 (wartend auf Umsetzung):**
  Navigation, direkte Aufrufe, Neuladen und alte Ansichten für Superadmin, Admin und
  Benutzer prüfen. Reguläre Google-/Microsoft-Anmeldung bleibt unabhängig nutzbar.
- **Wiederherstellungsanfragen/Ticketgrundlage (wartend auf Umsetzung):**
  Nach erstem Nachweis ohne vollständige 2FA nur eigene Anfrage erstellen und deren
  Stand/Rückfragen einsehen. Gemeinsame Adminliste, Benachrichtigung und Übernahme
  prüfen. Admins bearbeiten, nur Superadmins geben nach gesicherter Prüfung frei.
  Kein Fremdzugriff über Vorgangsnummern und keine internen Vermerke im Benutzerverlauf.
  Ticketabschluss allein verändert weder Faktor noch Kontozugriff. Desktop/Smartphone DE/EN.
  Ohne geeigneten Wiederherstellungsnachweis keine Freigabe nach Namen, Geburtsdatum,
  freiem Tickettext oder E-Mail-Zugriff allein ermöglichen. Notfalldokument zur
  einmaligen Ersatzcode-Ausgabe auf verständliche Darstellung und Druckbarkeit prüfen.
  PDF-Download in DE/EN auf Logo, Servicename, Kontozuordnung, Datum, Anleitung und
  vollständig lesbare Codes prüfen. Erzeugte Test-PDF zusätzlich rendern und auf
  korrekte A4-Seitenaufteilung ohne abgeschnittene Texte/Codes sichten.
  Vorbereitete Anleitung DE/EN anzeigen, als Ticketantwort festhalten und nur an
  die bestätigte Kontaktadresse des Kontos versenden. Keine Adresse aus Tickettext
  übernehmen. Ohne Authenticator/Ersatzcode bleibt der Zugang nach Versand und
  Ticketabschluss gesperrt. Keine Sicherheitsfragen oder zusätzliche Kontodeaktivierung.

## JUnit-Tests

- **TOTP und Ersatzcodes (wartend auf Umsetzung):** Standard-Testvektoren,
  Zeitgrenzen, führende Nullen, Replay einschließlich Parallelzugriff, kontobezogene
  Versuchslimits, verschlüsselte Speicherung, fehlenden Schlüssel und Rollback prüfen.
  Ersatzcodes sind atomar einmalig verwendbar. Neue Codes widerrufen alte.
- **2FA-Regeln und Vertrauensgrenzen (wartend auf Umsetzung):** Rollen,
  Funktionen, Mehrfachrollen, Nachweisfristen, Richtlinien-/Faktorversionen und
  Konfigurationsabschwächung nach alter Regel prüfen. Entwicklungsfreigabe und
  Google-Ausnahme separat absichern. Zweck-/Konto-/Versions-/Sitzungsbindung erhalten.
  Wechsel auf Pflicht bei bestehenden Konten mit/ohne Faktor auch in laufenden und
  wiederhergestellten Sitzungen durchsetzen. Mehrfachrollen verwenden die strengste Regel.
  Keine fest eingebaute Admin-/Superadmin-Pflicht entgegen der aktuellen Konfiguration.
  Aktionsfreigaben nur einmal und für den konkreten Zweck verwenden. Voreinstellung
  verlangt bei aktiver 2FA frische Bestätigung jeder sensiblen Änderung. Regeländerungen
  dürfen die separate Freigabe der Google-Entwicklungs-Ausnahme nicht ersetzen.
  Startfreigabe standardmäßig false: fehlende/ungültige Werte, direkte Aufrufe,
  gespeicherte Ausnahmen, alte Ansichten und aus Entwicklung stammende Freigaben
  gegen normale Durchsetzung prüfen. Startfreigabe allein deaktiviert nichts.
  Ausgesetzte Prüfung erzeugt keinen echten MFA-Nachweis. Bestehende Rollen-,
  Konto-, Token-/Signatur-, Eingabe-, CSRF- und Widerrufsschutz bleiben erhalten.
- **Anmeldung, Registrierung und Widerruf (wartend auf Umsetzung):**
  Passwort- und externe Dienstpfade, direkte Rücksprünge, gleiche E-Mail-Adressen,
  Erstregistrierung und dauerhafte Anmeldung prüfen. Kein Faktor-Reset oder
  Kontozugriff über schwächere Alternativen. Wiederherstellung, Faktorwechsel,
  letzter Superadmin, Sitzungswiderruf und Mail-/Transaktionsfehler absichern.
  Sitzungs-/Inaktivitätsgrenzen mit kontrollierter Uhr prüfen. Deaktivierte Zusatzregel
  löst keine eigene Inaktivitätsabfrage aus. Aktivierte Regel verlangt positiven
  gültigen Zeitraum, rechnet Minuten/Stunden/Tage korrekt um und bleibt von den
  Anmelde-/Sitzungs- und Aktionsregeln getrennt. Dauerhafte Anmeldung
  und Wiederherstellung einer Sitzung dürfen abgelaufene 2FA nicht als frisch markieren.
- **Superadmin-Anbieterverwaltung, P6 (wartend auf Umsetzung):**
  Lesen/Speichern/Neuladen mit aktuellen Rollen auch nach Rollenentzug prüfen.
  Interne Anbieterauflösung für reguläre OAuth-Anmeldungen benötigt keine Adminrolle.
- **Ticketgrundlage und Wiederherstellungsfreigabe (wartend auf Umsetzung):**
  Kontobindung, eigene/fremde Sichtbarkeit, Übernahmekonflikte, Eingabe-/Mengenlimits,
  doppelte offene Anfragen und aktuelle Bearbeiterrechte prüfen. Allgemeiner Ticketstatus
  oder Tickettyp erzeugt keine Sicherheitsfreigabe. Veraltete, abgelaufene oder bereits
  verwendete Freigaben auch nach Faktor-/Kontozustandsänderung ablehnen. Benachrichtigungen
  erst nach Commit erzeugen und keine Geheimnisse oder internen Antworten offenlegen.
  Eigene frische Superadmin-Bestätigung ersetzt nicht den fehlenden Kontonachweis
  des Antragstellers. Vollständiger Verlust der Nachweise führt zu keiner automatischen
  Freischaltung. Exportierte Ersatzcodes werden nicht für spätere Klartextabrufe gespeichert.
  Vorlagenversand mit aktuellen Bearbeiterrechten, korrekter Sprache/Empfängerbindung,
  Mehrfachklicks und Queuefehlern prüfen. Anleitung enthält keine Geheimnisse und
  erzeugt keine Sicherheitsfreigabe. Eine Anfrage allein darf kein Konto deaktivieren.
  Ticketvorschau/-antwort und MJML-Mail verwenden konsistente übersetzte Inhalte.
  Technische Betreiberanleitung ist keine auswählbare oder an Benutzer versendbare Antwort.
- **Ersteinrichtungsnachweis (wartend auf Umsetzung):** Codeablauf, einmalige
  Nutzung, Versand-/Prüflimits, erneuten Versand und Bindung an Konto, Sitzung, Zweck,
  Zugangs-/Faktorversion und bestätigte Empfängeradresse prüfen. Falscher Google-
  Kontobezug, zwischenzeitliche Kontaktänderung, fehlende Kontaktbestätigung und
  parallele Faktorregistrierung verhindern den Abschluss. E-Mail-Code darf keinen
  vorhandenen Faktor ersetzen oder allgemeine Aktionsfreigaben erzeugen. Mailfehler
  erfüllen keine Bestätigung. Sicherheitsmeldung über erfolgte Einrichtung erst nach Commit.
- **Manueller Betreiber-Notfallweg (wartend auf fertiges 2FA-Schema):** Dokumentierte
  zusammengehörige Datenbankänderungen an einem isolierten ausgesperrten Superadmin
  nachspielen. Alter Faktor, Ersatzcodes, Sitzungen, dauerhafte Zugänge und Freigaben
  sind anschließend ungültig. Neueinrichtung ist vor vollständigem Zugriff erforderlich.
  Betreiberfreigabe ist kontogebunden und nach Abschluss verbraucht. Bloß geleerte
  Faktor-Daten dürfen keinen erneuten offenen Erstregistrierungsweg freischalten.
  Andere Konten und deren 2FA-Regeln bleiben unverändert. Keine echte Datenbank verwenden.
- **PDF-Ersatzcodeausgabe (wartend auf Umsetzung):** Vorlage mit synthetischen Codes
  in DE/EN erzeugen und Inhalt, Ressourceneinbettung und Sonderzeichen/Langtexte prüfen.
  Classpath-Zugriff auch im gepackten JAR absichern. Keine externen Ressourcenabrufe,
  Klartext-Dateiablage oder Geheimnisse in Renderer-Fehlerausgaben. Fremde, abgelaufene
  und nach Abschluss erneut angeforderte Ausgabe ablehnen. PDF-Fehler erzeugen keinen
  weiteren gültigen Codesatz und stellen verbrauchte Codes nicht wieder her.

Menschliche Abnahmen bleiben im [manuellen Katalog](manual-tests/README.md).
