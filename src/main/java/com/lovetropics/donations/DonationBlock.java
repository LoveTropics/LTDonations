package com.lovetropics.donations;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DonationBlock extends Block implements EntityBlock {

	public static final BlockEntry<DonationBlock> BLOCK = LTDonations.registrate()
			.block("donation", DonationBlock::new)
			.properties(p -> Block.Properties.copy(Blocks.BEDROCK).noDrops())
			.blockEntity(DonationBlockEntity::new).build()
			.simpleItem()
			.register();

	public static final BlockEntityEntry<DonationBlockEntity> ENTITY = BlockEntityEntry.cast(LTDonations.registrate().get("donation", BlockEntityType.class));

    // TODO move this somewhere else
	public static final BlockEntry<AirBlock> AIR_LIGHT = LTDonations.registrate()
			.block("air_light", p -> (AirBlock) new AirBlock(p) {})
			.initialProperties(() -> Blocks.AIR)
			.properties(p -> p.lightLevel(s -> 15))
			.register();

    public static final void register() {}

	public DonationBlock(Block.Properties properties) {
		super(properties);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack stack, BlockGetter worldIn, List<Component> tooltip, TooltipFlag flagIn) {
		super.appendHoverText(stack, worldIn, tooltip, flagIn);
		tooltip.add(new TranslatableComponent(this.getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
		return new DonationBlockEntity(ENTITY.get(), pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
		return createTicker(type, ENTITY.get(), DonationBlockEntity::tick);
	}

	@Nullable
	@SuppressWarnings("unchecked")
	private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTicker(final BlockEntityType<A> type, final BlockEntityType<E> tickerType, final BlockEntityTicker<? super E> ticker) {
		return tickerType == type ? (BlockEntityTicker<A>) ticker : null;
	}
}
