package com.lovetropics.donations;

import com.lovetropics.donations.backend.ltts.json.Donation;
import eu.pb4.placeholders.api.Placeholder;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.ServerPlaceholderContext;
import net.minecraft.resources.Identifier;

import java.time.Duration;
import java.time.Instant;

public class DonationPlaceholders {
    private static final PlaceholderResult NO_DONOR_YET = PlaceholderResult.value("Nobody");

    public static void register() {
        Placeholders.registerServer(id("total"), totalHandler(DonationGroup.ALL));
        Placeholders.registerServer(id("count"), countHandler(DonationGroup.ALL));

        for (final DonationGroup group : DonationGroup.values()) {
            Placeholders.registerServer(id("total/" + group.getSerializedName()), totalHandler(group));
            Placeholders.registerServer(id("count/" + group.getSerializedName()), countHandler(group));
        }

        Placeholders.registerServer(id("last_donor"), (ctx, arg) -> {
            final Donation lastDonation = LTDonations.state().getLastDonation();
            if (lastDonation == null) {
                return NO_DONOR_YET;
            }
            return PlaceholderResult.value(lastDonation.getNameShown());
        });

        Placeholders.registerServer(id("last_donation"), (ctx, arg) -> {
            final Donation lastDonation = LTDonations.state().getLastDonation();
            final double amount = lastDonation != null ? lastDonation.amount() : 0.0;
            return PlaceholderResult.value(LTDonations.CURRENCY_FORMAT.format(amount));
        });

        Placeholders.registerServer(id("leading_team"), (ctx, arg) -> {
            final DonationState.LeadingTeam leadingTeam = LTDonations.state().getLeadingTeam();
            if (leadingTeam != null) {
                return PlaceholderResult.value(leadingTeam.group().getName());
            }
            return NO_DONOR_YET;
        });

        Placeholders.registerServer(id("leading_team_time"), (ctx, arg) -> {
            final DonationState.LeadingTeam leadingTeam = LTDonations.state().getLeadingTeam();
            if (leadingTeam != null) {
                final Duration leadingTime = Duration.between(leadingTeam.sinceTime(), Instant.now());
                return PlaceholderResult.value(DonationLangKeys.TIME_MINUTES.format(leadingTime.toMinutes()));
            }
            return NO_DONOR_YET;
        });
    }

    private static Placeholder.Handler<ServerPlaceholderContext, String> totalHandler(final DonationGroup group) {
        return (ctx, arg) -> {
            final double total = LTDonations.state().getAmount(group);
            return PlaceholderResult.value(LTDonations.CURRENCY_FORMAT.format(total));
        };
    }

    private static Placeholder.Handler<ServerPlaceholderContext, String> countHandler(final DonationGroup group) {
        return (ctx, arg) -> {
            final int count = LTDonations.state().getCount(group);
            return PlaceholderResult.value(String.valueOf(count));
        };
    }

    private static Identifier id(final String id) {
        return LTDonations.location(id);
    }
}
