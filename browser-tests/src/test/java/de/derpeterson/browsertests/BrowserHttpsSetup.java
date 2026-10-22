package de.derpeterson.browsertests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

/** Ausschließlich explizite, beaufsichtigte Einrichtung bzw. Rückbau der Browser-Test-CA. */
final class BrowserHttpsSetup {
  private BrowserHttpsSetup() {}

  static void setup(Path repo) throws Exception {
    Path directory = BrowserTestCa.directory(repo);
    BrowserTestCa.initialize(directory);
    try (BrowserTestCa.Lease lease = BrowserTestCa.lease(directory, false)) {
      X509Certificate root = BrowserTestCa.ownedCertificate(directory);
      if (BrowserHttps.ownsTrust(directory)) {
        BrowserTestCa.requireOwnershipMarker(directory, root);
        if (BrowserTestCa.trusted(root)) {
          BrowserTestCa.requireTrusted(directory);
          System.out.println("Browser HTTPS is already configured for this Windows user.");
          return;
        }
        trust(repo, directory, "Remove");
      }
      System.out.println(
          "One-time browser test CA setup: " + BrowserTestCa.fingerprint(root, "SHA-1"));
      try {
        trust(repo, directory, "Add");
        BrowserTestCa.requireTrusted(directory);
      } catch (Exception error) {
        if (BrowserHttps.ownsTrust(directory)) {
          try {
            trust(repo, directory, "Remove");
          } catch (Exception cleanupError) {
            error.addSuppressed(cleanupError);
          }
        }
        throw error;
      }
    }
    System.out.println("Browser HTTPS configured. Normal test runs do not modify Windows trust.");
  }

  static void remove(Path repo) throws Exception {
    Path directory = BrowserTestCa.directory(repo);
    if (!Files.exists(directory)) {
      System.out.println("No browser test CA is configured.");
      return;
    }
    try (BrowserTestCa.Lease lease = BrowserTestCa.lease(directory, false)) {
      X509Certificate root = BrowserTestCa.ownedCertificate(directory);
      if (BrowserHttps.ownsTrust(directory)) {
        BrowserTestCa.requireOwnershipMarker(directory, root);
        trust(repo, directory, "Remove");
      } else if (BrowserTestCa.trusted(root)) {
        throw new IOException("Refusing to remove Windows trust without its ownership marker");
      }
      // Nur eigene bekannte Dateien löschen, unter derselben exklusiven Einrichtungssperre.
      for (String file : List.of("root.p12", "password", "root.crt", "root.sha256")) {
        Files.deleteIfExists(directory.resolve(file));
      }
    }
    Files.deleteIfExists(directory.resolve("ca.lock"));
    boolean empty;
    try (Stream<Path> remaining = Files.list(directory)) {
      empty = remaining.findAny().isEmpty();
    }
    if (empty) {
      Files.delete(directory);
    }
    System.out.println("Owned browser test CA trust and private keys removed.");
  }

  private static void trust(Path repo, Path directory, String action) throws Exception {
    Process command =
        new ProcessBuilder(BrowserHttps.trustCommand(repo, directory, action)).inheritIO().start();
    BrowserHttps.awaitTrustCommand(command, Duration.ofSeconds(120));
  }
}
