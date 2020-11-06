package com.lovetropics.donations.backend.ltts.json;

import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class PendingEventList<T> {
	
	public List<T> events;

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
