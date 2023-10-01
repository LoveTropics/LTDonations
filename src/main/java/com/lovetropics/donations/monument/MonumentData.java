package com.lovetropics.donations.monument;

import com.lovetropics.donations.DonationGroup;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;

public record MonumentData(GlobalPos pos, DonationGroup donationGroup, boolean announce) {
    public static final Codec<MonumentData> CODEC = RecordCodecBuilder.create(i -> i.group(
            GlobalPos.CODEC.fieldOf("pos").forGetter(MonumentData::pos),
            DonationGroup.CODEC.fieldOf("donation_group").forGetter(MonumentData::donationGroup),
            Codec.BOOL.optionalFieldOf("announce", false).forGetter(MonumentData::announce)
    ).apply(i, MonumentData::new));
}
