package com.ridehail.facade;

import com.ridehail.journal.*;
import com.ridehail.payment.PaymentService;
import com.ridehail.pricing.FareService;
import com.ridehail.reservation.ReservationService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Narrow administrative API; clients receive plain data, never subsystem services. */
public final class RideAdminFacade {
  private final ReservationService reservations;
  private final FareService fares;
  private final PaymentService payments;
  private final ChainJournal journal;
  private final RideBookingFacade booking;

  public RideAdminFacade(
      ReservationService reservations,
      FareService fares,
      PaymentService payments,
      ChainJournal journal,
      RideBookingFacade booking) {
    this.reservations = reservations;
    this.fares = fares;
    this.payments = payments;
    this.journal = journal;
    this.booking = booking;
  }

  public Map<String, Object> inspect(String id) {
    return journal.exclusive(
        () -> {
          Map<String, Object> result = new LinkedHashMap<>();
          result.put("chain", journal.get(id).snapshot());
          var a = reservations.inspect(id);
          var b = fares.inspect(id);
          var c = payments.inspect(id);
          result.put("A", a == null ? null : a.snapshot());
          result.put("B", b == null ? null : b.snapshot());
          result.put("C", c == null ? null : c.snapshot());
          return Collections.unmodifiableMap(result);
        });
  }

  public List<Map<String, Object>> inspectAll() {
    return journal.ids().stream().map(this::inspect).toList();
  }

  public Integer walletBalance(String passenger) {
    return payments.balance(passenger);
  }

  public ChainState compensate(String id) {
    return booking.cancelPending(id);
  }

  public List<ChainState> recoverAll() {
    List<ChainState> result = new ArrayList<>();
    for (String id : journal.ids()) if (!journal.get(id).terminal()) result.add(booking.resume(id));
    return List.copyOf(result);
  }
}
