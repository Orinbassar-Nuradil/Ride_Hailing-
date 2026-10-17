package com.ridehail.app;

import com.ridehail.shared.RideException;
import com.ridehail.shared.StorageException;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Interactive front end for a normal IntelliJ Run action with no arguments. */
public final class ConsoleMenu {
  private ConsoleMenu() {}

  public static void run(Client client, InputStream input, PrintStream out, PrintStream err) {
    var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
    try {
      while (true) {
        out.println(
            "\n"
                + "Ride-Hailing | 1 Book | 2 Inspect | 3 Resume | 4 Compensate | 5 Recover all | 6"
                + " Wallet | 0 Exit");
        String choice = prompt(reader, out, "Choice", "0");
        try {
          switch (choice) {
            case "0" -> {
              return;
            }
            case "1" ->
                client.execute(
                    new String[] {
                      "book",
                      prompt(reader, out, "Booking ID", "ride1"),
                      "--passenger",
                      prompt(reader, out, "Passenger", "passenger1"),
                      "--driver",
                      prompt(reader, out, "Driver D1/D2/D3", "D1"),
                      "--pickup",
                      prompt(reader, out, "Pickup campus/station/riverside/hospital", "campus"),
                      "--destination",
                      prompt(reader, out, "Destination", "station"),
                      "--max-fare",
                      prompt(reader, out, "Maximum fare in KZT", "5000"),
                      "--funds",
                      prompt(reader, out, "Opening balance for a NEW passenger", "10000")
                    },
                    out);
            case "2" -> {
              String id = prompt(reader, out, "Booking ID (all for every booking)", "all");
              client.execute(
                  id.equals("all") ? new String[] {"inspect"} : new String[] {"inspect", id}, out);
            }
            case "3" ->
                client.execute(
                    new String[] {"resume", prompt(reader, out, "Booking ID", "ride1")}, out);
            case "4" ->
                client.execute(
                    new String[] {"compensate", prompt(reader, out, "Booking ID", "ride1")}, out);
            case "5" -> client.execute(new String[] {"recover"}, out);
            case "6" ->
                client.execute(
                    new String[] {"balance", prompt(reader, out, "Passenger", "passenger1")}, out);
            default -> out.println("Unknown menu option. Choose 0-6.");
          }
        } catch (RideException | StorageException | IllegalArgumentException error) {
          err.println("ERROR: " + error.getMessage());
        }
      }
    } catch (EOFException end) {
      /* End of input exits the menu cleanly. */
    } catch (IOException error) {
      throw new StorageException("Cannot read console input", error);
    }
  }

  private static String prompt(
      BufferedReader reader, PrintStream out, String label, String fallback) throws IOException {
    out.print(label + " [" + fallback + "]: ");
    out.flush();
    String value = reader.readLine();
    if (value == null) throw new EOFException();
    return value.isBlank() ? fallback : value.trim();
  }
}
