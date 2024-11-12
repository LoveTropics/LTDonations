package com.lovetropics.donations.monument;

import com.lovetropics.donations.DonationGroup;
import com.lovetropics.donations.DonationState;
import com.lovetropics.lib.BlockBox;
import com.lovetropics.lib.codec.MoreCodecs;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Stream;

public class ProgressBarMonument implements Monument {
	private static final Logger LOGGER = LogUtils.getLogger();

	private final ServerLevel level;
	private final BlockBox box;
	private final List<BlockState> blocks;
	private final BlockState emptyBlock;
	private final DonationGroup group;
	private final double dollarsInBar;
	private final MonumentData data;

	private final int length;
	private final Direction.Axis axis;

	private int lastFilledBlocks = -1;

	protected ProgressBarMonument(ServerLevel level, BlockBox box, List<BlockState> blocks, BlockState emptyBlock, DonationGroup group, double dollarsInBar, MonumentData data) {
		this.level = level;
		this.box = box;
		this.blocks = blocks;
		this.emptyBlock = emptyBlock;
		this.group = group;
		this.dollarsInBar = dollarsInBar;
		this.data = data;

		BlockPos size = box.size();
		axis = size.getX() > size.getZ() ? Direction.Axis.X : Direction.Axis.Z;
		length = size.get(axis);
	}

	@Override
	public void tick(MinecraftServer server, DonationState state) {
		sync(state);
	}

	@Override
	public void sync(DonationState state) {
		int filledBlocks = Mth.floor(state.getAmount(group) * length / dollarsInBar) % length;
		if (lastFilledBlocks == filledBlocks) {
			return;
		}
		for (BlockPos pos : box) {
			int index = pos.get(axis) - box.min().get(axis);
			if (index >= filledBlocks) {
				level.setBlockAndUpdate(pos, emptyBlock);
			} else {
				level.setBlockAndUpdate(pos, blocks.get(index % blocks.size()));
			}
		}
		lastFilledBlocks = filledBlocks;
	}

	@Override
	public MonumentData toData() {
		return data;
	}

	public record Data(ResourceKey<Level> dimension, BlockBox box, List<BlockState> blocks, BlockState emptyBlock, DonationGroup donationGroup, double dollarsInBar) implements MonumentData {
		private static final List<BlockState> DEFAULT_BLOCKS = Stream.of(
				Blocks.RED_CONCRETE_POWDER,
				Blocks.ORANGE_CONCRETE_POWDER,
				Blocks.YELLOW_CONCRETE_POWDER,
				Blocks.LIME_CONCRETE_POWDER,
				Blocks.LIGHT_BLUE_CONCRETE_POWDER,
				Blocks.PURPLE_CONCRETE_POWDER,
				Blocks.MAGENTA_CONCRETE_POWDER
		).map(Block::defaultBlockState).toList();

		public static final MapCodec<Data> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Data::dimension),
				BlockBox.CODEC.fieldOf("box").forGetter(Data::box),
				ExtraCodecs.nonEmptyList(MoreCodecs.BLOCK_STATE.listOf()).optionalFieldOf("blocks", DEFAULT_BLOCKS).forGetter(Data::blocks),
				MoreCodecs.BLOCK_STATE.optionalFieldOf("empty_block", Blocks.BLACK_CONCRETE.defaultBlockState()).forGetter(Data::emptyBlock),
				DonationGroup.CODEC.optionalFieldOf("donation_group", DonationGroup.ALL).forGetter(Data::donationGroup),
				Codec.DOUBLE.optionalFieldOf("dollars_in_bar", 1000.0).forGetter(Data::dollarsInBar)
		).apply(i, Data::new));

		@Override
		@Nullable
		public Monument create(MinecraftServer server) {
			ServerLevel level = server.getLevel(dimension);
			if (level == null) {
				LOGGER.warn("Could not find dimension: {}", dimension.location());
				return null;
			}
			return new ProgressBarMonument(level, box, blocks, emptyBlock, donationGroup, dollarsInBar, this);
		}

		@Override
		public MonumentType type() {
			return MonumentType.PROGRESS_BAR;
		}
	}
}
