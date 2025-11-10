package com.lovetropics.donations.backend.ltts;

import com.lovetropics.donations.backend.ltts.json.Donation;
import com.lovetropics.donations.backend.ltts.json.WhitelistEvent;
import com.lovetropics.lib.techstack.Crud;
import com.lovetropics.lib.techstack.TechstackEventSubscriber;
import com.mojang.serialization.Codec;
import net.minecraft.Util;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class WebSocketEvent<T> {
    private static final Map<String, WebSocketEvent<?>> EVENTS = new HashMap<>();

    public static final WebSocketEvent<Donation> DONATION = register("donation", Donation.CODEC)
            .on(Crud.CREATE, DonationHandler::queueDonation);

    private static final Function<MinecraftServer, CommandSourceStack> DUMMY_SOURCE = server -> new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, server.getLevel(Level.OVERWORLD), 2, "DumbCodeFix", CommonComponents.EMPTY, server, null);
    public static final WebSocketEvent<WhitelistEvent> WHITELIST = register("whitelist", WhitelistEvent.CODEC)
            .on(Crud.CREATE, e -> {
                if (!e.name().matches("[a-zA-z0-9_]+")) {
                    System.out.println("YEET: " + e.name());
                    return;
                }
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                CompletableFuture.supplyAsync(() -> server.getProfileCache().get(e.name()), Util.backgroundExecutor())
                        .thenAcceptAsync(profile -> {
                            if (profile.isEmpty()) {
                                return;
                            }
                            UserWhiteList whitelist = server.getPlayerList().getWhiteList();
                            UserWhiteListEntry entry = new UserWhiteListEntry(profile.get());
                            if (e.type() == WhitelistEvent.Type.WHITELIST && !whitelist.isWhiteListed(profile.get())) {
                                System.out.println("Whitelisting user: " + profile.get());
                                whitelist.add(entry);
                            } else if (e.type() == WhitelistEvent.Type.BLACKLIST && whitelist.isWhiteListed(profile.get())) {
                                System.out.println("Un-whitelisting user: " + profile.get());
                                whitelist.remove(entry);
                                server.kickUnlistedPlayers(DUMMY_SOURCE.apply(server));
                            }
                        }, server)
                        .thenRunAsync(() -> DonationRequests.get().ackWhitelist(e.name(), e.type()), Util.ioPool());
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
