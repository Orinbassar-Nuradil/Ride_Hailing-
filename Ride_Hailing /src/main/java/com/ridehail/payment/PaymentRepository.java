package com.ridehail.payment;

import com.ridehail.shared.RideException;
import com.ridehail.shared.SqliteStore;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

/** C's own gateway commits wallet, payment and invitation in one local transaction. */
public final class PaymentRepository {
  private final Path path;

  public PaymentRepository(Path path) {
    this.path = path;
    try (var db = SqliteStore.open(path);
        var sql = db.createStatement()) {
      sql.execute(
          "CREATE TABLE IF NOT EXISTS wallets(passenger TEXT PRIMARY KEY,balance INTEGER NOT NULL"
              + " CHECK(balance>=0))");
      sql.execute(
          "CREATE TABLE IF NOT EXISTS payments(booking_id TEXT PRIMARY KEY,passenger TEXT NOT"
              + " NULL,amount INTEGER NOT NULL,status TEXT NOT NULL CHECK(status IN"
              + " ('NEW','SETTLED','DECLINED')),rating_invitation TEXT NOT NULL)");
    } catch (SQLException error) {
      throw SqliteStore.failure(error);
    }
  }

  private PaymentRecord find(Connection db, String id) throws SQLException {
    try (var sql = db.prepareStatement("SELECT * FROM payments WHERE booking_id=?")) {
      sql.setString(1, id);
      try (var row = sql.executeQuery()) {
        return row.next()
            ? PaymentRecord.restore(
                row.getString("booking_id"),
                row.getString("passenger"),
                row.getInt("amount"),
                row.getString("status"),
                row.getString("rating_invitation"))
            : null;
      }
    }
  }

  public PaymentRecord find(String id) {
    try (var db = SqliteStore.open(path)) {
      return find(db, id);
    } catch (SQLException error) {
      throw SqliteStore.failure(error);
    }
  }

  public Integer balance(String passenger) {
    try (var db = SqliteStore.open(path);
        var sql = db.prepareStatement("SELECT balance FROM wallets WHERE passenger=?")) {
      sql.setString(1, passenger);
      try (var row = sql.executeQuery()) {
        return row.next() ? row.getInt(1) : null;
      }
    } catch (SQLException error) {
      throw SqliteStore.failure(error);
    }
  }

  public PaymentRecord settle(
      PaymentRecord item, int openingBalance, PaymentProcessor processor, RatingService ratings) {
    try (var db = SqliteStore.open(path);
        var tx = db.createStatement()) {
      tx.execute("BEGIN IMMEDIATE");
      try {
        var existing = find(db, item.bookingId());
        if (existing != null) {
          if (!existing.passenger().equals(item.passenger()) || existing.amount() != item.amount())
            throw new RideException.BookingConflict(
                "Payment ID already has different payment data");
          tx.execute("COMMIT");
          return existing;
        }
        try (var sql = db.prepareStatement("INSERT OR IGNORE INTO wallets VALUES(?,?)")) {
          sql.setString(1, item.passenger());
          sql.setInt(2, openingBalance);
          sql.executeUpdate();
        }
        int balance;
        try (var sql = db.prepareStatement("SELECT balance FROM wallets WHERE passenger=?")) {
          sql.setString(1, item.passenger());
          try (var row = sql.executeQuery()) {
            row.next();
            balance = row.getInt(1);
          }
        }
        try {
          int remaining = processor.debit(item.amount(), balance);
          item.settle();
          item.invite(ratings.schedule(item));
          try (var sql = db.prepareStatement("UPDATE wallets SET balance=? WHERE passenger=?")) {
            sql.setInt(1, remaining);
            sql.setString(2, item.passenger());
            sql.executeUpdate();
          }
        } catch (RideException.PaymentDeclined rejected) {
          item.decline();
        }
        try (var sql = db.prepareStatement("INSERT INTO payments VALUES(?,?,?,?,?)")) {
          sql.setString(1, item.bookingId());
          sql.setString(2, item.passenger());
          sql.setInt(3, item.amount());
          sql.setString(4, item.status());
          sql.setString(5, item.ratingInvitation());
          sql.executeUpdate();
        }
        tx.execute("COMMIT");
        return item;
      } catch (RuntimeException | SQLException error) {
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
}
