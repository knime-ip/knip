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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.features.zernike.helper.ZernikeComputer;
import net.imagej.ops.features.zernike.helper.ZernikeMoment;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

/**
 * {@link FeatureSet} to calculate {@link StatOp}s.
 * 
 * @author Daniel Seebacher, University of Konstanz
 * @author Tim-Oliver Buchholz, University of Konstanz
 * @author Christian Dietz, University of Konstanz
 * @param <T>
 * @param <O>
 */
@Plugin(type = FeatureSet.class, label = "Zernike Features", description = "<h1> Zernike Features </h1><h2> Description </h2> The Zernike polynomials were first proposed in 1934 by Zernike <a href=\"http://homepages.inf.ed.ac.uk/rbf/CVonline/LOCAL_COPIES/SHUTLER3/node17.html#Zernike34\">[22]</a>. Their moment formulation appears to be one of the most popular, outperforming the alternatives <a href=\"http://homepages.inf.ed.ac.uk/rbf/CVonline/LOCAL_COPIES/SHUTLER3/node17.html#Teh88\">[19]</a> (in terms of noise resilience, information redundancy and reconstruction capability). </br></br> For more information see <a href=\"http://homepages.inf.ed.ac.uk/rbf/CVonline/LOCAL_COPIES/SHUTLER3/node11.html\"> Complex Zernike Moments </a> <h2> Parameters </h2> <ul>	<li><strong>Minimum Order of Zernike Moment:</strong> The minimum order of the Zernike moments to be calculated. </li> 	<li><strong>Maximum Order of Zernike Moment:</strong>The maximum order of the Zernike moments to be calculated. </li></ul>")
public class ZernikeFeatureSet<T extends RealType<T>>
		extends AbstractCachedFeatureSet<IterableInterval<T>, DoubleType> {

	@Parameter(type = ItemIO.INPUT, label = "Minimum Order of Zernike Moment", description = "The minimum order of the zernike moment to be calculated.", min = "1", max = "2147483647", stepSize = "1")
	private int orderMin = 2;

	@Parameter(type = ItemIO.INPUT, label = "Maximum Order of Zernike Moment", description = "The maximum order of the zernike moment to be calculated.", min = "1", max = "2147483647", stepSize = "1")
	private int orderMax = 4;

	private ZernikeComputer<T> zernikeComputer;

	@Override
	public void initialize() {
		super.initialize();

		zernikeComputer = new ZernikeComputer<T>();
	}

	@Override
	public List<NamedFeature> getFeatures() {
		final List<NamedFeature> features = new ArrayList<NamedFeature>();

		for (int order = orderMin; order <= orderMax; order++) {
			for (int repetition = 0; repetition <= order; repetition++) {
				if (Math.abs(order - repetition) % 2 == 0) {
					features.add(new NamedFeature("Magnitude for Order " + order + " and Repetition " + repetition));
					features.add(new NamedFeature("Phase for Order " + order + " and Repetition " + repetition));
				}
			}
		}

		return features;
	}

	@Override
	public Map<NamedFeature, DoubleType> compute(IterableInterval<T> input) {
		HashMap<NamedFeature, DoubleType> map = new HashMap<NamedFeature, DoubleType>();

		for (int order = orderMin; order <= orderMax; order++) {
			for (int repetition = 0; repetition <= order; repetition++) {
				if (Math.abs(order - repetition) % 2 == 0) {
					zernikeComputer.setOrder(order);
					zernikeComputer.setRepetition(repetition);

					ZernikeMoment results = zernikeComputer.compute(input);

					map.put(new NamedFeature("Magnitude for Order " + order + " and Repetition " + repetition),
							new DoubleType(results.getMagnitude()));
					map.put(new NamedFeature("Phase for Order " + order + " and Repetition " + repetition),
							new DoubleType(results.getPhase()));
				}
			}
		}

		return map;
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
