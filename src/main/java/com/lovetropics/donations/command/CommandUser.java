package com.lovetropics.donations.command;

import org.apache.logging.log4j.LogManager;

import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class CommandUser implements CommandSource {

    @Override
    public void sendMessage(Component component, UUID senderUUID) {
        LogManager.getLogger().info(component.getString());
    }

    @Override
    public boolean acceptsSuccess() {
        return true;
    }

    @Override
    public boolean acceptsFailure() {
        return true;
    }

    @Override
    public boolean shouldInformAdmins() {
        return true;
    }
}
