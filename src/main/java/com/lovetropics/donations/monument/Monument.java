package com.lovetropics.donations.monument;

import com.lovetropics.donations.DonationTotals;
import net.minecraft.server.MinecraftServer;

public interface Monument {
    void tick(MinecraftServer server, DonationTotals totals);

    void sync(DonationTotals totals);

    MonumentData toData();
}
