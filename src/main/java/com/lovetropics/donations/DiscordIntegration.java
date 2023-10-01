package com.lovetropics.donations;

import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

// TODO: update to use JEDI
public class DiscordIntegration {
    private static final Logger LOGGER = LogManager.getLogger();

    private static boolean skipDiscord = false;
    private static Object discord;
    private static Method _sendMessage;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void send(final String content) {
        if (skipDiscord) return;
        if (discord == null) {
            try {
                final Class dcintegration = Class.forName("de.erdbeerbaerlp.dcintegration.DiscordIntegration");
                discord = ObfuscationReflectionHelper.getPrivateValue(dcintegration, null, "discord_instance");
                _sendMessage = ObfuscationReflectionHelper.findMethod(discord.getClass(), "sendMessage", String.class);
            } catch (final Exception e) {
                LOGGER.warn("Failed to setup dcintegration sender:", e);
                skipDiscord = true;
                return;
            }
        }
        try {
            _sendMessage.invoke(discord, content);
        } catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            LOGGER.error("Failed to send to dcintegration:", e);
            skipDiscord = true;
        }
    }
}
