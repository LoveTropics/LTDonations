package com.lovetropics.donations;

import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.TileEntityEntry;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.BlockGetter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

public class DonationBlock extends Block {

    public static final BlockEntry<DonationBlock> BLOCK = LTDonations.registrate()
    		.block("donation", DonationBlock::new)
            .properties(p -> Block.Properties.copy(Blocks.BEDROCK).noDrops())
            .simpleTileEntity(DonationTileEntity::new)
            .simpleItem()
            .register();

    public static final TileEntityEntry<DonationTileEntity> TILE = TileEntityEntry.cast(LTDonations.registrate().get("donation", BlockEntityType.class));

    // TODO move this somewhere else
	public static final BlockEntry<AirBlock> AIR_LIGHT = LTDonations.registrate()
			.block("air_light", p -> (AirBlock) new AirBlock(p) {})
			.initialProperties(() -> Blocks.AIR)
			.properties(p -> p.lightLevel(s -> 15))
			.register();

    public static final void register() {}

	public DonationBlock(Block.Properties properties) {
		super(properties);
	}
	
	@Override
	public boolean hasTileEntity(BlockState state) {
	    return true;
	}

	@Override
	public BlockEntity createTileEntity(BlockState state, BlockGetter world) {
		return TILE.create();
	}

	@Override
	@OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, BlockGetter worldIn, List<Component> tooltip, TooltipFlag flagIn) {
	    super.appendHoverText(stack, worldIn, tooltip, flagIn);
	    tooltip.add(new TranslatableComponent(this.getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
	}
}
