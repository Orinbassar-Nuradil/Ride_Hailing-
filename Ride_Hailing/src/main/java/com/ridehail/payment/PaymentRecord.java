package com.ridehail.payment;

import com.ridehail.shared.RideException;
import java.util.Map;
import java.util.Set;

/** C enforces NEW -> SETTLED or NEW -> DECLINED, independently of a facade. */
public final class PaymentRecord {
  private final String bookingId, passenger;
  private final int amount;
  private String status = "NEW", ratingInvitation = "NONE";

  public PaymentRecord(String bookingId, String passenger, int amount) {
    this.bookingId = bookingId;
    this.passenger = passenger;
    this.amount = amount;
  }

  public static PaymentRecord restore(
      String id, String passenger, int amount, String status, String invitation) {
    if (!Set.of("NEW", "SETTLED", "DECLINED").contains(status))
      throw new RideException.InvalidRequest("Unknown payment status: " + status);
    var item = new PaymentRecord(id, passenger, amount);
    item.status = status;
    item.ratingInvitation = invitation;
    return item;
  }

  public void settle() {
    if (!status.equals("NEW") && !status.equals("SETTLED"))
      throw new RideException.IllegalTransition("Declined payments cannot be settled");
    status = "SETTLED";
  }

  public void decline() {
    if (!status.equals("NEW") && !status.equals("DECLINED"))
      throw new RideException.IllegalTransition("Settled payments cannot be declined");
    status = "DECLINED";
  }

  public void invite(String invitation) {
    ratingInvitation = invitation;
  }

  public String bookingId() {
    return bookingId;
  }

  public String passenger() {
    return passenger;
  }

  public int amount() {
    return amount;
  }

  public String status() {
    return status;
  }

  public String ratingInvitation() {
    return ratingInvitation;
  }

  public Map<String, Object> snapshot() {
    return Map.of(
        "bookingId",
        bookingId,
        "passenger",
        passenger,
        "amount",
        amount,
        "status",
        status,
        "ratingInvitation",
        ratingInvitation);
  }
}
