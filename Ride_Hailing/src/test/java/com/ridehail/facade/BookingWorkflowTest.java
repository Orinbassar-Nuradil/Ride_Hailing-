package com.ridehail.facade;

import static org.junit.jupiter.api.Assertions.*;

import com.ridehail.app.AppFactory;
import com.ridehail.journal.*;
import com.ridehail.payment.*;
import com.ridehail.pricing.*;
import com.ridehail.reservation.*;
import com.ridehail.shared.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BookingWorkflowTest {
  @TempDir Path directory;
  RideScenario ride;

  @BeforeEach
  void freshPersistedState() {
    ride = new RideScenario(directory);
  }

  @Test
  void happyPathPersistsAllThreeStages() {
    assertEquals("COMPLETED", ride.passengerRequestsRide(5000, 10000).status());
    var state = ride.persistedBookingShouldBe("COMPLETED", "RESERVED", "ISSUED", "SETTLED");
    assertEquals(1760, ((Map<?, ?>) state.get("B")).get("amount"));
    assertEquals("AFTER_RIDE:ride1", ((Map<?, ?>) state.get("C")).get("ratingInvitation"));
    assertEquals(8240, ride.admin.walletBalance("aliya"));
  }

  @Test
  void bFailureCompensatesOnlyPersistedA() {
    ride.passengerRequestsRide(1000, 10000);
    var state = ride.persistedBookingShouldBe("COMPENSATED", "RELEASED", null, null);
    assertEquals(
        List.of("A_RESERVED", "A_RELEASED"), ((Map<?, ?>) state.get("chain")).get("events"));
    assertNull(ride.admin.walletBalance("aliya"));
  }

  @Test
  void cFailureCompensatesBThenAAndPreservesTheWallet() {
    ride.passengerRequestsRide(5000, 100);
    var state = ride.persistedBookingShouldBe("COMPENSATED", "RELEASED", "VOIDED", "DECLINED");
    assertEquals(
        List.of("A_RESERVED", "B_ISSUED", "B_VOIDED", "A_RELEASED"),
        ((Map<?, ?>) state.get("chain")).get("events"));
    assertEquals(100, ride.admin.walletBalance("aliya"));
    assertEquals("NONE", ((Map<?, ?>) state.get("C")).get("ratingInvitation"));
  }

  @Test
  void invalidRouteFailsBeforeAnySubsystemCommit() {
    ride.booking.book(
        new BookingRequest("ride1", "aliya", "D1", "unknown", "station", 5000, 10000));
    ride.persistedBookingShouldBe("FAILED", null, null, null);
  }

  @Test
  void occupiedDriverIsAnAdditionalRealisticFailure() {
    ride.passengerRequestsRide(5000, 10000);
    assertEquals("FAILED", ride.booking.book(RideScenario.request("second", 5000, 10000)).status());
    ride.persistedBookingShouldBe("COMPLETED", "RESERVED", "ISSUED", "SETTLED");
  }

  @Test
  void repeatedRequestDoesNotChargeAgainOrDuplicateEvents() {
    ride.passengerRequestsRide(5000, 10000);
    ride.passengerRequestsRide(5000, 10000);
    ride.booking.resume("ride1");
    assertEquals(8240, ride.admin.walletBalance("aliya"));
    assertEquals(1, ride.admin.inspectAll().size());
    assertEquals(3, ride.booking.resume("ride1").events().size());
  }

  @Test
  void sameIdWithChangedRequestIsRejected() {
    ride.passengerRequestsRide(5000, 10000);
    assertThrows(
        RideException.BookingConflict.class,
        () ->
            ride.booking.book(
                new BookingRequest("ride1", "aliya", "D2", "campus", "station", 5000, 10000)));
  }

  @Test
  void reloadAfterAUsesPersistedStateDespiteStaleJournal() {
    ride.booking.book(RideScenario.request("ride1", 5000, 10000), Checkpoint.A);
    ride.persistedBookingShouldBe("PENDING_A", "RESERVED", null, null);
    AppFactory.open(directory).booking().resume("ride1");
    ride.persistedBookingShouldBe("COMPLETED", "RESERVED", "ISSUED", "SETTLED");
  }

  @Test
  void reloadAfterBThenCDeclinesAndBothEarlierStepsAreUndone() {
    ride.booking.book(RideScenario.request("ride1", 5000, 100), Checkpoint.B);
    ride.persistedBookingShouldBe("PENDING_B", "RESERVED", "ISSUED", null);
    AppFactory.open(directory).booking().resume("ride1");
    ride.persistedBookingShouldBe("COMPENSATED", "RELEASED", "VOIDED", "DECLINED");
  }

  @Test
  void reloadAfterCDoesNotDebitTwice() {
    ride.booking.book(RideScenario.request("ride1", 5000, 10000), Checkpoint.C);
    ride.persistedBookingShouldBe("PENDING_C", "RESERVED", "ISSUED", "SETTLED");
    AppFactory.open(directory).booking().resume("ride1");
    ride.persistedBookingShouldBe("COMPLETED", "RESERVED", "ISSUED", "SETTLED");
    assertEquals(8240, ride.admin.walletBalance("aliya"));
  }

  @Test
  void manualCompensationIsIdempotent() {
    ride.booking.book(RideScenario.request("ride1", 5000, 10000), Checkpoint.B);
    ride.admin.compensate("ride1");
    ride.admin.compensate("ride1");
    ride.persistedBookingShouldBe("COMPENSATED", "RELEASED", "VOIDED", null);
    assertEquals("COMPENSATED", ride.booking.resume("ride1").status());
  }

  @Test
  void settledPaymentIsNotUndoneEvenWithAStaleJournal() {
    ride.booking.book(RideScenario.request("ride1", 5000, 10000), Checkpoint.C);
    assertThrows(RideException.BookingConflict.class, () -> ride.admin.compensate("ride1"));
    ride.persistedBookingShouldBe("COMPLETED", "RESERVED", "ISSUED", "SETTLED");
  }

  @Test
  void compensationCanRestartAfterBUndoAndActuallyVoidsBBeforeReleasingA() {
    var journal =
        new ChainJournal(
            directory.resolve("chains.sqlite"), directory.resolve("workflow_lock.sqlite")) {
          @Override
          public void update(String id, String status, String event, String reason) {
            if ("B_VOIDED".equals(event)) {
              assertEquals(
                  "RESERVED",
                  new ReservationRepository(directory.resolve("reservations.sqlite"))
                      .find(id)
                      .status());
              assertEquals(
                  "VOIDED",
                  new FareRepository(directory.resolve("fares.sqlite")).find(id).status());
              throw new SimulatedPowerLoss();
            }
            super.update(id, status, event, reason);
          }
        };
    var booking = bookingUsing(journal);
    assertThrows(
        SimulatedPowerLoss.class, () -> booking.book(RideScenario.request("ride1", 5000, 100)));
    ride.persistedBookingShouldBe("COMPENSATING", "RESERVED", "VOIDED", "DECLINED");
    assertEquals(1, AppFactory.open(directory).admin().recoverAll().size());
    var state = ride.persistedBookingShouldBe("COMPENSATED", "RELEASED", "VOIDED", "DECLINED");
    assertEquals(
        List.of("A_RESERVED", "B_ISSUED", "B_VOIDED", "A_RELEASED"),
        ((Map<?, ?>) state.get("chain")).get("events"));
    assertEquals(List.of(), ride.admin.recoverAll());
  }

  @Test
  void compensationCanRestartAfterAUndo() {
    ride.booking.book(RideScenario.request("ride1", 5000, 10000), Checkpoint.A);
    var journal =
        new ChainJournal(
            directory.resolve("chains.sqlite"), directory.resolve("workflow_lock.sqlite")) {
          @Override
          public void update(String id, String status, String event, String reason) {
            if ("A_RELEASED".equals(event)) throw new SimulatedPowerLoss();
            super.update(id, status, event, reason);
          }
        };
    assertThrows(SimulatedPowerLoss.class, () -> bookingUsing(journal).cancelPending("ride1"));
    ride.persistedBookingShouldBe("COMPENSATING", "RELEASED", null, null);
    ride.booking.resume("ride1");
    ride.persistedBookingShouldBe("COMPENSATED", "RELEASED", null, null);
  }

  @Test
  void unknownIdsAndMalformedRequestsUseDomainExceptions() {
    assertThrows(RideException.UnknownBooking.class, () -> ride.booking.resume("missing"));
    assertThrows(
        RideException.InvalidRequest.class, () -> RideScenario.request("bad id", 5000, 10000));
    assertThrows(RideException.InvalidRequest.class, () -> RideScenario.request("x", -1, 10000));
    assertThrows(
        RideException.InvalidRequest.class, () -> RideScenario.request("x", 5000, 1000001));
    assertThrows(
        RideException.InvalidRequest.class,
        () -> new BookingRequest("x", "p", null, "campus", "station", 5000, 10000));
    assertThrows(
        RideException.InvalidRequest.class,
        () -> ride.booking.book(RideScenario.request("x", 5000, 10000), null));
    assertEquals(List.of(), ride.admin.inspectAll());
  }

  @Test
  void compensatedResourceCanBeUsedByANewBookingId() {
    ride.passengerRequestsRide(1000, 10000);
    assertEquals(
        "COMPLETED", ride.booking.book(RideScenario.request("second", 5000, 10000)).status());
  }

  @Test
  void storageErrorLeavesRetryableProgressInsteadOfClaimingCompensation() throws Exception {
    ride.booking.book(RideScenario.request("ride1", 5000, 10000), Checkpoint.A);
    Files.move(directory.resolve("fares.sqlite"), directory.resolve("saved-fares.sqlite"));
    Files.createDirectory(directory.resolve("fares.sqlite"));
    assertThrows(StorageException.class, () -> ride.booking.resume("ride1"));
    Files.delete(directory.resolve("fares.sqlite"));
    Files.move(directory.resolve("saved-fares.sqlite"), directory.resolve("fares.sqlite"));
    ride.booking.resume("ride1");
    ride.persistedBookingShouldBe("COMPLETED", "RESERVED", "ISSUED", "SETTLED");
  }

  private RideBookingFacade bookingUsing(ChainJournal journal) {
    return new RideBookingFacade(
        new ReservationService(new ReservationRepository(directory.resolve("reservations.sqlite"))),
        new FareService(new FareRepository(directory.resolve("fares.sqlite"))),
        new PaymentService(new PaymentRepository(directory.resolve("payments.sqlite"))),
        journal);
  }

  static final class SimulatedPowerLoss extends Error {}
}
