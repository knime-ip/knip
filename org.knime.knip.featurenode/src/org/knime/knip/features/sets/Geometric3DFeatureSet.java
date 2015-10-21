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

import org.knime.knip.features.sets.AbstractCachedFeatureSet.KNIPCachedOpEnvironment.CachedFunctionOp;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.FunctionOp;
import net.imagej.ops.OpService;
import net.imagej.ops.Ops.Geometric.MainElongation;
import net.imagej.ops.Ops.Geometric.MarchingCubes;
import net.imagej.ops.Ops.Geometric.MedianElongation;
import net.imagej.ops.Ops.Geometric.Size;
import net.imagej.ops.Ops.Geometric.Spareness;
import net.imagej.ops.geom.geom3d.mesh.Mesh;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.type.numeric.RealType;

/**
 * {@link FeatureSet} to calculate 3D Geometric Features
 *
 * @author Christian Dietz, University of Konstanz
 * @param <I>
 * @param <O>
 */
@Plugin(type = FeatureSet.class, label = "Geometric Features 3D", description = "<h1> Geometric 3D Feature Set</h1> <h2>Description</h2> Calculates different shape descriptors on the given input. Some examples include <ul><li><strong>Perimeter:</strong> The length of the outside boundary of the input.</li><li><strong>Size:</strong> The volume of the input.</li><li><strong>Circularity:</strong> The circularity of the input.</li> <li><strong>...</strong></li></ul> For some more examples and descriptions see the <a href=\"http://rsbweb.nih.gov/ij/docs/guide/146-30.html#sub:Set-Measurements...\"> ImageJ User Guide </a>")
public class Geometric3DFeatureSet<L, O extends RealType<O>> extends AbstractOpRefFeatureSet<LabelRegion<L>, O> {

	private static final String PKG = "net.imagej.ops.Ops$Geometric$";

	@Parameter
	private OpService ops;

	@Parameter(required = false, label = "Volume (in pixel units)", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Size") })
	private boolean isSizeActive = true;

	@Parameter(required = false, label = "Volume of Convex Hull", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "SizeConvexHull") })
	private boolean isSizeConvexHullActive = true;

	@Parameter(required = false, label = "Surface Area of Convex Hull", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "BoundarySizeConvexHull") })
	private boolean isBoundarySizeConvexHullActive = true;

	@Parameter(required = false, label = "Surface Area", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "BoundarySize") })
	private boolean isBoundarySizeActive = true;

	@Parameter(required = false, label = "Surface Area (in pixel units)", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "BoundaryPixelCount") })
	private boolean isBoundaryPixelCountActive = true;

	@Parameter(required = false, label = "Surface Area of Convex Hull (in pixel units)", attrs = {
			@Attr(name = ATTR_FEATURE), @Attr(name = ATTR_TYPE, value = PKG + "BoundaryPixelCountConvexHull") })
	private boolean isBoundaryPixelCountConvexHullMeshActive = true;

	@Parameter(required = false, label = "Compactness", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Compactness") })
	private boolean isCompactnessActive = true;

	@Parameter(required = false, label = "Convexity", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Convexity") })
	private boolean isConvexityActive = true;

	@Parameter(required = false, label = "Rugosity", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Rugosity") })
	private boolean isRugosityActive = true;

	@Parameter(required = false, label = "Solidity", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Solidity") })
	private boolean isSolidityActive = true;

	@Parameter(required = false, label = "Sphericity", attrs = { @Attr(name = ATTR_FEATURE),
			@Attr(name = ATTR_TYPE, value = PKG + "Sphericity") })
	private boolean isSphericityActive = true;

	private FunctionOp<LabelRegion<L>, Mesh> converter;

	public Geometric3DFeatureSet() {
		// NB: Empty Constructor
	}

	@Override
	public void initialize() {
		super.initialize();

		converter = ops.function(MarchingCubes.class, Mesh.class, in());
	}

	/**
	 * Can be overriden by implementors to provide specialized implementations
	 * for certain functions.
	 * 
	 * @param func
	 *            function used to compute output. Will be any function added as
	 *            OpRef.
	 * @param input
	 *            input object
	 * @return
	 */
	protected O evalFunction(final FunctionOp<Object, ? extends O> func, final LabelRegion<L> input) {

		Class<?> funcClass = ((CachedFunctionOp<?, ?>) func).getDelegateType();
		// these ops can be directly computed on label region
		if (MainElongation.class.isAssignableFrom(funcClass) || MedianElongation.class.isAssignableFrom(funcClass)
				|| Spareness.class.isAssignableFrom(funcClass) || Size.class.isAssignableFrom(funcClass)) {
			return func.compute(input);
		}

		return func.compute(converter.compute(input));
	}

	@Override
	public boolean conforms() {
		return in().numDimensions() == 3;
	}

	@Override
	public boolean isCompatible(final Class<?> object, final Class<?> type) {
		return LabelRegion.class.isAssignableFrom(object);
	}

}
