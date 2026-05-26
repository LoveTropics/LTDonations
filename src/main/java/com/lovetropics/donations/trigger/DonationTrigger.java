package com.lovetropics.donations.trigger;

import com.lovetropics.donations.DonationListener;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ExtraCodecs;

import java.util.List;

public record DonationTrigger(
        List<Entry> entries
) implements DonationListener {
    public static final Codec<DonationTrigger> CODEC = ExtraCodecs.compactListCodec(Entry.CODEC).xmap(DonationTrigger::new, DonationTrigger::entries);

    @Override
    public void handleDonation(MinecraftServer server, Details details) {
        for (Entry entry : entries) {
            if (entry.predicate.test(details)) {
                entry.action.execute(server, details);
                return;
            }
        }
    }

    public record Entry(
            DonationPredicate predicate,
            DonationAction action
    ) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                DonationPredicate.CODEC.fieldOf("predicate").forGetter(Entry::predicate),
                DonationAction.CODEC.fieldOf("action").forGetter(Entry::action)
        ).apply(i, Entry::new));
    }
}
