package de.derpeterson.browsertests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Eigene dauerhafte Test-CA; nur die bewusste Einrichtung verändert Windows-Vertrauen. */
final class BrowserTestCa {
  private static final String ROOT_ALIAS = "browser-test-root";
  private static final String PASSWORD_ENV = "BROWSER_TEST_KEYSTORE_PASSWORD";
  private static final List<String> PRIVATE_WORKER_FILES =
      List.of("server.key", "server.p12", "server.csr", "issued.crt");

  private BrowserTestCa() {}

  static Path directory(Path repo) {
    return repo.resolve("data/https/browser-tests-ca");
  }

  static void initialize(Path directory) throws Exception {
    if (Files.exists(directory)) {
      validate(directory, Instant.now());
      return;
    }
    protectDirectory(directory);
    Files.createFile(directory.resolve("ca.lock"));
    String password = password();
    Files.writeString(directory.resolve("password"), password);
    keytool(
        directory,
        password,
        "-genkeypair",
        "-alias",
        ROOT_ALIAS,
        "-keyalg",
        "RSA",
        "-keysize",
        "3072",
        "-dname",
        "CN=VaadinSpringApp Browser Test CA " + UUID.randomUUID(),
        "-validity",
        "1095",
        "-ext",
        "BC=ca:true,pathlen:0",
        "-ext",
        "KU=keyCertSign,cRLSign",
        "-keystore",
        directory.resolve("root.p12").toString(),
        "-storetype",
        "PKCS12",
        "-noprompt");
    keytool(
        directory,
        password,
        "-exportcert",
        "-alias",
        ROOT_ALIAS,
        "-rfc",
        "-keystore",
        directory.resolve("root.p12").toString(),
        "-file",
        directory.resolve("root.crt").toString());
    Files.writeString(
        directory.resolve("root.sha256"),
        fingerprint(certificate(directory.resolve("root.crt")), "SHA-256"));
    validate(directory, Instant.now());
  }

  static X509Certificate validate(Path directory, Instant now) throws Exception {
    if (!Files.isRegularFile(directory.resolve("root.crt"))
        || !Files.isRegularFile(directory.resolve("root.p12"))
        || !Files.isRegularFile(directory.resolve("password"))
        || !Files.isRegularFile(directory.resolve("root.sha256"))
        || !Files.isRegularFile(directory.resolve("ca.lock"))) {
      throw new IOException(
          "Browser HTTPS is not initialized. Run browser-tests-zertifikat-einrichten.bat once while attended.");
    }
    X509Certificate root = ownedCertificate(directory);
    root.checkValidity(Date.from(now));
    root.checkValidity(Date.from(now.plus(1, ChronoUnit.DAYS)));
    if (root.getBasicConstraints() != 0 || root.getKeyUsage() == null || !root.getKeyUsage()[5]) {
      throw new IOException("Browser test CA must be a signing CA with path length zero");
    }
    KeyStore store =
        store(directory.resolve("root.p12"), Files.readString(directory.resolve("password")));
    if (!store.isKeyEntry(ROOT_ALIAS) || !root.equals(store.getCertificate(ROOT_ALIAS))) {
      throw new IOException("Browser test CA key store does not match its public certificate");
    }
    return root;
  }

  static X509Certificate ownedCertificate(Path directory) throws Exception {
    X509Certificate root = certificate(directory.resolve("root.crt"));
    if (!fingerprint(root, "SHA-256")
        .equals(Files.readString(directory.resolve("root.sha256")).strip())) {
      throw new IOException("Browser test CA ownership fingerprint mismatch");
    }
    if (!root.getSubjectX500Principal().equals(root.getIssuerX500Principal())) {
      throw new IOException("Browser test CA must be self-signed");
    }
    root.verify(root.getPublicKey());
    return root;
  }

  /** Windows-ROOT wird ausschließlich gelesen; ein Nachtlauf repariert keinen Trust. */
  static void requireTrusted(Path directory) throws Exception {
    X509Certificate root = validate(directory, Instant.now());
    requireOwnershipMarker(directory, root);
    if (!trusted(root)) {
      throw new IOException(
          "Browser test CA is not trusted by this Windows user. Run browser-tests-zertifikat-einrichten.bat while attended; no automatic import was attempted.");
    }
  }

