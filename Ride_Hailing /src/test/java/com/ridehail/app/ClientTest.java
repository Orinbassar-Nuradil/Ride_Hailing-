package com.ridehail.app;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClientTest {
  @TempDir Path directory;

  record Result(int code, String out, String err) {}

  private Result invoke(String input, String... arguments) {
    var out = new ByteArrayOutputStream();
    var err = new ByteArrayOutputStream();
    List<String> args = new ArrayList<>(List.of("--data", directory.resolve("store").toString()));
    args.addAll(List.of(arguments));
    int code =
        Main.run(
            args.toArray(String[]::new),
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
            new PrintStream(out),
            new PrintStream(err));
    return new Result(
        code, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
  }

  @Test
  void cliAllowsRealInputBothFailuresInspectionAndAdministration() {
    assertEquals(2, invoke("", "book", "b-fail", "--max-fare", "1000").code());
    assertTrue(invoke("", "inspect", "b-fail").out().contains("RELEASED"));
    assertEquals(2, invoke("", "book", "c-fail", "--passenger", "poor", "--funds", "100").code());
    assertTrue(invoke("", "inspect", "c-fail").out().contains("B_VOIDED"));
    assertEquals(0, invoke("", "book", "pending", "--stop-after", "B").code());
    assertTrue(invoke("", "inspect", "pending").out().contains("PENDING_B"));
    assertEquals(0, invoke("", "compensate", "pending").code());
    assertEquals(0, invoke("", "book", "success").code());
    assertEquals(0, invoke("", "resume", "success").code());
    assertEquals("[]", invoke("", "recover").out().trim());
    assertTrue(invoke("", "inspect").out().contains("success"));
    assertTrue(invoke("", "balance", "passenger1").out().contains("8240"));
  }

  @Test
  void invalidInputReturnsAnErrorInsteadOfAStackTrace() {
    for (String[] args :
        new String[][] {
          {"unknown"},
          {"resume", "missing"},
          {"book"},
          {"book", "x", "--funds", "bad"},
          {"book", "x", "--what", "1"},
          {"book", "x", "--funds"},
          {"book", "x", "--funds", "1", "--funds", "2"},
          {"book", "x", "--stop-after", "Z"},
          {"inspect", "x", "extra"},
          {"resume"},
          {"balance"}
        }) {
      Result result = invoke("", args);
      assertEquals(2, result.code(), Arrays.toString(args));
      assertTrue(result.err().contains("ERROR:"));
    }
    assertEquals(0, invoke("", "help").code());
    assertEquals(0, invoke("", "--help").code());
    assertEquals(
        2,
        Main.run(
            new String[] {"--data"},
            InputStream.nullInputStream(),
            System.out,
            new PrintStream(OutputStream.nullOutputStream())));
  }

  @Test
  void menuRunsFromTheSameFacadeApiAndRecoversFromBadSelections() {
    String input =
        String.join(
                "\n",
                "9",
                "1",
                "menu-ride",
                "",
                "",
                "",
                "",
                "",
                "",
                "2",
                "menu-ride",
                "3",
                "menu-ride",
                "4",
                "menu-ride",
                "5",
                "6",
                "passenger1",
                "0")
            + "\n";
    var result = invoke(input);
    assertEquals(0, result.code());
    assertTrue(result.out().contains("COMPLETED"));
    assertTrue(result.out().contains("8240"));
    assertTrue(result.err().contains("Already paid"));
    assertTrue(result.out().contains("Unknown menu option"));
    assertEquals(0, invoke("").code());
    assertEquals(0, invoke("1\n").code());
  }

  @Test
  void automaticLiveDemoChecksAllRequiredScenarios() {
    var result = invoke("", "demo");
    assertEquals(0, result.code(), result.err());
    assertTrue(result.out().contains("Verified nine CLI commands"));
    assertTrue(result.out().contains("B_VOIDED"));
  }

  @Test
  void resettingArchivesOldStateAndRecreatesACleanStore() throws Exception {
    assertEquals(0, invoke("", "reset").code());
    invoke("", "book", "old");
    assertEquals(0, invoke("", "reset").code());
    assertEquals("[]", invoke("", "inspect").out().trim());
    try (var files = Files.list(directory)) {
      assertTrue(files.anyMatch(p -> p.getFileName().toString().startsWith("store_backup_")));
    }
    Path unsafe = directory.resolve("not-a-store");
    Files.createDirectories(unsafe);
    assertThrows(
        com.ridehail.shared.RideException.InvalidRequest.class, () -> StoreReset.archive(unsafe));
  }

  @Test
  void genericJsonOutputEscapesControlCharactersAndHandlesSnapshots() {
    String json =
        JsonOutput.write(
            Map.of("x", List.of("a\"b", "line\nend", "a\\b", "\t\r\b\f\u0001", true, 4)));
    assertTrue(json.contains("a\\\"b"));
    assertTrue(json.contains("line\\nend"));
    assertTrue(json.contains("a\\\\b"));
    assertEquals("null", JsonOutput.write(null));
    assertEquals("\"text\"", JsonOutput.write("text"));
  }

  @Test
  void unavailableStorageIsReportedAndDoesNotClaimSuccess() throws Exception {
    Files.writeString(directory.resolve("store"), "not a directory");
    var result = invoke("", "inspect");
    assertEquals(2, result.code());
    assertTrue(result.err().contains("Cannot open persistent store"));
  }
}
