package com.ridehail.app;

import com.ridehail.shared.RideException;
import com.ridehail.shared.StorageException;
import java.io.*;
import java.nio.file.Path;
import java.util.*;

/** IntelliJ entry point, executable JAR entry point and testable CLI entry point. */
public final class Main {
  private Main() {}

  public static void main(String[] args) {
    int code = run(args, System.in, System.out, System.err);
    if (code != 0) System.exit(code);
  }

  public static int run(String[] arguments, InputStream input, PrintStream out, PrintStream err) {
    try {
      List<String> args = new ArrayList<>(List.of(arguments));
      Path directory = Path.of("data");
      if (!args.isEmpty() && args.get(0).equals("--data")) {
        if (args.size() < 2) throw new RideException.InvalidRequest("--data requires a directory");
        directory = Path.of(args.remove(1));
        args.remove(0);
      }
      if (!args.isEmpty() && Set.of("help", "--help", "-h").contains(args.get(0))) {
        help(out);
        return 0;
      }
      if (!args.isEmpty() && args.get(0).equals("reset")) {
        if (args.size() != 1)
          throw new RideException.InvalidRequest("reset takes no extra arguments");
        out.println(StoreReset.archive(directory));
        return 0;
      }
      if (!args.isEmpty() && args.get(0).equals("demo")) {
        directory = directory.resolve("demo_" + UUID.randomUUID().toString().substring(0, 8));
        out.println("Fresh demonstration store: " + directory);
      }
      var app = AppFactory.open(directory);
      var client = new Client(app.booking(), app.admin());
      if (args.isEmpty()) {
        ConsoleMenu.run(client, input, out, err);
        return 0;
      }
      return client.execute(args.toArray(String[]::new), out);
    } catch (RideException | StorageException | IllegalArgumentException error) {
      err.println(
          "ERROR: "
              + error.getMessage()
              + ". Persisted progress is retained; inspect and retry after fixing the cause.");
      return 2;
    }
  }

  public static void help(PrintStream out) {
    out.println("Ride-Hailing Facade | Java 17+ | integer KZT");
    out.println("Usage: java -jar ride-hailing.jar [--data DIRECTORY] [COMMAND]");
    out.println("No command opens the interactive menu.");
    out.println(
        "book ID [--passenger ID] [--driver D1|D2|D3] [--pickup STOP] [--destination STOP]");
    out.println("        [--max-fare KZT] [--funds KZT] [--stop-after A|B|C]");
    out.println(
        "inspect [ID] | resume ID | compensate ID | balance PASSENGER | recover | demo | reset |"
            + " help");
    out.println("Stops: campus, station, riverside, hospital. Default route costs 1760 KZT.");
    out.println("--funds initializes a NEW wallet only. Close other processes before reset.");
  }
}