  static boolean trusted(X509Certificate root) throws Exception {
    if (!System.getProperty("os.name").startsWith("Windows")) {
      throw new IOException("Browser HTTPS trust currently requires Windows");
    }
    KeyStore roots = KeyStore.getInstance("Windows-ROOT-CURRENTUSER");
    roots.load(null, null);
    return roots.getCertificateAlias(root) != null;
  }

  static void requireOwnershipMarker(Path directory, X509Certificate root) throws Exception {
    Path marker = directory.resolve("root.crt.owned");
    if (!Files.isRegularFile(marker)) {
      throw new IOException(
          "Browser test CA has no trust ownership marker; run the attended setup");
    }
    JsonNode data = new ObjectMapper().readTree(marker.toFile());
    // SHA-1 ist hier nur die Windows-Store-ID; Besitzprüfung verwendet zusätzlich SHA-256.
    if (!fingerprint(root, "SHA-1").equalsIgnoreCase(data.path("root").asText())
        || !data.path("intermediate").asText().isEmpty()) {
      throw new IOException("Browser test CA trust ownership mismatch");
    }
  }

  static Lease lease(Path directory, boolean shared) throws IOException {
    FileChannel channel =
        FileChannel.open(
            directory.resolve("ca.lock"),
            shared ? StandardOpenOption.READ : StandardOpenOption.WRITE);
    try {
      FileLock lock = channel.tryLock(0, Long.MAX_VALUE, shared);
      if (lock == null) {
        throw new IOException(
            "Browser test CA is in use; stop its test runs before setup or removal");
      }
      return new Lease(channel, lock);
    } catch (OverlappingFileLockException error) {
      channel.close();
      throw new IOException("Browser test CA is in use by this process", error);
    } catch (Exception error) {
      channel.close();
      throw error;
    }
  }

  record Lease(FileChannel channel, FileLock lock) implements AutoCloseable {
    @Override
    public void close() throws IOException {
      try {
        lock.release();
      } finally {
        channel.close();
      }
    }
  }

  /** Jeder Worker erhält nur sein eigenes Tageszertifikat; der CA-Schlüssel bleibt auf dem Host. */
  static void issue(Path authority, Path worker) throws Exception {
    X509Certificate root = validate(authority, Instant.now());
    protectDirectory(worker);
    if (Files.exists(worker.resolve("server.key"))) {
      throw new IOException("Refusing to overwrite an existing worker TLS identity");
    }
    String password = password();
    try {
      keytool(
          worker,
          password,
          "-genkeypair",
          "-alias",
          "server",
          "-keyalg",
          "RSA",
          "-keysize",
          "2048",
          "-dname",
          "CN=127.0.0.1",
          "-validity",
          "1",
          "-keystore",
          worker.resolve("server.p12").toString(),
          "-storetype",
          "PKCS12",
          "-noprompt");
      keytool(
          worker,
          password,
          "-certreq",
          "-alias",
          "server",
          "-rfc",
          "-keystore",
          worker.resolve("server.p12").toString(),
          "-file",
          worker.resolve("server.csr").toString());
      keytool(
          worker,
          Files.readString(authority.resolve("password")),
          "-gencert",
          "-alias",
          ROOT_ALIAS,
          "-keystore",
          authority.resolve("root.p12").toString(),
          "-infile",
          worker.resolve("server.csr").toString(),
          "-outfile",
          worker.resolve("issued.crt").toString(),
          "-rfc",
          "-validity",
          "1",
          "-ext",
          "BC=ca:false",
          "-ext",
          "KU=digitalSignature,keyEncipherment",
          "-ext",
          "EKU=serverAuth",
          "-ext",
          "SAN=ip:127.0.0.1");
      X509Certificate leaf = certificate(worker.resolve("issued.crt"));
      leaf.verify(root.getPublicKey());
      leaf.checkValidity();
      KeyStore identity = store(worker.resolve("server.p12"), password);
      Files.writeString(worker.resolve("server.crt"), pem("CERTIFICATE", leaf.getEncoded()));
      Files.writeString(worker.resolve("root.crt"), pem("CERTIFICATE", root.getEncoded()));
      Files.writeString(
          worker.resolve("server.key"),
          pem("PRIVATE KEY", identity.getKey("server", password.toCharArray()).getEncoded()));
    } finally {
      for (String file : List.of("server.p12", "server.csr", "issued.crt")) {
        Files.deleteIfExists(worker.resolve(file));
      }
    }
  }

