package org.knime.knip.bdv.events;

import org.scijava.event.SciJavaEvent;

import bdv.viewer.DisplayMode;

/**
 * 
 * Set state of {@link DisplayMode#FUSED} or {@link DisplayMode#FUSEDGROUP}.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class DisplayModeFuseActiveEvent extends SciJavaEvent {

	/**
	 * Active state of fuse-mode.
	 */
	private final boolean active;

	/**
	 * Set {@link DisplayMode#FUSED} or {@link DisplayMode#FUSEDGROUP} active.
	 * 
	 * @param active
	 */
	public DisplayModeFuseActiveEvent(final boolean active) {
		this.active = active;
	}

	/**
	 * 
	 * @return active state
	 */
	public boolean isActive() {
		return active;
	}
}
