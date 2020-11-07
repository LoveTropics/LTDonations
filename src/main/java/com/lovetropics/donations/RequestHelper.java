package com.lovetropics.donations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.datafixers.util.Either;

import io.netty.handler.codec.http.HttpMethod;
import net.minecraft.util.Unit;

public abstract class RequestHelper {

	private final String baseURL;
	private final Supplier<String> token;

	protected RequestHelper(String baseURL, Supplier<String> token) {
		this.baseURL = baseURL.endsWith("/") ? baseURL : baseURL + "/";
		this.token = token;
	}


	protected HttpURLConnection getAuthorizedConnection(HttpMethod method, String endpoint) throws IOException {
		if (endpoint.startsWith("/")) {
			endpoint = endpoint.substring(1);
		}
		URL url = new URL(baseURL + endpoint);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod(method.name());
		con.setRequestProperty("User-Agent", "LTDonations 1.0 (lovetropics.com)");
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestProperty("Authorization", "Bearer " + token.get());
		return con;
	}

	protected Either<Unit, String> request(HttpMethod method, String endpoint) {
		return request(method, endpoint, s -> Unit.INSTANCE);
	}

	protected <T> Either<T, String> request(HttpMethod method, String endpoint, Class<T> clazz) {
		return request(method, endpoint, TypeToken.get(clazz));
	}

	protected <T> Either<T, String> request(HttpMethod method, String endpoint, TypeToken<T> clazz) {
		return request(method, endpoint, s -> new Gson().<T>fromJson(s, clazz.getType()));
	}

	protected <T> Either<T, String> request(HttpMethod method, String endpoint, Function<String, T> parser) {
		try {
			HttpURLConnection con = getAuthorizedConnection(method, endpoint);
			try {
				String payload = readInput(con.getInputStream(), false);
				System.out.println("REST Response: " + payload);
				return Either.left(parser.apply(payload));
			} catch (IOException e) {
				return Either.right(readInput(con.getErrorStream(), true));
			}
		} catch (IOException e) {
			return Either.right(e.toString());
		}
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