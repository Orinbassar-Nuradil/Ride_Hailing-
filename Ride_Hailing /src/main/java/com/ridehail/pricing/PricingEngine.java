package com.ridehail.pricing;

import com.ridehail.shared.Checks;
import com.ridehail.shared.RideException;

/** Pure deterministic fare calculation in integer KZT. */
public final class PricingEngine {
  public int quote(int distanceKm, int maxFare) {
    Checks.amount(distanceKm, 1);
    Checks.amount(maxFare, 0);
    long amount = 500L + 180L * distanceKm;
    if (amount > maxFare)
      throw new RideException.FareRejected(
          "Fare " + amount + " KZT exceeds passenger limit " + maxFare + " KZT");
    return (int) amount;
  }
}
