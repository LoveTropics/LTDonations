package com.lovetropics.donations.monument;

import com.lovetropics.donations.DonationConfigs;
import com.lovetropics.donations.DonationListener;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Objects;

public class MonumentManager implements DonationListener {
	private static final Logger LOGGER = LogManager.getLogger();

	private double totalAmount;

	@Nullable
	private Monument monument;
	@Nullable
	private GlobalPos lastPos;

	@Override
	public void handleDonation(final MinecraftServer server, final String name, final double amount, final double total) {
		updateTotal(total, false);
	}

	public void updateTotal(final double amount, final boolean fast) {
		totalAmount = amount;
		if (monument != null) {
			monument.updateTotal(amount);
			if (fast) {
				monument.drainAll();
			}
		}
	}

	public void tick(final MinecraftServer server) {
		final GlobalPos pos = DonationConfigs.MONUMENT.active.get() ? DonationConfigs.MONUMENT.pos : null;
		if (!Objects.equals(pos, lastPos)) {
            monument = pos != null ? createMonument(server, pos) : null;
			if (monument != null) {
				monument.updateTotal(totalAmount);
				monument.drainAll();
			}
			lastPos = pos;
		}
		if (monument != null) {
			monument.tick();
		}
	}

	@Nullable
	private Monument createMonument(final MinecraftServer server, final GlobalPos pos) {
		final ServerLevel level = server.getLevel(pos.dimension());
        if (level == null) {
			LOGGER.warn("Failed to find dimension : " + pos.dimension().location());
			return null;
        }
        return Monument.create(level, pos.pos());
    }
}
