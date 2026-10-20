# Sonar-Abendpaket vom 20.10.2026

Ausgangspunkt: main, Commit `f11e4c1`, sauberer Arbeitsbaum, 127 offene Findings im vom Benutzer erneuerten Export. Freigegebenes Paket: 48 konkrete Findings aus 17 Regeln. Keine Architekturbereinigung oder vollständige Sessionserialisierung.

## Ergebnis

**48 Findings einzeln bewertet: 31 mit gezielten Codeänderungen bearbeitet, 17 begründet im bestehenden Code beibehalten.** Unter den 31 bleibt die Benutzerermittlung trotz verbessertem Fehlertext ein fachliches Sonar-Review. Die Zahl 31 bedeutet daher ausdrücklich nicht, dass 31 Issues bereits serverseitig verschwunden sind. Kein neuer Scan, keine neuen Server-Statusentscheidungen und kein aktueller Gate-Erfolg für diesen Quellstand behauptet.

Die vollständige Zuordnung mit ursprünglicher Exportzeile, Entscheidung und Begründung steht in [sonar-abendpaket-findings-2026-10-20.csv](sonar-abendpaket-findings-2026-10-20.csv). Die Bewertungen sind lokale Vorschläge; am Sonar-Server wurde nichts umgestellt. 79 der 127 Ausgangsfindings liegen außerhalb dieses Pakets.

## Fachliche Korrekturen

### Avatar-Metadaten und Transaktionen

Der öffentliche Einzelabruf `UserAvatarService.state` rief vorher intern die mit REQUIRES_NEW/readOnly annotierte Sammelmethode `states` auf. Der Selbstaufruf ging am Transaktionsproxy vorbei. In einer bereits laufenden Schreibtransaktion lieferte der Einzelabruf dadurch sogar deren geflushte, noch uncommittete Metadaten statt des committed Zustands.

Jetzt besitzen öffentliche Einzel- und Sammelabrufe dieselbe unabhängige Lesetransaktion. Gemeinsame private Lesehelfer enthalten die unveränderte Projektion. `open` ruft den privaten Helfer innerhalb seiner bereits bestehenden programmgesteuerten Transaktion auf, sodass hierfür keine weitere Metadatentransaktion verschachtelt wird. Keine Self-Injection und keine zusätzliche Serviceklasse.

Der neue Test `singleMetadataReadIsIndependentOfPendingOuterChanges` ändert den Avatar in einer äußeren Transaktion, flusht und prüft trotzdem den vorher committed Einzel-/Sammelzustand. Der äußere Rollback bleibt erhalten. Der Test schlug am alten Produktionscode fehl und besteht nach der Korrektur.

Am reinen `SecurityService.handleLogin`-Adapter wurde die redundante Annotation entfernt. Er schreibt selbst keine Daten und delegiert weiterhin an den injizierten, transaktionalen `UserService.recordLogin`. Status-/Aktivitätsupdate, Sperren und Commit-/Rollbackverhalten bleiben dort. Login-, Session-, Rollen-, Push- und Status-Persistenztests wurden erneut ausgeführt.

### Authentifizierungs-Snapshot

`AdminRolePolicy` bindet den geprüften Principal einmal als UserDetails. Das beseitigt einen zweiten potenziell widersprüchlichen getPrincipal-Aufruf. Die ablehnende Verzweigung wirft nun explizit die bestehende AccessDeniedException; Typ und Meldung bleiben erhalten. Null, unauthentifizierte und fremde Principals werden vor dem Rollenlookup abgewiesen. Ein Authentication-Testobjekt, das beim zweiten Lesen null liefert, reproduzierte vorher die NullPointerException; jetzt ist nur ein Lesen nötig.

Der private Session-install-Helfer in SecurityService prüft zusätzlich seine Nichtnull-Invariante vor einer Sessionänderung. Seine bestehenden produktiven Aufrufer liefern bereits erfolgreich erneuerte Authentication-Objekte; dies ist eine defensive Klarstellung, kein behaupteter neuer extern erreichbarer Exploit.

### Avatar-Bildwerte

