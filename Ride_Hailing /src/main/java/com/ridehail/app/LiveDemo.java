package com.ridehail.app;

import com.ridehail.facade.*;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/** Runs real client commands with business-rule failures rather than failure flags. */
public final class LiveDemo {
  private LiveDemo() {}

  public static void run(RideBookingFacade booking, RideAdminFacade admin, PrintStream out) {
    var client = new Client(booking, admin);
    String[][] commands = {
      {"book", "b-failure", "--max-fare", "1000"},
      {"book", "c-failure", "--passenger", "poor", "--funds", "100"},
      {"book", "restart", "--stop-after", "B"},
      {"inspect", "restart"},
      {"resume", "restart"},
      {"book", "busy-driver"},
      {"book", "manual", "--driver", "D2", "--stop-after", "B"},
      {"compensate", "manual"},
      {"inspect"}
    };
    int[] codes = {2, 2, 0, 0, 0, 2, 0, 0, 0};
    for (int i = 0; i < commands.length; i++) {
      out.println("$ " + String.join(" ", commands[i]));
      if (client.execute(commands[i], out) != codes[i])
        throw new IllegalStateException("Unexpected live demo exit code at step " + (i + 1));
    }
    var b = admin.inspect("b-failure");
    var c = admin.inspect("c-failure");
    require(
        ((Map<?, ?>) b.get("A")).get("status").equals("RELEASED")
            && b.get("B") == null
            && b.get("C") == null);
    require(
        ((Map<?, ?>) c.get("B")).get("status").equals("VOIDED")
            && ((Map<?, ?>) c.get("A")).get("status").equals("RELEASED"));
    require(
        ((Map<?, ?>) c.get("chain"))
            .get("events")
            .equals(List.of("A_RESERVED", "B_ISSUED", "B_VOIDED", "A_RELEASED")));
    require(admin.walletBalance("poor") == 100 && admin.walletBalance("passenger1") == 8240);
    out.println(
        "Verified nine CLI commands: both compensation depths, restart, inspection and manual"
            + " compensation.");
  }

  private static void require(boolean condition) {
    if (!condition) throw new IllegalStateException("A persisted demo invariant failed");
  }
}
