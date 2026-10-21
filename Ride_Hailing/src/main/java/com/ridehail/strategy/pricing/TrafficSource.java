package com.ridehail.strategy.pricing;

public interface TrafficSource {

    int additionalDelayMinutes(int distanceKm);
}