package com.lovetropics.donations.top_donor;

import com.lovetropics.donations.DonationConfigs;
import com.lovetropics.donations.backend.ltts.DonationRequests;
import com.lovetropics.donations.backend.ltts.json.TopDonor;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TopDonorManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private UUID[] lastEntityUuids;

    public void pollTopDonors() {
        UUID[] topDonorUuids = DonationConfigs.TOP_DONORS.getTopDonorUuids();

        CompletableFuture.supplyAsync(() -> DonationRequests.get().getTopDonors(topDonorUuids.length))
                .thenAcceptAsync(this::applyTopDonors, ServerLifecycleHooks.getCurrentServer());
    }

    private void applyTopDonors(List<TopDonor> topDonors) {
        UUID[] topDonorUuids = DonationConfigs.TOP_DONORS.getTopDonorUuids();
        LOGGER.debug("Applying {} top donators to {} entities", topDonors.size(), topDonorUuids.length);

        int newTopDonorLength = Math.min(topDonorUuids.length, topDonors.size());

        UUID[] entityIds = new UUID[newTopDonorLength];
        System.arraycopy(topDonorUuids, 0, entityIds, 0, newTopDonorLength);

        for (int i = 0; i < newTopDonorLength; i++) {
            UUID entityId = entityIds[i];
            TopDonor donor = topDonors.get(i);
            List<String> fallbacks = donor.displayNames();
            this.applyToEntity(entityId, donor.minecraftName().orElse(null), fallbacks.isEmpty() ? "Anonymous" : fallbacks.get(fallbacks.size() - 1), donor.total());
        }

        UUID[] lastEntityUuids = this.lastEntityUuids;
        if (lastEntityUuids != null && lastEntityUuids.length > entityIds.length) {
            for (int i = entityIds.length; i < lastEntityUuids.length; i++) {
                this.clearEntity(lastEntityUuids[i]);
            }
        }

        this.lastEntityUuids = entityIds;
    }

    private void applyToEntity(UUID entityId, @Nullable String minecraftName, String fallbackName, double total) {
        Entity entity = this.findEntity(entityId);
        if (entity == null) return;

        CompoundTag data = entity.saveWithoutId(new CompoundTag());
        if (minecraftName != null) {
        	data.remove("CustomName");
        	entity.setCustomName(null);
        	data.putString("ProfileName", minecraftName);
        } else {
        	data.putString("ProfileName", "");
        	data.putString("CustomName", "{\"text\":\"" + fallbackName + "\"}");
        }
        Component suffix = Component.literal(" - ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format("$%.2f", total)).withStyle(ChatFormatting.GREEN));
        data.putString("NameSuffix", Component.Serializer.toJson(suffix));
        entity.load(data);
    }

    private void clearEntity(UUID entityId) {
        Entity entity = this.findEntity(entityId);
        if (entity == null) return;

        CompoundTag data = entity.saveWithoutId(new CompoundTag());
        data.putString("CustomName", "{\"text\":\"A Future Donator\"}");
        data.putString("ProfileName", "");
        data.remove("NameSuffix");

        entity.load(data);
    }

    @Nullable
    private Entity findEntity(UUID entityId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerLevel world = this.getWorld(server);
        Entity entity = world.getEntity(entityId);
        if (entity == null) {
            LOGGER.error("Failed to find entity: " + entityId);
            return null;
        }
        return entity;
    }

    private ServerLevel getWorld(MinecraftServer server) {
        ResourceLocation dimensionId = new ResourceLocation(DonationConfigs.TOP_DONORS.dimension.get());
        ResourceKey<Level> dimensionType = ResourceKey.create(Registries.DIMENSION, dimensionId);
        ServerLevel world = server.getLevel(dimensionType);
        if (world == null) {
            LOGGER.error("Failed to find dimension : " + DonationConfigs.TOP_DONORS.dimension.get());
            world = server.overworld();
        }
        return world;
    }
}
