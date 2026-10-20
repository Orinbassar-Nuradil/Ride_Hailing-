package com.ridehail.pricing;

import com.ridehail.shared.Checks;

public final class ReceiptGenerator {
  public FareReceipt create(String id, int distanceKm, int amount) {
    return new FareReceipt(Checks.id(id), distanceKm, amount, "RH-" + id);
  }
}
