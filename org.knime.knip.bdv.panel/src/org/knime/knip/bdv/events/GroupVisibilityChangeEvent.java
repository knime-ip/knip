package org.knime.knip.bdv.events;

import org.scijava.event.SciJavaEvent;

import bdv.BigDataViewer;

/**
 * 
 * Change visibility of a group in {@link BigDataViewer}.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class GroupVisibilityChangeEvent extends SciJavaEvent {

	/**
	 * Name of the group.
	 */
	private final String groupName;

	/**
	 * Visibility of the group.
	 */
	private final boolean isVisible;

	/**
	 * Change visibility of a group.
	 * 
	 * @param groupName
	 *            of which visibility will be changed
	 * @param visibility
	 *            of the group
	 */
	public GroupVisibilityChangeEvent(final String groupName, final boolean visibility) {
		this.groupName = groupName;
		this.isVisible = visibility;
	}

	/**
	 * 
	 * @return name of the group
	 */
	public String getGroupName() {
		return this.groupName;
	}

	/**
	 * 
	 * @return visibility
	 */
	public boolean isVisible() {
		return this.isVisible;
	}
}
