package com.ridehail.payment;

import com.ridehail.shared.Checks;
import com.ridehail.shared.RideException;

/** Public C API, usable without a reservation, receipt or facade. */
public final class PaymentService {
  private final PaymentRepository repository;
  private final PaymentProcessor processor = new PaymentProcessor();
  private final RatingService ratings = new RatingService();

  public PaymentService(PaymentRepository repository) {
    this.repository = repository;
  }

  public PaymentRecord pay(String id, String passenger, int amount, int openingBalance) {
    Checks.id(id);
    Checks.id(passenger);
    Checks.amount(amount, 1);
    Checks.amount(openingBalance, 0);
    var item =
        repository.settle(
            new PaymentRecord(id, passenger, amount), openingBalance, processor, ratings);
    if (item.status().equals("DECLINED"))
      throw new RideException.PaymentDeclined(
          "Payment for " + id + " declined: insufficient wallet funds");
    return item;
  }

  public PaymentRecord inspect(String id) {
    return repository.find(id);
  }

  public Integer balance(String passenger) {
    return repository.balance(passenger);
  }
}
