# Zwei-Faktor-Bestätigung für sensible Kontoaktionen

## Auftrag und Abgrenzung

Als nächste separate Umsetzung vereinbart. Die jetzige Google-/Microsoft-Umsetzung
enthält nur die ausdrücklich akzeptierte Google-Zwischenlösung. Noch keine
2FA-Komponenten, Datenfelder oder Ersatzprüfungen als Provisorium einbauen.
Beim Featurestart die konkrete Planung und Abnahme gemäß dem Umsetzungsskill vorlegen.

## Problem und Ziel

Der erneute Google-OIDC-Durchlauf bestätigt das bereits verbundene Konto, erzwingt
aber keine neue aktive Google-Anmeldung. Eine offene Portal- und Google-Sitzung
kann daher zur Bestätigung sensibler Änderungen genügen. Das ist eine bekannte,
vorübergehend akzeptierte Sicherheitsschwäche, kein gleichwertiger MFA-Nachweis.
Aktueller Vertrag und Anbietergrenze: [Externe Anmeldung](../externe-anmeldung.md#google-zwischenlösung-und-bekannte-sicherheitsschwäche).

Sobald ein Benutzer 2FA eingerichtet hat, bestätigt er sensible Kontoaktionen mit
dieser Methode. Ein erneuter Google-Durchlauf oder das Portalpasswort allein darf
diese Anforderung dann nicht umgehen. Das gilt auch bei deaktivierten Anbietern
oder einer Anmeldung über einen anderen Zugang zum selben Konto.

## Geplanter Lösungsweg

- Standardisierten Authenticator-Code (TOTP) als Bestätigungsmethode einführen.
  Einrichtung erst nach hinreichender Identitätsprüfung abschließen und durch
  einen ersten gültigen Code bestätigen. Eine offene Sitzung allein darf die
  Einrichtung, den Austausch oder das Entfernen eines Faktors nicht autorisieren.
- Für bestehende Google-only-Konten ohne unabhängigen Nachweis einen ausdrücklich
  geprüften Einrichtungs-/Wiederherstellungsweg festlegen. Die Google-Zwischenlösung
  nicht stillschweigend zur gleichwertigen sicheren Erstregistrierung eines Faktors erklären.
- Ersatzcodes, Geräteverlust, Faktorwechsel und Entfernen samt Missbrauchsschutz
  gemeinsam planen. Keine schwächere Wiederherstellung als versteckten Umgehungsweg zulassen.
- Die bestehende Bindung an Konto, Zugangsversion, Sitzung und konkrete Aktion sowie
  die einmalige Fünf-Minuten-Freigabe weiterverwenden. Prüfversuche begrenzen,
  Geheimnisse geschützt speichern und Sicherheitsänderungen nachvollziehbar melden.
- Festlegen, ob 2FA zugleich bei der normalen Anmeldung verlangt wird und welche
  Einrichtungsregeln für Benutzer und Administratoren gelten. Dieser zusätzliche
  Anmeldeumfang ist beim Start zu entscheiden; jetzt ist die Priorität für sensible
  Aktionen vereinbart. Konten ohne eingerichtete 2FA und die spätere vollständige
  Ablösung der Google-Zwischenlösung ausdrücklich behandeln.
- Bestehende Vaadin-/Cosmos-Bausteine und DE/EN verwenden. Keine freie Neugestaltung
  der Kontosicherheit aus diesem Auftrag ableiten.

## Abnahme

Mit und ohne Portalpasswort sowie mit Google, Microsoft und beiden Verbindungen
prüfen. Eingerichtete 2FA darf weder über einen anderen Anmeldeweg noch über
direkte Rücksprünge oder Wiederherstellung umgangen werden. Fehlende, falsche,
abgelaufene und wiederverwendete Codes sowie Zweck-, Konto-, Versions- und
Sitzungswechsel prüfen. Automatisierter Bedarf steht zentral in
[Testbedarf](../testbedarf.md); keine parallele Testübergabe führen.

Den Eintrag in [BUGS.md](../../BUGS.md) erst nach Umsetzung, passenden Nachtests und
bewusster Entscheidung zum verbleibenden Risiko von Konten ohne 2FA schließen.
Den erledigten Plan nach Abnahme in die aktuelle Fachdokumentation überführen.

Grundlagen: [Google-Anbietergrenze](https://developers.google.com/identity/siwg/security-bundle#authentication_time),
[OWASP zu MFA und Wiederherstellung](https://cheatsheetseries.owasp.org/cheatsheets/Multifactor_Authentication_Cheat_Sheet.html).
