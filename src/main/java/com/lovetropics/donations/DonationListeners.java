package com.lovetropics.donations;

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
    }

    private static void announceDonation(final MinecraftServer server, final String name, final double amount) {
        if (name.isBlank()) {
            return;
        }
        for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.displayClientMessage(DonationLangKeys.NEW_DONATION.format(
                    ChatFormatting.AQUA + name + ChatFormatting.RESET,
                    ChatFormatting.GREEN + LTDonations.CURRENCY_FORMAT.format(amount) + ChatFormatting.RESET
            ), false);
        }
    }

    public static void updateState(final MinecraftServer server, final DonationState state, final boolean initial) {
        for (final DonationStateListener listener : STATE_LISTENERS) {
            listener.handleState(server, state, initial);
        }
    }

    public static void triggerDonation(final MinecraftServer server, final String name, final double amount) {
        for (final DonationListener listener : LISTENERS) {
            listener.handleDonation(server, name, amount);
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
