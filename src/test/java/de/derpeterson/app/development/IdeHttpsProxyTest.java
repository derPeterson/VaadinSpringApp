package de.derpeterson.app.development;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

class IdeHttpsProxyTest {
  @TempDir Path directory;

  @Test
  void launcherArgumentsArePrivateFromCreationUntilCleanup() throws Exception {
    Path file;
    Path folder;
    try (IdeHttpsArguments arguments = IdeHttpsArguments.create(directory)) {
      file = arguments.path();
      folder = arguments.directory();
      UserPrincipal owner =
          directory
              .getFileSystem()
              .getUserPrincipalLookupService()
              .lookupPrincipalByName(System.getProperty("user.name"));
      for (Path path : List.of(folder, file)) {
        AclFileAttributeView acl = Files.getFileAttributeView(path, AclFileAttributeView.class);
        if (acl != null) {
          List<AclEntry> entries = acl.getAcl();
          assertFalse(entries.isEmpty());
          assertTrue(
              entries.stream()
                  .allMatch(
                      entry ->
                          entry.type() == AclEntryType.ALLOW && entry.principal().equals(owner)),
              entries::toString);
        } else {
          assertEquals(
              PosixFilePermissions.fromString(Files.isDirectory(path) ? "rwx------" : "rw-------"),
              Files.getPosixFilePermissions(path));
        }
      }
      Files.writeString(file, "owned fixture arguments");
      assertEquals("owned fixture arguments", Files.readString(file));
    }
    assertFalse(Files.exists(file));
    assertFalse(Files.exists(folder));
  }

  @Test
  void ownedProxyCanStartStopAndRestartWithPathsContainingSpaces() throws Exception {
    Path work = Files.createDirectories(directory.resolve("test project"));
    for (int attempt = 0; attempt < 2; attempt++) {
      IdeHttpsProxy proxy = IdeHttpsProxy.start(work, fakeCommand("ready"), Map.of());
      assertNoLauncherArguments(work);
      ProcessHandle child = ProcessHandle.of(proxy.proxyPid()).orElseThrow();
      ProcessHandle supervisor = ProcessHandle.of(proxy.supervisorPid()).orElseThrow();
      assertTrue(child.isAlive());
      proxy.close();
      proxy.close();
      assertFalse(child.isAlive());
      assertFalse(supervisor.isAlive());
    }
  }

  @Test
  void startupFailureAndWrongConfirmationDoNotLeaveOwnedChildren() throws Exception {
    for (String mode : List.of("fail", "wrong")) {
      Path pidFile = directory.resolve(mode + ".pid");
      List<String> command = fakeCommand(mode, pidFile.toString());
      assertThrows(IOException.class, () -> IdeHttpsProxy.start(directory, command, Map.of()));
      assertNoLauncherArguments(directory);
      ProcessHandle child =
          ProcessHandle.of(Long.parseLong(Files.readString(pidFile))).orElse(null);
      assertTrue(child == null || !child.isAlive());
    }
  }

  @Test
  void missingStartupConfirmationTimesOutAndStopsTheOwnedChild() throws Exception {
    Path pidFile = directory.resolve("timeout.pid");
    List<String> command = fakeCommand("stall", pidFile.toString());
    assertThrows(IOException.class, () -> IdeHttpsProxy.start(directory, command, Map.of()));
    assertNoLauncherArguments(directory);
    ProcessHandle child = ProcessHandle.of(Long.parseLong(Files.readString(pidFile))).orElse(null);
    assertTrue(child == null || !child.isAlive());
  }

