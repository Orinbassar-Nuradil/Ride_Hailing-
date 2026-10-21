package com.ridehail.strategy.pricing;

public final class SimulatedTrafficSource implements TrafficSource {

    private final int additionalDelay;

    public SimulatedTrafficSource(int additionalDelay) {
        if (additionalDelay < 0) {
            throw new IllegalArgumentException(
                    "Additional delay cannot be negative"
            );
        }

        this.additionalDelay = additionalDelay;
    }

    @Override
    public int additionalDelayMinutes(int distanceKm) {
        return additionalDelay;
    }
}