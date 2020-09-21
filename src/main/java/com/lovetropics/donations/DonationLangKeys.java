package com.lovetropics.donations;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.providers.RegistrateLangProvider;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;

public enum DonationLangKeys {

    COMMAND_RESET_DONATION("command", "donation.reset", "Resetting donation data."),
    COMMAND_RESET_LAST_DONATION("command", "donation.setid", "Reset last seen donation ID to %d."),
    COMMAND_SIMULATE_DONATION("command", "donation.simulate", "Simulating donation for name %s and amount %s"),

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
    
    private TranslationTextComponent component;
    
    public TranslationTextComponent getComponent() {
    	return component;
    }
    
    public TranslationTextComponent format(Object... args) {
        return new TranslationTextComponent(getComponent().getKey(), args);
    }
    
    public String getLocalizedText() {
        return getComponent().getFormattedText();
    }
    
    public static void init(Registrate registrate) {
        for (DonationLangKeys lang : values()) {
            lang.component = registrate.addLang(lang.type, new ResourceLocation(LTDonations.MODID, lang.key), lang.value);
        }
    }
}
