package com.lovetropics.donations;

import java.util.EnumMap;
import java.util.Map;

public class DonationCounter implements DonationTotals {
    private final Map<DonationGroup, Double> amount = new EnumMap<>(DonationGroup.class);

    public void update(final DonationGroup group, final double total) {
        amount.put(group, total);
    }

    public void copyFrom(final DonationCounter counters) {
        amount.clear();
        amount.putAll(counters.amount);
    }

    @Override
    public double get(final DonationGroup group) {
        return amount.getOrDefault(group, 0.0);
    }
}
