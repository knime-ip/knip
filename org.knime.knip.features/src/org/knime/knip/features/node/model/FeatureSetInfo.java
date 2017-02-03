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
package org.knime.knip.features.node.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

import org.knime.knip.core.KNIPGateway;
import org.knime.knip.features.sets.FeatureSet;
import org.scijava.command.CommandInfo;
import org.scijava.module.Module;
import org.scijava.module.process.InitPreprocessor;
import org.scijava.module.process.ValidityPreprocessor;

/**
 * Simple wrapper for a {@link FeatureSet}.
 *
 * @author Daniel Seebacher, University of Konstanz.
 * @author Tim-Oliver Buchholz, University of Konstanz.
 */
@SuppressWarnings("rawtypes")
public class FeatureSetInfo {

	private final Class<? extends FeatureSet> featureSetClass;
	private final String[] parameterNames;
	private final Object[] parameterValues;
	private Module module;

	/**
	 * Default constructor.
	 *
	 * @param featureSet
	 *            The Class of a {@link FeatureSet}
	 * @param fieldNamesAndValues
	 *            A Map containing the Field Names are their Values
	 */
	public FeatureSetInfo(final Class<? extends FeatureSet> featureSet, final Map<String, Object> fieldNamesAndValues) {

		this.featureSetClass = featureSet;

		// parameter names, must be sorted
		if ((fieldNamesAndValues != null) && !fieldNamesAndValues.isEmpty()) {
			this.parameterNames = fieldNamesAndValues.keySet().toArray(new String[fieldNamesAndValues.size()]);
			Arrays.sort(this.parameterNames);

			// parameter values
			this.parameterValues = new Object[fieldNamesAndValues.size()];
			for (int i = 0; i < this.parameterValues.length; i++) {
				this.parameterValues[i] = fieldNamesAndValues.get(this.parameterNames[i]);
			}
		} else {
			this.parameterNames = new String[0];
			this.parameterValues = new Object[0];
		}
	}

	public Map<String, Object> getFieldNameAndValues() {
		final Map<String, Object> fieldNamesAndValues = new HashMap<String, Object>();
		for (int i = 0; i < this.parameterNames.length; i++) {
			fieldNamesAndValues.put(this.parameterNames[i], this.parameterValues[i]);
		}

		return fieldNamesAndValues;
	}

	public List<String> getFieldNames() {
		return Arrays.asList(parameterNames);
	}

	public Module load() {
		if (module == null) {
			module = KNIPGateway.ms().createModule(new CommandInfo(featureSetClass));
			module.setInputs(getFieldNameAndValues());
			validateAndInitialize(module);
		}
		return module;
	}

	public String getName() {
		return featureSetClass.getSimpleName();
	}

	private void validateAndInitialize(final Module module) {

		module.setResolved("ps", true);
		module.setResolved("cs", true);
		// resolve default input and output
		module.setResolved("in", true);
		module.setResolved("out", true);
		module.setResolved("outType", true);
		module.setResolved("prioritizedOps", true);

		// ensure the module is well-formed
		final ValidityPreprocessor validater = new ValidityPreprocessor();
		validater.process(module);
		if (validater.isCanceled()) {
			final String cancelReason = validater.getCancelReason();
			throw new IllegalArgumentException("Couldn't validate given module. " + cancelReason);
		}

		// run the module initializers
		new InitPreprocessor().process(module);
	}

	List<Pair<String, Object>> getSortedFieldNameAndValues() {

		List<Pair<String, Object>> fieldNameAndValues = new ArrayList<Pair<String, Object>>();
		for (int i = 0; i < parameterNames.length; i++) {
			fieldNameAndValues.add(new ValuePair<String, Object>(parameterNames[i], parameterValues[i]));
		}

		return fieldNameAndValues;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((featureSetClass == null) ? 0 : featureSetClass.hashCode());
		result = (prime * result) + Arrays.hashCode(parameterNames);
		result = (prime * result) + Arrays.hashCode(parameterValues);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof FeatureSetInfo)) {
			return false;
		}
		final FeatureSetInfo other = (FeatureSetInfo) obj;
		// if (!Arrays.equals(this.featureClasses, other.featureClasses)) {
		// return false;
		// }
		if (this.featureSetClass == null) {
			if (other.featureSetClass != null) {
				return false;
			}
		} else if (!this.featureSetClass.equals(other.featureSetClass)) {
			return false;
		}
		// if (!Arrays.equals(this.isFeatureSelected, other.isFeatureSelected))
		// {
		// return false;
		// }
		if (!Arrays.equals(this.parameterNames, other.parameterNames)) {
			return false;
		}
		if (!Arrays.equals(this.parameterValues, other.parameterValues)) {
			return false;
		}
		return true;
	}

	public Class<? extends FeatureSet> getFeatureSetClass() {
		return this.featureSetClass;
	}

}
