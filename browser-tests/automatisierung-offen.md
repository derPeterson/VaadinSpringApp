# Offene Automatisierung und Paketversuche

## Benutzerverwaltung – Versuch 1 (abgebrochen)

Der Browserlauf `2026-10-12T02-34-27_UTC__5310516d__3b69f418`
scheiterte an mehrdeutigen Locators: Vaadin behält unsichtbare alte Grid-Zellen
und Aktionsbuttons nach dem Filtern. Die sichtbare Tabelle enthielt ein Konto;
die globale DOM-Suche traf zusätzliche verborgene Renderer.
Versuch 2 beginnt erneut mit Bestandsaufnahme und grenzt auf sichtbare Inhalte ein.
Der rote Bericht und seine Screenshots/Traces bleiben unverändert.

### Versuch 2 – Paket bleibt offen

`2026-10-12T02-36-22_UTC__5310516d__f5349832`: sichtbare Zellen/Buttons
wurden eindeutig erkannt, anschließend öffneten vier Fälle den erwarteten
Bearbeitungsdialog nicht; beim Löschen lieferte der Overlay-Locator keinen
gerenderten Kontotext. 0/5 bestätigt. Kein Nachweis eines Produktionsfehlers.
Die fehlgeschlagene Suite und Helfererweiterung wurden auf den sauberen
Paket-Ausgangsstand zurückgeführt; beide historischen Berichte bleiben erhalten.
Nötig: Vaadin-Grid-Aktionszuordnung und Dialog-Slot-Struktur gezielt untersuchen.
USER-01 bis USER-04 bleiben offen; nur die bisherige Anlagevorbereitung ist belegt.

## Abhängiges Paket: Sitzungsrollen und Kontosperren

SEC-02/03/04, VER-04 und RESET-06 benötigen verlässlich bedienbare Adminänderungen
und Löschaktionen. Sie werden nicht auf dem unbestätigten neuen Helfer aufgebaut.
Nach Lösung des Benutzerverwaltungs-Pakets sind sie als eigene UI-Fälle nachzuholen.

## Weitere Kontoabläufe – Versuch 1 (abgebrochen)

`2026-10-12T02-40-22_UTC__fbc1ba18__7a45978d`: 3/5 bestanden
(Dublette, neutrale Antwort für unverifizierte Adresse, veraltetes Resetformular).
Die Einzelvalidierungen scheiterten an gleichzeitig vorhandenen gleichlautenden
Notifications nach mehreren Versuchen. Versuch 2 beginnt neu und schließt geprüfte
Hinweise über deren echte UI-Buttons, bevor die nächste Variante eingegeben wird.

### Weitere Kontoabläufe – Versuch 2 erfolgreich

DE: `2026-10-12T02-42-02_UTC__fbc1ba18__39d243db`, 5/5 bestanden.
Der zugehörige EN-Lauf wurde ebenfalls mit 5/5 bestätigt; Belege in den Run-Archiven.
Die Paketgrenze wurde eingehalten: keine dritte Reparaturrunde.

## Konfiguration/Mail – historisches Paket nach zwei Versuchen offen

**Nachfolgeauftrag:** Der Token-Dauerfehler ist inzwischen behoben und separat
DE/EN geprüft (siehe Nachprüfung unten). Weitere Config-/Mailfälle bleiben offen.
Der folgende Abschnitt beschreibt unverändert den damaligen Stand.

Versuch 1: `2026-10-12T02-47-02_UTC__ea41bc7e__420ff512`, 0/3 bestanden.
Der Wartungs-Boolean wurde in der falschen Gruppe gesucht; ein korrekt übertragener
Unicode-Mailtext enthielt den SMTP-typischen abschließenden Zeilenumbruch.
**Bestätigter Produktionsbefund:** `PT0S` wurde als Reset-Token-Gültigkeit gespeichert,
obwohl VerificationService/PasswordResetService strikt positive Dauern voraussetzen.
AdminConfigurationSection validiert derzeit nur die Syntax einer Dauer.

