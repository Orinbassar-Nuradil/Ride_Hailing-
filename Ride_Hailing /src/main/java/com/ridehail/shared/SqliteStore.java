package com.ridehail.shared;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Connection mechanics only: every subsystem gateway owns its schema and SQL. */
public final class SqliteStore {
  private SqliteStore() {}

  public static Connection open(Path path) {
    try {
      Files.createDirectories(path.toAbsolutePath().getParent());
      Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
      try (var sql = connection.createStatement()) {
        sql.execute("PRAGMA busy_timeout=3000");
        sql.execute("PRAGMA synchronous=FULL");
      } catch (SQLException error) {
        connection.close();
        throw error;
      }
      return connection;
    } catch (SQLException | IOException error) {
      throw new StorageException("Cannot open persistent store " + path.getFileName(), error);
    }
  }

  public static StorageException failure(SQLException error) {
    return new StorageException(
        "Persistent storage operation failed: " + error.getMessage(), error);
  }
}
