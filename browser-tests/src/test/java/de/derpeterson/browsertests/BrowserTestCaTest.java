package de.derpeterson.browsertests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class BrowserTestCaTest {
  @TempDir static Path rootDirectory;
  @TempDir Path directory;
  static Path authority;

  @BeforeAll
  static void createUntrustedFixtureCa() throws Exception {
    authority = rootDirectory.resolve("fixture authority with spaces");
    BrowserTestCa.initialize(authority);
  }

  @Test
  void parallelWorkersGetDifferentSignedNonCaIdentitiesWithoutChangingAuthority() throws Exception {
    byte[] originalStore = Files.readAllBytes(authority.resolve("root.p12"));
    byte[] originalPassword = Files.readAllBytes(authority.resolve("password"));
    Path first = directory.resolve("worker one"), second = directory.resolve("worker two");
    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      Future<?> firstIssuance =
          executor.submit(
              () -> {
                BrowserTestCa.issue(authority, first);
                return null;
              });
      Future<?> secondIssuance =
          executor.submit(
              () -> {
                BrowserTestCa.issue(authority, second);
                return null;
              });
      firstIssuance.get(60, TimeUnit.SECONDS);
      secondIssuance.get(60, TimeUnit.SECONDS);
    }
    X509Certificate root = BrowserTestCa.certificate(authority.resolve("root.crt"));
    X509Certificate one = BrowserTestCa.certificate(first.resolve("server.crt"));
    X509Certificate two = BrowserTestCa.certificate(second.resolve("server.crt"));
    one.verify(root.getPublicKey());
    two.verify(root.getPublicKey());
    assertNotEquals(one.getSerialNumber(), two.getSerialNumber());
    assertNotEquals(one.getPublicKey(), two.getPublicKey());
    assertEquals(-1, one.getBasicConstraints());
    assertEquals(List.of("1.3.6.1.5.5.7.3.1"), one.getExtendedKeyUsage());
    assertTrue(one.getSubjectAlternativeNames().contains(List.of(7, "127.0.0.1")));
    assertTrue(one.getNotAfter().toInstant().isBefore(Instant.now().plus(25, ChronoUnit.HOURS)));
    assertArrayEquals(originalStore, Files.readAllBytes(authority.resolve("root.p12")));
    assertArrayEquals(originalPassword, Files.readAllBytes(authority.resolve("password")));
    for (Path worker : List.of(first, second)) {
      assertTrue(Files.isRegularFile(worker.resolve("server.key")));
      assertFalse(Files.exists(worker.resolve("server.p12")));
      assertFalse(Files.exists(worker.resolve("password")));
      assertFalse(Files.exists(worker.resolve("root.p12")));
      assertFalse(Files.exists(worker.resolve("server.csr")));
      BrowserTestCa.cleanupWorker(worker);
      assertFalse(Files.exists(worker.resolve("server.key")));
      assertTrue(Files.exists(worker.resolve("server.crt")));
    }
  }

  @Test
  void cleanupRemovesOnlyKnownPrivateWorkerFilesAndIsIdempotent() throws Exception {
    for (String name :
        List.of(
            "server.key",
            "server.p12",
            "server.csr",
            "issued.crt",
            "server.crt",
            "root.crt",
            "unrelated.txt")) {
      Files.writeString(directory.resolve(name), "fixture");
    }
    BrowserTestCa.cleanupWorker(directory);
    BrowserTestCa.cleanupWorker(directory);
    assertFalse(Files.exists(directory.resolve("server.key")));
    assertFalse(Files.exists(directory.resolve("server.p12")));
    assertTrue(Files.exists(directory.resolve("server.crt")));
    assertTrue(Files.exists(directory.resolve("root.crt")));
    assertEquals("fixture", Files.readString(directory.resolve("unrelated.txt")));
    assertTrue(Files.exists(authority.resolve("root.p12")));
  }

  @Test
  void missingSetupCannotCreateFilesOrStartAnApplication() throws Exception {
    Path module = Files.createDirectories(directory.resolve("browser-tests"));
    String previousLanguage = System.getProperty("browser.tests.language");
    String previousWorkers = System.getProperty("browser.tests.workers");
    try {
      IOException failure =
          assertThrows(
              IOException.class,
              () ->
                  BrowserTestRunner.main(
                      new String[] {module.toString(), "--suite", "infrastructure", "--no-build"}));
      assertTrue(failure.getMessage().contains("browser-tests-zertifikat-einrichten.bat"));
      assertFalse(Files.exists(directory.resolve("data")));
      assertFalse(Files.exists(module.resolve("runs")));
    } finally {
      restore("browser.tests.language", previousLanguage);
      restore("browser.tests.workers", previousWorkers);
    }
  }

  @Test
  void incompleteSetupIsNotSilentlyRegeneratedOrTakenOver() throws Exception {
    Files.writeString(directory.resolve("unrelated.txt"), "keep");
    assertThrows(IOException.class, () -> BrowserTestCa.initialize(directory));
    assertEquals("keep", Files.readString(directory.resolve("unrelated.txt")));
    assertFalse(Files.exists(directory.resolve("root.p12")));
  }

  @Test
  void expiredAndNotYetValidAuthorityIsRejectedBeforeIssuance() {
    assertThrows(
        CertificateExpiredException.class,
        () -> BrowserTestCa.validate(authority, Instant.now().plus(1096, ChronoUnit.DAYS)));
    assertThrows(
        CertificateNotYetValidException.class,
        () -> BrowserTestCa.validate(authority, Instant.now().minus(1, ChronoUnit.DAYS)));
  }

  @Test
  void activeRunPreventsConcurrentSetupOrRemoval() throws Exception {
    try (BrowserTestCa.Lease lease = BrowserTestCa.lease(authority, true)) {
      assertThrows(IOException.class, () -> BrowserTestCa.lease(authority, false));
    }
    try (BrowserTestCa.Lease lease = BrowserTestCa.lease(authority, false)) {
      assertTrue(lease.lock().isValid());
    }
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void untrustedFixtureCaIsRejectedWithoutImportOrMarkerCreation() throws Exception {
    X509Certificate root = BrowserTestCa.ownedCertificate(authority);
    assertFalse(BrowserTestCa.trusted(root));
    assertThrows(IOException.class, () -> BrowserTestCa.requireTrusted(authority));
    assertFalse(Files.exists(authority.resolve("root.crt.owned")));
    assertFalse(BrowserTestCa.trusted(root));
  }

  @Test
  void hardWorkerAbortLeavesOnlyOwnedPrivateFilesForParentCleanup() throws Exception {
    Path worker = directory.resolve("aborted worker");
    Process process =
        new ProcessBuilder(
                BrowserParallelWorkers.javaExecutable(),
                "-cp",
                System.getProperty("java.class.path"),
                IssuingWorker.class.getName(),
                authority.toString(),
                worker.toString())
            .redirectErrorStream(true)
            .start();
    try {
      assertEquals("ready", process.inputReader().readLine());
      assertTrue(Files.exists(worker.resolve("server.key")));
      BrowserParallelWorkers.stopWorker(process);
      assertFalse(process.isAlive());
      BrowserTestCa.cleanupWorker(worker);
      assertFalse(Files.exists(worker.resolve("server.key")));
      assertTrue(Files.exists(authority.resolve("root.p12")));
    } finally {
      BrowserHttps.stopBeforeTrustCleanup(process);
      BrowserTestCa.cleanupWorker(worker);
    }
  }

  public static class IssuingWorker {
    public static void main(String[] args) throws Exception {
      BrowserTestCa.issue(Path.of(args[0]), Path.of(args[1]));
      System.out.println("ready");
      Thread.sleep(60000);
    }
  }

  private static void restore(String key, String value) {
    if (value == null) {
      System.clearProperty(key);
    } else {
      System.setProperty(key, value);
    }
  }
}
