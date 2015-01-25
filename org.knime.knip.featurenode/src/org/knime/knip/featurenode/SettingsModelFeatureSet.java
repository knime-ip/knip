package org.knime.knip.featurenode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelFlowVariableCompatible;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.FlowVariable.Type;
import org.scijava.plugin.Parameter;
import org.scijava.service.Service;

public class SettingsModelFeatureSet extends SettingsModel implements
		SettingsModelFlowVariableCompatible {

	private String m_configName;
	private List<Pair<Class<?>, Map<String, Object>>> m_featureSets;

	/**
	 * Creates a new object holding an integer value.
	 *
	 * @param configName
	 *            the identifier the value is stored with in the
	 *            {@link org.knime.core.node.NodeSettings} object
	 * @param defaultValue
	 *            the initial value
	 */
	public SettingsModelFeatureSet(final String configName,
			List<Pair<Class<?>, Map<String, Object>>> featureSets) {
		if ((configName == null) || "".equals(configName)) {
			throw new IllegalArgumentException("The configName must be a "
					+ "non-empty string");
		}

		m_configName = configName;
		if (featureSets == null) {
			m_featureSets = new ArrayList<Pair<Class<?>, Map<String, Object>>>();
		} else {
			m_featureSets = featureSets;
		}
	}

	/**
	 * Creates a new object holding an integer value.
	 *
	 * @param configName
	 *            the identifier the value is stored with in the
	 *            {@link org.knime.core.node.NodeSettings} object
	 * @param defaultValue
	 *            the initial value
	 */
	public SettingsModelFeatureSet(final String configName) {
		if ((configName == null) || "".equals(configName)) {
			throw new IllegalArgumentException("The configName must be a "
					+ "non-empty string");
		}
		m_featureSets = new ArrayList<Pair<Class<?>, Map<String, Object>>>();
		m_configName = configName;
	}

	public void addFeatureSet(Class<?> featureSet,
			Map<String, Object> fieldNameAndValues) {

		m_featureSets.add(new ValuePair<Class<?>, Map<String, Object>>(
				featureSet, fieldNameAndValues));
	}

	public List<Pair<Class<?>, Map<String, Object>>> getFeatureSets() {
		return m_featureSets;
	}

	public void clearFeatureSets() {
		m_featureSets.clear();
	}

	public void removeFeatureSet(int index) {
		m_featureSets.remove(index);
	}

	@Override
	public String getKey() {
		return m_configName;
	}

	@Override
	public Type getFlowVariableType() {
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected <T extends SettingsModel> T createClone() {
		return (T) new SettingsModelFeatureSet(m_configName, m_featureSets);
	}

	@Override
	protected String getModelTypeID() {
		return "SMID_featureset";
	}

	@Override
	protected String getConfigName() {
		return m_configName;
	}

	@Override
	protected void loadSettingsForDialog(NodeSettingsRO settings,
			PortObjectSpec[] specs) throws NotConfigurableException {
		try {
			m_featureSets = new ArrayList<Pair<Class<?>, Map<String, Object>>>();
			for (int i = 0; i < settings.getInt("num_featuresets", 0); i++) {

				NodeSettingsRO featureSetNodeSettings = settings
						.getNodeSettings("feature_set_" + i);

				Class<?> featureSet = Class.forName(featureSetNodeSettings
						.getString("feature_set_name"));

				NodeSettingsRO fieldNodeSettings = settings
						.getNodeSettings("feature_set_" + i + "_field_settings");

				Map<String, Object> fieldNameAndValues = new HashMap<String, Object>();
				for (Field field : featureSet.getDeclaredFields()) {
					if (field.getAnnotation(Parameter.class) == null) {
						continue;
					}
					
					String fieldName = field.getName();
					Object fieldValue = loadObjet(fieldNodeSettings,
							field.getType(), fieldName);
					if (fieldValue != null) {
						fieldNameAndValues.put(fieldName, fieldValue);
					}
				}

				m_featureSets.add(new ValuePair<Class<?>, Map<String, Object>>(
						featureSet, fieldNameAndValues));
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InvalidSettingsException e) {
			e.printStackTrace();
		} finally {
			notifyChangeListeners();
		}
	}

	@Override
	protected void saveSettingsForDialog(NodeSettingsWO settings)
			throws InvalidSettingsException {
		saveSettingsForModel(settings);
	}

	@Override
	protected void validateSettingsForModel(NodeSettingsRO settings)
			throws InvalidSettingsException {
		try {
			for (int i = 0; i < settings.getInt("num_featuresets", 0); i++) {

				NodeSettingsRO featureSetNodeSettings = settings
						.getNodeSettings("feature_set_" + i);

				Class<?> featureSet = Class.forName(featureSetNodeSettings
						.getString("feature_set_name"));

				NodeSettingsRO fieldNodeSettings = settings
						.getNodeSettings("feature_set_" + i + "_field_settings");

				Map<String, Object> fieldNameAndValues = new HashMap<String, Object>();
				for (Field field : featureSet.getDeclaredFields()) {

					if (field.getAnnotation(Parameter.class) == null) {
						continue;
					}

					String fieldName = field.getName();
					Object fieldValue = loadObjet(fieldNodeSettings,
							field.getType(), fieldName);
					if (fieldValue != null) {
						fieldNameAndValues.put(fieldName, fieldValue);
					}
				}
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void loadSettingsForModel(NodeSettingsRO settings)
			throws InvalidSettingsException {
		try {
			m_featureSets = new ArrayList<Pair<Class<?>, Map<String, Object>>>();
			for (int i = 0; i < settings.getInt("num_featuresets", 0); i++) {

				NodeSettingsRO featureSetNodeSettings = settings
						.getNodeSettings("feature_set_" + i);

				Class<?> featureSet = Class.forName(featureSetNodeSettings
						.getString("feature_set_name"));

				NodeSettingsRO fieldNodeSettings = settings
						.getNodeSettings("feature_set_" + i + "_field_settings");

				Map<String, Object> fieldNameAndValues = new HashMap<String, Object>();
				for (Field field : featureSet.getDeclaredFields()) {
					if (field.getAnnotation(Parameter.class) == null) {
						continue;
					}
					
					String fieldName = field.getName();
					Object fieldValue = loadObjet(fieldNodeSettings,
							field.getType(), fieldName);
					if (fieldValue != null) {
						fieldNameAndValues.put(fieldName, fieldValue);
					}
				}

				m_featureSets.add(new ValuePair<Class<?>, Map<String, Object>>(
						featureSet, fieldNameAndValues));
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void saveSettingsForModel(NodeSettingsWO settings) {
		settings.addInt("num_featuresets", (m_featureSets == null) ? 0
				: m_featureSets.size());
		for (int i = 0; i < m_featureSets.size(); i++) {

			NodeSettingsWO featureSetNodeSettings = settings
					.addNodeSettings("feature_set_" + i);

			Class<?> featureSet = m_featureSets.get(i).getA();
			Map<String, Object> fieldNameValueMap = m_featureSets.get(i).getB();

			featureSetNodeSettings.addString("feature_set_name", m_featureSets
					.get(i).getA().getCanonicalName());

			NodeSettingsWO fieldNodeSettings = settings
					.addNodeSettings("feature_set_" + i + "_field_settings");

			if (fieldNameValueMap != null && !fieldNameValueMap.isEmpty()) {

				for (Entry<String, Object> fieldNameAndValue : fieldNameValueMap
						.entrySet()) {

					Field f = null;
					try {
						f = featureSet.getDeclaredField(fieldNameAndValue
								.getKey());
					} catch (NoSuchFieldException | SecurityException e) {
						throw new IllegalArgumentException(
								"Can't save settings", e);
					}

					saveObject(fieldNodeSettings, f.getType(),
							fieldNameAndValue.getKey(),
							fieldNameAndValue.getValue());
				}
			}
		}
	}

	private void saveObject(NodeSettingsWO settings, Class<?> type, String key,
			Object value) {
		if (String.class.isAssignableFrom(type)) {
			settings.addString(key, (String) value);
		} else if (Double.class.isAssignableFrom(type)
				|| double.class.isAssignableFrom(type)) {
			settings.addDouble(key, (Double) value);
		} else if (Integer.class.isAssignableFrom(type)
				|| int.class.isAssignableFrom(type)) {
			settings.addInt(key, (Integer) value);
		} else if (Boolean.class.isAssignableFrom(type)
				|| boolean.class.isAssignableFrom(type)) {
			settings.addBoolean(key, (Boolean) value);
		} else if (Service.class.isAssignableFrom(type)) {
			// ignore, cant load or store services
		} else {
			throw new IllegalArgumentException("Can't save values of type "
					+ type);
		}
	}

	private Object loadObjet(NodeSettingsRO settings, Class<?> fieldType,
			String fieldName) throws InvalidSettingsException {
		if (String.class.isAssignableFrom(fieldType)) {
			return settings.getString(fieldName);
		} else if (Double.class.isAssignableFrom(fieldType)
				|| double.class.isAssignableFrom(fieldType)) {
			return settings.getDouble(fieldName);
		} else if (Integer.class.isAssignableFrom(fieldType)
				|| int.class.isAssignableFrom(fieldType)) {
			return settings.getInt(fieldName);
		} else if (Boolean.class.isAssignableFrom(fieldType)
				|| boolean.class.isAssignableFrom(fieldType)) {
			return settings.getBoolean(fieldName);
		} else if (Service.class.isAssignableFrom(fieldType)) {
			// ignore, cant load or store services
			return null;
		} else {
			throw new IllegalArgumentException("Can't load values of type "
					+ fieldType);
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " ('" + m_configName + "')";
	}
}
