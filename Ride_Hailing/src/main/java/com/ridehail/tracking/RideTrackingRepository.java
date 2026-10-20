package com.ridehail.tracking;

import com.ridehail.shared.SqliteStore;

import java.nio.file.Path;
import java.sql.SQLException;

public final class RideTrackingRepository {

    private final Path path;

    public RideTrackingRepository(Path path) {
        this.path = path;

        try (var db = SqliteStore.open(path);
             var sql = db.createStatement()) {

            sql.execute(
                    "CREATE TABLE IF NOT EXISTS ride_tracking (" +
                            "booking_id TEXT PRIMARY KEY, " +
                            "status TEXT NOT NULL)"
            );

        } catch (SQLException error) {
            throw SqliteStore.failure(error);
        }
    }

    public void save(String bookingId, String status) {
        try (var db = SqliteStore.open(path);
             var sql = db.prepareStatement(
                     "INSERT INTO ride_tracking(booking_id, status) " +
                             "VALUES(?, ?) " +
                             "ON CONFLICT(booking_id) DO UPDATE SET " +
                             "status=excluded.status"
             )) {

            sql.setString(1, bookingId);
            sql.setString(2, status);
            sql.executeUpdate();

        } catch (SQLException error) {
            throw SqliteStore.failure(error);
        }
    }

    public String find(String bookingId) {
        try (var db = SqliteStore.open(path);
             var sql = db.prepareStatement(
                     "SELECT status FROM ride_tracking WHERE booking_id=?"
             )) {

            sql.setString(1, bookingId);

            try (var result = sql.executeQuery()) {
                return result.next()
                        ? result.getString("status")
                        : null;
            }

        } catch (SQLException error) {
            throw SqliteStore.failure(error);
        }
    }
}


