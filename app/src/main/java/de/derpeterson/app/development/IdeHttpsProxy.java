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
  private final Process supervisorProcess;
  private final long proxyPid;
  private final AtomicBoolean proxyClosing = new AtomicBoolean();

  private IdeHttpsProxy(Process supervisorProcess, long proxyPid) {
    this.supervisorProcess = supervisorProcess;
    this.proxyPid = proxyPid;
  }

  static IdeHttpsProxy startHttpsProxy(
      Path workingDirectory, List<String> caddyCommand, Map<String, String> processEnvironment)
      throws IOException, InterruptedException {
    List<String> javaArguments =
        new ArrayList<>(
            List.of("-cp", System.getProperty("java.class.path"), IdeHttpsProxy.class.getName()));
    javaArguments.addAll(caddyCommand);
    Process supervisorProcess = null;
    try (IdeHttpsArguments launcherArguments =
        IdeHttpsArguments.createPrivateArgumentFile(workingDirectory)) {
      Files.write(
          launcherArguments.argumentFilePath(),
          javaArguments.stream().map(IdeHttpsProxy::quoteArgument).toList(),
          StandardCharsets.UTF_8);
      ProcessBuilder supervisorProcessBuilder =
          new ProcessBuilder(javaExecutable(), "@" + launcherArguments.argumentFilePath())
              .directory(workingDirectory.toFile())
              .redirectError(ProcessBuilder.Redirect.INHERIT);
      supervisorProcessBuilder.environment().putAll(processEnvironment);
      supervisorProcess = supervisorProcessBuilder.start();
      Process startedSupervisorProcess = supervisorProcess;
      CompletableFuture<String> startupConfirmation =
          CompletableFuture.supplyAsync(() -> {
            try {
              return startedSupervisorProcess.inputReader(StandardCharsets.UTF_8).readLine();
            } catch (IOException failure) {
              throw new IllegalStateException("Cannot read Caddy startup confirmation", failure);
            }
          });
      String readySignal = awaitSupervisorConfirmation(startupConfirmation);
      if (readySignal == null || !readySignal.startsWith("READY ")) {
        throw new IOException("Caddy could not start. See the preceding proxy diagnostic.");
      }
      return new IdeHttpsProxy(supervisorProcess, Long.parseLong(readySignal.substring(6)));
    } catch (IOException | InterruptedException | RuntimeException failure) {
      if (supervisorProcess != null) {
        try {
          stopSupervisor(supervisorProcess);
        } catch (RuntimeException cleanupFailure) {
          failure.addSuppressed(cleanupFailure);
        }
      }
      throw failure;
    }
  }

  private static String awaitSupervisorConfirmation(CompletableFuture<String> startupConfirmation)
      throws IOException, InterruptedException {
    try {
      return startupConfirmation.get(20, TimeUnit.SECONDS);
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

  static String quoteArgument(String argumentText) {
    return "\"" + argumentText.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }

  long proxyPid() {
    return proxyPid;
  }

  long supervisorPid() {
    return supervisorProcess.pid();
  }

  void failApplicationOnUnexpectedExit() {
    supervisorProcess
        .onExit()
        .thenRun(() -> {
          if (!proxyClosing.get()) {
            log.error("HTTPS proxy stopped unexpectedly; stopping the development application.");
            System.exit(1);
          }
        });
  }

  @Override
  public void close() {
    if (!proxyClosing.compareAndSet(false, true)) {
      return;
    }
    stopSupervisor(supervisorProcess);
  }

  private static void stopSupervisor(Process supervisorProcess) {
    try {
      // EOF ist der normale Stopauftrag; kein Kill fremder Prozesse über Namen oder Ports.
      supervisorProcess.getOutputStream().close();
      if (!supervisorProcess.waitFor(8, TimeUnit.SECONDS)) {
        throw new IllegalStateException("HTTPS supervisor did not finish its owned proxy cleanup");
      }
    } catch (IOException failure) {
      throw new IllegalStateException("Cannot signal HTTPS supervisor cleanup", failure);
    } catch (InterruptedException failure) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted during HTTPS supervisor cleanup", failure);
    }
  }

  static void stopOwnedProcess(Process ownedProcess) throws InterruptedException {
    if (ownedProcess.isAlive()) {
      ownedProcess.destroy();
      if (!ownedProcess.waitFor(2, TimeUnit.SECONDS)) {
        ownedProcess.destroyForcibly();
        if (!ownedProcess.waitFor(3, TimeUnit.SECONDS)) {
          throw new IllegalStateException("Owned HTTPS child process did not stop");
        }
      }
    }
  }

  /**
   * Separater JVM-Prozess: stdout ist ausschließlich das Startprotokoll, Caddy-Ausgaben gehen an
   * stderr.
   */
  public static void main(String[] supervisorArguments) throws IOException, InterruptedException {
    ProcessHandle ownerProcessHandle = ProcessHandle.current().parent().orElseThrow();
    CompletableFuture<Void> stopRequested =
        CompletableFuture.runAsync(() -> {
          try {
            System.in.read();
          } catch (IOException _) {
            // Auch eine unterbrochene Steuerleitung verlangt den Abbau des eigenen Proxys.
          }
        });
    byte[] startupChallengeBytes = new byte[32];
    new SecureRandom().nextBytes(startupChallengeBytes);
    try (ServerSocket callbackServerSocket = new ServerSocket()) {
      callbackServerSocket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0));
      callbackServerSocket.setSoTimeout(250);
      List<String> caddyCommand = new ArrayList<>(List.of(supervisorArguments));
      caddyCommand.addAll(
          List.of("--pingback", "127.0.0.1:" + callbackServerSocket.getLocalPort()));
      Process caddyProcess =
          new ProcessBuilder(caddyCommand).redirectError(ProcessBuilder.Redirect.INHERIT).start();
      Thread.ofVirtual()
          .start(() -> {
            try {
              caddyProcess.getInputStream().transferTo(System.err);
            } catch (IOException _) {
              // Der Prozessabbau kann den Ausgabestrom schließen.
            }
          });
      try {
        // Caddy bestätigt erst nach erfolgreichem Laden der Konfiguration diese Zufallsbytes.
        caddyProcess.getOutputStream().write(startupChallengeBytes);
        caddyProcess.getOutputStream().close();
        awaitConfirmation(
            callbackServerSocket,
            startupChallengeBytes,
            ownerProcessHandle,
            caddyProcess,
            stopRequested);
        System.out.println("READY " + caddyProcess.pid());
        System.out.flush();
        CompletableFuture.anyOf(ownerProcessHandle.onExit(), stopRequested, caddyProcess.onExit())
            .join();
        if (caddyProcess.isAlive() || !ownerProcessHandle.isAlive() || stopRequested.isDone()) {
          return;
        }
        throw new IOException(
            "Owned Caddy process exited unexpectedly: " + caddyProcess.exitValue());
      } finally {
        stopOwnedProcess(caddyProcess);
      }
    }
  }

  private static void awaitConfirmation(
      ServerSocket callbackServerSocket,
      byte[] expectedChallengeBytes,
      ProcessHandle ownerProcessHandle,
      Process caddyProcess,
      CompletableFuture<Void> stopRequested)
      throws IOException {
    Instant startupDeadline = Instant.now().plusSeconds(15);
    while (ownerProcessHandle.isAlive()
        && caddyProcess.isAlive()
        && !stopRequested.isDone()
        && Instant.now().isBefore(startupDeadline)) {
      try (Socket startupConfirmationSocket = callbackServerSocket.accept()) {
        startupConfirmationSocket.setSoTimeout(1000);
        if (Arrays.equals(
            expectedChallengeBytes,
            startupConfirmationSocket.getInputStream().readNBytes(expectedChallengeBytes.length))) {
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
