package com.lovetropics.donations.backend.ltts;

import com.lovetropics.lib.techstack.TechstackEventSubscriber;
import com.mojang.logging.LogUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;

public class WebSocketHelper {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Runnable onOpen;
    @Nullable
    private TechstackEventSubscriber subscriber;

    public WebSocketHelper(Runnable onOpen) {
        this.onOpen = onOpen;
    }

    public void updateConfig(String uri, String token) {
        if (subscriber != null) {
            subscriber.close();
        }
        subscriber = buildSubscriber(uri, token);
    }

    @Nullable
    private TechstackEventSubscriber buildSubscriber(String uriString, String token) {
        if (uriString.isBlank() || token.isBlank()) {
            return null;
        }

        URI uri;
        try {
            uri = new URI(uriString);
        } catch (URISyntaxException e) {
            LOGGER.warn("Malformed URI", e);
            return null;
        }

        TechstackEventSubscriber.Builder subscriber = TechstackEventSubscriber.builder(uri)
                .onConnectionOpen(onOpen)
                .authenticate(token);

        WebSocketEvent.addSubscribersTo(subscriber);

        return subscriber.build();
    }
}
