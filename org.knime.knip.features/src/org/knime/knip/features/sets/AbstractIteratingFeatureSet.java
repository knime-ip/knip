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

import net.imglib2.type.numeric.RealType;

/**
 * {@link FeatureSet} to calculate features which are organized in a structure,
 * accessible via an index. Typical example is {@link HistogramFeatureSet}.
 * 
 * @author Christian Dietz (University of Konstanz)
 * @param <T>
 */
public abstract class AbstractIteratingFeatureSet<I, O extends RealType<O>> extends AbstractCachedFeatureSet<I, O>
		implements FeatureSet<I, O> {

	private List<NamedFeature> infos;

	@Override
	public void initialize() {
		super.initialize();
		if (infos == null)
			infos = getFeatures();
	}

	@Override
	public Map<NamedFeature, O> compute(final I input) {
		final Map<NamedFeature, O> res = new HashMap<NamedFeature, O>();

		preCompute(input);

		int i = 0;
		for (final NamedFeature info : infos) {
			res.put(info, getResultAtIndex(i));
			i++;
		}

		return res;
	}

	@Override
	public List<NamedFeature> getFeatures() {
		final ArrayList<NamedFeature> infos = new ArrayList<NamedFeature>();

		for (int i = 0; i < getNumEntries(); i++) {
			final int f = i;
			infos.add(new NamedFeature(getNamePrefix() + " " + f));
		}
		return infos;
	}

	/**
	 * Called once before getResultAtIndex is called subsequently for each entry
	 * in the {@link FeatureSet}.
	 * 
	 * @param input
	 *            the input
	 */
	protected abstract void preCompute(final I input);

	/**
	 * @param i
	 *            index
	 * @return result hat given index
	 */
	protected abstract O getResultAtIndex(int i);

	/**
	 * @return number of entries in feature-set
	 */
	protected abstract int getNumEntries();

	/**
	 * @return semantic label prefix for the entries of the feature-set
	 */
	protected abstract String getNamePrefix();
}
