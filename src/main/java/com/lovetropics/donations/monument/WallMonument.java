package com.lovetropics.donations.monument;

import com.lovetropics.donations.DiscordIntegration;
import com.lovetropics.donations.DonationGroup;
import com.lovetropics.donations.DonationTotals;
import com.lovetropics.lib.BlockBox;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WallMonument implements Monument {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final double DOLLARS_PER_LAYER = 1000.0;
    private static final int BLOCKS_PER_COLOR = 4;

    private static final int PLACE_INTERVAL = SharedConstants.TICKS_PER_SECOND / 2;

    private static final Block BACKGROUND_BLOCK = Blocks.SCULK;
    private static final Block[][] PALETTE = new Block[][]{
            {
                    Blocks.RED_CONCRETE,
                    Blocks.ORANGE_CONCRETE,
                    Blocks.YELLOW_CONCRETE,
                    Blocks.LIME_CONCRETE,
                    Blocks.GREEN_CONCRETE,
                    Blocks.CYAN_CONCRETE,
                    Blocks.LIGHT_BLUE_CONCRETE,
                    Blocks.BLUE_CONCRETE,
                    Blocks.PURPLE_CONCRETE,
                    Blocks.MAGENTA_CONCRETE,
                    Blocks.PINK_CONCRETE,
            },
            {
                    Blocks.RED_GLAZED_TERRACOTTA,
                    Blocks.ORANGE_GLAZED_TERRACOTTA,
                    Blocks.YELLOW_GLAZED_TERRACOTTA,
                    Blocks.LIME_GLAZED_TERRACOTTA,
                    Blocks.GREEN_GLAZED_TERRACOTTA,
                    Blocks.CYAN_GLAZED_TERRACOTTA,
                    Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA,
                    Blocks.BLUE_GLAZED_TERRACOTTA,
                    Blocks.PURPLE_GLAZED_TERRACOTTA,
                    Blocks.MAGENTA_GLAZED_TERRACOTTA,
                    Blocks.PINK_GLAZED_TERRACOTTA,
            }
    };

    private static final Set<Block> REPLACEABLE_BLOCKS = Stream.concat(
            Stream.of(BACKGROUND_BLOCK),
            Arrays.stream(PALETTE).flatMap(Arrays::stream)
    ).collect(Collectors.toSet());

    private final ServerLevel level;
    private final LongList[] blocksByLayer;
    private final Data data;

    @Nullable
    private Cursor cursor;
    @Nullable
    private Cursor targetCursor;

    private WallMonument(final ServerLevel level, final LongList[] blocksByLayer, final Data data) {
        this.level = level;
        this.data = data;
        this.blocksByLayer = blocksByLayer;
    }

    @Nullable
    private static LongList[] scanBlocksByLayer(final ServerLevel level, final BlockBox box) {
        final int minY = box.min().getY();
        final int maxY = box.max().getY();
        final LongList[] result = new LongList[maxY - minY + 1];
        for (int i = 0; i < result.length; i++) {
            result[i] = new LongArrayList();
        }
        for (final BlockPos pos : box) {
            if (isReplaceable(level.getBlockState(pos))) {
                final int layer = pos.getY() - minY;
                result[layer].add(pos.asLong());
            }
        }
        for (int i = 0; i < result.length; i++) {
            final LongList layer = result[i];
            if (layer.isEmpty()) {
                LOGGER.error("No replaceable blocks in layer: {}", i + minY);
                return null;
            }
        }
        return result;
    }

    private static boolean isReplaceable(final BlockState state) {
        return REPLACEABLE_BLOCKS.contains(state.getBlock());
    }

    @Override
    public void tick(final MinecraftServer server, final DonationTotals totals) {
        targetCursor = computeCursor(totals);
        if (server.getTickCount() % PLACE_INTERVAL == 0) {
            tryBuild(targetCursor, true, 1);
        }
    }

    @Override
    public void sync(final DonationTotals totals) {
        targetCursor = computeCursor(totals);
        tryBuild(targetCursor, false, Integer.MAX_VALUE);
    }

    private void tryBuild(final Cursor target, final boolean effects, int budget) {
        int lastLayer = cursor != null ? cursor.layer : -1;

        while (cursor == null || cursor.compareTo(target) < 0) {
            cursor = step(cursor);
            place(cursor, effects);

            if (cursor.layer != lastLayer) {
                announceLayer(cursor.layer);
            }
            lastLayer = cursor.layer;

            if (--budget == 0) {
                break;
            }
        }
    }

    private void place(final Cursor cursor, final boolean effects) {
        final LongList blocks = getBlocksByLayer(cursor.layer);
        final BlockPos pos = BlockPos.of(blocks.getLong(cursor.blockInLayer));
        level.setBlock(pos, getBlockStateForLayer(cursor.layer), Block.UPDATE_ALL);
        if (effects) {
            MonumentEffects.spawnParticles(level, pos);
        }
    }

    private void announceLayer(final int layer) {
        if (!data.announce()) {
            return;
        }
        final Component message = Component.literal("The Monument")
                .withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD)
                .append(Component.literal(" has grown to ")
                        .setStyle(Style.EMPTY.withBold(false).withColor(ChatFormatting.WHITE))
                        .append(Component.literal("LEVEL " + (layer + 1) + "!")
                                .setStyle(Style.EMPTY.withUnderlined(true))));

        level.players().forEach(p -> p.displayClientMessage(message, false));
        DiscordIntegration.send(message.getString());
    }

    private LongList getBlocksByLayer(final int layer) {
        return blocksByLayer[layer % blocksByLayer.length];
    }

    private BlockState getBlockStateForLayer(final int layer) {
        final int type = layer / blocksByLayer.length;
        final Block[] palette = PALETTE[type % PALETTE.length];
        return palette[(layer / BLOCKS_PER_COLOR) % palette.length].defaultBlockState();
    }

    private Cursor computeCursor(final DonationTotals totals) {
        final double totalLayers = totals.get(data.group()) / DOLLARS_PER_LAYER;
        final int currentLayer = Mth.floor(totalLayers);
        final double layerProgress = totalLayers - currentLayer;

        final LongList layer = getBlocksByLayer(currentLayer);
        final int blockInLayer = Mth.floor(layerProgress * layer.size());
        return new Cursor(currentLayer, blockInLayer);
    }

    private Cursor step(@Nullable final Cursor cursor) {
        if (cursor == null) {
            return Cursor.START;
        }
        final LongList blocks = getBlocksByLayer(cursor.layer);
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
        public int compareTo(final WallMonument.Cursor other) {
            final int layer = Integer.compare(this.layer, other.layer);
            if (layer != 0) {
                return layer;
            }
            return Integer.compare(blockInLayer, other.blockInLayer);
        }
    }

    public record Data(ResourceKey<Level> dimension, BlockBox box, DonationGroup group, boolean announce) implements MonumentData {
        public static final MapCodec<Data> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Data::dimension),
                BlockBox.CODEC.fieldOf("box").forGetter(Data::box),
                DonationGroup.CODEC.fieldOf("group").forGetter(Data::group),
                Codec.BOOL.fieldOf("announce").forGetter(Data::announce)
        ).apply(i, Data::new));

        @Override
        @Nullable
        public Monument create(final MinecraftServer server) {
            final ServerLevel level = server.getLevel(dimension);
            if (level != null) {
                final LongList[] blocksByLayer = scanBlocksByLayer(level, box);
                if (blocksByLayer != null) {
                    return new WallMonument(level, blocksByLayer, this);
                }
            }
            return null;
        }

        @Override
        public MonumentType type() {
            return MonumentType.WALL;
        }
    }
}
