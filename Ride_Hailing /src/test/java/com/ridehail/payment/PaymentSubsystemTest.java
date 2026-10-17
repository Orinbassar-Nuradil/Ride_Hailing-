package com.ridehail.payment;

import static org.junit.jupiter.api.Assertions.*;

import com.ridehail.shared.RideException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PaymentSubsystemTest {
  @TempDir Path directory;

  @Test
  void cWorksWithoutAFacadeOrAOrBAndDoesNotDebitTwice() {
    var repository = new PaymentRepository(directory.resolve("c.sqlite"));
    var service = new PaymentService(repository);
    assertEquals("SETTLED", service.pay("r1", "aliya", 1760, 3000).status());
    assertEquals("AFTER_RIDE:r1", service.pay("r1", "aliya", 1760, 3000).ratingInvitation());
    assertEquals(1240, repository.balance("aliya"));
    assertThrows(RideException.BookingConflict.class, () -> service.pay("r1", "aliya", 100, 3000));
    assertThrows(RideException.BookingConflict.class, () -> service.pay("r1", "other", 1760, 3000));
    assertThrows(
        RideException.PaymentDeclined.class, () -> service.pay("r2", "aliya", 1760, 999999));
    assertThrows(
        RideException.PaymentDeclined.class, () -> service.pay("r2", "aliya", 1760, 999999));
    var reopened = new PaymentService(new PaymentRepository(directory.resolve("c.sqlite")));
    assertEquals("DECLINED", reopened.inspect("r2").status());
    assertEquals("NONE", reopened.inspect("r2").ratingInvitation());
    assertEquals(1240, repository.balance("aliya"));
    assertNull(repository.balance("missing"));
    assertNull(reopened.inspect("missing"));
  }

  @Test
  void paymentRulesAndAllStateTransitionsAreIndependent() {
    assertEquals(0, new PaymentProcessor().debit(100, 100));
    assertThrows(RideException.InvalidRequest.class, () -> new PaymentProcessor().debit(0, 100));
    var item = new PaymentRecord("x", "p", 100);
    assertEquals("NEW", item.status());
    assertThrows(RideException.PaymentDeclined.class, () -> new RatingService().schedule(item));
    item.settle();
    item.settle();
    assertThrows(RideException.IllegalTransition.class, item::decline);
    var declined = new PaymentRecord("d", "p", 100);
    declined.decline();
    declined.decline();
    assertThrows(RideException.IllegalTransition.class, declined::settle);
    assertThrows(
        RideException.InvalidRequest.class,
        () -> PaymentRecord.restore("x", "p", 100, "INVALID", "NONE"));
  }
}
