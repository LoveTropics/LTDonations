package com.lovetropics.donations.websockets;

import com.google.common.collect.Queues;
import com.google.gson.JsonObject;
import com.lovetropics.donations.Donation;
import com.lovetropics.donations.LTDonations;
import com.lovetropics.donations.TickerDonation;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.Queue;

@Mod.EventBusSubscriber(modid = LTDonations.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DonationHandler {

    public static final Queue<Donation> DONATION_QUEUE = Queues.newPriorityBlockingQueue();
    // TODO configurate
    // 3 seconds between fireworks
    private static final int TICKS_BEFORE_POLL = 60;
    private static int donationLastPolledTick;

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        final int tick = server.getTickCounter();

        // This is good to have...but we shouldn't need it hopefully
        WebSocketHelper.checkAndCycleConnection();

        // TODO check and make sure we are in web socket mode
        if (tick >= donationLastPolledTick + TICKS_BEFORE_POLL && donationsPending()) {
            final Donation donation = getDonation();
            if (donation == null) {
                return;
            }

            // TODO decide whether we want to phase out this class entirely or not
            TickerDonation.sendDonationMessage(donation.getName(), donation.getAmount());
            TickerDonation.triggerDonation();

            donationLastPolledTick = tick;
        }

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

    public static void handlePayload(String payload) {
        final JsonObject obj = WebSocketHelper.parse(payload);
        final String type = obj.get("type").getAsString();
        final String crud = obj.get("crud").getAsString();
        System.out.println("PAYLOAD");
        if (type.equals("state")) {
            System.out.println("state");
        } else if (type.equals("donation")) {
            if (crud.equals("create")) {
                System.out.println("Donation created");
                final Donation donation = Donation.fromJson(obj);
                queueDonation(donation);
            }
        }
    }
}

