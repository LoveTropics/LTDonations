package com.lovetropics.donations.backend.ltts.json;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class Donation implements Comparable<Donation> {
    private final String name;
    private final UUID uuid;
    private final double amount;
    private final String comments;
    @SerializedName("payment_time")
    private final String paymentTime;
    private final boolean anonymous;

    public Donation(String name, UUID uuid, double amount, String comments, String paymentTime, boolean anonymous) {
        this.name = name;
        this.uuid = uuid;
        this.amount = amount;
        this.comments = comments;
        this.paymentTime = paymentTime;
        this.anonymous = anonymous;
    }

    @Override
    public int compareTo(Donation other) {
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
        LocalDateTime date;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            date = LocalDateTime.parse(dateStr, formatter);
        }
        catch (final DateTimeParseException exc) {
            System.out.printf("%s is not parsable!%n", dateStr);
            return null;
        }
        return date;
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

    public boolean isAnonymous() {
        return anonymous;
    }

    public String getNameShown() {
        return isAnonymous() ? "Anonymous" : getName();
    }
}
