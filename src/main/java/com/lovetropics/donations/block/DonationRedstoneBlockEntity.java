package com.lovetropics.donations.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class DonationRedstoneBlockEntity extends DonationListenerBlockEntity {

	private int countdown = 0;
	private int pulseLengthIndex = 19;
	private int pulseLengths[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 40, 60, 80, 100};

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

			if (entity.countdown > 0) {
				entity.countdown--;

				if (entity.countdown == 0) {
					entity.setPoweredState(false);
				}
			}

			if (entity.getQueued() > 0 && (entity.getRandomOffset() == 0 || level.getGameTime() % 20 == entity.getRandomOffset()) && entity.countdown <= 0) {
				entity.setPoweredState(true);
				entity.countdown = entity.pulseLengths[entity.pulseLengthIndex];
				entity.setQueued(entity.getQueued()-1);
				entity.setChanged();
			}
		}
	}

	public void setPoweredState(boolean state) {
		BlockState blockState = level.getBlockState(this.getBlockPos());
		if (blockState.getBlock() instanceof DonationRedstoneBlock) {
			((DonationRedstoneBlock) blockState.getBlock()).setPoweredState(this.getBlockState(), level, this.getBlockPos(), state);

		}
	}

	public void pulseLengthUp(Player pPlayer) {
		pulseLengthIndex++;
		if (pulseLengthIndex >= pulseLengths.length) {
			pulseLengthIndex = 0;
		}
		pPlayer.sendSystemMessage(Component.literal("Set pulse length to " + pulseLengths[pulseLengthIndex]));
	}

	public void pulseLengthDown(Player pPlayer) {
		pulseLengthIndex--;
		if (pulseLengthIndex < 0) {
			pulseLengthIndex = pulseLengths.length-1;
		}
		pPlayer.sendSystemMessage(Component.literal("Set pulse length to " + pulseLengths[pulseLengthIndex]));
	}

	@Override
	public void load(final CompoundTag tag) {
		super.load(tag);
		this.pulseLengthIndex = tag.getInt("pulseLengthIndex");
	}

	@Override
	protected void saveAdditional(final CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("pulseLengthIndex", this.pulseLengthIndex);
	}
}
