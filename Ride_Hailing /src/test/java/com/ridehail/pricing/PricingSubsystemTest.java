package com.ridehail.pricing;

import static org.junit.jupiter.api.Assertions.*;

import com.ridehail.shared.RideException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PricingSubsystemTest {
  @TempDir Path directory;

  @Test
  void bWorksWithoutAFacadeOrAOrC() {
    var service = new FareService(new FareRepository(directory.resolve("b.sqlite")));
    assertEquals(1760, service.issue("r1", 7, 5000).amount());
    assertEquals("RH-r1", service.issue("r1", 7, 5000).receiptNumber());
    assertThrows(RideException.BookingConflict.class, () -> service.issue("r1", 8, 5000));
    service.voidReceipt("r1");
    service.voidReceipt("r1");
    service.voidReceipt("missing");
    var reopened = new FareService(new FareRepository(directory.resolve("b.sqlite")));
    assertEquals("VOIDED", reopened.inspect("r1").status());
    assertThrows(RideException.IllegalTransition.class, () -> reopened.issue("r1", 7, 5000));
    assertNull(reopened.inspect("missing"));
  }

  @Test
  void fareLimitsAndEntityTransitionsAreEnforced() {
    var pricing = new PricingEngine();
    assertEquals(1760, pricing.quote(7, 1760));
    assertThrows(RideException.FareRejected.class, () -> pricing.quote(7, 1759));
    assertThrows(RideException.InvalidRequest.class, () -> pricing.quote(0, 5000));
    assertThrows(RideException.InvalidRequest.class, () -> pricing.quote(7, -1));
    var item = new ReceiptGenerator().create("x", 7, 1760);
    assertEquals("NEW", item.status());
    assertThrows(RideException.IllegalTransition.class, item::voidReceipt);
    item.issue();
    item.issue();
    item.voidReceipt();
    item.voidReceipt();
    assertThrows(RideException.IllegalTransition.class, item::issue);
    assertThrows(
        RideException.InvalidRequest.class,
        () -> FareReceipt.restore("x", 7, 1760, "RH-x", "INVALID"));
  }
}
