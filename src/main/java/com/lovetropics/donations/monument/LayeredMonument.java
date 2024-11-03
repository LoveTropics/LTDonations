package com.lovetropics.donations.monument;

import com.lovetropics.donations.DiscordIntegration;
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

public class LayeredMonument implements Monument {
    private static final int BLOCKS_PER_COLOR = 4;

    private static final int PLACE_INTERVAL = SharedConstants.TICKS_PER_SECOND / 2;

    private final ServerLevel level;
    private final LongList[] blocksByLayer;
    private final WallMonumentData data;

    private Cursor cursor = Cursor.START;
    private Cursor targetCursor = Cursor.START;

    protected LayeredMonument(ServerLevel level, LongList[] blocksByLayer, WallMonumentData data) {
        this.level = level;
        this.data = data;
        this.blocksByLayer = blocksByLayer;
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
        for (LongList layer : blocksByLayer) {
            layer.forEach(l -> {
                BlockPos pos = mutablePos.set(l);
                level.setBlock(pos, WallMonumentData.BACKGROUND_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
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
        LongList blocks = getBlocksByLayer(cursor.layer);
        BlockPos pos = BlockPos.of(blocks.getLong(cursor.blockInLayer));
        level.setBlock(pos, getBlockStateForLayer(cursor.layer), Block.UPDATE_ALL);
        if (effects) {
            MonumentEffects.spawnParticles(level, pos);
        }
    }

    private void announceLayer(int layer) {
        if (data.style() != WallMonumentData.MonumentStyle.NORMAL) {
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

    private LongList getBlocksByLayer(int layer) {
        return blocksByLayer[layer % blocksByLayer.length];
    }

    private BlockState getBlockStateForLayer(int layer) {
        int type = layer / blocksByLayer.length;
        Block[][] palette = data.style().palette();
        Block[] paletteForType = palette[type % palette.length];
        return paletteForType[(layer / BLOCKS_PER_COLOR) % paletteForType.length].defaultBlockState();
    }

    private Cursor computeCursor(DonationState totals) {
        double totalLayers = totals.getAmount(data.group()) / data.style().dollarsPerLayer();
        int currentLayer = Mth.floor(totalLayers);
        double layerProgress = totalLayers - currentLayer;

        LongList layer = getBlocksByLayer(currentLayer);
        int blockInLayer = Mth.floor(layerProgress * layer.size());
        return new Cursor(currentLayer, blockInLayer);
    }

    private Cursor step(Cursor cursor) {
        LongList blocks = getBlocksByLayer(cursor.layer);
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
