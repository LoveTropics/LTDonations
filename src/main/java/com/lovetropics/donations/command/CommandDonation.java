package com.lovetropics.donations.command;

import com.lovetropics.donations.DonationLangKeys;
import com.lovetropics.donations.backend.ltts.DonationRequests;
import com.lovetropics.donations.backend.ltts.json.WhitelistEvent;
import com.lovetropics.donations.backend.tiltify.DonationData;
import com.lovetropics.donations.backend.tiltify.ThreadWorkerDonations;
import com.lovetropics.donations.backend.tiltify.TickerDonation;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.text.NumberFormat;
import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class CommandDonation {
    
    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            literal("donation").requires(s -> s.hasPermission(Commands.LEVEL_ADMINS))
            .then(literal("dumpresponse").executes(CommandDonation::dumpResponse))
            .then(literal("reset").executes(CommandDonation::resetDonations))
            .then(literal("last")
                    .then(argument("id", IntegerArgumentType.integer())
                            .executes(ctx -> setLastSeen(ctx, IntegerArgumentType.getInteger(ctx, "id")))))
            .then(literal("simulate")
                    .executes(ctx -> simulate(ctx, "Nigel Winthorpe", 42))
                    .then(argument("name", StringArgumentType.string())
                            .executes(ctx -> simulate(ctx, StringArgumentType.getString(ctx, "name"), 42))
                            .then(argument("amount", DoubleArgumentType.doubleArg(0, 100_000))
                                    .executes(ctx -> simulate(ctx, StringArgumentType.getString(ctx, "name"), DoubleArgumentType.getDouble(ctx, "amount"))))))
            .then(literal("fireworks")
                    .executes(CommandDonation::fireworks))
            .then(literal("pendingevents").executes(ctx -> {
            	try {
                    List<WhitelistEvent> events = DonationRequests.get().getUnprocessedEvents();
                    ctx.getSource().sendSuccess(() -> Component.literal(events.toString()), true);
            	} catch (Exception e) {
            		return 0;
            	}
            	return Command.SINGLE_SUCCESS;
            }))
            .then(literal("test").then(literal("whitelist").executes(ctx -> {
            	DonationRequests.get().fakeWhitelist();
            	return Command.SINGLE_SUCCESS;
            })).then(literal("blacklist").executes(ctx -> {
            	DonationRequests.get().fakeBlacklist();
            	return Command.SINGLE_SUCCESS;
            })))
        );
    }

    public static int dumpResponse(CommandContext<CommandSourceStack> ctx) {
        String data = ThreadWorkerDonations.getInstance().getData_Real();
        ctx.getSource().sendSuccess(() -> Component.literal(data), false);
        return Command.SINGLE_SUCCESS;
    }
    
    public static int resetDonations(CommandContext<CommandSourceStack> ctx) {
        DonationData data = TickerDonation.getSavedData();
        if (data != null) {
            synchronized (data) {
                data.resetData();
            }
            ctx.getSource().sendSuccess(DonationLangKeys.COMMAND_RESET_DONATION::getComponent, true);
        }
        return Command.SINGLE_SUCCESS;
    }

    public static int setLastSeen(CommandContext<CommandSourceStack> ctx, int id) {
        DonationData data = TickerDonation.getSavedData();
        if (data != null) {
            synchronized (data) {
                data.setLastSeenId(id);
                data.setLastSeenDate(0);
            }
            ctx.getSource().sendSuccess(() -> DonationLangKeys.COMMAND_RESET_LAST_DONATION.format(id), true);
        }
        return Command.SINGLE_SUCCESS;
    }
    
    public static int simulate(CommandContext<CommandSourceStack> ctx, String name, double amount) {
    	SharedConstants.IS_RUNNING_IN_IDE = true;
        if (!name.isEmpty()) {
            ctx.getSource().sendSuccess(() -> DonationLangKeys.COMMAND_SIMULATE_DONATION.format(name, NumberFormat.getCurrencyInstance().format(amount)), true);
        }
        TickerDonation.simulateDonation(name, amount);
        return Command.SINGLE_SUCCESS;
    }
    
    public static int fireworks(CommandContext<CommandSourceStack> ctx) {
        return simulate(ctx, "", 0);
    }
}
