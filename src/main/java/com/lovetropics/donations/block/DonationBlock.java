package com.lovetropics.donations.block;

import com.lovetropics.donations.LTDonations;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class DonationBlock extends Block implements EntityBlock {

    public static final BlockEntry<DonationBlock> BLOCK = LTDonations.registrate()
            .block("donation", DonationBlock::new)
            .initialProperties(() -> Blocks.BEDROCK)
            .properties(Properties::noLootTable)
            .blockEntity(DonationBlockEntity::new).build()
            .simpleItem()
            .register();

    public static final BlockEntityEntry<DonationBlockEntity> ENTITY = BlockEntityEntry.cast(LTDonations.registrate().get("donation", Registries.BLOCK_ENTITY_TYPE));

    public static final void register() {
    }

    public DonationBlock(Block.Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new DonationBlockEntity(ENTITY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        return createTicker(type, ENTITY.get(), DonationBlockEntity::tick);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTicker(final BlockEntityType<A> type, final BlockEntityType<E> tickerType, final BlockEntityTicker<? super E> ticker) {
        return tickerType == type ? (BlockEntityTicker<A>) ticker : null;
    }
}
