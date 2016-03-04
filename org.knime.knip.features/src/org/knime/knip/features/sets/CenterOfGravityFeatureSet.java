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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.imglib2.IterableInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

import org.scijava.plugin.Plugin;

/**
 * {@link FeatureSet} to calculate {@link AbstractOpRefFeatureSet<I, O>}.
 * 
 * @author Tim-Oliver Buchholz, University of Konstanz
 * @param <I>
 * @param <O>
 */
@Plugin(type = FeatureSet.class, label = "Center of Gravity", description = "<h1> Center of Gravity Feature Set</h1> <h2>Description</h2> Calculates the center of gravity for each dimension of the given input.")
public class CenterOfGravityFeatureSet<T extends RealType<T>>
		extends AbstractCachedFeatureSet<IterableInterval<T>, DoubleType> implements RequireNumDimensions {

	private int numDims = -1;

	@Override
	public List<NamedFeature> getFeatures() {
		final List<NamedFeature> fs = new ArrayList<NamedFeature>();

		if (numDims == -1) {
			numDims = in().numDimensions();
		}

		for (int i = 0; i < numDims; i++) {
			fs.add(new NamedFeature("Center of Gravity in Dimension#" + i));
		}
		return fs;
	}

	@Override
	public Map<NamedFeature, DoubleType> compute1(final IterableInterval<T> input) {
		Map<NamedFeature, DoubleType> res = new LinkedHashMap<NamedFeature, DoubleType>();
		RealLocalizable centroid = ops().geom().centerOfGravity(input);

		for (int i = 0; i < getFeatures().size(); i++) {
			res.put(new NamedFeature("Center of Gravity in Dimension#" + i),
					new DoubleType(centroid.getDoublePosition(i)));
		}

		return res;
	}

	@Override
	public boolean isCompatible(final Class<?> object, final Class<?> type) {
		return IterableInterval.class.isAssignableFrom(object) && RealType.class.isAssignableFrom(type);
	}

	@Override
	public boolean conforms() {
		return true;
	}

	@Override
	public void setNumDimensions(int numDims) {
		this.numDims = numDims;
	}

}
