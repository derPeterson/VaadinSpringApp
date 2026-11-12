package de.derpeterson.browsertests.mail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Eigener kurzlebiger SMTP-Testdienst: liest DATA, schließt aber vor der finalen Bestätigung. */
public final class AmbiguousSmtpServer implements AutoCloseable {
  private final ServerSocket smtpServerSocket = new ServerSocket();
  private final ExecutorService smtpExecutorService = Executors.newVirtualThreadPerTaskExecutor();
  private final Set<Socket> activeSockets = ConcurrentHashMap.newKeySet();
  private final AtomicInteger receivedMessageCount = new AtomicInteger();
  private final AtomicReference<IOException> serverFailure = new AtomicReference<>();

  public AmbiguousSmtpServer() throws IOException {
    // Wie der bestehende OIDC-Testdienst vom eigenen Docker-Container erreichbar.
    smtpServerSocket.bind(new InetSocketAddress("0.0.0.0", 0));
    smtpExecutorService.submit(this::acceptConnections);
  }

  public int port() {
    return smtpServerSocket.getLocalPort();
  }

  public int receivedMessageCount() {
    if (serverFailure.get() != null) {
      throw new IllegalStateException("Own SMTP test server failed", serverFailure.get());
    }
    return receivedMessageCount.get();
  }

  private void acceptConnections() {
    try {
      while (!smtpServerSocket.isClosed()) {
        Socket clientSocket = smtpServerSocket.accept();
        activeSockets.add(clientSocket);
        smtpExecutorService.submit(() -> handleConnection(clientSocket));
      }
    } catch (IOException acceptFailure) {
      if (!smtpServerSocket.isClosed()) {
        serverFailure.compareAndSet(null, acceptFailure);
      }
    }
  }

  private void handleConnection(Socket clientSocket) {
    try (clientSocket;
        BufferedReader commandReader =
            new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
        BufferedWriter replyWriter =
            new BufferedWriter(
                new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8))) {
      clientSocket.setSoTimeout(15_000);
      reply(replyWriter, "220 own SMTP uncertainty fixture");
      boolean readingData = false;
      String commandText;
      while ((commandText = commandReader.readLine()) != null) {
        if (readingData) {
          if (commandText.equals(".")) {
            receivedMessageCount.incrementAndGet();
            return; // Absichtlich kein 250 nach DATA; kein Weiterleiten oder Speichern des Inhalts.
          }
        } else if (commandText.equalsIgnoreCase("DATA")) {
          readingData = true;
          reply(replyWriter, "354 send own test data");
        } else if (commandText.equalsIgnoreCase("QUIT")) {
          reply(replyWriter, "221 closing");
          return;
        } else {
          reply(replyWriter, "250 own test server");
        }
      }
    } catch (IOException transportFailure) {
      if (!smtpServerSocket.isClosed()) {
        serverFailure.compareAndSet(null, transportFailure);
      }
    } finally {
      activeSockets.remove(clientSocket);
    }
  }

  private void reply(BufferedWriter replyWriter, String replyText) throws IOException {
    replyWriter.write(replyText + "\r\n");
    replyWriter.flush();
  }

  @Override
  public void close() throws IOException {
    smtpServerSocket.close();
    for (Socket clientSocket : activeSockets) {
      clientSocket.close();
    }
    smtpExecutorService.shutdownNow();
    smtpExecutorService.close();
  }
}
