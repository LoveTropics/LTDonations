package com.lovetropics.donations.command;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

import java.text.NumberFormat;

import com.lovetropics.donations.DonationData;
import com.lovetropics.donations.DonationLangKeys;
import com.lovetropics.donations.ThreadWorkerDonations;
import com.lovetropics.donations.TickerDonation;
import com.lovetropics.donations.websockets.WebSocketHelper;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.command.CommandSource;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.text.StringTextComponent;

public class CommandDonation {
    
    public static void register(final CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
            literal("donation").requires(s -> s.hasPermissionLevel(2))
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
            .then(literal("cycleconnection").executes(CommandDonation::cycleConnection))
        );
    }

    private static int cycleConnection(CommandContext<CommandSource> ctx) {
        final boolean openConnection = WebSocketHelper.cycleConnection();
        if (!openConnection) {
            ctx.getSource().sendFeedback(DonationLangKeys.COMMAND_COULDNT_ESTABLISH_CONNECTION.getComponent(), true);
            return 0;
        }
        ctx.getSource().sendFeedback(DonationLangKeys.COMMAND_ESTABLISHED_CONNECTION.getComponent(), true);
        return Command.SINGLE_SUCCESS;
    }

    public static int dumpResponse(CommandContext<CommandSource> ctx) {
        ctx.getSource().sendFeedback(new StringTextComponent(ThreadWorkerDonations.getInstance().getData_Real()), false);
        return Command.SINGLE_SUCCESS;
    }
    
    public static int resetDonations(CommandContext<CommandSource> ctx) {
        DonationData data = TickerDonation.getSavedData();
        if (data != null) {
            synchronized (data) {
                data.resetData();
            }
            ctx.getSource().sendFeedback(DonationLangKeys.COMMAND_RESET_DONATION.getComponent(), true);
        }
        return Command.SINGLE_SUCCESS;
    }

    public static int setLastSeen(CommandContext<CommandSource> ctx, int id) {
        DonationData data = TickerDonation.getSavedData();
        if (data != null) {
            synchronized (data) {
                data.setLastSeenId(id);
                data.setLastSeenDate(0);
            }
            ctx.getSource().sendFeedback(DonationLangKeys.COMMAND_RESET_LAST_DONATION.format(id), true);
        }
        return Command.SINGLE_SUCCESS;
    }
    
    public static int simulate(CommandContext<CommandSource> ctx, String name, double amount) {
    	SharedConstants.developmentMode = true;
        if (!name.isEmpty()) {
            ctx.getSource().sendFeedback(DonationLangKeys.COMMAND_SIMULATE_DONATION.format(name, NumberFormat.getCurrencyInstance().format(amount)), true);
        }
        TickerDonation.simulateDonation(name, amount);
        return Command.SINGLE_SUCCESS;
    }
    
    public static int fireworks(CommandContext<CommandSource> ctx) {
        return simulate(ctx, "", 0);
    }
}
