package com.lovetropics.donations;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Set;

public class DonationListeners {
    private static final Set<DonationListener> LISTENERS = new ReferenceOpenHashSet<>();

    static {
        LISTENERS.add(DonationListeners::announceDonation);
    }

    private static void announceDonation(final MinecraftServer server, final String name, final double amount, final DonationTotals totals) {
        if (name.isBlank()) {
            return;
        }
        for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.displayClientMessage(DonationLangKeys.NEW_DONATION.format(
                    ChatFormatting.AQUA + name + ChatFormatting.RESET,
                    ChatFormatting.GREEN + NumberFormat.getCurrencyInstance(Locale.US).format(amount) + ChatFormatting.RESET
            ), false);
        }
    }

    public static void triggerDonation(final MinecraftServer server, final String name, final double amount, final DonationTotals totals) {
        for (final DonationListener listener : LISTENERS) {
            listener.handleDonation(server, name, amount, totals);
        }
    }

    public static void register(final DonationListener listener) {
        LISTENERS.add(listener);
    }

    public static void unregister(final DonationListener listener) {
        LISTENERS.remove(listener);
    }
}
