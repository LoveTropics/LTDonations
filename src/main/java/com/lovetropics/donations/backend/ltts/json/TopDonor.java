package com.lovetropics.donations.backend.ltts.json;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record TopDonor(UUID uuid, Optional<String> minecraftName, List<String> displayNames, double total) {
    public static final Codec<TopDonor> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.STRING_CODEC.fieldOf("uuid").forGetter(TopDonor::uuid),
            Codec.STRING.optionalFieldOf("minecraft_name").forGetter(TopDonor::minecraftName),
            Codec.STRING.listOf().optionalFieldOf("display_names", List.of()).forGetter(TopDonor::displayNames),
            Codec.DOUBLE.fieldOf("total").forGetter(TopDonor::total)
    ).apply(i, TopDonor::new));

    public boolean isAnonymous() {
        return uuid.equals(Util.NIL_UUID);
    }
}
