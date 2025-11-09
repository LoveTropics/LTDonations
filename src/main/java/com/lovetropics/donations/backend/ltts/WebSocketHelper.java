package com.lovetropics.donations.backend.ltts;

import com.google.gson.JsonObject;
import com.lovetropics.donations.DonationConfigs;
import com.lovetropics.lib.backend.BackendConnection;
import com.lovetropics.lib.backend.BackendConnectionConfig;
import com.lovetropics.lib.backend.BackendProxy;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;

public class WebSocketHelper {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final BackendProxy proxy;

    public WebSocketHelper(final Runnable onOpen) {
        proxy = new BackendProxy(new BackendConnection.Handler() {
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
                LOGGER.error("Donations websocket closed with error: {}", cause.getMessage());
            }

            @Override
            public void acceptClosed(final int code, @Nullable final String reason) {
                LOGGER.error("Donations websocket closed with code: {} and reason: {}", code, reason);
            }
        });
    }

    @Nullable
    private static BackendConnectionConfig connectionConfig() {
        DonationConfigs.CategoryTechStack techStack = DonationConfigs.TECH_STACK;
        if (!techStack.shouldConnect()) {
            return null;
        }

        try {
            BackendConnectionConfig config = BackendConnectionConfig.of(new URI(techStack.websocketUrl.get()));
            String token = techStack.authKey.get();
            if (!token.isBlank()) {
                config = config.withToken(token);
            }
            return config.withSubscriptions(WebSocketEvent.subscriptions());
        } catch (URISyntaxException e) {
            LOGGER.warn("Malformed URI", e);
        }

        return null;
    }

    public void tick() {
        proxy.connectWith(connectionConfig());
        proxy.tick();
    }
}
