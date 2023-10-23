package com.lovetropics.donations;

public interface DonationTotals {
    DonationTotals ZERO = new DonationTotals() {
        @Override
        public double get(final DonationGroup group) {
            return 0.0;
        }

        @Override
        public int getCount(final DonationGroup group) {
            return 0;
        }
    };

    double get(DonationGroup group);

    int getCount(DonationGroup group);
}
