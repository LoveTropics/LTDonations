package com.lovetropics.donations.target;

import com.lovetropics.lib.codec.CodecRegistry;
import com.lovetropics.lib.permission.PermissionsApi;
import com.lovetropics.lib.permission.role.Role;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;

import java.util.Collections;
import java.util.List;

public class Targets {

    public static final CodecRegistry<String, MapCodec<? extends Target>> REGISTRY = Util.make(CodecRegistry.stringKeys(), registry -> {
       registry.register("tag", TagTarget.CODEC);
       registry.register("role", RoleTarget.CODEC);
       registry.register("all", AllTarget.CODEC);
    });

    public static final Codec<Target> CODEC = REGISTRY.dispatch(Target::getCodec, mapCodec -> mapCodec);

    public record TagTarget(String tag) implements Target {
        public static final MapCodec<TagTarget> CODEC = Codec.STRING.fieldOf("tag").xmap(TagTarget::new, TagTarget::tag);

        @Override
        public List<ServerPlayer> getTargets(MinecraftServer server) {
            return server.getPlayerList().getPlayers().stream()
                    .filter(player -> player.entityTags().contains(tag))
                    .toList();
        }

        @Override
        public MapCodec<? extends Target> getCodec() {
            return CODEC;
        }
    }

    public record RoleTarget(String roleId) implements Target {
        public static final MapCodec<RoleTarget> CODEC = Codec.STRING.fieldOf("role").xmap(RoleTarget::new, RoleTarget::roleId);

        @Override
        public List<ServerPlayer> getTargets(MinecraftServer server) {
            Role role = PermissionsApi.provider().get(roleId);
            if (role == null) {
                return Collections.emptyList();
            }
            return server.getPlayerList().getPlayers().stream()
                    .filter(player -> PermissionsApi.lookup().byPlayer(player).has(role))
                    .toList();
        }

        @Override
        public MapCodec<? extends Target> getCodec() {
            return CODEC;
        }
    }

    public record AllTarget() implements Target {

        public static final MapCodec<AllTarget> CODEC = MapCodec.unit(new AllTarget());

        @Override
        public List<ServerPlayer> getTargets(MinecraftServer server) {
            return server.getPlayerList().getPlayers();
        }

        @Override
        public MapCodec<? extends Target> getCodec() {
            return CODEC;
        }
    }
}
