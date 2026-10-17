package com.ridehail.facade;

import com.ridehail.journal.*;
import com.ridehail.payment.PaymentService;
import com.ridehail.pricing.FareService;
import com.ridehail.reservation.ReservationService;
import com.ridehail.shared.RideException;

/** The common client workflow: coordinate A -> B -> C and durable compensation. */
public final class RideBookingFacade {
  private final ReservationService reservations;
  private final FareService fares;
  private final PaymentService payments;
  private final ChainJournal journal;

  public RideBookingFacade(
      ReservationService reservations,
      FareService fares,
      PaymentService payments,
      ChainJournal journal) {
    this.reservations = reservations;
    this.fares = fares;
    this.payments = payments;
    this.journal = journal;
  }

  public ChainState book(BookingRequest request) {
    return book(request, Checkpoint.NONE);
  }

  public ChainState book(BookingRequest request, Checkpoint stopAfter) {
    if (stopAfter == null)
      throw new RideException.InvalidRequest("Checkpoint must be NONE, A, B or C");
    return journal.exclusive(
        () -> {
          journal.start(request);
          return advance(request.bookingId(), stopAfter);
        });
  }

  public ChainState resume(String id) {
    return journal.exclusive(() -> advance(id, Checkpoint.NONE));
  }

  private ChainState advance(String id, Checkpoint stopAfter) {
    var chain = journal.get(id);
    if (chain.terminal()) return chain;
    if (chain.status().equals("COMPENSATING")) return compensate(id);
    // Progress can lag a successful commit. Reconcile the actual C store first.
    var payment = payments.inspect(id);
    if (payment != null && payment.status().equals("SETTLED")) {
      journal.update(id, "COMPLETED", "C_SETTLED", null);
      return journal.get(id);
    }
    var request = chain.request();
    try {
      var reservation =
          reservations.reserve(id, request.driver(), request.pickup(), request.destination());
      if (stopAfter == Checkpoint.A) return journal.get(id);
      journal.update(id, "PENDING_B", "A_RESERVED", null);
      var receipt = fares.issue(id, reservation.distanceKm(), request.maxFare());
      if (stopAfter == Checkpoint.B) return journal.get(id);
      journal.update(id, "PENDING_C", "B_ISSUED", null);
      payments.pay(id, request.passenger(), receipt.amount(), request.openingBalance());
      if (stopAfter == Checkpoint.C) return journal.get(id);
      journal.update(id, "COMPLETED", "C_SETTLED", null);
    } catch (RideException rejected) {
      // Read persisted rows, never flags that disappear on process termination.
      if (reservations.inspect(id) != null || fares.inspect(id) != null) {
        journal.update(id, "COMPENSATING", null, rejected.getMessage());
        return compensate(id);
      }
      journal.update(id, "FAILED", null, rejected.getMessage());
    }
    return journal.get(id);
  }

  public ChainState cancelPending(String id) {
    return journal.exclusive(
        () -> {
          var chain = journal.get(id);
          var payment = payments.inspect(id);
          if (payment != null && payment.status().equals("SETTLED")) {
            journal.update(id, "COMPLETED", "C_SETTLED", null);
            throw new RideException.BookingConflict(
                "Already paid: a settled booking requires a separate refund workflow");
          }
          if (chain.terminal()) return chain;
          journal.update(id, "COMPENSATING", null, "Administrator cancelled pending booking");
          return compensate(id);
        });
  }

  private ChainState compensate(String id) {
    if (fares.inspect(id) != null) {
      fares.voidReceipt(id);
      journal.update(id, "COMPENSATING", "B_VOIDED", null);
    }
    if (reservations.inspect(id) != null) {
      reservations.release(id);
      journal.update(id, "COMPENSATING", "A_RELEASED", null);
    }
    journal.update(id, "COMPENSATED", null, null);
    return journal.get(id);
  }
}
