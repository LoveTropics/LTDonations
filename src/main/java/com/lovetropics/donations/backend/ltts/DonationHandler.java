package com.lovetropics.donations.backend.ltts;

import com.google.common.collect.Queues;
import com.lovetropics.donations.*;
import com.lovetropics.donations.backend.ltts.json.Donation;
import com.lovetropics.donations.backend.ltts.json.FullDonationState;
import com.lovetropics.donations.backend.ltts.json.EventAction;
import com.lovetropics.donations.monument.MonumentManager;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = LTDonations.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DonationHandler {
    private static final Queue<Donation> DONATION_QUEUE = Queues.newPriorityBlockingQueue();

    // TODO configurate
    // 3 seconds between fireworks
    private static final int TICKS_BEFORE_POLL = SharedConstants.TICKS_PER_SECOND * 3;
    private static int nextDonationPollTick;

    private static final int TOP_DONOR_POLL_INTERVAL = SharedConstants.TICKS_PER_MINUTE;
    private static int nextTopDonorPollTick;

    private static boolean topDonatorsDirty = true;

    private static final State STATE = new State();

    @Nullable
    private static Donation lastDonation;

    @Nullable
    private static TopDonorManager topDonors;

    @SubscribeEvent
    public static void tick(final TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        final MinecraftServer server = event.getServer();
        final int tick = server.getTickCount();

        LTDonations.websocket().tick();

        if (tick >= nextDonationPollTick) {
            final Donation donation = DONATION_QUEUE.poll();
            if (donation != null) {
                applyFullState(server, donation.fullState(), false);
                DonationListeners.triggerDonation(server, donation.getNameShown(), donation.amount(), STATE);
                nextDonationPollTick = tick + TICKS_BEFORE_POLL;
            }
        }

        if (topDonors != null && (tick >= nextTopDonorPollTick || topDonatorsDirty)) {
            topDonors.pollTopDonors();

            nextTopDonorPollTick = tick + TOP_DONOR_POLL_INTERVAL;
            topDonatorsDirty = false;
        }

        MonumentManager.get(server).tick(server);

        // FIXME TEMP ASK FOR MISSED WHITELISTS EVERY 5 MINUTES
        if (tick % (SharedConstants.TICKS_PER_MINUTE * 5) == 0) {
            CompletableFuture.supplyAsync(() -> DonationRequests.get().getUnprocessedEvents())
                    .thenAcceptAsync(events -> events.forEach(e -> WebSocketEvent.WHITELIST.act(EventAction.create, e)), server);
        }
    }

    public static void fetchFullState(final MinecraftServer server, final boolean initial) {
        final DonationRequests startupRequests = DonationRequests.get();
        CompletableFuture.supplyAsync(startupRequests::getUnprocessedEvents)
                .thenAcceptAsync(events -> events.forEach(e -> WebSocketEvent.WHITELIST.act(EventAction.create, e)), server);
        CompletableFuture.supplyAsync(startupRequests::getTotalDonations)
                .thenAcceptAsync(total -> applyFullState(server, total, initial), server);
    }

    public static void initialize(final MinecraftServer server) {
        final MonumentManager monuments = MonumentManager.get(server);
        DonationListeners.register(monuments);

        topDonors = new TopDonorManager();
        topDonors.pollTopDonors();

        fetchFullState(server, true);
    }

    private static void applyFullState(final MinecraftServer server, final FullDonationState data, final boolean initial) {
        STATE.set(DonationGroup.ALL, data.total(), 0);
        STATE.set(DonationGroup.TEAM_CENTS, data.teamCentsTotal(), data.teamCentsCount());
        STATE.set(DonationGroup.TEAM_NO_CENTS, data.teamNoCentsTotal(), data.teamNoCentsCount());
        STATE.set(DonationGroup.TEAM_NICE, 0.0, data.teamNiceCount());

        MonumentManager.get(server).update(STATE, initial);
    }

    public static void close(final MinecraftServer server) {
        final MonumentManager monuments = MonumentManager.get(server);
        DonationListeners.unregister(monuments);
        topDonors = null;
    }

    public static void queueDonation(final Donation donation) {
        DONATION_QUEUE.offer(donation);
        topDonatorsDirty = true;
        lastDonation = donation;
    }

    public static DonationState state() {
        return STATE;
    }

    @Nullable
    public static Donation getLastDonation() {
        return lastDonation;
    }

    private static class State implements DonationState {
        private final Map<DonationGroup, Double> amounts = new EnumMap<>(DonationGroup.class);
        private final Map<DonationGroup, Integer> counts = new EnumMap<>(DonationGroup.class);

        public void set(final DonationGroup group, final double total, final int count) {
            amounts.put(group, total);
            counts.put(group, count);
        }

        @Override
        public double getAmount(final DonationGroup group) {
            return amounts.getOrDefault(group, 0.0);
        }

        @Override
        public int getCount(final DonationGroup group) {
            return counts.getOrDefault(group, 0);
        }
    }
}

