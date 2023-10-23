package com.lovetropics.donations;

import com.lovetropics.donations.backend.ltts.json.Donation;

import javax.annotation.Nullable;
import java.time.Instant;

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

        @Nullable
        @Override
        public Donation getLastDonation() {
            return null;
        }

        @Nullable
        @Override
        public LeadingTeam getLeadingTeam() {
            return null;
        }
    };

    double getAmount(DonationGroup group);

    int getCount(DonationGroup group);

    @Nullable
    Donation getLastDonation();

    @Nullable
    LeadingTeam getLeadingTeam();

    record LeadingTeam(DonationGroup group, Instant sinceTime) {
    }
}
