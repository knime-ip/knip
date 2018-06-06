package org.knime.knip.bdv.events;

import org.scijava.event.SciJavaEvent;

/**
 * 
 * Group selection has changed.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class GroupSelectedEvent extends SciJavaEvent {

	/**
	 * Name of the selected group.
	 */
	private final String groupName;

	/**
	 * 
	 * Selected group has changed.
	 * 
	 * @param groupName
	 *            of the new selection
	 */
	public GroupSelectedEvent(final String groupName) {
		this.groupName = groupName;
	}

	/**
	 * 
	 * @return name of the new selected group
	 */
	public String getGroupName() {
		return groupName;
	}
}