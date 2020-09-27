package com.lovetropics.donations.rabbitmq;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lovetropics.donations.Donation;
import com.lovetropics.donations.LTDonations;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

public class Bunny {

    public static final JsonParser JSON_PARSER = new JsonParser();
    public static Connection CONNECTION;
    public static Channel CHANNEL;

    public static Connection getConnection() {
        try {
            if (CONNECTION == null) {
                final ConnectionFactory factory = new ConnectionFactory();
                final String uri = "";// TODO private credentials"
                factory.setUri(uri);
                CONNECTION = factory.newConnection();
            }
            return CONNECTION;
        } catch (final NoSuchAlgorithmException | KeyManagementException | URISyntaxException | IOException | TimeoutException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void checkAndReestablishConnection() {
        if (CONNECTION == null || !CONNECTION.isOpen() || CHANNEL == null || !CHANNEL.isOpen()) {
            System.out.println("Reopening connection");
            final boolean connectionOpened = openConnection();
            System.out.println("Connection reopened?: " + connectionOpened);
        }
    }

    // TODO needs code to automagically reopen if the connection is closed prematurely
    public static boolean openConnection() {
        CONNECTION = getConnection();
        if (CONNECTION != null) {
            try {
                CHANNEL = CONNECTION.createChannel();
                final String queueName = CHANNEL.queueDeclare().getQueue();
                final String exchangeName = "amq.topic";
                final String routingKey = "events";
                CHANNEL.queueBind(queueName, exchangeName, routingKey);
                boolean autoAck = false;
                CHANNEL.basicConsume(queueName, autoAck, "donation",
                    new DefaultConsumer(CHANNEL) {
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                            String routingKey = envelope.getRoutingKey();
                            String contentType = properties.getContentType();
                            long deliveryTag = envelope.getDeliveryTag();
                            // (process the message components here ...)
                            CHANNEL.basicAck(deliveryTag, false);
                            final JsonObject obj = Bunny.parse(body);
                            final String type = obj.get("type").getAsString();
                            final String crud = obj.get("crud").getAsString();
                            if (type.equals("state")) {
                                //System.out.println("state");
                            } else if (type.equals("donation")) {
                                if (crud.equals("create")) {
                                    final Donation donation = Donation.fromJson(obj);
                                    if (donation == null) {
                                        System.out.println("Null donation. why?");
                                        System.out.println(obj);
                                    } else {
                                        System.out.println("Queueing donation");
                                        LTDonations.queueDonation(donation);
                                    }
                                }
                            }
                        }
                    });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return CONNECTION != null;
    }

    public static void closeConnection() {
        if (CONNECTION != null  && CONNECTION.isOpen()) {
            try {
                if (CHANNEL != null && CHANNEL.isOpen()) {
                    CHANNEL.close();
                    CHANNEL = null;
                }
                CONNECTION.close();
                CONNECTION = null;
            } catch (final IOException | TimeoutException e) {
                e.printStackTrace();
            }
        }
    }

    public static JsonObject parse(final byte[] body) {
        // TODO once we know our data model, create an actual Gson object for it
        return JSON_PARSER.parse(new String(body)).getAsJsonObject();
    }
}
