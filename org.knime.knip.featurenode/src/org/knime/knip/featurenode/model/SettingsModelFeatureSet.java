package org.knime.knip.featurenode.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imagej.ops.features.FeatureSet;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;

@SuppressWarnings("rawtypes")
public class SettingsModelFeatureSet extends SettingsModel {

	private final String m_configName;
	private List<FeatureSetInfo> m_featureSets;

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
			final List<FeatureSetInfo> featureSets) {
		if ((configName == null) || "".equals(configName)) {
			throw new IllegalArgumentException("The configName must be a "
					+ "non-empty string");
		}

		this.m_configName = configName;
		if (featureSets == null) {
			this.m_featureSets = new ArrayList<FeatureSetInfo>();
		} else {
			this.m_featureSets = featureSets;
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
		this.m_featureSets = new ArrayList<FeatureSetInfo>();
		this.m_configName = configName;
	}

	public void addFeatureSet(final FeatureSetInfo fs) {
		this.m_featureSets.add(fs);
	}

	public void addFeatureSet(final Class<? extends FeatureSet> featureSetClass) {
		this.m_featureSets
				.add(new FeatureSetInfo(featureSetClass,
						new HashMap<String, Object>(),
						new HashMap<Class<?>, Boolean>()));
	}

	public void addFeatureSet(
			final Class<? extends FeatureSet> featureSetClass,
			final Map<String, Object> fieldNameAndValues) {
		this.m_featureSets.add(new FeatureSetInfo(featureSetClass,
				fieldNameAndValues, new HashMap<Class<?>, Boolean>()));
	}

	public void addFeatureSet(
			final Class<? extends FeatureSet> featureSetClass,
			final Map<String, Object> fieldNameAndValues,
			final Map<Class<?>, Boolean> selectedFeatures) {
		this.m_featureSets.add(new FeatureSetInfo(featureSetClass,
				fieldNameAndValues, selectedFeatures));
	}

	public List<FeatureSetInfo> getFeatureSets() {
		return this.m_featureSets;
	}

	public void clearFeatureSets() {
		this.m_featureSets.clear();
	}

	public void removeFeatureSet(final int index) {
		this.m_featureSets.remove(index);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends SettingsModel> T createClone() {
		return (T) new SettingsModelFeatureSet(this.m_configName,
				this.m_featureSets);
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
	public void loadSettingsForDialog(final NodeSettingsRO settings,
			final PortObjectSpec[] specs) throws NotConfigurableException {

		try {
			final byte[] byteArray = settings.getByteArray("feature_sets");

			if (byteArray != null) {
				this.m_featureSets = decode(byteArray);
			}

		} catch (final InvalidSettingsException e) {
			e.printStackTrace();
		} finally {
			notifyChangeListeners();
		}
	}

	@Override
	public void saveSettingsForDialog(final NodeSettingsWO settings)
			throws InvalidSettingsException {
		saveSettingsForModel(settings);
	}

	@Override
	public void validateSettingsForModel(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		try {
			final byte[] byteArray = settings.getByteArray("feature_sets");

			if (byteArray != null) {
				decode(byteArray);
			}
		} catch (final InvalidSettingsException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void loadSettingsForModel(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		try {
			final byte[] byteArray = settings.getByteArray("feature_sets");

			if (byteArray != null) {
				this.m_featureSets = decode(byteArray);
			}

		} catch (final InvalidSettingsException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void saveSettingsForModel(final NodeSettingsWO settings) {
		final byte[] data = encode(this.m_featureSets);
		settings.addByteArray("feature_sets", data);

	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " ('" + this.m_configName + "')";
	}

	private byte[] encode(final List<FeatureSetInfo> featureSets) {

		byte[] data = null;

		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutputStream out = new ObjectOutputStream(bos)) {
			out.writeObject(featureSets);
			data = bos.toByteArray();
		} catch (final IOException e) {
			throw new IllegalArgumentException(
					"Couldn't convert input object to byte array.");
		}

		if (data == null) {
			throw new IllegalArgumentException(
					"Couldn't convert input object to byte array.");
		}

		return data;
	}

	@SuppressWarnings("unchecked")
	private List<FeatureSetInfo> decode(final byte[] data) {
		List<FeatureSetInfo> featureSets = new ArrayList<FeatureSetInfo>();

		try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
				ObjectInput in = new ObjectInputStream(bis)) {
			featureSets = (List<FeatureSetInfo>) in.readObject();
		} catch (final IOException e) {
			throw new IllegalArgumentException(
					"Couldn't convert byte array to input object", e);
		} catch (final ClassNotFoundException e) {
			throw new IllegalArgumentException(
					"Couldn't convert byte array to input object", e);
		}

		return featureSets;
	}
}
