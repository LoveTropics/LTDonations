package com.lovetropics.donations;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.providers.RegistrateLangProvider;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.TranslatableComponent;

public enum DonationLangKeys {

    COMMAND_RESET_DONATION("command", "donation.reset", "Resetting donation data."),
    COMMAND_RESET_LAST_DONATION("command", "donation.setid", "Reset last seen donation ID to %d."),
    COMMAND_SIMULATE_DONATION("command", "donation.simulate", "Simulating donation for name %s and amount %s"),
    COMMAND_COULDNT_ESTABLISH_CONNECTION("command", "donation.connection_failed", "Couldn't establish a connection to the donation servers"),
    COMMAND_ESTABLISHED_CONNECTION("command", "donation.connection_established", "Re-established a connection to the donation servers!"),

    NEW_DONATION("notification", "donation.new", "%s donated %s!"),
    ;
    
    private final String type, key, value;
    
    private DonationLangKeys(String type, String key) {
        this(type, key, RegistrateLangProvider.toEnglishName(key));
    }
    
    private DonationLangKeys(String type, String key, String value) {
    	this.type = type;
    	this.key = key;
    	this.value = value;
    }
    
    private TranslatableComponent component;
    
    public TranslatableComponent getComponent() {
    	return component;
    }
    
    public TranslatableComponent format(Object... args) {
        return new TranslatableComponent(getComponent().getKey(), args);
    }
    
    public String getLocalizedText() {
        return getComponent().getString();
    }
    
    public static void init(Registrate registrate) {
        for (DonationLangKeys lang : values()) {
            lang.component = registrate.addLang(lang.type, new ResourceLocation(LTDonations.MODID, lang.key), lang.value);
        }
    }
}
