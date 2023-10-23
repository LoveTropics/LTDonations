package com.lovetropics.donations.monument;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public class MonumentEffects {
    public static void spawnParticles(final ServerLevel level, final BlockPos pos) {
        final RandomSource random = level.getRandom();
        final Vec3 center = Vec3.atLowerCornerOf(pos).add(0.5, 0.5, 0.5);
        for (int i = 0; i < 20; i++) {
            final Direction dir = random.nextInt(3) != 0 ? Direction.UP : Direction.from2DDataValue(random.nextInt(4));
            final Vec3 spawnPos = center.add(Vec3.atLowerCornerOf(dir.getNormal()).scale(0.6f))
                    .add((random.nextDouble() - 0.5) * (1 - Math.abs(dir.getStepX())),
                            (random.nextDouble() - 0.5) * (1 - Math.abs(dir.getStepY())),
                            (random.nextDouble() - 0.5) * (1 - Math.abs(dir.getStepZ())));
            final Vec3 speed = spawnPos.subtract(center);
            level.sendParticles(ParticleTypes.END_ROD, spawnPos.x, spawnPos.y, spawnPos.z, 0, speed.x, speed.y, speed.z, 0.075);
        }
    }
}
