package com.lovetropics.donations.backend.ltts;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.lovetropics.donations.backend.ltts.json.Donation;
import com.lovetropics.donations.backend.ltts.json.EventAction;
import com.lovetropics.donations.backend.ltts.json.WebSocketEventData;
import com.lovetropics.donations.backend.ltts.json.WhitelistEvent;
import com.mojang.authlib.GameProfile;

import net.minecraft.command.CommandSource;
import net.minecraft.command.ICommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.WhiteList;
import net.minecraft.server.management.WhitelistEntry;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class WebSocketEvent<T> {

	private static final Gson GSON = new Gson();
	private static final DonationRequests REQUESTS = new DonationRequests();

	private static final Map<String, WebSocketEvent<?>> EVENTS = new HashMap<>();

	public static final WebSocketEvent<Donation> DONATION = register("donation", Donation.class)
			.on(EventAction.create, DonationHandler::queueDonation);

	private static final Function<MinecraftServer, CommandSource> DUMMY_SOURCE = server -> new CommandSource(ICommandSource.DUMMY, Vec3d.ZERO, Vec2f.ZERO, server.getWorld(DimensionType.OVERWORLD), 2, "DumbCodeFix", new StringTextComponent(""), server, null);
	public static final WebSocketEvent<WhitelistEvent> WHITELIST = register("whitelist", WhitelistEvent.class)
			.on(EventAction.create, e -> {
				MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
				GameProfile profile = server.getPlayerProfileCache().getGameProfileForUsername(e.name);
				if (profile == null) return;
				WhiteList whitelist = server.getPlayerList().getWhitelistedPlayers();
				WhitelistEntry entry = new WhitelistEntry(profile);
				if (e.type == WhitelistEvent.Type.whitelist && !whitelist.isWhitelisted(profile)) {
					System.out.println("Whitelisting user: " + profile);
					whitelist.addEntry(entry);
				} else if (e.type == WhitelistEvent.Type.blacklist && whitelist.isWhitelisted(profile)) {
					System.out.println("Un-whitelisting user: " + profile);
					whitelist.removeEntry(entry);
					server.kickPlayersNotWhitelisted(DUMMY_SOURCE.apply(server));
				}
				REQUESTS.ackWhitelist(e.name, e.type);
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

	public static void handleEvent(String dataStr) {
		WebSocketEventData data = GSON.fromJson(dataStr, WebSocketEventData.class);
		WebSocketEvent<?> event = EVENTS.get(data.type);
		// Discard unknown events
		if (event != null) {
			event.act(data.crud, data.payload);
		}
	}
}
