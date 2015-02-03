package org.knime.knip.featurenode.model;

import java.io.Serializable;
import java.util.HashMap;
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
	 * serialVersionUID.
	 */
	private static final long serialVersionUID = -6878209581144227328L;

	private final Class<? extends FeatureSet> featureSet;
	private final Map<String, Object> fieldNamesAndValues;
	private final Map<Class<?>, Boolean> selectedFeatures;

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
		this.featureSet = featureSet;
		this.fieldNamesAndValues = (fieldNamesAndValues != null) ? fieldNamesAndValues
				: new HashMap<String, Object>();
		this.selectedFeatures = (selectedFeatures != null) ? selectedFeatures
				: new HashMap<Class<?>, Boolean>();
	}

	public Class<? extends FeatureSet> getFeatureSetClass() {
		return this.featureSet;
	}

	public Map<String, Object> getFieldNamesAndValues() {
		return this.fieldNamesAndValues;
	}

	public Map<Class<?>, Boolean> getSelectedFeatures() {
		return this.selectedFeatures;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result)
				+ ((this.featureSet == null) ? 0 : this.featureSet.hashCode());
		result = (prime * result)
				+ ((this.fieldNamesAndValues == null) ? 0
						: this.fieldNamesAndValues.hashCode());
		result = (prime * result)
				+ ((this.selectedFeatures == null) ? 0 : this.selectedFeatures
						.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final FeatureSetInfo other = (FeatureSetInfo) obj;
		if (this.featureSet == null) {
			if (other.featureSet != null) {
				return false;
			}
		} else if (!this.featureSet.equals(other.featureSet)) {
			return false;
		}
		if (this.fieldNamesAndValues == null) {
			if (other.fieldNamesAndValues != null) {
				return false;
			}
		} else if (!this.fieldNamesAndValues.equals(other.fieldNamesAndValues)) {
			return false;
		}
		if (this.selectedFeatures == null) {
			if (other.selectedFeatures != null) {
				return false;
			}
		} else if (!this.selectedFeatures.equals(other.selectedFeatures)) {
			return false;
		}
		return true;
	}

}
