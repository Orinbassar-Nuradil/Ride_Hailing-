package com.ridehail.app;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
  @Test
  void eachIndependentSubsystemHasFiveClassesAndNoCrossSubsystemImports() throws Exception {
    var packages = List.of("reservation", "pricing", "payment");
    for (String subsystem : packages) {
      Path source = Path.of("src/main/java/com/ridehail", subsystem);
      try (var files = Files.list(source)) {
        var classes = files.filter(p -> p.toString().endsWith(".java")).toList();
        assertEquals(5, classes.size(), subsystem);
        for (Path file : classes) {
          String text = Files.readString(file);
          for (String other : packages)
            if (!subsystem.equals(other))
              assertFalse(text.contains("com.ridehail." + other), file.toString());
          assertFalse(text.contains("com.ridehail.facade"), file.toString());
          assertFalse(text.contains("com.ridehail.journal"), file.toString());
        }
      }
    }
  }
}
