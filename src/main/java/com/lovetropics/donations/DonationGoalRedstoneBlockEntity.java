package com.lovetropics.donations;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class DonationGoalRedstoneBlockEntity extends BlockEntity {

	private int donationGoalIndex = 1;
	private int donationGoalStep = 250;
	private int randomOffset = 0;
	private boolean lastPoweredState = false;

	public int getRandomOffset() {
		return randomOffset;
	}

	public void setRandomOffset(int randomOffset) {
		this.randomOffset = randomOffset;
	}

	public DonationGoalRedstoneBlockEntity(BlockEntityType<? extends DonationGoalRedstoneBlockEntity> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void setLevel(final Level level) {
		super.setLevel(level);
		setRandomOffset(20);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, DonationGoalRedstoneBlockEntity entity) {
		if (!level.isClientSide) {

			if (entity.getRandomOffset() == 0 || level.getGameTime() % 20 == 0) {
				if (LTDonations.totals().get(DonationGroup.ALL) >= entity.getDonationGoalAmount()) {
					if (!entity.lastPoweredState) {
						entity.setPoweredState(true);
					}
				} else {
					if (entity.lastPoweredState) {
						entity.setPoweredState(false);
					}
				}
			}
		}
	}

	public int getDonationGoalAmount() {
		return this.donationGoalIndex * this.donationGoalStep;
	}

	public void setPoweredState(boolean state) {
		BlockState blockState = level.getBlockState(this.getBlockPos());
		if (blockState.getBlock() instanceof DonationGoalRedstoneBlock) {
			((DonationGoalRedstoneBlock) blockState.getBlock()).setPoweredState(this.getBlockState(), level, this.getBlockPos(), state);
			this.lastPoweredState = state;
		}
	}

	public void pulseLengthUp(Player pPlayer) {
		donationGoalIndex++;
		pPlayer.sendSystemMessage(Component.literal("Donation goal trigger set to " + getDonationGoalAmount()));
	}

	public void pulseLengthDown(Player pPlayer) {
		donationGoalIndex--;
		if (donationGoalIndex < 0) {
			donationGoalIndex = 0;
		}
		pPlayer.sendSystemMessage(Component.literal("Donation goal trigger set to " + getDonationGoalAmount()));
	}

	@Override
	public void load(final CompoundTag tag) {
		super.load(tag);
		this.donationGoalIndex = tag.getInt("donationGoalIndex");
		this.lastPoweredState = tag.getBoolean("lastPoweredState");
	}

	@Override
	protected void saveAdditional(final CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("donationGoalIndex", this.donationGoalIndex);
		tag.putBoolean("lastPoweredState", this.lastPoweredState);
	}
}
