package com.ridehail.strategy.matching;

public final class DriverMatchingSelector {

    public DriverMatchingStrategy selectManual(String mode) {
        if (mode == null) {
            throw new IllegalArgumentException(
                    "Matching mode is required"
            );
        }

        return switch (mode) {
            case "NEAREST" -> new NearestDriverStrategy();
            case "HIGHEST_RATED" -> new HighestRatedDriverStrategy();
            case "LONGEST_IDLE" -> new LongestIdleDriverStrategy();
            default -> throw new IllegalArgumentException(
                    "Unknown matching mode: " + mode
            );
        };
    }

    public DriverMatchingStrategy selectAutomatic(
            boolean premiumCustomer,
            boolean highDemand
    ) {
        if (premiumCustomer) {
            return new HighestRatedDriverStrategy();
        }

        if (highDemand) {
            return new NearestDriverStrategy();
        }

        return new LongestIdleDriverStrategy();
    }
}