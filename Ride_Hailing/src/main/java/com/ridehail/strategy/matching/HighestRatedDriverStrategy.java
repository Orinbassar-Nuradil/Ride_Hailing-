package com.ridehail.strategy.matching;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class HighestRatedDriverStrategy
        implements DriverMatchingStrategy {

    @Override
    public Optional<DriverCandidate> choose(
            List<DriverCandidate> candidates
    ) {
        return candidates.stream()
                .filter(DriverCandidate::available)
                .min(
                        Comparator.comparingDouble(
                                        DriverCandidate::rating
                                ).reversed()
                                .thenComparing(DriverCandidate::id)
                );
    }
}