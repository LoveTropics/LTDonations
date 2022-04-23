package com.lovetropics.donations;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lovetropics.donations.backend.ltts.DonationRequests;
import com.lovetropics.donations.backend.ltts.json.TopDonor;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public final class TopDonorManager {
    private static final Logger LOGGER = LogManager.getLogger(TopDonorManager.class);

    private UUID[] lastEntityUuids;

    public void pollTopDonors() {
        UUID[] topDonorUuids = DonationConfigs.TOP_DONORS.getTopDonorUuids();

        CompletableFuture.supplyAsync(() -> DonationRequests.get().getTopDonors(topDonorUuids.length))
                .thenAcceptAsync(this::applyTopDonors, ServerLifecycleHooks.getCurrentServer());
    }

    private void applyTopDonors(List<TopDonor> topDonors) {
        UUID[] topDonorUuids = DonationConfigs.TOP_DONORS.getTopDonorUuids();

        int newTopDonorLength = Math.min(topDonorUuids.length, topDonors.size());

        UUID[] entityIds = new UUID[newTopDonorLength];
        System.arraycopy(topDonorUuids, 0, entityIds, 0, newTopDonorLength);

        for (int i = 0; i < newTopDonorLength; i++) {
            UUID entityId = entityIds[i];
            TopDonor donor = topDonors.get(i);
            List<String> fallbacks = donor.displayNames;
            this.applyToEntity(entityId, donor.minecraftName, fallbacks.isEmpty() ? "Anonymous" : fallbacks.get(fallbacks.size() - 1), donor.total);
        }

        UUID[] lastEntityUuids = this.lastEntityUuids;
        if (lastEntityUuids != null && lastEntityUuids.length > entityIds.length) {
            for (int i = entityIds.length; i < lastEntityUuids.length; i++) {
                this.clearEntity(lastEntityUuids[i]);
            }
        }

        this.lastEntityUuids = entityIds;
    }

    private void applyToEntity(UUID entityId, String minecraftName, String fallbackName, double total) {
        Entity entity = this.findEntity(entityId);
        if (entity == null) return;

        CompoundNBT data = entity.saveWithoutId(new CompoundNBT());
        if (minecraftName != null) {
        	data.remove("CustomName");
        	entity.setCustomName(null);
        	data.putString("ProfileName", minecraftName);
        } else {
        	data.putString("ProfileName", "");
        	data.putString("CustomName", "{\"text\":\"" + fallbackName + "\"}");
        }
        ITextComponent suffix = new StringTextComponent(" - ").withStyle(TextFormatting.GRAY)
                .append(new StringTextComponent(String.format("$%.2f", total)).withStyle(TextFormatting.GREEN));
        data.putString("NameSuffix", ITextComponent.Serializer.toJson(suffix));
        entity.load(data);
    }

    private void clearEntity(UUID entityId) {
        Entity entity = this.findEntity(entityId);
        if (entity == null) return;

        CompoundNBT data = entity.saveWithoutId(new CompoundNBT());
        data.putString("CustomName", "{\"text\":\"A Future Donator\"}");
        data.putString("ProfileName", "");
        data.remove("NameSuffix");

        entity.load(data);
    }

    @Nullable
    private Entity findEntity(UUID entityId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerWorld world = this.getWorld(server);
        Entity entity = world.getEntity(entityId);
        if (entity == null) {
            LOGGER.error("Failed to find entity: " + entityId);
            return null;
        }
        return entity;
    }

    private ServerWorld getWorld(MinecraftServer server) {
        ResourceLocation dimensionId = new ResourceLocation(DonationConfigs.TOP_DONORS.dimension.get());
        RegistryKey<World> dimensionType = RegistryKey.create(Registry.DIMENSION_REGISTRY, dimensionId);
        ServerWorld world = server.getLevel(dimensionType);
        if (world == null) {
            LOGGER.error("Failed to find dimension : " + DonationConfigs.TOP_DONORS.dimension.get());
            world = server.overworld();
        }
        return world;
    }
}
