package com.ridehail.strategy.pricing;

import java.util.Objects;

public final class PricingContext {

    private PricingStrategy strategy;

    public PricingContext(PricingStrategy strategy) {
        setStrategy(strategy);
    }

    public void setStrategy(PricingStrategy strategy) {
        this.strategy = Objects.requireNonNull(
                strategy, "Pricing strategy is required"
        );
    }

    public long calculate(int distanceKm, int delayMinutes) {
        return strategy.calculate(distanceKm, delayMinutes);
    }
}