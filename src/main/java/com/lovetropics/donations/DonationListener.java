package com.lovetropics.donations;

import net.minecraft.server.MinecraftServer;

public interface DonationListener {
    void handleDonation(MinecraftServer server, String name, double amount);
}
