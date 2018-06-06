package org.knime.knip.bdv.uicomponents;

import java.awt.Color;
import java.util.Set;

import bdv.util.BdvStackSource;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;

/**
 * 
 * SourceProperties holds all information about a source added to  the UI.
 * 
 * @author Tim-Oliver Buchholz, CSBD/MPI-CBG Dresden
 *
 */
public class SourceProperties<T extends NumericType<T>> extends MetaSourceProperties<T> {
	private AffineTransform3D transformation;
	private BdvStackSource<T> source;

	/**
	 * Information about a specific source.
	 * 
	 * @param sourceName 
	 * @param sourceID
	 * @param sourceType
	 * @param groupNames
	 * @param color
	 * @param dims
	 * @param visibility
	 */
	public SourceProperties(final String sourceName, final int sourceID, final String sourceType,
			final Set<String> groupNames, final Color color, final String dims,
			final boolean visibility, final AffineTransform3D transformation, final BdvStackSource<T> source, final boolean isLabeling) {
		super(sourceName, sourceID, sourceType, groupNames, color, dims, visibility, isLabeling);
		this.transformation = transformation;
		this.source = source;
	}
	
	/**
	 * Get the transformation of this source.
	 * 
	 * @return the transformation
	 */
	public AffineTransform3D getTransformation() {
		return transformation;
	}
	
	/**
	 * Set transformation of this source.
	 * @param t
	 */
	public void setTransformation(final AffineTransform3D t) {
		this.transformation = t;
	}
	
	/**
	 * Get the actual image source.
	 * 
	 * @return the image
	 */
	public BdvStackSource<T> getSource() {
		return source;
	}
}