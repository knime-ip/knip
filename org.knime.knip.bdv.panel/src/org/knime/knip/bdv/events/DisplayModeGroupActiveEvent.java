package org.knime.knip.bdv.events;

import org.scijava.event.SciJavaEvent;

import bdv.viewer.DisplayMode;

/**
 * 
 * Set state of {@link DisplayMode#GROUP} or {@link DisplayMode#FUSEDGROUP}.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class DisplayModeGroupActiveEvent extends SciJavaEvent {

	/**
	 * Active state of group-mode.
	 */
	private final boolean active;

	/**
	 * Set {@link DisplayMode#GROUP} or {@link DisplayMode#FUSEDGROUP} active.
	 * 
	 * @param active
	 */
	public DisplayModeGroupActiveEvent(final boolean active) {
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
