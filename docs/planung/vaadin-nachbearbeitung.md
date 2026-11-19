# Offene Nachbearbeitungen

**Offen: Größen- und Typografievorschläge aus Punkten 1 und 2 gemeinsam abstimmen.**

Die Bestandsaufnahmen sind erstellt; Gestaltungsänderungen daraus benötigen die
Benutzerfreigabe. Abgenommene Nachbearbeitungen sind aus diesem Plan entfernt;
ihre gültigen Bausteine stehen in der [Theme-Anleitung](../theme.md), menschliche
Abnahmen im [manuellen Katalog](../manual-tests/README.md). Automatisierte
Browsernachtests bleiben unabhängig davon im [Testbedarf](../testbedarf.md) offen.

Die Gesamtumsetzung des [Vaadin-Komponenten-Abgleichs](vaadin-komponenten-abgleich.md)
bleibt offen; aktueller Branch ist `codex/vaadin-komponenten-folgearbeiten-5`.

## Neue Nachbearbeitung – Punkt 1: Badge-Größen analysieren und vereinheitlichen

**Bestandsaufnahme und Vorschlag freigegeben; Gestaltung anschließend abstimmen.**

- [x] Projektweit die Badge-Einsatzstellen und ihre aktuellen Höhen erfassen und
      eine verständliche Übersicht erstellen.
- [x] Einen Vorschlag für zentrale Größen-Tokens mit benannten Abstufungen
      (beispielsweise XS, S, M und L) vorlegen, nahe an den bisherigen Größen.
      Nur tatsächlich sinnvolle Größen vorsehen; weitere Abstufungen bei Bedarf begründen.
- [ ] Als projektweite Vorgabe berücksichtigen: Badges innerhalb eines fachlich
      zusammengehörigen Bereichs erhalten eine einheitliche Größe, insbesondere
      dieselbe Höhe. Unterschiedliche Beschriftungen oder Zustandsfarben allein
      begründen keine Größenunterschiede. Die vier Badges im Benachrichtigungs-Overlay
      sind ein Beispiel für diese allgemeine Regel.
- [ ] Bewusste Hervorhebungen dürfen eine größere Größenstufe verwenden, etwa die
      Limitangabe bei „Geräte und Sitzungen“. Größen nach fachlicher Funktion und
      Gewichtung zuordnen; gleichartige Badges im jeweiligen Themenbereich gleich gestalten.
- [ ] Vorschlag gemeinsam abstimmen; die Umsetzung erst nach Freigabe beginnen.

### Ergebnis und Vorschlag zu Punkt 1

Bestandsaufnahme anhand des aktuellen Java-/CSS-Codes und der installierten
Vaadin-Version 25.3.5. 22 Java-Dateien verwenden native Badges, darunter zwei
Badge-Unterklassen. Die folgende Tabelle gruppiert deren Einsatzstellen nach
Gestaltungsregeln. Keine Browsermessung: feste Höhen sind CSS-Vorgaben; errechnete
Werte setzen die vorhandenen Schrift-/Zeilenhöhen voraus. Native Höhen sind
gerundet und können bei abweichender Vererbung variieren.

Der native Badge setzt bereits eine Höhe aus Zeilenhöhe plus Rahmen, gerundet
auf 0,125 rem, sowie negative Rahmen-Außenabstände. Lokale Regeln überschreiben
teils nur Padding oder Schrift, teils zusätzlich Höhe und Boxmodell. Daher darf
man nicht generell Zeilenhöhe plus Padding als tatsächliche Außenhöhe angeben.

