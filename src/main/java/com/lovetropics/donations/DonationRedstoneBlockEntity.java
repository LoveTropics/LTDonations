package com.lovetropics.donations;

import com.lovetropics.lib.entity.FireworkPalette;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class DonationRedstoneBlockEntity extends BlockEntity {
	@Nullable
    private DonationListener activeListener;

    private int queued = 0;
    private int randomOffset = 0;

	private int countdown = 0;

	public DonationRedstoneBlockEntity(BlockEntityType<? extends DonationRedstoneBlockEntity> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void setLevel(final Level level) {
		super.setLevel(level);
		randomOffset = level.random.nextInt(20);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, DonationRedstoneBlockEntity entity) {
		if (!level.isClientSide) {
			if (entity.activeListener == null) {
				DonationListener listener = (server, name, amount) -> entity.triggerDonation();
				DonationListeners.register(listener);
				entity.activeListener = listener;
			}
			if (entity.queued > 0 && level.getGameTime() % 20 == entity.randomOffset && entity.countdown == 0) {

				entity.toggle();

				entity.countdown = 20;

				entity.queued--;
				entity.setChanged();
			}

			if (entity.countdown > 0) {
				entity.countdown--;

				if (entity.countdown == 0) {
					entity.toggle();
				}
			}
		}
	}

	public void toggle() {
		BlockState blockState = level.getBlockState(this.getBlockPos());
		if (blockState.getBlock() instanceof DonationRedstoneBlock) {
			((DonationRedstoneBlock) blockState.getBlock()).toggle(this.getBlockState(), level, this.getBlockPos());
		}
	}

	@Override
	public void setRemoved() {
	    super.setRemoved();
		unregisterListener();
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		unregisterListener();
	}

	private void unregisterListener() {
		DonationListeners.unregister(activeListener);
		activeListener = null;
	}

	@SuppressWarnings("deprecation")
    public void triggerDonation() {
        if (level.hasChunkAt(getBlockPos())) {
            queued++;
            setChanged();
        }
    }

	@Override
	public void load(final CompoundTag tag) {
		super.load(tag);
		this.queued = tag.getInt("queuedDonations");
	}

	@Override
	protected void saveAdditional(final CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("queuedDonations", queued);
	}
}
