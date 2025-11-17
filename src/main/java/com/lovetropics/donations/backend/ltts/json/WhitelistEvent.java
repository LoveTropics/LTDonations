package com.lovetropics.donations.backend.ltts.json;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.StringRepresentable;

import java.util.Optional;
import java.util.UUID;

public record WhitelistEvent(Type type, String profileName, Optional<UUID> profileId) {
    public static final Codec<WhitelistEvent> CODEC = RecordCodecBuilder.create(i -> i.group(
            Type.CODEC.fieldOf("type").forGetter(WhitelistEvent::type),
            Codec.STRING.fieldOf("name").forGetter(WhitelistEvent::profileName),
            // No ID is sent as an empty string if the techstack fails to fetch it
            UUIDUtil.AUTHLIB_CODEC.lenientOptionalFieldOf("uuid").forGetter(WhitelistEvent::profileId)
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
