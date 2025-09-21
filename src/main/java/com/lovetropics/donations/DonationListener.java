package com.lovetropics.donations;

import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;
import java.util.UUID;

public interface DonationListener {
    void handleDonation(MinecraftServer server, Details details);

    record Details(
            double amount,
            String name,
            @Nullable UUID donorPlayerId,
            double donorTotal,
            DonationState oldState,
            DonationState newState
    ) {
    }
}