Versuch 2 wurde schon bei der Vorbereitung der Korrektur abgebrochen:
Das Bearbeitungsskript las die UTF-8-Produktionsdatei irrtümlich als Windows-1252
und scheiterte mit UnicodeDecodeError. Es wurden noch keine Produktionsänderungen
geschrieben und kein weiterer Browserlauf gestartet. Die unbestätigten neuen
Helper-/Suiteänderungen wurden zum erfolgreichen Konto-Paketstand zurückgeführt.
Kein dritter Versuch. Nötig: explizites UTF-8-Lesen, positive Prüfung für die vier
Token-Dauern, korrigierte Gruppenbedienung und SMTP-Zeilenabschlussvergleich;
anschließend DE/EN-Regressionsläufe. CONFIG-01/02 und MAIL-01 bleiben offen.

## Sprache/Bedienung – Versuch 1 (abgebrochen)

`2026-10-12T02-51-59_UTC__91a4c046__60ee3047`, 1/5 bestanden
(Tastaturlogin). Registrierung, Verifikation und Reset besitzen keinen Avatar-
Sprachschalter; ein tatsächlicher UI-Wechsel bei offenem Hinweis ist dort mit
dem bestehenden Produkt nicht erreichbar. Keine künstlichen Session-/JS-Ereignisse
als Ersatz. LANG-01 bleibt dort offen; feste DE/EN-Durchläufe bestehen weiterhin.
Mobil bleibt der Datepicker nach Enter im Vollbild geöffnet; echtes Escape ist
für den Formularfall möglich. Normaler Home-Logout invalidiert die Session ohne
zwingend die Route zu ändern. Zweiter Versuch passt die Bedienung an diese echten
Verträge an und behält Grenzen ausdrücklich bei.

### Sprache/Bedienung – Versuch 2 erfolgreich im erreichbaren Teilumfang

DE: `2026-10-12T02-55-38_UTC__91a4c046__97513ab7`, 4/4 bestanden;
auch der entsprechende EN-Lauf besteht 4/4. Kein Produkt-Sprachschalter ergänzt,
kein fehlender Formularfall als bestanden gewertet. Die zwei unerreichbaren
Schalterfälle sind durch den ehrlich eingeschränkten Home-Schalterfall ersetzt;
ursprünglicher Umfang LANG-01 bleibt teilweise offen. Mobiles Escape und frische
Sprachpräferenz wurden tatsächlich durch die UI bestätigt.

## Remember-me – Versuch 1 (abgebrochen)

`2026-10-12T02-58-58_UTC__b4d8d751__74d6dab4`: ohne Option echter Neustart
erfolgreich; mit Option kein `remember-me`-Cookie nach normaler UI-Anmeldung
beobachtet (1/2 bestanden). Zweiter Versuch dokumentiert die vor dem Login
tatsächlich gesetzte Checkbox und prüft direkt den echten neuen Browserprozess;
es werden keine Cookie-/Authentifizierungszustände injiziert.

### Remember-me – Versuch 2 gescheitert, Paket bleibt offen

`2026-10-12T03-01-51_UTC__b4d8d751__326fdb88`: erneut 1/2 bestanden.
Die Variante ohne Option bestätigt zweimal den echten Browserprozess-Neustart.
Die Variante mit Option scheiterte diesmal schon an einem verdeckten Konto-E-Mail-
Element im nachgeladenen Popover. Der eigentliche Remember-me-Neustart wurde daher
nicht vollständig erreicht. Die Cookiebeobachtung aus Versuch 1 ist ein Hinweis,
aber kein abschließender Nachweis einer defekten Persistenzfunktion.
Beide Testberichte bleiben erhalten; unbestätigte Suite und Auswahlregistrierung
wurden wieder entfernt. AUTH-03 bleibt ohne zuverlässigen DE/EN-Gesamtnachweis offen.
Nötig: Popover-Lebenszyklus nach Login/Sprachwahl zuverlässig synchronisieren,
danach Cookieauslieferung und echte Profilneustarts gezielt prüfen. Keine Sicherheits-
oder Authentifizierungsänderung ohne eindeutige Ursache vorgenommen.

