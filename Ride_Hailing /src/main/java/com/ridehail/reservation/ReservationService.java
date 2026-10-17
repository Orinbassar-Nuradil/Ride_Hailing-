package com.ridehail.reservation;

import com.ridehail.shared.Checks;
import com.ridehail.shared.RideException;

/** Public standalone API of A; does not know B, C or either facade. */
public final class ReservationService {
  private final ReservationRepository repository;
  private final GpsRouter router = new GpsRouter();
  private final DriverMatcher matcher = new DriverMatcher();

  public ReservationService(ReservationRepository repository) {
    this.repository = repository;
  }

  public DriverReservation reserve(String id, String driver, String pickup, String destination) {
    Checks.id(id);
    int km = router.distance(pickup, destination);
    var existing = repository.find(id);
    if (existing != null) {
      if (!existing.driver().equals(driver)
          || !existing.pickup().equals(pickup)
          || !existing.destination().equals(destination)) {
        throw new RideException.BookingConflict(
            "Reservation ID already has a different driver or route");
      }
      existing.reserve();
      return existing;
    }
    var item =
        new DriverReservation(
            id, matcher.choose(driver, repository.occupied()), pickup, destination, km);
    item.reserve();
    repository.save(item);
    return item;
  }

  public void release(String id) {
    var item = repository.find(id);
    if (item != null) {
      item.release();
      repository.save(item);
    }
  }

  public DriverReservation inspect(String id) {
    return repository.find(id);
  }

  public ReservationRepository repository() {
    return repository;
  }

  public GpsRouter router() {
    return router;
  }

  public DriverMatcher matcher() {
    return matcher;
  }
}
