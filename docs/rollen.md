# Rollen und zusätzliche Berechtigungen

Bei der administrativen Kontoanlage darf ausschließlich ein Superadmin ein
**Entwicklungskonto** anlegen. Es sendet keine Kontomails, besitzt aber dieselben
Rollen- und 2FA-Prüfungen wie ein reguläres Konto. Das Kennzeichen wird nach der
Anlage nicht über die Benutzerbearbeitung geändert. Die separate Ausnahme von
der E-Mail-Verifikation für reguläre Konten bleibt ebenfalls Superadmins vorbehalten.

Diese Übersicht beschreibt die aktuell vorhandenen Rechte von oben nach unten.
Die gemeinsamen Kontofunktionen stehen einmal im Abschnitt
[Angemeldete Kontoinhaber](#angemeldete-kontoinhaber). Die Rollenabschnitte nennen
die darüber hinausgehenden Rechte und ihre Grenzen. Noch nicht umgesetzte
Entscheidungen sind am Ende ausdrücklich als geplant gekennzeichnet.

## Superadmin · SUPERADMIN

Der Superadmin erhält zusätzlich zu den Adminrechten folgende Befugnisse:

- Superadmin-Konten anlegen, bearbeiten, sperren und löschen sowie die
  Superadmin-Rolle vergeben oder entziehen. Die Schutzregel für den letzten
  aktiven Superadmin gilt weiterhin.
- Bei der administrativen Kontoanlage die standardmäßige E-Mail-Verifikation
  ausdrücklich abwählen. Das Konto wird dann ohne E-Mail-Nachweis bereitgestellt
  und als „Nicht geprüft“ gekennzeichnet. Das ist keine bestätigte E-Mail-Adresse.
- Google und Microsoft unter „Konfiguration → Anmeldeanbieter“ konfigurieren
  und freigeben, einschließlich der Anbieterzugangsdaten.
- Rollenbezogene Zwei-Faktor-Regeln und die gemeinsamen Einstellungen dieser
  2FA-Konfiguration ändern. Dazu gehören auch ausdrücklich vorgesehene
  Entwicklungsoptionen; die normalen Identitätsnachweise werden dadurch nicht
  stillschweigend ersetzt.
- Interne Mitteilungen an ausgewählte Konten oder alle berechtigten Empfänger
  versenden und den dazugehörigen administrativen Versandbereich verwenden.
- Im Ticketsystem zusätzlich fremde noch nicht abgeschlossene Vorgänge
  beobachten. Normale Admins können nur ihre eigenen zugewiesenen Vorgänge
  beobachten. Beobachtung überträgt nicht die Bearbeitungszuständigkeit.

Auch ein Superadmin kann nicht das letzte aktive Superadmin-Konto löschen,
deaktivieren oder ihm die letzte erforderliche Berechtigung entziehen. Er erhält
keine Möglichkeit, über ein Wiederherstellungsticket den Authenticator eines
anderen Kontos zurückzusetzen oder dessen Zugang ohne gültigen Nachweis freizugeben.

Details: [Benutzerverwaltung](anwendung.md#administration),
[Anmeldeanbieter](externe-anmeldung.md),
[2FA](zwei-faktor-authentifizierung.md) und
[Ticketzustände und Beobachtung](zwei-faktor-authentifizierung.md#ticketzustände-zähler-und-glocke).

## Admin · ADMIN

Zusätzlich zu den gemeinsamen Kontofunktionen darf ein Admin:

- Die Administration öffnen und Konten ohne Superadmin-Rolle verwalten:
  anlegen, bearbeiten, aktivieren, deaktivieren und löschen. Dazu gehören auch
  andere Admin-Konten. Rollen ohne Superadmin-Berechtigung können im Rahmen
  dieser Verwaltung zugewiesen werden.
- Benutzerdetails einschließlich Registrierungsweg, verfügbaren Anmeldewegen
  und E-Mail-Nachweis einsehen. Verifikations- und Passwortresetmails für dafür
  geeignete Konten an die gespeicherte Adresse anfordern. Berechtigungs-,
  Nachweis- und Versandgrenzen bleiben wirksam.
- Die allgemeinen Konfigurationsbereiche verwenden: Allgemein, Sicherheit und
  Sitzungen, Tokens, Benutzerstatus, Benachrichtigungen, E-Mail-Absender, SMTP
  und Mailqueue. Die gesonderten Superadmin-Bereiche sind davon ausgenommen.
- Den Mailtest verwenden, um die konfigurierte SMTP-Verbindung zu prüfen.
- Wiederherstellungsanfragen in der administrativen Ticketansicht einsehen,
  offene Vorgänge übernehmen und im Rahmen der Zuständigkeit bearbeiten:
  öffentlich antworten, interne Vermerke verfassen, geprüfte Anleitungen senden
  und Vorgänge abschließen. Eigene zugewiesene Vorgänge können beobachtet werden.
- Während des Wartungsmodus weiterhin anmelden und die Freigabe verwalten.
  Die übrigen Sicherheitsprüfungen bleiben erforderlich.

Ein Admin darf keine Superadmin-Konten verwalten und sich oder anderen keine
Superadmin-Rechte verschaffen. Er darf nicht die Anbieterkonfiguration, die
rollenbezogenen 2FA-Regeln oder den internen Mitteilungsversand des Superadmins
übernehmen. Auch bei sichtbaren oder selbst zusammengestellten Aufrufen gelten
die fachlichen Berechtigungsprüfungen.

**Aktueller Sonderfall:** Die Aufbewahrungs- und Erinnerungsfristen für selbst
deaktivierte Konten stehen noch unter „Sicherheit & Sitzungen“ und sind damit
auch für Admins zugänglich. Die beschlossene Einschränkung steht unter
[Geplante Änderung](#geplante-änderung-kontoverwaltung).

Details zu Bedienung und Grenzen: [Administration](anwendung.md#administration),
[Mailqueue](mailqueue.md) und [Ticketregeln](zwei-faktor-authentifizierung.md#ticketzustände-zähler-und-glocke).

## Benutzer · USER

USER ist die reguläre Rolle für selbst registrierte Konten, auch bei neuer
Registrierung über Google oder Microsoft. Sie hat die gemeinsamen Kontofunktionen,
aber keine zusätzlichen administrativen Befugnisse.

Die Rolle erlaubt weder Zugriff auf fremde Konten noch auf Administration,
anwendungsweite Konfiguration oder fremde Wiederherstellungsanfragen.

## Gast · GUEST

GUEST ist eine zuweisbare Rolle eines angemeldeten Kontos. Sie bezeichnet keinen
anonymen Besucher. Aktuell gibt es keinen eigenen reduzierten Nur-Lesen-Umfang
für Gastkonten; sie verwenden dieselben gemeinsamen Kontofunktionen wie USER.

Die Rolle kann bei den 2FA-Regeln gesondert behandelt werden. Daraus folgt keine
administrative Berechtigung. Ein reines Gastkonto erhält wie ein reines
Benutzerkonto keinen Zugriff auf Administration oder fremde Kontodaten.

Die Rolle war ursprünglich für einen möglichen Zugang „Als Gast anmelden“
gedacht. Ein solcher eigener Anmeldeablauf ist derzeit nicht umgesetzt und hat
noch keinen konkreten Anwendungsfall. GUEST bleibt vorerst bestehen; daraus
entsteht kein Auftrag für neue Gastfunktionen. Bei späteren Erweiterungen soll
der Nutzen gegen zusätzliche Komplexität bei Rechten, 2FA, Oberfläche und Tests
abgewogen werden. Die offene Entscheidung steht im [TODO](../TODO.md).

## Angemeldete Kontoinhaber

Diese Funktionen gelten für alle vier Rollen, soweit Konto- und Sicherheitszustand
die jeweilige Aktion erlauben:

- Das eigene Portal und die persönlichen Benachrichtigungen öffnen sowie eigene
  Nachrichten lesen und verwalten.
- Eigene persönliche Daten, Profilbild und Sprache pflegen.
- Eigene Zugangsdaten und Anmeldewege verwalten: Portalpasswort und E-Mail ändern
  sowie Google oder Microsoft verbinden oder trennen, soweit die konfigurierten
  Anbieter und erforderlichen Identitätsnachweise dies erlauben.
- Den eigenen Authenticator und Ersatzcodes über die vorgesehenen
  Einrichtungs-, Wechsel- und Wiederherstellungsabläufe verwalten.
- Eigene Geräte, aktive Sitzungen und gespeicherte Anmeldungen einsehen sowie
  andere eigene Sitzungen abmelden oder gespeicherte Anmeldungen widerrufen.
- Das eigene Konto nach Identitätsbestätigung deaktivieren oder endgültig
  löschen. Ein selbst deaktiviertes Konto kann innerhalb seiner Frist über den
  gültigen Link reaktiviert werden. Administrative Sperren eröffnen diesen Weg
  nicht. Der letzte aktive Superadmin ist von gefährdenden Selbstaktionen ausgeschlossen.
- Bei Zugangsproblemen die eigenen Wiederherstellungsanfragen über den
  vorgesehenen Hilfsablauf anlegen, lesen, beantworten und schließen. Das ist
  derzeit kein allgemeines Supportformular für beliebige Anliegen.

Ein erster erfolgreicher Anmeldenachweis allein gibt diese Funktionen noch
nicht vollständig frei: Eine ausstehende E-Mail-Bestätigung, erforderliche 2FA
oder ein gesperrter Sicherheitsablauf kann den Zugang auf den jeweiligen Schritt
beschränken. Der Hilfeweg kann deshalb erreichbar sein, während das normale
Portal noch gesperrt ist. Funktionen mit zusätzlichem Nachweis verlangen diesen
auch nach einer bereits erfolgreichen Anmeldung.

Details: [Mein Konto](anwendung.md#mein-konto),
[Kontodeaktivierung und Löschung](sicherheit.md#eigenes-konto-deaktivieren-und-löschen)
und [Zwei-Faktor-Authentifizierung](zwei-faktor-authentifizierung.md).

## Besucher ohne Anmeldung

Ein Besucher hat keine der vier Kontorollen. Er kann die öffentlichen Seiten,
Anmeldung, Registrierung und die vorgesehenen Linkabläufe für Verifikation,
Passwortreset, E-Mail-Änderung und Reaktivierung erreichen. Der Aufruf einer
solchen Seite ist kein Kontozugriff; die jeweilige Aktion benötigt weiterhin
einen gültigen Nachweis oder Token. Im Wartungsmodus gelten zusätzliche Grenzen.

## Rollenvergabe und gemeinsame Schutzregeln

Konten können mehrere ausdrücklich zugewiesene Rollen besitzen. Technisch erbt
nur SUPERADMIN zusätzlich ADMIN. ADMIN, USER und GUEST bilden keine automatisch
vererbte Kette. Die gemeinsamen Kontofunktionen setzen eine geeignete Anmeldung
voraus und verlangen nicht, dass jedem Admin zusätzlich USER zugewiesen wird.
Eine Rolle bei einem Konto zu speichern ist von ihren wirksamen Berechtigungen
zu unterscheiden; die Benutzerverwaltung zeigt die ausdrücklich vergebenen Rollen.

Serverseitige Prüfungen entscheiden über Berechtigungen, nicht allein die
Sichtbarkeit eines Buttons. Sperren, Löschung, Rollenänderungen und widerrufene
Sitzungen müssen auch bei bereits geöffneten Ansichten berücksichtigt werden.
Die Anmeldung über Google, Microsoft oder Portalpasswort verleiht keine höheren
Rollen. „Verifiziert“, „Nicht geprüft“ und der Registrierungsweg sind ebenfalls
keine administrativen Rollen. Ein Entwicklungskonto ist als separates Merkmal
geplant und soll keine zusätzlichen Rechte verleihen.

Die 2FA-Regeln können sich nach Rolle und Aktion unterscheiden. Mehrere Rollen
sind deshalb kein Grund, einfach die vermeintlich schwächste Sicherheitsregel
anzuwenden. Die verbindlichen Regeln und Ausnahmen stehen in der
[Sicherheitsdokumentation](sicherheit.md) und der [2FA-Dokumentation](zwei-faktor-authentifizierung.md).

## Geplante Änderung: Kontoverwaltung

Freigegeben, **noch nicht umgesetzt**: Die beiden globalen Fristen für
Aufbewahrung nach Selbstdeaktivierung und Löscherinnerung ziehen in die neue
Konfigurationsrubrik „Kontoverwaltung“. Nur Superadmins dürfen diese Rubrik
sehen und ihre Werte ändern. Admins und normale Benutzer dürfen dies auch
nicht durch direkte URLs oder Dienstaufrufe umgehen.

Die Verwaltung einzelner Konten durch berechtigte Admins und die Verwaltung des
eigenen Kontos bleiben davon getrennt und behalten ihre bisherigen Rechte.
Nach Umsetzung diesen Abschnitt in die geltenden Rollenrechte übernehmen und
den bisherigen Sonderfall bei ADMIN entfernen.
Arbeitsauftrag: [Nachtaufgabe 10](planung/nachtaufgaben.md#10-konfigurationsfelder-für-deaktivierungsfristen-vereinheitlichen).
