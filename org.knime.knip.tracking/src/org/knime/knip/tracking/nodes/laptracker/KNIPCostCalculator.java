package org.knime.knip.tracking.nodes.laptracker;

import java.util.Map;

import org.knime.knip.tracking.data.TrackedNode;

import fiji.plugin.trackmate.tracking.oldlap.costfunction.CostCalculator;
import fiji.plugin.trackmate.util.FeatureHolderUtils;
import fiji.plugin.trackmate.util.TrackableObjectUtils;

public class KNIPCostCalculator<L extends Comparable<L>> implements
		CostCalculator<TrackedNode<L>> {

	@Override
	public double computeLinkingCostFor(final TrackedNode<L> t0, final TrackedNode<L> t1,
			final double distanceCutOff, final double blockingValue,
			final Map<String, Double> featurePenalties) {

		final double d2 = TrackableObjectUtils.squareDistanceTo(t0, t1);

		// Distance threshold
		if (d2 > distanceCutOff * distanceCutOff) {
			return blockingValue;
		}

		double penalty = 1;
		for (final String feature : featurePenalties.keySet()) {
			final double ndiff = FeatureHolderUtils.normalizeDiffTo(t0, t1, feature);
			if (Double.isNaN(ndiff))
				continue;
			final double factor = featurePenalties.get(feature);
			penalty += factor * 1.5 * ndiff;
		}

		// Set score
		return d2 * penalty * penalty;

	}

}