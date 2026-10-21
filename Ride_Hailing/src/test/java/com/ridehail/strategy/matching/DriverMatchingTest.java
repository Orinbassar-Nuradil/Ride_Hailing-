package com.ridehail.strategy.matching;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

class DriverMatchingTest {

    private final List<DriverCandidate> drivers = List.of(
            new DriverCandidate("D1", 1, 4.2, 5, true),
            new DriverCandidate("D2", 4, 4.9, 10, true),
            new DriverCandidate("D3", 3, 4.5, 40, true),
            new DriverCandidate("D4", 0, 5.0, 100, false)
    );

    static Stream<DriverMatchingStrategy> strategies() {
        return Stream.of(
                new NearestDriverStrategy(),
                new HighestRatedDriverStrategy(),
                new LongestIdleDriverStrategy()
        );
    }

    @Test
    void nearestChoosesClosestAvailableDriver() {
        var result = new NearestDriverStrategy().choose(drivers);

        assertEquals("D1", result.orElseThrow().id());
    }

    @Test
    void highestRatedChoosesBestAvailableRating() {
        var result = new HighestRatedDriverStrategy().choose(drivers);

        assertEquals("D2", result.orElseThrow().id());
    }

    @Test
    void longestIdleChoosesLongestWaitingAvailableDriver() {
        var result = new LongestIdleDriverStrategy().choose(drivers);

        assertEquals("D3", result.orElseThrow().id());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void everyStrategyReturnsAnAvailableInputDriver(
            DriverMatchingStrategy strategy
    ) {
        var selected = strategy.choose(drivers).orElseThrow();

        assertTrue(selected.available());
        assertTrue(drivers.contains(selected));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void everyStrategyHandlesEmptyList(
            DriverMatchingStrategy strategy
    ) {
        assertTrue(strategy.choose(List.of()).isEmpty());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void everyStrategyHandlesNoAvailableDrivers(
            DriverMatchingStrategy strategy
    ) {
        var unavailable = List.of(
                new DriverCandidate("D4", 0, 5.0, 100, false)
        );

        assertTrue(strategy.choose(unavailable).isEmpty());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void tiesAreResolvedByDriverId(
            DriverMatchingStrategy strategy
    ) {
        var tied = List.of(
                new DriverCandidate("D2", 2, 4.8, 20, true),
                new DriverCandidate("D1", 2, 4.8, 20, true)
        );

        assertEquals(
                "D1",
                strategy.choose(tied).orElseThrow().id()
        );
    }

    @Test
    void contextSwitchesStrategyAtRuntime() {
        var context = new DriverMatchingContext(
                new NearestDriverStrategy()
        );

        assertEquals("D1", context.choose(drivers).orElseThrow().id());

        context.setStrategy(new HighestRatedDriverStrategy());

        assertEquals("D2", context.choose(drivers).orElseThrow().id());

        context.setStrategy(new LongestIdleDriverStrategy());

        assertEquals("D3", context.choose(drivers).orElseThrow().id());
    }

    @Test
    void contextRejectsNullStrategy() {
        assertThrows(
                NullPointerException.class,
                () -> new DriverMatchingContext(null)
        );
    }

    @Test
    void rejectedReplacementKeepsPreviousStrategy() {
        var context = new DriverMatchingContext(
                new NearestDriverStrategy()
        );

        assertThrows(
                NullPointerException.class,
                () -> context.setStrategy(null)
        );

        assertEquals("D1", context.choose(drivers).orElseThrow().id());
    }

    @Test
    void selectorSupportsEveryManualMode() {
        var selector = new DriverMatchingSelector();

        assertInstanceOf(
                NearestDriverStrategy.class,
                selector.selectManual("NEAREST")
        );
        assertInstanceOf(
                HighestRatedDriverStrategy.class,
                selector.selectManual("HIGHEST_RATED")
        );
        assertInstanceOf(
                LongestIdleDriverStrategy.class,
                selector.selectManual("LONGEST_IDLE")
        );
    }

    @Test
    void selectorRejectsInvalidManualMode() {
        var selector = new DriverMatchingSelector();

        assertThrows(
                IllegalArgumentException.class,
                () -> selector.selectManual(null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> selector.selectManual("UNKNOWN")
        );
    }

    @Test
    void premiumCustomerUsesHighestRating() {
        var selector = new DriverMatchingSelector();

        assertInstanceOf(
                HighestRatedDriverStrategy.class,
                selector.selectAutomatic(true, false)
        );
    }

    @Test
    void premiumRuleHasPriorityOverDemand() {
        var selector = new DriverMatchingSelector();

        assertInstanceOf(
                HighestRatedDriverStrategy.class,
                selector.selectAutomatic(true, true)
        );
    }

    @Test
    void highDemandUsesNearestDriver() {
        var selector = new DriverMatchingSelector();

        assertInstanceOf(
                NearestDriverStrategy.class,
                selector.selectAutomatic(false, true)
        );
    }

    @Test
    void normalDemandUsesLongestIdleDriver() {
        var selector = new DriverMatchingSelector();

        assertInstanceOf(
                LongestIdleDriverStrategy.class,
                selector.selectAutomatic(false, false)
        );
    }
}