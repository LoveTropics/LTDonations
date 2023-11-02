package com.lovetropics.donations.backend.ltts;

import com.lovetropics.donations.DonationConfigs;
import com.lovetropics.donations.backend.ltts.json.FullDonationState;
import com.lovetropics.donations.backend.ltts.json.TopDonor;
import com.lovetropics.donations.backend.ltts.json.WhitelistEvent;
import com.mojang.serialization.Codec;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;


public class DonationRequests {
    private static final DonationRequests INSTANCE = new DonationRequests(
            DonationConfigs.TECH_STACK.apiUrl,
            DonationConfigs.TECH_STACK.authKey
	);

    private static final Codec<List<WhitelistEvent>> WHITELIST_EVENTS_CODEC = WhitelistEvent.CODEC.listOf().fieldOf("events").codec();
	private static final Comparator<TopDonor> TOP_DONOR_COMPARATOR = Comparator.comparingDouble(TopDonor::total).reversed();
	private static final Codec<List<TopDonor>> TOP_DONORS_CODEC = TopDonor.CODEC.listOf().fieldOf("donors").codec().xmap(
			topDonors -> {
				final List<TopDonor> sorted = new ArrayList<>(topDonors);
				sorted.sort(TOP_DONOR_COMPARATOR);
				return List.copyOf(sorted);
			},
			topDonors -> topDonors
	);

	private final RequestHelper requests;

    private DonationRequests(final Supplier<String> baseURL, final Supplier<String> token) {
		requests = new RequestHelper(baseURL, token);
	}

	public static DonationRequests get() {
		return INSTANCE;
	}

	public List<WhitelistEvent> getUnprocessedEvents() {
		return requests.get("players/pendingevents", WHITELIST_EVENTS_CODEC).orThrow();
	}

	public void ackWhitelist(final String name, final WhitelistEvent.Type type) {
		try {
			requests.post("players/ack/" + type.name() + "/" + URLEncoder.encode(name, StandardCharsets.US_ASCII))
				.orThrow();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private static int id = 0;
	public void fakeWhitelist() {
		requests.post("players/whitelist/tterrag" + id++).orThrow();
	}
	public void fakeBlacklist() {
		requests.post("players/blacklist/tterrag" + id--).orThrow();
	}

	public FullDonationState getTotalDonations() {
		return requests.get("donations/total", FullDonationState.CODEC).orThrow();
	}

	public List<TopDonor> getTopDonors(final int count) {
		return requests.get("donors/top/" + count + "?separate_anonymous=true", TOP_DONORS_CODEC).orThrow();
	}
}
