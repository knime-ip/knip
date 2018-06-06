package org.knime.knip.bdv.events;

import org.scijava.event.SciJavaEvent;

import bdv.BigDataViewer;

/**
 * 
 * Add a new group to {@link BigDataViewer}.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class GroupAddNewEvent extends SciJavaEvent {

	/**
	 * Name of the new group.
	 */
	private final String newGroupName;

	/**
	 * Add a new group.
	 * 
	 * @param newGroupName
	 */
	public GroupAddNewEvent(final String newGroupName) {
		this.newGroupName = newGroupName;
	}

	/**
	 * 
	 * @return name of the new group
	 */
	public String getGroupName() {
		return newGroupName;
	}
}
