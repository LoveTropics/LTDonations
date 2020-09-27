package com.lovetropics.donations;

import com.google.gson.JsonObject;
import com.lovetropics.donations.rabbitmq.Bunny;

import javax.annotation.Nullable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

public class Donation implements Comparable {
    private final String name;
    private final UUID uuid;
    private final double amount;
    private final String comments;
    private final String paymentTime;

    public Donation(String name, UUID uuid, double amount, String comments, String paymentTime) {
        this.name = name;
        this.uuid = uuid;
        this.amount = amount;
        this.comments = comments;
        this.paymentTime = paymentTime;
    }

    @Override
    public int compareTo(Object o) {
        final Donation other = (Donation) o;

        final LocalDateTime thisDate = getDate(paymentTime);
        final LocalDateTime thatDate = getDate(other.paymentTime);
        if (thisDate != null && thatDate != null) {
            return thisDate.compareTo(thatDate);
        }
        return 0;
    }

    @Nullable
    private LocalDateTime getDate(String dateStr) {
        dateStr = dateStr.split("\\.")[0];
        LocalDateTime date = null;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            date = LocalDateTime.parse(dateStr, formatter);
            System.out.printf("%s%n", date);
        }
        catch (DateTimeParseException exc) {
            System.out.printf("%s is not parsable!%n", dateStr);
            // TODO throw exc;      // Rethrow the exception.
            return null;
        }
        return date;
    }

    public static Donation fromJson(final JsonObject obj) {
        // TODO UUID needs to be received
        final JsonObject payload = obj.getAsJsonObject("payload");
        return new Donation(
                getString(payload, "name"),
                UUID.randomUUID(),
                getDouble(payload, "amount"),
                getString(payload, "comments"),
                getString(payload, "payment_time")
        );
    }

    private static String getString(final JsonObject obj, final String name) {
        return obj.get(name).getAsString();
    }

    private static double getDouble(final JsonObject obj, final String value) {
        return obj.get(value).getAsDouble();
    }

    public String getName() {
        return name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public double getAmount() {
        return amount;
    }

    public String getComments() {
        return comments;
    }

    public String getPaymentTime() {
        return paymentTime;
    }
}
