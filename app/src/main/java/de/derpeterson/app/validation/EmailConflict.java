package de.derpeterson.app.validation;

import java.util.Locale;
import org.hibernate.exception.ConstraintViolationException;

/** Recognizes only the named email uniqueness constraint, never arbitrary integrity failures. */
public final class EmailConflict {
  private EmailConflict() {}

  public static boolean isDuplicate(Throwable operationFailure) {
    for (Throwable failureCause = operationFailure;
        failureCause != null;
        failureCause = failureCause.getCause()) {
      if (failureCause instanceof ConstraintViolationException constraintViolation) {
        String constraintName = constraintViolation.getConstraintName();
        if ("23505".equals(constraintViolation.getSQLState()) && constraintName != null) {
          // H2 exposes the constraint plus backing index/table; other dialects
          // may expose the exact constraint name. Do not match message text.
          String normalizedConstraintName = constraintName.toLowerCase(Locale.ROOT);
          if (normalizedConstraintName.equals("uk_users_email")
              // Optionaler Schema-Präfix (?:...)?; dann exakt uk_users_email index, Indexname und
              // optionaler Tabellenzusatz.
              // Erkennt nur die benannte Constraint in H2-Metadaten, keine beliebigen
              // Fehlermeldungen.
              || normalizedConstraintName.matches(
                  "(?:[a-z0-9_]+\\.)?uk_users_email index [a-z0-9_.]+(?: on .*)?")) {
            return true;
          }
        }
      }
    }

    return false;
  }
}
