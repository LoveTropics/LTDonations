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
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

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
		private static Block extrasBlock(String id) {
			DeferredHolder<Block, Block> holder = DeferredHolder.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("ltextras", id));
			return holder.isBound() ? holder.value() : Blocks.AIR;
		}

		private static Map<Block, List<BlockState>> standardPalette() {
			return Stream.of(
					List.of(Blocks.RED_CONCRETE, extrasBlock("imposter_fire_coral_block"), Blocks.REDSTONE_BLOCK),
					List.of(Blocks.ORANGE_CONCRETE, Blocks.SHROOMLIGHT, Blocks.WAXED_COPPER_BLOCK),
					List.of(Blocks.YELLOW_CONCRETE, extrasBlock("imposter_horn_coral_block"), Blocks.GOLD_BLOCK),
					List.of(Blocks.LIME_CONCRETE, extrasBlock("lime_block"), Blocks.EMERALD_BLOCK),
					List.of(Blocks.LIGHT_BLUE_CONCRETE, Blocks.WARPED_WART_BLOCK, Blocks.DIAMOND_BLOCK),
					List.of(Blocks.BLUE_CONCRETE, extrasBlock("imposter_tube_coral_block"), Blocks.LAPIS_BLOCK),
					List.of(Blocks.PURPLE_CONCRETE, Blocks.WARPED_HYPHAE, Blocks.AMETHYST_BLOCK),
					List.of(Blocks.MAGENTA_CONCRETE, extrasBlock("imposter_bubble_coral_block"), Blocks.PURPUR_BLOCK)
			).collect(Collectors.toMap(List::getFirst, blocks -> blocks.stream().map(Block::defaultBlockState).toList()));
		}

		public static final Codec<Template> CODEC = RecordCodecBuilder.create(i -> i.group(
				Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Template::dimension),
				BlockPos.CODEC.fieldOf("origin").forGetter(Template::origin),
				Codec.unboundedMap(BuiltInRegistries.BLOCK.byNameCodec(), MoreCodecs.BLOCK_STATE.listOf()).fieldOf("layer_palettes").orElseGet(Template::standardPalette).forGetter(Template::layerPalettes)
		).apply(i, Template::new));
	}

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
		for (int z = -1; z <= 1; z++) {
			for (int y = -1; y <= 1; y++) {
				for (int x = -1; x <= 1; x++) {
					mutablePos.set(originPos).move(x, y, z);
					long neighborPos = mutablePos.asLong();
					if (visitedBlocks.add(neighborPos)) {
						queue.enqueue(neighborPos);
					}
				}
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
