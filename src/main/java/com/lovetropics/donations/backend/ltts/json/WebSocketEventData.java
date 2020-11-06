package com.lovetropics.donations.backend.ltts.json;

import com.google.gson.JsonObject;

public class WebSocketEventData {

	public String type;
	public EventAction crud;
	public JsonObject payload;
}
