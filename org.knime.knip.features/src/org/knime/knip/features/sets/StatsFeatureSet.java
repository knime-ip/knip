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

import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;

import org.scijava.plugin.Attr;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * {@link FeatureSet} to calculate first order statistic features
 * 
 * @author Christian Dietz, University of Konstanz
 * @param <I>
 * @param <O>
 */
@Plugin(type = FeatureSet.class, label = "Statistic Features", description = "<h1> Statistic Features </h1> Feature Set of statistical measurements of the image. Some examples include: <ul> <li> <strong>Minimum</strong>: The minimum pixel value of the image</li> <li> <strong>Maximum</strong>: The maximum pixel value of the image</li> <li> <strong>Variance</strong>: The variance of the  pixel values of the image</li> <li> ... </li> </ul> For more examples and information see the <a href=\"http://rsbweb.nih.gov/ij/docs/guide/146-30.html#sub:Set-Measurements...\"> ImageJ User Guide </a>")
public class StatsFeatureSet<T extends RealType<T>, O extends RealType<O>>
		extends AbstractOpRefFeatureSet<Iterable<T>, O> {

	private static final String PKG = "net.imagej.ops.Ops$Stats$";

	@Parameter(required = false, label = "Minimum", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Min") })
	private boolean isMinActive = true;

	@Parameter(required = false, label = "Maximum", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Max") })
	private boolean isMaxActive = true;

	@Parameter(required = false, label = "Mean", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Mean") })
	private boolean isMeanActive = true;

	@Parameter(required = false, label = "Sum", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Sum") })
	private boolean isSumActive = true;

	@Parameter(required = false, label = "Skewness", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Skewness") })
	private boolean isSkewnessActive = true;

	@Parameter(required = false, label = "Median", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Median") })
	private boolean isMedianActive = true;

	@Parameter(required = false, label = "Kurtosis", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Kurtosis") })
	private boolean isKurtosisActive = true;

	@Parameter(required = false, label = "Standard Deviation", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "StdDev") })
	private boolean isStdDevActive = true;

	@Parameter(required = false, label = "Variance", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Variance") })
	private boolean isVarianceActive = true;

	@Parameter(required = false, label = "Sum Of Logs", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "SumOfLogs") })
	private boolean isSumOfLogsActive = true;

	@Parameter(required = false, label = "Sum Of Squares", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "SumOfSquares") })
	private boolean isSumOfSquaresActive = true;

	@Parameter(required = false, label = "Sum Of Inverses", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "SumOfInverses") })
	private boolean isSumOfInversesActive = true;

	@Parameter(required = false, label = "Moment 1 About Mean", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Moment1AboutMean") })
	private boolean isMoment1AboutMeanActive = true;

	@Parameter(required = false, label = "Moment 2 About Mean", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Moment2AboutMean") })
	private boolean isMoment2AboutMeanActive = true;

	@Parameter(required = false, label = "Moment 3 About Mean", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Moment3AboutMean") })
	private boolean isMoment3AboutMeanActive = true;

	@Parameter(required = false, label = "Moment 4 About Mean", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Moment4AboutMean") })
	private boolean isMoment4AboutMeanActive = true;

	@Parameter(required = false, label = "Harmonic Mean", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "HarmonicMean") })
	private boolean isHarmonicMeanActive = true;

	@Parameter(required = false, label = "Geometric Mean", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "GeometricMean") })
	private boolean isGeometricMeanActive = true;

	public StatsFeatureSet() {
		// NB: Empty Constructor
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
