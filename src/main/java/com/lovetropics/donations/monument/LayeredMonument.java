package com.lovetropics.donations.monument;

import com.lovetropics.donations.DiscordIntegration;
import com.lovetropics.donations.DonationGroup;
import com.lovetropics.donations.DonationState;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class LayeredMonument implements Monument {
    private static final int PLACE_INTERVAL = SharedConstants.TICKS_PER_SECOND / 2;

    private final ServerLevel level;
    private final List<Layer> layers;
    private final DonationGroup group;
    private final double dollarsPerLayer;
    private final boolean announceLayer;

    private final MonumentData data;

    private Cursor cursor = Cursor.START;
    private Cursor targetCursor = Cursor.START;

    protected LayeredMonument(ServerLevel level, List<Layer> layers, DonationGroup group, double dollarsPerLayer, boolean announceLayer, MonumentData data) {
        this.level = level;
		this.group = group;
		this.dollarsPerLayer = dollarsPerLayer;
		this.announceLayer = announceLayer;
        this.layers = layers;
        this.data = data;
    }

    @Override
    public void tick(MinecraftServer server, DonationState state) {
        targetCursor = computeCursor(state);
        if (server.getTickCount() % PLACE_INTERVAL == 0) {
            tryBuild(targetCursor, true, 1);
        }
    }

    @Override
    public void sync(DonationState state) {
        clear();
        targetCursor = computeCursor(state);
        tryBuild(targetCursor, false, Integer.MAX_VALUE);
    }

    private void clear() {
        cursor = Cursor.START;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (Layer layer : layers) {
            layer.blocks.forEach(l -> {
                BlockPos pos = mutablePos.set(l);
                level.setBlock(pos, layer.emptyState(), Block.UPDATE_ALL);
            });
        }
    }

    private void tryBuild(Cursor target, boolean effects, int budget) {
        while (cursor.compareTo(target) < 0) {
            place(cursor, effects);

            Cursor newCursor = step(cursor);
            if (newCursor.layer != cursor.layer) {
                announceLayer(cursor.layer);
            }
            cursor = newCursor;

            if (--budget == 0) {
                break;
            }
        }
    }

    private void place(Cursor cursor, boolean effects) {
        Layer layer = getLayer(cursor.layer);
		BlockPos pos = BlockPos.of(layer.blocks.getLong(cursor.blockInLayer));
        level.setBlock(pos, getBlockStateForLayer(cursor, layer), Block.UPDATE_ALL);
        if (effects) {
            MonumentEffects.spawnParticles(level, pos);
        }
    }

    private void announceLayer(int layer) {
        if (!announceLayer) {
            return;
        }
        Component message = Component.literal("The Monument")
                .withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD)
                .append(Component.literal(" has grown to ")
                        .setStyle(Style.EMPTY.withBold(false).withColor(ChatFormatting.WHITE))
                        .append(Component.literal("LEVEL " + (layer + 1) + "!")
                                .setStyle(Style.EMPTY.withUnderlined(true))));

        level.players().forEach(p -> p.displayClientMessage(message, false));
        DiscordIntegration.send(message.getString());
    }

    private BlockState getBlockStateForLayer(Cursor cursor, Layer layer) {
        int type = cursor.layer / layers.size();
        return layer.palette.get(type % layer.palette.size());
    }

    private Layer getLayer(int layer) {
        return layers.get(layer % layers.size());
    }

    private Cursor computeCursor(DonationState totals) {
        double totalLayers = totals.getAmount(group) / dollarsPerLayer;
        int currentLayer = Mth.floor(totalLayers);
        double layerProgress = totalLayers - currentLayer;

        LongList layer = getLayer(currentLayer).blocks();
        int blockInLayer = Mth.floor(layerProgress * layer.size());
        return new Cursor(currentLayer, blockInLayer);
    }

    private Cursor step(Cursor cursor) {
        LongList blocks = getLayer(cursor.layer).blocks();
        if (cursor.blockInLayer + 1 < blocks.size()) {
            return new Cursor(cursor.layer, cursor.blockInLayer + 1);
        } else {
            return new Cursor(cursor.layer + 1, 0);
        }
    }

    @Override
    public MonumentData toData() {
        return data;
    }

    public record Layer(LongList blocks, BlockState emptyState, List<BlockState> palette) {
    }

    private record Cursor(int layer, int blockInLayer) implements Comparable<Cursor> {
        public static final Cursor START = new Cursor(0, 0);

        @Override
        public int compareTo(LayeredMonument.Cursor other) {
            int layer = Integer.compare(this.layer, other.layer);
            if (layer != 0) {
                return layer;
            }
            return Integer.compare(blockInLayer, other.blockInLayer);
        }
    }
}
