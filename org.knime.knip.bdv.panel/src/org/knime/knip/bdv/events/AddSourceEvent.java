package org.knime.knip.bdv.events;

import java.awt.Color;
import java.util.Set;

import org.knime.knip.bdv.uicomponents.MetaSourceProperties;
import org.scijava.event.SciJavaEvent;

import bdv.BigDataViewer;
import net.imglib2.type.numeric.NumericType;

/**
 * 
 * Add new source to {@link BigDataViewer}.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class AddSourceEvent<T extends NumericType<T>> extends SciJavaEvent {

	private final MetaSourceProperties<T> properties;

	/**
	 * Add a new source to the {@link BigDataViewer}.
	 * 
	 * Note: Type and dimensions have to be provided. The GUI will not perform any
	 * information extraction.
	 * 
	 * @param sourceName
	 *            a unique name
	 * @param sourceID
	 *            a unique ID
	 * @param sourceType
	 *            displayed as information
	 * @param groupNames
	 *            an image is per default part of group "All"
	 * @param groupIDs
	 *            --> maybe remove
	 * @param color
	 *            display color
	 * @param dims
	 *            displayed as information
	 * @param visibility
	 *            of the source
	 */
	public AddSourceEvent(final String sourceName, final int sourceID, final String sourceType,
			final Set<String> groupNames, final Color color, final String dims,
			final boolean visibility, final boolean isLabeling) {
		properties = new MetaSourceProperties<>(sourceName, sourceID, sourceType, groupNames, color, dims, visibility, isLabeling);
	}

	/**
	 * 
	 * @return name of the source
	 */
	public String getSourceName() {
		return properties.getSourceName();
	}

	/**
	 * 
	 * @return id of the source
	 */
	public int getSourceID() {
		return properties.getSourceID();
	}

	/**
	 * 
	 * @return type which will be displayed
	 */
	public String getType() {
		return properties.getSourceType();
	}

	/**
	 * 
	 * @return groups to which this source is added
	 */
	public Set<String> getGroupNames() {
		return properties.getGroupNames();
	}

	/**
	 * 
	 * @return display color of this source
	 */
	public Color getColor() {
		return properties.getColor();
	}

	/**
	 * 
	 * @return dims which will be displayed
	 */
	public String getDims() {
		return properties.getDims();
	}

	/**
	 * 
	 * @return visibility of this source
	 */
	public boolean isVisible() {
		return properties.isVisible();
	}

	public boolean isLabeling() {
		return properties.isLabeling();
	}
}
