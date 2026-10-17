package com.ridehail.app;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/** Renders only plain snapshots. It has no dependency on any subsystem. */
public final class JsonOutput {
  private JsonOutput() {}

  public static String write(Object value) {
    if (value == null) return "null";
    if (value instanceof Number || value instanceof Boolean) return value.toString();
    if (value instanceof Map<?, ?> map)
      return map.entrySet().stream()
          .map(e -> quote(e.getKey().toString()) + ":" + write(e.getValue()))
          .collect(Collectors.joining(",", "{", "}"));
    if (value instanceof Collection<?> collection)
      return collection.stream().map(JsonOutput::write).collect(Collectors.joining(",", "[", "]"));
    return quote(value.toString());
  }

  private static String quote(String text) {
    StringBuilder result = new StringBuilder("\"");
    for (char c : text.toCharArray()) {
      switch (c) {
        case '"' -> result.append("\\\"");
        case '\\' -> result.append("\\\\");
        case '\n' -> result.append("\\n");
        case '\r' -> result.append("\\r");
        case '\t' -> result.append("\\t");
        case '\b' -> result.append("\\b");
        case '\f' -> result.append("\\f");
        default -> {
          if (c < 32) result.append(String.format("\\u%04x", (int) c));
          else result.append(c);
        }
      }
    }
    return result.append('"').toString();
  }
}
