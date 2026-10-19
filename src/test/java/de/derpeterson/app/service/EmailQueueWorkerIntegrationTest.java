package de.derpeterson.app.service;

import static de.derpeterson.app.service.EmailQueueWorkerPoolTest.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.derpeterson.app.config.MailConfig;
import de.derpeterson.app.config.ThreadPoolConfig;
import de.derpeterson.app.model.EmailQueueEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.*;
import de.derpeterson.app.repository.AdminNotificationAttemptRepository;
import de.derpeterson.app.repository.EmailQueueRepository;
import de.derpeterson.app.repository.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Real worker threads, isolated H2 and an own loopback SMTP fixture; no Boot app or provider. */
@SpringJUnitConfig(EmailQueueServicePersistenceTest.Config.class)
@Execution(ExecutionMode.SAME_THREAD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EmailQueueWorkerIntegrationTest {
  @Autowired ConfigService config;
  @Autowired EmailQueueRepository emails;
  @Autowired UserRepository users;
  @Autowired AdminNotificationAttemptRepository attempts;
  @Autowired EmailQueueServicePersistenceTest.Probes probes;
  @Autowired JpaTransactionManager manager;
  @Autowired TransactionTemplate tx;
  @Autowired EmailService mail;
  @Autowired MutableQueueClock clock;
  private final List<EmailQueueWorkerPool> pools = new ArrayList<>();
  private Long userId;

  @BeforeEach
  void seed() {
    reset(config, mail, probes.emails(), probes.users(), probes.adminAttempts());
    clock.set(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
    when(config.getInteger(ConfigEntry.EMAIL_QUEUE_POOL_SIZE)).thenReturn(2);
    when(config.getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY)).thenReturn(50);
    when(config.getInteger(ConfigEntry.EMAIL_QUEUE_MAX_RETRY)).thenReturn(0);
    when(config.getString(ConfigEntry.EMAIL_ADMIN)).thenReturn("admin@example.test");
    tx.executeWithoutResult(
        status -> {
          attempts.deleteAll();
          emails.deleteAll();
          users.deleteAll();
          users.flush();
          userId =
              users
                  .saveAndFlush(
                      UserEntity.builder()
                          .firstName("Worker")
                          .lastName("Fixture")
                          .email("recipient@example.test")
                          .password("isolated-hash")
                          .gender(Gender.OTHER)
                          .birthDate(LocalDate.of(1990, 1, 1))
                          .preferredLocale(Locale.ENGLISH)
                          .roleEntities(new ArrayList<>())
                          .build())
                  .getId();
        });
  }

  @AfterEach
  void closeWorkers() {
    pools.forEach(EmailQueueWorkerPool::shutdown);
  }

  EmailQueueWorkerPool pool() {
    var pool = new EmailQueueWorkerPool(new ThreadPoolConfig(config).taskExecutor());
    pools.add(pool);
    return pool;
  }

  EmailQueueService service(EmailQueueWorkerPool pool, EmailService sender) {
    return new EmailQueueService(
        config,
        probes.emails(),
        sender,
        probes.users(),
        manager,
        probes.adminAttempts(),
        clock,
        pool);
  }

  Long insert(String subject, LocalDateTime retry) {
    return tx.execute(
        status -> {
          var item = new EmailQueueEntity();
          item.setUserEntity(users.findById(userId).orElseThrow());
          item.setSubject(subject);
          item.setBody("<p>body:" + subject + "</p>");
          item.setStatus(EmailStatus.PENDING);
          item.setEmailType(EmailType.NOTIFICATION);
          item.setCreatedAt(LocalDateTime.now(clock).minusMinutes(2));
          item.setLastRetryAt(retry);
          return emails.saveAndFlush(item).getId();
        });
  }

  private long statusCount(EmailStatus status) {
    return emails.findAll().stream().filter(entry -> entry.getStatus() == status).count();
  }

  @Test
  void realSmtpUsesTwoConcurrentTransportsAndRefillsWithoutMimeMixups() throws Exception {
    try (var smtp = new SmtpFixture(2);
        var caller = Executors.newSingleThreadExecutor()) {
      when(config.getString(ConfigEntry.MAIL_HOST)).thenReturn("127.0.0.1");
      when(config.getInteger(ConfigEntry.MAIL_PORT)).thenReturn(smtp.port());
      when(config.getString(ConfigEntry.MAIL_SMTP_SSL_TRUST)).thenReturn("127.0.0.1");
      when(config.getString(ConfigEntry.MAIL_DEBUG)).thenReturn("false");
      when(config.getString(ConfigEntry.EMAIL_FROM)).thenReturn("sender@example.test");
      when(config.getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY)).thenReturn(3);
      var sender = new MailConfig(config).javaMailSender();
      var properties =
          ((org.springframework.mail.javamail.JavaMailSenderImpl) sender).getJavaMailProperties();
      properties.put("mail.smtp.connectiontimeout", "5000");
      properties.put("mail.smtp.timeout", "5000");
      properties.put("mail.smtp.writetimeout", "5000");
      var pendingRetry = insert("not-due", LocalDateTime.now(clock).minusSeconds(20));
      var ids =
          List.of(insert("smtp-one", null), insert("smtp-two", null), insert("smtp-three", null));
      var run = caller.submit(service(pool(), new EmailService(config, sender))::processQueue);
      try {
        await(smtp.firstWave);
        assertEquals(2, smtp.messages.size());
        assertEquals(2, statusCount(EmailStatus.IN_PROGRESS));
        assertEquals(2, statusCount(EmailStatus.PENDING));
        assertFalse(run.isDone());
      } finally {
        smtp.release.countDown();
      }
      run.get(15, TimeUnit.SECONDS);
      for (var id : ids)
        assertEquals(EmailStatus.SENT, emails.findById(id).orElseThrow().getStatus());
      assertEquals(EmailStatus.PENDING, emails.findById(pendingRetry).orElseThrow().getStatus());
      assertEquals(Set.of("smtp-one", "smtp-two", "smtp-three"), smtp.messages.keySet());
      smtp.messages.forEach(
          (subject, body) -> assertEquals("<p>body:" + subject + "</p>\r\n", body));
      assertTrue(smtp.failures.isEmpty(), () -> smtp.failures.toString());
    }
  }

  @Test
  void batchCapacityBoundsSelectionDespiteFreeWorkers() throws Exception {
    when(config.getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY)).thenReturn(2);
    var ids = List.of(insert("one", null), insert("two", null), insert("three", null));
    service(pool(), mail).processQueue();
    assertEquals(EmailStatus.SENT, emails.findById(ids.get(0)).orElseThrow().getStatus());
    assertEquals(EmailStatus.SENT, emails.findById(ids.get(1)).orElseThrow().getStatus());
    assertEquals(EmailStatus.PENDING, emails.findById(ids.get(2)).orElseThrow().getStatus());
    verify(mail, times(2)).sendEmail(any(), any(), any());
  }

  @Test
  void twoIndependentPoolsShareDatabaseClaimsWithoutDoubleSending() throws Exception {
    var ids = List.of(insert("one", null), insert("two", null), insert("three", null));
    var selected = new CyclicBarrier(2);
    doAnswer(
            call -> {
              var result = emails.findPendingEmails(call.getArgument(0), call.getArgument(1));
              selected.await(10, TimeUnit.SECONDS);
              return result;
            })
        .when(probes.emails())
        .findPendingEmails(any(), any());
    try (var callers = Executors.newFixedThreadPool(2)) {
      var first = callers.submit(service(pool(), mail)::processQueue);
      var second = callers.submit(service(pool(), mail)::processQueue);
      first.get(15, TimeUnit.SECONDS);
      second.get(15, TimeUnit.SECONDS);
    }
    for (var id : ids)
      assertEquals(EmailStatus.SENT, emails.findById(id).orElseThrow().getStatus());
    for (String subject : List.of("one", "two", "three"))
      verify(mail).sendEmail(any(), eq(subject), any());
  }

  @Test
  void rejectedTasksNeverClaimTheirCandidates() throws Exception {
    var id = insert("pending", null);
    var executor = new ThreadPoolConfig(config).taskExecutor();
    var pool = new EmailQueueWorkerPool(executor);
    pools.add(pool);
    executor.shutdown();
    service(pool, mail).processQueue();
    assertEquals(EmailStatus.PENDING, emails.findById(id).orElseThrow().getStatus());
    verifyNoInteractions(mail);
  }

  @Test
  void forcedShutdownKeepsACommittedClaimAndUnstartedCandidatesIntact() throws Exception {
    when(config.getInteger(ConfigEntry.EMAIL_QUEUE_POOL_SIZE)).thenReturn(1);
    var firstId = insert("started", null);
    var nextId = insert("not-started", null);
    var executor = new ThreadPoolConfig(config).taskExecutor();
    var pool = new EmailQueueWorkerPool(executor, Duration.ZERO);
    pools.add(pool);
    var sending = new CountDownLatch(1);
    doAnswer(
            call -> {
              assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
              sending.countDown();
              try {
                new CountDownLatch(1).await();
              } catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("uncertain dispatch", failure);
              }
              return null;
            })
        .when(mail)
        .sendEmail(any(), any(), any());
    try (var caller = Executors.newSingleThreadExecutor()) {
      var run = caller.submit(service(pool, mail)::processQueue);
      await(sending);
      assertEquals(EmailStatus.IN_PROGRESS, emails.findById(firstId).orElseThrow().getStatus());
      pool.shutdown();
      run.get(10, TimeUnit.SECONDS);
      assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }
    var claimed = emails.findById(firstId).orElseThrow();
    assertEquals(EmailStatus.IN_PROGRESS, claimed.getStatus());
    assertEquals(0, claimed.getRetryCount());
    assertNull(claimed.getLastRetryAt());
    assertEquals(EmailStatus.PENDING, emails.findById(nextId).orElseThrow().getStatus());
    verify(mail).sendEmail(any(), eq("started"), any());
    verify(mail, never()).sendEmail(any(), eq("not-started"), any());
    assertEquals(0, attempts.count());
  }

  @Test
  void parallelOutcomesAndAdminAuditStayIndependent() throws Exception {
    var healthy = insert("healthy", null);
    var known = insert("known", null);
    var unknown = insert("unknown", null);
    doThrow(new MailAuthenticationException("fixture authentication failure"))
        .when(mail)
        .sendEmail(any(), eq("known"), any());
    doThrow(new MailSendException("fixture unknown SMTP outcome"))
        .when(mail)
        .sendEmail(any(), eq("unknown"), any());
    var service = service(pool(), mail);
    service.processQueue();
    assertEquals(EmailStatus.SENT, emails.findById(healthy).orElseThrow().getStatus());
    assertEquals(EmailStatus.FAILED, emails.findById(known).orElseThrow().getStatus());
    assertEquals(EmailStatus.IN_PROGRESS, emails.findById(unknown).orElseThrow().getStatus());
    var audit = attempts.findAll().getFirst();
    assertEquals(known, audit.getQueueId());
    assertEquals(AdminNotificationStatus.SENT, audit.getStatus());
    service.processQueue();
    verify(mail).sendAdminEmail(any(), any(), any());
    assertEquals(1, attempts.count());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void failedClaimOrResultCommitDoesNotRollbackAnotherWorker(boolean resultCommit)
      throws Exception {
    var affected = insert("affected", null);
    var healthy = insert("healthy", null);
    var injected = new AtomicBoolean();
    doAnswer(
            call -> {
              EmailQueueEntity item = call.getArgument(0);
              if (item.getSubject().equals("affected")
                  && item.getStatus() == (resultCommit ? EmailStatus.SENT : EmailStatus.IN_PROGRESS)
                  && injected.compareAndSet(false, true)) {
                TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                      @Override
                      public void beforeCommit(boolean readOnly) {
                        throw new IllegalStateException("fixture commit failure");
                      }
                    });
              }
              return emails.save(item);
            })
        .when(probes.emails())
        .save(any());
    service(pool(), mail).processQueue();
    assertTrue(injected.get());
    assertEquals(
        resultCommit ? EmailStatus.IN_PROGRESS : EmailStatus.PENDING,
        emails.findById(affected).orElseThrow().getStatus());
    assertEquals(EmailStatus.SENT, emails.findById(healthy).orElseThrow().getStatus());
    verify(mail, times(resultCommit ? 1 : 0)).sendEmail(any(), eq("affected"), any());
  }

  static class SmtpFixture implements AutoCloseable {
    final ServerSocket server;
    final ExecutorService handlers = Executors.newVirtualThreadPerTaskExecutor();
    final Set<Socket> sockets = ConcurrentHashMap.newKeySet();
    final Map<String, String> messages = new ConcurrentHashMap<>();
    final Queue<Throwable> failures = new ConcurrentLinkedQueue<>();
    final CountDownLatch firstWave;
    final CountDownLatch release = new CountDownLatch(1);

    SmtpFixture(int parallel) throws IOException {
      firstWave = new CountDownLatch(parallel);
      server = new ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"));
      handlers.submit(
          () -> {
            while (!server.isClosed()) {
              try {
                Socket socket = server.accept();
                sockets.add(socket);
                handlers.submit(() -> handle(socket));
              } catch (IOException failure) {
                if (!server.isClosed()) failures.add(failure);
              }
            }
          });
    }

    int port() {
      return server.getLocalPort();
    }

    void handle(Socket socket) {
      try (socket;
          var reader =
              new BufferedReader(
                  new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
          var writer =
              new BufferedWriter(
                  new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
        socket.setSoTimeout(10000);
        reply(writer, "220 fixture ESMTP");
        String command;
        while ((command = reader.readLine()) != null) {
          if (command.startsWith("EHLO") || command.startsWith("HELO"))
            reply(writer, "250 fixture");
          else if (command.startsWith("MAIL FROM:")
              || command.startsWith("RCPT TO:")
              || command.equals("RSET")) reply(writer, "250 OK");
          else if (command.equals("DATA")) {
            reply(writer, "354 Continue");
            var data = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null && !line.equals(".")) {
              data.append(line.startsWith("..") ? line.substring(1) : line).append("\r\n");
            }
            var message =
                new MimeMessage(
                    Session.getInstance(new Properties()),
                    new ByteArrayInputStream(data.toString().getBytes(StandardCharsets.UTF_8)));
            assertEquals("recipient@example.test", message.getAllRecipients()[0].toString());
            assertNull(messages.putIfAbsent(message.getSubject(), message.getContent().toString()));
            firstWave.countDown();
            await(release);
            reply(writer, "250 accepted");
          } else if (command.equals("QUIT")) {
            reply(writer, "221 bye");
            break;
          } else reply(writer, "502 unsupported");
        }
      } catch (Throwable failure) {
        if (!server.isClosed()) failures.add(failure);
      } finally {
        sockets.remove(socket);
      }
    }

    void reply(BufferedWriter writer, String response) throws IOException {
      writer.write(response + "\r\n");
      writer.flush();
    }

    @Override
    public void close() throws Exception {
      release.countDown();
      server.close();
      for (Socket socket : sockets) socket.close();
      handlers.shutdownNow();
      assertTrue(handlers.awaitTermination(10, TimeUnit.SECONDS));
    }
  }
}
