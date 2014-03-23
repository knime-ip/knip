package org.knime.knip.tracking.nodes.laptracker;

import java.util.Map;

import org.knime.knip.tracking.data.TrackedNode;
import org.knime.knip.tracking.nodes.laptracker.LAPTrackerNodeModel.LAPTrackerAlgorithm;

import fiji.plugin.trackmate.tracking.TrackableObjectCollection;
import fiji.plugin.trackmate.tracking.hungarian.AssignmentAlgorithm;
import fiji.plugin.trackmate.tracking.hungarian.HungarianAlgorithm;
import fiji.plugin.trackmate.tracking.hungarian.MunkresKuhnAlgorithm;
import fiji.plugin.trackmate.tracking.trackers.LAPTracker;

public class GenericLapTracker<L extends Comparable<L>> extends
		LAPTracker<TrackedNode<L>> {

	private final LAPTrackerAlgorithm algorithm;

	public GenericLapTracker(LAPTrackerAlgorithm algorithm, TrackableObjectCollection<TrackedNode<L>> objects, Map<String, Object> defSettings) {
		super(new KNIPCostCalculator<L>(), objects, defSettings );
		this.algorithm = algorithm;
	}

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
