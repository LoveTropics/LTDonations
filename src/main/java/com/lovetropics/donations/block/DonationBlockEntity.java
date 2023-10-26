package com.lovetropics.donations.block;

import com.lovetropics.lib.entity.FireworkPalette;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class DonationBlockEntity extends DonationListenerBlockEntity {

	public DonationBlockEntity(BlockEntityType<? extends DonationBlockEntity> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void setLevel(final Level level) {
		super.setLevel(level);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, DonationBlockEntity entity) {
		if (!level.isClientSide) {
			entity.monitorListener();
			if (entity.getQueued() > 0 && level.getGameTime() % 20 == entity.getRandomOffset()) {
				BlockPos fireworkPos = pos.above();
				while (!level.isEmptyBlock(fireworkPos) && fireworkPos.getY() < pos.getY() + 10) {
					fireworkPos = fireworkPos.above();
				}

				FireworkPalette.OSA_CONSERVATION.spawn(fireworkPos, level);
				entity.setQueued(entity.getQueued()-1);
				entity.setChanged();
			}
		}
	}
}
