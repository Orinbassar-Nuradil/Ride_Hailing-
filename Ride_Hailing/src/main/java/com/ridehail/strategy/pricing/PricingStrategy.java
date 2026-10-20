package com.ridehail.strategy.pricing;

public interface PricingStrategy {

    long calculate(int distanceKm, int delayMinutes);
}
