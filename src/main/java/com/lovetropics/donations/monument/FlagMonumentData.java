package com.lovetropics.donations.monument;

import com.lovetropics.donations.DonationGroup;
import com.lovetropics.lib.codec.MoreCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record FlagMonumentData(
		GlobalPos pos,
		Template template,
		DonationGroup group,
		double dollarsPerLayer,
		boolean announceLayer
) implements MonumentData {
	public static final MapCodec<FlagMonumentData> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GlobalPos.MAP_CODEC.forGetter(FlagMonumentData::pos),
			Template.CODEC.fieldOf("template").forGetter(FlagMonumentData::template),
			DonationGroup.CODEC.optionalFieldOf("group", DonationGroup.ALL).forGetter(FlagMonumentData::group),
			Codec.DOUBLE.optionalFieldOf("dollars_per_layer", 1000.0).forGetter(FlagMonumentData::dollarsPerLayer),
			Codec.BOOL.optionalFieldOf("announce_layer", false).forGetter(FlagMonumentData::announceLayer)
	).apply(i, FlagMonumentData::new));

	public record Template(
			ResourceKey<Level> dimension,
			BlockPos origin,
			Map<Block, List<BlockState>> layerPalettes
	) {
		private static final Map<Block, List<BlockState>> CONCRETE_PALETTE = Stream.of(
				List.of(Blocks.RED_CONCRETE, Blocks.RED_GLAZED_TERRACOTTA),
				List.of(Blocks.WHITE_CONCRETE, Blocks.WHITE_GLAZED_TERRACOTTA),
				List.of(Blocks.BLACK_CONCRETE, Blocks.BLACK_GLAZED_TERRACOTTA),
				List.of(Blocks.PINK_CONCRETE, Blocks.PINK_GLAZED_TERRACOTTA),
				List.of(Blocks.ORANGE_CONCRETE, Blocks.ORANGE_GLAZED_TERRACOTTA),
				List.of(Blocks.YELLOW_CONCRETE, Blocks.YELLOW_GLAZED_TERRACOTTA),
				List.of(Blocks.GREEN_CONCRETE, Blocks.GREEN_GLAZED_TERRACOTTA),
				List.of(Blocks.LIME_CONCRETE, Blocks.LIME_GLAZED_TERRACOTTA),
				List.of(Blocks.CYAN_CONCRETE, Blocks.CYAN_GLAZED_TERRACOTTA),
				List.of(Blocks.LIGHT_BLUE_CONCRETE, Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA),
				List.of(Blocks.LIGHT_GRAY_CONCRETE, Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA),
				List.of(Blocks.GRAY_CONCRETE, Blocks.GRAY_GLAZED_TERRACOTTA),
				List.of(Blocks.BROWN_CONCRETE, Blocks.BROWN_GLAZED_TERRACOTTA),
				List.of(Blocks.BLUE_CONCRETE, Blocks.BLUE_GLAZED_TERRACOTTA),
				List.of(Blocks.PURPLE_CONCRETE, Blocks.PURPLE_GLAZED_TERRACOTTA),
				List.of(Blocks.MAGENTA_CONCRETE, Blocks.MAGENTA_GLAZED_TERRACOTTA)
		).collect(Collectors.toMap(List::getFirst, blocks -> blocks.stream().map(Block::defaultBlockState).toList()));

		public static final Codec<Template> CODEC = RecordCodecBuilder.create(i -> i.group(
				Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Template::dimension),
				BlockPos.CODEC.fieldOf("origin").forGetter(Template::origin),
				Codec.unboundedMap(BuiltInRegistries.BLOCK.byNameCodec(), MoreCodecs.BLOCK_STATE.listOf()).optionalFieldOf("layer_palettes", CONCRETE_PALETTE).forGetter(Template::layerPalettes)
		).apply(i, Template::new));
	}

	private static final Direction[] DIRECTIONS = Direction.values();

	@Override
	@Nullable
	public Monument create(MinecraftServer server) {
		ServerLevel level = server.getLevel(pos.dimension());
		ServerLevel templateLevel = server.getLevel(template.dimension());
		if (level == null || templateLevel == null) {
			return null;
		}
		List<LayeredMonument.Layer> layers = scanForLayers(templateLevel, template.origin, pos.pos());
		return new LayeredMonument(level, layers, group, dollarsPerLayer, announceLayer, this);
	}

	// TODO: Rewrite this in a better way?
	private List<LayeredMonument.Layer> scanForLayers(ServerLevel level, BlockPos scanOrigin, BlockPos targetOrigin) {
		BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
		LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
		LongArrayFIFOQueue nextLayerQueue = new LongArrayFIFOQueue();

		List<LayeredMonument.Layer> layers = new ArrayList<>();
		BlockState layerBlock = null;

		LongSet visitedBlocks = new LongOpenHashSet();
		LongList blocksInLayer = new LongArrayList();

		queue.enqueue(scanOrigin.asLong());
		visitedBlocks.add(scanOrigin.asLong());

		BlockPos templateToTarget = targetOrigin.subtract(scanOrigin);

		while (true) {
			while (!queue.isEmpty()) {
				long templatePos = queue.dequeueLong();
				BlockState blockState = level.getBlockState(mutablePos.set(templatePos));

				// This block does not fit the current layer, defer it to be checked later
				if (layerBlock != null && blockState != layerBlock) {
					nextLayerQueue.enqueue(templatePos);
					continue;
				}

				if (layerBlock == null) {
					// Try to start a new layer
					if (template.layerPalettes.containsKey(blockState.getBlock())) {
						layerBlock = blockState;
					} else {
						continue;
					}
				}

				long targetPos = BlockPos.offset(templatePos, templateToTarget.getX(), templateToTarget.getY(), templateToTarget.getZ());
				blocksInLayer.add(targetPos);
				enqueueNeighbors(mutablePos, templatePos, queue, visitedBlocks);
			}

			if (blocksInLayer.isEmpty() || layerBlock == null) {
				return layers;
			}

			layers.add(new LayeredMonument.Layer(
					sortedBlocksInLayer(targetOrigin, blocksInLayer),
					Blocks.AIR.defaultBlockState(),
					template.layerPalettes.get(layerBlock.getBlock())
			));

			// Swap our queues, and start looking in the next layer
			LongArrayFIFOQueue swap = queue;
			queue = nextLayerQueue;
			nextLayerQueue = swap;
			layerBlock = null;
			blocksInLayer.clear();
		}
	}

	private static void enqueueNeighbors(BlockPos.MutableBlockPos mutablePos, long originPos, LongArrayFIFOQueue queue, LongSet visitedBlocks) {
		for (Direction direction : DIRECTIONS) {
			mutablePos.set(originPos).move(direction);
			long neighborPos = mutablePos.asLong();
			if (visitedBlocks.add(neighborPos)) {
				queue.enqueue(neighborPos);
			}
		}
	}

	private static LongList sortedBlocksInLayer(BlockPos targetOrigin, LongList blocks) {
		LongList sorted = new LongArrayList(blocks);
		sorted.sort((pos1, pos2) -> {
			// Sort first top-down on Y
			int compareY = Integer.compare(
					BlockPos.getY(pos2),
					BlockPos.getY(pos1)
			);
			if (compareY != 0) {
				return compareY;
			}
			// Sort inwards by manhattan distance from origin on the XZ plane
			return Integer.compare(
					manhattanXZ(targetOrigin, pos2),
					manhattanXZ(targetOrigin, pos1)
			);
		});
		return sorted;
	}

	private static int manhattanXZ(BlockPos origin, long pos) {
		return Math.abs(origin.getX() - BlockPos.getX(pos))
				+ Math.abs(origin.getZ() - BlockPos.getZ(pos));
	}

	@Override
	public MonumentType type() {
		return MonumentType.FLAG;
	}
}
