# Zwei-Faktor-Authentifizierung und externe Kontoaktionen

## Auftrag und Abgrenzung

Nächste separate Umsetzung nach der abgeschlossenen Google-/Microsoft-Anmeldung.
Branch: `codex/zwei-faktor-authentifizierung`, Basis:
`9be7038f3d0a3bd986d3733777a22d216650fa3d` auf lokalem `main`.
Die besprochenen Grundsatzentscheidungen sind abgestimmt. Bisher sind ausschließlich
Planungsdokumente geändert. Die Implementierung hat noch nicht begonnen.

Vereinbart sind TOTP-Authenticator, Ersatzcodes und abgesicherte Administratorhilfe.
Wann und für wen 2FA erforderlich ist, soll konfigurierbar sein. Eine eigene Seite
unter Administration → Konfiguration darf ausschließlich von Superadmins gelesen
und verwaltet werden. Normale Administratoren erhalten darauf keinen Zugriff.

Für Entwicklungszwecke sollen Anforderungen abschaltbar sein, ohne Faktoren zu
löschen. Der bisherige Google-Weg bleibt als ausdrücklich aktivierbare, gewarnte
Entwicklungs-Ausnahme erhalten. Die Google-/Microsoft-Anbieterverwaltung wird im
selben Umfang auf Superadmins beschränkt, siehe Paket P6.

## Problem und Ziel

