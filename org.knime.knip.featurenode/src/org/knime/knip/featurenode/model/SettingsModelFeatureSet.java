package org.knime.knip.featurenode.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;

import net.imagej.ops.features.FeatureSet;
import net.imglib2.util.Pair;

public class SettingsModelFeatureSet extends SettingsModel {

	/**
	 * CONSTANTS
	 */
	private static final String NUM_FEATURE_SETS = "NUM_FEATURE_SETS";
	private static final String FEATURE_SET = "FEATURE_SET_";
	private static final String FEATURE_SET_CLASSNAME = "FEATURE_SET_CLASSNAME";

	private static final String NUM_FIELDS = "NUM_FIELDS";
	private static final String FIELD_NAME = "FIELD_NAME_";
	private static final String FIELD_VALUE = "FIELD_VALUE_";

	private static final String NUM_FEATURES = "NUM_FEATURES";
	private static final String FEATURE_CLASS_NAME = "FEATURE_CLASS_NAME_";
	private static final String FEATURE_CLASS_IS_SELECTED = "FEATURE_SELECTED_";

	private final String m_configName;
	private List<FeatureSetInfo> m_featureSets;

	/**
	 * Creates a new object holding an integer value.
	 *
	 * @param configName
	 *            the identifier the value is stored with in the
	 *            {@link org.knime.core.node.NodeSettings} object
	 * @param featureSets
	 *            the initial value
	 */
	public SettingsModelFeatureSet(final String configName, final Collection<FeatureSetInfo> featureSets) {
		if ((configName == null) || "".equals(configName)) {
			throw new IllegalArgumentException("The configName must be a " + "non-empty string");
		}

		this.m_configName = configName;
		if (featureSets == null) {
			this.m_featureSets = new ArrayList<FeatureSetInfo>();
		} else {
			this.m_featureSets = new ArrayList<FeatureSetInfo>(featureSets);
		}
	}

