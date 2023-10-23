package com.lovetropics.donations;

public interface DonationState {
    DonationState ZERO = new DonationState() {
        @Override
        public double getAmount(final DonationGroup group) {
            return 0.0;
        }

        @Override
        public int getCount(final DonationGroup group) {
            return 0;
        }
    };

    double getAmount(DonationGroup group);

    int getCount(DonationGroup group);
}
