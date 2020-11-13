package com.lovetropics.donations;

import com.lovetropics.donations.backend.ltts.DonationHandler;
import com.lovetropics.donations.backend.ltts.DonationRequests;
import com.lovetropics.donations.backend.ltts.WebSocketEvent;
import com.lovetropics.donations.backend.ltts.WebSocketHelper;
import com.lovetropics.donations.backend.ltts.json.EventAction;
import com.lovetropics.donations.command.CommandDonation;
import com.lovetropics.donations.monument.MonumentManager;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.NonNullLazyValue;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@Mod(LTDonations.MODID)
public class LTDonations {

	public static final String MODID = "ltdonations";

	public static final ItemGroup ITEM_GROUP = new ItemGroup(MODID) {

		@Override
		public ItemStack createIcon() {
			return DonationBlock.BLOCK.asStack();
		}
	};

	private static NonNullLazyValue<Registrate> registrate = new NonNullLazyValue<>(
			() -> Registrate.create(MODID).itemGroup(() -> ITEM_GROUP));

	public static Registrate registrate() {
		return registrate.get();
	}

	public LTDonations() {
    	// Compatible with all versions that match the semver (excluding the qualifier e.g. "-beta+42")
    	ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(LTDonations::getCompatVersion, (s, v) -> LTDonations.isCompatibleVersion(s)));

		DonationBlock.register();
		DonationLangKeys.init(registrate());

		MinecraftForge.EVENT_BUS.addListener(this::serverStartingEvent);
		MinecraftForge.EVENT_BUS.addListener(this::serverStoppingEvent);

		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DonationConfigs.COMMON_CONFIG);

		registrate().addDataGenerator(ProviderType.LANG, p -> p.add(ITEM_GROUP, "LTDonations"));
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

	public static final WebSocketHelper WEBSOCKET = new WebSocketHelper();
	
	private void serverStartingEvent(FMLServerStartingEvent event) {
        CommandDonation.register(event.getCommandDispatcher());

        WEBSOCKET.open();

        final DonationRequests startupRequests = new DonationRequests();
        CompletableFuture.supplyAsync(() -> startupRequests.getUnprocessedEvents())
        	.thenAccept(events -> events.forEach(e -> WebSocketEvent.WHITELIST.act(EventAction.create, e)));
        CompletableFuture.supplyAsync(() -> startupRequests.getTotalDonations())
        	.thenAcceptAsync(t -> {
        		// Make sure no monument updates run before the initial one
        		DonationHandler.monument = new MonumentManager();
        		// Run a forced update (no particles)
        		DonationHandler.monument.updateMonument(t, true);

        		DonationHandler.topDonors = new TopDonorManager();
        		DonationHandler.topDonors.pollTopDonors();
        	}, event.getServer());
	}

	private void serverStoppingEvent(final FMLServerStoppingEvent event) {
		DonationHandler.close();
	}
}
