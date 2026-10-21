package com.ridehail.strategy.matching;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class NearestDriverStrategy
        implements DriverMatchingStrategy {

    @Override
    public Optional<DriverCandidate> choose(
            List<DriverCandidate> candidates
    ) {
        return candidates.stream()
                .filter(DriverCandidate::available)
                .min(
                        Comparator.comparingInt(
                                DriverCandidate::pickupDistanceKm
                        ).thenComparing(DriverCandidate::id)
                );
    }
}