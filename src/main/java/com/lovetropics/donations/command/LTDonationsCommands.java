package com.lovetropics.donations.command;

import com.lovetropics.donations.LTDonations;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class LTDonationsCommands {
    public static void registerArguments() {
        LTDonations.registrate().simple("monument", Registries.COMMAND_ARGUMENT_TYPE, () -> ArgumentTypeInfos.registerByClass(
                MonumentConfigArgument.class,
                SingletonArgumentInfo.contextAware(MonumentConfigArgument::new)
        ));
    }

    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDonation.register(event.getDispatcher(), event.getBuildContext());
    }
}
