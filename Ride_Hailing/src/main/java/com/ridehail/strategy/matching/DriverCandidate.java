package com.ridehail.strategy.matching;

public record DriverCandidate(
        String id,
        int pickupDistanceKm,
        double rating,
        int idleMinutes,
        boolean available
) {
    public DriverCandidate {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Driver ID is required");
        }
        if (pickupDistanceKm < 0) {
            throw new IllegalArgumentException(
                    "Pickup distance cannot be negative"
            );
        }
        if (!Double.isFinite(rating) || rating < 0 || rating > 5) {
            throw new IllegalArgumentException(
                    "Rating must be between 0 and 5"
            );
        }
        if (idleMinutes < 0) {
            throw new IllegalArgumentException(
                    "Idle time cannot be negative"
            );
        }
    }
}