package com.lovetropics.donations;

public class JsonDataDonationEntry {

    public int id;
    public float amount;
    public String name;
    public String comment;
    public long completedAt;
    public boolean sustained;

    public long getDate() {
        return completedAt;
    }

}
