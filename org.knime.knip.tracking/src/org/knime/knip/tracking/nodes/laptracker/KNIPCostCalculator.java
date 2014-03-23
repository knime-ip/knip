package org.knime.knip.tracking.nodes.laptracker;

import java.util.Map;

import org.knime.knip.tracking.data.TrackedNode;

import fiji.plugin.trackmate.FeatureHolderUtils;
import fiji.plugin.trackmate.tracking.TrackableObjectUtils;
import fiji.plugin.trackmate.tracking.costfunction.CostCalculator;

public class KNIPCostCalculator<L extends Comparable<L>> implements
		CostCalculator<TrackedNode<L>> {

	@Override
	public double computeLinkingCostFor(TrackedNode<L> t0, TrackedNode<L> t1,
			double distanceCutOff, double blockingValue,
			Map<String, Double> featurePenalties) {

		double d2 = TrackableObjectUtils.squareDistanceTo(t0, t1);

		// Distance threshold
		if (d2 > distanceCutOff * distanceCutOff) {
			return blockingValue;
		}

		double penalty = 1;
		for (String feature : featurePenalties.keySet()) {
			double ndiff = FeatureHolderUtils.normalizeDiffTo(t0, t1, feature);
			if (Double.isNaN(ndiff))
				continue;
			double factor = featurePenalties.get(feature);
			penalty += factor * 1.5 * ndiff;
		}

		// Set score
		return d2 * penalty * penalty;

	}

}