Der erneute Google-OIDC-Durchlauf bestätigt das bereits verbundene Konto, erzwingt
aber keine neue aktive Google-Anmeldung. Eine offene Portal- und Google-Sitzung
kann daher zur Bestätigung sensibler Änderungen genügen. Das ist eine bekannte,
vorübergehend akzeptierte Sicherheitsschwäche, kein gleichwertiger MFA-Nachweis.
Aktueller Vertrag und Anbietergrenze: [Externe Anmeldung](../externe-anmeldung.md#google-zwischenlösung-und-bekannte-sicherheitsschwäche).

Wenn die geltende Regel 2FA verlangt, darf ein erneuter Google-Durchlauf oder das
Portalpasswort allein diese Anforderung nicht umgehen. Das gilt auch bei
deaktivierten Anbietern oder einem anderen Zugang zum selben Konto.

Im sicheren Betrieb darf die Google-Zwischenlösung allein weder eine vorgeschriebene
2FA noch deren geschützte Einrichtung oder Wiederherstellung ersetzen. Die normale
Google-Registrierung ist dadurch nicht pauschal fehlerhaft. Zu prüfen ist die gesamte
Kette vom neuen Konto bis zur späteren sensiblen Kontoaktion.

Die gewünschte Entwicklungs-Ausnahme bleibt ein bewusst akzeptiertes Restrisiko.
Sie ist standardmäßig aus und separat von der normalen 2FA-Konfiguration freizugeben.
Das Abschalten einer 2FA-Regel erlaubt nicht automatisch den schwachen Google-Weg.
Eine Warnung ist kein Sicherheitsnachweis. Bleibt die Ausnahme außerhalb klar
abgegrenzter Testbedingungen erreichbar, ist der Fehler nicht vollständig geschlossen.

## Abgestimmtes Konfigurationsmodell

| Bereich | Geplante Einstellung |
| --- | --- |
| Benutzer | Regeln für Superadmin, Administrator, Benutzer und Gast |
| Normale Anmeldung | 2FA nach dem ersten Nachweis, unabhängig von Portalpasswort, Google oder Microsoft |
| Neue Sitzung | Verhalten nach Ablauf, erneuter Anmeldung und dauerhafter Anmeldung |
| Administrative Bereiche | Zusätzliche Bestätigung beim Zugriff, soweit konfiguriert |
| Passwort | Portalpasswort einrichten und ändern |
| E-Mail-Adresse | Änderung der Portaladresse |
| Anmeldeanbieter | Google/Microsoft verbinden und trennen |
| Wiederholung | Jede betroffene Aktion oder begrenzte Nachweisfrist |
| Wiederherstellung | Ersatzcodes und abgesicherte Hilfe, gegebenenfalls rollenabhängig |
| Entwicklung | Anforderungen abschalten, eingerichteten Faktor erhalten, Warnung anzeigen |
| Google-Ausnahme | Schwachen erneuten Google-Durchlauf ausdrücklich und separat zulassen |

Vorgesehene Auswahl je Regel: **Aus**, **bei eingerichteter 2FA**, **Pflicht**. Bei Pflicht
ohne Faktor nur den abgesicherten Einrichtungsweg zulassen, keinen vollständigen
Portalzugriff. Die Oberfläche unterscheidet eingerichtete Faktoren von der effektiv
geltenden Anforderung.

Abgestimmte Voreinstellungen:

| Rollen | Anmeldung und sensible Kontoaktionen |
| --- | --- |
| Superadmin und Administrator | 2FA verpflichtend |
| Benutzer und Gast mit Konto | Einrichtung freiwillig. Nach Einrichtung ist 2FA erforderlich. |
| Mehrere Rollen | Die strengste passende Regel gilt. |

Beim bloßen Navigieren im Adminbereich keine zusätzliche ständige Codeabfrage
voreinstellen. Alle Rollen-/Funktionsregeln bleiben durch den Superadmin änderbar.
Das schließt Administratoren und Superadmins ausdrücklich ein: Auch für diese
Rollen kann er die Pflicht auf freiwillige Einrichtung oder Aus umstellen.
Es gibt keine unveränderliche rollenbedingte 2FA-Pflicht. Die Berechtigung zum
Verwalten der Konfiguration bleibt ausschließlich bei Superadmins, unabhängig
von der eingestellten 2FA-Anforderung. Bei mehreren Rollen gilt weiterhin die
strengste tatsächlich konfigurierte Regel, keine fest eingebaute Adminregel.
Insbesondere kann er später auch für normale Benutzer von freiwilliger Einrichtung
auf Pflicht umstellen. Diese Änderung gilt für neue und bestehende Konten.
Ohne Faktor wird ab dem nächsten geschützten Zugriff die gesicherte Einrichtung
verlangt. Mit Faktor, aber ohne gültigen Nachweis, wird die Bestätigung verlangt.
Eine bereits offene Sitzung oder dauerhafte Anmeldung darf die neue Pflicht nicht
umgehen. Bis zur Erfüllung bleibt der Zugriff auf die notwendigen Einrichtungs-,
Bestätigungs- und Hilfswege beschränkt. Keine Übergangsfrist voreinstellen.
Die zusätzliche Inaktivitätsregel ist zunächst deaktiviert, siehe Sitzungsverhalten.

### Abgestimmtes Verhalten der laufenden Sitzung

Wenn die Anmelderegel 2FA verlangt, wird sie beim Aufbau des vollständigen
Anmeldezugangs bestätigt. Solange diese Sitzung gültig ist und eine gegebenenfalls
aktivierte Inaktivitätsregel erfüllt ist, keine wiederholte Codeabfrage bei normaler
Benutzung oder Navigation verlangen.
Ein Seiten-Refresh oder neuer Tab mit derselben gültigen Sitzung ist für sich
allein keine neue Anmeldung.

Nach Abmelden und erneuter Anmeldung sowie nach Ablauf der Sitzung wird 2FA gemäß
aktueller Regel erneut benötigt. Eine dauerhafte Anmeldung darf dabei den ersten
Anmeldeschritt erleichtern, aber keine neue 2FA-Bestätigung vortäuschen.

Die zusätzliche 2FA-Abfrage nach Inaktivität ist als Anfangswert **deaktiviert**.
Der Superadmin kann sie später einschalten und einen Zeitraum festlegen.
Die bestehenden Anmelde-, Sitzungsablauf- und Aktionsregeln werden durch diese
deaktivierte Zusatzregel nicht abgeschaltet.

Für die Eingabe die vorhandene Zahl-/Einheiten-Darstellung der Konfiguration
wiederverwenden. Empfehlung: Minuten, Stunden und Tage anbieten, Stunden als
anfängliche Anzeigeeinheit. So sind kurze Testintervalle und längere Zeiträume
ohne Sekundenrechnung eingebbar. Aktivierung durch einen separaten Switch steuern,
nicht durch einen mehrdeutigen Zahlenwert 0. Bei ausgeschaltetem Switch ist das
Dauerfeld deaktiviert. Einen bereits eingegebenen Zeitraum beim Ausschalten erhalten.
Bei Aktivierung einen positiven gültigen Zeitraum verlangen, dessen Einheit beim
Speichern und Laden ohne Rundungsfehler berücksichtigt wird.

Nur bei eingeschalteter Inaktivitätsregel nach deren Ablauf erneut bestätigen,
bevor geschützter Zugriff fortgesetzt wird. Hintergrundabfragen, Vaadin-Heartbeats
und Push-Verbindungen gelten nicht als Benutzeraktivität und dürfen diese Frist
nicht endlos verlängern. Zeitgrenzen serverseitig durchsetzen und vorhandene
Sitzungsgrenzen erhalten.
Ist nur der 2FA-Nachweis abgelaufen, kann die laufende Sitzung nach gültiger
Bestätigung weiterverwendet werden. Ist die gesamte Sitzung abgelaufen, ist eine
neue Anmeldung nötig. Es wird kein aktives Inaktivitätsintervall voreingestellt.

### Abgestimmte Voreinstellung für sensible Änderungen

Für Passwort einrichten/ändern, E-Mail-Adresse ändern, Anbieter verbinden/trennen
und Faktor-/Ersatzcodeverwaltung verlangt die Voreinstellung bei eingerichteter
und nach geltender Regel aktiver 2FA eine frische Bestätigung für jede einzelne
Aktion. Die Bestätigung der normalen Anmeldung genügt dafür nicht automatisch.
Die Freigabe ist einmalig und auf den bestätigten Zweck beschränkt. Standardmäßig
keine gemeinsame Schonfrist für mehrere unterschiedliche Änderungen verwenden.

Auch diese Anforderung bleibt je Funktion und Rolle durch den Superadmin
konfigurierbar. Bei Pflicht ohne eingerichteten Faktor ist zunächst die gesicherte
Einrichtung erforderlich. Bei ausgeschalteter Anforderung gelten weiterhin die
anderen vorgesehenen Identitätsprüfungen. Die Google-Entwicklungs-Ausnahme ist
separat freizugeben und wird nicht durch das Abschalten von 2FA aktiviert.

Änderungen an der Sicherheitskonfiguration benötigen selbst einen hinreichenden
Nachweis nach der zuvor geltenden Regel. Eine noch nicht gespeicherte Abschwächung
darf ihre eigene Freigabe nicht erleichtern. Rollenentzug und zwischenzeitliche
Regeländerungen müssen auch in alten Ansichten und bei direkten Dienstaufrufen wirken.

### Abgestimmte Startfreigabe der Entwicklungsoptionen

Die zusätzlichen Sicherheitsausnahmen sind ausschließlich nach ausdrücklicher
Freigabe in der Startkonfiguration verfügbar, bevorzugt über `.env`. Geplanter
Schalter: `APP_SECURITY_DEVELOPMENT_OPTIONS_ENABLED`, Standardwert **false**.
Die tatsächlich angebundene Einstellung bei Umsetzung in `.env.example` und der
Entwicklungsanleitung dokumentieren. Keine echte `.env` ungefragt ändern.

Bei false oder fehlender Freigabe die Entwicklungsoptionen in der Oberfläche
nicht anbieten und jeden Versuch der Aktivierung oder Anwendung auch im Backend
abweisen. Das gilt ebenso für Superadmins, direkte Aufrufe und alte geöffnete
Ansichten. Gespeicherte Ausnahmen aus einem früheren Entwicklungslauf werden ohne
Startfreigabe nicht wirksam. Auch daraus stammende Freigaben dürfen die normale
Sicherheitsprüfung nach dem Wechsel zurück nicht umgehen. Startfreigabe ausschließlich
beim Anwendungsstart lesen. Eine Änderung erfordert Neustart und kann nicht über
die Oberfläche, eine Browseradresse oder einen URL-Parameter vorgenommen werden.
Die bestehende H2-Konsolenfreigabe ist nur das Bedienvorbild, keine automatische Kopplung.

Bei true erscheint ausschließlich für Superadmins ein klar gekennzeichneter
Entwicklungsabschnitt innerhalb der 2FA-Konfiguration. Die Startfreigabe allein
schaltet keine Schutzprüfung ab. Darunter die konkret vereinbarten Ausnahmen
einzeln anbieten: 2FA-Abfragen für Entwicklung vorübergehend aussetzen und den
schwachen erneuten Google-Durchlauf gesondert erlauben. Keine allgemeine Einstellung
„Sicherheitslevel Minimum“, die weitere Schutzmaßnahmen stillschweigend abschaltet,
und keine zusätzliche Entwicklerplattform als Nebenprojekt aufbauen.

Aktive Ausnahmen im Entwicklungsabschnitt und den betroffenen Sicherheitsabläufen
mit einer Warnung sichtbar machen. Eingerichtete Faktoren bleiben erhalten. Eine
ausgesetzte Prüfung nicht als tatsächlich erfolgreich geprüfte 2FA speichern.
Kontobindung, Rollenprüfung, CSRF-, Eingabe-, Token-/Signatur- und Widerrufsschutz
bleiben bestehen. Normale rollen-/funktionsbezogene 2FA-Regeln einschließlich
freiwilliger oder ausgeschalteter Anforderungen bleiben davon getrennt konfigurierbar.

## Konten, Ersteinrichtung und Wiederherstellung

| Ausgangslage | Geplanter sicherer Weg |
| --- | --- |
| Faktor vorhanden, Regel verlangt 2FA | Authenticator oder zulässiger Ersatzcode. Anbieterwechsel umgeht die Regel nicht. |
| Kein Faktor, Portalpasswort vorhanden | Frisches Passwort als Erstnachweis und erster gültiger TOTP zur Einrichtung. |
| Kein Faktor, Microsoft verbunden | Geeigneten frischen Anbieternachweis prüfen, anschließend TOTP-Einrichtung bestätigen. |
| Bestehendes Google-Konto ohne Portalpasswort und ohne eingerichteten Faktor | Zugehöriges Google-Konto erneut bestätigen, einmaligen E-Mail-Code an bestätigte Kontaktadresse prüfen, anschließend ersten TOTP bestätigen. Begrenztes Restrisiko dieser Ersteinrichtung ist akzeptiert. |
| Anbieter deaktiviert | Vorhandenen lokalen Faktor oder anderen zulässigen Nachweis verwenden. Sonst verständlich blockieren und gesicherte Hilfe anbieten. |
| Faktor verloren | Eingeschränkte Wiederherstellung. Fehlender Faktor schaltet die Anforderung nicht ab. |

Auch ohne konfigurierte 2FA bleibt die unabhängige Bestätigung sensibler Aktionen
erforderlich. Google-only-Konten dürfen diese im sicheren Betrieb nicht einfach
durch einen weiteren Google-Rücksprung ersetzen. Nur die ausdrücklich aktivierte
Entwicklungs-Ausnahme darf den heutigen Übergangsweg weiter erlauben.

Ein über den schwachen Entwicklungsweg eingerichtetes Passwort oder ein neuer
Faktor darf nicht stillschweigend als unabhängig abgesicherter Erstnachweis gelten.
Diese Vertrauensgrenze beim Wechsel zurück in den sicheren Betrieb behandeln.
Bereits erfolgte Kontoübernahmen werden durch 2FA nicht rückwirkend geheilt.

### Abgestimmte erste Einrichtung bei Google-Konten ohne Portalpasswort

Für den ersten Entwurf ist folgender Ablauf ausdrücklich freigegeben:

1. Das bereits mit dem Portalbenutzer verbundene Google-Konto erneut bestätigen.
   Der OIDC-Rücksprung muss zum begonnenen Ablauf und bestehenden Konto gehören.
2. Einen zufälligen, kurz gültigen, einmaligen Bestätigungscode an die bereits
   hinterlegte bestätigte Kontaktadresse senden. Diesen Code vor der Einrichtung prüfen.
3. Authenticator einrichten und einen ersten gültigen TOTP bestätigen.
4. Ersatzcodes ausgeben und die abgeschlossene Einrichtung mit einer Sicherheitsmail melden.

Dies ist ausschließlich eine Erstregistrierung eines Faktors für ein Konto, das
noch keinen Faktor besitzt. Keine bereits eingerichtete 2FA damit ersetzen,
entfernen, zurücksetzen oder eine sensible Kontoaktion direkt freigeben.
Insbesondere darf das Entfernen oder der Verlust eines vorhandenen Faktors den
Fall nicht wieder zu einer ungeschützten Ersteinrichtung machen. Solche Zustände
bleiben im geschützten Wiederherstellungs-/Wechselablauf.

Code und Ablauf an Benutzer, Sitzung, Zweck, aktuelle Zugangs-/Faktorversion und
die bestätigte Empfängeradresse binden. Änderungen dieser Angaben machen die
Freigabe ungültig. Ohne bestätigte Kontaktadresse erst den bestehenden Kontakt-
bestätigungsweg abschließen. Keine neue Adresse aus dem Einrichtungsdialog oder
Ticket übernehmen. Gültigkeit und Versuche begrenzen, Codes nicht im Klartext
speichern oder protokollieren und nach Verwendung verwerfen. Wiederholter Versand
darf keine parallelen gültigen Freigaben oder unbegrenzten Prüfversuche erzeugen.
Fehlgeschlagener Versand ist keine abgeschlossene E-Mail-Bestätigung.

Bei der abschließenden Speicherung unter den bestehenden Sperren erneut prüfen,
dass noch keine konkurrierende Einrichtung oder Faktoränderung erfolgt ist.
Die E-Mail-Bestätigung ist keine allgemeine Aktionsfreigabe. Bei verpflichtender
Anmelde-2FA bleibt der vollständige Zugriff bis zur fertigen Einrichtung eingeschränkt.

Bewusst akzeptiertes Restrisiko: Wer sowohl Portal-/Google-Sitzung als auch
Kontaktpostfach kontrolliert, kann auch den E-Mail-Code lesen und einen eigenen
Authenticator einrichten. Die E-Mail ist hier kein unabhängiger zweiter Faktor
gegen diese gemeinsame Übernahme. Der Ablauf bietet mehr Schutz als eine offene
Portalsitzung allein, beseitigt dieses Szenario jedoch nicht vollständig. Diese
Grenze nicht als behobene Sicherheitslücke oder gleichwertigen frischen Google-
Nachweis darstellen. Sie gilt als akzeptierte Grenze der ersten Einrichtung und
ist von der separat geplanten schwachen Google-Entwicklungs-Ausnahme zu unterscheiden.

Auch bei regulärem Faktorwechsel Sicherheitsmails vorsehen. Diese dienen der
Information über eine tatsächliche Änderung und ersetzen keinen vorherigen
Sicherheitsnachweis. Eine Mail mit „Falls Sie das nicht waren“ hebt nicht allein
eine Änderung sicher auf. Ein zusätzlicher automatischer Rücknahmeweg ist nicht beauftragt.

Bei neuer externer Registrierung weiterhin kein zusätzliches Portalpasswort
verlangen. Kontoidentität, Kontaktbestätigung und Einrichtung nach geltender
Anmelderichtlinie vor vollständigem Zugriff prüfen. Registrierung und erneute
Sicherheitsbestätigung trennen. Gleiche E-Mail-Adressen, wiederholte Rücksprünge
oder neue Anbieter dürfen kein bestehendes Konto übernehmen oder einen Faktor zurücksetzen.

Ersatzcodes zufällig erzeugen, nur einmal anzeigen und ausschließlich gehasht
speichern. Einmalige Verwendung atomar absichern. Neue Codes machen alte ungültig.
Sie ersetzen den zweiten Faktor, nicht beliebige fehlende Identitätsnachweise.
Erzeugen und Erneuern benötigen die passende Sicherheitsbestätigung.
In der Einrichtung verständlich erklären: Ersatzcodes helfen bei verlorenem
Smartphone oder fehlendem Zugriff auf die Authenticator-App. Ein Code ersetzt
die entsprechende Authenticator-Bestätigung und wird dabei verbraucht. Er ersetzt
nicht die reguläre Anmeldung. Zum sicheren, getrennten Aufbewahren auffordern.
Die Sicherung durch ein herunterladbares, druckbares Notfalldokument mit klarer
Erklärung erleichtern. Die Datei enthält die bei dieser Ausgabe erzeugten Codes,
keinen zusätzlichen allgemeinen Hauptschlüssel. Codes nach der vorgesehenen
einmaligen Anzeige/Ausgabe nicht im Klartext für spätere Downloads aufbewahren.

### Abgestimmtes PDF-Notfalldokument

Die PDF aus einer eigenen HTML-/CSS-Vorlage unter den Anwendungsressourcen
erzeugen, beispielsweise `pdf/recovery-codes.html`. Als Java-Renderer ist
**OpenPDF-html** vereinbart. Vor Einbindung passende Version, Java-Kompatibilität,
Abhängigkeiten und Lizenzhinweise prüfen. Kein eigener PDF-Layoutbau in Java und
kein zusätzlicher externer PDF-Dienst. Die vorhandenen MJML-Mailvorlagen bleiben
unverändert, ihre Vorlagenidee wird für das druckbare Dokument übernommen.

Vorlage mit dem vorhandenen Service-Logo, konfiguriertem Servicenamen,
Kontozuordnung, Erstellungsdatum und den gerade ausgegebenen Ersatzcodes befüllen.
Überschrift „Ihre 2FA-Ersatzcodes“ sowie kurze Anleitung zu Verwendung, einmaliger
Gültigkeit und sicherer Aufbewahrung aufnehmen. DE/EN aus dem gemeinsamen
typisierten Textkatalog bedienen. Eingefügte Werte sicher maskieren.

Druckfreundliche helle A4-Gestaltung mit den abgestimmten blauen Akzenten und
übersichtlicher zweispaltiger Codeliste verwenden. Logo und geeignete Schriften
über Classpath-Ressourcen einbetten. Keine externen Bild-/Schriftabrufe oder vom
Benutzer bestimmbaren Ressourcenadressen zulassen. Die PDF muss auch aus dem
gepackten JAR zuverlässig erzeugt werden können.

Die PDF beim autorisierten Download im Speicher erzeugen und direkt ausliefern,
ohne HTML-Zwischendateien, PDF-Dateien oder geheime Codes dauerhaft auf dem Server
abzulegen. Keine geheimen Inhalte im Renderer-Logging oder in Fehlermeldungen.
Download gegen fremde/abgelaufene Ausgabezugriffe und Caching schützen.
Die einmalige Ausgabephase ist von der einmaligen Verwendung jedes Codes getrennt.
Nach Abschluss der Ausgabephase stehen alte Codes nicht erneut zum Abruf bereit.
Später nur durch erneute Sicherheitsbestätigung einen neuen Satz erzeugen, der
alle bisherigen Ersatzcodes ungültig macht. Kein neuer allgemeiner PDF-Vorlageneditor
oder Dokumentenverwaltungsbereich gehört zu diesem Umfang.

Abgestimmte Grenze: Das Portal hat keine vorher verifizierte bürgerliche Identität
seiner Benutzer. Name, Geburtsdatum, Ausweis oder neu eingetragene Anschrift sind
deshalb kein ausreichender Nachweis der Kontoinhaberschaft. Kein Postident und
keine zusätzliche Ausweissammlung als Teil dieses Pakets einführen. Wiederherstellung
auf vorher eingerichtete geeignete Nachweise stützen. Sind Authenticator und alle
Wiederherstellungsnachweise verloren, keinen garantierten Wiederzugang versprechen.
Ein E-Mail-Zugriff allein wird nicht zum Ersatz der vorgeschriebenen 2FA erklärt.

Sicherheitsfragen sind ausdrücklich ausgeschlossen. Wiederherstellung über die
vorher ausgegebenen Ersatzcodes anbieten. Eine weitere persönliche Frage oder ein
neu erzeugter allgemeiner Notfallschlüssel wird dafür nicht eingeführt.

Admins und Superadmins dürfen Wiederherstellungsanfragen gemeinsam sichten,
übernehmen und bearbeiten. Die sicherheitsrelevante Freigabe erfolgt ausschließlich
durch einen berechtigten Superadmin mit eigener frischer Bestätigung und
Prüfung eines geeigneten bestehenden Nachweises der Kontoinhaberschaft.
Ein Ticket oder die Einschätzung eines Bearbeiters allein genügt nicht. Fehlt ein
geeigneter Nachweis, unterstützt der Admin bei vorhandenen Wiederherstellungswegen,
gibt aber keinen ungesicherten Faktor-Reset frei.
Ein E-Mail-Link oder eine offene Sitzung allein entfernt keinen Faktor. Hilfe als
befristeten, zweckgebundenen Wiederherstellungsweg gestalten. Betroffene Sitzungen,
Aktionsfreigaben und Ersatzcodes entsprechend widerrufen. Bei Pflicht-2FA kein
ersatzloses Entfernen zulassen. Für den Verlust des letzten Superadmin-Faktors
gilt der nachfolgende manuelle Betreiberweg. Mailfehler dürfen den tatsächlichen
Ausgang einer bereits gespeicherten Sicherheitsänderung nicht verschleiern.

Bleiben Authenticator und gültige Ersatzcodes unzugänglich, bleibt der geschützte
Kontozugang gesperrt. Admins dürfen diesen Zustand nicht aufgrund des Tickets,
einer E-Mail-Antwort oder persönlicher Angaben umgehen. Ohne gültigen Nachweis
kann die Anfrage mit verständlicher Begründung geschlossen werden. Die fehlende
2FA wird dadurch weder erfüllt noch abgeschaltet. Keine automatische Löschung
oder zusätzliche Kontodeaktivierung allein durch eine Anfrage auslösen.

### Letzter ausgesperrter Superadmin: manueller Betreiberweg

Für diesen seltenen Totalausfall ist ein dokumentierter manueller Datenbankeingriff
durch den Betreiber mit direktem Server-/Datenbankzugriff vereinbart. Vorzugsweise
einen bestehenden Superadmin gezielt wiederherstellen. Kein verstecktes Notfallkonto,
keine neue Rolle, keine zusätzliche Reparaturoberfläche und kein eigenes
Wiederherstellungsprogramm als Teil dieser Umsetzung bauen.

Nach Fertigstellung des Datenmodells eine kurze, zum aktuellen Schema passende
Anleitung in die Sicherheits-/Betriebsdokumentation aufnehmen und isoliert nachprüfen.

Diese Betreiberanleitung liegt als Markdown unter `docs/`, außerhalb des Java-Codes.
Sie ist weder E-Mail-Vorlage noch auswählbare Ticketantwort und wird nicht an
Benutzer versendet oder über den eingeschränkten Hilfszugang ausgeliefert.
Änderungen am Sicherheitsdatenmodell und seiner manuellen Reparaturanleitung
müssen zusammen gepflegt werden.

Sie beschreibt den kontrollierten Eingriff für genau ein bestimmtes Konto:

- Anwendung für die Datenbankwartung kontrolliert anhalten, damit keine parallelen
  Sitzungs- oder Faktoränderungen mit dem Eingriff konkurrieren.
- Betroffenes Superadmin-Konto eindeutig bestimmen. Verlorenen Faktor und dessen
  Ersatzcodes gezielt ungültig machen, vollständige geschützte Neueinrichtung verlangen.
- Zugehörige Zugangsversion ändern und bestehende Sitzungen, dauerhafte Zugänge sowie
  offene Aktions-/Wiederherstellungsfreigaben konsistent ungültig machen. Nicht lediglich
  ein einzelnes Faktor-Feld leeren und dadurch alte Berechtigungen weiterverwenden.
- Nach Neustart regulär anmelden und den vorhandenen Einrichtungsablauf abschließen.
  Vor vollständigem Zugriff einen neuen Authenticator und neue Ersatzcodes einrichten.
  Die allgemeine 2FA-Richtlinie anderer Benutzer bleibt bestehen.

Der durch den Betreiber gesetzte Neueinrichtungszustand ist ausdrücklich von einer
normalen Ersteinrichtung und einer Web-Anfrage zu unterscheiden. Er erlaubt nach
regulärer Anmeldung nur die erzwungene Neueinrichtung für dieses Konto und wird
beim erfolgreichen Abschluss verbraucht. Bloßes Leeren von Faktor-Daten darf diese
Betreiberfreigabe nicht erzeugen oder einen verlorenen Faktor zu einer offenen
Google-/E-Mail-Ersteinrichtung machen.

Konkrete Tabellen/Felder und nötige zusammengehörige Anweisungen erst anhand des
fertigen Schemas dokumentieren. Der manuelle Eingriff muss nachvollziehbar festgehalten
werden. Er ist eine bewusste Betreibermaßnahme außerhalb der normalen Webrechte und
kein Wiederherstellungsversprechen für Benutzer ohne Nachweis. Falls zusätzlich ein
Passwort geändert werden muss, ausschließlich einen mit dem aktuellen Encoder
erzeugten Hash speichern, kein Klartextpasswort oder festes Standardpasswort.

### Kleine Ticketgrundlage für Wiederherstellungsanfragen

Vereinbart ist ein kleiner gemeinsamer Grundaufbau, der später auch Feedback,
Problemmeldungen und weitere Aufgaben aufnehmen kann. Im jetzigen 2FA-Paket wird
nur der Anfragetyp Wiederherstellung umgesetzt. Die spätere Feedback-Oberfläche
und ein umfassendes Ticketsystem sind damit nicht beauftragt.

Der Benutzer erreicht nach dem ersten Anmeldenachweis an der 2FA-Abfrage den Weg
„Kein Zugriff auf Authenticator oder Ersatzcodes?“. Er kann dort eine eigene
Anfrage stellen und ihren Stand samt Rückfragen einsehen. Das ist ein eingeschränkter
Hilfszugang, keine vollständige Anmeldung und kein Zugriff auf fremde Anfragen.
Die Anfrage wird an das bereits bestimmte Konto gebunden, nicht an einen frei
eingegebenen fremden Kontonamen. Sie stellt selbst keinen Identitätsnachweis dar.

Kleine gemeinsame Grundlage:

- Vorgangsnummer, Anfragetyp, Ersteller/Kontobezug, Betreff und Beschreibung,
  Erstellungs-/Änderungszeit sowie zuständiger Bearbeiter.
- Allgemeiner Bearbeitungsstatus und begrenzter Rückfrage-/Antwortverlauf.
  Sicherheitsentscheidung und Wiederherstellungsfreigabe getrennt davon halten.
- Kontaktaufnahme an die aktuell im Konto hinterlegte bestätigte E-Mail-Adresse
  ermöglichen. Die Adresse wird aus dem Konto ermittelt, nicht aus freiem Tickettext
  übernommen. Eine Antwort über diesen Kontaktweg ist kein Ersatzcode oder 2FA-Nachweis.
- Gemeinsame Adminliste mit Übernahme durch einen Bearbeiter. Alle berechtigten
  Admins werden über den vorhandenen Benachrichtigungsmechanismus auf neue Anfragen
  aufmerksam gemacht. Die dauerhafte Anfrage bleibt unabhängig von der Benachrichtigung bestehen.
- Sichtbare Zuständigkeit und gegen konkurrierende Übernahme/Änderung geschützte
  Bearbeitung. Antragsteller sehen nur ihre eigenen freigegebenen Inhalte und Antworten.
  Interne Vermerke dürfen nicht versehentlich als Benutzerantwort erscheinen.
- Angemessene Text-/Mengen-/Versuchsgrenzen, Schutz vor wiederholten gleichartigen
  offenen Anfragen und geregelte Aufbewahrung. Keine Passwörter, TOTP-Geheimnisse,
  Ersatzcodes oder Wiederherstellungstokens in Tickettext oder Benachrichtigung speichern.

Allgemeine Statuswerte passend zum kleinen Bearbeitungsablauf benennen. Keine
unbelegte Identitätsbehauptung im Ticket als Sicherheitsnachweis verwenden.
Die gemeinsame Grundlage soll verständlich bleiben: keine frei konfigurierbare
Workflow-Engine, Anhänge, Prioritäts-/SLA-Systeme
oder weiteren Kommunikationskanäle über Ticketantwort und die beschriebene E-Mail hinaus.

### Vorbereitete Hilfeantwort

Admins erhalten im Ticket die Aktion „Anleitung senden“ mit einer vorbereiteten
Antwort in Deutsch und Englisch. Die passende Sprachfassung anhand der
Benutzersprache anbieten und den konkreten Inhalt vor dem Versand anzeigen.
Die Anleitung als Antwort im Vorgang festhalten und über die vorhandene Mailqueue
an die hinterlegte bestätigte Kontaktadresse senden. Kein neues allgemeines
Massenmail-/Vorlagensystem für diesen Zweck bauen.

Auswahlkennung und Versandlogik sind im Anwendungscode festgelegt. Die eigentlichen
Benutzertexte liegen in übersetzten Ressourcen, nicht als lange Java-Textliterale.
Für die E-Mail die bestehende MJML-zu-HTML-Vorlagenverarbeitung unter
`app/src/main/resources/email/` verwenden. Anleitungstexte für Ticketvorschau/-antwort
und E-Mail aus denselben übersetzten Inhalten beziehen, damit sie nicht auseinanderlaufen.
UI-Bezeichnungen und gemeinsame Texte über den bestehenden typisierten Katalog
führen. Kein neuer Vorlageneditor im Adminbereich ist Teil dieser Umsetzung.
Vorlagen werden als Projektressourcen gepflegt und über den vorhandenen Vorlagen-/
Anwendungsbuild übernommen, nicht als frei änderbare Inhalte in der laufenden Datenbank.

Die Standardantwort erklärt:

- Ersatzcodes wurden bei der 2FA-Einrichtung zum Herunterladen und Drucken angeboten.
  Im gespeicherten Notfalldokument oder eigenen Passwortmanager danach suchen.
- Regulär mit Portalpasswort oder Google/Microsoft anmelden. An der 2FA-Abfrage
  „Ersatzcode verwenden“ wählen und einen noch unbenutzten Code selbst eingeben.
- Ein Ersatzcode funktioniert nur einmal. Nach erfolgreicher Bestätigung den
  Authenticator über den abgesicherten Ablauf neu einrichten und neue Codes sichern.
- Niemals Passwort, Authenticator-Geheimnis oder Ersatzcodes an den Support schicken
  oder in das Ticket schreiben. Die E-Mail enthält selbst keine neuen Zugangscodes.
- Ohne Authenticator und gültigen Ersatzcode können wir die Sperre nicht aufheben.

„Anleitung senden“ und Ticketabschluss erzeugen keine Wiederherstellungsfreigabe
und verändern den Faktorstatus nicht. Versandaktion rollenbasiert und gegen
Wiederholungs-/Mehrfachklicks absichern. Eine Queueaufnahme nicht als bereits
zugestellte E-Mail melden. Fehler verständlich anzeigen, sodass der Admin den
Versand gezielt wiederholen kann, ohne Freigaben oder Kontostatus zu verändern.

Die fachliche Wiederherstellung bleibt ein eigener abgesicherter Ablauf. Weder
„Ticket erledigt“ noch ein Wechsel des Tickettyps darf 2FA zurücksetzen, eine
Freigabe erzeugen oder zusätzliche Rechte verleihen. Freigabe und Verwendung erneut
gegen Konto, aktuellen Faktor-/Zugangsstand und Berechtigung prüfen. Sicherheits-
änderungen machen veraltete offene Freigaben ungültig. Spätere Feedback-Tickets
können den gemeinsamen Ablauf verwenden, erhalten aber keine Wiederherstellungsrechte.

## Technische Ansatzpunkte

Die APIs anhand der in `pom.xml` verwendeten Versionen prüfen. Vorhandene Spring-
Security-Mechanismen für Faktorinformationen und Berechtigungen verwenden.
Ein E-Mail-One-Time-Token ist kein TOTP-Authenticator.

- `SecurityConfig`, `LoginCompletionFilter`, `SecurityService` und
  `SessionAdmissionService`: eingeschränkten Zustand nach erstem Nachweis vom
  vollständigen Zugriff trennen. Sitzungsreservierungen und Fehler-Rollback erhalten.
- `ExternalLoginFlow`, `ExternalAuthenticationToken` und `ExternalIdentityService`:
  Anmeldung, Registrierung, Verbindung und Sicherheitsbestätigung sauber trennen.
- `AccountSecurityService`: sowohl direkte Passwortprüfung als auch externe
  Aktionsfreigabe absichern. Regeln innerhalb bestehender Transaktions-/Sperrgrenzen
  bei der tatsächlichen Änderung erneut prüfen. Eine UI-Sperre reicht nicht.
- `DurableAuthentication`: unterstützt derzeit keine zusätzlichen Faktoren zusammen
  mit einem externen Anmeldetoken. Tatsächlich geprüfte Informationen erhalten,
  aktuelle Rechte/Versionen prüfen und beim Neustart keine Frische erfinden.
- Bestehende Sicherheitstabs und Cosmos-Dialoge erweitern. Native Vaadin-Komponenten,
  DE/EN und HTML-Referenz gemeinsam pflegen. Eigene typisierte Konfigurationsgruppe
  mit Superadmin-Zugriff statt frei eingegebener URL-Muster als Sicherheitsregeln.
- Datenmodell und aktuelles H2-Schema gemeinsam ändern und mit frischen isolierten
  Daten prüfen. Keine Altstand-Migration oder vorsorgliche Kompatibilitätsschicht.

Die Einrichtung bleibt herstellerunabhängig über TOTP. Für unerfahrene Benutzer
Google Authenticator und Microsoft Authenticator als unverbindliche Beispiele mit
offiziellen Bezugs-/Hilfelinks anbieten. Andere kompatible TOTP-Apps bleiben nutzbar.
App-Auswahl und Google-/Microsoft-Anmeldeanbieter sind unabhängig. Keine Anmeldung
bei einem bestimmten App-Anbieter als Voraussetzung unserer Einrichtung einführen.

TOTP-Vorschlag: sechsstelliger Code, 30-Sekunden-Schritte und enge Toleranz.
Zufälliges Geheimnis pro Benutzer, authentifiziert verschlüsselt speichern und
Schlüssel außerhalb der Datenbank bereitstellen. Wiederverwendung auch bei
parallelen Anfragen atomar verhindern. Kontobezogene Versuchslimits dürfen durch
neue Dialoge oder Sitzungen nicht zurückgesetzt werden.

QR-Code, manueller Einrichtungsschlüssel und einmal angezeigte Ersatzcodes dürfen
nicht unbeabsichtigt in Vaadin-Zustand oder JDBC-Sitzungen persistieren. Geschützte,
kurzlebige Ausgabe und anschließendes Entfernen ausdrücklich entwickeln und testen.
Keine Geheimnisse in Logs, URLs, Browserablage oder externen QR-Diensten. Einen
erforderlichen eigenen UI-Baustein erst nach Prüfung nativer Alternativen und
gezielter Abstimmung gemäß Theme-Regeln einführen.

Aktionsfreigaben bleiben einmalig an Konto, Zugangsversion, Sitzung und Zweck
gebunden. Die bestehende Fünf-Minuten-Freigabe bildet zunächst die Obergrenze für
eine konkrete Aktion. Faktor- und Richtlinienänderungen berücksichtigen. Ein allgemeiner
2FA-Sitzungsstatus ist nicht automatisch eine Freigabe für jede spätere Kontoaktion.

## Arbeitspakete

| Paket | Inhalt und Ergebnis |
| --- | --- |
| P1 – Regeln und Fehlernachweis | Abgestimmtes Konfigurationsmodell, Standardwerte, Ersteinrichtung und Entwicklungsgrenze fachlich konkretisieren. Google-Umgehungsweg gezielt durch Regressionen erfassen. |
| P2 – Faktorgrundlage | TOTP, geschützte Geheimnisse, Schema, Replay-/Versuchsschutz und atomare Ersatzcodes. PDF-Erzeugung aus HTML-/CSS-Vorlage mit OpenPDF-html und vorhandenen Logo-/Textressourcen. |
| P3 – Anmeldung und Kontoaktionen | Regeln für alle Anmeldewege, dauerhafte Anmeldung und fachliche Änderungen. Google-Ersteinrichtung über E-Mail-Code und ersten TOTP mit dokumentiertem Restrisiko. Eingerichtete 2FA gegen die bisherigen Google-Umgehungswege absichern. |
| P4 – Benutzeroberfläche | Einrichtung, Codeabfrage, Verwaltung und verständliche Zustände in bestehenden Sicherheitstabs und Dialogen, DE/EN und Referenz. |
| P5 – Superadmin-Konfiguration | Rollen, Funktionen und Wiederholung. Gewarnte einzelne Entwicklungs-Ausnahmen nur nach standardmäßig ausgeschalteter Startfreigabe über Umgebungsvariable. Rechte auch bei direkten Aufrufen und alten Ansichten. |
| P6 – Anbieter nur für Superadmins | Google-/Microsoft-Konfiguration im selben Umsetzungspaket rollenbasiert beschränken, einschließlich UI, Dienstzugriff und Nachtests. Vor der gemeinsamen Abnahme fertigstellen. |
| P7 – Wiederherstellung, Ticketgrundlage und Widerruf | Kleine gemeinsame Anfrageverwaltung zunächst für Wiederherstellung. Admins bearbeiten, Superadmins geben abgesichert frei. Ersatzcodes, Faktorwechsel und Regel-/Rollenänderungen. Manuellen Datenbank-Notfallweg des Betreibers dokumentieren und isoliert prüfen, keine zusätzliche Reparaturoberfläche. |
| P8 – Prüfung und Abschluss | Regressionen, isolierter Vergleich und Fachdokumentation für alle Pakete. Gemeinsame Abnahme einschließlich Anbieterberechtigungen. Bug entsprechend tatsächlich abgesichertem Umfang schließen. |

P6 betrifft ausschließlich die administrative Anbieter-Verwaltung. Aktuell erlaubt
`ExternalProviderSettings` über `AdminRolePolicy` auch Administratoren das Laden
und Speichern. Navigation, direkten Aufruf, Laden, Speichern, Neuladen und bereits
geöffnete Ansichten nach Rollenentzug gemeinsam absichern. Normale Admins dürfen
auch nicht indirekt beim Aufbau der Konfiguration Anbieterformulare oder
Zugangsdaten laden. Interner Anbieterzugriff für reguläre OAuth-Anmeldungen bleibt
unabhängig davon. Die normale Google-/Microsoft-Anmeldung für Benutzer funktioniert weiter.

## Planungsstand und Umfangsgrenze

Die besprochenen Grundsatzfragen sind geklärt: veränderbare Rollen-/Funktionsregeln,
Sitzungsverhalten mit zunächst deaktivierter Inaktivitätsabfrage, frische Bestätigung
sensibler Änderungen, Google-Ersteinrichtung mit akzeptiertem Restrisiko, Ersatzcodes,
kleine Ticketgrundlage samt Hilfeantwort, manueller Betreiber-Notfallweg und über
die Startkonfiguration begrenzte Entwicklungsoptionen. Paket P6 gehört verbindlich dazu.

In zusammenhängenden, einzeln prüfbaren Etappen umsetzen. Kein vollständiges
Ticketsystem, keine Feedback-Funktion, kein verstecktes Notfallkonto, keine
Reparaturoberfläche und keine allgemeine Entwicklerplattform aus diesem Auftrag
ableiten. Die separate Vaadin-Komponentenübernahme bleibt außerhalb dieses Pakets.
Gemeinsame UI-/Funktionsabnahme bleibt nach Umsetzung erforderlich und umfasst
ausdrücklich auch die Anbieterberechtigungen. Diese werden nicht nach der übrigen
Abnahme nachgereicht.

## Abnahme

Mit und ohne Portalpasswort sowie mit Google, Microsoft und beiden Verbindungen
prüfen. Nach geltender Regel vorgeschriebene 2FA darf weder über einen anderen
Anmeldeweg noch über direkte Rücksprünge oder Wiederherstellung umgangen werden. Fehlende, falsche,
abgelaufene und wiederverwendete Codes sowie Zweck-, Konto-, Versions- und
Sitzungswechsel prüfen. Automatisierter Bedarf steht zentral in
[Testbedarf](../testbedarf.md); keine parallele Testübergabe führen.

Den Eintrag in [BUGS.md](../../BUGS.md) erst nach Umsetzung, passenden Nachtests und
bewusster Entscheidung zum verbleibenden Risiko von Konten ohne 2FA schließen.
Den erledigten Plan nach Abnahme in die aktuelle Fachdokumentation überführen.

Sicherer Betrieb und bewusst unsicherer Entwicklungsbetrieb erhalten getrennte
Erwartungen. Ein im Ausnahmebetrieb erfolgreicher Google-Rücksprung beweist keine
Behebung der Sicherheitsschwäche. Echte Authenticator-Bedienung und Verständlichkeit
zusätzlich gemeinsam abnehmen. TOTP schützt nicht gegen jede Phishing-Form.

Grundlagen:
[Google-Anbietergrenze](https://developers.google.com/identity/siwg/security-bundle#authentication_time),
[TOTP/RFC 6238](https://www.rfc-editor.org/rfc/rfc6238),
[Spring Security MFA](https://docs.spring.io/spring-security/reference/servlet/authentication/mfa.html),
[OTT ist kein TOTP](https://docs.spring.io/spring-security/reference/servlet/authentication/onetimetoken.html),
[OWASP zu Sitzungsablauf und Inaktivität](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html),
[NIST zu Grenzen von Sicherheitsfragen](https://pages.nist.gov/800-63-4/sp800-63b.html),
[OWASP zu MFA und Wiederherstellung](https://cheatsheetseries.owasp.org/cheatsheets/Multifactor_Authentication_Cheat_Sheet.html).

PDF-Grundlage: [OpenPDF-html](https://github.com/LibrePDF/OpenPDF/blob/master/openpdf-html/README.md).
