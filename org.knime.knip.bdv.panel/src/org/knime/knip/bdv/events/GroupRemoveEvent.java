package org.knime.knip.bdv.events;

import org.scijava.event.SciJavaEvent;

import bdv.BigDataViewer;

/**
 * 
 * Remove group from {@link BigDataViewer}.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class GroupRemoveEvent extends SciJavaEvent {

	private final String groupName;

	/**
	 * Remove group.
	 * 
	 * @param groupName
	 *            name to removes
	 */
	public GroupRemoveEvent(final String groupName) {
		this.groupName = groupName;
	}

	/**
	 * 
	 * @return group name to remove
	 */
	public String getGroupName() {
		return groupName;
	}

}