| Einsatzstellen / Klassen | Aktuelle Höhenregel | Zuständige CSS-Datei |
| --- | --- | --- |
| Quellen „Administration“ und „System“, Vorschau/Inbox/Detail/Entwurf: `cosmos-notification-preview-source` innerhalb `message-meta` | `height:auto`, mindestens 20; bei 16 px Zeilenhöhe + 2 × 2 px Padding + Rahmen rechnerisch 22 px | `cosmos-notifications.css` |
| „Ungelesen“ und „Gelesen“: `cosmos-notification-preview-state[data-read]` | Fest 19 px, Border-Box | `cosmos-notifications.css` |
| Ticketbezogene Relevanz „Bereits übernommen“ usw.: `preview-state[data-relevance]` | Content-Box, 2 px Padding je Seite; native Höhe bei 16 px Zeilenhöhe ca. 18 px, mit Padding/Rahmen außen ca. 24 px | `cosmos-notifications.css` |
| Notification-Filterzähler: `portal-filter-count` | Fest 18 px | `cosmos-notifications.css` |
| Glocke, Kontonavigation und Startseiten-Zähler: `cosmos-notification-count`, `account-nav-count`, `portal-home-unread` | Fest 20 px | `cosmos-shell.css`, Grundregel auch `cosmos-notifications.css` |
| Anwesenheit in Kontokarte und Benutzermenü: `account-summary-presence`, `portal-menu-presence` | Mindestens 21 px; 16 px Zeilenhöhe, 2 px Padding je Seite, native Höhe zusätzlich aktiv | `cosmos-shell.css` |
| Verifikation, Reaktivierung, Portalpasswort, 2FA-Einrichtung und verbundene Anmeldeanbieter | 16 px Zeilenhöhe und 2 px Padding je Seite, native Höhenregel nicht zurückgesetzt (typisch ca. 18 px statt automatisch 22 px) | `cosmos-account.css`, `cosmos-mfa.css`, `cosmos-external-login.css` |
| Sitzungskennzeichnungen: aktuelles Gerät, aktive/gespeicherte Anmeldung | Gleiche kompakte Schrift-/Padding-Regeln und native Höhe wie oben | `cosmos-account.css` |
| Sitzungslimit `account-sessions-limit` | `height:auto`, 16 + 8 + 2 = rechnerisch 26 px | `cosmos-account.css` |
| Sitzungsübersicht `account-session-count` | `height:auto`, 20 + 8 + 2 = mindestens rechnerisch 30 px; größere eingebettete Ziffern möglich | `cosmos-account.css` |
| Benutzerdialog: Registrierung über, Kontotyp, Anmeldemethoden | 16 px Zeilenhöhe, 2 px Padding je Seite, native Höhe aktiv | `admin-users.css` |
| Benutzertabelle: „Sie“ / Rollen | „Sie“ rechnerisch 22 px; Rollen rechnerisch 24 px, beide `height:auto` | `admin-users.css` |
| Benutzertabelle: Zugang / Anwesenheit | Native Badges als rahmenlose Text-/Iconanzeige, `height:auto`, 18 px Zeilenhöhe; keine gerahmten Statuschips | `admin-users.css` |
| Konfigurationsanzahl, Versandstatus und Versandhistorien-Zähler: `cosmos-config-setting-count`, `cosmos-panel-meta` | `height:auto`, 16 + 12 + 2 = rechnerisch 30 px | `cosmos-admin.css` |
| Filterzähler im Benutzer-Filterdialog | Fest 36 px; andere Filterkontexte haben eigene Regeln bzw. native Höhe | `admin-users.css`, `cosmos-admin.css` |
| Anmeldeanbieter-Konfiguration: `provider-status` mit Logo | Fest 30 px | `cosmos-provider-settings.css` |
| Bereinigungsstatus `admin-mail-queue-status` | 16 px Zeilenhöhe, 2 px Padding je Seite, native Höhe aktiv | `cosmos-mailqueue-assistant.css` |
| Ticketstatus in Liste, Kopf und Verlauf; „Sie“; Abschnittszähler; interner Vermerk | Meist 16 px Zeilenhöhe, 2 px Padding je Seite und native Höhe; allgemeine Ticketstatus-Regel hat 10 px Padding, das konkrete Kontexte wieder reduzieren | `cosmos-mfa-confirmation.css` |
| Ticket-Ungelesenzähler | Mindestens 22 px breit, keine feste lokale Höhe; native Rundung bei Zeilenhöhe 1 | `cosmos-mfa.css` |
| Ticket-Aktivität in Navigation | 8 px hoher Punkt, kein beschriftetes Badge | `cosmos-admin.css` |
| Nummerierte Anleitungsschritte | Fest 20 × 20 px | `cosmos-mfa-confirmation.css` |
| Mitteilungsentwurf und Versandarchiv: `cosmos-notification-compose-status`, `cosmos-notification-sent-status` | Native Höhe; Entwurfsstatus hat 11 px Schrift / 1,5 Zeilenhöhe und 14-px-Icon, Archivstatus zusätzlich vererbte Typografie | `cosmos-notifications.css` |

