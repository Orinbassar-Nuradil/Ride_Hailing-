package com.ridehail.app;

import com.ridehail.facade.*;
import com.ridehail.journal.BookingRequest;
import com.ridehail.shared.RideException;
import java.io.PrintStream;
import java.util.*;

/** A real application client with exactly two immediate service dependencies. */
public final class Client {
  private final RideBookingFacade booking;
  private final RideAdminFacade admin;

  public Client(RideBookingFacade booking, RideAdminFacade admin) {
    this.booking = booking;
    this.admin = admin;
  }

  public int execute(String[] args, PrintStream out) {
    String command = args[0];
    switch (command) {
      case "book" -> {
        if (args.length < 2) throw new RideException.InvalidRequest("book requires a booking ID");
        var options = options(args);
        var request =
            new BookingRequest(
                args[1],
                options.getOrDefault("--passenger", "passenger1"),
                options.getOrDefault("--driver", "D1"),
                options.getOrDefault("--pickup", "campus"),
                options.getOrDefault("--destination", "station"),
                Integer.parseInt(options.getOrDefault("--max-fare", "5000")),
                Integer.parseInt(options.getOrDefault("--funds", "10000")));
        var result =
            booking.book(request, Checkpoint.valueOf(options.getOrDefault("--stop-after", "NONE")));
        out.println(JsonOutput.write(admin.inspect(args[1])));
        return rejected(result.status()) ? 2 : 0;
      }
      case "inspect" -> {
        if (args.length > 2)
          throw new RideException.InvalidRequest("inspect accepts at most one ID");
        out.println(
            JsonOutput.write(args.length == 2 ? admin.inspect(args[1]) : admin.inspectAll()));
      }
      case "resume" -> {
        requireId(args);
        var result = booking.resume(args[1]);
        out.println(JsonOutput.write(result.snapshot()));
        return rejected(result.status()) ? 2 : 0;
      }
      case "compensate" -> {
        requireId(args);
        out.println(JsonOutput.write(admin.compensate(args[1]).snapshot()));
      }
      case "balance" -> {
        requireId(args);
        out.println(JsonOutput.write(admin.walletBalance(args[1])));
      }
      case "recover" -> {
        requireCount(args, 1);
        out.println(
            JsonOutput.write(admin.recoverAll().stream().map(state -> state.snapshot()).toList()));
      }
      case "demo" -> {
        requireCount(args, 1);
        LiveDemo.run(booking, admin, out);
      }
      default -> throw new RideException.InvalidRequest("Unknown command: " + command);
    }
    return 0;
  }

  private static boolean rejected(String status) {
    return status.equals("FAILED") || status.equals("COMPENSATED");
  }

  private static void requireId(String[] args) {
    requireCount(args, 2);
  }

  private static void requireCount(String[] args, int count) {
    if (args.length != count)
      throw new RideException.InvalidRequest("Wrong number of arguments for " + args[0]);
  }

  private static Map<String, String> options(String[] args) {
    Set<String> allowed =
        Set.of(
            "--passenger",
            "--driver",
            "--pickup",
            "--destination",
            "--max-fare",
            "--funds",
            "--stop-after");
    Map<String, String> result = new HashMap<>();
    for (int i = 2; i < args.length; i += 2) {
      if (!allowed.contains(args[i]) || i + 1 >= args.length)
        throw new RideException.InvalidRequest("Unknown or incomplete option: " + args[i]);
      if (result.put(args[i], args[i + 1]) != null)
        throw new RideException.InvalidRequest("Duplicate option: " + args[i]);
    }
    return result;
  }
}
