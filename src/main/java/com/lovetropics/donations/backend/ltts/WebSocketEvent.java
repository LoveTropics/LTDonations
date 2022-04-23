package com.lovetropics.donations.backend.ltts;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.lovetropics.donations.backend.ltts.json.Donation;
import com.lovetropics.donations.backend.ltts.json.EventAction;
import com.lovetropics.donations.backend.ltts.json.WebSocketEventData;
import com.lovetropics.donations.backend.ltts.json.WhitelistEvent;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class WebSocketEvent<T> {

	private static final Gson GSON = new Gson();

	private static final Map<String, WebSocketEvent<?>> EVENTS = new HashMap<>();

	public static final WebSocketEvent<Donation> DONATION = register("donation", Donation.class)
			.on(EventAction.create, DonationHandler::queueDonation);

	private static final Function<MinecraftServer, CommandSourceStack> DUMMY_SOURCE = server -> new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, server.getLevel(Level.OVERWORLD), 2, "DumbCodeFix", new TextComponent(""), server, null);
	public static final WebSocketEvent<WhitelistEvent> WHITELIST = register("whitelist", WhitelistEvent.class)
			.on(EventAction.create, e -> {
				if (!e.name.matches("[a-zA-z0-9_]+")) {
					System.out.println("YEET: " + e.name);
					return;
				}
				MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
				CompletableFuture.supplyAsync(() -> server.getProfileCache().get(e.name))
					.thenAcceptAsync(profile -> {
						if (profile.isEmpty()) return;
						UserWhiteList whitelist = server.getPlayerList().getWhiteList();
						UserWhiteListEntry entry = new UserWhiteListEntry(profile.get());
						if (e.type == WhitelistEvent.Type.whitelist && !whitelist.isWhiteListed(profile.get())) {
							System.out.println("Whitelisting user: " + profile.get());
							whitelist.add(entry);
						} else if (e.type == WhitelistEvent.Type.blacklist && whitelist.isWhiteListed(profile.get())) {
							System.out.println("Un-whitelisting user: " + profile.get());
							whitelist.remove(entry);
							server.kickUnlistedPlayers(DUMMY_SOURCE.apply(server));
						}
					}, server)
					.thenRunAsync(() -> DonationRequests.get().ackWhitelist(e.name, e.type));
			});

	private static <T> WebSocketEvent<T> register(String key, Class<T> type) {
		WebSocketEvent<T> ret = new WebSocketEvent<>(type);
		EVENTS.put(key, ret);
		return ret;
	}

	private final TypeToken<T> type;
	private final EnumMap<EventAction, Consumer<T>> actions = new EnumMap<>(EventAction.class);

	private WebSocketEvent(Class<T> type) {
		this(TypeToken.get(type));
	}

	private WebSocketEvent<T> on(EventAction action, Consumer<T> callback) {
		if (this.actions.putIfAbsent(action, callback) != null) {
			throw new IllegalArgumentException("Cannot add duplicate event action handler for " + action + " on event " + type);
		}
		return this;
	}

	private WebSocketEvent(TypeToken<T> type) {
		this.type = type;
	}

	private T parse(JsonObject payload) {
		return GSON.<T>fromJson(payload, type.getType()); 
	}

	private void act(EventAction action, JsonObject payload) {
		act(action, parse(payload));
	}
	
	public void act(EventAction action, T payload) {
		Consumer<T> callback = actions.get(action);
		if (callback == null) {
			throw new IllegalArgumentException("Unhandled event action " + action + " on event " + type);
		}
		callback.accept(payload);
	}

	public static void handleEvent(JsonObject json) {
		WebSocketEventData data = GSON.fromJson(json, WebSocketEventData.class);
		WebSocketEvent<?> event = EVENTS.get(data.type);
		// Discard unknown events
		if (event != null) {
			event.act(data.crud, data.payload);
		}
	}
}
