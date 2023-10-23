package com.lovetropics.donations.monument;

import com.google.common.collect.ImmutableList;
import com.lovetropics.donations.DiscordIntegration;
import com.lovetropics.donations.DonationGroup;
import com.lovetropics.donations.DonationTotals;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import org.slf4j.Logger;

import java.util.*;

public class PillarMonument implements Monument {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int GLASS_SEARCH_HORIZ = 18;
    private static final int GLASS_SEARCH_VERT = 42;

    private static final int DRAIN_INTERVAL = SharedConstants.TICKS_PER_SECOND / 2;

    private static final int DOLLARS_PER_LAYER = 1000;
    private static final int LAYERS_PER_COLOR = 5;

    private static final List<BlockPos> LAYER_POSITIONS;

    static {
        final List<BlockPos> allPositions = new ArrayList<>(ImmutableList.<BlockPos>builder()
                .addAll(range(-3, -1, -3, 1))
                .addAll(range(-2, -2, -2, 2))
                .addAll(range(-1, -3, -1, 3))
                .addAll(range(0, -3, 0, 3))
                .addAll(range(1, -3, 1, 3))
                .addAll(range(2, -2, 2, 2))
                .addAll(range(3, -1, 3, 1))
                .build());

        allPositions.sort(Comparator.<BlockPos>comparingDouble(p -> Math.max(Math.abs(p.getX()), Math.abs(p.getZ()))) // Sort by "shell"
                // Then by angle
                .thenComparingDouble(p -> {
                    if (p.equals(BlockPos.ZERO)) return -1;

                    final Vec3 v1 = Vec3.atLowerCornerOf(p);
                    final Vec3 v2 = new Vec3(-1.0, 0.0, 0.0);

                    final Vec3 cross = v1.cross(v2);
                    final double dot = v1.dot(v2);

                    double angle = Math.atan2(cross.length(), dot);

                    final double test = new Vec3(0.0, 1.0, .0).dot(cross);
                    if (test < 0.0) angle = -angle + (Math.PI * 2);
                    return angle;
                }));

        LAYER_POSITIONS = List.copyOf(allPositions);
    }

    private static List<BlockPos> range(final int minX, final int minZ, final int maxX, final int maxZ) {
        return BlockPos.betweenClosedStream(minX, 0, minZ, maxX, 0, maxZ).map(BlockPos::immutable).toList();
    }

    private static final Block[][] BLOCKS = new Block[][]{
            {Blocks.RED_CONCRETE, Blocks.RED_STAINED_GLASS},
            {Blocks.ORANGE_CONCRETE, Blocks.ORANGE_STAINED_GLASS},
            {Blocks.YELLOW_CONCRETE, Blocks.YELLOW_STAINED_GLASS},
            {Blocks.LIME_CONCRETE, Blocks.LIME_STAINED_GLASS},
            {Blocks.LIGHT_BLUE_CONCRETE, Blocks.LIGHT_BLUE_STAINED_GLASS},
            {Blocks.BLUE_CONCRETE, Blocks.BLUE_STAINED_GLASS},
            {Blocks.PURPLE_CONCRETE, Blocks.PURPLE_STAINED_GLASS},
    };
    private static final ChatFormatting[] COLORS = new ChatFormatting[]{
            ChatFormatting.RED,
            ChatFormatting.GOLD,
            ChatFormatting.YELLOW,
            ChatFormatting.GREEN,
            ChatFormatting.AQUA,
            ChatFormatting.BLUE,
            ChatFormatting.DARK_PURPLE,
    };

    private final ServerLevel level;
    private final BlockPos origin;
    private final List<BlockPos> nearbyGlass;
    private final Data data;

    private final Deque<QueuedBlock> blockQueue = new ArrayDeque<>();

    private int step = 0;

    private PillarMonument(final ServerLevel level, final BlockPos origin, final List<BlockPos> nearbyGlass, final Data data) {
        this.level = level;
        this.origin = origin;
        this.nearbyGlass = nearbyGlass;
        this.data = data;
    }

    private static List<BlockPos> scanForGlass(final ServerLevel level, final BlockPos origin) {
        final List<BlockPos> nearbyGlass = new ArrayList<>();
        final BlockPos first = origin.below(1).east(GLASS_SEARCH_HORIZ).north(GLASS_SEARCH_HORIZ);
        final BlockPos second = origin.above(GLASS_SEARCH_VERT).west(GLASS_SEARCH_HORIZ).south(GLASS_SEARCH_HORIZ);
        final Map<ChunkPos, LevelChunk> chunkCache = new HashMap<>();
        BlockPos.betweenClosedStream(first, second).forEach(pos -> {
            final LevelChunk chunk = chunkCache.computeIfAbsent(new ChunkPos(pos), p -> level.getChunk(p.x, p.z));
            if (chunk.getBlockState(pos).is(Tags.Blocks.STAINED_GLASS)) {
                nearbyGlass.add(pos.immutable());
            }
        });
        LOGGER.debug("Found {} glass blocks nearby", nearbyGlass.size());
        return nearbyGlass;
    }

    private void queueBlock(final int layer, final int step) {
        final BlockPos pos = origin.above(layer).offset(LAYER_POSITIONS.get(step));
        final int color = (layer / LAYERS_PER_COLOR) % COLORS.length;
        final BlockState state = BLOCKS[color][0].defaultBlockState();
        blockQueue.offer(new QueuedBlock(pos, state, color, layer, step));
    }

