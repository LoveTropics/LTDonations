package com.lovetropics.donations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Supplier;

public abstract class RequestHelper {

	private final Supplier<String> token;

	protected RequestHelper(Supplier<String> token) {
		this.token = token;
	}

	protected HttpURLConnection getAuthorizedConnection(String method, String address) throws IOException {
		URL url = new URL(address);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod(method);
		con.setRequestProperty("User-Agent", "LTDonations 1.0 (lovetropics.com)");
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestProperty("Authorization", "Bearer " + token.get());
		return con;
	}

	protected final String readInput(InputStream is, boolean newlines) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(is));
		String inputLine;
		StringBuffer content = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			content.append(inputLine);
			if (newlines) {
				content.append("\n");
			}
		}
		in.close();
		return content.toString();
	}

}