AvatarDraftStore.Image besitzt jetzt Inhaltsgleichheit und passenden Hashcode für byte[]. Konstruktor und Accessor kopieren das Array, sodass ein Aufrufer weder über die ursprüngliche Eingabe noch über einen zurückgegebenen Snapshot die gespeicherten Draftbytes verändern kann. Die bisher zulässigen null-Komponenten bleiben darstellbar. toString zeigt nur ID und Byteanzahl, keine Bildbytes.

Drei Prüfungen reproduzierten vorher falsche Wertgleichheit beziehungsweise veränderbare Snapshots; alle bestehen jetzt. Der zusätzliche reale Draft-Test verwendet einen eigenen gültigen PNG-Upload und zeigt, dass eine Änderung des abgerufenen Arrays den gespeicherten Originalinhalt nicht mehr beschädigt.

### Kleine Ressourcen-/Stilkorrekturen

- Sieben Folgehinweise aus der Kosmetik: vorhandene zentrale Home-Sicherheitsroute verwendet, zwei vorbereitete Testoperationen als Methodenreferenzen, vier bewusst unbenannte Testressourcen mit var. Nur diese Lebensdauer-Ressourcen erhalten diese Ausnahme; keine allgemeine var-Umstellung.
- Vier konkret gemeldete Streams verwenden try-with-resources. Die Hinweise betreffen ByteArrayOutputStream, den begrenzten FilterOutputStream und zwei Test-ByteArrayInputStreams, nicht BufferedImage. flush/dispose und die bisherige Thread-/Latch-Freigabe bleiben erhalten.
- Der private Bild-Validierungszweig liefert ein leeres Array statt null. validate verwirft diesen Rückgabewert weiterhin; normalize liefert weiterhin das normalisierte PNG.
- Der Upload-Zuschlag wird als long berechnet. Bei der heutigen 5-MiB-Grenze bestand noch kein Überlauf; geändert ist die Zwischenarithmetik als defensive Absicherung.
- Fünf Boxed-Boolean-Stellen verwenden Boolean.TRUE.equals. Der unnötige Mailtest-Nullguard entfällt; vorher wird dieses Feld in derselben Methode bereits unbedingt benutzt.
- Der private Übersetzungsadapter verwendet UnaryOperator<String>. Keine Änderung am öffentlichen Übersetzungskatalog, an der live ausgewerteten Session-Locale oder den festen Testlocales.

## Explizite Zeitsemantik

Die drei verbliebenen now-Aufrufe bei Geburtsdaten, deren maximalem UI-Datum und EmailQueueEntity.createdAt verwenden jetzt explizit ZoneId.systemDefault. Das macht die **bisherige** Server-Kalender-/Mailqueue-Semantik sichtbar; es stellt diese Werte nicht auf UTC oder Europe/Berlin um. Die frühere Instant-Umstellung der Aktivitätszeitpunkte bleibt unverändert. Keine historischen Daten werden gelesen, umgeschrieben oder stillschweigend neu interpretiert. Eine fachliche Festlegung auf eine andere Geburtsdatums-/Mailqueue-Zone wäre ein eigenes Änderungspaket.

## Neun beibehaltene Transaktionshinweise

| Aufruf | Bewertung und Vertrag |
| --- | --- |
| UserService.createAdminUser → save | Bestehende REQUIRED-Transaktion hält Anlage, Validierung und Superadminschutz atomar. |
| UserService.saveUser → save | Äußerer Einstieg besitzt bereits REQUIRED; keine zusätzliche Grenze benötigt. |
| UserService.recordLogin → updateUserStatus | Status und Aktivität gehören zum selben Login-Commit/Rollback. |
| UserService.recordLogin → updateLastActivity | Dieselbe Login-Transaktion und bereits gehaltene Sperren erhalten. |
| UserService.updateUserLocale → save | Localeänderung und Persistenz bleiben ein atomarer REQUIRED-Vorgang. |
| UserService.updateScheduledStatus → updateUserStatus | REQUIRES_NEW liegt bereits außen pro Schedulerbenutzer; inneres REQUIRED soll genau dort mitlaufen. |
| UserNotificationService.searchInbox → inbox | Beide Verträge REQUIRED/readOnly; leere Suche nutzt die bestehende Lesetransaktion. |
| PasswordResetTokenLookupImpl.findByTokenAndStatus → findByToken | Beide REQUIRED; Flush/Refresh gehören zur vorhandenen Passwort-/Tokentransaktion und deren Rollback. |
| UserRepository.findSecurityUser → findCanonicalSecurityUser | Kontextueller False-Positive-Vorschlag: Default-Methode am echten Spring-Data-Repositoryproxy erhält tatsächlich die unabhängige Security-Abfrage. Neuer Test `repositoryDefaultMethodRetainsTheIndependentSecurityRead` sieht trotz geflushtem uncommittetem Disable den committed aktivierten Benutzer. |

