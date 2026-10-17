package com.ridehail.app;

import static org.junit.jupiter.api.Assertions.*;

import com.ridehail.facade.RideBookingFacade;
import com.ridehail.journal.*;
import com.ridehail.payment.*;
import com.ridehail.pricing.*;
import com.ridehail.reservation.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SeparateJvmRestartTest {
  @TempDir Path directory;

  private ProcessBuilder command(String main, String... arguments) {
    var args =
        new ArrayList<>(
            List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp",
                System.getProperty("java.class.path"),
                main));
    args.addAll(List.of(arguments));
    return new ProcessBuilder(args).redirectErrorStream(true);
  }

  @Test
  void realJvmCanStopAfterBAndAnotherJvmResumesIt() throws Exception {
    var first =
        command(
                Main.class.getName(),
                "--data",
                directory.toString(),
                "book",
                "restart",
                "--stop-after",
                "B")
            .start();
    String before =
        new String(first.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    assertEquals(0, first.waitFor(), before);
    assertTrue(before.contains("PENDING_B"));
    var second =
        command(Main.class.getName(), "--data", directory.toString(), "resume", "restart").start();
    String after =
        new String(second.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    assertEquals(0, second.waitFor(), after);
    assertTrue(after.contains("COMPLETED"));
    assertEquals(8240, AppFactory.open(directory).admin().walletBalance("passenger1"));
  }

  @Test
  void hardJvmHaltAfterBCommitReleasesLockAndRecoversBothCompensations() throws Exception {
    var first = command(CrashAfterB.class.getName(), directory.toString()).start();
    String output =
        new String(first.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    assertEquals(73, first.waitFor(), output);
    var journal =
        new ChainJournal(
            directory.resolve("chains.sqlite"), directory.resolve("workflow_lock.sqlite"));
    assertEquals("PENDING_B", journal.get("hard-exit").status());
    assertEquals(
        "ISSUED", new FareRepository(directory.resolve("fares.sqlite")).find("hard-exit").status());
    assertEquals("COMPENSATED", AppFactory.open(directory).booking().resume("hard-exit").status());
    assertEquals(
        "VOIDED", new FareRepository(directory.resolve("fares.sqlite")).find("hard-exit").status());
    assertEquals(
        "RELEASED",
        new ReservationRepository(directory.resolve("reservations.sqlite"))
            .find("hard-exit")
            .status());
  }

  public static class CrashAfterB {
    public static void main(String[] args) {
      Path root = Path.of(args[0]);
      var journal =
          new ChainJournal(root.resolve("chains.sqlite"), root.resolve("workflow_lock.sqlite")) {
            @Override
            public void update(String id, String status, String event, String reason) {
              if ("B_ISSUED".equals(event)) Runtime.getRuntime().halt(73);
              super.update(id, status, event, reason);
            }
          };
      new RideBookingFacade(
              new ReservationService(
                  new ReservationRepository(root.resolve("reservations.sqlite"))),
              new FareService(new FareRepository(root.resolve("fares.sqlite"))),
              new PaymentService(new PaymentRepository(root.resolve("payments.sqlite"))),
              journal)
          .book(new BookingRequest("hard-exit", "poor", "D1", "campus", "station", 5000, 100));
    }
  }
}
