package com.lovetropics.donations;

import com.lovetropics.donations.backend.ltts.DonationHandler;
import com.lovetropics.donations.backend.ltts.DonationRequests;
import com.lovetropics.donations.backend.ltts.WebSocketEvent;
import com.lovetropics.donations.backend.ltts.WebSocketHelper;
import com.lovetropics.donations.backend.ltts.json.EventAction;
import com.lovetropics.donations.command.CommandDonation;
import com.lovetropics.donations.monument.MonumentManager;
import com.tterrag.registrate.Registrate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@Mod(LTDonations.MODID)
public class LTDonations {

	public static final String MODID = "ltdonations";

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
		DonationLangKeys.init(registrate());

		MinecraftForge.EVENT_BUS.addListener(this::serverStartingEvent);
		MinecraftForge.EVENT_BUS.addListener(this::serverStoppingEvent);
		MinecraftForge.EVENT_BUS.addListener(this::registerCommands);

		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DonationConfigs.COMMON_CONFIG);
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

	public static final NonNullLazy<WebSocketHelper> WEBSOCKET = NonNullLazy.of(WebSocketHelper::new);

	private void registerCommands(RegisterCommandsEvent event) {
		CommandDonation.register(event.getDispatcher());
	}

	private void serverStartingEvent(ServerStartingEvent event) {
        final DonationRequests startupRequests = DonationRequests.get();
        CompletableFuture.supplyAsync(startupRequests::getUnprocessedEvents)
        	.thenAcceptAsync(events -> events.forEach(e -> WebSocketEvent.WHITELIST.act(EventAction.create, e)), event.getServer());
        CompletableFuture.supplyAsync(startupRequests::getTotalDonations)
        	.thenAcceptAsync(t -> {
        		// Make sure no monument updates run before the initial one
				MonumentManager monument = new MonumentManager();
        		// Run a forced update (no particles)
				monument.updateMonument(t, true);
				DonationListeners.register(monument);
				DonationHandler.monument = monument;

        		DonationHandler.topDonors = new TopDonorManager();
        		DonationHandler.topDonors.pollTopDonors();
        	}, event.getServer());
	}

	private void serverStoppingEvent(final ServerStoppingEvent event) {
		DonationHandler.close();
	}
}
