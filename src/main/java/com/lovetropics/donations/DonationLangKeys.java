package com.lovetropics.donations;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.providers.ProviderType;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public enum DonationLangKeys {

    COMMAND_RESET_DONATION("command", "donation.reset", "Resetting donation data."),
    COMMAND_RESET_LAST_DONATION("command", "donation.setid", "Reset last seen donation ID to %d."),
    COMMAND_SIMULATE_DONATION("command", "donation.simulate", "Simulating donation for name %s and amount %s"),
    COMMAND_COULDNT_ESTABLISH_CONNECTION("command", "donation.connection_failed", "Couldn't establish a connection to the donation servers"),
    COMMAND_ESTABLISHED_CONNECTION("command", "donation.connection_established", "Re-established a connection to the donation servers!"),

    NEW_DONATION("notification", "donation.new", "%s donated %s!"),
    ;

    private final String key;
    private final String value;

    DonationLangKeys(final String type, final String key, final String value) {
        this.key = Util.makeDescriptionId(type, new ResourceLocation(LTDonations.MODID, key));
        this.value = value;
    }

    public Component getComponent() {
        return Component.translatable(key);
    }

    public Component format(final Object... args) {
        return Component.translatable(key, args);
    }

    public static void init(final Registrate registrate) {
        registrate.addDataGenerator(ProviderType.LANG, prov -> {
            for (final DonationLangKeys lang : values()) {
                prov.add(lang.key, lang.value);
            }
        });
    }
}
