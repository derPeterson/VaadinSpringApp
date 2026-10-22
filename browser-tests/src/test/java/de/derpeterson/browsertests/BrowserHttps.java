package de.derpeterson.browsertests;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/** Nur die eigene CA wird vertraut; Hostname und Zertifikatskette werden vollständig geprüft. */
final class BrowserHttps {
  private BrowserHttps() {}

  static List<String> trustCommand(Path repo, Path directory, String action) {
    if (!System.getProperty("os.name").startsWith("Windows")) {
      throw new IllegalStateException("Local HTTPS browser trust currently requires Windows");
    }
    return List.of(
        "powershell.exe",
        "-NoProfile",
        "-NonInteractive",
        "-ExecutionPolicy",
        "RemoteSigned",
        "-File",
        repo.resolve("scripts/https/trust.ps1").toString(),
        "-Action",
        action,
        "-Certificate",
        directory.resolve("root.crt").toString(),
        "-Intermediate",
        directory.resolve("intermediate.crt").toString());
  }

  static boolean ownsTrust(Path directory) {
    return Files.isRegularFile(directory.resolve("root.crt.owned"));
  }

  static void awaitTrustCommand(Process command, Duration timeout)
      throws IOException, InterruptedException {
    try {
      if (!command.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
        throw new IOException("Windows certificate confirmation timed out after " + timeout);
      }
      if (command.exitValue() != 0) {
        throw new IOException("Certificate command failed with exit code " + command.exitValue());
      }
    } finally {
      stopBeforeTrustCleanup(command);
    }
  }

  /** Einen noch wartenden Import beenden, bevor dessen Trust-Einträge entfernt werden. */
  static void stopBeforeTrustCleanup(Process command) throws IOException, InterruptedException {
    if (command != null && command.isAlive()) {
      command.destroyForcibly();
      if (!command.waitFor(10, TimeUnit.SECONDS)) {
        throw new IOException(
            "Certificate command remains alive; trust cleanup cannot safely begin");
      }
    }
  }

  static void awaitReady(String origin, Path rootCertificate) throws Exception {
    URI target = URI.create(origin + "/login");
    if (!"https".equals(target.getScheme())) {
      throw new IllegalArgumentException("HTTPS readiness requires an HTTPS origin");
    }
    KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
    store.load(null, null);
    try (InputStream certificate = Files.newInputStream(rootCertificate)) {
      store.setCertificateEntry(
          "owned-proxy", CertificateFactory.getInstance("X.509").generateCertificate(certificate));
    }
    TrustManagerFactory managers =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    managers.init(store);
    SSLContext tls = SSLContext.getInstance("TLS");
    tls.init(null, managers.getTrustManagers(), null);
    try (HttpClient client =
        HttpClient.newBuilder().sslContext(tls).connectTimeout(Duration.ofSeconds(3)).build()) {
      HttpRequest request =
          HttpRequest.newBuilder(target).timeout(Duration.ofSeconds(5)).GET().build();
      Instant deadline = Instant.now().plusSeconds(30);
      while (true) {
        try {
          HttpResponse<Void> response =
              client.send(request, HttpResponse.BodyHandlers.discarding());
          if (response.statusCode() != 200) {
            throw new IOException("HTTPS login readiness returned " + response.statusCode());
          }
          return;
        } catch (ConnectException error) {
          if (Instant.now().isAfter(deadline)) {
            throw error;
          }
          Thread.sleep(250);
        }
      }
    }
  }
}
