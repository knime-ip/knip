package org.knime.knip.bdv.events;

import org.scijava.event.SciJavaEvent;

/**
 * 
 * Remove a source from a group.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class RemoveSourceFromGroupEvent extends SciJavaEvent {

	/**
	 * Name of the group.
	 */
	private final String groupName;

	/**
	 * Name of the source.
	 */
	private final String sourceName;

	/**
	 * Remove a source from a group.
	 * 
	 * @param sourceName
	 * @param groupName
	 */
	public RemoveSourceFromGroupEvent(final String sourceName, final String groupName) {
		this.sourceName = sourceName;
		this.groupName = groupName;
	}

	/**
	 * 
	 * @return name of the group
	 */
	public String getGroupName() {
		return groupName;
	}

	/**
	 * 
	 * @return name of the source
	 */
	public String getSourceName() {
		return sourceName;
	}

}
