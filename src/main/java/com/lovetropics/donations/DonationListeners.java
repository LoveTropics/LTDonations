package com.lovetropics.donations;

import com.lovetropics.donations.trigger.DonationTriggerConfigs;
import com.lovetropics.donations.trigger.DonationTriggerHolder;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

public class DonationListeners {
    private static final Set<DonationListener> LISTENERS = new ReferenceOpenHashSet<>();
    private static final Set<DonationStateListener> STATE_LISTENERS = new ReferenceOpenHashSet<>();

    static {
        LISTENERS.add(DonationListeners::announceDonation);
        STATE_LISTENERS.add(new DonationScoreboard());

        LISTENERS.add((server, details) -> {
            for (DonationTriggerHolder holder : DonationTriggerConfigs.REGISTRY) {
                holder.trigger().handleDonation(server, details);
            }
        });
    }

    private static void announceDonation(final MinecraftServer server, final DonationListener.Details details) {
        if (details.name().isBlank()) {
            return;
        }
        for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(DonationLangKeys.NEW_DONATION.format(
                    ChatFormatting.AQUA + details.name() + ChatFormatting.RESET,
                    ChatFormatting.GREEN + LTDonations.CURRENCY_FORMAT.format(details.amount()) + ChatFormatting.RESET
            ), false);
        }
    }

    public static void updateState(final MinecraftServer server, final DonationState state, final boolean initial) {
        for (final DonationStateListener listener : STATE_LISTENERS) {
            listener.handleState(server, state, initial);
        }
    }

    public static void triggerDonation(final MinecraftServer server, final DonationListener.Details details) {
        for (final DonationListener listener : LISTENERS) {
            listener.handleDonation(server, details);
        }
    }

    public static void register(final DonationListener listener) {
        LISTENERS.add(listener);
    }

    public static void register(final DonationStateListener listener) {
        STATE_LISTENERS.add(listener);
    }

    public static void unregister(final DonationListener listener) {
        LISTENERS.remove(listener);
    }

    public static void unregister(final DonationStateListener listener) {
        STATE_LISTENERS.remove(listener);
    }
}
