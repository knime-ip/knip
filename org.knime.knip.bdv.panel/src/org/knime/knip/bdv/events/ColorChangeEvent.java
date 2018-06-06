package org.knime.knip.bdv.events;

import java.awt.Color;

import org.scijava.event.SciJavaEvent;

/**
 * 
 * Change display color of a specific source.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class ColorChangeEvent extends SciJavaEvent {

	/**
	 * Name of the source of which the color has changed.
	 */
	private final String sourceName;

	/**
	 * The new color.
	 */
	private final Color color;

	/**
	 * Change the color of a specific source.
	 * 
	 * @param sourceName
	 *            which will be changed
	 * @param color
	 *            the new color
	 */
	public ColorChangeEvent(final String sourceName, final Color color) {
		this.sourceName = sourceName;
		this.color = color;
	}

	/**
	 * 
	 * @return source name
	 */
	public String getSourceName() {
		return this.sourceName;
	}

	/**
	 * 
	 * @return new color
	 */
	public Color getColor() {
		return this.color;
	}

}
