package com.lovetropics.donations;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.providers.ProviderType;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public enum DonationLangKeys {
    COMMAND_SIMULATE_DONATION("command", "donation.simulate", "Simulating donation for name %s and amount %s"),
    ADDED_MONUMENT("command", "donation.added_monument", "A monument has been created with id: '%s'"),
    REMOVED_MONUMENT("command", "donation.removed_monument", "A monument has been removed with id: '%s'"),
    INVALID_DONATION_GROUP("command", "donation.invalid_group", "There is no donation group with the id: '%s'"),
    MONUMENT_ALREADY_EXISTS("command", "donation.monument_already_exists", "There is already a monument with the id: '%s'"),
    MONUMENT_DOES_NOT_EXIST("command", "donation.monument_does_not_exist", "There is no monument with the id: '%s'"),
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
