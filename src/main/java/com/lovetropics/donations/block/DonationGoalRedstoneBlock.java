package com.lovetropics.donations.block;

import com.lovetropics.donations.LTDonations;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import org.jetbrains.annotations.Nullable;

public class DonationGoalRedstoneBlock extends Block implements EntityBlock {

	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	public static final BlockEntry<DonationGoalRedstoneBlock> BLOCK = LTDonations.registrate()
			.block("donation_goal_redstone", DonationGoalRedstoneBlock::new)
			.initialProperties(() -> Blocks.BEDROCK)
			.properties(Properties::noLootTable)
            .blockstate(() -> DonationBlockModels::generateFullPoweredBlock)
			.lang("Donation Threshold Goal Redstone Emitter")
			.item()
            .model(() -> (ctx, prov) -> prov.generateBlockItem(ctx.get(), "_off"))
			.build()
			.blockEntity(DonationGoalRedstoneBlockEntity::new).build()

			.register();

    public static final BlockEntityEntry<DonationGoalRedstoneBlockEntity> ENTITY = BlockEntityEntry.cast(LTDonations.registrate().get("donation_goal_redstone", Registries.BLOCK_ENTITY_TYPE));

    public static final void register() {}

	public DonationGoalRedstoneBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
		pBuilder.add(POWERED);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof DonationGoalRedstoneBlockEntity blockEntity) {
                if (player.isCrouching()) {
					blockEntity.pulseLengthDown(serverPlayer);
				} else {
					blockEntity.pulseLengthUp(serverPlayer);
				}
				return InteractionResult.SUCCESS;
			}
		}
		return super.useWithoutItem(state, level, pos, player, hitResult);
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
		return new DonationGoalRedstoneBlockEntity(ENTITY.get(), pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
		return createTicker(type, ENTITY.get(), DonationGoalRedstoneBlockEntity::tick);
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

	public boolean isPowered(BlockState pState, Level pLevel, BlockPos pPos) {
		return pState.getValue(POWERED).booleanValue();
	}

	public BlockState toggle(BlockState pState, Level pLevel, BlockPos pPos) {
		pState = pState.cycle(POWERED);
		pLevel.setBlock(pPos, pState, 3);
		pLevel.updateNeighborsAt(pPos, this);
		return pState;
	}
}
