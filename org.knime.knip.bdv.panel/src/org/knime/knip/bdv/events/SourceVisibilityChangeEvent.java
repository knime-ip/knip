package org.knime.knip.bdv.events;

import org.scijava.event.SciJavaEvent;

import bdv.BigDataViewer;

/**
 * 
 * Change visibility of a source in {@link BigDataViewer}.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class SourceVisibilityChangeEvent extends SciJavaEvent {

	/**
	 * Name of the source.
	 */
	private final String sourceName;

	/**
	 * Visibility of the source.
	 */
	private final boolean isVisible;

	/**
	 * Change visibility of a source.
	 * 
	 * @param sourceName
	 *            of which visibility will be changed
	 * @param visibility
	 *            of the source
	 */
	public SourceVisibilityChangeEvent(final String sourceName, final boolean visibility) {
		this.sourceName = sourceName;
		this.isVisible = visibility;
	}

	/**
	 * 
	 * @return name of the source
	 */
	public String getSourceName() {
		return this.sourceName;
	}

	/**
	 * 
	 * @return visibility
	 */
	public boolean isVisible() {
		return this.isVisible;
	}
}