	/**
	 * Creates a new object holding an integer value.
	 *
	 * @param configName
	 *            the identifier the value is stored with in the
	 *            {@link org.knime.core.node.NodeSettings} object
	 */
	public SettingsModelFeatureSet(final String configName) {
		if ((configName == null) || "".equals(configName)) {
			throw new IllegalArgumentException("The configName must be a " + "non-empty string");
		}
		this.m_featureSets = new ArrayList<FeatureSetInfo>();
		this.m_configName = configName;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <T extends SettingsModel> T createClone() {
		return (T) new SettingsModelFeatureSet(this.m_configName, this.m_featureSets);
	}

	@Override
	public String getModelTypeID() {
		return "SMID_featureset";
	}

	@Override
	public String getConfigName() {
		return this.m_configName;
	}

	@Override
	public void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
			throws NotConfigurableException {

		// load number of feature sets
		try {
			loadSettingsForModel(settings);
		} catch (final InvalidSettingsException e) {
			throw new NotConfigurableException("Couldn't load settings", e);
		}

	}

	@Override
	public void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
		saveSettingsForModel(settings);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
		final List<FeatureSetInfo> featureSets = new ArrayList<FeatureSetInfo>();

		// load number of feature sets
		final int numFeatureSets = settings.getInt(NUM_FEATURE_SETS);

		// load each feature set
		for (int i = 0; i < numFeatureSets; i++) {

			final NodeSettingsRO featureSetSettings = settings.getNodeSettings(FEATURE_SET + i);

			// load class
			Class<? extends FeatureSet> featureSetClass;
			try {
				featureSetClass = (Class<? extends FeatureSet>) Class
						.forName(featureSetSettings.getString(FEATURE_SET_CLASSNAME));
			} catch (final ClassNotFoundException e) {
				throw new InvalidSettingsException("Couldn't load feature set class", e);
			}

			// load number of fields
			final int numFields = featureSetSettings.getInt(NUM_FIELDS);

			// load each field
			final Map<String, Object> fieldNamesAndValues = new HashMap<String, Object>();
			for (int j = 0; j < numFields; j++) {
				final String fieldName = featureSetSettings.getString(FIELD_NAME + j);

				Object fieldValue;
				try {
					fieldValue = loadObject(featureSetSettings, FIELD_VALUE + j,
							featureSetClass.getDeclaredField(fieldName).getType());
				} catch (NoSuchFieldException | SecurityException e) {
					throw new InvalidSettingsException("Couldn't load field value", e);
				}

				fieldNamesAndValues.put(fieldName, fieldValue);
			}

			// load number of features
			final int numFeatures = featureSetSettings.getInt(NUM_FEATURES);

			final Map<Class<?>, Boolean> selectedFeatures = new HashMap<Class<?>, Boolean>();
			// load each feature
			for (int j = 0; j < numFeatures; j++) {

				Class<?> featureClass;
				try {
					featureClass = Class.forName(featureSetSettings.getString(FEATURE_CLASS_NAME + j));
				} catch (final ClassNotFoundException e) {
					throw new InvalidSettingsException("Couldn't load feature  class", e);
				}

				final boolean featureSelected = featureSetSettings.getBoolean(FEATURE_CLASS_IS_SELECTED + j);

				selectedFeatures.put(featureClass, featureSelected);
			}

			featureSets.add(new FeatureSetInfo(featureSetClass, fieldNamesAndValues, selectedFeatures));
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {

		final List<FeatureSetInfo> featureSets = new ArrayList<FeatureSetInfo>();

		// load number of feature sets
		final int numFeatureSets = settings.getInt(NUM_FEATURE_SETS);

		// load each feature set
		for (int i = 0; i < numFeatureSets; i++) {

			final NodeSettingsRO featureSetSettings = settings.getNodeSettings(FEATURE_SET + i);

			// load class
			Class<? extends FeatureSet> featureSetClass;
			try {
				featureSetClass = (Class<? extends FeatureSet>) Class
						.forName(featureSetSettings.getString(FEATURE_SET_CLASSNAME));
			} catch (final ClassNotFoundException e) {
				throw new InvalidSettingsException("Couldn't load feature set class", e);
			}

			// load number of fields
			final int numFields = featureSetSettings.getInt(NUM_FIELDS);

			// load each field
			final Map<String, Object> fieldNamesAndValues = new HashMap<String, Object>();
			for (int j = 0; j < numFields; j++) {
				final String fieldName = featureSetSettings.getString(FIELD_NAME + j);

				Object fieldValue;
				try {
					fieldValue = loadObject(featureSetSettings, FIELD_VALUE + j,
							featureSetClass.getDeclaredField(fieldName).getType());
				} catch (NoSuchFieldException | SecurityException e) {
					throw new InvalidSettingsException("Couldn't load field value", e);
				}

				fieldNamesAndValues.put(fieldName, fieldValue);
			}

			// load number of features
			final int numFeatures = featureSetSettings.getInt(NUM_FEATURES);

			final Map<Class<?>, Boolean> selectedFeatures = new HashMap<Class<?>, Boolean>();
			// load each feature
			for (int j = 0; j < numFeatures; j++) {

				Class<?> featureClass;
				try {
					featureClass = Class.forName(featureSetSettings.getString(FEATURE_CLASS_NAME + j));
				} catch (final ClassNotFoundException e) {
					throw new InvalidSettingsException("Couldn't load feature  class", e);
				}

				final boolean featureSelected = featureSetSettings.getBoolean(FEATURE_CLASS_IS_SELECTED + j);

				selectedFeatures.put(featureClass, featureSelected);
			}

			featureSets.add(new FeatureSetInfo(featureSetClass, fieldNamesAndValues, selectedFeatures));
		}

		this.m_featureSets = featureSets;
	}

	@Override
	protected void saveSettingsForModel(final NodeSettingsWO settings) {

		// save number of feature sets
		final int numFeatureSets = this.m_featureSets.size();
		settings.addInt(NUM_FEATURE_SETS, numFeatureSets);

		// save each feature set
		int i = 0;
		final Iterator<FeatureSetInfo> iterator = this.m_featureSets.iterator();
		while (iterator.hasNext()) {
			final FeatureSetInfo featureSetInfo = iterator.next();

			// create a settingsmodel for the feature set
			final NodeSettingsWO featureSetSettings = settings.addNodeSettings(FEATURE_SET + i);

			// save class
			featureSetSettings.addString(FEATURE_SET_CLASSNAME, featureSetInfo.getFeatureSetClass().getCanonicalName());

			// save number of fields
			final List<Pair<String, Object>> sortedFieldNameAndValues = featureSetInfo.getSortedFieldNameAndValues();
			final int numFields = sortedFieldNameAndValues.size();
			featureSetSettings.addInt(NUM_FIELDS, numFields);

			// save each field
			for (int j = 0; j < numFields; j++) {
				featureSetSettings.addString(FIELD_NAME + j, sortedFieldNameAndValues.get(j).getA());

				saveObject(featureSetSettings, FIELD_VALUE + j, sortedFieldNameAndValues.get(j).getB());
			}

			// save number of selected features
			final List<Pair<Class<?>, Boolean>> sortedSelectedFeatures = featureSetInfo.getSortedSelectedFeatures();
			final int numFeatures = sortedSelectedFeatures.size();
			featureSetSettings.addInt(NUM_FEATURES, numFeatures);

			// save each feature
			for (int j = 0; j < numFeatures; j++) {

				featureSetSettings.addString(FEATURE_CLASS_NAME + j, sortedSelectedFeatures.get(j).getA().getName());

				featureSetSettings.addBoolean(FEATURE_CLASS_IS_SELECTED + j, sortedSelectedFeatures.get(j).getB());
			}

			i++;
		}
	}

	private void saveObject(final NodeSettingsWO featureSetSettings, final String name, final Object obj) {
		if (boolean.class.isAssignableFrom(obj.getClass()) || Boolean.class.isAssignableFrom(obj.getClass())) {
			featureSetSettings.addBoolean(name, (boolean) obj);
		} else if (byte.class.isAssignableFrom(obj.getClass()) || Byte.class.isAssignableFrom(obj.getClass())) {
			featureSetSettings.addByte(name, (byte) obj);
		} else if (short.class.isAssignableFrom(obj.getClass()) || Short.class.isAssignableFrom(obj.getClass())) {
			featureSetSettings.addShort(name, (short) obj);
		} else if (int.class.isAssignableFrom(obj.getClass()) || Integer.class.isAssignableFrom(obj.getClass())) {
			featureSetSettings.addInt(name, (int) obj);
		} else if (float.class.isAssignableFrom(obj.getClass()) || Float.class.isAssignableFrom(obj.getClass())) {
			featureSetSettings.addFloat(name, (float) obj);
		} else if (double.class.isAssignableFrom(obj.getClass()) || Double.class.isAssignableFrom(obj.getClass())) {
			featureSetSettings.addDouble(name, (double) obj);
		} else if (String.class.isAssignableFrom(obj.getClass())) {
			featureSetSettings.addString(name, (String) obj);
		} else {
			throw new IllegalArgumentException(
					"Unsupported Object type: " + obj.getClass().getSimpleName() + ". Can't save settings");
		}
	}

	private Object loadObject(final NodeSettingsRO featureSetSettings, final String name, final Class<?> clz)
			throws InvalidSettingsException {

		if (boolean.class.isAssignableFrom(clz) || Boolean.class.isAssignableFrom(clz)) {
			return featureSetSettings.getBoolean(name);
		} else if (byte.class.isAssignableFrom(clz) || Byte.class.isAssignableFrom(clz)) {
			return featureSetSettings.getByte(name);
		} else if (short.class.isAssignableFrom(clz) || Short.class.isAssignableFrom(clz)) {
			return featureSetSettings.getShort(name);
		} else if (int.class.isAssignableFrom(clz) || Integer.class.isAssignableFrom(clz)) {
			return featureSetSettings.getInt(name);
		} else if (float.class.isAssignableFrom(clz) || Float.class.isAssignableFrom(clz)) {
			return featureSetSettings.getFloat(name);
		} else if (double.class.isAssignableFrom(clz) || Double.class.isAssignableFrom(clz)) {
			return featureSetSettings.getDouble(name);
		} else if (String.class.isAssignableFrom(clz)) {
			return featureSetSettings.getString(name);
		} else {
			throw new IllegalArgumentException(
					"Unsupported Object type: " + clz.getSimpleName() + ". Can't load settings");
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " ('" + this.m_configName + "')";
	}

	public void addFeatureSet(final FeatureSetInfo p) {
		this.m_featureSets.add(p);
	}

	public List<FeatureSetInfo> getFeatureSets() {
		return this.m_featureSets;
	}

	public void clearFeatureSets() {
		this.m_featureSets.clear();
	}
}
