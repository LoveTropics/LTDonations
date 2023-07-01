package com.lovetropics.donations.backend.tiltify;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lovetropics.donations.DonationBlockEntity;
import com.lovetropics.donations.DonationConfigs;
import com.lovetropics.donations.DonationLangKeys;
import com.lovetropics.donations.LTDonations;
import com.lovetropics.donations.backend.tiltify.json.JsonDataDonation;
import com.lovetropics.donations.backend.tiltify.json.JsonDataDonationEntry;
import com.lovetropics.donations.backend.tiltify.json.JsonDeserializerDonation;
import com.lovetropics.donations.backend.tiltify.json.JsonDeserializerDonationTotal;
import com.lovetropics.donations.command.CommandUser;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.text.NumberFormat;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@EventBusSubscriber(modid = LTDonations.MODID, bus = Bus.FORGE)
public class TickerDonation {

    public static final Gson GSON = (new GsonBuilder()).registerTypeAdapter(JsonDataDonation.class, new JsonDeserializerDonation()).create();
    public static final Gson GSON_TOTAL = (new GsonBuilder()).registerTypeAdapter(JsonDataDonation.class, new JsonDeserializerDonationTotal()).create();
    
    private static final Set<DonationBlockEntity> callbacks = new HashSet<>();
    
    private static DonationData donationData;

    @SubscribeEvent
    public static void tick(ServerTickEvent event) {
        if (event.phase != Phase.END) return;
        if (!ThreadWorkerDonations.getInstance().running 
                && !DonationConfigs.TILTIFY.appToken.get().isEmpty() && DonationConfigs.TILTIFY.campaignId.get() != 0 
                && ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerCount() > 0) {
            donationData = getSavedData();
            ThreadWorkerDonations.getInstance().startThread(donationData);
        }
    }

    public static void callbackDonations(JsonDataDonation data) {
        //make sure server instance didnt end while running thread work
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            ServerLifecycleHooks.getCurrentServer()
                    .execute(() -> processDonationsServer(data));
        } else {
            ThreadWorkerDonations.getInstance().stopThread();
            callbacks.clear();
        }
    }
    
    public static DonationData getSavedData() {
        return getOverworld().getDataStorage().computeIfAbsent(DonationData::load, DonationData::new, DonationData.ID);
    }

    /** called once thread checked for new data, and made sure server is still running **/
    public static void processDonationsServer(JsonDataDonation data) {

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerLevel world = getOverworld();

        if (world == null) return;

        data.new_donations.stream()
                .sorted(Comparator.comparingLong(JsonDataDonationEntry::getDate))
                .filter(entry -> entry.getDate() > donationData.getLastSeenDate())
                .map(donation -> DonationLangKeys.NEW_DONATION.format(
                		ChatFormatting.AQUA + donation.name + ChatFormatting.RESET.toString(),
                		ChatFormatting.GREEN.toString() + NumberFormat.getCurrencyInstance(Locale.US).format(donation.amount) + ChatFormatting.RESET))
                .forEach(msg -> {
                    server.getPlayerList().getPlayers().stream()
                            .forEach(p -> p.displayClientMessage(msg, false));

                    triggerDonation();
                });

        long lastSeenDate = data.new_donations.stream()
                .mapToLong(d -> d.getDate())
                .max()
                .orElse(0);

        int lastSeenId = data.new_donations.stream()
                .mapToInt(d -> d.id)
                .max()
                .orElse(0);
        
        synchronized (donationData) {

            donationData.setLastSeenDate(Math.max(donationData.getLastSeenDate(), lastSeenDate));
            donationData.setLastSeenId(Math.max(donationData.getLastSeenId(), lastSeenId));

            int amountPerMonument = DonationConfigs.TILTIFY.donationAmountPerMonument.get();
            if (amountPerMonument > 0) {
                while (donationData.getMonumentsPlaced() < data.totalDonated / amountPerMonument) {
                    donationData.setMonumentsPlaced(donationData.getMonumentsPlaced() + 1);
                    server.getCommands().performPrefixedCommand(
                            new CommandSourceStack(new CommandUser(), Vec3.atLowerCornerOf(world.getSharedSpawnPos()), Vec2.ZERO, world, 4, "LTDonations", Component.literal("Tiltify Donation Tracker"), server, null),
                            DonationConfigs.TILTIFY.tiltifyCommandRun.get());
                }
            }
        }
    }

    public static void simulateDonation(String name, double amount) {

        Level world = getOverworld();

        if (world == null) return;

        if (!name.equals("")) {
            sendDonationMessage(name, amount);
        }

        triggerDonation();
    }

    public static void sendDonationMessage(final String name, final double amount) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        server.getPlayerList().getPlayers()
                .forEach(p -> p.displayClientMessage(DonationLangKeys.NEW_DONATION.format(
                        ChatFormatting.AQUA + name + ChatFormatting.RESET.toString(),
                        ChatFormatting.GREEN.toString() + NumberFormat.getCurrencyInstance(Locale.US).format(amount) + ChatFormatting.RESET), false));
    }
    
    private static ServerLevel getOverworld() {
        return ServerLifecycleHooks.getCurrentServer().overworld();
    }

    public static void triggerDonation() {
        callbacks.forEach(DonationBlockEntity::triggerDonation);
    }

    public static void addCallback(DonationBlockEntity tile) {
        callbacks.add(tile);
    }
    
    public static void removeCallback(DonationBlockEntity tile) {
        callbacks.remove(tile);
    }

}
