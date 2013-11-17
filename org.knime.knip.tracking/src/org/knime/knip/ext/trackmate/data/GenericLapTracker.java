package org.knime.knip.ext.trackmate.data;

import java.util.Map;

import org.knime.knip.ext.trackmate.nodes.tracker.LAPTrackerNodeModel.LAPTrackerAlgorithm;

import fiji.plugin.trackmate.FeatureHolderUtils;
import fiji.plugin.trackmate.tracking.LAPTracker;
import fiji.plugin.trackmate.tracking.TrackableObject;
import fiji.plugin.trackmate.tracking.TrackingUtils;
import fiji.plugin.trackmate.tracking.costfunction.CostCalculator;
import fiji.plugin.trackmate.tracking.hungarian.AssignmentAlgorithm;
import fiji.plugin.trackmate.tracking.hungarian.HungarianAlgorithm;
import fiji.plugin.trackmate.tracking.hungarian.MunkresKuhnAlgorithm;

public class GenericLapTracker<L extends Comparable<L>> extends
		LAPTracker<TrackedNode<L>> {

	private final LAPTrackerAlgorithm algorithm;

	public GenericLapTracker(LAPTrackerAlgorithm algorithm) {
		this.algorithm = algorithm;
	}

	// Currently the same implementation
	class KNIPCostCalculator implements CostCalculator {

		@Override
		public double computeLinkingCostFor(TrackableObject t0,
				TrackableObject t1, double distanceCutOff,
				double blockingValue, Map<String, Double> featurePenalties) {

			double d2 = TrackingUtils.squareDistanceTo(t0, t1);

			// Distance threshold
			if (d2 > distanceCutOff * distanceCutOff) {
				return blockingValue;
			}

			double penalty = 1;
			for (String feature : featurePenalties.keySet()) {
				double ndiff = FeatureHolderUtils.normalizeDiffToSp(t0, t1,
						feature);
				if (Double.isNaN(ndiff))
					continue;
				double factor = featurePenalties.get(feature);
				penalty += factor * 1.5 * ndiff;
			}

			// Set score
			return d2 * penalty * penalty;

		}

	}

	@Override
	protected CostCalculator defaultCostCalculator() {
		return new KNIPCostCalculator();
	};

	@Override
	protected AssignmentAlgorithm createAssignmentProblemSolver() {
		switch (algorithm) {
		case MUNKRESKUHN:
			return new MunkresKuhnAlgorithm();
		case HUNGARIAN:
			return new HungarianAlgorithm();
		default:
			throw new IllegalArgumentException("Unknown LAPTracker");
		}
	}
}
