package com.lovetropics.donations.backend.ltts;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.lovetropics.donations.DonationConfigs;
import com.lovetropics.lib.backend.BackendConnection;
import com.lovetropics.lib.backend.BackendProxy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Supplier;

public class WebSocketHelper {
    private static final Logger LOGGER = LogManager.getLogger(WebSocketHelper.class);

    private final BackendProxy proxy;

    public WebSocketHelper() {
        Supplier<URI> address = () -> {
            if (!Strings.isNullOrEmpty(DonationConfigs.TECH_STACK.authKey.get())) {
                try {
                    return new URI(getUrl());
                } catch (URISyntaxException ignored) {
                }
            }
            return null;
        };

        this.proxy = new BackendProxy(address, new BackendConnection.Handler() {
            @Override
            public void acceptOpened() {
            }

            @Override
            public void acceptMessage(JsonObject payload) {
                WebSocketEvent.handleEvent(payload);
            }

            @Override
            public void acceptError(Throwable cause) {
                LOGGER.error("Donations websocket closed with error", cause);
            }

            @Override
            public void acceptClosed(int code, @Nullable String reason) {
                LOGGER.error("Donations websocket closed with code: {} and reason: {}", code, reason);
            }
        });
    }

    private static String getUrl() {
        final int configPort = DonationConfigs.TECH_STACK.websocketPort.get();
        final String port = configPort == 0 ? "" : ":" + configPort;
        return "wss://" + DonationConfigs.TECH_STACK.websocketUrl.get() + port + "/ws";
    }

    public void tick() {
        this.proxy.tick();
    }
}
