package org.knime.knip.featurenode.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imagej.ops.features.FeatureSet;

/**
 * Simple wrapper for a {@link FeatureSet}.
 *
 * @author Daniel Seebacher, University of Konstanz.
 */
@SuppressWarnings("rawtypes")
public class FeatureSetInfo implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 5538059260299069153L;

	private final Class<?> featureSetClass;
	private final String[] parameterNames;
	private final Object[] parameterValues;
	private final List<Class<?>> featureClasses;
	private final boolean[] isFeatureSelected;

	/**
	 * Default constructor.
	 *
	 * @param featureSet
	 *            The Class of a {@link FeatureSet}
	 * @param fieldNamesAndValues
	 *            A Map containing the Field Names are their Values
	 * @param selectedFeatures
	 *            A map containing the Features inside this {@link FeatureSet}
	 *            and whether they should be calculated or not
	 */
	public FeatureSetInfo(final Class<? extends FeatureSet> featureSet,
			final Map<String, Object> fieldNamesAndValues,
			final Map<Class<?>, Boolean> selectedFeatures) {

		this.featureSetClass = featureSet;

		// parameter names, must be sorted
		if ((fieldNamesAndValues != null) && !fieldNamesAndValues.isEmpty()) {
			this.parameterNames = fieldNamesAndValues.keySet().toArray(
					new String[fieldNamesAndValues.size()]);
			Arrays.sort(this.parameterNames);

			// parameter values
			this.parameterValues = new Object[fieldNamesAndValues.size()];
			for (int i = 0; i < this.parameterValues.length; i++) {
				this.parameterValues[i] = fieldNamesAndValues
						.get(this.parameterNames[i]);
			}
		} else {
			this.parameterNames = new String[0];
			this.parameterValues = new Object[0];
		}

		if ((selectedFeatures != null) && !selectedFeatures.isEmpty()) {
			this.featureClasses = new ArrayList<Class<?>>(
					selectedFeatures.keySet());
			Collections.sort(this.featureClasses, new Comparator<Class<?>>() {
				@Override
				public int compare(final Class<?> o1, final Class<?> o2) {
					return o1.getCanonicalName().compareTo(
							o2.getCanonicalName());
				}
			});

			this.isFeatureSelected = new boolean[this.featureClasses.size()];
			for (int i = 0; i < this.featureClasses.size(); i++) {
				this.isFeatureSelected[i] = selectedFeatures
						.get(this.featureClasses.get(i));
			}
		} else {
			this.featureClasses = new ArrayList<Class<?>>();
			this.isFeatureSelected = new boolean[0];
		}
	}

	@SuppressWarnings("unchecked")
	public Class<? extends FeatureSet> getFeatureSetClass() {
		return (Class<? extends FeatureSet>) this.featureSetClass;
	}

	public Map<String, Object> getFieldNameAndValues() {
		final Map<String, Object> fieldNamesAndValues = new HashMap<String, Object>();
		for (int i = 0; i < this.parameterNames.length; i++) {
			fieldNamesAndValues.put(this.parameterNames[i],
					this.parameterValues[i]);
		}

		return fieldNamesAndValues;
	}

	public Map<Class<?>, Boolean> getSelectedFeatures() {
		final Map<Class<?>, Boolean> selectedFeatures = new HashMap<Class<?>, Boolean>();
		for (int i = 0; i < this.featureClasses.size(); i++) {
			selectedFeatures.put(this.featureClasses.get(i),
					this.isFeatureSelected[i]);
		}

		return selectedFeatures;
	}
}
