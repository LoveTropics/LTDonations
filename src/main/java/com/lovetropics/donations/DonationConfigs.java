package com.lovetropics.donations;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = LTDonations.MODID, bus = Bus.MOD)
public class DonationConfigs {

    private static final Builder COMMON_BUILDER = new Builder();

    public static final CategoryTechStack TECH_STACK = new CategoryTechStack();
    public static final CategoryTopDonors TOP_DONORS = new CategoryTopDonors();

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
	}
}
