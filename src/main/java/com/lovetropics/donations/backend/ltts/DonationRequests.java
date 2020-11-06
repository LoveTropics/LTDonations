package com.lovetropics.donations.backend.ltts;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lovetropics.donations.DonationConfigs;
import com.lovetropics.donations.RequestHelper;
import com.lovetropics.donations.backend.ltts.json.PendingEventList;
import com.lovetropics.donations.backend.ltts.json.WhitelistEvent;
import com.mojang.datafixers.util.Either;

import net.minecraft.util.Unit;

public class DonationRequests extends RequestHelper {

	public DonationRequests() {
		super(DonationConfigs.TECH_STACK.authKey::get);
	}

	@Override
	protected HttpURLConnection getAuthorizedConnection(String method, String address) throws IOException {
		return super.getAuthorizedConnection(method, "https://2020.lovetropics.com/" + address);
	}

	protected Either<Unit, String> request(String method, String endpoint) {
		return request(method, endpoint, s -> Unit.INSTANCE);
	}

	protected <T> Either<T, String> request(String method, String endpoint, Class<T> clazz) {
		return request(method, endpoint, TypeToken.get(clazz));
	}

	protected <T> Either<T, String> request(String method, String endpoint, TypeToken<T> clazz) {
		return request(method, endpoint, s -> new Gson().<T>fromJson(s, clazz.getType()));
	}

	protected <T> Either<T, String> request(String method, String endpoint, Function<String, T> parser) {
		try {
			HttpURLConnection con = getAuthorizedConnection(method, endpoint);
			try {
				String payload = readInput(con.getInputStream(), false);
				System.out.println("REST Response: " + payload);
				return Either.left(parser.apply(payload));
			} catch (IOException e) {
				return Either.right(readInput(con.getErrorStream(), true));
			}
		} catch (IOException e) {
			return Either.right(e.toString());
		}
	}

	public List<WhitelistEvent> getUnprocessedEvents() {
		return request("GET", "players/pendingevents", new TypeToken<PendingEventList<WhitelistEvent>>() {})
				.orThrow()
				.events;
	}

	public List<String> getPendingWhitelists() {
		try {
			HttpURLConnection con = getAuthorizedConnection("POST", "players/ack/whitlitterrag" + (int) (Math.random() * 10000));
			String payload;
			try {
				payload = readInput(con.getInputStream(), false);
			} catch (IOException e) {
				payload = readInput(con.getErrorStream(), true);
			}
			System.out.println(payload);
			return null;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static int id = 0;
	public void fakeWhitelist() {
		request("POST", "players/whitelist/tterrag" + id++).orThrow();
	}
	public void fakeBlacklist() {
		request("POST", "players/blacklist/tterrag" + id--).orThrow();
	}
}
