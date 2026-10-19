package de.derpeterson.app.service;

import static de.derpeterson.app.service.EmailQueueWorkerPoolTest.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.derpeterson.app.config.MailConfig;
import de.derpeterson.app.config.ThreadPoolConfig;
import de.derpeterson.app.model.AdminNotificationAttemptEntity;
import de.derpeterson.app.model.EmailQueueEntity;
import de.derpeterson.app.model.UserEntity;
import de.derpeterson.app.model.enums.AdminNotificationStatus;
import de.derpeterson.app.model.enums.ConfigEntry;
import de.derpeterson.app.model.enums.EmailStatus;
import de.derpeterson.app.model.enums.EmailType;
import de.derpeterson.app.model.enums.Gender;
import de.derpeterson.app.repository.AdminNotificationAttemptRepository;
import de.derpeterson.app.repository.EmailQueueRepository;
import de.derpeterson.app.repository.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
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
    EmailQueueWorkerPool pool =
        new EmailQueueWorkerPool(new ThreadPoolConfig(config).taskExecutor());
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
          EmailQueueEntity item = new EmailQueueEntity();
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
    try (SmtpFixture smtp = new SmtpFixture(2);
        ExecutorService caller = Executors.newSingleThreadExecutor()) {
      when(config.getString(ConfigEntry.MAIL_HOST)).thenReturn("127.0.0.1");
      when(config.getInteger(ConfigEntry.MAIL_PORT)).thenReturn(smtp.port());
      when(config.getString(ConfigEntry.MAIL_SMTP_SSL_TRUST)).thenReturn("127.0.0.1");
      when(config.getString(ConfigEntry.MAIL_DEBUG)).thenReturn("false");
      when(config.getString(ConfigEntry.EMAIL_FROM)).thenReturn("sender@example.test");
      when(config.getInteger(ConfigEntry.EMAIL_QUEUE_CAPACITY)).thenReturn(3);
      JavaMailSender sender = new MailConfig(config).javaMailSender();
      Properties properties =
          ((org.springframework.mail.javamail.JavaMailSenderImpl) sender).getJavaMailProperties();
      properties.put("mail.smtp.connectiontimeout", "5000");
      properties.put("mail.smtp.timeout", "5000");
      properties.put("mail.smtp.writetimeout", "5000");
      Long pendingRetry = insert("not-due", LocalDateTime.now(clock).minusSeconds(20));
      List<Long> ids =
          List.of(insert("smtp-one", null), insert("smtp-two", null), insert("smtp-three", null));
      Future<?> run =
          caller.submit(service(pool(), new EmailService(config, sender))::processQueue);
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
      for (Long id : ids) {
        assertEquals(EmailStatus.SENT, emails.findById(id).orElseThrow().getStatus());
      }
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
    List<Long> ids = List.of(insert("one", null), insert("two", null), insert("three", null));
    service(pool(), mail).processQueue();
    assertEquals(EmailStatus.SENT, emails.findById(ids.get(0)).orElseThrow().getStatus());
    assertEquals(EmailStatus.SENT, emails.findById(ids.get(1)).orElseThrow().getStatus());
    assertEquals(EmailStatus.PENDING, emails.findById(ids.get(2)).orElseThrow().getStatus());
    verify(mail, times(2)).sendEmail(any(), any(), any());
  }

  @Test
  void twoIndependentPoolsShareDatabaseClaimsWithoutDoubleSending() throws Exception {
    List<Long> ids = List.of(insert("one", null), insert("two", null), insert("three", null));
    CyclicBarrier selected = new CyclicBarrier(2);
    doAnswer(
            call -> {
              List<EmailQueueEntity> result =
                  emails.findPendingEmails(call.getArgument(0), call.getArgument(1));
              selected.await(10, TimeUnit.SECONDS);
              return result;
            })
        .when(probes.emails())
        .findPendingEmails(any(), any());
    try (ExecutorService callers = Executors.newFixedThreadPool(2)) {
      Future<?> first = callers.submit(service(pool(), mail)::processQueue);
      Future<?> second = callers.submit(service(pool(), mail)::processQueue);
      first.get(15, TimeUnit.SECONDS);
      second.get(15, TimeUnit.SECONDS);
    }
    for (Long id : ids) {
      assertEquals(EmailStatus.SENT, emails.findById(id).orElseThrow().getStatus());
    }
    for (String subject : List.of("one", "two", "three")) {
      verify(mail).sendEmail(any(), eq(subject), any());
    }
  }

  @Test
  void rejectedTasksNeverClaimTheirCandidates() throws Exception {
    Long id = insert("pending", null);
    ThreadPoolExecutor executor = new ThreadPoolConfig(config).taskExecutor();
    EmailQueueWorkerPool pool = new EmailQueueWorkerPool(executor);
    pools.add(pool);
    executor.shutdown();
    service(pool, mail).processQueue();
    assertEquals(EmailStatus.PENDING, emails.findById(id).orElseThrow().getStatus());
    verifyNoInteractions(mail);
  }

  @Test
  void forcedShutdownKeepsACommittedClaimAndUnstartedCandidatesIntact() throws Exception {
    when(config.getInteger(ConfigEntry.EMAIL_QUEUE_POOL_SIZE)).thenReturn(1);
    Long firstId = insert("started", null);
    Long nextId = insert("not-started", null);
    ThreadPoolExecutor executor = new ThreadPoolConfig(config).taskExecutor();
    EmailQueueWorkerPool pool = new EmailQueueWorkerPool(executor, Duration.ZERO);
    pools.add(pool);
    CountDownLatch sending = new CountDownLatch(1);
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
    try (ExecutorService caller = Executors.newSingleThreadExecutor()) {
      Future<?> run = caller.submit(service(pool, mail)::processQueue);
      await(sending);
      assertEquals(EmailStatus.IN_PROGRESS, emails.findById(firstId).orElseThrow().getStatus());
      pool.shutdown();
      run.get(10, TimeUnit.SECONDS);
      assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }
    EmailQueueEntity claimed = emails.findById(firstId).orElseThrow();
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
    Long healthy = insert("healthy", null);
    Long known = insert("known", null);
    Long unknown = insert("unknown", null);
    doThrow(new MailAuthenticationException("fixture authentication failure"))
        .when(mail)
        .sendEmail(any(), eq("known"), any());
    doThrow(new MailSendException("fixture unknown SMTP outcome"))
        .when(mail)
        .sendEmail(any(), eq("unknown"), any());
    EmailQueueService service = service(pool(), mail);
    service.processQueue();
    assertEquals(EmailStatus.SENT, emails.findById(healthy).orElseThrow().getStatus());
    assertEquals(EmailStatus.FAILED, emails.findById(known).orElseThrow().getStatus());
    assertEquals(EmailStatus.IN_PROGRESS, emails.findById(unknown).orElseThrow().getStatus());
    AdminNotificationAttemptEntity audit = attempts.findAll().getFirst();
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
    Long affected = insert("affected", null);
    Long healthy = insert("healthy", null);
    AtomicBoolean injected = new AtomicBoolean();
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
                if (!server.isClosed()) {
                  failures.add(failure);
                }
              }
            }
          });
    }

    int port() {
      return server.getLocalPort();
    }

    void handle(Socket socket) {
      try (socket;
          BufferedReader reader =
              new BufferedReader(
                  new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
          BufferedWriter writer =
              new BufferedWriter(
                  new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
        socket.setSoTimeout(10000);
        reply(writer, "220 fixture ESMTP");
        String command;
        while ((command = reader.readLine()) != null) {
          if (command.startsWith("EHLO") || command.startsWith("HELO")) {
            reply(writer, "250 fixture");
          } else if (command.startsWith("MAIL FROM:")
              || command.startsWith("RCPT TO:")
              || command.equals("RSET")) {
            reply(writer, "250 OK");
          } else if (command.equals("DATA")) {
            reply(writer, "354 Continue");
            StringBuilder data = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null && !line.equals(".")) {
              data.append(line.startsWith("..") ? line.substring(1) : line).append("\r\n");
            }
            MimeMessage message =
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
          } else {
            reply(writer, "502 unsupported");
          }
        }
      } catch (Throwable failure) {
        if (!server.isClosed()) {
          failures.add(failure);
        }
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
      for (Socket socket : sockets) {
        socket.close();
      }
      handlers.shutdownNow();
      assertTrue(handlers.awaitTermination(10, TimeUnit.SECONDS));
    }
  }
}
