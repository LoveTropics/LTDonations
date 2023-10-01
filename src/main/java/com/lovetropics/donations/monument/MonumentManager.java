package com.lovetropics.donations.monument;

import com.lovetropics.donations.DonationGroup;
import com.lovetropics.donations.DonationListener;
import com.lovetropics.donations.DonationTotals;
import com.lovetropics.donations.LTDonations;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MonumentManager extends SavedData implements DonationListener {
    private static final String STORAGE_ID = LTDonations.MODID + "_monuments";

    private static final Codec<Map<String, MonumentData>> CODEC = Codec.unboundedMap(Codec.STRING, MonumentData.CODEC);

    private final Map<String, Monument> monuments = new Object2ObjectOpenHashMap<>();
    private final Map<String, MonumentData> pendingMonuments = new Object2ObjectOpenHashMap<>();

    private DonationTotals totals = DonationTotals.ZERO;

    public static MonumentManager get(final MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(MonumentManager::load, MonumentManager::new, STORAGE_ID);
    }

    private static MonumentManager load(final CompoundTag tag) {
        final MonumentManager manager = new MonumentManager();
        CODEC.parse(NbtOps.INSTANCE, tag.get("monuments")).result().ifPresent(manager.pendingMonuments::putAll);
        return manager;
    }

    @Override
    public CompoundTag save(final CompoundTag tag) {
        final Map<String, MonumentData> data = monuments.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toData()));
        data.putAll(pendingMonuments);
        tag.put("monuments", Util.getOrThrow(CODEC.encodeStart(NbtOps.INSTANCE, data), IllegalStateException::new));
        return tag;
    }

    @Override
    public void handleDonation(final MinecraftServer server, final String name, final double amount, final DonationTotals totals) {
        update(totals, false);
    }

    public void update(final DonationTotals totals, final boolean fast) {
        setDirty();
        this.totals = totals;
        for (final Monument monument : monuments.values()) {
            monument.updateTotal(totals);
            if (fast) {
                monument.drainAll();
            }
        }
    }

    public void tick(final MinecraftServer server) {
        if (!pendingMonuments.isEmpty()) {
            pendingMonuments.forEach((id, data) -> {
                final Monument monument = Monument.create(server, data);
                monument.updateTotal(totals);
                monument.drainAll();
                monuments.put(id, monument);
            });
            pendingMonuments.clear();
        }

        monuments.values().forEach(Monument::tick);
    }

    public Stream<String> ids() {
        return monuments.keySet().stream();
    }

    public boolean add(final String id, final ResourceKey<Level> dimension, final BlockPos pos, final DonationGroup group, final boolean announce) {
        if (monuments.containsKey(id)) {
            return false;
        }
        final MonumentData data = new MonumentData(GlobalPos.of(dimension, pos), group, announce);
        if (pendingMonuments.putIfAbsent(id, data) == null) {
            setDirty();
            return true;
        }
        return false;
    }

    public boolean remove(final String id) {
        if (monuments.remove(id) != null | pendingMonuments.remove(id) != null) {
            setDirty();
            return true;
        }
        return false;
    }
}
