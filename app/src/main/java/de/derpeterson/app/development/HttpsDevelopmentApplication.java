package de.derpeterson.app.development;

import de.derpeterson.app.Application;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
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
  private static final String CADDY_VERSION = "v2.11.9";

  private HttpsDevelopmentApplication() {}

  public static void main(String[] applicationArguments) throws IOException, InterruptedException {
    if (Runtime.version().feature() != 25 || !System.getProperty("os.name").startsWith("Windows")) {
      throw new IllegalStateException("The HTTPS IDE launcher requires Windows and Java 25");
    }
    Path repositoryPath = Path.of("").toAbsolutePath().normalize();
    Path applicationModulePath = requireApplicationModule(repositoryPath);
    // Laufzeitdaten bleiben im Root; Vaadin muss die Quellen im app-Modul finden.
    System.getProperties().putIfAbsent("vaadin.project.basedir", applicationModulePath.toString());
    Path caddyExecutablePath =
        repositoryPath
            .resolve(System.getProperty("https.dev.caddy", "data/https/tools/caddy.exe"))
            .normalize();
    verifyCaddy(caddyExecutablePath);
    requireFreePorts(8080, 8081, 8443);
    String bindHostName = "localhost";
    Path caddyfilePath = repositoryPath.resolve("scripts/https/Caddyfile.ide");
    String lanHost = System.getProperty("https.dev.lan-host");
    if (lanHost != null) {
      InetAddress bindAddress = privateIpv4(lanHost);
      if (NetworkInterface.getByInetAddress(bindAddress) == null) {
        throw new IOException("The LAN IPv4 address is not assigned to this computer");
      }
      requireFreePort(bindAddress, 8443);
      bindHostName = bindAddress.getHostAddress();
      caddyfilePath = repositoryPath.resolve("scripts/https/Caddyfile.ide-lan");
      System.setProperty("app.https.origin", "https://" + bindHostName + ":8443");
      System.setProperty("server.address", "127.0.0.1");
    }
    // Ein DevTools-Neustart des Main würde einen zweiten Proxy starten. Java-Debugging bleibt
    // aktiv.
    System.setProperty("spring.devtools.restart.enabled", "false");
    IdeHttpsProxy httpsProxy =
        IdeHttpsProxy.startHttpsProxy(
            repositoryPath,
            List.of(
                caddyExecutablePath.toString(),
                "run",
                "--config",
                caddyfilePath.toString(),
                "--adapter",
                "caddyfile"),
            Map.of(
                "APP_HTTPS_CA_DIR",
                repositoryPath.resolve("data/https/ide").toString().replace('\\', '/'),
                "APP_HTTPS_IDE_LAN_HOST",
                bindHostName));
    Runtime.getRuntime().addShutdownHook(new Thread(httpsProxy::close, "ide-https-cleanup"));
    httpsProxy.failApplicationOnUnexpectedExit();
    try {
      SpringApplication springApplication = new SpringApplication(Application.class);
      springApplication.setAdditionalProfiles("https");
      springApplication.run(applicationArguments);
      log.info("HTTPS development access: https://{}:8443", bindHostName);
    } catch (Throwable startupFailure) {
      // Auch Linkage-/VM-Startfehler beenden nur den eigenen Proxy; kein Wiederherstellungsversuch.
      try {
        httpsProxy.close();
      } catch (RuntimeException cleanupFailure) {
        startupFailure.addSuppressed(cleanupFailure);
      }
      throw startupFailure;
    }
  }

  static Path requireApplicationModule(Path repositoryDirectory) throws IOException {
    Path repositoryRoot = repositoryDirectory.toAbsolutePath().normalize();
    Path applicationModulePath = repositoryRoot.resolve("app");
    if (!Files.isRegularFile(applicationModulePath.resolve("pom.xml"))
        || !Files.isRegularFile(repositoryRoot.resolve("scripts/https/Caddyfile.ide"))) {
      throw new IOException(
          "Start Application HTTPS with the repository root as working directory; app/pom.xml and scripts/https/Caddyfile.ide are required.");
    }
    return applicationModulePath;
  }

  static void verifyCaddy(Path caddyExecutable) throws IOException, InterruptedException {
    if (!Files.isRegularFile(caddyExecutable)) {
      throw new IOException(
          "Caddy is missing. Place "
              + CADDY_VERSION
              + " in data/https/tools/caddy.exe or set -Dhttps.dev.caddy=<path>");
    }
    Process versionProcess =
        new ProcessBuilder(caddyExecutable.toString(), "version").redirectErrorStream(true).start();
    try {
      if (!versionProcess.waitFor(5, TimeUnit.SECONDS)) {
        throw new IOException("Caddy version check timed out");
      }
      String versionOutput =
          new String(versionProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      if (versionProcess.exitValue() != 0 || !versionOutput.startsWith(CADDY_VERSION + " ")) {
        throw new IOException("Caddy " + CADDY_VERSION + " is required");
      }
    } finally {
      IdeHttpsProxy.stopOwnedProcess(versionProcess);
    }
  }

  static void requireFreePorts(int... requiredPorts) throws IOException {
    for (int requiredPort : requiredPorts) {
      requireFreePort(InetAddress.ofLiteral("127.0.0.1"), requiredPort);
    }
  }

  static InetAddress privateIpv4(String hostAddressText) {
    InetAddress bindAddress = InetAddress.ofLiteral(hostAddressText.strip());
    if (!(bindAddress instanceof Inet4Address) || !bindAddress.isSiteLocalAddress()) {
      throw new IllegalArgumentException("The HTTPS LAN launcher requires a private IPv4 address");
    }
    return bindAddress;
  }

  private static void requireFreePort(InetAddress bindAddress, int requiredPort)
      throws IOException {
    try (ServerSocket portProbeSocket = new ServerSocket()) {
      portProbeSocket.setReuseAddress(false);
      portProbeSocket.bind(new InetSocketAddress(bindAddress, requiredPort));
    } catch (IOException startupFailure) {
      throw new IOException(
          "HTTPS IDE address "
              + bindAddress.getHostAddress()
              + ":"
              + requiredPort
              + " is unavailable. Stop the previous application or BAT-started Caddy first.",
          startupFailure);
    }
  }
}
