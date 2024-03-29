package com.lovetropics.donations.monument;

import com.lovetropics.donations.DiscordIntegration;
import com.lovetropics.donations.DonationGroup;
import com.lovetropics.donations.DonationState;
import com.lovetropics.lib.BlockBox;
import com.mojang.logging.LogUtils;
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
import net.minecraft.util.StringRepresentable;
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

    private static final int BLOCKS_PER_COLOR = 4;

    private static final int PLACE_INTERVAL = SharedConstants.TICKS_PER_SECOND / 2;

    private static final Block BACKGROUND_BLOCK = Blocks.SCULK;

    private final ServerLevel level;
    private final LongList[] blocksByLayer;
    private final Data data;

    private Cursor cursor = Cursor.START;
    private Cursor targetCursor = Cursor.START;

    private WallMonument(final ServerLevel level, final LongList[] blocksByLayer, final Data data) {
        this.level = level;
        this.data = data;
        this.blocksByLayer = blocksByLayer;
    }

    @Nullable
    private static LongList[] scanBlocksByLayer(final ServerLevel level, final BlockBox box, final MonumentStyle style) {
        final int minY = box.min().getY();
        final int maxY = box.max().getY();
        final LongList[] result = new LongList[maxY - minY + 1];
        for (int i = 0; i < result.length; i++) {
            result[i] = new LongArrayList();
        }
        for (final BlockPos pos : box) {
            if (isReplaceable(style, level.getBlockState(pos))) {
                final int layer = pos.getY() - minY;
                result[layer].add(pos.asLong());
            }
        }
        final BlockPos centerBlock = box.centerBlock();
        for (int i = 0; i < result.length; i++) {
            final LongList layer = result[i];
            layer.sort((a, b) -> Integer.compare(
                    BlockPos.of(a).distManhattan(centerBlock),
                    BlockPos.of(b).distManhattan(centerBlock))
            );
            if (layer.isEmpty()) {
                LOGGER.error("No replaceable blocks in layer: {}", i + minY);
                return null;
            }
        }
        return result;
    }

    private static boolean isReplaceable(final MonumentStyle style, final BlockState state) {
        return style.replaceableBlocks.contains(state.getBlock());
    }

    @Override
    public void tick(final MinecraftServer server, final DonationState state) {
        targetCursor = computeCursor(state);
        if (server.getTickCount() % PLACE_INTERVAL == 0) {
            tryBuild(targetCursor, true, 1);
        }
    }

    @Override
    public void sync(final DonationState state) {
        clear();
        targetCursor = computeCursor(state);
        tryBuild(targetCursor, false, Integer.MAX_VALUE);
    }

    private void clear() {
        cursor = Cursor.START;
        final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (final LongList layer : blocksByLayer) {
            layer.forEach(l -> {
                final BlockPos pos = mutablePos.set(l);
                level.setBlock(pos, BACKGROUND_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            });
        }
    }

    private void tryBuild(final Cursor target, final boolean effects, int budget) {
        while (cursor.compareTo(target) < 0) {
            place(cursor, effects);

            final Cursor newCursor = step(cursor);
            if (newCursor.layer != cursor.layer) {
                announceLayer(cursor.layer);
            }
            cursor = newCursor;

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
        if (data.style() != MonumentStyle.NORMAL) {
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
        final Block[][] palette = data.style().palette;
        final Block[] paletteForType = palette[type % palette.length];
        return paletteForType[(layer / BLOCKS_PER_COLOR) % paletteForType.length].defaultBlockState();
    }

    private Cursor computeCursor(final DonationState totals) {
        final double totalLayers = totals.getAmount(data.group()) / data.style().dollarsPerLayer;
        final int currentLayer = Mth.floor(totalLayers);
        final double layerProgress = totalLayers - currentLayer;

        final LongList layer = getBlocksByLayer(currentLayer);
        final int blockInLayer = Mth.floor(layerProgress * layer.size());
        return new Cursor(currentLayer, blockInLayer);
    }

    private Cursor step(final Cursor cursor) {
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

    public record Data(ResourceKey<Level> dimension, BlockBox box, DonationGroup group, MonumentStyle style) implements MonumentData {
        public static final MapCodec<Data> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Data::dimension),
                BlockBox.CODEC.fieldOf("box").forGetter(Data::box),
                DonationGroup.CODEC.fieldOf("group").forGetter(Data::group),
                MonumentStyle.CODEC.optionalFieldOf("style", MonumentStyle.NORMAL).forGetter(Data::style)
        ).apply(i, Data::new));

        @Override
        @Nullable
        public Monument create(final MinecraftServer server) {
            final ServerLevel level = server.getLevel(dimension);
            if (level != null) {
                final LongList[] blocksByLayer = scanBlocksByLayer(level, box, style);
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

    public enum MonumentStyle implements StringRepresentable {
        NORMAL("normal", 1000.0, new Block[][]{
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
        }),
        TEAM_CENTS("team_cents", 500.0, new Block[][]{
                {Blocks.BLUE_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE},
                {Blocks.BLUE_GLAZED_TERRACOTTA, Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA},
                {Blocks.DIAMOND_BLOCK, Blocks.LAPIS_BLOCK}
        }),
        TEAM_NO_CENTS("team_no_cents", 500.0, new Block[][]{
                {Blocks.RED_CONCRETE, Blocks.ORANGE_CONCRETE},
                {Blocks.RED_GLAZED_TERRACOTTA, Blocks.ORANGE_GLAZED_TERRACOTTA},
                {Blocks.REDSTONE_BLOCK, Blocks.COPPER_BLOCK}
        }),
        ;

        public static final EnumCodec<MonumentStyle> CODEC = StringRepresentable.fromEnum(MonumentStyle::values);

        private final String name;
        private final double dollarsPerLayer;
        private final Block[][] palette;
        private final Set<Block> replaceableBlocks;

        MonumentStyle(final String name, final double dollarsPerLayer, final Block[][] palette) {
            this.name = name;
            this.dollarsPerLayer = dollarsPerLayer;
            this.palette = palette;

            replaceableBlocks = Stream.concat(
                    Stream.of(BACKGROUND_BLOCK),
                    Arrays.stream(palette).flatMap(Arrays::stream)
            ).collect(Collectors.toSet());
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public static Stream<String> names() {
            return Arrays.stream(values()).map(MonumentStyle::getSerializedName);
        }
    }
}
