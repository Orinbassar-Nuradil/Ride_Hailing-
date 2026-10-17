package com.ridehail.shared;

public final class Checks {
  private Checks() {}

  public static String id(String value) {
    if (value == null || !value.matches("[A-Za-z0-9_-]{1,40}")) {
      throw new RideException.InvalidRequest(
          "IDs require 1-40 letters, digits, underscores or hyphens");
    }
    return value;
  }

  public static int amount(int value, int minimum) {
    if (value < minimum || value > 1_000_000) {
      throw new RideException.InvalidRequest("Amount must be between " + minimum + " and 1000000");
    }
    return value;
  }
}
