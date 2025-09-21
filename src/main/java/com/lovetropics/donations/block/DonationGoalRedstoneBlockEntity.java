package com.lovetropics.donations.block;

import com.lovetropics.donations.DonationGroup;
import com.lovetropics.donations.LTDonations;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class DonationGoalRedstoneBlockEntity extends BlockEntity {

    private int donationGoalIndex = 1;
    private int donationGoalStep = 250;
    private int randomOffset = 0;
    private boolean lastPoweredState = false;

    public int getRandomOffset() {
        return randomOffset;
    }

    public void setRandomOffset(int randomOffset) {
        this.randomOffset = randomOffset;
    }

    public DonationGoalRedstoneBlockEntity(BlockEntityType<? extends DonationGoalRedstoneBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void setLevel(final Level level) {
        super.setLevel(level);
        setRandomOffset(20);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DonationGoalRedstoneBlockEntity entity) {
        if (!level.isClientSide) {

            if (entity.getRandomOffset() == 0 || level.getGameTime() % 20 == 0) {
                if (LTDonations.state().getAmount(DonationGroup.ALL) >= entity.getDonationGoalAmount()) {
                    if (!entity.lastPoweredState) {
                        entity.setPoweredState(true);
                    }
                } else {
                    if (entity.lastPoweredState) {
                        entity.setPoweredState(false);
                    }
                }
            }
        }
    }

    public int getDonationGoalAmount() {
        return this.donationGoalIndex * this.donationGoalStep;
    }

    public void setPoweredState(boolean state) {
        BlockState blockState = level.getBlockState(this.getBlockPos());
        if (blockState.getBlock() instanceof DonationGoalRedstoneBlock) {
            ((DonationGoalRedstoneBlock) blockState.getBlock()).setPoweredState(this.getBlockState(), level, this.getBlockPos(), state);
            this.lastPoweredState = state;
        }
    }

    public void pulseLengthUp(ServerPlayer player) {
        donationGoalIndex++;
        player.sendSystemMessage(Component.literal("Donation goal trigger set to " + getDonationGoalAmount()));
    }

    public void pulseLengthDown(ServerPlayer player) {
        donationGoalIndex--;
        if (donationGoalIndex < 0) {
            donationGoalIndex = 0;
        }
        player.sendSystemMessage(Component.literal("Donation goal trigger set to " + getDonationGoalAmount()));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        donationGoalIndex = input.getIntOr("donationGoalIndex", 1);
        lastPoweredState = input.getBooleanOr("lastPoweredState", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("donationGoalIndex", this.donationGoalIndex);
        output.putBoolean("lastPoweredState", this.lastPoweredState);
    }
}
