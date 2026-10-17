package com.ridehail.facade;

import static org.junit.jupiter.api.Assertions.*;

import com.ridehail.app.AppFactory;
import com.ridehail.journal.BookingRequest;
import com.ridehail.journal.ChainState;
import java.nio.file.Path;
import java.util.Map;

/** A small domain-specific testing language. Every assertion reopens the stores. */
final class RideScenario {
  final Path directory;
  final RideBookingFacade booking;
  final RideAdminFacade admin;

  RideScenario(Path directory) {
    this.directory = directory;
    var app = AppFactory.open(directory);
    booking = app.booking();
    admin = app.admin();
  }

  static BookingRequest request(String id, int limit, int funds) {
    return new BookingRequest(id, "aliya", "D1", "campus", "station", limit, funds);
  }

  ChainState passengerRequestsRide(int limit, int funds) {
    return booking.book(request("ride1", limit, funds));
  }

  Map<String, Object> persistedBookingShouldBe(String chain, String a, String b, String c) {
    var snapshot = AppFactory.open(directory).admin().inspect("ride1");
    assertEquals(chain, ((Map<?, ?>) snapshot.get("chain")).get("status"));
    for (var entry :
        Map.of(
                "A",
                a == null ? "ABSENT" : a,
                "B",
                b == null ? "ABSENT" : b,
                "C",
                c == null ? "ABSENT" : c)
            .entrySet()) {
      var state = (Map<?, ?>) snapshot.get(entry.getKey());
      assertEquals(entry.getValue(), state == null ? "ABSENT" : state.get("status"));
    }
    return snapshot;
  }
}
