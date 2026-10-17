package com.ridehail.reservation;

import com.ridehail.shared.RideException;
import java.util.Set;

/** Pure resource-availability decision, separate from persisted reservation state. */
public final class DriverMatcher {
  private static final Set<String> FLEET = Set.of("D1", "D2", "D3");

  public String choose(String requested, Set<String> occupied) {
    if (requested == null || !FLEET.contains(requested))
      throw new RideException.InvalidRequest("Driver must be D1, D2 or D3");
    if (occupied.contains(requested))
      throw new RideException.DriverUnavailable("Driver " + requested + " is already reserved");
    return requested;
  }
}
