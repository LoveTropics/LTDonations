package com.lovetropics.donations.backend.ltts.json;

import com.lovetropics.lib.codec.MoreCodecs;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

import java.time.Instant;

public record FullDonationState(
        double total,
        int teamCentsCount,
        int teamNoCentsCount,
        int teamNiceCount,
        double teamCentsTotal,
        double teamNoCentsTotal,
        long teamCentsLeadTime,
        long teamNoCentsLeadTime,
        Instant teamLeadChangeTimestamp,
        String latestTeam
) {
    // TODO: I don't know why we send it like this
    private static final Codec<Double> STRINGIFIED_DOUBLE = Codec.either(Codec.STRING, Codec.DOUBLE).comapFlatMap(
            either -> either.map(s -> {
                try {
                    return DataResult.success(Double.parseDouble(s));
                } catch (final NumberFormatException e) {
                    return DataResult.error(() -> "Not a valid number: " + s);
                }
            }, DataResult::success),
            Either::right
    );

    public static final MapCodec<FullDonationState> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("total", 0.0).forGetter(FullDonationState::total),
            Codec.INT.optionalFieldOf("team_cents_count", 0).forGetter(FullDonationState::teamCentsCount),
            Codec.INT.optionalFieldOf("team_no_cents_count", 0).forGetter(FullDonationState::teamNoCentsCount),
            Codec.INT.optionalFieldOf("team_nice_count", 0).forGetter(FullDonationState::teamNiceCount),
            STRINGIFIED_DOUBLE.optionalFieldOf("team_cents_total", 0.0).forGetter(FullDonationState::teamCentsTotal),
            STRINGIFIED_DOUBLE.optionalFieldOf("team_no_cents_total", 0.0).forGetter(FullDonationState::teamNoCentsTotal),
            Codec.LONG.optionalFieldOf("team_cents_lead_time", 0L).forGetter(FullDonationState::teamCentsLeadTime),
            Codec.LONG.optionalFieldOf("team_no_cents_lead_time", 0L).forGetter(FullDonationState::teamNoCentsLeadTime),
            ExtraCodecs.INSTANT_ISO8601.optionalFieldOf("team_lead_change_timestamp", Instant.EPOCH).forGetter(FullDonationState::teamLeadChangeTimestamp),
            Codec.STRING.optionalFieldOf("latest_team", "").forGetter(FullDonationState::latestTeam)
    ).apply(i, FullDonationState::new));

    public static final Codec<FullDonationState> CODEC = MAP_CODEC.codec();

    public static FullDonationState forTotal(double total) {
        return new FullDonationState(
                total,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                Instant.EPOCH,
                ""
        );
    }
}