Die ersten acht sind bewusst gleiche Transaktionsumfänge, keine Behauptung, dass Spring-Selbstaufrufe allgemein neue Advice auslösen. Ein pauschales REQUIRES_NEW würde hier Sperr-/Rollbackverträge verändern und zusätzliche Verbindungen beanspruchen. Keine Statusänderungen am Server vorgenommen.

## Weitere acht beibehaltene Hinweise

- Zwei defensive Nullguards für nullable JdbcTemplate-Metadatenresultate bleiben. Vier neue Parameterfälle (beide Schema-Updater mit null beziehungsweise VARCHAR) beweisen, dass kein DDL ausgelöst wird. Diese konservativen Guards werden nicht nur für die Sonar-Zahl entfernt.
- Der Queue-Claim-Nullguard ist ein False-Positive-Vorschlag: der Callback kann bei übersprungenem Claim ausdrücklich null liefern. Die Prüfung vor dem Versand bleibt notwendig.
- Die öffentliche Consumer<Long>-Callback-API des UserRoleBroadcaster bleibt erhalten. LongConsumer würde einen primitiven Eingabevertrag und Unboxing erzwingen. Ohne fachlichen Nutzen wird diese bestehende API hier nicht verändert.
- Beide S2077-SQL-Stellen verwenden feste Bezeichner und ausschließlich eigene Enum.name-Werte. Kein externer Eingabepfad in die DDL-Zeichenfolge; False-Positive-Vorschläge. Die vorhandenen H2-Schema-Upgradefälle wurden nachgetestet.
- Beide PASSWORD_REGEX-Meldungen sind False-Positive-Vorschläge: Regeltext beziehungsweise Alias, keine Credentials oder fest eingebauten Passwörter.

Diese acht plus die neun oben ergeben die 17 ohne Produktionsänderung beibehaltenen Findings.

## Benutzerermittlung: Verbesserung und verbleibender Review

Die übermittelte E-Mail-Adresse wurde aus UsernameNotFoundException entfernt; der private Mapper benötigt diesen Parameter dadurch nicht mehr. Der konkrete DaoAuthenticationProvider mit demselben Passwortservice und Encoder meldet für ein fehlendes Konto und ein falsches Passwort denselben BadCredentialsException-Typ und dieselbe Meldung. Beide Loginwege verwenden den gemeinsamen Passwortanbieter. Vorhandene Login-/Rate-Limit-/Admission-Verträge bleiben erhalten.

Die Framework-Ausnahme bei nicht gefundenen Benutzern bleibt ein erforderlicher interner Vertrag. Deshalb wird nicht behauptet, S5804 verschwinde automatisch oder dass sämtliche Anmelde-/Registrierungs-Timingkanäle weltweit ausgeschlossen wären. Die getestete Gleichheit von Fehlertyp und Meldung ist keine Konstantzeitmessung. Eine Accepted-Entscheidung für diese konkret geschützte Grenze bleibt dem Benutzer vorbehalten.

## Tatsächlich ausgeführte Prüfungen

