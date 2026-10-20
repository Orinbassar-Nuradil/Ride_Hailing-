package com.ridehail.reservation;

import com.ridehail.shared.RideException;
import java.util.Map;

/** Pure offline routing rule. It has no database and no service dependencies. */
public final class GpsRouter {
  private static final Map<String, int[]> POINTS =
      Map.of(
          "campus",
          new int[] {0, 0},
          "station",
          new int[] {3, 4},
          "riverside",
          new int[] {0, 6},
          "hospital",
          new int[] {5, 1});

  public int distance(String pickup, String destination) {
    if (pickup == null
        || destination == null
        || !POINTS.containsKey(pickup)
        || !POINTS.containsKey(destination)
        || pickup.equals(destination)) {
      throw new RideException.InvalidRequest(
          "Choose two different stops: campus, station, riverside, hospital");
    }
    var from = POINTS.get(pickup);
    var to = POINTS.get(destination);
    return Math.abs(from[0] - to[0]) + Math.abs(from[1] - to[1]);
  }
}