    private void updateTotal(final DonationTotals totals) {
        final int blocksPerLayer = LAYER_POSITIONS.size();
        final double dollarsPerBlock = (double) DOLLARS_PER_LAYER / blocksPerLayer;
        final int newStep = Mth.floor(totals.get(data.donationGroup()) / dollarsPerBlock);
        LOGGER.debug("Step Increase: {} -> {}", step, newStep);
        while (newStep > step) {
            queueBlock(step / blocksPerLayer, step % blocksPerLayer);
            step++;
        }
    }

    @Override
    public void tick(final MinecraftServer server, final DonationTotals totals) {
        updateTotal(totals);
        if (level.getGameTime() % DRAIN_INTERVAL == 0) {
            drain(1);
        }
    }

    @Override
    public void sync(final DonationTotals totals) {
        updateTotal(totals);
        drain(-1);
    }

    private void drain(int amt) {
        if (amt == 0 || blockQueue.isEmpty()) {
            return;
        }
        int updateGlassToColor = -1;
        while ((amt < 0 || amt-- > 0) && !blockQueue.isEmpty()) {
            final QueuedBlock queued = blockQueue.poll();
            final BlockPos pos = queued.pos;
            if (level.setBlock(pos, queued.state, 0)) {
                level.getChunkSource().blockChanged(pos);
                if (queued.step == LAYER_POSITIONS.size() - 1) {
                    announceLayer(COLORS[queued.color], queued.layer);
                } else if (queued.step == 0 && queued.layer % LAYERS_PER_COLOR == 0) {
                    // A new layer has begun, update the glass
                    updateGlassToColor = queued.color;
                }
                if (amt >= 0) { // Not an infinite drain
                    // Throw some particles around
                    final RandomSource rand = level.getRandom();
                    final Vec3 center = Vec3.atLowerCornerOf(pos).add(0.5, 0.5, 0.5);
                    for (int i = 0; i < 20; i++) {
                        final Direction dir = rand.nextInt(3) != 0 ? Direction.UP : Direction.from2DDataValue(rand.nextInt(4));
                        final Vec3 spawnPos = center.add(Vec3.atLowerCornerOf(dir.getNormal()).scale(0.6f))
                                .add((rand.nextDouble() - 0.5) * (1 - Math.abs(dir.getStepX())),
                                        (rand.nextDouble() - 0.5) * (1 - Math.abs(dir.getStepY())),
                                        (rand.nextDouble() - 0.5) * (1 - Math.abs(dir.getStepZ())));
                        final Vec3 speed = spawnPos.subtract(center);
                        level.sendParticles(ParticleTypes.END_ROD, spawnPos.x, spawnPos.y, spawnPos.z, 0, speed.x, speed.y, speed.z, 0.075);
                    }
                }
            }
            for (final Direction dir : Direction.values()) {
                if (dir != Direction.DOWN) {
                    final BlockPos airPos = pos.relative(dir);
                    final BlockState atPos = level.getBlockState(airPos);
                    if ((atPos.isAir() || atPos.getBlock() == Blocks.WATER) && level.setBlockAndUpdate(airPos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, Block.UPDATE_ALL_IMMEDIATE | Block.UPDATE_INVISIBLE))) {
                        level.getChunkSource().blockChanged(airPos);
                    }
                }
            }
        }
        // Run this at the end so that it doesn't go multiple times in the initial drain
        if (updateGlassToColor >= 0) {
            for (final BlockPos glass : nearbyGlass) {
                if (level.setBlock(glass, BLOCKS[updateGlassToColor][1].defaultBlockState(), 0)) {
                    level.getChunkSource().blockChanged(glass);
                }
            }
        }
    }

    private void announceLayer(final ChatFormatting color, final int layer) {
        if (!data.announce()) {
            return;
        }
        final Component message = Component.literal("The Monument")
                .withStyle(ChatFormatting.BOLD, color)
                .append(Component.literal(" has grown to ")
                        .setStyle(Style.EMPTY.withBold(false).withColor(ChatFormatting.WHITE))
                        .append(Component.literal("LEVEL " + (layer + 1) + "!")
                                .setStyle(Style.EMPTY.withUnderlined(true))));

        level.players().forEach(p -> p.displayClientMessage(message, false));
        DiscordIntegration.send(message.getString());
    }

    @Override
    public MonumentData toData() {
        return data;
    }

    private record QueuedBlock(BlockPos pos, BlockState state, int color, int layer, int step) {
    }

    public record Data(GlobalPos pos, DonationGroup donationGroup, boolean announce) implements MonumentData {
        public static final MapCodec<Data> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                GlobalPos.CODEC.fieldOf("pos").forGetter(Data::pos),
                DonationGroup.CODEC.fieldOf("donation_group").forGetter(Data::donationGroup),
                Codec.BOOL.optionalFieldOf("announce", false).forGetter(Data::announce)
        ).apply(i, Data::new));

        @Override
        public Monument create(final MinecraftServer server) {
            ServerLevel level = server.getLevel(pos.dimension());
            if (level == null) {
                LOGGER.warn("Could not find dimension : " + pos.dimension().location());
                level = server.overworld();
            }
            final BlockPos origin = pos.pos();
            return new PillarMonument(level, origin, scanForGlass(level, origin), this);
        }

        @Override
        public MonumentType type() {
            return MonumentType.PILLAR;
        }
    }
}