## Manueller Status/Live-Aktualisierung – Versuch 1 (abgebrochen)

`2026-10-12T03-04-04_UTC__65c6ddbd__765e2104`, 0/2 bestanden.
Der Overlay-Descendant-Locator traf keine Statusauswahl. Im tatsächlichen Trace
ist die sichtbare `vaadin-select` über Vaadin-Slots außerhalb des Overlay-DOMs
angeordnet; die gerenderten Optionen gehören zur sichtbaren Select-Listbox.
Zweiter Versuch beginnt neu und verwendet diese anhand des Traces überprüfte
DOM-Zuordnung. Kein Service-/Statuszustand wird künstlich gesetzt.

## Manueller Status/Live-Aktualisierung – nach zwei Versuchen offen

Versuch 1: `2026-10-12T03-04-04_UTC__65c6ddbd__765e2104`, 0/2 bestanden,
Overlay-Descendant-Locator fand die über Slots gerenderte Statusauswahl nicht.
Versuch 2: `2026-10-12T03-07-08_UTC__65c6ddbd__6e2bcfca`, 0/2 bestanden.
Die Auswahl war bedienbar; beim Prüfen wurden jedoch sowohl die aktuelle
Value-Anzeige als auch eine gleichlautende Option gefunden (Strict-mode-Fehler).
Nötig: Assertions ausdrücklich auf `vaadin-select-value-button` bzw. die aktuelle
Anzeige eingrenzen und die Optionsliste getrennt behandeln. Kein dritter Versuch.
Unbestätigte Suite/Helfer wurden entfernt; beide Berichte bleiben erhalten.
STATUS-01 bleibt offen. Der vorhandene technische Scheduler-/Persistenznachweis
ersetzt nicht den fehlenden vollständigen Browser-/Live-Nachweis.


## Nicht ausgeführte abhängige und besondere Fälle

- VER-03, RESET-04 und STATUS-02: kurze Laufzeit/Timeout muss zuerst über eine
  bestätigte Config-Oberfläche eingestellt und danach wiederhergestellt werden.
  Config-/Statuspakete sind gescheitert; keine Uhr- oder Datenbankabkürzung.
- START-02/03: Neustart mit derselben wegwerfbaren Datenbank und geänderten
  Erstbefüllungsvorgaben benötigt einen kontrollierten Runner-Lebenszyklus.
  Der bisherige Runner erstellt pro Lauf absichtlich neue Volumes. Das ist
  automatisierbar, wurde hier aber nicht zusätzlich in die Infrastruktur eingebaut.
- MAIL-03/04: realer SMTP-Ausfall/Wiederanlauf, einminütiger Retry und Audit-/Queue-
  Belege erfordern eigene Betriebsfixtures; die Anwendung hat keine Queue-/Audit-UI.
  Vorhandene technische Queue-/Adaptertests bleiben gültig, sind kein realer SMTP-Ausfalltest.
- OPS-01/02/04: Transportfehlerinjektion, präzise alte Zustände/Ablaufgrenzen und
  technisches Failure-Handling gehören in kontrollierte technische Integrations-
  und Persistenztests. Die vorhandenen Regressionen wurden mit dem Anwendungslauf
  geprüft; der ursprüngliche zusätzliche Betriebsnachweis bleibt offen.
- OPS-03: Recovery verlangt eine belegte operatorische Entscheidung über Versand
  und gestoppte Worker; keine pauschale Browserautomation oder automatische Wiederfreigabe.
