# Gesammelte Entwicklungsaufgaben für die Nacht

Status: **Nur gesammelt. Umsetzung noch nicht gestartet.**

Diese Liste wird bis zum ausdrücklichen Startsignal des Benutzers ergänzt.
Die Aussage, später schlafen zu gehen, startet keine Arbeiten automatisch.
Die Aufgaben gehören zum bestehenden Branch `codex/vaadin-komponentenanpassungen`.
Vor dem Start die bestätigten Fehler in [BUGS.md](../../BUGS.md) berücksichtigen.
Bestehende offene Arbeiten bleiben in ihren bisherigen Plänen.

## 1. Entwicklungskonten ausdrücklich kennzeichnen

- [ ] Eine eigene gespeicherte Eigenschaft „Entwicklungskonto“ einführen,
  unabhängig von Registrierungsweg, E-Mail-Nachweis und Berechtigungen.
- [ ] Bei der Kontoanlage in der Benutzerverwaltung einen Schalter nur für
  Superadmins anbieten. Standardmäßig ist er ausgeschaltet. Bestehende
  Vaadin-Komponenten, Cosmos-Stile und deutsche/englische Texte verwenden.
- [ ] Bei Startkonten die Eigenschaft pro Benutzer mitgeben können. Unsere
  Beispielkonten ausdrücklich als Entwicklungskonten kennzeichnen; echte
  Startkonten müssen weiterhin möglich sein. Beispiele und Dokumentation ergänzen.
- [ ] Bei eingeschaltetem Entwicklungskonto „Verifikation“ abwählen und
  deaktivieren. Der Hilfetext erklärt: „Für Entwicklung und Tests. Für dieses
  Konto werden keine E-Mails versendet.“
- [ ] Den bisherigen Verifikationsschalter für reguläre Konten behalten.
  Superadmins können damit weiterhin echte Konten ohne Verifikationspflicht
  bereitstellen. „Nicht geprüft“ bezeichnet den E-Mail-Nachweis und unterdrückt
  bei solchen regulären Konten keine Sicherheitsmails.
- [ ] In den Benutzerdetails ein eigenes Badge „Entwicklungskonto“ anzeigen.
- [ ] Sämtlichen kontobezogenen Mailversand für Entwicklungskonten unterbinden,
  auch Sicherheitsbenachrichtigungen nach 2FA-Änderungen. Bewusst angeforderte
  Mails wie Passwortreset und Reaktivierung gehören ebenfalls dazu. Fristmails
  und Löschbestätigungen berücksichtigen, auch wenn das Konto beim Versand
  bereits entfernt wurde. Allgemeine Mailtests sind davon getrennt.
- [ ] Mailabhängige Aktionen verständlich sperren beziehungsweise erklären.
  Kein scheinbarer Versandserfolg und kein Umgehen eines notwendigen
  Sicherheitsnachweises. Die konkreten Oberflächen bei der Umsetzung durchgehen.
- [ ] Bestehende Rollen-, Rechte-, Login- und 2FA-Prüfungen erhalten.
  „Entwicklungskonto“ gewährt keine zusätzlichen Rechte und keine 2FA-Ausnahme.
- [ ] Passende isolierte Regressionen ergänzen, Lesbarkeit prüfen und die
  aktuelle Fachdokumentation nachziehen. Offene automatisierte Prüfungen
  ausschließlich in [Testbedarf](../testbedarf.md) pflegen. Browserläufe erst
  mit passender ausdrücklicher Freigabe ausführen.

## 2. Vollständige Aktionslinks in E-Mails zum Kopieren anbieten

- [ ] Alle E-Mail-Vorlagen mit Aktionslink durchgehen, etwa Verifikation,
  Passwortreset, E-Mail-Änderung und Kontoreaktivierung. Unter jedem Aktionsbutton
  die vollständige Original-URL als auswählbaren Text anbieten.
- [ ] Einen kurzen erklärenden Hinweis auf Deutsch und Englisch ergänzen:
  Falls der Button nicht funktioniert, den vollständigen Link in die Adresszeile
  des Browsers kopieren. Kein zusätzlicher JavaScript-Kopierbutton in der Mail.
- [ ] Button und sichtbarer Link verwenden dieselbe Zieladresse und denselben
  bestehenden Token. Gültigkeit, Sicherheitsprüfungen und bisherige Funktionen
  erhalten; URLs weder kürzen noch mit Auslassungspunkten abschneiden.
- [ ] Den Zusatz in das bestehende gemeinsame E-Mail-Design einpassen:
  vorhandene Farben und Schriftgestaltung übernehmen, den Hinweis dezent halten
  und einen gut lesbaren Abstand unter dem Button vorsehen. Lange URLs auf
  Smartphonebreite sauber umbrechen lassen, ohne ihren kopierten Inhalt zu verändern.
- [ ] Gemeinsame Vorlagenbausteine wiederverwenden und einheitliche Darstellung
  in allen betroffenen Mails sicherstellen. Mails ohne Aktionslink erhalten keinen
  leeren Zusatzbereich. Eine gegebenenfalls vorhandene Textalternative ebenfalls
  berücksichtigen.
- [ ] In der abschließenden Testrunde die tatsächlichen Testmails in DE/EN auf
  Desktop- und Smartphonebreite prüfen und kontrollieren, dass sichtbare URL und
  Buttonziel übereinstimmen. Nur isolierte Testmails verwenden.

## Abschließender Punkt: große Testrunde

Dieser Punkt bleibt am Ende der Liste. Weitere gesammelte Entwicklungsaufgaben
werden davor eingefügt. Die Runde beginnt nach den vorherigen Aufgaben und erst
mit dem ausdrücklichen Startsignal für die Nachtarbeit.

- [ ] Den dann aktuellen Gesamtstand nach dem bestehenden Ablauf für die große
  Testrunde prüfen, einschließlich Build, JUnit-, Frontend- und Browserprüfungen.
- [ ] [Offenen Testbedarf](../testbedarf.md) mit den vorhandenen Tests abgleichen,
  fehlende sinnvolle Regressionen ergänzen und tatsächlich ausführen. Auch die
  offenen Nachtests aus den vorherigen Arbeiten berücksichtigen.
- [ ] Browserprüfungen isoliert in Deutsch und Englisch durchführen; betroffene
  Oberflächen auf Desktop- und Smartphonebreite prüfen. Die Freigabe für diese
  abschließende Runde umfasst diese Browserläufe. Die laufende Benutzeranwendung,
  ihre Daten und Sitzungen bleiben unverändert; kein echter Mailversand.
- [ ] Gefundene Fehler im vereinbarten Umfang beheben, eigene Änderungen auf
  Lesbarkeit prüfen und passende Nachtests ausführen. Menschliche Abnahmen und
  wegen fehlender Entscheidungen wartende Prüfungen ausdrücklich offen lassen.
- [ ] Den Prüfstand und verbleibenden Testbedarf aktualisieren. Zum Abschluss
  die Ergebnisse aller Nachtaufgaben und noch offenen Punkte zusammenfassen.
