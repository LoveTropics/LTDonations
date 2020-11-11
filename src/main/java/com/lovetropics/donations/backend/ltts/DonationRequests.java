package com.lovetropics.donations.backend.ltts;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.POST;

import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.lovetropics.donations.DonationConfigs;
import com.lovetropics.donations.RequestHelper;
import com.lovetropics.donations.backend.ltts.json.PendingEventList;
import com.lovetropics.donations.backend.ltts.json.WhitelistEvent;

public class DonationRequests extends RequestHelper {

	public DonationRequests() {
		super("https://donations.lovetropics.com/", DonationConfigs.TECH_STACK.authKey::get);
	}

	public List<WhitelistEvent> getUnprocessedEvents() {
		return request(GET, "players/pendingevents", new TypeToken<PendingEventList<WhitelistEvent>>() {})
				.orThrow()
				.events;
	}

	public void ackWhitelist(String name, WhitelistEvent.Type type) {
		request(POST, "players/ack/" + type.name() + "/" + name)
			.orThrow();
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
}
