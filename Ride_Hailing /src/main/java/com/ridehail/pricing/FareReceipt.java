package com.ridehail.pricing;

import com.ridehail.shared.RideException;
import java.util.Map;
import java.util.Set;

/** B owns the lifecycle NEW -> ISSUED -> VOIDED. */
public final class FareReceipt {
  private final String bookingId, receiptNumber;
  private final int distanceKm, amount;
  private String status = "NEW";

  public FareReceipt(String bookingId, int distanceKm, int amount, String receiptNumber) {
    this.bookingId = bookingId;
    this.distanceKm = distanceKm;
    this.amount = amount;
    this.receiptNumber = receiptNumber;
  }

  public static FareReceipt restore(String id, int km, int amount, String number, String status) {
    if (!Set.of("NEW", "ISSUED", "VOIDED").contains(status))
      throw new RideException.InvalidRequest("Unknown receipt status: " + status);
    var item = new FareReceipt(id, km, amount, number);
    item.status = status;
    return item;
  }

  public void issue() {
    if (!status.equals("NEW") && !status.equals("ISSUED"))
      throw new RideException.IllegalTransition("Voided receipts cannot be issued again");
    status = "ISSUED";
  }

  public void voidReceipt() {
    if (!status.equals("ISSUED") && !status.equals("VOIDED"))
      throw new RideException.IllegalTransition("Only issued receipts can be voided");
    status = "VOIDED";
  }

  public String bookingId() {
    return bookingId;
  }

  public int distanceKm() {
    return distanceKm;
  }

  public int amount() {
    return amount;
  }

  public String receiptNumber() {
    return receiptNumber;
  }

  public String status() {
    return status;
  }

  public Map<String, Object> snapshot() {
    return Map.of(
        "bookingId",
        bookingId,
        "distanceKm",
        distanceKm,
        "amount",
        amount,
        "receiptNumber",
        receiptNumber,
        "status",
        status);
  }
}
