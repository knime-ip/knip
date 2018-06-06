package org.knime.knip.bdv.uicomponents;

import java.util.HashSet;
import java.util.Set;

/**
 * 
 * GroupProperties holds all information about a group added to the UI.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class GroupProperties {
	private String groupName;
	private Set<String> sourceNames;
	private boolean visibility;

	/**
	 * Information about a specific group.
	 * 
	 * @param groupName
	 * @param groupID
	 * @param sourceNames
	 * @param sourceIDs
	 * @param visibility
	 */
	public GroupProperties(final String groupName, final String sourceName, final boolean visibility) {
		this.groupName = groupName;
		this.sourceNames = new HashSet<>();
		this.sourceNames.add(sourceName);
		this.visibility = visibility;
	}

	/**
	 * Creates an empty group with no source.
	 * 
	 * @param groupName
	 * @param visibility
	 */
	public GroupProperties(final String groupName, final boolean visibility) {
		this.groupName = groupName;
		this.sourceNames = new HashSet<>();
		this.visibility = visibility;
	}

	/**
	 * 
	 * @return group name
	 */
	public String getGroupName() {
		return groupName;
	}

	/**
	 * Names of all sources assigned to this group.
	 * 
	 * @return names of the sources
	 */
	public Set<String> getSourceNames() {
		return sourceNames;
	}

	/**
	 * Add a source to this group.
	 * 
	 * @param sourceName
	 * @param sourceID
	 */
	public void addSource(final String sourceName) {
		sourceNames.add(sourceName);
	}

	/**
	 * Remove a source from this group.
	 * 
	 * @param sourceName
	 * @param sourceID
	 */
	public void removeSource(final String sourceName) {
		sourceNames.remove(sourceName);
	}

	/**
	 * 
	 * @return visibility
	 */
	public boolean isVisible() {
		return visibility;
	}

	/**
	 * Set visibility.
	 * 
	 * @param visible
	 */
	public void setVisible(final boolean visible) {
		this.visibility = visible;
	}

	@Override
	public boolean equals(Object obj) {

		if (obj instanceof GroupProperties) {
			GroupProperties g = (GroupProperties) obj;
			return g.getGroupName() == this.groupName;
		}
		return false;
	}
}
