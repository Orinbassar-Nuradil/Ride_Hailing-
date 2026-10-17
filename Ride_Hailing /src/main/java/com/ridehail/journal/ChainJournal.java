package com.ridehail.journal;

import com.ridehail.shared.RideException;
import com.ridehail.shared.SqliteStore;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Persists requests, progress and ordered events independently of domain stores. */
public class ChainJournal {
  private final Path path, lockPath;

  public ChainJournal(Path path, Path lockPath) {
    this.path = path;
    this.lockPath = lockPath;
    try (var db = SqliteStore.open(path);
        var sql = db.createStatement()) {
      sql.execute(
          "CREATE TABLE IF NOT EXISTS chains(booking_id TEXT PRIMARY KEY,passenger TEXT NOT"
              + " NULL,driver TEXT NOT NULL,pickup TEXT NOT NULL,destination TEXT NOT NULL,max_fare"
              + " INTEGER NOT NULL,opening_balance INTEGER NOT NULL,status TEXT NOT NULL,reason"
              + " TEXT NOT NULL DEFAULT '')");
      sql.execute(
          "CREATE TABLE IF NOT EXISTS events(sequence INTEGER PRIMARY KEY AUTOINCREMENT,booking_id"
              + " TEXT NOT NULL,action TEXT NOT NULL,UNIQUE(booking_id,action))");
    } catch (SQLException error) {
      throw SqliteStore.failure(error);
    }
  }

  public <T> T exclusive(Supplier<T> operation) {
    try (var db = SqliteStore.open(lockPath);
        var tx = db.createStatement()) {
      tx.execute("BEGIN IMMEDIATE");
      try {
        T result = operation.get();
        tx.execute("COMMIT");
        return result;
      } catch (RuntimeException | Error error) {
        try {
          tx.execute("ROLLBACK");
        } catch (SQLException rollback) {
          error.addSuppressed(rollback);
        }
        throw error;
      }
    } catch (SQLException error) {
      throw SqliteStore.failure(error);
    }
  }

  public void start(BookingRequest request) {
    if (ids().contains(request.bookingId())) {
      if (!get(request.bookingId()).request().equals(request))
        throw new RideException.BookingConflict("Booking ID was already used with different input");
      return;
    }
    try (var db = SqliteStore.open(path);
        var sql =
            db.prepareStatement(
                "INSERT INTO"
                    + " chains(booking_id,passenger,driver,pickup,destination,max_fare,opening_balance,status)"
                    + " VALUES(?,?,?,?,?,?,?,'PENDING_A')")) {
      sql.setString(1, request.bookingId());
      sql.setString(2, request.passenger());
      sql.setString(3, request.driver());
      sql.setString(4, request.pickup());
      sql.setString(5, request.destination());
      sql.setInt(6, request.maxFare());
      sql.setInt(7, request.openingBalance());
      sql.executeUpdate();
    } catch (SQLException error) {
      throw SqliteStore.failure(error);
    }
  }

  public ChainState get(String id) {
    try (var db = SqliteStore.open(path);
        var sql = db.prepareStatement("SELECT * FROM chains WHERE booking_id=?")) {
      sql.setString(1, id);
      try (var row = sql.executeQuery()) {
        if (!row.next()) throw new RideException.UnknownBooking("No booking named " + id);
        var request =
            new BookingRequest(
                id,
                row.getString("passenger"),
                row.getString("driver"),
                row.getString("pickup"),
                row.getString("destination"),
                row.getInt("max_fare"),
                row.getInt("opening_balance"));
        List<String> events = new ArrayList<>();
        try (var eventQuery =
            db.prepareStatement("SELECT action FROM events WHERE booking_id=? ORDER BY sequence")) {
          eventQuery.setString(1, id);
          try (var rows = eventQuery.executeQuery()) {
            while (rows.next()) events.add(rows.getString(1));
          }
        }
        return new ChainState(request, row.getString("status"), row.getString("reason"), events);
      }
    } catch (SQLException error) {
      throw SqliteStore.failure(error);
    }
  }

  public void update(String id, String status, String event, String reason) {
    try (var db = SqliteStore.open(path)) {
      db.setAutoCommit(false);
      try (var sql =
          db.prepareStatement(
              "UPDATE chains SET status=?,reason=COALESCE(?,reason) WHERE booking_id=?")) {
        sql.setString(1, status);
        sql.setString(2, reason);
        sql.setString(3, id);
        if (sql.executeUpdate() != 1)
          throw new RideException.UnknownBooking("No booking named " + id);
      }
      if (event != null) {
        try (var sql =
            db.prepareStatement("INSERT OR IGNORE INTO events(booking_id,action) VALUES(?,?)")) {
          sql.setString(1, id);
          sql.setString(2, event);
          sql.executeUpdate();
        }
      }
      db.commit();
    } catch (SQLException error) {
      throw SqliteStore.failure(error);
    }
  }

  public List<String> ids() {
    try (var db = SqliteStore.open(path);
        var sql = db.createStatement();
        var rows = sql.executeQuery("SELECT booking_id FROM chains ORDER BY booking_id")) {
      List<String> result = new ArrayList<>();
      while (rows.next()) result.add(rows.getString(1));
      return List.copyOf(result);
    } catch (SQLException error) {
      throw SqliteStore.failure(error);
    }
  }
}
