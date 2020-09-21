package com.lovetropics.donations;

import com.lovetropics.donations.command.CommandDonation;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.NonNullLazyValue;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

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
		DonationBlock.register();
		DonationLangKeys.init(registrate());

		MinecraftForge.EVENT_BUS.addListener(this::registerCommands);

		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DonationConfigs.COMMON_CONFIG);

		registrate().addDataGenerator(ProviderType.LANG, p -> p.add(ITEM_GROUP, "LTDonations"));
	}
	
	private void registerCommands(FMLServerStartingEvent event) {
        CommandDonation.register(event.getCommandDispatcher());
	}
}
