package com.lovetropics.donations.monument;

import com.lovetropics.donations.DonationState;
import net.minecraft.server.MinecraftServer;

public interface Monument {
    void tick(MinecraftServer server, DonationState state);

    void sync(DonationState state);

    MonumentData toData();
}
