package com.ridehail.pricing;

import com.ridehail.shared.SqliteStore;
import java.nio.file.Path;
import java.sql.SQLException;

/** B owns fares.sqlite; it never queries A, C or the chain journal. */
public final class FareRepository {
  private final Path path;

  public FareRepository(Path path) {
    this.path = path;
    try (var db = SqliteStore.open(path);
        var sql = db.createStatement()) {
      sql.execute(
          "CREATE TABLE IF NOT EXISTS receipts(booking_id TEXT PRIMARY KEY, distance_km INTEGER NOT"
              + " NULL, amount INTEGER NOT NULL, receipt_number TEXT NOT NULL UNIQUE, status TEXT"
              + " NOT NULL CHECK(status IN ('NEW','ISSUED','VOIDED')))");
    } catch (SQLException error) {
      throw SqliteStore.failure(error);
    }
  }

  public FareReceipt find(String id) {
    try (var db = SqliteStore.open(path);
        var sql = db.prepareStatement("SELECT * FROM receipts WHERE booking_id=?")) {
      sql.setString(1, id);
      try (var row = sql.executeQuery()) {
        return row.next()
            ? FareReceipt.restore(
                row.getString("booking_id"),
                row.getInt("distance_km"),
                row.getInt("amount"),
                row.getString("receipt_number"),
                row.getString("status"))
            : null;
      }
    } catch (SQLException error) {
      throw SqliteStore.failure(error);
    }
  }

  public void save(FareReceipt item) {
    try (var db = SqliteStore.open(path);
        var sql =
            db.prepareStatement(
                "INSERT INTO receipts VALUES(?,?,?,?,?) ON CONFLICT(booking_id) DO UPDATE SET"
                    + " status=excluded.status")) {
      sql.setString(1, item.bookingId());
      sql.setInt(2, item.distanceKm());
      sql.setInt(3, item.amount());
      sql.setString(4, item.receiptNumber());
      sql.setString(5, item.status());
      sql.executeUpdate();
    } catch (SQLException error) {
      throw SqliteStore.failure(error);
    }
  }
}
