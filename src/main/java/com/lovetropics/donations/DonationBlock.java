package com.lovetropics.donations;

import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.TileEntityEntry;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
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

    public static final TileEntityEntry<DonationTileEntity> TILE = TileEntityEntry.cast(LTDonations.registrate().get("donation", TileEntityType.class));

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
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return TILE.create();
	}

	@Override
	@OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, IBlockReader worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
	    super.appendHoverText(stack, worldIn, tooltip, flagIn);
	    tooltip.add(new TranslationTextComponent(this.getDescriptionId() + ".desc").withStyle(TextFormatting.GRAY));
	}
}
