package com.lovetropics.donations;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class DonationRedstoneBlockEntity extends DonationListenerBlockEntity {

	private int countdown = 0;

	public DonationRedstoneBlockEntity(BlockEntityType<? extends DonationRedstoneBlockEntity> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void setLevel(final Level level) {
		super.setLevel(level);
		setRandomOffset(0);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, DonationRedstoneBlockEntity entity) {
		if (!level.isClientSide) {
			entity.monitorListener();
			if (entity.getQueued() > 0 && level.getGameTime() % 20 == entity.getRandomOffset() && entity.countdown <= 0) {
				entity.setPoweredState(true);
				entity.countdown = 20;
				entity.setQueued(entity.getQueued()-1);
				entity.setChanged();
			}

			if (entity.countdown > 0) {
				entity.countdown--;

				if (entity.countdown == 0) {
					entity.setPoweredState(false);
				}
			}
		}
	}

	public void setPoweredState(boolean state) {
		BlockState blockState = level.getBlockState(this.getBlockPos());
		if (blockState.getBlock() instanceof DonationRedstoneBlock) {
			((DonationRedstoneBlock) blockState.getBlock()).setPoweredState(this.getBlockState(), level, this.getBlockPos(), state);

		}
	}
}
