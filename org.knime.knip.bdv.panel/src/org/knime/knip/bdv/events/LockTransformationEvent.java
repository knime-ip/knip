package org.knime.knip.bdv.events;

import org.scijava.event.SciJavaEvent;

import bdv.BigDataViewer;

/**
 * 
 * Restrict transformations of the {@link BigDataViewer}.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class LockTransformationEvent extends SciJavaEvent {

	/**
	 * Allow translation.
	 */
	private final boolean allowTranslation;

	/**
	 * Allow rotation.
	 */
	private final boolean allowRotation;

	/**
	 * Restrict transformation.
	 * 
	 * @param allowTranslation
	 *            of the {@link BigDataViewer}
	 * @param allowRotation
	 *            of the {@link BigDataViewer}
	 */
	public LockTransformationEvent(final boolean allowTranslation, final boolean allowRotation) {
		this.allowTranslation = allowTranslation;
		this.allowRotation = allowRotation;
	}

	/**
	 * 
	 * @return allow translation
	 */
	public boolean allowTranslation() {
		return allowTranslation;
	}

	/**
	 * 
	 * @return allow rotation
	 */
	public boolean allowRotation() {
		return allowRotation;
	}
}
