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
			final byte[] byteArray = settings.getByteArray(this.m_configName);

			if (byteArray != null) {
				this.m_featureSets = decode(byteArray);
			}

		} catch (final InvalidSettingsException e) {
			// ignore
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
		final byte[] byteArray = settings.getByteArray(this.m_configName);

		if (byteArray != null) {
			decode(byteArray);
		}

	}

	@Override
	public void loadSettingsForModel(final NodeSettingsRO settings)
			throws InvalidSettingsException {
		final byte[] byteArray = settings.getByteArray(this.m_configName);

		if (byteArray != null) {
			this.m_featureSets = decode(byteArray);
		}

	}

	@Override
	public void saveSettingsForModel(final NodeSettingsWO settings) {
		final byte[] data = encode(this.m_featureSets);
		settings.addByteArray(this.m_configName, data);
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

			e.printStackTrace();

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

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result)
				+ ((this.m_configName == null) ? 0 : this.m_configName
						.hashCode());
		result = (prime * result)
				+ ((this.m_featureSets == null) ? 0 : this.m_featureSets
						.hashCode());
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
		if (!(obj instanceof SettingsModelFeatureSet)) {
			return false;
		}
		final SettingsModelFeatureSet other = (SettingsModelFeatureSet) obj;
		if (this.m_configName == null) {
			if (other.m_configName != null) {
				return false;
			}
		} else if (!this.m_configName.equals(other.m_configName)) {
			return false;
		}
		if (this.m_featureSets == null) {
			if (other.m_featureSets != null) {
				return false;
			}
		} else if (!(this.m_featureSets.containsAll(other.m_featureSets) && other.m_featureSets
				.containsAll(this.m_featureSets))) {
			return false;
		}

		return true;
	}

}
