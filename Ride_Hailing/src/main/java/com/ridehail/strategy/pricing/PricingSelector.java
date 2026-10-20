package com.ridehail.strategy.pricing;

public final class PricingSelector {

    public PricingStrategy selectManual(String mode) {
        if (mode == null) {
            throw new IllegalArgumentException(
                    "Pricing mode is required"
            );
        }

        return switch (mode) {
            case "STANDARD" -> new StandardPricingStrategy();
            case "TRAFFIC" -> new TrafficPricingStrategy();
            case "AIRPORT" -> new AirportPricingStrategy();
            default -> throw new IllegalArgumentException(
                    "Unknown pricing mode: " + mode
            );
        };
    }

    public PricingStrategy selectAutomatic(
            boolean airportTrip,
            int delayMinutes
    ) {
        if (delayMinutes < 0) {
            throw new IllegalArgumentException(
                    "Delay cannot be negative"
            );
        }

        if (airportTrip) {
            return new AirportPricingStrategy();
        }

        if (delayMinutes >= 10) {
            return new TrafficPricingStrategy();
        }

        return new StandardPricingStrategy();
    }
}