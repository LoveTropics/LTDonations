package com.lovetropics.donations.block;

import com.lovetropics.donations.DonationListener;
import com.lovetropics.donations.DonationListeners;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;

public class DonationListenerBlockEntity extends BlockEntity {
    private static final double DEFAULT_UPPER_THRESHOLD = Double.MAX_VALUE;

    @Nullable
    private DonationListener activeListener;
	private double threshold;
    private double upperThreshold = DEFAULT_UPPER_THRESHOLD;

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
			DonationListener listener = (server, details) -> this.triggerDonation(details.amount());
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        queued = input.getIntOr("queuedDonations", 0);
        threshold = input.getDoubleOr("threshold", 0.0);
        upperThreshold = input.getDoubleOr("upperThreshold", DEFAULT_UPPER_THRESHOLD);
	}

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("queuedDonations", queued);
        output.putDouble("threshold", threshold);
        if (upperThreshold != DEFAULT_UPPER_THRESHOLD) {
            output.putDouble("upper_threshold", upperThreshold);
		}
	}
}
