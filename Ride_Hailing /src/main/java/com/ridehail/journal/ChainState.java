package com.ridehail.journal;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record ChainState(
    BookingRequest request, String status, String reason, List<String> events) {
  public ChainState {
    events = List.copyOf(events);
  }

  public boolean terminal() {
    return Set.of("COMPLETED", "COMPENSATED", "FAILED").contains(status);
  }

  public Map<String, Object> snapshot() {
    return Map.of(
        "bookingId",
        request.bookingId(),
        "request",
        request.snapshot(),
        "status",
        status,
        "reason",
        reason,
        "events",
        events);
  }
}
