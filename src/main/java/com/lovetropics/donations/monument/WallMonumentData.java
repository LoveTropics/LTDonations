package com.lovetropics.donations.monument;

import com.lovetropics.donations.DonationGroup;
import com.lovetropics.lib.BlockBox;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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

public record WallMonumentData(ResourceKey<Level> dimension, BlockBox box, DonationGroup group, MonumentStyle style) implements MonumentData {
	public static final MapCodec<WallMonumentData> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(WallMonumentData::dimension),
			BlockBox.CODEC.fieldOf("box").forGetter(WallMonumentData::box),
			DonationGroup.CODEC.optionalFieldOf("group", DonationGroup.ALL).forGetter(WallMonumentData::group),
			MonumentStyle.CODEC.optionalFieldOf("style", MonumentStyle.NORMAL).forGetter(WallMonumentData::style)
	).apply(i, WallMonumentData::new));

	public static final Block BACKGROUND_BLOCK = Blocks.SCULK;

	private static final Logger LOGGER = LogUtils.getLogger();

	@Override
	@Nullable
	public Monument create(MinecraftServer server) {
		ServerLevel level = server.getLevel(dimension);
		if (level != null) {
			LongList[] blocksByLayer = scanBlocksByLayer(level, box, style);
			if (blocksByLayer != null) {
				return new LayeredMonument(level, blocksByLayer, this);
			}
		}
		return null;
	}

	@Nullable
	private static LongList[] scanBlocksByLayer(ServerLevel level, BlockBox box, MonumentStyle style) {
		int minY = box.min().getY();
		int maxY = box.max().getY();
		LongList[] result = new LongList[maxY - minY + 1];
		for (int i = 0; i < result.length; i++) {
			result[i] = new LongArrayList();
		}
		for (BlockPos pos : box) {
			if (isReplaceable(style, level.getBlockState(pos))) {
				int layer = pos.getY() - minY;
				result[layer].add(pos.asLong());
			}
		}
		BlockPos centerBlock = box.centerBlock();
		for (int i = 0; i < result.length; i++) {
			LongList layer = result[i];
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

	private static boolean isReplaceable(MonumentStyle style, BlockState state) {
		return style.replaceableBlocks.contains(state.getBlock());
	}

	@Override
	public MonumentType type() {
		return MonumentType.WALL;
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

		MonumentStyle(String name, double dollarsPerLayer, Block[][] palette) {
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

		public Block[][] palette() {
			return palette;
		}

		public double dollarsPerLayer() {
			return dollarsPerLayer;
		}
	}
}
