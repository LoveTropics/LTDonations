package com.lovetropics.donations.monument;

import com.mojang.serialization.Codec;
import net.minecraft.server.MinecraftServer;

public interface MonumentData {
    Codec<MonumentData> CODEC = MonumentType.CODEC.dispatch(MonumentData::type, type -> type.codec().codec());

    Monument create(MinecraftServer server);

    MonumentType type();
}
