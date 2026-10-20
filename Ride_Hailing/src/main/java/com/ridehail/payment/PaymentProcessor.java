package com.ridehail.payment;

import com.ridehail.shared.Checks;
import com.ridehail.shared.RideException;

/** Pure decision; only the gateway can commit a debit. */
public final class PaymentProcessor {
  public int debit(int amount, int balance) {
    Checks.amount(amount, 1);
    if (balance < amount)
      throw new RideException.PaymentDeclined(
          "Wallet has " + balance + " KZT; payment requires " + amount + " KZT");
    return balance - amount;
  }
}