**Vorschlag, noch nicht umgesetzt:** vier Außenhöhen mit `box-sizing:border-box`.

| Geplantes Token | Höhe | Vorgeschlagener Einsatz |
| --- | --- | --- |
| `--cosmos-badge-height-xs` | 20 px | Kompakte Zähler, nummerierte Schritte |
| `--cosmos-badge-height-s` | 22 px | Normale Quellen-/Statusbadges; alle vier Kennzeichnungen der Benachrichtigungen ebenso wie gleichartige Statusgruppen |
| `--cosmos-badge-height-m` | 24 px | Rollen bzw. bewusst etwas größere Kennzeichnungen, wenn als Gruppe sinnvoll |
| `--cosmos-badge-height-l` | 30 px | Hervorgehobene Kopf-/Limitangaben, Konfigurationsanzahl, Anbieterstatus |

Schriftgröße und Zeilenhöhe aus den vorhandenen Typografie-Tokens beziehen;
ergänzende Padding-/Radius-Zuordnung zentral halten. Farben bleiben fachlich.
Die native Höhenformel für diese Größen gezielt ersetzen, statt pro Status
unterschiedliche Boxmodelle zu verwenden. Kein XL/XXL ohne tatsächlichen Bedarf:
der 36-px-Filterzähler ist an die Werkzeugzeile gekoppelt und kann vorerst eine
begründete Control-Ausnahme bleiben. Der 8-px-Aktivitätspunkt bleibt separat.
Die beiden zusammengehörigen Sitzungslimit-/Anzahl-Anzeigen wären ein Kandidat
für gemeinsam L; die Entscheidung erfolgt bei der Abstimmung.

## Neue Nachbearbeitung – Punkt 2: Eingaben, Buttons und Formularschrift erfassen

**Bestandsaufnahme und Vorschlag freigegeben; Änderungen anschließend abstimmen.**

- [x] Alle verwendeten Eingabekomponenten erfassen, nicht nur Textfelder:
      unter anderem TextAreas, Passwort-, Zahlen-, Datums- und Dauerfelder,
      ComboBox/Select, Mehrfachauswahl, Checkboxen, Switches und weitere vorhandene Typen.
- [x] Je Variante die aktuelle Höhe, Einsatzstellen und maßgeblichen Größenregeln
      auflisten. Bei mehrzeiligen oder zusammengesetzten Komponenten die eigentliche
      Eingabefläche von Beschriftung, Hilfetext und Gesamthöhe unterscheiden.
- [x] Alle gerahmten Buttons einschließlich reiner Icon-Buttons ebenso erfassen:
      Höhe, Einsatzstellen und Größenregeln gegenüberstellen.
- [x] Prüfen, ob gleichartige Komponenten innerhalb zusammengehöriger Formularbereiche
      einheitlich dimensioniert sind. Bewusste funktionale Größenunterschiede von
      unbeabsichtigten Abweichungen unterscheiden.
- [x] Die Schriftgestaltung in Formularen und vergleichbaren Bedienbereichen mit
      den vorhandenen Typografie-Tokens abgleichen und Abweichungen ausweisen,
      insbesondere bei Schriftgröße, Schriftgewicht und Zeilenhöhe.
      Individuelle Gestaltung auf der Startseite gehört nicht zu dieser Prüfung.
- [x] Eine verständliche Übersicht und einen Vorschlag zur Vereinheitlichung anhand
      vorhandener Tokens vorlegen; nötige Ergänzungen gesondert begründen.
      Änderungen erst nach gemeinsamer Abstimmung und Freigabe umsetzen.

