package com.lovetropics.donations;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Unit;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.Util;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Function;
import java.util.function.Supplier;

public final class RequestHelper {
	private static final Logger LOGGER = LogUtils.getLogger();

	private static final HttpClient CLIENT = HttpClient.newBuilder()
			.executor(Util.ioPool())
			.build();

	private final Supplier<String> baseUrl;
	private final Supplier<String> token;

	public RequestHelper(final Supplier<String> baseUrl, final Supplier<String> token) {
		this.baseUrl = () -> {
			final String url = baseUrl.get();
			return url.endsWith("/") ? url : url + "/";
		};
		this.token = token;
	}

	@Nullable
	private HttpRequest.Builder requestBuilder(String endpoint) {
		final String baseUrl = this.baseUrl.get();
		final String token = this.token.get();
		if (baseUrl.isEmpty() || token.isEmpty()) {
			return null;
		}
		if (endpoint.startsWith("/")) {
			endpoint = endpoint.substring(1);
		}
		return HttpRequest.newBuilder(URI.create(baseUrl + endpoint))
				.header(HttpHeaders.USER_AGENT, "LTDonations 1.0 (lovetropics.org)")
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
	}

	private <T> Either<T, String> request(final String endpoint, final Function<HttpRequest.Builder, HttpRequest> builder, final Function<String, Either<T, String>> parser) {
		LOGGER.debug("Sending request to {}", endpoint);

		final HttpRequest.Builder request = requestBuilder(endpoint);
		if (request == null) {
			return Either.right("Connection not configured");
		}

		try {
			final HttpResponse<String> response = CLIENT.send(builder.apply(request), HttpResponse.BodyHandlers.ofString());
			LOGGER.debug("Got {} from {}: {}", response.statusCode(), endpoint, response.body());

			if (response.statusCode() >= 200 && response.statusCode() < 300) {
				return parser.apply(response.body());
			}

			return Either.right(response.body());
		} catch (final IOException | InterruptedException e) {
			return Either.right(e.toString());
		}
	}

	public Either<Unit, String> post(final String endpoint) {
		return request(endpoint,
				request -> request.POST(HttpRequest.BodyPublishers.noBody()).build(),
				string -> Either.left(Unit.INSTANCE)
		);
	}

	public <T> Either<T, String> get(final String endpoint, final Codec<T> codec) {
		return request(endpoint,
				request -> request.GET().build(),
				body -> {
					try {
						final JsonElement json = JsonParser.parseString(body);
						return codec.parse(JsonOps.INSTANCE, json).get().mapRight(DataResult.PartialResult::message);
					} catch (final JsonParseException e) {
						return Either.right("Malformed JSON: " + e.getMessage());
					}
				}
		);
	}
}
