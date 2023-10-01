package com.lovetropics.donations;

public interface DonationTotals {
    DonationTotals ZERO = group -> 0.0;

    double get(DonationGroup group);
}
