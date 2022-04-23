package com.lovetropics.donations;

import com.lovetropics.donations.backend.tiltify.TickerDonation;
import com.lovetropics.lib.entity.FireworkPalette;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.TickableBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class DonationTileEntity extends BlockEntity implements TickableBlockEntity {

    private boolean registered;
    
    private int queued = 0;
    private int randomOffset = 0;

    public DonationTileEntity(BlockEntityType<? extends DonationTileEntity> type) {
        super(type);
    }
    
    @Override
    public void setLevelAndPosition(Level worldIn, BlockPos pos) {
        super.setLevelAndPosition(worldIn, pos);
        this.randomOffset = worldIn.getRandom().nextInt(20);
    }
    
	@Override
	public void tick() {
	    if (!getLevel().isClientSide) {
	        if (!registered) {
	            TickerDonation.addCallback(this);
	            registered = true;
	        }
	        if (queued > 0 && getLevel().getGameTime() % 20 == randomOffset) {
	            BlockPos pos = getBlockPos().above();
	            while (!getLevel().isEmptyBlock(pos) && pos.getY() < getBlockPos().getY() + 10) {
	                pos = pos.above();
	            }

				FireworkPalette.OSA_CONSERVATION.spawn(pos, getLevel());
	            queued--;
	            setChanged();
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
    public void load(BlockState blockState, CompoundTag nbt) {
		super.load(blockState, nbt);
		this.queued = nbt.getInt("queuedDonations");
	}

    @Override
    public CompoundTag save(CompoundTag nbt) {
		super.save(nbt);
		nbt.putInt("queuedDonations", queued);
		return nbt;
	}
}
