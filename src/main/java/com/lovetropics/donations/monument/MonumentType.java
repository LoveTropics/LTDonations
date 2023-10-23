package com.lovetropics.donations.monument;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.StringRepresentable;

public enum MonumentType implements StringRepresentable {
    PILLAR("pillar", PillarMonument.Data.CODEC),
    WALL("wall", WallMonument.Data.CODEC),
    ;

    public static final Codec<MonumentType> CODEC = StringRepresentable.fromEnum(MonumentType::values);

    private final String name;
    private final MapCodec<? extends MonumentData> codec;

    MonumentType(final String name, final MapCodec<? extends MonumentData> codec) {
        this.name = name;
        this.codec = codec;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public MapCodec<? extends MonumentData> codec() {
        return codec;
    }
}
