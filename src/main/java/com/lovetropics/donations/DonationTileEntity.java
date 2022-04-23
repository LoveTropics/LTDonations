package com.lovetropics.donations;

import com.lovetropics.donations.backend.tiltify.TickerDonation;
import com.lovetropics.lib.entity.FireworkPalette;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DonationTileEntity extends TileEntity implements ITickableTileEntity {

    private boolean registered;
    
    private int queued = 0;
    private int randomOffset = 0;

    public DonationTileEntity(TileEntityType<? extends DonationTileEntity> type) {
        super(type);
    }
    
    @Override
    public void setLevelAndPosition(World worldIn, BlockPos pos) {
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
    public void load(BlockState blockState, CompoundNBT nbt) {
		super.load(blockState, nbt);
		this.queued = nbt.getInt("queuedDonations");
	}

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
		super.save(nbt);
		nbt.putInt("queuedDonations", queued);
		return nbt;
	}
}
