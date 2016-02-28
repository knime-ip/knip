/*
 * ------------------------------------------------------------------------
 *
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
  ---------------------------------------------------------------------
 *
 */
package org.knime.knip.features.sets;

import net.imagej.ops.Contingent;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

import org.scijava.ItemIO;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * {@link FeatureSet} to calculate Tamura 2D Features
 *
 * @param <I>
 * @param <O>
 */
@Plugin(type = FeatureSet.class, label = "Tamura Features", description = "<h1> Tamura Features </h1> <h2>Note: Runs only on single Images!</h2> <h2>Description</h2> Feature Set of the Tamura texture features. These features include: <ul> <li> <strong>Coarseness</strong> which relates to distances of notable spatial variations of grey levels, that is, implicitly, to the size of the primitive elements (texels) forming the texture. The proposed computational procedure accounts for differences between the average signals for the non-overlapping windows of different size:</li> <li> <strong>Contrast</strong> which measures how grey levels q; q = 0, 1, ..., qmax, vary in the image g and to what extent their distribution is biased to black or white. </li> <li> <strong>Directionality</strong> which is measured using the frequency distribution of oriented local edges against their directional angles.</li>  </ul> For more examples and information see the <a href=\"https://www.cs.auckland.ac.nz/courses/compsci708s1c/lectures/Glect-html/topic4c708FSC.htm#tamura\"> Tamura's Texture Features</a> <h2>Parameters</h2> <ul><li><strong>Histogram Size (Directionality):</strong> The size of the histogram used by the directionality feature.</li></ul>")
public class Tamura2DFeatureSet<T extends RealType<T>, O extends RealType<O>>
		extends AbstractOpRefFeatureSet<RandomAccessibleInterval<T>, O> implements Contingent {

	private static final String PKG = "net.imagej.ops.Ops$Tamura$";

	@Parameter(type = ItemIO.INPUT, label = "Histogram Size (Directionality)", description = "The size of the histogram used by the directionality feature.", min = "1", max = "2147483647", stepSize = "1")
	private int histogramSize = 16;

	@Parameter(required = false, label = "Coarseness", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Coarseness") })
	private boolean isCoarsenessActive = true;

	@Parameter(required = false, label = "Contrast", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Contrast") })
	private boolean isContrastActive = true;

	@Parameter(required = false, label = "Directionality", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_PARAMS, value = "histogramSize"),
			@Attr(name = ATTR_TYPE, value = PKG + "Directionality") })
	private boolean isDirectionalityActive = true;

	public int getHistogramSize() {
		return histogramSize;
	}

	public void setHistogramSize(int histogramSize) {
		this.histogramSize = histogramSize;
	}

	@Override
	public boolean conforms() {
		return in().numDimensions() == 2;
	}

	@Override
	public boolean isCompatible(final Class<?> object, final Class<?> type) {
		return RandomAccessibleInterval.class.isAssignableFrom(object) && RealType.class.isAssignableFrom(type);
	}
}
