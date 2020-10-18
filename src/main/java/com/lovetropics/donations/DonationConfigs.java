package com.lovetropics.donations;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

public class DonationConfigs {

    private static final Builder COMMON_BUILDER = new Builder();
    
    public static final CategoryTiltify TILTIFY = new CategoryTiltify();
    public static final CategoryTechStack TECH_STACK = new CategoryTechStack();
    
    public static final class CategoryTiltify {
        
        public final ConfigValue<String> appToken;
        public final IntValue campaignId;
        public final IntValue donationTrackerRefreshRate;
        public final IntValue donationAmountPerMonument;
        public final ConfigValue<String> tiltifyCommandRun;
        
        private CategoryTiltify() {
            COMMON_BUILDER.comment("Used for the LoveTropics charity drive.").push("tiltify");
            
            appToken = COMMON_BUILDER
                    .comment("Add a token here to enable donation tracking, leave blank to disable")
                    .define("tiltifyAppToken", "");
            campaignId = COMMON_BUILDER
                    .comment("The tiltify campaign to track donations from")
                    .defineInRange("tiltifyCampaign", 0, 0, 99999999);
            donationTrackerRefreshRate = COMMON_BUILDER
                    .comment("How often the tracker checks for new donations, in seconds")
                    .defineInRange("donationTrackerRefreshRate", 10, 1, 1000);
            donationAmountPerMonument = COMMON_BUILDER
                    .comment("Amount of $ required per monument command run")
                    .defineInRange("donationAmountPerMonument", 500, 1, 100000);
            tiltifyCommandRun = COMMON_BUILDER
                    .comment("Command run when donation comes in")
                    .define("tiltifyCOmmandRun", "function internaluseonly:addmonument");
            
            COMMON_BUILDER.pop();
        }
    }

    public static final class CategoryTechStack {
        public final ConfigValue<String> authKey;
        public final ConfigValue<String> url;
        public final IntValue port;

        private CategoryTechStack() {
            COMMON_BUILDER.comment("Connection to the tech stack").push("techStack");

            authKey = COMMON_BUILDER
                    .comment("API Key used to allow authentication with the tech stack")
                    .define("authKey", "");

            url = COMMON_BUILDER
                    .comment("Websocket url to post to")
                    .define("url", "localhost");

            port = COMMON_BUILDER
                    .comment("Port number the websocket runs on")
                    .defineInRange("port", 0, 0, 99999);
        }
    }

    public static final ForgeConfigSpec COMMON_CONFIG = COMMON_BUILDER.build();
}
