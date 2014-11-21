package org.knime.knip.tracking.nodes.laptracker;

import java.util.Map;

import org.knime.knip.tracking.data.TrackedNode;
import org.knime.knip.tracking.nodes.laptracker.LAPTrackerNodeModel.LAPTrackerAlgorithm;

import fiji.plugin.trackmate.tracking.TrackableObjectCollection;
import fiji.plugin.trackmate.tracking.oldlap.LAPTracker;
import fiji.plugin.trackmate.tracking.oldlap.hungarian.AssignmentAlgorithm;
import fiji.plugin.trackmate.tracking.oldlap.hungarian.HungarianAlgorithm;
import fiji.plugin.trackmate.tracking.oldlap.hungarian.JonkerVolgenantAlgorithm;
import fiji.plugin.trackmate.tracking.oldlap.hungarian.MunkresKuhnAlgorithm;

public class GenericLapTracker<L extends Comparable<L>> extends
		LAPTracker<TrackedNode<L>> {

	private final LAPTrackerAlgorithm algorithm;

	public GenericLapTracker(final LAPTrackerAlgorithm algorithm, final TrackableObjectCollection<TrackedNode<L>> objects, final Map<String, Object> defSettings) {
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
		case JONKERVOLGENANT:
			return new JonkerVolgenantAlgorithm();
			
		default:
			throw new IllegalArgumentException("Unknown LAPTracker");
		}
	}
}
