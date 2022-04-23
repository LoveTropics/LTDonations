package com.lovetropics.donations.monument;

import com.google.common.collect.ImmutableList;
import com.lovetropics.donations.DonationBlock;
import com.lovetropics.donations.DonationConfigs;
import com.mojang.math.Vector3f;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class MonumentManager {

	private static class QueuedBlock {
		final BlockPos pos;
		final BlockState state;
		final int color;
		final int layer;
		final int step;

		public QueuedBlock(BlockPos pos, BlockState state, int color, int layer, int step) {
			this.pos = pos;
			this.state = state;
			this.color = color;
			this.layer = layer;
			this.step = step;
		}
	}

	private static final Logger LOGGER = LogManager.getLogger();

	private static final int DOLLARS_PER_LAYER = 1000;
	private static final int LAYERS_PER_COLOR = 5;
	private static final int GLASS_SEARCH_HORIZ = 18;
	private static final int GLASS_SEARCH_VERT = 42;

	private static final List<BlockPos> LAYER_POSITIONS;
	static {
		List<BlockPos> allPositions = new ArrayList<>(ImmutableList.<BlockPos>builder()
			.addAll(range(-3, -1, -3, 1))
			.addAll(range(-2, -2, -2, 2))
			.addAll(range(-1, -3, -1, 3))
			.addAll(range( 0, -3,  0, 3))
			.addAll(range( 1, -3,  1, 3))
			.addAll(range( 2, -2,  2, 2))
			.addAll(range( 3, -1,  3, 1))
			.build());

		allPositions.sort(Comparator.<BlockPos>comparingDouble(p -> Math.max(Math.abs(p.getX()), Math.abs(p.getZ()))) // Sort by "shell"
				// Then by angle
				.thenComparingDouble(p -> {
					if (p.equals(BlockPos.ZERO)) return -1;

					Vec3 v1 = Vec3.atLowerCornerOf(p);
					Vec3 v2 = new Vec3(Vector3f.XN);

					Vec3 cross = v1.cross(v2);
					double dot = v1.dot(v2);

					double angle = Math.atan2(cross.length(), dot);

					double test = new Vec3(Vector3f.YP).dot(cross);
					if (test < 0.0) angle = -angle + (Math.PI * 2);
					return angle;
				}));

		LAYER_POSITIONS = ImmutableList.copyOf(allPositions);
	}

	private static List<BlockPos> range(int minX, int minZ, int maxX, int maxZ) {
		return BlockPos.betweenClosedStream(minX, 0, minZ, maxX, 0, maxZ).map(BlockPos::immutable).collect(Collectors.toList());
	}

	private static final Block[][] BLOCKS = new Block[][] {
			{ Blocks.RED_CONCRETE, Blocks.RED_STAINED_GLASS },
			{ Blocks.ORANGE_CONCRETE, Blocks.ORANGE_STAINED_GLASS },
			{ Blocks.YELLOW_CONCRETE, Blocks.YELLOW_STAINED_GLASS },
			{ Blocks.LIME_CONCRETE, Blocks.LIME_STAINED_GLASS },
			{ Blocks.LIGHT_BLUE_CONCRETE, Blocks.LIGHT_BLUE_STAINED_GLASS },
			{ Blocks.BLUE_CONCRETE, Blocks.BLUE_STAINED_GLASS },
			{ Blocks.PURPLE_CONCRETE, Blocks.PURPLE_STAINED_GLASS },
	};
	private static final ChatFormatting[] COLORS = new ChatFormatting[] {
			ChatFormatting.RED,
			ChatFormatting.GOLD,
			ChatFormatting.YELLOW,
			ChatFormatting.GREEN,
			ChatFormatting.AQUA,
			ChatFormatting.BLUE,
			ChatFormatting.DARK_PURPLE,			
	};

	private BlockPos lastPos = null;
	private final List<BlockPos> nearbyGlass = new ArrayList<>();
	private double prevAmount;

	private int prevStep = 0;
	private final Deque<QueuedBlock> blockQueue = new ArrayDeque<>();

	public void updateMonument(double amount, boolean fast) {
		if (!DonationConfigs.MONUMENT.active.get()) return;
		LOGGER.info("Total: " + amount);
		if (amount == prevAmount) return;
		ServerLevel world = getWorld(ServerLifecycleHooks.getCurrentServer());
		BlockPos pos = DonationConfigs.MONUMENT.pos;
		if (!pos.equals(lastPos)) {
			lastPos = pos;
			rescanGlass(world, pos);
			LOGGER.info("Found {} glass blocks nearby", nearbyGlass.size());
		}
		lastPos = pos;

		int blocksPerLayer = LAYER_POSITIONS.size();
		double dollarsPerBlock = (double) DOLLARS_PER_LAYER / blocksPerLayer;
		int step = (int) (amount / dollarsPerBlock);
		LOGGER.info("Step Increase: {} -> {}", prevStep, step);
		while (step > prevStep) {
			addMonumentBlock(world, prevStep / blocksPerLayer, prevStep % blocksPerLayer);
			prevStep++;
		}
		if (fast) {
			drain(world, -1);
		}
	}

	private ServerLevel getWorld(MinecraftServer server) {
		ResourceKey<Level> dimension = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(DonationConfigs.MONUMENT.dimension.get()));
		ServerLevel world = server.getLevel(dimension);
		if (world == null) {
			LOGGER.error("Failed to find dimension : " + DonationConfigs.MONUMENT.dimension.get());
			world = ServerLifecycleHooks.getCurrentServer().overworld();
		}
		return world;
	}

	private @Nullable BlockPos addMonumentBlock(ServerLevel world, int level, int step) {
		BlockPos pos = DonationConfigs.MONUMENT.pos;
		pos = pos.above(level);
		BlockPos offset = LAYER_POSITIONS.get(step);
		pos = pos.offset(offset);
		int color = (level / LAYERS_PER_COLOR) % COLORS.length;
		BlockState state = BLOCKS[color][0].defaultBlockState();
		blockQueue.offer(new QueuedBlock(pos, state, color, level, step));
		return null;
	}

	public void tick(MinecraftServer server) {
		if (!DonationConfigs.MONUMENT.active.get()) return;
		if (server.getTickCount() % 10 == 0) {
			drain(server, 1);
		}
	}

	private void drain(MinecraftServer server, int amt) {
		if (amt == 0 || blockQueue.isEmpty()) {
			return;
		}
		drain(getWorld(server), amt);
	}

	private void drain(ServerLevel world, int amt) {
		int updateGlassToColor = -1;
		while ((amt < 0 || amt-- > 0) && !blockQueue.isEmpty()) {
			QueuedBlock queued = blockQueue.poll();
			BlockPos pos = queued.pos;
			if (world.setBlock(pos, queued.state, 0)) {
				world.getChunkSource().blockChanged(pos);
				if (queued.step == LAYER_POSITIONS.size() - 1) {
					// A layer has completed, send a message
					Component message = new TextComponent("The Monument")
							.withStyle(ChatFormatting.BOLD, COLORS[queued.color])
							.append(new TextComponent(" has grown to ")
								.setStyle(Style.EMPTY.withBold(false).withColor(ChatFormatting.WHITE))
								.append(new TextComponent("LEVEL " + (queued.layer + 1) + "!")
									.setStyle(Style.EMPTY.setUnderlined(true))));

					world.players().forEach(p -> p.displayClientMessage(message, false));
					sendToDiscord(message.getString());
				} else if (queued.step == 0 && queued.layer % LAYERS_PER_COLOR == 0) {
					// A new layer has begun, update the glass
					updateGlassToColor = queued.color;
				}
				if (amt >= 0) { // Not an infinite drain
					// Throw some particles around
					Random rand = world.getRandom();
					Vec3 center = Vec3.atLowerCornerOf(pos).add(0.5, 0.5, 0.5);
					for (int i = 0; i < 20; i++) {
						Direction dir = rand.nextInt(3) != 0 ? Direction.UP : Direction.from2DDataValue(rand.nextInt(4));
						Vec3 spawnPos = center.add(Vec3.atLowerCornerOf(dir.getNormal()).scale(0.6f))
								.add((rand.nextDouble() - 0.5) * (1 - Math.abs(dir.getStepX())),
									 (rand.nextDouble() - 0.5) * (1 - Math.abs(dir.getStepY())),
									 (rand.nextDouble() - 0.5) * (1 - Math.abs(dir.getStepZ())));
						Vec3 speed = spawnPos.subtract(center);
						world.sendParticles(ParticleTypes.END_ROD, spawnPos.x, spawnPos.y, spawnPos.z, 0, speed.x, speed.y, speed.z, 0.075);
					}
				}
			}
			for (Direction dir : Direction.values()) {
				if (dir != Direction.DOWN) {
					BlockPos airPos = pos.relative(dir);
					BlockState atPos = world.getBlockState(airPos);
					if ((atPos.isAir() || atPos.getBlock() == Blocks.WATER) && world.setBlockAndUpdate(airPos, DonationBlock.AIR_LIGHT.getDefaultState())) {
						world.getChunkSource().blockChanged(airPos);
					}
				}
			}
		}
		// Run this at the end so that it doesn't go multiple times in the intitial drain
		if (updateGlassToColor >= 0) {
			for (BlockPos glass : nearbyGlass) {
				if (world.setBlock(glass, BLOCKS[updateGlassToColor][1].defaultBlockState(), 0)) {
					world.getChunkSource().blockChanged(glass);
				}
			}
		}
	}

	private void rescanGlass(ServerLevel world, BlockPos center) {
		nearbyGlass.clear();
		BlockPos first = center.below(1).east(GLASS_SEARCH_HORIZ).north(GLASS_SEARCH_HORIZ);
		BlockPos second = center.above(GLASS_SEARCH_VERT).west(GLASS_SEARCH_HORIZ).south(GLASS_SEARCH_HORIZ);
		Map<ChunkPos, LevelChunk> chunkCache = new HashMap<>();
		BlockPos.betweenClosedStream(first, second).forEach(pos -> {
			LevelChunk chunk = chunkCache.computeIfAbsent(new ChunkPos(pos), p -> world.getChunk(p.x, p.z));
			if (chunk.getBlockState(pos).is(Tags.Blocks.STAINED_GLASS)) {
				nearbyGlass.add(pos.immutable());
			}
		});
	}

	private static boolean skipDiscord = false;
	private static Object discord;
	private static Method _sendMessage;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void sendToDiscord(String content) {
		if (skipDiscord) return;
		if (discord == null) {
			try {
				Class dcintegration = Class.forName("de.erdbeerbaerlp.dcintegration.DiscordIntegration");
				discord = ObfuscationReflectionHelper.getPrivateValue(dcintegration, null, "discord_instance");
				_sendMessage = ObfuscationReflectionHelper.findMethod(discord.getClass(), "sendMessage", String.class);
			} catch (Exception e) {
				LOGGER.warn("Failed to setup dcintegration sender:", e);
				skipDiscord = true;
				return;
			}
		}
		try {
			_sendMessage.invoke(discord, content);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			LOGGER.error("Failed to send to dcintegration:", e);
			skipDiscord = true;
		}
	}
}
