package org.knime.knip.bdv.uicomponents;

import java.awt.Color;
import java.util.Set;

import net.imglib2.type.numeric.NumericType;

/**
 * 
 * SourceProperties holds all information about a source added to the UI.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class MetaSourceProperties<T extends NumericType<T>> {
	private String sourceName;
	private int sourceID;
	private String sourceType;
	private Set<String> groupNames;
	private Color color;
	private String dims;
	private boolean visibility;
	private boolean isLabeling;

	/**
	 * Information about a specific source.
	 * 
	 * @param sourceName
	 * @param sourceID
	 * @param sourceType
	 * @param groupNames
	 * @param groupIDs
	 * @param color
	 * @param dims
	 * @param visibility
	 */
	public MetaSourceProperties(final String sourceName, final int sourceID, final String sourceType,
			final Set<String> groupNames, final Color color, final String dims, final boolean visibility,
			final boolean isLabeling) {
		this.sourceName = sourceName;
		this.sourceID = sourceID;
		this.sourceType = sourceType;
		this.groupNames = groupNames;
		this.color = color;
		this.dims = dims;
		this.visibility = visibility;
		this.isLabeling = isLabeling;
	}

	/**
	 * 
	 * @return source name
	 */
	public String getSourceName() {
		return sourceName;
	}

	/**
	 * 
	 * @return source ID
	 */
	public int getSourceID() {
		return sourceID;
	}
	
	/**
	 * Set sourceID
	 * 
	 * @param id
	 */
	public void setSourceID(int id) {
		sourceID = id;
	}

	/**
	 * 
	 * @return type of the source
	 */
	public String getSourceType() {
		return sourceType;
	}

	/**
	 * Names of the groups to which this source is assigned.
	 * 
	 * @return names of the groups
	 */
	public Set<String> getGroupNames() {
		return groupNames;
	}

	/**
	 * Adds a group to this source.
	 * 
	 * @param groupName
	 * @param groupID
	 */
	public void addGroup(final String groupName, final int groupID) {
		groupNames.add(groupName);
	}

	/**
	 * Remove group from this source.
	 * 
	 * @param groupName
	 * @param groupID
	 */
	public void removeGroup(final String groupName, final int groupID) {
		groupNames.remove(groupName);
	}

	/**
	 * 
	 * @return color of the source
	 */
	public Color getColor() {
		return color;
	}

	/**
	 * 
	 * @param color
	 *            of the source
	 */
	public void setColor(final Color color) {
		this.color = color;
	}

	/**
	 * 
	 * @return dimensions of the source
	 */
	public String getDims() {
		return dims;
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

	public boolean isLabeling() {
		return isLabeling;
	}
}