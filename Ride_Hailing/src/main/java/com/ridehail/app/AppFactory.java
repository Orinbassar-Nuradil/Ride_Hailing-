package com.ridehail.app;

import com.ridehail.facade.*;
import com.ridehail.journal.ChainJournal;
import com.ridehail.payment.*;
import com.ridehail.pricing.*;
import com.ridehail.reservation.*;
import java.nio.file.Path;

/** Composition root: constructs dependencies, with no booking decisions or SQL. */
public final class AppFactory {
  private AppFactory() {}

  public record Application(RideBookingFacade booking, RideAdminFacade admin) {}

  public static Application open(Path directory) {
    var a =
        new ReservationService(new ReservationRepository(directory.resolve("reservations.sqlite")));
    var b = new FareService(new FareRepository(directory.resolve("fares.sqlite")));
    var c = new PaymentService(new PaymentRepository(directory.resolve("payments.sqlite")));
    var journal =
        new ChainJournal(
            directory.resolve("chains.sqlite"), directory.resolve("workflow_lock.sqlite"));
    var booking = new RideBookingFacade(a, b, c, journal);
    return new Application(booking, new RideAdminFacade(a, b, c, journal, booking));
  }
}
