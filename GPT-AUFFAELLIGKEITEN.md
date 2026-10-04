# Auffälligkeit Nr. 1: Status-/Broadcast-Widerspruch — behoben

## Ursache

`UserService.updateUserStatus` verwendete den angeforderten `newStatus` für das
Ereignis. `UserEntity.setAutomaticStatus` kann diesen Wechsel jedoch bei manuell
gesetztem `EMPLOYED` oder `OFFLINE` ablehnen. Das Ereignis konnte dadurch einen
anderen Status melden als das gespeicherte Benutzerobjekt.

## Änderung und Entscheidung

Nach erfolgreichem Speichern wird der tatsächlich resultierende Entity-Status
gelesen. Nur wenn er vom ursprünglichen Status abweicht, wird ein Ereignis mit
diesem Status gesendet. Entity-Regeln und Broadcaster bleiben unverändert.

Ein abgelehnter oder statusgleicher Aufruf erzeugt damit kein Statuswechselereignis.
Dies entspricht dem bestehenden Empfänger `UserActivityAwareView`: Er führt die
Aktualisierung bereits ausschließlich bei unterschiedlichen Ereignisstatus aus.
Die untersuchten Empfänger benötigen kein Ereignis für eine reine Änderung des
`statusManuallySet`-Flags. Das Speichern bleibt trotzdem erhalten, denn auch bei
statusgleichem Aufruf kann dieses Flag gesetzt oder zurückgesetzt werden.

Ein formaler fachlicher Ereignisvertrag ist im untersuchten Code nicht definiert;
die Entscheidung folgt dem vorhandenen Empfängerverhalten. Neue Empfänger, die
Flag-Änderungen benötigen, müssten dafür einen gesonderten Vertrag erhalten.
`UserPopoverMenu` setzt den Status bereits vor dem Service-Aufruf; dieser bestehende
Aufruferablauf und die allgemeine Transaktions-/Listenerfehlerbehandlung werden
im Rahmen dieser eng begrenzten Korrektur nicht verändert.

## Absicherung in `UserServiceTest.StatusUpdates`

- Manueller und automatischer erfolgreicher Wechsel: Entity-Status, Flag und
  vollständige Ereignisdaten werden geprüft; manuell zusätzlich Save vor Broadcast.
- Abgelehnte automatische Wechsel bei manuellem `EMPLOYED` und `OFFLINE`:
  Status und Flag bleiben erhalten, Save erfolgt, kein Broadcast.
- Statusgleiche manuelle und automatische Aufrufe: Flag-Änderung wird gespeichert,
  kein Broadcast.
- Save-Fehler bei manuellen, automatischen und abgelehnten Wechseln:
  identische Ausnahme wird weitergegeben, kein Broadcast.
- Broadcast-Fehler nach erfolgreichem Save: Ausnahme wird weiterhin weitergegeben.

Die Tests sind isolierte Unit-Tests ohne Spring-Kontext, Datenbank oder Browser.
Die ursprünglich erwähnte Backlog-Datei war im aktuellen Checkout nicht vorhanden;
dieses Dokument behandelt ausschließlich die mitgelieferte Auffälligkeit Nr. 1.
