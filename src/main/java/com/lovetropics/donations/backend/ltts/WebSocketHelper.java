package com.lovetropics.donations.backend.ltts;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lovetropics.donations.DonationConfigs;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class WebSocketHelper {

    private static final JsonParser JSON_PARSER = new JsonParser();
    private static final int REQUEST_TIMEOUT = 3000;

    private AsyncHttpClient client = null;

    public static JsonObject parse(final String body) {
        // TODO once we know our data model, create an actual Gson object for it
        return JSON_PARSER.parse(body).getAsJsonObject();
    }

    public boolean cycleConnection() {
        try {
            if (client != null && !client.isClosed()) {
                client.close();
            }

            return open();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public void checkAndCycleConnection() {
        if (client == null || client.isClosed()) {
            cycleConnection();
        }
    }

    public boolean open() {
    	if (DonationConfigs.TECH_STACK.authKey.get().isEmpty()) {
    		return false;
    	}
        if (client == null || client.isClosed()) {
            client = Dsl.asyncHttpClient();
        }
        try {
            WebSocket websocket = client.prepareGet(getUrl()).setRequestTimeout(REQUEST_TIMEOUT)
                .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
                    new WebSocketListener() {
                        @Override
                        public void onOpen(WebSocket websocket) {
                            System.out.println("Web socket opened");
                        }

                        @Override
                        public void onClose(WebSocket websocket, int code, String reason) {
                            System.out.println("Web socket closed: " + reason + " (" + code + ")");
                        }

                        @Override
                        public void onTextFrame(String payload, boolean finalFragment, int rsv) {
                            WebSocketEvent.handleEvent(payload);
                        }

                        @Override
                        public void onError(Throwable t) {
                            System.out.println("Error occurred in web socket!");
                            t.printStackTrace();
                        }
                    }).build()).get();
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Error connecting to web socket. It's probably not running?");
            e.printStackTrace();
        }

        return client != null && !client.isClosed();
    }

    public void close() {
        if (client != null && !client.isClosed()) {
            try {
                client.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String getUrl() {
        final int configPort = DonationConfigs.TECH_STACK.port.get();
        final String port = configPort == 0 ? "" : ":" + configPort;
        return "wss://" + DonationConfigs.TECH_STACK.url.get() + port + "/ws";
    }
}
