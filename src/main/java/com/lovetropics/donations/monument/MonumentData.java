package com.lovetropics.donations.monument;

import com.mojang.serialization.Codec;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;

public interface MonumentData {
    Codec<MonumentData> CODEC = MonumentType.CODEC.dispatch(MonumentData::type, MonumentType::codec);

    @Nullable
    Monument create(MinecraftServer server);

    MonumentType type();
}
