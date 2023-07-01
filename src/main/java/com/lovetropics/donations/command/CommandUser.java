package com.lovetropics.donations.command;

import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;

public class CommandUser implements CommandSource {
    @Override
    public void sendSystemMessage(Component component) {
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
