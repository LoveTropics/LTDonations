package com.lovetropics.donations.command;

import com.lovetropics.donations.DonationConfigs;
import com.lovetropics.donations.DonationGroup;
import com.lovetropics.donations.DonationLangKeys;
import com.lovetropics.donations.DonationListener;
import com.lovetropics.donations.DonationListeners;
import com.lovetropics.donations.DonationState;
import com.lovetropics.donations.backend.ltts.DonationHandler;
import com.lovetropics.donations.backend.ltts.DonationRequests;
import com.lovetropics.donations.backend.ltts.json.FullDonationState;
import com.lovetropics.donations.backend.ltts.json.WhitelistEvent;
import com.lovetropics.donations.monument.MonumentData;
import com.lovetropics.donations.monument.MonumentManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.text.NumberFormat;
import java.util.List;

import static com.lovetropics.donations.command.MonumentConfigArgument.getMonumentConfig;
import static com.lovetropics.donations.command.MonumentConfigArgument.monumentConfig;
import static com.mojang.brigadier.arguments.StringArgumentType.*;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class CommandDonation {
    private static final DynamicCommandExceptionType MONUMENT_ALREADY_EXISTS = new DynamicCommandExceptionType(DonationLangKeys.MONUMENT_ALREADY_EXISTS::format);
    private static final DynamicCommandExceptionType MONUMENT_DOES_NOT_EXIST = new DynamicCommandExceptionType(DonationLangKeys.MONUMENT_DOES_NOT_EXIST::format);

    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext buildContext) {
        dispatcher.register(
                literal("donation").requires(s -> s.hasPermission(Commands.LEVEL_GAMEMASTERS))
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
                                                .then(argument("monument", monumentConfig(buildContext))
                                                        .executes(context -> addMonument(context, getString(context, "id"), getMonumentConfig(context, "monument")))
                                                )
                                        )
                                )
                                .then(literal("update")
                                        .then(argument("id", word()).suggests(suggestMonuments())
                                                .then(argument("monument", monumentConfig(buildContext))
                                                        .executes(context -> updateMonument(context, getString(context, "id"), getMonumentConfig(context, "monument")))
                                                )
                                        )
                                )
                                .then(literal("remove")
                                        .then(argument("id", word()).suggests(suggestMonuments())
                                                .executes(ctx -> removeMonument(ctx, getString(ctx, "id")))
                                        )
                                )
                        )
        );
    }

    private static SuggestionProvider<CommandSourceStack> suggestMonuments() {
        return (ctx, builder) -> {
            final MonumentManager monuments = MonumentManager.get(ctx.getSource().getServer());
            return SharedSuggestionProvider.suggest(monuments.ids(), builder);
        };
    }

    public static int simulate(CommandContext<CommandSourceStack> ctx, String name, double amount) {
        if (!name.isEmpty()) {
            ctx.getSource().sendSuccess(() -> DonationLangKeys.COMMAND_SIMULATE_DONATION.format(name, NumberFormat.getCurrencyInstance().format(amount)), true);
        }
        MinecraftServer server = ctx.getSource().getServer();
        DonationState oldState = DonationState.copyOf(DonationHandler.state());
        // Not set up with the tech stack, so let simulation actually update our internal state
        if (!DonationConfigs.TECH_STACK.shouldConnect()) {
            double newTotal = oldState.getAmount(DonationGroup.ALL) + amount;
            DonationHandler.applyFullState(server, FullDonationState.forTotal(newTotal), false);
        }
        DonationListeners.triggerDonation(server, new DonationListener.Details(
                amount,
                name,
                null,
                0.0,
                oldState,
                DonationHandler.state()
        ));
        return Command.SINGLE_SUCCESS;
    }

    public static int fireworks(CommandContext<CommandSourceStack> ctx) {
        return simulate(ctx, "", 0);
    }

    private static int addMonument(final CommandContext<CommandSourceStack> ctx, final String id, final MonumentData data) throws CommandSyntaxException {
        final MonumentManager monuments = MonumentManager.get(ctx.getSource().getServer());
        if (!monuments.add(id, data)) {
            throw MONUMENT_ALREADY_EXISTS.create(id);
        }
        ctx.getSource().sendSuccess(() -> DonationLangKeys.ADDED_MONUMENT.format(id), true);
        return 1;
    }

    private static int updateMonument(final CommandContext<CommandSourceStack> ctx, final String id, final MonumentData data) throws CommandSyntaxException {
        final MonumentManager monuments = MonumentManager.get(ctx.getSource().getServer());
        if (!monuments.update(id, data)) {
            throw MONUMENT_DOES_NOT_EXIST.create(id);
        }
        ctx.getSource().sendSuccess(() -> DonationLangKeys.UPDATED_MONUMENT.format(id), true);
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
