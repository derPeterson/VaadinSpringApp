package de.derpeterson.app.helper.logging;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class LogSafetyTest {
  @Test
  void preservesFailureTypesAndFramesWithoutAnyFreeMessages() {
    IOException cause = new IOException("token=secret@example.com");
    SQLException original = new SQLException("INSERT password=secret", cause);
    original.addSuppressed(new IllegalStateException("body and credentials"));
    Throwable safe = LogSafety.safeFailure(original);
    String trace = trace(safe);
    assertTrue(trace.contains(SQLException.class.getName()));
    assertTrue(trace.contains(IOException.class.getName()));
    assertTrue(trace.contains(IllegalStateException.class.getName()));
    assertArrayEquals(original.getStackTrace(), safe.getStackTrace());
    assertArrayEquals(cause.getStackTrace(), safe.getCause().getStackTrace());
    assertFalse(trace.contains("secret"));
    assertFalse(trace.contains("password="));
    assertFalse(trace.contains("credentials"));
    assertNotSame(original, safe);
    assertSame(cause, original.getCause());
    assertEquals("INSERT password=secret", original.getMessage());
  }

  @Test
  void nullRemainsNull() {
    assertNull(LogSafety.safeFailure(null));
  }

  @Test
  void neverEvaluatesThrowableMessageOrToString() {
    RuntimeException original =
        new RuntimeException() {
          @Override
          public String getMessage() {
            throw new AssertionError("must not read secret");
          }

          @Override
          public String toString() {
            throw new AssertionError("must not read secret");
          }
        };
    assertEquals(original.getClass().getName(), LogSafety.safeFailure(original).getMessage());
  }

  @Test
  void cyclicCausesStayPrintableAndSanitized() {
    RuntimeException first = new RuntimeException("private1");
    RuntimeException second = new RuntimeException("private2", first);
    first.initCause(second);
    String trace = trace(LogSafety.safeFailure(first));
    assertTrue(trace.contains("CIRCULAR REFERENCE"));
    assertFalse(trace.contains("private"));
  }

  @Test
  void deepCausesAndSuppressedFailuresAreBounded() {
    Throwable chain = new IOException("secret");
    for (int i = 0; i < 100; i++) {
      chain = new RuntimeException("secret", chain);
    }
    for (int i = 0; i < 100; i++) {
      chain.addSuppressed(new IOException("secret"));
    }
    Throwable safe = LogSafety.safeFailure(chain);
    int depth = 0;
    for (Throwable current = safe; current != null; current = current.getCause()) {
      depth++;
    }
    assertEquals(9, depth);
    assertEquals(8, safe.getSuppressed().length);
    assertFalse(trace(safe).contains("secret"));
  }

  private String trace(Throwable failure) {
    StringWriter text = new StringWriter();
    failure.printStackTrace(new PrintWriter(text));
    return text.toString();
  }
}
