package org.knime.knip.bdv.events;

import org.scijava.event.SciJavaEvent;

/**
 * 
 * Change the display range of the selected source.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class DisplayRangeChangedEvent extends SciJavaEvent {

	private int sourceID;
	private double min;
	private double max;

	/**
	 * 
	 * @param sourceId
	 * @param min
	 * @param max
	 */
	public DisplayRangeChangedEvent(final int sourceId, final double min, final double max) {
		this.sourceID = sourceId;
		this.min = min;
		this.max = max;
	}

	/**
	 * @return the sourceID
	 */
	public int getSourceID() {
		return sourceID;
	}

	/**
	 * @return the min
	 */
	public double getMin() {
		return min;
	}

	/**
	 * @return the max
	 */
	public double getMax() {
		return max;
	}

}
