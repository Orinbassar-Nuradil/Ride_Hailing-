package com.ridehail.strategy.pricing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrafficPricingStrategyTest {

    @Test
    void includesAdditionalDelayFromFakeSource() {
        TrafficSource fakeSource = distanceKm -> 5;

        var strategy = new TrafficPricingStrategy(fakeSource);

        assertEquals(4850L, strategy.calculate(20, 10));
    }

    @Test
    void supportsReplacingDataSource() {
        TrafficSource clearRoad = distanceKm -> 0;
        TrafficSource busyRoad = distanceKm -> 20;

        var clearStrategy =
                new TrafficPricingStrategy(clearRoad);

        var busyStrategy =
                new TrafficPricingStrategy(busyRoad);

        assertEquals(4600L, clearStrategy.calculate(20, 10));
        assertEquals(5600L, busyStrategy.calculate(20, 10));
    }

    @Test
    void passesTripDistanceToSource() {
        TrafficSource fakeSource = distanceKm -> {
            assertEquals(20, distanceKm);
            return 5;
        };

        var strategy = new TrafficPricingStrategy(fakeSource);

        assertEquals(4850L, strategy.calculate(20, 10));
    }

    @Test
    void rejectsNegativeDelayFromSource() {
        TrafficSource invalidSource = distanceKm -> -1;

        var strategy = new TrafficPricingStrategy(invalidSource);

        assertThrows(
                IllegalArgumentException.class,
                () -> strategy.calculate(20, 10)
        );
    }

    @Test
    void rejectsNullSource() {
        assertThrows(
                NullPointerException.class,
                () -> new TrafficPricingStrategy(null)
        );
    }

    @Test
    void rejectsInvalidTripInput() {
        TrafficSource fakeSource = distanceKm -> 5;

        var strategy = new TrafficPricingStrategy(fakeSource);

        assertThrows(
                IllegalArgumentException.class,
                () -> strategy.calculate(0, 10)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> strategy.calculate(20, -1)
        );
    }

    @Test
    void combinesLargeDelaysWithoutIntegerOverflow() {
        TrafficSource fakeSource =
                distanceKm -> Integer.MAX_VALUE;

        var strategy = new TrafficPricingStrategy(fakeSource);

        assertEquals(
                214_748_365_380L,
                strategy.calculate(1, Integer.MAX_VALUE)
        );
    }

    @Test
    void simulatedSourceReturnsConfiguredDelay() {
        var source = new SimulatedTrafficSource(5);

        assertEquals(5, source.additionalDelayMinutes(20));
    }

    @Test
    void simulatedSourceRejectsNegativeDelay() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SimulatedTrafficSource(-1)
        );
    }
}