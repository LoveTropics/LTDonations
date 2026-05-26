package com.lovetropics.donations.monument;

import com.lovetropics.donations.DonationState;
import com.lovetropics.donations.DonationStateListener;
import com.lovetropics.donations.LTDonations;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MonumentManager extends SavedData implements DonationStateListener {
    private static final Identifier STORAGE_ID = LTDonations.location("monuments");

    private static final Codec<MonumentManager> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, MonumentData.CODEC).fieldOf("monuments").forGetter(MonumentManager::packData)
    ).apply(i, MonumentManager::new));

    private static final SavedDataType<MonumentManager> TYPE = new SavedDataType<>(STORAGE_ID, MonumentManager::new, CODEC);

    private final Map<String, Monument> monuments = new Object2ObjectOpenHashMap<>();
    private final Map<String, MonumentData> pendingMonuments = new Object2ObjectOpenHashMap<>();

    private DonationState state = DonationState.ZERO;

    public MonumentManager() {
    }

    private MonumentManager(final Map<String, MonumentData> monuments) {
        pendingMonuments.putAll(monuments);
    }

    public static MonumentManager get(final MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    private Map<String, MonumentData> packData() {
        final Map<String, MonumentData> data = monuments.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toData()));
        data.putAll(pendingMonuments);
        return data;
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
