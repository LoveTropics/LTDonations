package com.lovetropics.donations;

import com.lovetropics.donations.backend.ltts.DonationHandler;
import com.lovetropics.donations.backend.ltts.json.Donation;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.resources.ResourceLocation;

public class DonationPlaceholders {
    private static final PlaceholderResult NO_DONATIONS_YET = PlaceholderResult.value("No donations yet! :(");

    public static void register() {
        Placeholders.register(id("total"), (ctx, arg) -> {
            final double total = DonationHandler.totals().get(DonationGroup.ALL);
            return PlaceholderResult.value(LTDonations.CURRENCY_FORMAT.format(total));
        });

        Placeholders.register(id("last_donor"), (ctx, arg) -> {
            final Donation lastDonation = DonationHandler.getLastDonation();
            if (lastDonation == null) {
                return NO_DONATIONS_YET;
            }
            return PlaceholderResult.value(lastDonation.getNameShown());
        });

        Placeholders.register(id("last_donation"), (ctx, arg) -> {
            final Donation lastDonation = DonationHandler.getLastDonation();
            if (lastDonation == null) {
                return NO_DONATIONS_YET;
            }
            return PlaceholderResult.value(LTDonations.CURRENCY_FORMAT.format(lastDonation.getAmount()));
        });
    }

    private static ResourceLocation id(final String id) {
        return new ResourceLocation(LTDonations.MODID, id);
    }
}
