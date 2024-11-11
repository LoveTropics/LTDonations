package com.lovetropics.donations.trigger;

import com.lovetropics.donations.DonationGroup;
import com.lovetropics.donations.DonationListener;
import com.lovetropics.donations.DonationState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.MinMaxBounds;

import java.util.Optional;

public record DonationPredicate(
		MinMaxBounds.Doubles amount,
		Optional<Total> totalExceeded,
		Optional<Double> donorTotalExceeded
) {
	public static final Codec<DonationPredicate> CODEC = RecordCodecBuilder.create(i -> i.group(
			MinMaxBounds.Doubles.CODEC.optionalFieldOf("amount", MinMaxBounds.Doubles.ANY).forGetter(DonationPredicate::amount),
			Total.CODEC.optionalFieldOf("total_exceeded").forGetter(DonationPredicate::totalExceeded),
			Codec.DOUBLE.optionalFieldOf("donor_total_exceeded").forGetter(DonationPredicate::donorTotalExceeded)
	).apply(i, DonationPredicate::new));

	public boolean test(DonationListener.Details details) {
		if (!amount.matches(details.amount())) {
			return false;
		}
		if (totalExceeded.isPresent() && !totalExceeded.get().test(details.oldState(), details.newState())) {
			return false;
		}
		if (donorTotalExceeded.isPresent()) {
			double threshold = donorTotalExceeded.get();
			if (details.donorTotal() < threshold || details.donorTotal() - details.amount() >= threshold) {
				return false;
			}
		}
		return true;
	}

	public record Total(double amount, DonationGroup group) {
		private static final Codec<Total> FULL_CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.DOUBLE.fieldOf("amount").forGetter(Total::amount),
				DonationGroup.CODEC.optionalFieldOf("group", DonationGroup.ALL).forGetter(Total::group)
		).apply(i, Total::new));

		public static final Codec<Total> CODEC = Codec.withAlternative(
				FULL_CODEC,
				Codec.DOUBLE, amount -> new Total(amount, DonationGroup.ALL)
		);

		public boolean test(DonationState oldState, DonationState newState) {
			return oldState.getAmount(group) < amount && newState.getAmount(group) >= amount;
		}
	}
}
