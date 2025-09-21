package com.lovetropics.donations;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.providers.ProviderType;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;

public enum DonationLangKeys {
    COMMAND_SIMULATE_DONATION("command", "donation.simulate", "Simulating donation for name %s and amount %s"),
    ADDED_MONUMENT("command", "donation.added_monument", "A monument has been created with id: '%s'"),
    UPDATED_MONUMENT("command", "donation.updated_monument", "The monument with id '%s' has been updated"),
    REMOVED_MONUMENT("command", "donation.removed_monument", "A monument has been removed with id: '%s'"),
    MONUMENT_ALREADY_EXISTS("command", "donation.monument_already_exists", "There is already a monument with the id: '%s'"),
    MONUMENT_DOES_NOT_EXIST("command", "donation.monument_does_not_exist", "There is no monument with the id: '%s'"),
    MONUMENT_TYPE_UNKNOWN("command", "donation.monument_type_unknown", "There is no monument type with the id: '%s'"),
    MONUMENT_CONFIG_INVALID("command", "donation.monument_config_invalid", "The monument config is invalid: %s"),
    NEW_DONATION("notification", "donation.new", "%s donated %s!"),
    GROUP_ALL("donation_group", "all", "Global"),
    TEAM_CENTS("donation_group", "team_cents", "Team Cents"),
    TEAM_NO_CENTS("donation_group", "team_no_cents", "Team No Cents"),
    TEAM_NICE("donation_group", "team_nice", "Team Nice"),
    TIME_MINUTES("time", "minutes", "%s minutes");

    private final String key;
    private final String value;

    DonationLangKeys(final String type, final String key, final String value) {
        this.key = Util.makeDescriptionId(type, LTDonations.location(key));
        this.value = value;
    }

    public Component getComponent() {
        return Component.translatable(key);
    }

    public Component format(final Object... args) {
        return Component.translatableEscape(key, args);
    }

    public static void init(final Registrate registrate) {
        registrate.addDataGenerator(ProviderType.LANG, prov -> {
            for (final DonationLangKeys lang : values()) {
                prov.add(lang.key, lang.value);
            }
        });
    }
}