### Ergebnis und Vorschlag zu Punkt 2

Quellprüfung der verwendeten Komponenten und Theme-Regeln, keine Browsermessung.
Die Höhen beziehen sich auf die Bedienfläche ohne Label/Hilfetext; Gesamthöhen
mehrzeiliger Komponenten hängen zusätzlich vom Inhalt ab. Die individuelle
Startseitengestaltung wurde nicht als Abweichung bewertet.

| Eingaben / Einsatz | Aktuelle Höhe / Besonderheit | CSS-Quelle |
| --- | --- | --- |
| TextField, EmailField, PasswordField: öffentliche Formulare, Konto und Referenzdialoge | Meist 40 px; gemeinsamer älterer Basiswert nur mindestens 36 px | `cosmos-theme.css`, `admin-users.css` |
| Konfigurationsfelder und Mail-Test | Einzeilig 40 px | `cosmos-admin.css` |
| ComboBox, DatePicker, Select | Meist 40 px in Formularen; Datumskalender-Tage 36 px sind ein anderer Baustein | `cosmos-theme.css`, `cosmos-mfa.css`, `cosmos-aura.css` |
| TimePicker | Gemeinsame Mindesthöhe 36 px, Filterdialog überschreibt auf 40 px; nicht überall dieselbe explizite Referenzregel | `cosmos-theme.css`, `admin-users.css` |
| IntegerField / vorhandene Zahlenfelder | Konfiguration mindestens 40 px; gemeinsame Basis mindestens 36 px | `cosmos-admin.css`, `cosmos-theme.css` |
| CosmosDurationField mit IntegerField/BigDecimalField und Einheit | Zahlenteil 40 px; Einheit integriert, Aktionen 28 px; äußeres CustomField ergänzt keine zweite Eingabefläche | `cosmos-duration-field.css` |
| MultiSelectComboBox: Rollen und Empfänger | Mindestens 40 px; Rollenfeld im Benutzerdialog zusätzlich feste Eingabefläche 40 px, ausgewählte Chips/Empfängerzusammenfassung gesondert | `admin-users.css`, `cosmos-admin.css` |
| TextArea: Mailnachricht | Mindestens 144 px Eingabefläche | `cosmos-admin.css` |
| TextArea: Mitteilungsentwurf / Ticketnachricht | Mindestens 122 px Eingabefläche; Ticket-Host mindestens 144 px inklusive Umgebung | `cosmos-admin.css`, `cosmos-mfa.css` |
| Checkbox / CheckboxGroup | Kästchen meist 16 px; spezifischere Benutzerdialog-Regel 17 px. Gruppenhöhe folgt Anzahl/Layout; Filterauswahl hat 38-px-Zeilen | `cosmos-public.css`, `cosmos-admin.css`, `admin-users.css` |
| RadioButtonGroup | Native Radio-Geometrie und Anzahl/Layout; Filterlabel separat gestaltet, keine projektweit festgelegte Gruppenhöhe | `admin-users.css`, Aura/native Radio-Komponente |
| Switch | Zentral 34 × 20 px, Griff 14 × 14 px. Konfiguration reserviert mindestens 40 px Zeilenhöhe, andere Labels/Untertexte bestimmen ihre Zeilenhöhe | `cosmos-theme.css`, `cosmos-admin.css` |
| OTP-Feld | 44 px pro Ziffernfläche, Breite responsiv; kein normales Textfeld | `cosmos-mfa.css` |
| Seitengrößenauswahl in Pagination | 32 px, bewusst kompakter als Formulareingaben | `cosmos-theme.css`, `admin-users.css`, `cosmos-notifications.css` |

