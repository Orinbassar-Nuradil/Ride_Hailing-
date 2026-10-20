package com.ridehail.strategy.pricing;

public final class PricingCalculator {

    public long calculate(
            String mode,
            int distanceKm,
            int delayMinutes
    ) {
        if (mode == null) {
            throw new IllegalArgumentException("Pricing mode is required");
        }
        if (distanceKm <= 0) {
            throw new IllegalArgumentException("Distance must be positive");
        }
        if (delayMinutes < 0) {
            throw new IllegalArgumentException("Delay cannot be negative");
        }

        return switch (mode) {
            case "STANDARD" -> 500L + 180L * distanceKm;

            case "TRAFFIC" ->
                    500L + 180L * distanceKm + 50L * delayMinutes;

            case "AIRPORT" ->
                    3000L + 200L * Math.max(0, distanceKm - 15);

            default ->
                    throw new IllegalArgumentException(
                            "Unknown pricing mode: " + mode
                    );
        };
    }
}