- UI-01/02: echte Handyrotation/Bildschirmtastatur, 200-%-Browserzoom, sämtliche
  Admin-/Resetdialoge, Fokusqualität, Lesbarkeit und Designbeurteilung bleiben offen.
  Die neue schmale Registrierung und der Tastaturlogin decken nur ihren eigenen Umfang.
- VER-01/LANG-02/RESET-01: Mail-Footer ausdrücklich gegen die vorhandenen Getter
  prüfen; Betreff/Empfänger/Link/Platzhalterfreiheit allein beweisen keinen vollständigen Footer.
- VER-02: das angebotene Mailformular auf der ungültigen Verifikationskarte benötigt
  eigene ungültige Eingabe-/Korrekturfälle. Nicht mit der getesteten Resetanforderung verwechseln.

Diese Punkte sind offen oder mit der gegenwärtigen UI nicht erreichbar. Sie wurden
nicht zweimal erfolglos ausgeführt, wenn oben kein entsprechender Paketversuch steht.
Die Zwei-Versuche-Grenze wird nicht dadurch umgangen, dass ein gescheitertes Paket
umbenannt oder in neue Reparaturpakete zerlegt wird. Daher auch die jetzt erkannte
Vaadin-Slot-/Value-Zuordnung nicht als dritter Versuch für User/Status verwendet.

## Sinnvolle nächste Reihenfolge

1. Erledigt im ausdrücklich beauftragten Nachfolgefix: vier Token-Dauern mit
   Mindestgrenzen 1/2 Minuten abgesichert, ungültige Werte werden nicht gespeichert.
   Nachweise und noch offene übrige Config-Fälle siehe unten.
2. Admin-Grid-Aktionen und Vaadin-Dialoginhalte anhand echter Slots/DOM-Belege
   zuverlässig bedienen; Benutzerverwaltung und veraltete Formulare bestätigen.
3. Davon abhängig Rollenentzug/Sperre/Löschung in laufenden Sitzungen und alte Links.
4. Status-Value-Anzeige vom Optionsbereich trennen; anschließend echtes Live-Update.
5. Remember-me-Cookieauslieferung und Browserneustart prüfen, ohne auf einen
   verdeckten Popover-E-Mail-Locator als einzige Anmeldungsaussage angewiesen zu sein.
6. Config-/Mailpaket mit korrekten Gruppen und SMTP-Zeilenabschlussvergleich;
   danach Tokenablauf/erneuter Versand und kontrollierte Betriebsfixtures.

Es sind noch keine 100 % gleichen manuellen Abläufe automatisiert.


## Abschlussprüfung – zwei Durchläufe ausgeschöpft

1. Vollständiger DE-Lauf `2026-10-12T03-08-53_UTC__65c6ddbd__a3c3a570`:
   28/30 bestanden. LANG-02A erreicht das neue Konto in der Adminliste nicht;
   UI-01A sieht nach der mobilen Datepicker-Bedienung einen leeren Geburtstag
   statt `1990-01-01`. Beide Fälle hatten einzeln DE/EN bestanden; die Widersprüche
   werden ausdrücklich als unzuverlässiger Gesamtnachweis dokumentiert.
2. Unveränderter Neustart der Abschlussprüfung, zuerst EN:
   `2026-10-12T03-13-47_UTC__65c6ddbd__a7500da5`, 29/30 bestanden.
   Wieder LANG-02A. Danach wurde der geplante zweite DE-Lauf gemäß Stop-Regel
   nicht mehr begonnen. Kein dritter Lauf und keine weitere Code-Reparatur.

