package com.lovetropics.donations.monument;

import com.lovetropics.donations.DonationState;
import com.lovetropics.donations.DonationStateListener;
import com.lovetropics.donations.LTDonations;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MonumentManager extends SavedData implements DonationStateListener {
    private static final String STORAGE_ID = LTDonations.MODID + "_monuments";
    private static final Factory<MonumentManager> FACTORY = new Factory<>(MonumentManager::new, MonumentManager::load);

    private static final Codec<Map<String, MonumentData>> CODEC = Codec.unboundedMap(Codec.STRING, MonumentData.CODEC);

    private final Map<String, Monument> monuments = new Object2ObjectOpenHashMap<>();
    private final Map<String, MonumentData> pendingMonuments = new Object2ObjectOpenHashMap<>();

    private DonationState state = DonationState.ZERO;

    public static MonumentManager get(final MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, STORAGE_ID);
    }

    private static MonumentManager load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final MonumentManager manager = new MonumentManager();
        CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("monuments")).result().ifPresent(manager.pendingMonuments::putAll);
        return manager;
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final Map<String, MonumentData> data = monuments.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toData()));
        data.putAll(pendingMonuments);
        tag.put("monuments", CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), data).getOrThrow());
        return tag;
    }

    @Override
    public void handleState(final MinecraftServer server, final DonationState state, final boolean initial) {
        update(state, initial);
    }

    public void update(final DonationState state, final boolean fast) {
        setDirty();
        this.state = state;
        if (fast) {
            for (final Monument monument : monuments.values()) {
                monument.sync(state);
            }
        }
    }

    public void tick(final MinecraftServer server) {
        if (!pendingMonuments.isEmpty()) {
            pendingMonuments.forEach((id, data) -> {
                final Monument monument = data.create(server);
                if (monument != null) {
                    monument.sync(state);
                    monuments.put(id, monument);
                }
            });
            pendingMonuments.clear();
        }

        monuments.values().forEach(monument -> monument.tick(server, state));
    }

    public Stream<String> ids() {
        return monuments.keySet().stream();
    }

    public boolean add(final String id, final MonumentData data) {
        if (monuments.containsKey(id)) {
            return false;
        }
        if (pendingMonuments.putIfAbsent(id, data) == null) {
            setDirty();
            return true;
        }
        return false;
    }

    public boolean update(final String id, final MonumentData data) {
        if (!monuments.containsKey(id) || !pendingMonuments.containsKey(id)) {
            return false;
        }
        pendingMonuments.put(id, data);
        setDirty();
        return true;
    }

    public boolean remove(final String id) {
        if (monuments.remove(id) != null | pendingMonuments.remove(id) != null) {
            setDirty();
            return true;
        }
        return false;
    }
}