  static void cleanupWorker(Path worker) throws IOException, InterruptedException {
    // Nach einem harten Workerabbruch können letzte eigene keytool-Dateihandles kurz nachlaufen.
    Instant deadline = Instant.now().plusSeconds(15);
    while (true) {
      try {
        for (String file : PRIVATE_WORKER_FILES) {
          Files.deleteIfExists(worker.resolve(file));
        }
        return;
      } catch (IOException error) {
        if (Instant.now().isAfter(deadline)) {
          throw error;
        }
        Thread.sleep(100);
      }
    }
  }

  static void protectDirectory(Path directory) throws IOException {
    Files.createDirectories(directory);
    AclFileAttributeView acl = Files.getFileAttributeView(directory, AclFileAttributeView.class);
    if (acl != null) {
      UserPrincipal user =
          directory
              .getFileSystem()
              .getUserPrincipalLookupService()
              .lookupPrincipalByName(System.getProperty("user.name"));
      AclEntry ownerOnly =
          AclEntry.newBuilder()
              .setType(AclEntryType.ALLOW)
              .setPrincipal(user)
              .setPermissions(EnumSet.allOf(AclEntryPermission.class))
              .setFlags(AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT)
              .build();
      acl.setAcl(List.of(ownerOnly));
    } else {
      Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx------"));
    }
  }

  static X509Certificate certificate(Path file) throws Exception {
    try (InputStream source = Files.newInputStream(file)) {
      return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(source);
    }
  }

  static String fingerprint(X509Certificate certificate, String algorithm) throws Exception {
    return HexFormat.of()
        .withUpperCase()
        .formatHex(MessageDigest.getInstance(algorithm).digest(certificate.getEncoded()));
  }

  private static KeyStore store(Path file, String password) throws Exception {
    KeyStore store = KeyStore.getInstance("PKCS12");
    try (InputStream source = Files.newInputStream(file)) {
      store.load(source, password.toCharArray());
    }
    return store;
  }

  private static String password() {
    byte[] random = new byte[32];
    new SecureRandom().nextBytes(random);
    return Base64.getEncoder().encodeToString(random);
  }

  private static String pem(String type, byte[] encoded) {
    return "-----BEGIN "
        + type
        + "-----\n"
        + Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(encoded)
        + "\n-----END "
        + type
        + "-----\n";
  }

  private static void keytool(Path outputDirectory, String password, String... arguments)
      throws Exception {
    String executable =
        System.getProperty("os.name").startsWith("Windows") ? "keytool.exe" : "keytool";
    List<String> command =
        new ArrayList<>(
            List.of(Path.of(System.getProperty("java.home"), "bin", executable).toString()));
    command.addAll(List.of(arguments));
    command.addAll(List.of("-storepass:env", PASSWORD_ENV));
    Path output = Files.createTempFile(outputDirectory, "keytool-", ".log");
    ProcessBuilder builder =
        new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile());
    builder.environment().put(PASSWORD_ENV, password);
    Process process = builder.start();
    process.getOutputStream().close();
    try {
      if (!process.waitFor(30, TimeUnit.SECONDS)) {
        throw new IOException("keytool timed out");
      }
      if (process.exitValue() != 0) {
        throw new IOException("keytool failed: " + Files.readString(output));
      }
    } finally {
      BrowserHttps.stopBeforeTrustCleanup(process);
      Files.deleteIfExists(output);
    }
  }
}
