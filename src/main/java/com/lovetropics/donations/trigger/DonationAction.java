package com.lovetropics.donations.trigger;

import com.lovetropics.donations.DonationListener;
import com.lovetropics.lib.codec.CodecRegistry;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public interface DonationAction {
    Logger LOGGER = LogUtils.getLogger();

    Codec<DonationAction> CODEC = Codec.lazyInitialized(() -> {
        Codec<DonationAction> fullCodec = DonationAction.REGISTRY.dispatch(DonationAction::codec, Function.identity());
        return Codec.withAlternative(
                fullCodec,
                fullCodec.listOf(), Sequence::new
        );
    });

    CodecRegistry<String, MapCodec<? extends DonationAction>> REGISTRY = Util.make(CodecRegistry.stringKeys(), registry -> {
        registry.register("sequence", Sequence.CODEC);
        registry.register("run_function", RunFunction.CODEC);
    });

    void execute(MinecraftServer server, DonationListener.Details details);

    MapCodec<? extends DonationAction> codec();

    record Sequence(List<DonationAction> actions) implements DonationAction {
        public static final MapCodec<Sequence> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                DonationAction.CODEC.listOf().fieldOf("action").forGetter(Sequence::actions)
        ).apply(i, Sequence::new));

        @Override
        public void execute(MinecraftServer server, DonationListener.Details details) {
            for (DonationAction action : actions) {
                action.execute(server, details);
            }
        }

        @Override
        public MapCodec<Sequence> codec() {
            return CODEC;
        }
    }

    record RunFunction(
            Identifier functionId,
            Optional<CompoundTag> arguments,
            boolean asDonor
    ) implements DonationAction {
        public static final MapCodec<RunFunction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.fieldOf("function").forGetter(RunFunction::functionId),
                TagParser.LENIENT_CODEC.optionalFieldOf("arguments").forGetter(RunFunction::arguments),
                Codec.BOOL.optionalFieldOf("as_donor", false).forGetter(RunFunction::asDonor)
        ).apply(i, RunFunction::new));

        @Override
        public void execute(MinecraftServer server, DonationListener.Details details) {
            Optional<CommandFunction<CommandSourceStack>> function = server.getFunctions().get(functionId);
            if (function.isEmpty()) {
                LOGGER.warn("Failed to find function with id '{}'", functionId);
                return;
            }
            CommandSourceStack source = createExecutionSource(server, details.donorPlayerId());
            execute(server, source, function.get());
        }

        private void execute(MinecraftServer server, CommandSourceStack source, CommandFunction<CommandSourceStack> functionIn) {
            ServerFunctionManager functionManager = server.getFunctions();
            try {
                InstantiatedFunction<CommandSourceStack> function = functionIn.instantiate(arguments.orElse(null), functionManager.getDispatcher());
                Commands.executeCommandInContext(
                        source, context -> ExecutionContext.queueInitialFunctionCall(context, function, source, CommandResultCallback.EMPTY)
                );
            } catch (FunctionInstantiationException _) {
            } catch (Exception e) {
                LOGGER.warn("Failed to execute function {}", functionIn.id(), e);
            }
        }

        private CommandSourceStack createExecutionSource(MinecraftServer server, @Nullable UUID donorPlayerId) {
            CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
            if (asDonor) {
                ServerPlayer donorPlayer = donorPlayerId != null ? server.getPlayerList().getPlayer(donorPlayerId) : null;
                if (donorPlayer != null) {
                    source = source.withEntity(donorPlayer);
                }
            }
            return source;
        }

        @Override
        public MapCodec<RunFunction> codec() {
            return CODEC;
        }
    }
}
