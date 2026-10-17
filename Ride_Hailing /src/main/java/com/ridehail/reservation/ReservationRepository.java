package com.ridehail.reservation;

import com.ridehail.shared.SqliteStore;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/** A's own gateway; it talks to reservations.sqlite directly. */
public final class ReservationRepository {
  private final Path path;

  public ReservationRepository(Path path) {
    this.path = path;
    try (var db = SqliteStore.open(path);
        var sql = db.createStatement()) {
      sql.execute(
          "CREATE TABLE IF NOT EXISTS reservations(booking_id TEXT PRIMARY KEY, driver TEXT NOT"
              + " NULL, pickup TEXT NOT NULL, destination TEXT NOT NULL, distance_km INTEGER NOT"
              + " NULL, status TEXT NOT NULL CHECK(status IN ('NEW','RESERVED','RELEASED')))");
      sql.execute(
          "CREATE UNIQUE INDEX IF NOT EXISTS active_driver ON reservations(driver) WHERE"
              + " status='RESERVED'");
    } catch (SQLException error) {
      throw SqliteStore.failure(error);
    }
  }

  public DriverReservation find(String id) {
    try (var db = SqliteStore.open(path);
        var sql = db.prepareStatement("SELECT * FROM reservations WHERE booking_id=?")) {
      sql.setString(1, id);
      try (var row = sql.executeQuery()) {
        return row.next()
            ? DriverReservation.restore(
                row.getString("booking_id"),
                row.getString("driver"),
                row.getString("pickup"),
                row.getString("destination"),
                row.getInt("distance_km"),
                row.getString("status"))
            : null;
      }
    } catch (SQLException error) {
      throw SqliteStore.failure(error);
    }
  }

  public Set<String> occupied() {
    try (var db = SqliteStore.open(path);
        var sql = db.createStatement();
        var rows = sql.executeQuery("SELECT driver FROM reservations WHERE status='RESERVED'")) {
      Set<String> result = new HashSet<>();
      while (rows.next()) result.add(rows.getString(1));
      return result;
    } catch (SQLException error) {
      throw SqliteStore.failure(error);
    }
  }

  public void save(DriverReservation item) {
    try (var db = SqliteStore.open(path);
        var sql =
            db.prepareStatement(
                "INSERT INTO reservations VALUES(?,?,?,?,?,?) ON CONFLICT(booking_id) DO UPDATE SET"
                    + " status=excluded.status")) {
      sql.setString(1, item.bookingId());
      sql.setString(2, item.driver());
      sql.setString(3, item.pickup());
      sql.setString(4, item.destination());
      sql.setInt(5, item.distanceKm());
      sql.setString(6, item.status());
      sql.executeUpdate();
    } catch (SQLException error) {
      if (error.getErrorCode() == 19)
        throw new com.ridehail.shared.RideException.DriverUnavailable(
            "Driver " + item.driver() + " was reserved concurrently");
      throw SqliteStore.failure(error);
    }
  }
}
