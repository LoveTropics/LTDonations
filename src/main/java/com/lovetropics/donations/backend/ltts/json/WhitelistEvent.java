package com.lovetropics.donations.backend.ltts.json;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

public record WhitelistEvent(Type type, String name) {
	public static final Codec<WhitelistEvent> CODEC = RecordCodecBuilder.create(i -> i.group(
			Type.CODEC.fieldOf("type").forGetter(WhitelistEvent::type),
			Codec.STRING.fieldOf("name").forGetter(WhitelistEvent::name)
	).apply(i, WhitelistEvent::new));

	public enum Type implements StringRepresentable {
		WHITELIST("whitelist"),
		BLACKLIST("blacklist"),
		;

		public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

		private final String name;

		Type(final String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}
}
