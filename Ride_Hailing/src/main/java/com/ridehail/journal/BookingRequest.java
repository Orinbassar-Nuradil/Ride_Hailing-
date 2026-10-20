package com.ridehail.journal;

import com.ridehail.shared.Checks;
import com.ridehail.shared.RideException;
import java.util.Map;

/** Stable operation input; checkpoints are deliberately not part of request identity. */
public record BookingRequest(
    String bookingId,
    String passenger,
    String driver,
    String pickup,
    String destination,
    int maxFare,
    int openingBalance) {
  public BookingRequest {
    Checks.id(bookingId);
    Checks.id(passenger);
    Checks.amount(maxFare, 0);
    Checks.amount(openingBalance, 0);
    if (driver == null || pickup == null || destination == null)
      throw new RideException.InvalidRequest("Driver and both stops are required");
  }

  public Map<String, Object> snapshot() {
    return Map.of(
        "bookingId",
        bookingId,
        "passenger",
        passenger,
        "driver",
        driver,
        "pickup",
        pickup,
        "destination",
        destination,
        "maxFare",
        maxFare,
        "openingBalance",
        openingBalance);
  }
}
