package com.lovetropics.donations;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.NonNullLazyValue;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

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
		
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerCommands);
		
		registrate()
			.addDataGenerator(ProviderType.LANG, p -> p.add(ITEM_GROUP, "LTDonations"));
	}
	
	private void registerCommands(FMLServerStartingEvent event) {
        CommandDonation.register(event.getCommandDispatcher());
	}
}
