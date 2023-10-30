package com.lovetropics.donations;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.util.Mth;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class DonationScoreboard implements DonationStateListener {
    private static final DonationGroup[] DONATION_GROUPS = DonationGroup.values();
    private static final String TOTALS_OBJECTIVE = "donations.totals";
    private static final String COUNTS_OBJECTIVE = "donations.counts";

    @Override
    public void handleState(final MinecraftServer server, final DonationState state, final boolean initial) {
        updateScoreboard(server.getScoreboard(), state);
    }

    private static void updateScoreboard(final ServerScoreboard scoreboard, final DonationState state) {
        final Objective totalsObjective = getOrCreateObjective(scoreboard, TOTALS_OBJECTIVE);
        final Objective countsObjective = getOrCreateObjective(scoreboard, COUNTS_OBJECTIVE);
        for (final DonationGroup group : DONATION_GROUPS) {
            final int total = Mth.floor(state.getAmount(group));
            final int count = state.getCount(group);
            scoreboard.getOrCreatePlayerScore(group.getSerializedName(), totalsObjective).setScore(total);
            scoreboard.getOrCreatePlayerScore(group.getSerializedName(), countsObjective).setScore(count);
        }
    }

    private static Objective getOrCreateObjective(final ServerScoreboard scoreboard, final String name) {
        final Objective objective = scoreboard.getObjective(name);
        if (objective == null) {
            return scoreboard.addObjective(name, ObjectiveCriteria.DUMMY, Component.literal("Donations"), ObjectiveCriteria.RenderType.INTEGER);
        }
        return objective;
    }
}
