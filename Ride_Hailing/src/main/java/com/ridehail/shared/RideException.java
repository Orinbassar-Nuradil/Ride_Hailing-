package com.ridehail.shared;

/** Expected domain failures. Storage failures deliberately use a separate type. */
public class RideException extends RuntimeException {
  public RideException(String message) {
    super(message);
  }

  public static final class InvalidRequest extends RideException {
    public InvalidRequest(String message) {
      super(message);
    }
  }

  public static final class DriverUnavailable extends RideException {
    public DriverUnavailable(String message) {
      super(message);
    }
  }

  public static final class FareRejected extends RideException {
    public FareRejected(String message) {
      super(message);
    }
  }

  public static final class PaymentDeclined extends RideException {
    public PaymentDeclined(String message) {
      super(message);
    }
  }

  public static final class BookingConflict extends RideException {
    public BookingConflict(String message) {
      super(message);
    }
  }

  public static final class IllegalTransition extends RideException {
    public IllegalTransition(String message) {
      super(message);
    }
  }

  public static final class UnknownBooking extends RideException {
    public UnknownBooking(String message) {
      super(message);
    }
  }
}
