package com.lovetropics.donations.command;

import com.lovetropics.donations.DonationGroup;
import com.lovetropics.donations.DonationLangKeys;
import com.lovetropics.donations.DonationListeners;
import com.lovetropics.donations.backend.ltts.DonationHandler;
import com.lovetropics.donations.backend.ltts.DonationRequests;
import com.lovetropics.donations.backend.ltts.json.WhitelistEvent;
import com.lovetropics.donations.monument.MonumentManager;
import com.lovetropics.donations.monument.PillarMonument;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.text.NumberFormat;
import java.util.List;

import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.coordinates.BlockPosArgument.*;
import static net.minecraft.commands.arguments.coordinates.BlockPosArgument.getBlockPos;

public class CommandDonation {
    private static final DynamicCommandExceptionType INVALID_DONATION_GROUP = new DynamicCommandExceptionType(DonationLangKeys.INVALID_DONATION_GROUP::format);
    private static final DynamicCommandExceptionType MONUMENT_ALREADY_EXISTS = new DynamicCommandExceptionType(DonationLangKeys.MONUMENT_ALREADY_EXISTS::format);
    private static final DynamicCommandExceptionType MONUMENT_DOES_NOT_EXIST = new DynamicCommandExceptionType(DonationLangKeys.MONUMENT_DOES_NOT_EXIST::format);

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            literal("donation").requires(s -> s.hasPermission(Commands.LEVEL_ADMINS))
            .then(literal("simulate")
                    .executes(ctx -> simulate(ctx, "Nigel Winthorpe", 42))
                    .then(argument("name", string())
                            .executes(ctx -> simulate(ctx, getString(ctx, "name"), 42))
                            .then(argument("amount", DoubleArgumentType.doubleArg(0, 100_000))
                                    .executes(ctx -> simulate(ctx, getString(ctx, "name"), DoubleArgumentType.getDouble(ctx, "amount"))))))
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
                    .then(literal("monument")
                            .then(literal("add")
                                    .then(argument("id", word())
                                            .then(argument("pos", blockPos())
                                                    .then(argument("group", word())
                                                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(DonationGroup.names(), builder))
                                                            .executes(ctx -> addMonument(ctx, getString(ctx, "id"), getBlockPos(ctx, "pos"), getDonationGroup(ctx), false))
                                                            .then(literal("withAnnouncement")
                                                                    .executes(ctx -> addMonument(ctx, getString(ctx, "id"), getBlockPos(ctx, "pos"), getDonationGroup(ctx), true))
                                                            )
                                                    )
                                            )
                                    )
                            )
                            .then(literal("remove")
                                    .then(argument("id", word())
                                            .suggests((ctx, builder) -> {
                                                final MonumentManager monuments = MonumentManager.get(ctx.getSource().getServer());
                                                return SharedSuggestionProvider.suggest(monuments.ids(), builder);
                                            })
                                            .executes(ctx -> removeMonument(ctx, getString(ctx, "id")))
                                    )
                            )
                    )
        );
    }

    private static DonationGroup getDonationGroup(final CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        final String id = getString(ctx, "group");
        final DonationGroup group = DonationGroup.CODEC.byName(id);
        if (group == null) {
            throw INVALID_DONATION_GROUP.create(id);
        }
        return group;
    }

    public static int simulate(CommandContext<CommandSourceStack> ctx, String name, double amount) {
    	SharedConstants.IS_RUNNING_IN_IDE = true;
        if (!name.isEmpty()) {
            ctx.getSource().sendSuccess(() -> DonationLangKeys.COMMAND_SIMULATE_DONATION.format(name, NumberFormat.getCurrencyInstance().format(amount)), true);
        }
        DonationListeners.triggerDonation(ctx.getSource().getServer(), name, amount, DonationHandler.totals());
        return Command.SINGLE_SUCCESS;
    }
    
    public static int fireworks(CommandContext<CommandSourceStack> ctx) {
        return simulate(ctx, "", 0);
    }

    private static int addMonument(final CommandContext<CommandSourceStack> ctx, final String id, final BlockPos pos, final DonationGroup group, final boolean announce) throws CommandSyntaxException {
        final MonumentManager monuments = MonumentManager.get(ctx.getSource().getServer());
        final ResourceKey<Level> dimension = ctx.getSource().getLevel().dimension();
        if (!monuments.add(id, new PillarMonument.Data(GlobalPos.of(dimension, pos), group, announce))) {
            throw MONUMENT_ALREADY_EXISTS.create(id);
        }
        ctx.getSource().sendSuccess(() -> DonationLangKeys.ADDED_MONUMENT.format(id), true);
        return 1;
    }

    private static int removeMonument(final CommandContext<CommandSourceStack> ctx, final String id) throws CommandSyntaxException {
        final MonumentManager monuments = MonumentManager.get(ctx.getSource().getServer());
        if (!monuments.remove(id)) {
            throw MONUMENT_DOES_NOT_EXIST.create(id);
        }
        ctx.getSource().sendSuccess(() -> DonationLangKeys.REMOVED_MONUMENT.format(id), true);
        return 1;
    }
}
