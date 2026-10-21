package com.ridehail.strategy.matching;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class DriverMatchingContext {

    private DriverMatchingStrategy strategy;

    public DriverMatchingContext(DriverMatchingStrategy strategy) {
        setStrategy(strategy);
    }

    public void setStrategy(DriverMatchingStrategy strategy) {
        this.strategy = Objects.requireNonNull(
                strategy, "Driver matching strategy is required"
        );
    }

    public Optional<DriverCandidate> choose(
            List<DriverCandidate> candidates
    ) {
        return strategy.choose(List.copyOf(candidates));
    }
}