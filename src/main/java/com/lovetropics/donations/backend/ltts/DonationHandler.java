package com.lovetropics.donations.backend.ltts;

import java.util.Queue;

import com.google.common.collect.Queues;
import com.lovetropics.donations.LTDonations;
import com.lovetropics.donations.backend.ltts.json.Donation;
import com.lovetropics.donations.backend.tiltify.TickerDonation;
import com.lovetropics.donations.monument.MonumentManager;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = LTDonations.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DonationHandler {

    public static final Queue<Donation> DONATION_QUEUE = Queues.newPriorityBlockingQueue();
    // TODO configurate
    // 3 seconds between fireworks
    private static final int TICKS_BEFORE_POLL = 60;
    private static int donationLastPolledTick;

    public static MonumentManager monument;

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        final int tick = server.getTickCounter();

        // This is good to have...but we shouldn't need it hopefully
        LTDonations.WEBSOCKET.checkAndCycleConnection();

        // TODO check and make sure we are in web socket mode
        if (tick >= donationLastPolledTick + TICKS_BEFORE_POLL && donationsPending()) {
            final Donation donation = getDonation();
            if (donation == null) {
                return;
            }

            // TODO decide whether we want to phase out this class entirely or not
            TickerDonation.sendDonationMessage(donation.getNameShown(), donation.getAmount());
            TickerDonation.triggerDonation();
            
            if (monument != null) {
            	monument.updateMonument(donation.getTotal(), false);
            }

            donationLastPolledTick = tick;
        }
        
        if (monument != null) {
        	monument.tick(server);
        }
    }

    public static void close() {
    	LTDonations.WEBSOCKET.close();
    	monument = null;
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

