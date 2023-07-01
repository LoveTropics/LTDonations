package com.lovetropics.donations.backend.ltts;

import com.google.common.collect.Queues;
import com.lovetropics.donations.DonationListeners;
import com.lovetropics.donations.LTDonations;
import com.lovetropics.donations.TopDonorManager;
import com.lovetropics.donations.backend.ltts.json.Donation;
import com.lovetropics.donations.backend.ltts.json.EventAction;
import com.lovetropics.donations.monument.MonumentManager;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = LTDonations.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DonationHandler {

    public static final Queue<Donation> DONATION_QUEUE = Queues.newPriorityBlockingQueue();
    // TODO configurate
    // 3 seconds between fireworks
    private static final int TICKS_BEFORE_POLL = SharedConstants.TICKS_PER_SECOND * 3;
    private static int donationLastPolledTick;

    private static final int TOP_DONOR_POLL_INTERVAL = SharedConstants.TICKS_PER_MINUTE;
    private static int nextTopDonorPollTick;

    private static boolean donatorsDirty = true;

    public static MonumentManager monument;
    public static TopDonorManager topDonors;

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        final int tick = server.getTickCount();

        LTDonations.WEBSOCKET.get().tick();

        // TODO check and make sure we are in web socket mode
        if (tick >= donationLastPolledTick + TICKS_BEFORE_POLL && donationsPending()) {
            final Donation donation = getDonation();
            if (donation == null) {
                return;
            }

            DonationListeners.triggerDonation(server, donation.getName(), donation.getAmount());

            donationLastPolledTick = tick;
            donatorsDirty = true;
        }

        if ((tick >= nextTopDonorPollTick || donatorsDirty) && topDonors != null) {
            topDonors.pollTopDonors();

            nextTopDonorPollTick = tick + TOP_DONOR_POLL_INTERVAL;
            donatorsDirty = false;
        }

        if (monument != null) {
        	monument.tick(server);
        }

        // FIXME TEMP ASK FOR MISSED WHITELISTS EVERY 5 MINUTES
        if (tick % (SharedConstants.TICKS_PER_MINUTE * 5) == 0) {
			CompletableFuture.supplyAsync(() -> DonationRequests.get().getUnprocessedEvents())
				.thenAcceptAsync(events -> events.forEach(e -> WebSocketEvent.WHITELIST.act(EventAction.create, e)), server);
        }
    }

    public static void close() {
        if (monument != null) {
            DonationListeners.unregister(monument);
        }
    	monument = null;
    	topDonors = null;
    }

    public static void queueDonation(final Donation donation) {
        DONATION_QUEUE.offer(donation);
    }

    public static Donation getDonation() {
        return DONATION_QUEUE.poll();
    }

    public static boolean donationsPending() {
        return !DONATION_QUEUE.isEmpty();
    }
}

