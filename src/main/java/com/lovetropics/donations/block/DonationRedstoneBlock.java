package com.lovetropics.donations.block;

import com.lovetropics.donations.LTDonations;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DonationRedstoneBlock extends Block implements EntityBlock {

	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	public static final BlockEntry<DonationRedstoneBlock> BLOCK = LTDonations.registrate()
			.block("donation_redstone", DonationRedstoneBlock::new)
			.initialProperties(() -> Blocks.BEDROCK)
			.properties(Properties::noLootTable)
			.blockstate((ctx, prov) -> prov.getVariantBuilder(ctx.get()).forAllStates(state -> {
				String name = ctx.getName() + (state.getValue(DonationRedstoneBlock.POWERED) ? "_on" : "_off");
				BlockModelBuilder model = prov.models().cubeAll(name, prov.modLoc("block/" + name));
				return ConfiguredModel.builder().modelFile(model).build();
			}))
			.lang("Donation Redstone Emitter")
			.item()
				.model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/" + ctx.getName() + "_off")))
			.build()
			.blockEntity(DonationRedstoneBlockEntity::new).build()

			.register();

	public static final BlockEntityEntry<DonationRedstoneBlockEntity> ENTITY = BlockEntityEntry.cast(LTDonations.registrate().get("donation_redstone", Registries.BLOCK_ENTITY_TYPE));

    public static final void register() {}

	public DonationRedstoneBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, Boolean.valueOf(false)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
		pBuilder.add(POWERED);
	}

	@Override
	public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
		if (!pLevel.isClientSide && pHand == InteractionHand.MAIN_HAND) {
			BlockEntity ent = pLevel.getBlockEntity(pPos);
			if (ent instanceof DonationRedstoneBlockEntity) {
				if (pPlayer.isCrouching()) {
					((DonationRedstoneBlockEntity) ent).pulseLengthDown(pPlayer);
				} else {
					((DonationRedstoneBlockEntity) ent).pulseLengthUp(pPlayer);
				}
				return InteractionResult.SUCCESS;
			}
		}
		return super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack stack, BlockGetter worldIn, List<Component> tooltip, TooltipFlag flagIn) {
		super.appendHoverText(stack, worldIn, tooltip, flagIn);
		tooltip.add(Component.translatable(this.getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
		return new DonationRedstoneBlockEntity(ENTITY.get(), pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
		return createTicker(type, ENTITY.get(), DonationRedstoneBlockEntity::tick);
	}

	@Nullable
	@SuppressWarnings("unchecked")
	private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTicker(final BlockEntityType<A> type, final BlockEntityType<E> tickerType, final BlockEntityTicker<? super E> ticker) {
		return tickerType == type ? (BlockEntityTicker<A>) ticker : null;
	}

	@Override
	public int getSignal(BlockState pState, BlockGetter pLevel, BlockPos pPos, Direction pDirection) {
		return pState.getValue(POWERED) ? 15 : 0;
	}

	@Override
	public int getDirectSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
		return pBlockState.getValue(POWERED) ? 15 : 0;
	}

	@Override
	public boolean isSignalSource(BlockState pState) {
		return true;
	}

	public BlockState setPoweredState(BlockState pState, Level pLevel, BlockPos pPos, boolean state) {
		pLevel.setBlock(pPos, pState.setValue(POWERED, Boolean.valueOf(state)), 3);
		pLevel.updateNeighborsAt(pPos, this);
		return pState;
	}

	public BlockState toggle(BlockState pState, Level pLevel, BlockPos pPos) {
		pState = pState.cycle(POWERED);
		pLevel.setBlock(pPos, pState, 3);
		pLevel.updateNeighborsAt(pPos, this);
		return pState;
	}
}
