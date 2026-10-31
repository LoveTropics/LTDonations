package com.lovetropics.donations.target;

import com.mojang.serialization.MapCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public interface Target {
    List<ServerPlayer> getTargets(MinecraftServer server);

    MapCodec<? extends Target> getCodec();
}
