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

import net.imagej.ops.Ops.Geometric.Contour;
import net.imagej.ops.Ops.Geometric.Size;
import net.imagej.ops.geom.geom2d.DefaultSizePolygon;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imglib2.roi.geometric.Polygon;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

import org.knime.knip.core.KNIPGateway;
import org.knime.knip.features.sets.optimizedfeatures.KNIPCachedOpEnvironment;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * {@link FeatureSet} to calculate Geometric2DFeatureSet
 * 
 * @author Christian Dietz, University of Konstanz
 * @param <I>
 * @param <O>
 */
@Plugin(type = FeatureSet.class, label = "Geometric Features 2D", description = "<h1> Geometric 2D Feature Set</h1> <h2>Description</h2> Calculates different shape descriptors on the given input. Some examples include <ul><li><strong>Perimeter:</strong> The length of the outside boundary of the input.</li><li><strong>Size:</strong> The area of the input.</li><li><strong>Circularity:</strong> The circularity of the input.</li> <li><strong>...</strong></li></ul> For some more examples and descriptions see the <a href=\"http://rsbweb.nih.gov/ij/docs/guide/146-30.html#sub:Set-Measurements...\"> ImageJ User Guide </a>")
public class Geometric2DFeatureSet<L, O extends RealType<O>> extends AbstractOpRefFeatureSet<LabelRegion<L>, O> {

	private static final String PKG = "net.imagej.ops.Ops$Geometric$";

	@Parameter(required = false, label = "Size", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Size") })
	private boolean isSizeActive = true;

	@Parameter(required = false, label = "Circularity", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Circularity") })
	private boolean isCircularityActive = true;

	@Parameter(required = false, label = "Convexity", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Convexity") })
	private boolean isConvexityActive = true;

	@Parameter(required = false, label = "Eccentricity", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Eccentricity") })
	private boolean isEccentricityActive = true;

	@Parameter(required = false, label = "Main Elongation", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "MainElongation") })
	private boolean isMainElongationActive = true;

	@Parameter(required = false, label = "Ferets Angle", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "FeretsAngle") })
	private boolean isFeretsAngleActive = true;

	@Parameter(required = false, label = "Ferets Diameter", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "FeretsDiameter") })
	private boolean isFeretsDiameterActive = true;

	@Parameter(required = false, label = "Major Axis", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "MajorAxis") })
	private boolean isMajorAxisActive = true;

	@Parameter(required = false, label = "Minor Axis", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "MinorAxis") })
	private boolean isMinorAxisActive = true;

	@Parameter(required = false, label = "Boundary Size", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "BoundarySize") })
	private boolean isBoundarySizeActive = true;

	@Parameter(required = false, label = "Boxivity", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Boxivity") })
	private boolean isBoxivityActive = true;

	@Parameter(required = false, label = "Roundness", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Roundness") })
	private boolean isRoundnessActive = true;

	@Parameter(required = false, label = "Solidity", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Solidity") })
	private boolean isSolidityActive = true;

	private UnaryFunctionOp<LabelRegion<L>, Polygon> converter;

	private UnaryFunctionOp<Polygon, DoubleType> polygonSize;

	@SuppressWarnings("unchecked")
	public Geometric2DFeatureSet() {
		prioritizedOps = new Class[] { DefaultSizePolygon.class };
	}

	@Override
	public void initialize() {
		super.initialize();
		converter = Functions.unary(ops(), Contour.class, Polygon.class, in(), true);
		polygonSize = Functions.unary(ops(), Size.class, DoubleType.class, Polygon.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected O evalFunction(final UnaryFunctionOp<Object, ? extends O> func, LabelRegion<L> input) {

		if (input.size() <= 4) {
			KNIPGateway.log()
					.warn("LabelRegion was too small to calculate geometric features. Must be bigger than 4 pixels");
			return (O) new DoubleType(Double.NaN);
		}

		// FIXME: REMOVE THIS HACK ASAP. We have to fix converter matching in
		// ops
		Class<?> funcClass = ((KNIPCachedOpEnvironment.CachedFunctionOp<?, ?>) func).getDelegateType();
		if (Size.class.isAssignableFrom(funcClass)) {
			return (O) polygonSize.calculate(converter.calculate(input));
		} else {
			return func.calculate(converter.calculate(input));
		}
	}

	@Override
	public boolean conforms() {
		return in().numDimensions() == 2;
	}

	@Override
	public boolean isCompatible(final Class<?> object, final Class<?> type) {
		return LabelRegion.class.isAssignableFrom(object);
	}

}
