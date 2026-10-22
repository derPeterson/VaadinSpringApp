package de.derpeterson.app.development;

import de.derpeterson.app.Application;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;

/**
 * IDE-Einstieg ohne Testklassenpfad; das Entwicklungspaket wird vom Anwendungs-JAR ausgeschlossen.
 */
public final class HttpsDevelopmentApplication {
  private static final Logger log = LoggerFactory.getLogger(HttpsDevelopmentApplication.class);

  private HttpsDevelopmentApplication() {}

  public static void main(String[] args) throws IOException, InterruptedException {
    if (Runtime.version().feature() != 25 || !System.getProperty("os.name").startsWith("Windows")) {
      throw new IllegalStateException("The HTTPS IDE launcher requires Windows and Java 25");
    }
    Path repo = Path.of("").toAbsolutePath().normalize();
    Path module = requireApplicationModule(repo);
    // Laufzeitdaten bleiben im Root; Vaadin muss die Quellen im app-Modul finden.
    System.getProperties().putIfAbsent("vaadin.project.basedir", module.toString());
    Path caddy =
        repo.resolve(System.getProperty("https.dev.caddy", "data/https/tools/caddy.exe"))
            .normalize();
    verifyCaddy(caddy);
    requireFreePorts(8080, 8081, 8443);
    // Ein DevTools-Neustart des Main würde einen zweiten Proxy starten. Java-Debugging bleibt
    // aktiv.
    System.setProperty("spring.devtools.restart.enabled", "false");
    IdeHttpsProxy proxy =
        IdeHttpsProxy.start(
            repo,
            List.of(
                caddy.toString(),
                "run",
                "--config",
                repo.resolve("scripts/https/Caddyfile.ide").toString(),
                "--adapter",
                "caddyfile"),
            Map.of(
                "APP_HTTPS_CA_DIR", repo.resolve("data/https/ide").toString().replace('\\', '/')));
    Runtime.getRuntime().addShutdownHook(new Thread(proxy::close, "ide-https-cleanup"));
    proxy.failApplicationOnUnexpectedExit();
    try {
      SpringApplication application = new SpringApplication(Application.class);
      application.setAdditionalProfiles("https");
      application.run(args);
      log.info("HTTPS development access: https://localhost:8443");
    } catch (Throwable failure) {
      // Auch Linkage-/VM-Startfehler beenden nur den eigenen Proxy; kein Wiederherstellungsversuch.
      try {
        proxy.close();
      } catch (RuntimeException cleanupFailure) {
        failure.addSuppressed(cleanupFailure);
      }
      throw failure;
    }
  }

  static Path requireApplicationModule(Path repository) throws IOException {
    Path root = repository.toAbsolutePath().normalize();
    Path module = root.resolve("app");
    if (!Files.isRegularFile(module.resolve("pom.xml"))
        || !Files.isRegularFile(root.resolve("scripts/https/Caddyfile.ide"))) {
      throw new IOException(
          "Start Application HTTPS with the repository root as working directory; app/pom.xml and scripts/https/Caddyfile.ide are required.");
    }
    return module;
  }

  static void verifyCaddy(Path executable) throws IOException, InterruptedException {
    if (!Files.isRegularFile(executable)) {
      throw new IOException(
          "Caddy is missing. Place v2.10.2 in data/https/tools/caddy.exe or set -Dhttps.dev.caddy=<path>");
    }
    Process version =
        new ProcessBuilder(executable.toString(), "version").redirectErrorStream(true).start();
    try {
      if (!version.waitFor(5, TimeUnit.SECONDS)) {
        throw new IOException("Caddy version check timed out");
      }
      String output = new String(version.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      if (version.exitValue() != 0 || !output.startsWith("v2.10.2 ")) {
        throw new IOException("Caddy v2.10.2 is required");
      }
    } finally {
      IdeHttpsProxy.stopOwnedProcess(version);
    }
  }

  static void requireFreePorts(int... ports) throws IOException {
    for (int port : ports) {
      try (ServerSocket socket = new ServerSocket()) {
        socket.setReuseAddress(false);
        socket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port));
      } catch (IOException failure) {
        throw new IOException(
            "HTTPS IDE port "
                + port
                + " is occupied. Stop the previous application or BAT-started Caddy first; no existing process was stopped.",
            failure);
      }
    }
  }
}
