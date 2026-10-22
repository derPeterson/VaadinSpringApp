package de.derpeterson.app.development;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Eigene Prozessaufsicht überlebt auch einen harten IDE-Abbruch des Anwendungsprozesses. */
public final class IdeHttpsProxy implements AutoCloseable {
  private static final Logger log = LoggerFactory.getLogger(IdeHttpsProxy.class);
  private final Process supervisor;
  private final long proxyPid;
  private final AtomicBoolean closing = new AtomicBoolean();

  private IdeHttpsProxy(Process supervisor, long proxyPid) {
    this.supervisor = supervisor;
    this.proxyPid = proxyPid;
  }

  static IdeHttpsProxy start(Path directory, List<String> command, Map<String, String> environment)
      throws IOException, InterruptedException {
    List<String> javaArguments =
        new ArrayList<>(
            List.of("-cp", System.getProperty("java.class.path"), IdeHttpsProxy.class.getName()));
    javaArguments.addAll(command);
    Process supervisor = null;
    try (IdeHttpsArguments arguments = IdeHttpsArguments.create(directory)) {
      Files.write(
          arguments.path(),
          javaArguments.stream().map(IdeHttpsProxy::quoteArgument).toList(),
          StandardCharsets.UTF_8);
      ProcessBuilder builder =
          new ProcessBuilder(javaExecutable(), "@" + arguments.path())
              .directory(directory.toFile())
              .redirectError(ProcessBuilder.Redirect.INHERIT);
      builder.environment().putAll(environment);
      supervisor = builder.start();
      Process started = supervisor;
      CompletableFuture<String> confirmation =
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  return started.inputReader(StandardCharsets.UTF_8).readLine();
                } catch (IOException failure) {
                  throw new IllegalStateException(
                      "Cannot read Caddy startup confirmation", failure);
                }
              });
      String ready = awaitSupervisorConfirmation(confirmation);
      if (ready == null || !ready.startsWith("READY ")) {
        throw new IOException("Caddy could not start. See the preceding proxy diagnostic.");
      }
      return new IdeHttpsProxy(supervisor, Long.parseLong(ready.substring(6)));
    } catch (IOException | InterruptedException | RuntimeException failure) {
      if (supervisor != null) {
        try {
          stopSupervisor(supervisor);
        } catch (RuntimeException cleanupFailure) {
          failure.addSuppressed(cleanupFailure);
        }
      }
      throw failure;
    }
  }

  private static String awaitSupervisorConfirmation(CompletableFuture<String> confirmation)
      throws IOException, InterruptedException {
    try {
      return confirmation.get(20, TimeUnit.SECONDS);
    } catch (ExecutionException | TimeoutException failure) {
      throw new IOException("Cannot confirm HTTPS supervisor startup", failure);
    }
  }

  static String javaExecutable() {
    return Path.of(
            System.getProperty("java.home"),
            "bin",
            System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java")
        .toString();
  }

  static String quoteArgument(String value) {
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }

  long proxyPid() {
    return proxyPid;
  }

  long supervisorPid() {
    return supervisor.pid();
  }

  void failApplicationOnUnexpectedExit() {
    supervisor
        .onExit()
        .thenRun(
            () -> {
              if (!closing.get()) {
                log.error(
                    "HTTPS proxy stopped unexpectedly; stopping the development application.");
                System.exit(1);
              }
            });
  }

  @Override
  public void close() {
    if (!closing.compareAndSet(false, true)) {
      return;
    }
    stopSupervisor(supervisor);
  }

  private static void stopSupervisor(Process supervisor) {
    try {
      // EOF ist der normale Stopauftrag; kein Kill fremder Prozesse über Namen oder Ports.
      supervisor.getOutputStream().close();
      if (!supervisor.waitFor(8, TimeUnit.SECONDS)) {
        throw new IllegalStateException("HTTPS supervisor did not finish its owned proxy cleanup");
      }
    } catch (IOException failure) {
      throw new IllegalStateException("Cannot signal HTTPS supervisor cleanup", failure);
    } catch (InterruptedException failure) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted during HTTPS supervisor cleanup", failure);
    }
  }

  static void stopOwnedProcess(Process process) throws InterruptedException {
    if (process.isAlive()) {
      process.destroy();
      if (!process.waitFor(2, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        if (!process.waitFor(3, TimeUnit.SECONDS)) {
          throw new IllegalStateException("Owned HTTPS child process did not stop");
        }
      }
    }
  }

  /**
   * Separater JVM-Prozess: stdout ist ausschließlich das Startprotokoll, Caddy-Ausgaben gehen an
   * stderr.
   */
  public static void main(String[] args) throws IOException, InterruptedException {
    ProcessHandle owner = ProcessHandle.current().parent().orElseThrow();
    CompletableFuture<Void> stopRequested =
        CompletableFuture.runAsync(
            () -> {
              try {
                System.in.read();
              } catch (IOException _) {
                // Auch eine unterbrochene Steuerleitung verlangt den Abbau des eigenen Proxys.
              }
            });
    byte[] challenge = new byte[32];
    new SecureRandom().nextBytes(challenge);
    try (ServerSocket callback = new ServerSocket()) {
      callback.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0));
      callback.setSoTimeout(250);
      List<String> command = new ArrayList<>(List.of(args));
      command.addAll(List.of("--pingback", "127.0.0.1:" + callback.getLocalPort()));
      Process child =
          new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT).start();
      Thread.ofVirtual()
          .start(
              () -> {
                try {
                  child.getInputStream().transferTo(System.err);
                } catch (IOException _) {
                  // Der Prozessabbau kann den Ausgabestrom schließen.
                }
              });
      try {
        // Caddy bestätigt erst nach erfolgreichem Laden der Konfiguration diese Zufallsbytes.
        child.getOutputStream().write(challenge);
        child.getOutputStream().close();
        awaitConfirmation(callback, challenge, owner, child, stopRequested);
        System.out.println("READY " + child.pid());
        System.out.flush();
        CompletableFuture.anyOf(owner.onExit(), stopRequested, child.onExit()).join();
        if (child.isAlive() || !owner.isAlive() || stopRequested.isDone()) {
          return;
        }
        throw new IOException("Owned Caddy process exited unexpectedly: " + child.exitValue());
      } finally {
        stopOwnedProcess(child);
      }
    }
  }

  private static void awaitConfirmation(
      ServerSocket callback,
      byte[] expected,
      ProcessHandle owner,
      Process child,
      CompletableFuture<Void> stopRequested)
      throws IOException {
    Instant deadline = Instant.now().plusSeconds(15);
    while (owner.isAlive()
        && child.isAlive()
        && !stopRequested.isDone()
        && Instant.now().isBefore(deadline)) {
      try (Socket confirmation = callback.accept()) {
        confirmation.setSoTimeout(1000);
        if (Arrays.equals(expected, confirmation.getInputStream().readNBytes(expected.length))) {
          return;
        }
      } catch (SocketTimeoutException _) {
        // Begrenzt auf die eigene Startbestätigung warten und zugleich Besitzer/Kind überwachen.
      }
    }
    throw new IOException(
        "Owned Caddy did not confirm startup; check configuration and occupied ports");
  }
}
