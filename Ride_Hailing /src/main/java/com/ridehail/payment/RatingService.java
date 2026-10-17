package com.ridehail.payment;

import com.ridehail.shared.RideException;

/** Schedules a future invitation; it does not solicit a rating before the ride. */
public final class RatingService {
  public String schedule(PaymentRecord payment) {
    if (!payment.status().equals("SETTLED"))
      throw new RideException.PaymentDeclined("A rating invitation requires settled payment");
    return "AFTER_RIDE:" + payment.bookingId();
  }
}
