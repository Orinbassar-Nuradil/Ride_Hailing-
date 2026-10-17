package com.ridehail.reservation;

import static org.junit.jupiter.api.Assertions.*;

import com.ridehail.shared.RideException;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReservationSubsystemTest {
  @TempDir Path directory;

  @Test
  void aWorksWithoutAFacadeOrAnyOtherSubsystem() {
    var service = new ReservationService(new ReservationRepository(directory.resolve("a.sqlite")));
    var reservation = service.reserve("r1", "D1", "campus", "station");
    assertEquals(7, reservation.distanceKm());
    assertEquals("RESERVED", reservation.status());
    assertEquals("RESERVED", service.reserve("r1", "D1", "campus", "station").status());
    assertThrows(
        RideException.BookingConflict.class,
        () -> service.reserve("r1", "D2", "campus", "station"));
    assertThrows(
        RideException.DriverUnavailable.class,
        () -> service.reserve("r2", "D1", "campus", "hospital"));
    service.release("r1");
    service.release("r1");
    service.release("missing");
    var reopened = new ReservationService(new ReservationRepository(directory.resolve("a.sqlite")));
    assertEquals("RELEASED", reopened.inspect("r1").status());
    assertEquals("RESERVED", reopened.reserve("r2", "D1", "campus", "hospital").status());
    assertThrows(
        RideException.IllegalTransition.class,
        () -> reopened.reserve("r1", "D1", "campus", "station"));
  }

  @Test
  void entityEnforcesThreeMeaningfulStates() {
    var entity = new DriverReservation("x", "D1", "campus", "station", 7);
    assertEquals("NEW", entity.status());
    assertThrows(RideException.IllegalTransition.class, entity::release);
    entity.reserve();
    entity.reserve();
    entity.release();
    entity.release();
    assertThrows(RideException.IllegalTransition.class, entity::reserve);
    assertThrows(
        RideException.InvalidRequest.class,
        () -> DriverReservation.restore("x", "D1", "campus", "station", 7, "BOGUS"));
  }

  @Test
  void pureRulesRejectUnknownOrUnavailableResources() {
    var router = new GpsRouter();
    assertEquals(6, router.distance("campus", "riverside"));
    assertThrows(RideException.InvalidRequest.class, () -> router.distance("campus", "campus"));
    assertThrows(RideException.InvalidRequest.class, () -> router.distance("missing", "station"));
    assertThrows(RideException.InvalidRequest.class, () -> router.distance("campus", "missing"));
    var matcher = new DriverMatcher();
    assertThrows(RideException.InvalidRequest.class, () -> matcher.choose("D9", Set.of()));
    assertThrows(RideException.DriverUnavailable.class, () -> matcher.choose("D1", Set.of("D1")));
  }

  @Test
  void databaseItselfPreventsDoubleReservation() {
    var repository = new ReservationRepository(directory.resolve("a.sqlite"));
    var one = new DriverReservation("one", "D1", "campus", "station", 7);
    one.reserve();
    repository.save(one);
    var two = new DriverReservation("two", "D1", "campus", "station", 7);
    two.reserve();
    assertThrows(RideException.DriverUnavailable.class, () -> repository.save(two));
    assertNull(repository.find("two"));
  }
}
