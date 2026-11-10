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
record IdeHttpsArguments(Path argumentDirectory, Path argumentFilePath) implements AutoCloseable {
  static IdeHttpsArguments createPrivateArgumentFile(Path projectDirectory) throws IOException {
    FileAttribute<?> directoryPermissions = ownerPermissions(projectDirectory, true);
    Path launcherParentDirectory =
        Files.createDirectories(
            projectDirectory.resolve("data/https/ide-launcher"), directoryPermissions);
    Path argumentDirectory =
        Files.createTempDirectory(launcherParentDirectory, "start-", directoryPermissions);

    try {
      Path argumentFilePath =
          Files.createFile(
              argumentDirectory.resolve("launcher.args"),
              ownerPermissions(argumentDirectory, false));
      return new IdeHttpsArguments(argumentDirectory, argumentFilePath);
    } catch (IOException | RuntimeException argumentFileCreationFailure) {
      try {
        Files.delete(argumentDirectory);
      } catch (IOException cleanupFailure) {
        argumentFileCreationFailure.addSuppressed(cleanupFailure);
      }
      throw argumentFileCreationFailure;
    }
  }

  private static FileAttribute<?> ownerPermissions(
      Path permissionReferencePath, boolean forDirectory) throws IOException {
    if (Files.getFileStore(permissionReferencePath).supportsFileAttributeView("acl")) {
      UserPrincipal ownerPrincipal =
          permissionReferencePath
              .getFileSystem()
              .getUserPrincipalLookupService()
              .lookupPrincipalByName(System.getProperty("user.name"));

      AclEntry ownerAclEntry =
          AclEntry.newBuilder()
              .setType(AclEntryType.ALLOW)
              .setPrincipal(ownerPrincipal)
              .setPermissions(EnumSet.allOf(AclEntryPermission.class))
              .build();

      return new FileAttribute<List<AclEntry>>() {
        @Override
        public String name() {
          return "acl:acl";
        }

        @Override
        public List<AclEntry> value() {
          return List.of(ownerAclEntry);
        }
      };
    }
    // Ohne sichere initiale Rechte scheitert die Anlage; kein ungeschützter Fallback.
    return PosixFilePermissions.asFileAttribute(
        PosixFilePermissions.fromString(forDirectory ? "rwx------" : "rw-------"));
  }

  @Override
  public void close() throws IOException {
    Files.deleteIfExists(argumentFilePath);
    Files.deleteIfExists(argumentDirectory);
  }
}
