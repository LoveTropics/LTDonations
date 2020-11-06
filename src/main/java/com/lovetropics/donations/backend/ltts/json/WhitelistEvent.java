package com.lovetropics.donations.backend.ltts.json;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class WhitelistEvent {

	public enum Type {
		whitelist,
		blacklist,
		;
	}

	public Type type;
	public String name;

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
	}
}
