package com.lovetropics.donations.block;

import com.lovetropics.donations.DonationListener;
import com.lovetropics.donations.DonationListeners;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class DonationListenerBlockEntity extends BlockEntity {
	@Nullable
    private DonationListener activeListener;
	private double threshold;
	private double upperThreshold = Double.MAX_VALUE;

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
			DonationListener listener = (server, name, amount, totals) -> this.triggerDonation(amount);
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
    public void triggerDonation(double amount) {
		if (amount < threshold || amount > upperThreshold) {
			return;
		}
        if (level.hasChunkAt(getBlockPos())) {
            queued++;
            setChanged();
        }
    }

	@Override
	public void load(final CompoundTag tag) {
		super.load(tag);
		this.queued = tag.getInt("queuedDonations");
		threshold = tag.getDouble("threshold");
		if (tag.contains("upper_threshold", Tag.TAG_DOUBLE)) {
			upperThreshold = tag.getDouble("upper_threshold");
		}
	}

	@Override
	protected void saveAdditional(final CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("queuedDonations", queued);
		tag.putDouble("threshold", threshold);
		if (upperThreshold != Double.MAX_VALUE) {
			tag.putDouble("upper_threshold", upperThreshold);
		}
	}
}
