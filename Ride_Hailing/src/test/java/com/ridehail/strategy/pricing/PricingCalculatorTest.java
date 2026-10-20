package com.ridehail.strategy.pricing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PricingCalculatorTest {

    private final PricingCalculator calculator =
            new PricingCalculator();

    @Test
    void standardUsesDistance() {
        assertEquals(4100L,
                calculator.calculate("STANDARD", 20, 10));
    }

    @Test
    void trafficIncludesDelay() {
        assertEquals(4600L,
                calculator.calculate("TRAFFIC", 20, 10));

        assertEquals(4100L,
                calculator.calculate("TRAFFIC", 20, 0));
    }

    @Test
    void airportIncludesFirstFifteenKilometres() {
        assertEquals(3000L,
                calculator.calculate("AIRPORT", 10, 0));

        assertEquals(3000L,
                calculator.calculate("AIRPORT", 15, 0));

        assertEquals(4000L,
                calculator.calculate("AIRPORT", 20, 0));
    }

    @ParameterizedTest
    @CsvSource({
            "STANDARD, 1, 0",
            "TRAFFIC, 1, 0",
            "AIRPORT, 1, 0",
            "STANDARD, 20, 10",
            "TRAFFIC, 20, 10",
            "AIRPORT, 20, 10"
    })
    void priceIsNeverNegative(
            String mode,
            int distanceKm,
            int delayMinutes
    ) {
        assertTrue(
                calculator.calculate(
                        mode, distanceKm, delayMinutes
                ) >= 0
        );
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(null, 20, 10));

        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate("UNKNOWN", 20, 10));

        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate("STANDARD", 0, 10));

        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate("STANDARD", -1, 10));

        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate("TRAFFIC", 20, -1));
    }

    @Test
    void handlesLargeValuesWithoutIntegerOverflow() {
        assertEquals(
                386_547_056_960L,
                calculator.calculate(
                        "STANDARD", Integer.MAX_VALUE, 0
                )
        );
    }
}