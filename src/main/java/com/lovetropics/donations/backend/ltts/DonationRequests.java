package com.lovetropics.donations.backend.ltts;

import com.google.common.base.Charsets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.lovetropics.donations.DonationConfigs;
import com.lovetropics.donations.RequestHelper;
import com.lovetropics.donations.backend.ltts.json.PendingEventList;
import com.lovetropics.donations.backend.ltts.json.TopDonor;
import com.lovetropics.donations.backend.ltts.json.WhitelistEvent;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.POST;

public class DonationRequests extends RequestHelper {
	private static final DonationRequests INSTANCE = new DonationRequests(
			DonationConfigs.TECH_STACK.apiUrl::get,
			DonationConfigs.TECH_STACK.authKey::get
	);

	private DonationRequests(Supplier<String> baseURL, Supplier<String> token) {
		super(baseURL, token);
	}

	public static DonationRequests get() {
		return INSTANCE;
	}

	public List<WhitelistEvent> getUnprocessedEvents() {
		return request(GET, "players/pendingevents", new TypeToken<PendingEventList<WhitelistEvent>>() {})
				.orThrow()
				.events;
	}

	public void ackWhitelist(String name, WhitelistEvent.Type type) {
		try {
			request(POST, "players/ack/" + type.name() + "/" + URLEncoder.encode(name, Charsets.US_ASCII.name()))
				.orThrow();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static int id = 0;
	public void fakeWhitelist() {
		request(POST, "players/whitelist/tterrag" + id++).orThrow();
	}
	public void fakeBlacklist() {
		request(POST, "players/blacklist/tterrag" + id--).orThrow();
	}

	public double getTotalDonations() {
		return request(GET, "donations/total", JsonObject.class)
				.orThrow()
				.get("total")
				.getAsDouble();
	}

	public List<TopDonor> getTopDonors(int count) {
		JsonArray topDonorArray = request(GET, "donors/top/" + count, JsonObject.class)
				.orThrow()
				.getAsJsonArray("donors");

		List<TopDonor> topDonors = new ArrayList<>(topDonorArray.size());

		for (JsonElement element : topDonorArray) {
			JsonObject donorRoot = element.getAsJsonObject();
			UUID uuid = UUID.fromString(donorRoot.get("uuid").getAsString());
			JsonElement nameElement = donorRoot.get("minecraft_name");
			String minecraftName = nameElement.isJsonNull() ? null : nameElement.getAsString();
			double total = donorRoot.get("total").getAsDouble();
			topDonors.add(new TopDonor(uuid, minecraftName, total));
		}

		topDonors.sort(Comparator.<TopDonor>comparingDouble(value -> value.total).reversed());

		return topDonors;
	}
}
