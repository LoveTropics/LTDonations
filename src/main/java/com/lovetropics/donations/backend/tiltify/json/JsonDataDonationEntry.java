package com.lovetropics.donations.backend.tiltify.json;

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
