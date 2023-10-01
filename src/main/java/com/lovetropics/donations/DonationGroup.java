package com.lovetropics.donations;

import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.stream.Stream;

public enum DonationGroup implements StringRepresentable {
    ALL("all"),
    ;

    public static final EnumCodec<DonationGroup> CODEC = StringRepresentable.fromEnum(DonationGroup::values);

    private final String name;

    DonationGroup(final String name) {
        this.name = name;
    }

    public static Stream<String> names() {
        return Arrays.stream(values()).map(DonationGroup::getSerializedName);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