| Prüfung | Ergebnis |
| --- | --- |
| Java / Maven | JDK 25.0.4.1, Maven Wrapper 3.9.16 |
| Neue Charakterisierung vor Produktionskorrektur | 16 Fälle: 6 reproduzierte Failures, 0 Errors/Skips |
| Derselbe gezielte Nachtest nach Korrektur | 16/16 erfolgreich |
| Breitere Sicherheits-/Persistenz-/UI-Komponentenrunde | **591/591 erfolgreich**, 0 Failures/Errors/Skips |
| Vollständiger Maven-Lauf mit Produktionsprofil und verify | **2000/2000 erfolgreich**, 0 Failures/Errors/Skips |
| Produktions-JAR | Erfolgreich gebaut und repackaged |
| Spotless apply/check, Checkstyle nach letzter Java-Änderung | Erfolgreich, keine Stilabweichungen |
| Diff-/Whitespaceprüfung | Erfolgreich |

17 zusätzliche Testausprägungen gegenüber dem Ausgangsstand mit 1983. Die 591 gezielten Fälle und die neuen Tests sind Teil der 2000 und werden nicht nochmals als zusätzliche eindeutige Gesamtmenge gezählt. Der finale Produktionslauf endete am 20.10.2026 um 20:13:38 +02:00. Surefire-XML-Summen und Maven-Zusammenfassung stimmen überein.

Frische, ausschließlich aus diesem vollständigen Lauf erzeugte JaCoCo-Coverage: **10231/11654 Zeilen = 87,79 %**, **2979/3866 Zweige = 77,06 %**, 219 Anwendungsklassen. Kein neuer Browser-UI-Lauf, kein Dockerimage, kein eigenständiger neuer Browser-Helfer-/Frontendtestlauf. Vorhandene DE-/EN-Komponententests sind enthalten. Browserfreigabe lag für dieses Paket nicht vor; es wurde keine Browserabnahme behauptet oder ersetzt.

## Risiken, Grenzen und Einführung

Die defensiven Image-Kopien verursachen zusätzliche begrenzte Bytearray-Allokationen bei Snapshot-Konstruktion und -Abruf. Die bestehenden Upload-/Draftgrenzen (5 MiB pro Original, höchstens 16 Drafts) bleiben erhalten; es wurde kein Last-/Heapbenchmark behauptet. Ein extern aus einer laufenden Transaktion gerufener Einzel-Metadatenabruf benötigt wegen REQUIRES_NEW eine eigene Verbindung, entsprechend dem bestehenden Sammelvertrag. Innerhalb open entsteht hierfür bewusst keine weitere Metadaten-Lesetransaktion.

Keine Theme-/Layout-/Sprachänderungen. Um die Java-Änderungen in die laufende Anwendung zu übernehmen, das neu gebaute Produktions-JAR verwenden und die Anwendung neu starten; Browser-Refresh genügt nicht. Es wurde kein bestehender App-/Containerprozess gestartet, gestoppt oder neu erstellt. Keine bestehenden Datenbanken zurückgesetzt oder reale E-Mails versendet. Die Tests benutzen eigene H2-/SMTP-/HTTP-Fixtures.

Große Komplexitätsrefactorings und vollständige Vaadin-UI-/Sessionserialisierung bleiben außerhalb dieses Pakets. Die 79 übrigen Ausgangsfindings wurden nicht pauschal als unproblematisch eingestuft.

Keine Commits, Pushes, Merges, Dependency-/Lockdateiänderungen, Sonar-Scans oder Server-Statusänderungen. Die reviewbaren Änderungen liegen uncommittet auf main. Nächster Schritt ist der vom Benutzer gestartete neue Scan/Export; die CSV trennt Codebearbeitung von lokalem Review und darf nicht als serverseitiger Abschlussnachweis verwendet werden.

## Technische Grundlagen

[Spring-Transaktionsproxy und Selbstaufrufe](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html), [DaoAuthenticationProvider](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/dao-authentication-provider.html), [BufferedImage und seine Schnittstellen](https://docs.oracle.com/en/java/javase/25/docs/api/java.desktop/java/awt/image/BufferedImage.html). Die konkreten Einstiegs-/Rollback-/Proxyentscheidungen oben beruhen zusätzlich auf dem tatsächlichen Anwendungscode und den genannten Tests.