Der EN-Stack/Allure-Schritt lokalisiert LANG-02A eindeutig in
`AdminUsersPage.createActiveUser` (E-Mail-Sichtprüfung nach Speichern), bevor
die eigentliche Sprachpräferenz geprüft wird. Nicht als Sprachspeicherungsfehler
interpretieren. Viele zuvor registrierte Konten füllen das Grid; ein isolierter
Suite-Lauf mit wenigen Konten findet das neue Konto auf der ersten Seite.
**Nötige Korrektur:** nach Anlage über die vorhandene sichtbare Suche auf die eigene
Adresse filtern und den Grid-Refresh abwarten. Den tatsächlichen Konto-/Popoverzustand
erst danach prüfen; keine `.first()`-Abkürzung oder globale neueste Identität verwenden.

UI-01A benötigt eine verlässlich bestätigte Datumsauswahl im mobilen Vollbildpicker,
bevor Escape schließt. Nicht nur das Verschwinden des Overlays als Übernahmebeweis
ansehen. Originale Assertions wurden nicht abgeschwächt.

Die zuvor einzeln erfolgreich geprüften und committeten Pakete bleiben erhalten,
wie beauftragt; die später gefundenen Grenzen sind hier sichtbar. Der Branch ist
**noch nicht als insgesamt bestanden oder mergebereit bestätigt**.

Aufbau, Abbau und HTML-Erzeugung der finalen Läufe erfolgreich; eigene Container
und Volumes jeweils entfernt. Anwendung: 971 Tests und JaCoCo erfolgreich;
Helper/Reporter: 19 Tests erfolgreich. Keine Produktionsänderung, kein Merge/Push.

## Nachprüfung: Mindestdauern für Tokens behoben

Der Nutzer hat nach dem abgeschlossenen nächtlichen Auftrag einen eigenen Fix
beauftragt: mindestens eine Minute Linkgültigkeit und zwei Minuten Aufbewahrung
nach Ablauf für Verifikation und Reset. Null bedeutet nicht unbegrenzt.

Der früher durch CONFIG-02A sichtbare Syntax-only-Check war eine echte Lücke der
Adminvalidierung: Eine Null-Dauer konnte gespeichert werden, obwohl die Dienste
sie ablehnten. Die UI prüft nun dieselben Mindestgrenzen wie beide Dienste;
DE/EN-Feldfehler, Hilfstexte und Erklärungsdialoge wurden angepasst. Vorhandene
zu kurze Konfigurationswerte müssen geprüft/angepasst werden; bestehende Token-
Ablaufdaten werden nicht geändert. Die Mindestdaueränderung benötigt kein neues Schema.

Dieses Paket wurde mit höchstens zwei Umsetzungsversuchen durchgeführt:

1. Anwendungstests erfolgreich; erster DE-Browserlauf
   `2026-10-12T07-39-32_UTC__3a10c1b8__ad01c17b` scheiterte an einem
   mehrdeutigen Testlocator nach dem Erklärungsdialog. Kein neuer Anwendungsfehler:
   Der damalige Locator ordnete Eingaben über Layouttext statt ihrer Feldbeschriftung zu.
2. Nach erneuter Bestandsaufnahme wurden die tatsächlichen sichtbaren Feldlabels
   zugänglich mit den Eingaben verbunden und diese Namen im Seitenhelfer verwendet.
   Anwendung 1007/1007, Helper/Reporter 21/21, CONFIG-03A–D DE 4/4 und EN 4/4 erfolgreich.

Erfolgreiche Berichte:

- DE: `runs/2026-10-12T07-46-38_UTC__3a10c1b8__2eb51d1c/report.md`
- EN: `runs/2026-10-12T07-50-30_UTC__3a10c1b8__2ea921ba/report.md`

Die Testumgebungen wurden jeweils abgebaut. Historische Berichte bleiben erhalten.
Keine weiteren Reparaturversuche und kein Merge/Push. Adminlisten-Vorbereitung,
mobiler Datepicker, übrige Konfigurationsfelder, Admin-Testmail und weitere bisher
offene Pakete wurden durch diesen Fix nicht mit erledigt. Die Gesamtübersicht
bleibt unvollständig; kein neuer vollständiger Browserlauf aller 34 Fälle.
