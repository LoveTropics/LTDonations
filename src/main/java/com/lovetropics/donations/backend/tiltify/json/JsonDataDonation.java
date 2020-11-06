package com.lovetropics.donations.backend.tiltify.json;

import java.util.ArrayList;
import java.util.List;

public class JsonDataDonation {

    public int totalDonated = 0;

    public JsonDataDonationMeta meta = new JsonDataDonationMeta();

    public List<JsonDataDonationEntry> new_donations = new ArrayList<>();

}
