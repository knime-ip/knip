package org.knime.knip.bdv.events;

import org.scijava.event.SciJavaEvent;

import bdv.BigDataViewer;
import bdv.viewer.Interpolation;

/**
 * 
 * Change {@link Interpolation} mode.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class InterpolationModeChangeEvent extends SciJavaEvent {

	/**
	 * Interpolation mode.
	 */
	private final Interpolation interpolation;

	/**
	 * Change {@link Interpolation} mode of the {@link BigDataViewer}.
	 * 
	 * @param mode
	 */
	public InterpolationModeChangeEvent(final Interpolation mode) {
		this.interpolation = mode;
	}

	/**
	 * 
	 * @return {@link Interpolation} mode
	 */
	public Interpolation getInterpolationMode() {
		return interpolation;
	}
}
