package de.derpeterson.browsertests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

/** Expliziter TLS-Negativnachweis mit echten isolierten Caddy-/Chromium-Prozessen. */
@Tag("browser")
@EnabledIfSystemProperty(named = "browser.tests.tlsProbe", matches = "true")
class BrowserTlsVerificationTest {
  @TempDir Path directory;

  @Test
  void chromiumAcceptsOwnedTrustButRejectsWrongHostnameAndUntrustedAuthority() throws Exception {
    Path authority = BrowserTestCa.directory(Path.of("..").toAbsolutePath().normalize());
    BrowserTestCa.requireTrusted(authority);
    List<Process> proxies = new ArrayList<>();
    try (BrowserTestCa.Lease _ = BrowserTestCa.lease(authority, true);
        Playwright playwright = Playwright.create();
        Browser browser = playwright.chromium().launch();
        BrowserContext context = browser.newContext()) {
      String trusted = startProxy(authority, directory.resolve("trusted"), proxies);
      Path untrusted = directory.resolve("untrusted authority");
      BrowserTestCa.initialize(untrusted);
      String unknown = startProxy(untrusted, directory.resolve("untrusted worker"), proxies);
      Page page = context.newPage();
      assertEquals(200, page.navigate(trusted).status());
      String wrongHostname = trusted.replace("127.0.0.1", "localhost");
      PlaywrightException hostname =
          assertThrows(PlaywrightException.class, () -> page.navigate(wrongHostname));
      assertTrue(hostname.getMessage().contains("ERR_CERT_COMMON_NAME_INVALID"));
      PlaywrightException chain =
          assertThrows(PlaywrightException.class, () -> page.navigate(unknown));
      assertTrue(chain.getMessage().contains("ERR_CERT_AUTHORITY_INVALID"));
    } finally {
      for (Process proxy : proxies) {
        BrowserHttps.stopBeforeTrustCleanup(proxy);
      }
      BrowserTestCa.cleanupWorker(directory.resolve("trusted"));
      BrowserTestCa.cleanupWorker(directory.resolve("untrusted worker"));
    }
  }

  private String startProxy(Path authority, Path worker, List<Process> proxies) throws Exception {
    BrowserTestCa.issue(authority, worker);
    int port;
    try (ServerSocket reservation = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      port = reservation.getLocalPort();
    }
    Path configuration = worker.resolve("Caddyfile");
    Files.writeString(
        configuration,
        """
        {
          admin off
          skip_install_trust
          auto_https disable_redirects
        }
        https://:%d {
          bind 127.0.0.1
          tls "%s" "%s"
          respond "isolated TLS fixture"
        }
        """
            .formatted(
                port,
                worker.resolve("server.crt").toString().replace('\\', '/'),
                worker.resolve("server.key").toString().replace('\\', '/')));
    ProcessBuilder builder =
        new ProcessBuilder(
                System.getProperty("browser.tests.caddyExecutable"),
                "run",
                "--config",
                configuration.toString(),
                "--adapter",
                "caddyfile")
            .redirectErrorStream(true)
            .redirectOutput(worker.resolve("proxy.log").toFile());
    builder.environment().put("XDG_DATA_HOME", worker.resolve("caddy-data").toString());
    builder.environment().put("XDG_CONFIG_HOME", worker.resolve("caddy-config").toString());
    proxies.add(builder.start());
    String origin = "https://127.0.0.1:" + port;
    BrowserHttps.awaitReady(origin, worker.resolve("root.crt"));
    return origin;
  }
}
