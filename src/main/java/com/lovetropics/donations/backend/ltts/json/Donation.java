package com.lovetropics.donations.backend.ltts.json;

import com.lovetropics.lib.codec.MoreCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record Donation(
        String name,
        UUID uuid,
        double amount,
        String comments,
        Instant paymentTime,
        boolean anonymous,
        FullDonationState fullState
) implements Comparable<Donation> {
    private static final Codec<Instant> TIME_CODEC = MoreCodecs.localDateTime(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")).xmap(
            localTime -> localTime.atOffset(ZoneOffset.UTC).toInstant(),
            instant -> instant.atOffset(ZoneOffset.UTC).toLocalDateTime()
    );

    public static final Codec<Donation> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("name", "").forGetter(Donation::name),
            UUIDUtil.AUTHLIB_CODEC.fieldOf("uuid").forGetter(Donation::uuid),
            Codec.DOUBLE.fieldOf("amount").forGetter(Donation::amount),
            Codec.STRING.optionalFieldOf("comments", "").forGetter(Donation::comments),
            TIME_CODEC.optionalFieldOf("payment_time", Instant.EPOCH).forGetter(Donation::paymentTime),
            Codec.BOOL.optionalFieldOf("anonymous", true).forGetter(Donation::anonymous),
            FullDonationState.MAP_CODEC.forGetter(Donation::fullState)
    ).apply(i, Donation::new));

    @Override
    public int compareTo(final Donation other) {
        return paymentTime.compareTo(other.paymentTime);
    }

    public String getNameShown() {
        return anonymous() ? "Anonymous" : name();
    }
}
