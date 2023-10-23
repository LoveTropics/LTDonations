package com.lovetropics.donations.backend.ltts;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.lovetropics.donations.DonationConfigs;
import com.lovetropics.lib.backend.BackendConnection;
import com.lovetropics.lib.backend.BackendProxy;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Supplier;

public class WebSocketHelper {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final BackendProxy proxy;

    public WebSocketHelper(final Runnable onOpen) {
        final Supplier<URI> address = () -> {
            if (!Strings.isNullOrEmpty(DonationConfigs.TECH_STACK.authKey.get())) {
                try {
                    return new URI(DonationConfigs.TECH_STACK.websocketUrl.get());
                } catch (final URISyntaxException ignored) {
                }
            }
            return null;
        };

        proxy = new BackendProxy(address, new BackendConnection.Handler() {
            @Override
            public void acceptOpened() {
                onOpen.run();
            }

            @Override
            public void acceptMessage(final JsonObject payload) {
                WebSocketEvent.handleEvent(payload);
            }

            @Override
            public void acceptError(final Throwable cause) {
                LOGGER.error("Donations websocket closed with error", cause);
            }

            @Override
            public void acceptClosed(final int code, @Nullable final String reason) {
                LOGGER.error("Donations websocket closed with code: {} and reason: {}", code, reason);
            }
        });
    }

    public void tick() {
        proxy.tick();
    }
}
