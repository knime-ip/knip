package org.knime.knip.bdv.events;

import org.knime.knip.bdv.uicomponents.MetaSourceProperties;
import org.scijava.event.SciJavaEvent;

import net.imglib2.type.numeric.NumericType;

/**
 * 
 * Current source selection has changed.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class SourceSelectionChangeEvent<T extends NumericType<T>> extends SciJavaEvent {

	/**
	 * Properties of the source.
	 */
	private final MetaSourceProperties<T> source;

	/**
	 * Selected source has changed.
	 * 
	 * @param source
	 *            properties of the new selected source
	 */
	public SourceSelectionChangeEvent(final MetaSourceProperties<T> source) {
		this.source = source;
	}

	/**
	 * 
	 * @return properties of the new selection
	 */
	public MetaSourceProperties<T> getSource() {
		return source;
	}
}
