package com.lovetropics.donations.top_donor;

import com.lovetropics.donations.DonationConfigs;
import com.lovetropics.donations.backend.ltts.DonationRequests;
import com.lovetropics.donations.backend.ltts.json.TopDonor;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
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
    private static final String EMPTY_COMPONENT_STRING = Component.Serializer.toJson(CommonComponents.EMPTY);

    public void pollTopDonors() {
        UUID[] topDonorUuids = DonationConfigs.TOP_DONORS.getTopDonorUuids();

        CompletableFuture.supplyAsync(() -> DonationRequests.get().getTopDonors(topDonorUuids.length))
                .thenAcceptAsync(this::applyTopDonors, ServerLifecycleHooks.getCurrentServer());
    }

    private void applyTopDonors(List<TopDonor> topDonors) {
        UUID[] entityIds = DonationConfigs.TOP_DONORS.getTopDonorUuids();
        LOGGER.debug("Applying {} top donators to {} entities", topDonors.size(), entityIds.length);

        for (int i = 0; i < entityIds.length; i++) {
            UUID entityId = entityIds[i];
            Entity entity = findEntity(entityId);
            if (entity == null) {
                continue;
            }

            if (i < topDonors.size()) {
                TopDonor donor = topDonors.get(i);
                List<String> fallbacks = donor.displayNames();
                String minecraftName = donor.minecraftName().orElse(null);
                String fallbackName = fallbacks.isEmpty() ? "Anonymous" : fallbacks.get(fallbacks.size() - 1);
                applyToEntity(entity, minecraftName, Component.literal(fallbackName), donor.total(), donor.isAnonymous());
            } else {
                clearEntity(entity);
            }
        }
    }

    private void applyToEntity(Entity entity, @Nullable String minecraftName, Component fallbackName, double total, boolean anonymous) {
        CompoundTag data = entity.saveWithoutId(new CompoundTag());
        if (anonymous) {
            // We look for the null UUID in the datapack
            data.putUUID("ProfileID", Util.NIL_UUID);
            data.putString("CustomName", Component.Serializer.toJson(fallbackName));
        } else if (minecraftName != null) {
        	data.putString("CustomName", EMPTY_COMPONENT_STRING);
        	entity.setCustomName(null);
        	data.putString("ProfileName", minecraftName);
        } else {
        	data.putString("ProfileName", "");
        	data.putString("CustomName", Component.Serializer.toJson(fallbackName));
        }
        Component suffix = Component.literal(" - ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.format("$%.2f", total)).withStyle(ChatFormatting.GREEN));
        data.putString("NameSuffix", Component.Serializer.toJson(suffix));
        data.putBoolean("CustomNameVisible", true);
        entity.load(data);
    }

    private void clearEntity(Entity entity) {
        CompoundTag data = entity.saveWithoutId(new CompoundTag());
        data.putString("CustomName", "{\"text\":\"A Future Donator\"}");
        data.putString("ProfileName", "");
        data.putString("NameSuffix", EMPTY_COMPONENT_STRING);

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
