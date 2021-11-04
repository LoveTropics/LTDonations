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
    public void setWorldAndPos(World worldIn, BlockPos pos) {
        super.setWorldAndPos(worldIn, pos);
        this.randomOffset = worldIn.getRandom().nextInt(20);
    }
    
	@Override
	public void tick() {
	    if (!getWorld().isRemote) {
	        if (!registered) {
	            TickerDonation.addCallback(this);
	            registered = true;
	        }
	        if (queued > 0 && getWorld().getGameTime() % 20 == randomOffset) {
	            BlockPos pos = getPos().up();
	            while (!getWorld().isAirBlock(pos) && pos.getY() < getPos().getY() + 10) {
	                pos = pos.up();
	            }

				FireworkPalette.OSA_CONSERVATION.spawn(pos, getWorld());
	            queued--;
	            markDirty();
	        }
	    }
	}

	@Override
	public void remove() {
	    super.remove();
	    TickerDonation.removeCallback(this);
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		TickerDonation.removeCallback(this);
	}

	@SuppressWarnings("deprecation")
    public void triggerDonation() {
        if (world.isBlockLoaded(getPos())) {
            queued++;
            markDirty();
        }
    }

    @Override
    public void read(BlockState blockState, CompoundNBT nbt) {
		super.read(blockState, nbt);
		this.queued = nbt.getInt("queuedDonations");
	}

    @Override
    public CompoundNBT write(CompoundNBT nbt) {
		super.write(nbt);
		nbt.putInt("queuedDonations", queued);
		return nbt;
	}
}
