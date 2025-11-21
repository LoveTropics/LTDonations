package com.lovetropics.donations.backend.ltts;

import com.lovetropics.donations.DonationConfigs;
import com.lovetropics.donations.backend.ltts.json.Donation;
import com.lovetropics.donations.backend.ltts.json.WhitelistEvent;
import com.lovetropics.lib.permission.PermissionsApi;
import com.lovetropics.lib.permission.role.Role;
import com.lovetropics.lib.techstack.Crud;
import com.lovetropics.lib.techstack.TechstackEventSubscriber;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StringUtil;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class WebSocketEvent<T> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<String, WebSocketEvent<?>> EVENTS = new HashMap<>();

    public static final WebSocketEvent<Donation> DONATION = register("donation", Donation.CODEC)
            .on(Crud.CREATE, DonationHandler::queueDonation);

    public static final WebSocketEvent<WhitelistEvent> WHITELIST = register("whitelist", WhitelistEvent.CODEC)
            .on(Crud.CREATE, event -> {
                if (!StringUtil.isValidPlayerName(event.profileName())) {
                    LOGGER.warn("Ignoring requested whitelist for invalid player name: {}", event.profileName());
                    return;
                }
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                CompletableFuture<Optional<GameProfile>> profileFuture;
                if (event.profileId().isPresent()) {
                    profileFuture = CompletableFuture.completedFuture(Optional.of(
                            new GameProfile(event.profileId().get(), event.profileName())
                    ));
                } else {
                    // The techstack might fail its player name lookup - but we'll retry until it's successful
                    // TODO: Eventually we should ensure that the techstack never fails and only sends fully formed whitelist events
                    profileFuture = CompletableFuture.supplyAsync(
                            () -> server != null ? server.getProfileCache().get(event.profileName()) : Optional.empty(),
                            Util.nonCriticalIoPool()
                    );
                }
                profileFuture.thenAcceptAsync(profile -> {
                    if (profile.isEmpty()) {
                        LOGGER.warn("Ignoring requested whitelist for profile that does not exist: {}", event.profileName());
                        return;
                    }
                    String roleId = DonationConfigs.WHITELIST.role.get();
                    Role role = PermissionsApi.provider().get(roleId);
                    if (role == null) {
                        LOGGER.warn("No role exists with id: {}, ignoring whitelist request for {} for now", roleId, event.profileName());
                        return;
                    }
                    if (event.type() == WhitelistEvent.Type.WHITELIST) {
                        LOGGER.info("Adding {} role to {} ({})", role, profile.get().getName(), profile.get().getId());
                        PermissionsApi.modifier().addRoleTo(profile.get().getId(), role);
                    } else if (event.type() == WhitelistEvent.Type.BLACKLIST) {
                        LOGGER.info("Removing {} role from {} ({})", role, profile.get().getName(), profile.get().getId());
                        PermissionsApi.modifier().removeRoleFrom(profile.get().getId(), role);
                    }
                    Util.nonCriticalIoPool().execute(() ->
                            DonationRequests.get().ackWhitelist(event.profileName(), event.type())
                    );
                }, server);
            });

    private static <T> WebSocketEvent<T> register(String key, Codec<T> type) {
        WebSocketEvent<T> ret = new WebSocketEvent<>(type);
        EVENTS.put(key, ret);
        return ret;
    }

    private final Codec<T> codec;
    private final EnumMap<Crud, Consumer<T>> actions = new EnumMap<>(Crud.class);

    private WebSocketEvent(Codec<T> codec) {
        this.codec = codec;
    }

    private WebSocketEvent<T> on(Crud action, Consumer<T> callback) {
        if (this.actions.putIfAbsent(action, callback) != null) {
            throw new IllegalArgumentException("Cannot add duplicate event action handler for " + action + " on event " + codec);
        }
        return this;
    }

    private WebSocketEvent<T> onServerThread(Crud action, Consumer<T> callback) {
        return on(action, event -> {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.execute(() -> callback.accept(event));
            }
        });
    }

    public void act(Crud action, T payload) {
        Consumer<T> callback = actions.get(action);
        if (callback == null) {
            throw new IllegalArgumentException("Unhandled event action " + action + " on event " + codec);
        }
        callback.accept(payload);
    }

    public static void addSubscribersTo(TechstackEventSubscriber.Builder subscriber) {
        EVENTS.forEach((id, event) -> {
            addSubscribersTo(subscriber, id, event);
        });
    }

    private static <T> void addSubscribersTo(TechstackEventSubscriber.Builder subscriber, String id, WebSocketEvent<T> event) {
        event.actions.forEach((crud, handler) ->
                subscriber.subscribe(crud, id, event.codec, handler)
        );
    }
}
