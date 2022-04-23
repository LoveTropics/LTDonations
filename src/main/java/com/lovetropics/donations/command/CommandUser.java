package com.lovetropics.donations.command;

import org.apache.logging.log4j.LogManager;

import net.minecraft.command.ICommandSource;
import net.minecraft.util.text.ITextComponent;

import java.util.UUID;

public class CommandUser implements ICommandSource {

    @Override
    public void sendMessage(ITextComponent component, UUID senderUUID) {
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
