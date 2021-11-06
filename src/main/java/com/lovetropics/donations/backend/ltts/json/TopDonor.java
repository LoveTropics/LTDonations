package com.lovetropics.donations.backend.ltts.json;

import java.util.List;
import java.util.UUID;

public final class TopDonor {
    public final UUID uuid;
    public final String minecraftName;
    public final List<String> displayNames;
    public final double total;

    public TopDonor(UUID uuid, String minecraftName, List<String> displayNames, double total) {
        this.uuid = uuid;
        this.minecraftName = minecraftName;
        this.displayNames = displayNames;
        this.total = total;
    }
}
