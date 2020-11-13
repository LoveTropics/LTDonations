package com.lovetropics.donations.backend.ltts.json;

import java.util.UUID;

public final class TopDonor {
    public final UUID uuid;
    public final String minecraftName;
    public final double total;

    public TopDonor(UUID uuid, String minecraftName, double total) {
        this.uuid = uuid;
        this.minecraftName = minecraftName;
        this.total = total;
    }
}
