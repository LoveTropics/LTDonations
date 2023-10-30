package com.lovetropics.donations;

import net.minecraft.server.MinecraftServer;

public interface DonationStateListener {
    void handleState(MinecraftServer server, DonationState state, boolean initial);
}
