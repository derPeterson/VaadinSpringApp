package de.derpeterson.app.development;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.EnumSet;
import java.util.List;

/** Eigene kurzlebige Java-Argumentdatei, schon bei der Anlage auf den Benutzer beschränkt. */
record IdeHttpsArguments(Path directory, Path path) implements AutoCloseable {
  static IdeHttpsArguments create(Path project) throws IOException {
    FileAttribute<?> directoryPermissions = ownerPermissions(project, true);
    Path parent =
        Files.createDirectories(project.resolve("data/https/ide-launcher"), directoryPermissions);
    Path directory = Files.createTempDirectory(parent, "start-", directoryPermissions);
    try {
      Path file =
          Files.createFile(directory.resolve("launcher.args"), ownerPermissions(directory, false));
      return new IdeHttpsArguments(directory, file);
    } catch (IOException | RuntimeException failure) {
      try {
        Files.delete(directory);
      } catch (IOException cleanupFailure) {
        failure.addSuppressed(cleanupFailure);
      }
      throw failure;
    }
  }

  private static FileAttribute<?> ownerPermissions(Path base, boolean directory)
      throws IOException {
    if (Files.getFileStore(base).supportsFileAttributeView("acl")) {
      UserPrincipal owner =
          base.getFileSystem()
              .getUserPrincipalLookupService()
              .lookupPrincipalByName(System.getProperty("user.name"));
      AclEntry access =
          AclEntry.newBuilder()
              .setType(AclEntryType.ALLOW)
              .setPrincipal(owner)
              .setPermissions(EnumSet.allOf(AclEntryPermission.class))
              .build();
      return new FileAttribute<List<AclEntry>>() {
        @Override
        public String name() {
          return "acl:acl";
        }

        @Override
        public List<AclEntry> value() {
          return List.of(access);
        }
      };
    }
    // Ohne sichere initiale Rechte scheitert die Anlage; kein ungeschützter Fallback.
    return PosixFilePermissions.asFileAttribute(
        PosixFilePermissions.fromString(directory ? "rwx------" : "rw-------"));
  }

  @Override
  public void close() throws IOException {
    Files.deleteIfExists(path);
    Files.deleteIfExists(directory);
  }
}
