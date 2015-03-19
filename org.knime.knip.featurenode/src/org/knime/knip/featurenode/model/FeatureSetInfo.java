package org.knime.knip.featurenode.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imagej.ops.features.FeatureSet;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

import org.scijava.module.DefaultMutableModuleInfo;

/**
 * Simple wrapper for a {@link FeatureSet}.
 *
 * @author Daniel Seebacher, University of Konstanz.
 */
@SuppressWarnings("rawtypes")
public class FeatureSetInfo extends DefaultMutableModuleInfo implements
		Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 5538059260299069153L;

	private final Class<? extends FeatureSet> featureSetClass;
	private final String[] parameterNames;
	private final Object[] parameterValues;
	private final Class<?>[] featureClasses;
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
					selectedFeatures.keySet())
					.toArray(new Class<?>[selectedFeatures.keySet().size()]);

			Arrays.sort(this.featureClasses, new Comparator<Class<?>>() {
				@Override
				public int compare(final Class<?> o1, final Class<?> o2) {
					return o1.getCanonicalName().compareTo(
							o2.getCanonicalName());
				}
			});

			this.isFeatureSelected = new boolean[this.featureClasses.length];
			for (int i = 0; i < this.featureClasses.length; i++) {
				this.isFeatureSelected[i] = selectedFeatures
						.get(this.featureClasses[i]);
			}
		} else {
			this.featureClasses = new Class<?>[0];
			this.isFeatureSelected = new boolean[0];
		}
	}

	public Class<? extends FeatureSet> getFeatureSetClass() {
		return this.featureSetClass;
	}

	public Map<String, Object> getFieldNameAndValues() {
		final Map<String, Object> fieldNamesAndValues = new HashMap<String, Object>();
		for (int i = 0; i < this.parameterNames.length; i++) {
			fieldNamesAndValues.put(this.parameterNames[i],
					this.parameterValues[i]);
		}

		return fieldNamesAndValues;
	}

	public List<Pair<String, Object>> getSortedFieldNameAndValues() {

		List<Pair<String, Object>> fieldNameAndValues = new ArrayList<Pair<String, Object>>();
		for (int i = 0; i < parameterNames.length; i++) {
			fieldNameAndValues.add(new ValuePair<String, Object>(
					parameterNames[i], parameterValues[i]));
		}

		return fieldNameAndValues;
	}

	public Map<Class<?>, Boolean> getSelectedFeatures() {
		final Map<Class<?>, Boolean> selectedFeatures = new HashMap<Class<?>, Boolean>();
		for (int i = 0; i < this.featureClasses.length; i++) {
			selectedFeatures.put(this.featureClasses[i],
					this.isFeatureSelected[i]);
		}

		return selectedFeatures;
	}

	public List<Pair<Class<?>, Boolean>> getSortedSelectedFeatures() {
		final List<Pair<Class<?>, Boolean>> selectedFeatures = new ArrayList<Pair<Class<?>, Boolean>>();

		for (int i = 0; i < this.featureClasses.length; i++) {
			selectedFeatures.add(new ValuePair<Class<?>, Boolean>(
					this.featureClasses[i], this.isFeatureSelected[i]));
		}

		return selectedFeatures;
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
		result = (prime * result) + Arrays.hashCode(this.featureClasses);
		result = (prime * result)
				+ ((this.featureSetClass == null) ? 0 : this.featureSetClass
						.hashCode());
		result = (prime * result) + Arrays.hashCode(this.isFeatureSelected);
		result = (prime * result) + Arrays.hashCode(this.parameterNames);
		result = (prime * result) + Arrays.hashCode(this.parameterValues);
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
		if (!Arrays.equals(this.featureClasses, other.featureClasses)) {
			return false;
		}
		if (this.featureSetClass == null) {
			if (other.featureSetClass != null) {
				return false;
			}
		} else if (!this.featureSetClass.equals(other.featureSetClass)) {
			return false;
		}
		if (!Arrays.equals(this.isFeatureSelected, other.isFeatureSelected)) {
			return false;
		}
		if (!Arrays.equals(this.parameterNames, other.parameterNames)) {
			return false;
		}
		if (!Arrays.equals(this.parameterValues, other.parameterValues)) {
			return false;
		}
		return true;
	}

}
