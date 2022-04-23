package com.lovetropics.donations.backend.tiltify;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public class DonationData extends SavedData {
    
    public static final String ID = "donationData";

    //now uses completedAt because tiltify id ordering unreliable
    private long lastSeenDate = 0;
    private int lastSeenId = 0;

    private int monumentsPlaced = 0;

    public DonationData() {
        super(ID);
    }

    @Override
    public void load(CompoundTag nbt) {
        lastSeenDate = nbt.getLong("lastSeenDate");
        lastSeenId = nbt.getInt("lastSeenId");
        monumentsPlaced = nbt.getInt("monumentsPlaced");
    }

    @Override
    public CompoundTag save(CompoundTag compound) {
        compound.putLong("lastSeenDate", lastSeenDate);
        compound.putInt("lastSeenId", lastSeenId);
        compound.putInt("monumentsPlaced", monumentsPlaced);
        return compound;
    }
    
    public long getLastSeenDate() {
        return lastSeenDate;
    }
    
    public void setLastSeenDate(long id) {
        this.lastSeenDate = id;
        setDirty();
    }

    public int getLastSeenId() {
        return lastSeenId;
    }

    public void setLastSeenId(int id) {
        this.lastSeenId = id;
        setDirty();
    }

    public int getMonumentsPlaced() {
        return monumentsPlaced;
    }

    public void setMonumentsPlaced(int monumentsPlaced) {
        this.monumentsPlaced = monumentsPlaced;
    }

    public void resetData() {
        lastSeenDate = 0;
        lastSeenId = 0;
        monumentsPlaced = 0;
    }
}
