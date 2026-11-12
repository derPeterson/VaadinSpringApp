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

## 3. Zahlenfeld mit integrierter Einheitenauswahl analysieren

- [ ] Die bisherigen Dauerfelder und die feste Tagesanzeige in der
  Benachrichtigungskonfiguration als Ausgangspunkt verwenden. Ziel ist ein
  zusammenhängendes Feld: Zahlenwert links, rechts die aktuelle Einheit mit
  Aufklappsymbol und einer integrierten Auswahl statt einer separaten ComboBox.
- [ ] Zuerst die öffentlichen APIs und Gestaltungsmöglichkeiten der tatsächlich
  verwendeten Vaadin-Version prüfen: natives Zahlenfeld, Suffix sowie native
  Auswahlkomponenten und deren Zusammensetzung. Keine eigene Eingabekomponente
  bauen, bevor die konkrete Lücke und die geprüften Alternativen feststehen.
- [ ] Fokus, Tastaturbedienung, Zahlenfeldaktionen, Overlay-Platzierung, gemeinsame
  Feldhöhe und Darstellung auf schmalen Bildschirmen in den Vorschlag einbeziehen.
  Die Gestaltung soll an die bereits abgenommene Tagesanzeige anschließen.
- [ ] Pro Einstellung klären, welche Einheiten fachlich zulässig sind und wie
  eine Umrechnung ohne Wertverlust funktioniert. Die bisher ausschließlich in
  Tagen gespeicherten Aufbewahrungswerte nicht nur optisch auf Sekunden umstellen.
  Nötige Änderungen an Genauigkeit, Grenzen oder Speicherung gesondert benennen.
- [ ] Eine konkrete Empfehlung mit Aufwand und Grenzen vorlegen. Einen eventuell
  nötigen Eigenanteil erläutern und den Umbau erst nach Freigabe beginnen.

## 4. Einheitliche Seitenpfade und Breadcrumb-Navigation analysieren

- [ ] Das abgenommene Erscheinungsbild erhalten: blaue Kontextzeile, große
  Seitenüberschrift, bestehende Farben, Größen, Abstände und Positionen.
  Auch native Vaadin-Komponenten müssen diese vorhandene Gestaltung übernehmen.
  Geändert werden die einheitliche Benennungslogik und die Navigation.
- [ ] Die blauen Kontextzeilen und Seitenüberschriften in Besucherbereich,
  angemeldetem Portal, Administration, Konfiguration samt Untergruppen und Tabs,
  Ticketsystem sowie 2FA-Einrichtung, Anmeldung und Wiederherstellung vergleichen.
  Unterschiedliche Hierarchietiefen, doppelte Angaben und wechselnde Trennzeichen
  beziehungsweise Großschreibung konkret erfassen.
- [ ] Prüfen, ob die verwendete Vaadin-Version einen passenden nativen
  Breadcrumb-Baustein bietet. Dessen Vorhandensein nicht voraussetzen; alternativ
  eine fachlich passende Zusammensetzung vorhandener nativer Komponenten bewerten.
- [ ] Überall dieselbe Zuordnung verwenden: Die blaue Zeile zeigt die
  übergeordneten Bereiche, die große Überschrift den aktuellen Ort. Die aktuelle
  Seite nicht zusätzlich im blauen Pfad wiederholen. Einheitliche Bezeichnungen
  für dieselben Ziele verwenden; freie Beschreibungen gehören nicht in den
  Navigationspfad. Die Regel gilt für Besucherbereich, Benutzerportal,
  Administration und geschützte Zwischenschritte gleichermaßen.
- [ ] Schreibweise und Trennzeichen konsistent aus dem vorhandenen Design
  ableiten. Die konkrete Zuordnung von Bereichen, Unterseiten und Tabs anhand
  der Bestandsaufnahme zur gemeinsamen Sichtung vorlegen.
- [ ] Übergeordnete Ebenen sollen außerhalb gesperrter Abläufe als echte
  Navigationslinks dienen. Aktuelle Position, Zielseiten sowie erforderliche
  Abschnitts-/Tabparameter eindeutig festlegen. Tastatur, Fokus und schmale
  Bildschirmbreiten berücksichtigen; DE/EN gemeinsam planen.
- [ ] In den eingeschränkten 2FA-/Wiederherstellungs-/Hilfsabläufen keine
  Navigation anbieten, mit der die bestehende Sitzungssperre verlassen wird.
  Die serverseitige Sperre bleibt erhalten; auch ein normal dargestellter
  Kontextpfad erteilt keine zusätzlichen Navigationsrechte.
- [ ] Beispiele für die betroffenen Seiten und eine konkrete Empfehlung
  zur gemeinsamen Sichtung vorlegen. Erst nach abgestimmtem System umbauen;
  keine globale Umgestaltung während der Bestandsaufnahme.

## Vorerst zurückgestellt

Die Umstellung der Benachrichtigungsfilter, die Kennzahlenkarten und der erweiterte
Formularschutz bei Browseraktionen werden aktuell nicht bearbeitet. Sie bleiben
im [Komponentenplan](vaadin-komponenten-abgleich.md) erhalten und sind kein Auftrag
für diese Nachtliste.

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
