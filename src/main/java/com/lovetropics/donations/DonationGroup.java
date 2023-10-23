package com.lovetropics.donations;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.stream.Stream;

public enum DonationGroup implements StringRepresentable {
    ALL("all", DonationLangKeys.GROUP_ALL.getComponent()),
    TEAM_CENTS("team_cents", DonationLangKeys.TEAM_CENTS.getComponent().copy().withStyle(ChatFormatting.GREEN)),
    TEAM_NO_CENTS("team_no_cents", DonationLangKeys.TEAM_NO_CENTS.getComponent().copy().withStyle(ChatFormatting.GOLD)),
    TEAM_NICE("team_nice", DonationLangKeys.TEAM_NICE.getComponent().copy().withStyle(ChatFormatting.RED)),
    ;

    public static final EnumCodec<DonationGroup> CODEC = StringRepresentable.fromEnum(DonationGroup::values);

    private final String key;
    private final Component name;

    DonationGroup(final String key, final Component name) {
        this.key = key;
        this.name = name;
    }

    public static Stream<String> names() {
        return Arrays.stream(values()).map(DonationGroup::getSerializedName);
    }

    public Component getName() {
        return name;
    }

    @Override
    public String getSerializedName() {
        return key;
    }
}
