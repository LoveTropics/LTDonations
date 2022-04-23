package com.lovetropics.donations;

import com.lovetropics.donations.backend.tiltify.TickerDonation;
import com.lovetropics.lib.entity.FireworkPalette;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class DonationBlockEntity extends BlockEntity {
    private boolean registered;
    
    private int queued = 0;
    private int randomOffset = 0;

	public DonationBlockEntity(BlockEntityType<? extends DonationBlockEntity> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public void setLevel(final Level level) {
		super.setLevel(level);
		randomOffset = level.random.nextInt(20);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, DonationBlockEntity entity) {
		if (!level.isClientSide) {
			if (!entity.registered) {
				TickerDonation.addCallback(entity);
				entity.registered = true;
			}
			if (entity.queued > 0 && level.getGameTime() % 20 == entity.randomOffset) {
				BlockPos fireworkPos = pos.above();
				while (!level.isEmptyBlock(fireworkPos) && fireworkPos.getY() < pos.getY() + 10) {
					fireworkPos = fireworkPos.above();
				}

				FireworkPalette.OSA_CONSERVATION.spawn(fireworkPos, level);
				entity.queued--;
				entity.setChanged();
			}
		}
	}

	@Override
	public void setRemoved() {
	    super.setRemoved();
	    TickerDonation.removeCallback(this);
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		TickerDonation.removeCallback(this);
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
