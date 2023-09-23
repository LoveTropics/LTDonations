package com.lovetropics.donations;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class DonationListenerBlockEntity extends BlockEntity {
	@Nullable
    private DonationListener activeListener;

    private int queued = 0;
    private int randomOffset = 0;

	public int getQueued() {
		return queued;
	}

	public void setQueued(int queued) {
		this.queued = queued;
	}

	public int getRandomOffset() {
		return randomOffset;
	}

	public void setRandomOffset(int randomOffset) {
		this.randomOffset = randomOffset;
	}

	public DonationListenerBlockEntity(BlockEntityType<? extends DonationListenerBlockEntity> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void setLevel(final Level level) {
		super.setLevel(level);
		randomOffset = level.random.nextInt(20);
	}

	public void monitorListener() {
		if (this.activeListener == null) {
			DonationListener listener = (server, name, amount) -> this.triggerDonation();
			DonationListeners.register(listener);
			this.activeListener = listener;
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
