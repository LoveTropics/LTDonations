package com.lovetropics.donations;

import com.lovetropics.donations.backend.ltts.DonationHandler;
import com.lovetropics.donations.backend.ltts.WebSocketHelper;
import com.lovetropics.donations.block.DonationBlock;
import com.lovetropics.donations.block.DonationGoalRedstoneBlock;
import com.lovetropics.donations.block.DonationRedstoneBlock;
import com.lovetropics.donations.command.LTDonationsCommands;
import com.tterrag.registrate.Registrate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import javax.annotation.Nullable;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

@Mod(LTDonations.MODID)
public class LTDonations {

	public static final String MODID = "ltdonations";

	public static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

	private static final ResourceLocation TAB_ID = ResourceLocation.fromNamespaceAndPath(MODID, "ltdonations");

	private static final Lazy<Registrate> REGISTRATE = Lazy.of(() -> Registrate.create(MODID));

	public static Registrate registrate() {
		return REGISTRATE.get();
	}

	public LTDonations(ModContainer container) {
		registrate().generic(TAB_ID.getPath(), Registries.CREATIVE_MODE_TAB, () -> CreativeModeTab.builder()
				.title(registrate().addLang("itemGroup", TAB_ID, "LTDonations"))
				.icon(() -> DonationBlock.BLOCK.asStack())
				.build()
		).build()
				.defaultCreativeTab(ResourceKey.create(Registries.CREATIVE_MODE_TAB, TAB_ID));

		DonationBlock.register();
		DonationRedstoneBlock.register();
		DonationGoalRedstoneBlock.register();
		DonationLangKeys.init(registrate());
		LTDonationsCommands.registerArguments();

		NeoForge.EVENT_BUS.addListener(this::serverStartingEvent);
		NeoForge.EVENT_BUS.addListener(this::serverStoppingEvent);
		NeoForge.EVENT_BUS.addListener(LTDonationsCommands::registerCommands);

		container.registerConfig(ModConfig.Type.COMMON, DonationConfigs.COMMON_CONFIG);

		DonationPlaceholders.register();
	}

	@Nullable
	private static WebSocketHelper websocket;

	public static ResourceLocation location(String path) {
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}

	private void serverStartingEvent(ServerStartingEvent event) {
        final MinecraftServer server = event.getServer();
        DonationHandler.initialize(server);
        websocket = new WebSocketHelper(() -> {
			// In the time we haven't been connected to the websocket, we might have missed events
			// Note: there's still a potential race condition here where we receive the total with outdated information as an event comes in at the same time
            DonationHandler.fetchFullState(server, false);
        });
    }

	private void serverStoppingEvent(final ServerStoppingEvent event) {
		DonationHandler.close(event.getServer());
	}

    public static WebSocketHelper websocket() {
        return Objects.requireNonNull(websocket, "Websocket has not been initialized");
    }

	public static DonationState state() {
		return DonationHandler.state();
	}
}
