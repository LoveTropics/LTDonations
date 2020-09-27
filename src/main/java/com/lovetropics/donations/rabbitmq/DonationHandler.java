package com.lovetropics.donations.rabbitmq;

import com.lovetropics.donations.Donation;
import com.lovetropics.donations.DonationLangKeys;
import com.lovetropics.donations.LTDonations;
import com.lovetropics.donations.TickerDonation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.text.NumberFormat;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = LTDonations.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DonationHandler {

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
        Bunny.checkAndReestablishConnection();

        // TODO check and make sure we are in rabbitmq mode
        if (tick >= donationLastPolledTick + TICKS_BEFORE_POLL && LTDonations.donationsPending()) {
            final Donation donation = LTDonations.getDonation();
            if (donation == null) {
                System.out.println("NO DONATIONS");
                System.out.println(LTDonations.DONATION_QUEUE.size());
                return;
            }

            final TranslationTextComponent msg = DonationLangKeys.NEW_DONATION.format(
                    TextFormatting.AQUA + donation.getName() + TextFormatting.RESET.toString(),
                    TextFormatting.GREEN.toString() + NumberFormat.getCurrencyInstance(Locale.US).format(donation.getAmount()) + TextFormatting.RESET);

            server.getPlayerList().getPlayers().forEach(p -> p.sendMessage(msg));
            // TODO decide whether we want to phase out this class entirely or not
            //TickerDonation.triggerDonation();

            donationLastPolledTick = tick;
        }

    }
}

