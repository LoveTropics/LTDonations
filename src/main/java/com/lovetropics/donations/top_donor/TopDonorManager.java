package com.lovetropics.donations.top_donor;

import com.lovetropics.donations.DonationConfigs;
import com.lovetropics.donations.backend.ltts.DonationRequests;
import com.lovetropics.donations.backend.ltts.json.TopDonor;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class TopDonorManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Component FUTURE_DONATOR = Component.literal("A Future Donator");

    // Note: We look for the null UUID in the datapack
    private static final UUID ANONYMOUS_PLAYER_ID = Util.NIL_UUID;

    public void pollTopDonors() {
        UUID[] topDonorUuids = DonationConfigs.TOP_DONORS.getTopDonorUuids();

        CompletableFuture.supplyAsync(() -> DonationRequests.get().getTopDonors(topDonorUuids.length), Util.nonCriticalIoPool())
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
                String fallbackName = fallbacks.isEmpty() ? "Anonymous" : fallbacks.getLast();
                applyToEntity(entity, minecraftName, Component.literal(fallbackName), donor.total(), donor.isAnonymous());
            } else {
                clearEntity(entity);
            }
        }
    }

    private void applyToEntity(Entity entity, @Nullable String minecraftName, Component fallbackName, double total, boolean anonymous) {
        ResolvableProfile profile = createProfile(minecraftName, anonymous);

        modifyEntityData(entity, output -> {
            if (profile != null) {
                output.store("profile", ResolvableProfile.CODEC, profile);
            } else {
                output.discard("profile");
            }
            Component suffix = Component.literal(" - ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.format("$%.2f", total)).withStyle(ChatFormatting.GREEN));
            output.store("name_suffix", ComponentSerialization.CODEC, suffix);
        });

        if (anonymous) {
            entity.setCustomName(fallbackName);
        } else if (minecraftName != null) {
            entity.setCustomName(null);
        } else {
            entity.setCustomName(fallbackName);
        }
        entity.setCustomNameVisible(true);
    }

    @Nullable
    private static ResolvableProfile createProfile(@Nullable String minecraftName, boolean anonymous) {
        if (anonymous) {
            return new ResolvableProfile(
                    Optional.empty(),
                    Optional.of(ANONYMOUS_PLAYER_ID),
                    new PropertyMap()
            );
        } else if (minecraftName != null) {
            return new ResolvableProfile(
                    Optional.of(minecraftName),
                    Optional.empty(),
                    new PropertyMap()
            );
        }
        return null;
    }

    private void clearEntity(Entity entity) {
        modifyEntityData(entity, output -> {
            output.discard("profile");
            output.store("name_suffix", ComponentSerialization.CODEC, CommonComponents.EMPTY);
        });
        entity.setCustomName(FUTURE_DONATOR);
    }

    private static void modifyEntityData(Entity entity, Consumer<ValueOutput> modifier) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(entity.problemPath(), LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, entity.registryAccess());
            entity.saveWithoutId(output);

            modifier.accept(output);

            CompoundTag data = output.buildResult();
            entity.load(TagValueInput.create(reporter, entity.registryAccess(), data));
        }
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
        ResourceLocation dimensionId = ResourceLocation.parse(DonationConfigs.TOP_DONORS.dimension.get());
        ResourceKey<Level> dimensionType = ResourceKey.create(Registries.DIMENSION, dimensionId);
        ServerLevel world = server.getLevel(dimensionType);
        if (world == null) {
            LOGGER.error("Failed to find dimension : " + DonationConfigs.TOP_DONORS.dimension.get());
            world = server.overworld();
        }
        return world;
    }
}