  @Test
  void hardOwnerExitCleansProxyEvenWithJavaDebuggerEnabled() throws Exception {
    Path commandFile = javaArguments(FakeCaddy.class, List.of("ready"));
    Path ownerFile =
        javaArguments(Owner.class, List.of(directory.toString(), commandFile.toString()));
    Process owner =
        new ProcessBuilder(
                IdeHttpsProxy.javaExecutable(),
                "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:0",
                "@" + ownerFile)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start();
    ProcessHandle child = null;
    ProcessHandle supervisor = null;
    try {
      CompletableFuture<String> ready =
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  String line;
                  while ((line = owner.inputReader().readLine()) != null) {
                    if (line.startsWith("CHILD ")) {
                      return line;
                    }
                  }
                  throw new IllegalStateException("Owner exited before setup");
                } catch (IOException failure) {
                  throw new IllegalStateException(failure);
                }
              });
      String[] pids = ready.get(20, TimeUnit.SECONDS).split(" ");
      child = ProcessHandle.of(Long.parseLong(pids[1])).orElseThrow();
      supervisor = ProcessHandle.of(Long.parseLong(pids[2])).orElseThrow();
      assertTrue(child.isAlive());
      owner.destroyForcibly();
      assertTrue(owner.waitFor(5, TimeUnit.SECONDS));
      child.onExit().get(10, TimeUnit.SECONDS);
      supervisor.onExit().get(10, TimeUnit.SECONDS);
      assertFalse(child.isAlive());
      assertFalse(supervisor.isAlive());
    } finally {
      owner.destroyForcibly();
      owner.waitFor();
      if (child != null && child.isAlive()) {
        child.destroyForcibly();
      }
      if (supervisor != null && supervisor.isAlive()) {
        supervisor.destroyForcibly();
      }
    }
  }

  @Test
  void proxyFailureStopsTheOwningDevelopmentApplication() throws Exception {
    Path commandFile = javaArguments(FakeCaddy.class, List.of("ready"));
    Process owner =
        new ProcessBuilder(
                IdeHttpsProxy.javaExecutable(),
                "@"
                    + javaArguments(
                        Owner.class, List.of(directory.toString(), commandFile.toString())))
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start();
    try {
      String[] pids = owner.inputReader().readLine().split(" ");
      ProcessHandle child = ProcessHandle.of(Long.parseLong(pids[1])).orElseThrow();
      child.destroyForcibly();
      assertTrue(owner.waitFor(10, TimeUnit.SECONDS));
      assertEquals(1, owner.exitValue());
      assertFalse(child.isAlive());
    } finally {
      owner.destroyForcibly();
      owner.waitFor();
    }
  }

  @Test
  void occupiedPortIsReportedWithoutTakingOverTheExistingListener() throws Exception {
    try (ServerSocket occupied = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
      IOException failure =
          assertThrows(
              IOException.class,
              () -> HttpsDevelopmentApplication.requireFreePorts(occupied.getLocalPort()));
      assertTrue(failure.getMessage().contains(String.valueOf(occupied.getLocalPort())));
      try (Socket client = new Socket("127.0.0.1", occupied.getLocalPort());
          Socket accepted = occupied.accept()) {
        assertTrue(client.isConnected());
        assertTrue(accepted.isConnected());
      }
    }
  }

  @Test
  void absentCaddyIsRejectedBeforeApplicationStartup() {
    assertThrows(
        IOException.class,
        () -> HttpsDevelopmentApplication.verifyCaddy(directory.resolve("absent.exe")));
  }

  @Test
  @EnabledIfSystemProperty(named = "https.test.caddy", matches = ".+")
  void fullIdeApplicationStartsWithoutTestConfigurationsAndCleansUpAfterStop() throws Exception {
    HttpsDevelopmentApplication.requireFreePorts(8080, 8081, 8443);
    Path repo = Path.of("").toAbsolutePath();
    Path work = Files.createDirectories(directory.resolve("isolated IDE project"));
    Path scripts = Files.createDirectories(work.resolve("scripts/https"));
    Files.copy(repo.resolve("scripts/https/Caddyfile.ide"), scripts.resolve("Caddyfile.ide"));
    Path output = work.resolve("application-output.txt");
    try (ServerSocket smtp = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
      Path arguments =
          javaArguments(
              HttpsDevelopmentApplication.class,
              List.of(
                  "--spring.profiles.active=https",
                  "--server.port=8080",
                  "--spring.config.import=",
                  "--spring.datasource.url=jdbc:h2:mem:ide-start-"
                      + UUID.randomUUID()
                      + ";MODE=MySQL;DB_CLOSE_DELAY=-1;SCHEMA=PUBLIC",
                  "--app.bootstrap.enabled=false",
                  "--app.config.defaults.rememberMe.secret.key=isolated-ide-start-check",
                  "--app.config.defaults.mail.host=127.0.0.1",
                  "--app.config.defaults.mail.port=" + smtp.getLocalPort(),
                  "--app.config.defaults.mail.smtp.auth=false",
                  "--vaadin.productionMode=true",
                  "--vaadin.launch-browser=false",
                  "--spring.h2.console.enabled=false",
                  "--app.security.expose-h2-console=false"),
              applicationClasspath());
      Process application =
          new ProcessBuilder(
                  IdeHttpsProxy.javaExecutable(),
                  "-Dhttps.dev.caddy="
                      + Path.of(System.getProperty("https.test.caddy")).toAbsolutePath(),
                  "@" + arguments)
              .directory(work.toFile())
              .redirectErrorStream(true)
              .redirectOutput(output.toFile())
              .start();
      List<ProcessHandle> ownedChildren = List.of();
      try {
        Path root = work.resolve("data/https/ide/pki/authorities/local/root.crt");
        assertTrue(
            awaitApplicationReady(application, root),
            () -> {
              try {
                return Files.readString(output);
              } catch (IOException failure) {
                return "No application startup output: " + failure.getClass().getSimpleName();
              }
            });
        assertFalse(Files.readString(output).contains("BeanDefinitionOverrideException"));
        ownedChildren = application.descendants().toList();
        assertTrue(ownedChildren.size() >= 2);
        IdeHttpsProxy.stopOwnedProcess(application);
        for (ProcessHandle child : ownedChildren) {
          child.onExit().get(10, TimeUnit.SECONDS);
          assertFalse(child.isAlive());
        }
      } finally {
        IdeHttpsProxy.stopOwnedProcess(application);
        for (ProcessHandle child : ownedChildren) {
          if (child.isAlive()) {
            child.destroyForcibly();
          }
        }
      }
    }
  }

  @Test
  @EnabledIfSystemProperty(named = "https.test.caddy", matches = ".+")
  void realCaddyUsesIsolatedCaAndFullHttpsCertificateValidation() throws Exception {
    Path caddy = Path.of(System.getProperty("https.test.caddy")).toAbsolutePath();
    HttpsDevelopmentApplication.verifyCaddy(caddy);
    int port;
    try (ServerSocket reservation = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
      port = reservation.getLocalPort();
    }
    Path config = directory.resolve("Caddyfile");
    Path ca = directory.resolve("own ca");
    Files.writeString(
        config,
        "{\nadmin off\npersist_config off\nskip_install_trust\nauto_https disable_redirects\nstorage file_system \"{$APP_HTTPS_CA_DIR}\"\n}\nhttps://localhost:"
            + port
            + " {\nbind 127.0.0.1\ntls internal\nrespond OK\n}\n");
    long childPid;
    try (IdeHttpsProxy proxy =
        IdeHttpsProxy.start(
            directory,
            List.of(
                caddy.toString(), "run", "--config", config.toString(), "--adapter", "caddyfile"),
            Map.of("APP_HTTPS_CA_DIR", ca.toString().replace('\\', '/')))) {
      childPid = proxy.proxyPid();
      try (HttpClient client =
          HttpClient.newBuilder()
              .sslContext(trustOnly(ca.resolve("pki/authorities/local/root.crt")))
              .connectTimeout(Duration.ofSeconds(3))
              .build()) {
        HttpResponse<String> response =
            client.send(
                HttpRequest.newBuilder(URI.create("https://localhost:" + port))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("OK", response.body());
      }
    }
    assertTrue(
        ProcessHandle.of(childPid).isEmpty()
            || !ProcessHandle.of(childPid).orElseThrow().isAlive());
  }

  private List<String> fakeCommand(String... arguments) throws IOException {
    return List.of(
        IdeHttpsProxy.javaExecutable(), "@" + javaArguments(FakeCaddy.class, List.of(arguments)));
  }

  private static boolean awaitApplicationReady(Process application, Path root) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
    try (ScheduledExecutorService probes = Executors.newSingleThreadScheduledExecutor()) {
      while (application.isAlive() && System.nanoTime() < deadline) {
        if (probes
            .schedule(() -> isLoginReady(root), 250, TimeUnit.MILLISECONDS)
            .get(5, TimeUnit.SECONDS)) {
          return true;
        }
      }
      return false;
    }
  }

  private static boolean isLoginReady(Path root) throws Exception {
    if (!Files.isRegularFile(root)) {
      return false;
    }
    try (HttpClient client =
        HttpClient.newBuilder()
            .sslContext(trustOnly(root))
            .connectTimeout(Duration.ofSeconds(1))
            .build()) {
      HttpResponse<String> response =
          client.send(
              HttpRequest.newBuilder(URI.create("https://localhost:8443/login"))
                  .timeout(Duration.ofSeconds(2))
                  .GET()
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      return response.statusCode() == 200;
    } catch (IOException _) {
      // Ausschließlich auf die eigene frisch gestartete Anwendung warten.
      return false;
    }
  }

  private void assertNoLauncherArguments(Path project) throws IOException {
    try (Stream<Path> remaining = Files.list(project.resolve("data/https/ide-launcher"))) {
      assertEquals(List.of(), remaining.toList());
    }
  }

  private Path javaArguments(Class<?> mainClass, List<String> arguments) throws IOException {
    return javaArguments(mainClass, arguments, System.getProperty("java.class.path"));
  }

  private Path javaArguments(Class<?> mainClass, List<String> arguments, String classpath)
      throws IOException {
    Path file = Files.createTempFile(directory, "java fixture ", ".args");
    List<String> command = new ArrayList<>(List.of("-cp", classpath, mainClass.getName()));
    command.addAll(arguments);
    Files.write(
        file, command.stream().map(IdeHttpsProxy::quoteArgument).toList(), StandardCharsets.UTF_8);
    return file;
  }

  private static String applicationClasspath() throws Exception {
    Path tests =
        Path.of(IdeHttpsProxyTest.class.getProtectionDomain().getCodeSource().getLocation().toURI())
            .toAbsolutePath()
            .normalize();
    return Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
        .filter(entry -> !Path.of(entry).toAbsolutePath().normalize().equals(tests))
        .collect(Collectors.joining(File.pathSeparator));
  }

  private static SSLContext trustOnly(Path certificate) throws Exception {
    KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
    store.load(null, null);
    try (InputStream root = Files.newInputStream(certificate)) {
      store.setCertificateEntry(
          "own-test-ca", CertificateFactory.getInstance("X.509").generateCertificate(root));
    }
    TrustManagerFactory managers =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    managers.init(store);
    SSLContext tls = SSLContext.getInstance("TLS");
    tls.init(null, managers.getTrustManagers(), null);
    return tls;
  }

  public static class Owner {
    public static void main(String[] args) throws Exception {
      IdeHttpsProxy proxy =
          IdeHttpsProxy.start(
              Path.of(args[0]), List.of(IdeHttpsProxy.javaExecutable(), "@" + args[1]), Map.of());
      Runtime.getRuntime().addShutdownHook(new Thread(proxy::close));
      proxy.failApplicationOnUnexpectedExit();
      System.out.println("CHILD " + proxy.proxyPid() + " " + proxy.supervisorPid());
      System.out.flush();
      System.in.read();
    }
  }

  /**
   * Eigener Prozess mit Caddys Startbestätigungsprotokoll, ohne App, Datenbank oder
   * Zertifikatsspeicher.
   */
  public static class FakeCaddy {
    public static void main(String[] args) throws Exception {
      if (args.length > 3) {
        Files.writeString(Path.of(args[1]), String.valueOf(ProcessHandle.current().pid()));
      }
      if (args[0].equals("fail")) {
        System.exit(7);
      }
      if (args[0].equals("stall")) {
        awaitOwnerShutdown();
        return;
      }
      byte[] challenge = System.in.readAllBytes();
      String[] address = args[args.length - 1].split(":");
      try (Socket callback = new Socket(address[0], Integer.parseInt(address[1]))) {
        if (args[0].equals("wrong")) {
          challenge[0] ^= 1;
        }
        callback.getOutputStream().write(challenge);
      }
      if (args[0].equals("wrong")) {
        System.exit(7);
      }
      awaitOwnerShutdown();
    }

    private static void awaitOwnerShutdown() throws InterruptedException {
      CountDownLatch shutdown = new CountDownLatch(1);
      Runtime.getRuntime().addShutdownHook(new Thread(shutdown::countDown));
      // Der Fake bleibt bis zum Stop durch seinen Besitzer aktiv, höchstens 60 Sekunden.
      if (!shutdown.await(60, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Owner did not stop fake Caddy");
      }
    }
  }
}
