package com.ridehail.strategy.pricing;

public final class PricingCalculator {

    private final PricingSelector selector =
            new PricingSelector();

    public long calculate(
            String mode,
            int distanceKm,
            int delayMinutes
    ) {
        PricingStrategy strategy =
                selector.selectManual(mode);

        PricingContext context =
                new PricingContext(strategy);

        return context.calculate(distanceKm, delayMinutes);
    }
}