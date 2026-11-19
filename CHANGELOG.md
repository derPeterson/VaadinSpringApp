# Änderungen / Changelog

## [Unreleased]

### DE

- Eigene Wiederherstellungsanfragen in einer scrollbaren Liste mit höchstens fünf
  sichtbaren Einträgen durchsuchen; Strg+Klick hebt die Ticketauswahl auf und erhält Antwortentwürfe.
- Die Avatarvorschau einer Mitteilung öffnet eine vollständige Empfängerliste mit
  Suche nach Name und E-Mail-Adresse, auch ohne „+N“-Überlauf.
- Konfiguration, Mail-Test, Tickets und Mitteilungsentwürfe erklären Eingabefehler
  konkret beim Speichern, Senden oder Öffnen der Vorschau.
- Wiederherstellungsanleitungen im Ticket nach 60 Sekunden erneut anfordern;
  offene zugehörige Mailaufträge verhindern Doppelversand. Entwicklerkonten
  erhalten die Anleitung ausschließlich im Ticketverlauf.
- In der Zwei-Faktor-Bestätigung stehen App-Code und Ersatzcode im Kopf der
  Seite oder des Dialogs. Beim Methodenwechsel werden angefangene Eingaben geleert.
- Im Admin-Ticketsystem öffnet das Vorgangsdetail bei wenig Platz über der
  Liste. Beim Zurückkehren zur Liste bleiben ungesendete Antwortentwürfe erhalten.

### EN

- Browse personal recovery requests in a scrollable list with up to five visible
  entries; Ctrl-click clears the selected ticket while preserving reply drafts.
- The avatar preview of an announcement opens the complete recipient list with
  search by name and email address, even without a “+N” overflow.
- Configuration, mail tests, tickets and announcement drafts explain invalid input
  when saving, sending or opening the preview.
- Request recovery instructions again after 60 seconds within a support ticket;
  pending associated emails prevent duplicate delivery. Development accounts
  receive the instructions only in the ticket conversation.
- Two-factor verification shows the app code and recovery code tabs in the page
  or dialog header. Switching methods clears partial entries.
- In the admin ticket system, ticket details open over the list on narrow screens.
  Returning to the list preserves unsent reply drafts.

## [0.1.0] - 2026-11-18

### DE

- Benutzerkonten registrieren, E-Mail-Adressen bestätigen und Passwörter zurücksetzen.
- Persönliche Daten, Profilbild, Anmeldeanbieter und Zwei-Faktor-Authentifizierung verwalten.
- Eigene Geräte und Sitzungen verwalten sowie Konten selbst deaktivieren, reaktivieren oder löschen.
- Persönliche Benachrichtigungen lesen und bei Zugangsproblemen Wiederherstellungshilfe anfordern.
- Als berechtigter Administrator Benutzer, Benachrichtigungen, Konfiguration und Mailqueue verwalten.

### EN

- Register accounts, verify email addresses and reset passwords.
- Manage personal details, profile pictures, sign-in providers and two-factor authentication.
- Manage devices and sessions, and deactivate, reactivate or delete your own account.
- Read personal notifications and request recovery assistance for access problems.
- As an authorized administrator, manage users, notifications, configuration and the mail queue.
