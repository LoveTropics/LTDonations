package com.lovetropics.donations.backend.tiltify;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;

import com.lovetropics.donations.DonationConfigs;
import com.lovetropics.donations.RequestHelper;
import com.lovetropics.donations.backend.tiltify.json.JsonDataDonation;

import io.netty.handler.codec.http.HttpMethod;

public class ThreadWorkerDonations extends RequestHelper implements Runnable {

	private static ThreadWorkerDonations instance;
    private static Thread thread;

    public volatile boolean running = false;

    private DonationData donationData;

    public static ThreadWorkerDonations getInstance() {
        if (instance == null) {
            instance = new ThreadWorkerDonations();
        }
        return instance;
    }

    public ThreadWorkerDonations() {
		super("https://tiltify.com/api/v3/", DonationConfigs.TILTIFY.appToken::get);
	}

    public void startThread(DonationData donationData) {
        this.donationData = donationData;
        running = true;

        if (thread == null || thread.getState() == Thread.State.TERMINATED) {
            thread = new Thread(instance, "Donations lookup thread");
            thread.start();
        }
    }

    public void stopThread() {
        running = false;
    }

    @Override
    public void run() {

        try {
            while (running) {
                checkDonations();
                Thread.sleep(TimeUnit.SECONDS.toMillis(DonationConfigs.TILTIFY.donationTrackerRefreshRate.get()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        running = false;
    }

    public void checkDonations() {
        //http code
        try {

            //check if we decided to shut off donation querying after it started
            if (DonationConfigs.TILTIFY.appToken.get().isEmpty() || DonationConfigs.TILTIFY.campaignId.get() == 0) {
                stopThread();
                return;
            }

            String contents = getData_Real();

            String contentsTotal = getData_TotalDonations();

            JsonDataDonation json = TickerDonation.GSON.fromJson(contents, JsonDataDonation.class);

            //store into temp object to scrap later once we take the total from it
            JsonDataDonation jsonTotal = TickerDonation.GSON_TOTAL.fromJson(contentsTotal, JsonDataDonation.class);

            //dont judge me
            json.totalDonated = jsonTotal.totalDonated;
            
            TickerDonation.callbackDonations(json);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public String getData_Real() {
        try {
            String endpoint;
            synchronized (donationData) {
                endpoint = "campaigns/" + DonationConfigs.TILTIFY.campaignId.get() + "/donations?count=100" + (donationData.getLastSeenId() == 0 ? "" : "&after=" + donationData.getLastSeenId());
            }
            
            HttpURLConnection con = getAuthorizedConnection(HttpMethod.GET, endpoint);
            try {
                return readInput(con.getInputStream(), false);
            } catch (IOException ex) {
                LogManager.getLogger().error(readInput(con.getErrorStream(), true));
            } finally {
                con.disconnect();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return "ERROR";
    }

    public String getData_TotalDonations() {
        try {
            String endpoint = "campaigns/" + DonationConfigs.TILTIFY.campaignId.get();
            HttpURLConnection con = getAuthorizedConnection(HttpMethod.GET, endpoint);
            try {
                return readInput(con.getInputStream(), false);
            } catch (IOException ex) {
                LogManager.getLogger().error(readInput(con.getErrorStream(), true));
            } finally {
                con.disconnect();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return "ERROR";
    }
}
