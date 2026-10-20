package com.ridehail.strategy.pricing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PricingContextTest {

    @Test
    void usesStrategyProvidedInConstructor() {
        var context = new PricingContext(
                new StandardPricingStrategy()
        );

        assertEquals(4100L, context.calculate(20, 10));
    }

    @Test
    void switchesStrategyAtRuntime() {
        var context = new PricingContext(
                new StandardPricingStrategy()
        );

        assertEquals(4100L, context.calculate(20, 10));

        context.setStrategy(new TrafficPricingStrategy());

        assertEquals(4600L, context.calculate(20, 10));

        context.setStrategy(new AirportPricingStrategy());

        assertEquals(4000L, context.calculate(20, 10));
    }

    @Test
    void acceptsNewAlgorithmWithoutChangingContext() {
        PricingStrategy fixedPrice = (distanceKm, delayMinutes) -> 2500L;

        var context = new PricingContext(fixedPrice);

        assertEquals(2500L, context.calculate(20, 10));
    }

    @Test
    void rejectsNullStrategyInConstructor() {
        assertThrows(
                NullPointerException.class,
                () -> new PricingContext(null)
        );
    }

    @Test
    void rejectedReplacementKeepsPreviousStrategy() {
        var context = new PricingContext(
                new StandardPricingStrategy()
        );

        assertThrows(
                NullPointerException.class,
                () -> context.setStrategy(null)
        );

        assertEquals(4100L, context.calculate(20, 10));
    }
}