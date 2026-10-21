package de.derpeterson.app.manual;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Versendet Mailbeispiele an ein eigenes Postfach zur menschlichen Sichtprüfung.
 *
 * <p>Start in IntelliJ:
 *
 * <ol>
 *   <li>Diese Klasse einmal über das grüne Dreieck starten. Ohne Aktivierung wird der Test
 *       übersprungen; die JUnit-Startkonfiguration ist danach vorhanden.
 *   <li>In dieser Startkonfiguration unter VM options (nicht Program arguments) {@code
 *       -Dmail.preview.send=true} eintragen. Nur der exakte Wert {@code true} aktiviert den
 *       Versand; kein {@code @Disabled} entfernen.
 *   <li>Unter Environment variables die folgenden Platzhalter durch eigene Angaben ersetzen:
 *       <pre>{@code
 * MAIL_PREVIEW_TO=deine.empfaengeradresse@example.com
 * MAIL_PREVIEW_LANGUAGES=de,en
 * MAIL_PREVIEW_FROM=deine.absenderadresse@example.com
 * MAIL_PREVIEW_SMTP_HOST=smtp.dein-anbieter.de
 * MAIL_PREVIEW_SMTP_PORT=587
 * MAIL_PREVIEW_SMTP_USERNAME=DEIN_SMTP_BENUTZERNAME
 * MAIL_PREVIEW_SMTP_PASSWORD=DEIN_SMTP_ODER_APP_PASSWORT
 * MAIL_PREVIEW_SECURITY=starttls
 *
 * }</pre>
 *   <li>Konfiguration erneut starten und die eingegangenen Mails einschließlich Spamordner im
 *       gewünschten Mailprogramm ansehen. Jeder aktivierte Start versendet die Beispiele erneut.
 * </ol>
 *
 * <p>Sprachen: {@code de}, {@code en} oder {@code de,en} (Standard). Eine Sprache ergibt sieben
 * Mails, beide ergeben 13. Enthalten sind alle fünf HTML-Vorlagen, die lokalisierte Admin-Testmail
 * und einmal der technische, auch in der Anwendung englische Klartext-Fehlerhinweis.
 *
 * <p>SMTP gemäß Anbieter konfigurieren: Standard ist Port 587 mit {@code starttls}; bei direktem
 * TLS Port 465 und {@code ssl} verwenden. {@code none} ist ausdrücklich unverschlüsselter Versand.
 * {@code MAIL_PREVIEW_SMTP_AUTH} ist standardmäßig {@code true}; nur bei einem passenden eigenen
 * Relay/Testserver auf {@code false} setzen. Absender muss für den SMTP-Zugang erlaubt sein.
 * Zugangsdaten und reale Adressen ausschließlich lokal in der Startkonfiguration halten, nicht in
 * Java-Dateien oder versionierten Konfigurationen.
 *
 * <p>Die Anwendung und Docker müssen nicht laufen. Es werden weder {@code .env} noch
 * Anwendungsdatenbanken geladen. Kontodaten sind erfunden und Links zeigen auf {@code
 * https://preview.invalid/}; keine gültigen Tokens oder Kontoänderungen. Ein grüner Test bestätigt
 * nur die SMTP-Annahme, nicht Zustellung oder gutes Aussehen. Bei einem Fehler stoppt der Test ohne
 * Retry; vor erneutem Start bereits eingegangene Mails prüfen.
 *
 * <p>Alternativ mit denselben lokalen Umgebungsvariablen vom Projektroot aus starten:
 *
 * <pre>{@code
 * .\mvnw.cmd '-Dtest=ManualEmailPreviewIT' '-Dmail.preview.send=true' test
 * }</pre>
 */
@EnabledIfSystemProperty(named = "mail.preview.send", matches = "true")
class ManualEmailPreviewIT {
  @Test
  @DisplayName("Mailbeispiele für die menschliche Sichtprüfung ausdrücklich versenden")
  void sendExamplesToTheExplicitRecipient() {
    MailVisualPreview preview = MailVisualPreview.prepare(System::getenv);
    preview.send(preview.createSender());
    System.out.println(
        "Mailbeispiele vom SMTP-Transport angenommen: "
            + preview.examples().size()
            + ". Zustellung und Aussehen jetzt im eigenen Postfach prüfen.");
  }
}
