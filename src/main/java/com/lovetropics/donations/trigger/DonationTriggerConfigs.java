package com.lovetropics.donations.trigger;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.lovetropics.donations.LTDonations;
import com.lovetropics.lib.codec.CodecRegistry;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.Util;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@EventBusSubscriber(modid = LTDonations.MODID)
public class DonationTriggerConfigs {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final CodecRegistry<ResourceLocation, DonationTriggerHolder> REGISTRY = CodecRegistry.resourceLocationKeys();
    private static final FileToIdConverter LISTER = FileToIdConverter.json("donation_trigger");

    @SubscribeEvent
    public static void addReloadListener(AddServerReloadListenersEvent event) {
        RegistryOps<JsonElement> ops = event.getRegistryAccess().createSerializationContext(JsonOps.INSTANCE);
        event.addListener(LTDonations.location("donation_triggers"), (stage, resourceManager, backgroundExecutor, gameExecutor) ->
                CompletableFuture.supplyAsync(() -> listTriggers(ops, resourceManager, backgroundExecutor), backgroundExecutor).thenCompose(f -> f)
                        .thenCompose(stage::wait)
                        .thenAcceptAsync(triggers -> {
                            REGISTRY.clear();
                            triggers.forEach(holder -> REGISTRY.register(holder.id(), holder));
                        }, gameExecutor)
        );
    }

    private static CompletableFuture<List<DonationTriggerHolder>> listTriggers(DynamicOps<JsonElement> ops, ResourceManager resourceManager, Executor executor) {
        List<CompletableFuture<DonationTriggerHolder>> futures = LISTER.listMatchingResources(resourceManager).entrySet().stream()
                .map(entry -> {
                    ResourceLocation path = entry.getKey();
                    ResourceLocation id = LISTER.fileToId(path);
                    Resource resource = entry.getValue();
                    return CompletableFuture.supplyAsync(() -> {
                        DonationTrigger trigger = loadTrigger(ops, path, resource);
                        return trigger != null ? new DonationTriggerHolder(id, trigger) : null;
                    }, executor);
                })
                .toList();
        return Util.sequence(futures).thenApply(holders -> holders.stream().filter(Objects::nonNull).toList());
    }

    @Nullable
    private static DonationTrigger loadTrigger(DynamicOps<JsonElement> ops, ResourceLocation path, Resource resource) {
        try (BufferedReader reader = resource.openAsReader()) {
            return DonationTrigger.CODEC.parse(ops, JsonParser.parseReader(reader))
                    .ifError(error -> LOGGER.error("Failed to load donation trigger at {}: {}", path, error.error()))
                    .resultOrPartial()
                    .orElse(null);
        } catch (IOException | JsonParseException e) {
            LOGGER.error("Failed to load donation trigger at {}", path, e);
            return null;
        }
    }
}
