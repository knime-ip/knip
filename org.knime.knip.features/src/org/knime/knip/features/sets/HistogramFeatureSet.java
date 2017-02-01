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

import net.imagej.ops.image.histogram.HistogramCreate;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imglib2.IterableInterval;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.LongType;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * {@link FeatureSet} representing each bin of a histogram as a feature
 * 
 * @author Christian Dietz (University of Konstanz)
 * @param <T>
 */
@Plugin(type = FeatureSet.class, label = "Histogram Features", description = "<h1> Histogram Feature Set</h1> <h2> Description</h2> Distributes the pixels into bins depending on their pixel values and counts the numbers of pixels per bin.<h2>Parameters</h2> <ul><li><strong>Number of Bins:</strong> The number of bins of the histogram</li></ul>")
public class HistogramFeatureSet<T extends RealType<T>> extends AbstractIteratingFeatureSet<Iterable<T>, LongType>
		implements FeatureSet<Iterable<T>, LongType> {

	@Parameter(type = ItemIO.INPUT, label = "Number of Bins", description = "The number of bins of the histogram", min = "1", max = "2147483647", stepSize = "1")
	private int numBins = 256;

	@SuppressWarnings("rawtypes")
	private UnaryFunctionOp<Iterable<T>, Histogram1d> histogramFunc;

	private Histogram1d<T> histogram;

	@Override
	public void initialize() {
		super.initialize();
		histogramFunc = Functions.unary(ops(), HistogramCreate.class, Histogram1d.class, in(), numBins);

	}

	@SuppressWarnings("unchecked")
	@Override
	protected void preCompute(final Iterable<T> input) {
		histogram = histogramFunc.calculate(input);
	}

	@Override
	protected String getNamePrefix() {
		return "Histogram Bin:";
	}

	@Override
	protected LongType getResultAtIndex(int i) {
		return new LongType(histogram.frequency(i));
	}

	@Override
	protected int getNumEntries() {
		return numBins;
	}

	@Override
	public boolean isCompatible(final Class<?> object, final Class<?> type) {
		return IterableInterval.class.isAssignableFrom(object) && RealType.class.isAssignableFrom(type);
	}

	@Override
	public boolean conforms() {
		return true;
	}

}
