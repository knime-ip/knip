package org.knime.knip.bdv.events;

import org.scijava.event.SciJavaEvent;

import bdv.BigDataViewer;

/**
 * 
 * Remove source from {@link BigDataViewer}.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class RemoveSourceEvent extends SciJavaEvent {

	/**
	 * Name of the source to remove.
	 */
	private final String sourceName;
	
	/**
	 * Index of the source to remove.
	 */
	private final int index;

	/**
	 * Remove source.
	 * 
	 * @param sourceName
	 * @param index
	 */
	public RemoveSourceEvent(final String sourceName, final int index) {
		this.sourceName = sourceName;
		this.index = index;
	}

	/**
	 * 
	 * @return name of the source to remove
	 */
	public String getSourceName() {
		return sourceName;
	}
	
	/**
	 * 
	 * @return index of the source to remove
	 */
	public int getSourceIndex() { 
		return index;
	}
}
