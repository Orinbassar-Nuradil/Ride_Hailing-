package com.ridehail.reservation;

import com.ridehail.shared.RideException;
import java.util.Map;
import java.util.Set;

/** A owns and enforces NEW -> RESERVED -> RELEASED without any facade. */
public final class DriverReservation {
  private final String bookingId, driver, pickup, destination;
  private final int distanceKm;
  private String status = "NEW";

  public DriverReservation(
      String bookingId, String driver, String pickup, String destination, int distanceKm) {
    this.bookingId = bookingId;
    this.driver = driver;
    this.pickup = pickup;
    this.destination = destination;
    this.distanceKm = distanceKm;
  }

  public static DriverReservation restore(
      String id, String driver, String pickup, String destination, int km, String status) {
    if (!Set.of("NEW", "RESERVED", "RELEASED").contains(status)) {
      throw new RideException.InvalidRequest("Unknown reservation status: " + status);
    }
    var item = new DriverReservation(id, driver, pickup, destination, km);
    item.status = status;
    return item;
  }

  public void reserve() {
    if (!status.equals("NEW") && !status.equals("RESERVED"))
      throw new RideException.IllegalTransition("Released reservations cannot be reserved again");
    status = "RESERVED";
  }

  public void release() {
    if (!status.equals("RESERVED") && !status.equals("RELEASED"))
      throw new RideException.IllegalTransition("Only a reserved driver can be released");
    status = "RELEASED";
  }

  public String bookingId() {
    return bookingId;
  }

  public String driver() {
    return driver;
  }

  public String pickup() {
    return pickup;
  }

  public String destination() {
    return destination;
  }

  public int distanceKm() {
    return distanceKm;
  }

  public String status() {
    return status;
  }

  public Map<String, Object> snapshot() {
    return Map.of(
        "bookingId",
        bookingId,
        "driver",
        driver,
        "pickup",
        pickup,
        "destination",
        destination,
        "distanceKm",
        distanceKm,
        "status",
        status);
  }
}
