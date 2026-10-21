package com.ridehail.strategy.pricing;

import java.util.Objects;

public final class TrafficPricingStrategy implements PricingStrategy {

    private final TrafficSource trafficSource;

    public TrafficPricingStrategy() {
        this(new SimulatedTrafficSource(0));
    }

    public TrafficPricingStrategy(TrafficSource trafficSource) {
        this.trafficSource = Objects.requireNonNull(
                trafficSource, "Traffic source is required"
        );
    }

    @Override
    public long calculate(int distanceKm, int delayMinutes) {
        if (distanceKm <= 0) {
            throw new IllegalArgumentException(
                    "Distance must be positive"
            );
        }

        if (delayMinutes < 0) {
            throw new IllegalArgumentException(
                    "Delay cannot be negative"
            );
        }

        int additionalDelay =
                trafficSource.additionalDelayMinutes(distanceKm);

        if (additionalDelay < 0) {
            throw new IllegalArgumentException(
                    "Traffic source returned a negative delay"
            );
        }

        long totalDelay = (long) delayMinutes + additionalDelay;

        return 500L + 180L * distanceKm + 50L * totalDelay;
    }
}