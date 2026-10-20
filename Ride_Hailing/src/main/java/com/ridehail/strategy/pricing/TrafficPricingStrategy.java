package com.ridehail.strategy.pricing;

public final class TrafficPricingStrategy implements PricingStrategy {

    @Override
    public long calculate(int distanceKm, int delayMinutes) {
        validate(distanceKm, delayMinutes);
        return 500L + 180L * distanceKm + 50L * delayMinutes;
    }

    private void validate(int distanceKm, int delayMinutes) {
        if (distanceKm <= 0) {
            throw new IllegalArgumentException("Distance must be positive");
        }
        if (delayMinutes < 0) {
            throw new IllegalArgumentException("Delay cannot be negative");
        }
    }
}