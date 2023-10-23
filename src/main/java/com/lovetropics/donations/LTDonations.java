package com.lovetropics.donations;

import com.lovetropics.donations.backend.ltts.DonationHandler;
import com.lovetropics.donations.backend.ltts.WebSocketHelper;
import com.lovetropics.donations.backend.ltts.json.Donation;
import com.lovetropics.donations.command.CommandDonation;
import com.tterrag.registrate.Registrate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.NonNullLazy;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

import javax.annotation.Nullable;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Mod(LTDonations.MODID)
public class LTDonations {

	public static final String MODID = "ltdonations";

	public static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

	private static final ResourceLocation TAB_ID = new ResourceLocation(MODID, "ltdonations");

	private static final NonNullLazy<Registrate> REGISTRATE = NonNullLazy.of(() -> Registrate.create(MODID));

	public static Registrate registrate() {
		return REGISTRATE.get();
	}

	public LTDonations() {
    	// Compatible with all versions that match the semver (excluding the qualifier e.g. "-beta+42")
    	ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(LTDonations::getCompatVersion, (s, v) -> LTDonations.isCompatibleVersion(s)));

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

		MinecraftForge.EVENT_BUS.addListener(this::serverStartingEvent);
		MinecraftForge.EVENT_BUS.addListener(this::serverStoppingEvent);
		MinecraftForge.EVENT_BUS.addListener(this::registerCommands);

		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DonationConfigs.COMMON_CONFIG);

		DonationPlaceholders.register();
	}

    private static final Pattern QUALIFIER = Pattern.compile("-\\w+\\+\\d+");
    public static String getCompatVersion() {
    	return getCompatVersion(ModList.get().getModContainerById(MODID).orElseThrow(IllegalStateException::new).getModInfo().getVersion().toString());
    }
    private static String getCompatVersion(String fullVersion) {
    	return QUALIFIER.matcher(fullVersion).replaceAll("");
    }
    public static boolean isCompatibleVersion(String version) {
    	return getCompatVersion().equals(getCompatVersion(version));
    }

	@Nullable
	private static WebSocketHelper websocket;

	private void registerCommands(RegisterCommandsEvent event) {
		CommandDonation.register(event.getDispatcher());
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

	public static DonationTotals totals() {
		return DonationHandler.totals();
	}

	@Nullable
	public static Donation lastDonation() {
		return DonationHandler.getLastDonation();
	}
}
