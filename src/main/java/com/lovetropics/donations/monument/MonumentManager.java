package com.lovetropics.donations.monument;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableList;
import com.lovetropics.donations.DonationBlock;
import com.lovetropics.donations.DonationConfigs;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

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

					Vec3d v1 = new Vec3d(p);
					Vec3d v2 = new Vec3d(Vector3f.XN);

					Vec3d cross = v1.crossProduct(v2);
					double dot = v1.dotProduct(v2);

					double angle = Math.atan2(cross.length(), dot);

					double test = new Vec3d(Vector3f.YP).dotProduct(cross);
					if (test < 0.0) angle = -angle + (Math.PI * 2);
					return angle;
				}));

		LAYER_POSITIONS = ImmutableList.copyOf(allPositions);
	}

	private static List<BlockPos> range(int minX, int minZ, int maxX, int maxZ) {
		return BlockPos.getAllInBox(minX, 0, minZ, maxX, 0, maxZ).map(BlockPos::toImmutable).collect(Collectors.toList());
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
	private static final TextFormatting[] COLORS = new TextFormatting[] {
			TextFormatting.RED,
			TextFormatting.GOLD,
			TextFormatting.YELLOW,
			TextFormatting.GREEN,
			TextFormatting.AQUA,
			TextFormatting.BLUE,
			TextFormatting.DARK_PURPLE,			
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
		ServerWorld world = getWorld(ServerLifecycleHooks.getCurrentServer());
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

	private ServerWorld getWorld(MinecraftServer server) {
		ServerWorld world = server.getWorld(DimensionType.byName(new ResourceLocation(DonationConfigs.MONUMENT.dimension.get())));
		if (world == null) {
			LOGGER.error("Failed to find dimension : " + DonationConfigs.MONUMENT.dimension.get());
			world = ServerLifecycleHooks.getCurrentServer().getWorld(DimensionType.OVERWORLD);
		}
		return world;
	}

	private @Nullable BlockPos addMonumentBlock(ServerWorld world, int level, int step) {
		BlockPos pos = DonationConfigs.MONUMENT.pos;
		pos = pos.up(level);
		BlockPos offset = LAYER_POSITIONS.get(step);
		pos = pos.add(offset);
		int color = (level / LAYERS_PER_COLOR) % COLORS.length;
		BlockState state = BLOCKS[color][0].getDefaultState();
		blockQueue.offer(new QueuedBlock(pos, state, color, level, step));
		return null;
	}

	public void tick(MinecraftServer server) {
		if (!DonationConfigs.MONUMENT.active.get()) return;
		if (server.getTickCounter() % 10 == 0) {
			drain(server, 1);
		}
	}

	private void drain(MinecraftServer server, int amt) {
		if (amt == 0 || blockQueue.isEmpty()) {
			return;
		}
		drain(getWorld(server), amt);
	}

	private void drain(ServerWorld world, int amt) {
		int updateGlassToColor = -1;
		while ((amt < 0 || amt-- > 0) && !blockQueue.isEmpty()) {
			QueuedBlock queued = blockQueue.poll();
			BlockPos pos = queued.pos;
			if (world.setBlockState(pos, queued.state, 0)) {
				world.getChunkProvider().markBlockChanged(pos);
				if (queued.step == LAYER_POSITIONS.size() - 1) {
					// A layer has completed, send a message
					ITextComponent message = new StringTextComponent("The Monument")
							.applyTextStyles(TextFormatting.BOLD, COLORS[queued.color])
							.appendSibling(new StringTextComponent(" has grown to ")
								.setStyle(new Style().setBold(false).setColor(TextFormatting.WHITE))
								.appendSibling(new StringTextComponent("LEVEL " + (queued.layer + 1) + "!")
									.setStyle(new Style().setUnderlined(true))));

					world.getPlayers().forEach(p -> p.sendMessage(message));
					sendToDiscord(message.getString());
				} else if (queued.step == 0 && queued.layer % LAYERS_PER_COLOR == 0) {
					// A new layer has begun, update the glass
					updateGlassToColor = queued.color;
				}
				if (amt >= 0) { // Not an infinite drain
					// Throw some particles around
					Random rand = world.getRandom();
					Vec3d center = new Vec3d(pos).add(0.5, 0.5, 0.5);
					for (int i = 0; i < 20; i++) {
						Direction dir = rand.nextInt(3) != 0 ? Direction.UP : Direction.byHorizontalIndex(rand.nextInt(4));
						Vec3d spawnPos = center.add(new Vec3d(dir.getDirectionVec()).scale(0.6f))
								.add((rand.nextDouble() - 0.5) * (1 - Math.abs(dir.getXOffset())),
									 (rand.nextDouble() - 0.5) * (1 - Math.abs(dir.getYOffset())),
									 (rand.nextDouble() - 0.5) * (1 - Math.abs(dir.getZOffset())));
						Vec3d speed = spawnPos.subtract(center);
						world.spawnParticle(ParticleTypes.END_ROD, spawnPos.x, spawnPos.y, spawnPos.z, 0, speed.x, speed.y, speed.z, 0.075);
					}
				}
			}
			for (Direction dir : Direction.values()) {
				if (dir != Direction.DOWN) {
					BlockPos airPos = pos.offset(dir);
					BlockState atPos = world.getBlockState(airPos);
					if ((atPos.isAir(world, airPos) || atPos.getBlock() == Blocks.WATER) && world.setBlockState(airPos, DonationBlock.AIR_LIGHT.getDefaultState())) {
						world.getChunkProvider().markBlockChanged(airPos);
					}
				}
			}
		}
		// Run this at the end so that it doesn't go multiple times in the intitial drain
		if (updateGlassToColor >= 0) {
			for (BlockPos glass : nearbyGlass) {
				if (world.setBlockState(glass, BLOCKS[updateGlassToColor][1].getDefaultState(), 0)) {
					world.getChunkProvider().markBlockChanged(glass);
				}
			}
		}
	}

	private void rescanGlass(ServerWorld world, BlockPos center) {
		nearbyGlass.clear();
		BlockPos first = center.down(1).east(GLASS_SEARCH_HORIZ).north(GLASS_SEARCH_HORIZ);
		BlockPos second = center.up(GLASS_SEARCH_VERT).west(GLASS_SEARCH_HORIZ).south(GLASS_SEARCH_HORIZ);
		Map<ChunkPos, Chunk> chunkCache = new HashMap<>();
		BlockPos.getAllInBox(first, second).forEach(pos -> {
			Chunk chunk = chunkCache.computeIfAbsent(new ChunkPos(pos), p -> world.getChunk(p.x, p.z));
			if (chunk.getBlockState(pos).isIn(Tags.Blocks.STAINED_GLASS)) {
				nearbyGlass.add(pos.toImmutable());
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
