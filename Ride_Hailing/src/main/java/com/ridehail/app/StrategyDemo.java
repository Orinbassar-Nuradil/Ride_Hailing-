package com.ridehail.app;

import com.ridehail.strategy.matching.*;
import com.ridehail.strategy.pricing.*;

import java.util.List;

public final class StrategyDemo {

    public static void main(String[] args) {
        demonstratePricing();
        demonstrateMatching();
    }

    private static void demonstratePricing() {
        int distanceKm = 20;
        int delayMinutes = 10;

        System.out.println("=== PRICING ===");
        System.out.println("Same trip: 20 km, 10 minutes delay");

        List<PricingStrategy> strategies = List.of(
                new StandardPricingStrategy(),
                new TrafficPricingStrategy(),
                new AirportPricingStrategy()
        );

        for (PricingStrategy strategy : strategies) {
            var context = new PricingContext(strategy);

            System.out.println(
                    strategy.getClass().getSimpleName()
                            + ": "
                            + context.calculate(distanceKm, delayMinutes)
                            + " KZT"
            );
        }

        var selector = new PricingSelector();

        PricingStrategy manual =
                selector.selectManual("STANDARD");

        PricingStrategy automatic =
                selector.selectAutomatic(false, delayMinutes);

        System.out.println(
                "Manual choice: "
                        + manual.getClass().getSimpleName()
        );

        System.out.println(
                "Automatic choice: "
                        + automatic.getClass().getSimpleName()
        );

        var context = new PricingContext(manual);

        System.out.println(
                "Before runtime switch: "
                        + context.calculate(distanceKm, delayMinutes)
                        + " KZT"
        );

        context.setStrategy(automatic);

        System.out.println(
                "After runtime switch: "
                        + context.calculate(distanceKm, delayMinutes)
                        + " KZT"
        );

        TrafficSource source = new SimulatedTrafficSource(5);

        context.setStrategy(new TrafficPricingStrategy(source));

        System.out.println(
                "With external source (+5 minutes): "
                        + context.calculate(distanceKm, delayMinutes)
                        + " KZT"
        );
    }

    private static void demonstrateMatching() {
        List<DriverCandidate> drivers = List.of(
                new DriverCandidate("D1", 1, 4.2, 5, true),
                new DriverCandidate("D2", 4, 4.9, 10, true),
                new DriverCandidate("D3", 3, 4.5, 40, true),
                new DriverCandidate("D4", 0, 5.0, 100, false)
        );

        System.out.println();
        System.out.println("=== DRIVER MATCHING ===");
        System.out.println("Same candidates for every strategy:");
        drivers.forEach(System.out::println);

        List<DriverMatchingStrategy> strategies = List.of(
                new NearestDriverStrategy(),
                new HighestRatedDriverStrategy(),
                new LongestIdleDriverStrategy()
        );

        for (DriverMatchingStrategy strategy : strategies) {
            var context = new DriverMatchingContext(strategy);

            System.out.println(
                    strategy.getClass().getSimpleName()
                            + ": "
                            + selectedDriver(context, drivers)
            );
        }

        var selector = new DriverMatchingSelector();

        DriverMatchingStrategy manual =
                selector.selectManual("LONGEST_IDLE");

        DriverMatchingStrategy automatic =
                selector.selectAutomatic(true, false);

        System.out.println(
                "Manual choice: "
                        + manual.getClass().getSimpleName()
        );

        System.out.println(
                "Automatic choice: "
                        + automatic.getClass().getSimpleName()
        );

        var context = new DriverMatchingContext(manual);

        System.out.println(
                "Before runtime switch: "
                        + selectedDriver(context, drivers)
        );

        context.setStrategy(automatic);

        System.out.println(
                "After runtime switch: "
                        + selectedDriver(context, drivers)
        );
    }

    private static String selectedDriver(
            DriverMatchingContext context,
            List<DriverCandidate> drivers
    ) {
        return context.choose(drivers)
                .map(DriverCandidate::id)
                .orElse("No available driver");
    }
}