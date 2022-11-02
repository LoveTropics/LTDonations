package com.lovetropics.donations;

import net.minecraft.core.BlockPos;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = LTDonations.MODID, bus = Bus.MOD)
public class DonationConfigs {

    private static final Builder COMMON_BUILDER = new Builder();
    
    public static final CategoryTiltify TILTIFY = new CategoryTiltify();
    public static final CategoryTechStack TECH_STACK = new CategoryTechStack();
    public static final CategoryMonument MONUMENT = new CategoryMonument();
    public static final CategoryTopDonors TOP_DONORS = new CategoryTopDonors();

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
        public final ConfigValue<String> apiUrl;
        public final ConfigValue<String> websocketUrl;

        private CategoryTechStack() {
            COMMON_BUILDER.comment("Connection to the tech stack").push("techStack");

            authKey = COMMON_BUILDER
                    .comment("API Key used to allow authentication with the tech stack")
                    .define("authKey", "");

            apiUrl = COMMON_BUILDER
                    .comment("API url to post to")
                    .define("apiUrl", "http://localhost");

            websocketUrl = COMMON_BUILDER
                    .comment("Websocket url to receive from")
                    .define("websocketUrl", "wss://localhost:443/ws");

            COMMON_BUILDER.pop();
        }
    }

    public static final class CategoryMonument {
    	public final BooleanValue active;

        public final ConfigValue<String> posConfig;
        public BlockPos pos = BlockPos.ZERO;
        
        public final ConfigValue<String> dimension;
        
        private CategoryMonument() {
            COMMON_BUILDER.comment("Monument Settings").push("monument");

            active = COMMON_BUILDER
            		.comment("Activate the monument manager, defaults to false so you get a chance to configure the position/dimension first")
            		.define("active", false);

            posConfig = COMMON_BUILDER
                    .comment("Position of the monument, given as the center position comma separated, e.g. 42,60,-99")
                    .define("pos", "0,64,0", s -> s instanceof String && tryParse((String) s) != null);
            
            dimension = COMMON_BUILDER
            		.comment("Dimension the monument is in")
            		.define("dimension", "tropicraft:tropics");
            
            COMMON_BUILDER.pop();
        }

		BlockPos tryParse(String cfg) {
			String[] coords = cfg.split(",");
			if (coords.length != 3) {
				return null;
			}
			try {
				return new BlockPos(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]));
			} catch (NumberFormatException e) {
				return null;
			}
		}
    }

    public static final class CategoryTopDonors {
        public final BooleanValue active;

        public final ConfigValue<String> dimension;
        public final ConfigValue<List<? extends String>> topDonorUuidsConfig;

        private CategoryTopDonors() {
            COMMON_BUILDER.comment("Top Donor Settings").push("top_donor");

            active = COMMON_BUILDER
                    .comment("Activate the top donator manager, defaults to false so you get a chance to configure the position/entities first")
                    .define("active", false);

            dimension = COMMON_BUILDER
                    .comment("Dimension the top donor display is in")
                    .define("dimension", "tropicraft:tropics");

            topDonorUuidsConfig = COMMON_BUILDER
                    .comment("The list of top donator entities to use")
                    .defineList("top_donor_uuids", new ArrayList<>(), o -> true);

            COMMON_BUILDER.pop();
        }

        public UUID[] getTopDonorUuids() {
            if(this.active.get()) {
                List<? extends String> uuidStrings = this.topDonorUuidsConfig.get();
                List<UUID> uuids = new ArrayList<>(uuidStrings.size());
                for (String string : uuidStrings) {
                    try {
                        uuids.add(UUID.fromString(string));
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }
                return uuids.toArray(new UUID[0]);
            } else {
                return new UUID[0];
            }
        }
    }

    public static final ForgeConfigSpec COMMON_CONFIG = COMMON_BUILDER.build();

    @SubscribeEvent
	public static void configLoad(ModConfigEvent.Loading event) {
		parseConfigs();
	}

	@SubscribeEvent
	public static void configReload(ModConfigEvent.Reloading event) {
		parseConfigs();
	}

	public static void parseConfigs() {
		MONUMENT.pos = MONUMENT.tryParse(MONUMENT.posConfig.get());
	}
}
