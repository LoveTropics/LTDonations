package com.lovetropics.donations;

import net.minecraft.server.MinecraftServer;

public class GlobalDonationTracker implements DonationListener {

    private double totalAmount;

    GlobalDonationTracker() {
    }

    public void handleDonation(MinecraftServer server, String name, double amount, DonationTotals totals) {
        this.addDonation(amount);
    }

    public void addDonation(double amount) {
        this.totalAmount += amount;
    }

    public double getTotalAmount() {
        return this.totalAmount;
    }

}
