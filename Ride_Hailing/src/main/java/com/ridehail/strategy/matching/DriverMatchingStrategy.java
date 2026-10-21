package com.ridehail.strategy.matching;

import java.util.List;
import java.util.Optional;

public interface DriverMatchingStrategy {

    Optional<DriverCandidate> choose(
            List<DriverCandidate> candidates
    );
}