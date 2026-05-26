package com.lovetropics.donations;

import com.lovetropics.donations.backend.ltts.json.Donation;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.EnumMap;

public interface DonationState {
    DonationState ZERO = new DonationState() {
        @Override
        public double getAmount(DonationGroup group) {
            return 0.0;
        }

        @Override
        public int getCount(DonationGroup group) {
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

    static DonationState copyOf(DonationState state) {
        EnumMap<DonationGroup, Double> amounts = new EnumMap<>(DonationGroup.class);
        EnumMap<DonationGroup, Integer> counts = new EnumMap<>(DonationGroup.class);
        for (DonationGroup group : DonationGroup.values()) {
            amounts.put(group, state.getAmount(group));
            counts.put(group, state.getCount(group));
        }
        Donation lastDonation = state.getLastDonation();
        LeadingTeam leadingTeam = state.getLeadingTeam();
        return new DonationState() {
            @Override
            public double getAmount(DonationGroup group) {
                return amounts.get(group);
            }

            @Override
            public int getCount(DonationGroup group) {
                return counts.get(group);
            }

            @Override
            @Nullable
            public Donation getLastDonation() {
                return lastDonation;
            }

            @Override
            @Nullable
            public LeadingTeam getLeadingTeam() {
                return leadingTeam;
            }
        };
    }

    double getAmount(DonationGroup group);

    int getCount(DonationGroup group);

    @Nullable
    Donation getLastDonation();

    @Nullable
    LeadingTeam getLeadingTeam();

    record LeadingTeam(DonationGroup group, Instant sinceTime) {
    }
}
