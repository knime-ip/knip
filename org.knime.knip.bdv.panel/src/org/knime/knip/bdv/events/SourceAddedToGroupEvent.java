package org.knime.knip.bdv.events;

import org.scijava.event.SciJavaEvent;

/**
 * 
 * Add a source to a group.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class SourceAddedToGroupEvent extends SciJavaEvent {

	/**
	 * Name of the group.
	 */
	private final String groupName;

	/**
	 * Name of the source.
	 */
	private final String sourceName;

	/**
	 * Add source to the group.
	 * 
	 * @param sourceName
	 * @param groupName
	 */
	public SourceAddedToGroupEvent(final String sourceName, final String groupName) {
		this.sourceName = sourceName;
		this.groupName = groupName;
	}

	/**
	 * 
	 * @return name of the source
	 */
	public String getSource() {
		return this.sourceName;
	}

	/**
	 * 
	 * @return name of the group
	 */
	public String getGroup() {
		return this.groupName;
	}

}
