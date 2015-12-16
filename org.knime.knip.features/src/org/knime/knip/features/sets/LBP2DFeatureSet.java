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

import java.util.ArrayList;
import java.util.List;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.Ops.LBP.LBP2D;
import net.imagej.ops.special.Functions;
import net.imagej.ops.special.UnaryFunctionOp;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.LongType;

/**
 * {@link FeatureSet} representing each bin of a histogram as a feature
 * 
 * @author Christian Dietz (University of Konstanz)
 * @param <T>
 */
@Plugin(type = FeatureSet.class, label = "Local Binary Patterns 2D", description = "<h1> 2D Local Binary Pattern Feature Set</h1> <h2>Note: Runs only on single Images!</h2> <h2> Description</h2> Local binary patterns (LBP) is a type of feature used for classification in computer vision.. The LBP feature vector, in its simplest form, is created in the following manner: <ul> <li> For each pixel in a cell, compare the pixel to each of its 8 neighbors (on its left-top, left-middle, left-bottom, right-top, etc.). Follow the pixels along a circle, i.e. clockwise or counter-clockwise. </li> <li> Where the center pixel's value is greater than the neighbor's value, write \"1\". Otherwise, write \"0\". This gives an 8-digit binary number (which is usually converted to decimal for convenience).</li> <li> Compute the histogram, over the cell, of the frequency of each \"number\" occurring (i.e., each combination of which pixels are smaller and which are greater than the center).</li> </ul><h2>Parameters</h2> <ul><li><strong>Distance: </strong> Distance of the neighborhood pixels to the center pixel </li> <li><strong>Number of Bins: </strong> The number of bins of the histogram</li></ul> See <a href=\"https://en.wikipedia.org/wiki/Local_binary_patterns\"> Wikipedia</a> for more information.")
public class LBP2DFeatureSet<T extends RealType<T>>
		extends AbstractIteratingFeatureSet<RandomAccessibleInterval<T>, LongType>
		implements FeatureSet<RandomAccessibleInterval<T>, LongType>, RequireNumDimensions {

	@Parameter(required = true, label = "Distance", description = "Size of the neighborhood around each pixel", min = "1", max = "2147483647", stepSize = "1")
	private int distance = 1;

	@Parameter(type = ItemIO.INPUT, label = "Number of Bins", description = "The number of bins of the histogram", min = "1", max = "2147483647", stepSize = "1")
	private int numBins = 256;

	@SuppressWarnings("rawtypes")
	private UnaryFunctionOp<RandomAccessibleInterval<T>, ArrayList> histogramFunc;

	private List<LongType> histogram;

	private int numDims;

	@Override
	public void initialize() {
		super.initialize();
		histogramFunc = Functions.unary(ops(), LBP2D.class, ArrayList.class, in(), distance, numBins);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void preCompute(final RandomAccessibleInterval<T> input) {
		histogram = histogramFunc.compute1(input);
	}

	@Override
	protected String getNamePrefix() {
		return "LBP 2D Bin:";
	}

	@Override
	protected LongType getResultAtIndex(int i) {
		return histogram.get(i);
	}

	@Override
	protected int getNumEntries() {
		return numBins;
	}

	@Override
	public boolean isCompatible(final Class<?> object, final Class<?> type) {
		return RandomAccessibleInterval.class.isAssignableFrom(object) && RealType.class.isAssignableFrom(type);
	}

	@Override
	public boolean conforms() {
		return numDims < 0 ? in().numDimensions() == 2 : numDims == 2;
	}

	@Override
	public void setNumDimensions(int numDims) {
		this.numDims = numDims;
	}

}
