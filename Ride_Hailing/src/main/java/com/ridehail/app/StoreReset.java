package com.ridehail.app;

import com.ridehail.shared.RideException;
import com.ridehail.shared.StorageException;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** Preserves the old store by renaming it; never deletes user records. */
public final class StoreReset {
  private StoreReset() {}

  public static String archive(Path directory) {
    Path path = directory.toAbsolutePath().normalize();
    if (Files.isSymbolicLink(path)
        || path.getParent() == null
        || path.equals(Path.of("").toAbsolutePath().normalize())) {
      throw new RideException.InvalidRequest("Refusing an unsafe store path");
    }
    if (!Files.exists(path)) return "Clean store will be created on the next command.";
    if (!Files.isDirectory(path) || !Files.isRegularFile(path.resolve("chains.sqlite"))) {
      throw new RideException.InvalidRequest(
          "Refusing to reset a directory without a chain journal");
    }
    Path backup =
        path.resolveSibling(
            path.getFileName()
                + "_backup_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + "_"
                + UUID.randomUUID().toString().substring(0, 6));
    try {
      Files.move(path, backup);
      return "Previous store archived as "
          + backup.getFileName()
          + ". A clean store will be created on the next command.";
    } catch (IOException error) {
      throw new StorageException(
          "Cannot archive the store; close other application processes first", error);
    }
  }
}