| Gerahmte Buttons / Einsatz | Aktuelle Höhe | CSS-Quelle |
| --- | --- | --- |
| Normale Formular-/Dialogaktionen und Schließen-Icon | 36 px; Textbuttons bei langen mobilen Beschriftungen teils automatische Höhe ab Mindesthöhe 36 px | `cosmos-theme.css`, `cosmos-notifications.css`, `cosmos-mfa.css` |
| Benutzer-Tabellenaktionen | 32 px (lokale Regel), Mindesthöhe 30 px | `admin-users.css` |
| Pagination / Seitenzahlen | 28 px | `cosmos-admin.css`, `cosmos-notifications.css` |
| Werkzeuge in der Benachrichtigungsvorschau | 32 × 32 px; einzelne Zeilenaktionen erben sonst Basisgrößen | `cosmos-notifications.css` |
| Such-/Sprungaktion im Ticketfilter | 40 px, passend zum Eingabefeld | `cosmos-mfa.css` |
| Feldhilfe-Konfiguration | Basis 44 px, Konfigurations-/Mailbereiche überschreiben auf 36 px; daneben 40-px-Felder | `cosmos-admin.css` |
| Avatar-Zuschneideaktionen | Automatische Höhe, mindestens 36 px | `cosmos-account.css` |
| Kleine Sprachauswahl im Kontomenü | 24 px; bewusst kompakter Umschalter | `cosmos-shell.css` |

**Typografie-Befunde:**

- Formularlabel überwiegend 12 px / Gewicht 500, Hilfen 11 px; diese Größen sind
  bereits über Tokens abgebildet. Native Basis-Eingaben verwenden 13 px / Gewicht
  700, die Referenzregeln setzen überwiegend 14 px / 400. Der Benutzerdialog setzt
  Eingabetext wiederum auf 13 px / 400. Das ist eine tatsächliche bereichsabhängige
  Abweichung; den gemeinsamen Zielwert vor einer Änderung abstimmen.
- Dauerwerte verwenden 14 px, ihr Placeholder ausdrücklich 13 px. Diese bereits
  gewünschte Unterscheidung erhalten; sie erklärt nicht allein den Positionssprung.
- Referenzbuttons verwenden überwiegend 12 px / 600, die allgemeine Basis nennt
  13 px für normale Buttons bzw. 14 px für andere Varianten. Auch hier überlagern
  sich Basis und Referenzgestaltung.
- Direkte, aber bereits tokenfähige Werte stehen unter anderem in
  `cosmos-theme.css` (14 px / 400), `cosmos-admin.css` (14 px / 400 sowie Switchlabel
  12 px / 500), `admin-users.css` (Switchlabel 12 px / 400, einzelne Gewichte 600)
  und `cosmos-duration-field.css` (14/13 px / 400). OTP verwendet direkt 18 px.
  Numerische Gewichte innerhalb von `font:`-Kurzformen sind ebenfalls verbreitet.
  Diese Werte entsprechen bestehenden Tokens, umgehen aber deren zentrale Pflege.

**Vorschlag, noch nicht umgesetzt:**

1. Einzeilige Formulareingaben gemeinsam mit einem semantischen 40-px-Token
   versehen; vorhandene 36-px-Buttons nicht ohne gestalterischen Grund vergrößern.
2. Buttonhöhen 28/32/36/40 px als benannte Größen zentral zuordnen. Den bereits
   vorhandenen 28-px-Pagination- und 36-px-Controlwert wiederverwenden; 24-px-
   Sprachumschalter und OTP bewusst getrennt halten.
3. Checkbox-Kästchen einheitlich 16 px, Switch-Grundmaße wie bisher. Mehrzeilige
   Beschriftungen und Gruppen nicht künstlich auf die Höhe eines Textfeldes zwingen.
4. TextAreas nach Aufgabe mehrzeilig lassen; unterschiedliche Mindesthöhen 122/144
   ausdrücklich zuordnen. Feldhilfe-Aktionen neben Eingaben als gemeinsame Zeile
   betrachten und den gewünschten 36-/40-px-Abgleich gemeinsam sichten.
5. Bestehende Schrift-Tokens konsequent referenzieren. Den Zielwert für Eingabetext
   (13 oder 14 px) und normale Buttonschrift (12 oder 13 px) gemeinsam entscheiden;
   keinen globalen Schriftwechsel aus dieser Bestandsaufnahme ableiten.

- [ ] Vorschläge aus Punkt 2 gemeinsam abstimmen und konkrete Umstellungen freigeben.
