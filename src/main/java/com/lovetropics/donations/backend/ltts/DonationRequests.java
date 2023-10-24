package com.lovetropics.donations.backend.ltts;

import com.lovetropics.donations.DonationConfigs;
import com.lovetropics.donations.RequestHelper;
import com.lovetropics.donations.backend.ltts.json.FullDonationState;
import com.lovetropics.donations.backend.ltts.json.TopDonor;
import com.lovetropics.donations.backend.ltts.json.WhitelistEvent;
import com.lovetropics.lib.codec.MoreCodecs;
import com.mojang.serialization.Codec;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import static com.lovetropics.lib.repack.io.netty.handler.codec.http.HttpMethod.GET;
import static com.lovetropics.lib.repack.io.netty.handler.codec.http.HttpMethod.POST;


public class DonationRequests extends RequestHelper {
    private static final DonationRequests INSTANCE = new DonationRequests(
            DonationConfigs.TECH_STACK.apiUrl,
            DonationConfigs.TECH_STACK.authKey
	);

    private static final Codec<List<WhitelistEvent>> WHITELIST_EVENTS_CODEC = WhitelistEvent.CODEC.listOf().fieldOf("events").codec();
    private static final Codec<List<TopDonor>> TOP_DONORS_CODEC = MoreCodecs.sorted(
            TopDonor.CODEC.listOf().fieldOf("donors").codec(),
            Comparator.comparingDouble(TopDonor::total).reversed()
    );

    private DonationRequests(final Supplier<String> baseURL, final Supplier<String> token) {
		super(baseURL, token);
	}

	public static DonationRequests get() {
		return INSTANCE;
	}

	public List<WhitelistEvent> getUnprocessedEvents() {
		return request(GET, "players/pendingevents", WHITELIST_EVENTS_CODEC).orThrow();
	}

	public void ackWhitelist(final String name, final WhitelistEvent.Type type) {
		try {
			request(POST, "players/ack/" + type.name() + "/" + URLEncoder.encode(name, StandardCharsets.US_ASCII))
				.orThrow();
		} catch (final Exception e) {
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

	public FullDonationState getTotalDonations() {
		return request(GET, "donations/total", FullDonationState.CODEC).orThrow();
	}

	public List<TopDonor> getTopDonors(final int count) {
		return request(GET, "donors/top/" + count, TOP_DONORS_CODEC).orThrow();
	}
}
