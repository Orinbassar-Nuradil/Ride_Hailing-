package com.ridehail.strategy.pricing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PricingSelectorTest {

    private final PricingSelector selector =
            new PricingSelector();

    @Test
    void manualSelectionSupportsEveryMode() {
        assertInstanceOf(
                StandardPricingStrategy.class,
                selector.selectManual("STANDARD")
        );

        assertInstanceOf(
                TrafficPricingStrategy.class,
                selector.selectManual("TRAFFIC")
        );

        assertInstanceOf(
                AirportPricingStrategy.class,
                selector.selectManual("AIRPORT")
        );
    }

    @Test
    void manualSelectionRejectsMissingMode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> selector.selectManual(null)
        );
    }

    @Test
    void manualSelectionRejectsUnknownMode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> selector.selectManual("UNKNOWN")
        );
    }

    @Test
    void airportTripUsesAirportPricing() {
        assertInstanceOf(
                AirportPricingStrategy.class,
                selector.selectAutomatic(true, 0)
        );
    }

    @Test
    void airportRuleHasPriorityOverTraffic() {
        assertInstanceOf(
                AirportPricingStrategy.class,
                selector.selectAutomatic(true, 20)
        );
    }

    @Test
    void tenMinuteDelayUsesTrafficPricing() {
        assertInstanceOf(
                TrafficPricingStrategy.class,
                selector.selectAutomatic(false, 10)
        );
    }

    @Test
    void longerDelayUsesTrafficPricing() {
        assertInstanceOf(
                TrafficPricingStrategy.class,
                selector.selectAutomatic(false, 20)
        );
    }

    @Test
    void delayBelowThresholdUsesStandardPricing() {
        assertInstanceOf(
                StandardPricingStrategy.class,
                selector.selectAutomatic(false, 9)
        );
    }

    @Test
    void noDelayUsesStandardPricing() {
        assertInstanceOf(
                StandardPricingStrategy.class,
                selector.selectAutomatic(false, 0)
        );
    }

    @Test
    void automaticSelectionRejectsNegativeDelay() {
        assertThrows(
                IllegalArgumentException.class,
                () -> selector.selectAutomatic(false, -1)
        );
    }
}
