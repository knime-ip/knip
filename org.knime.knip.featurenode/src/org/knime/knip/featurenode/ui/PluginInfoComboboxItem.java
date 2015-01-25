package org.knime.knip.featurenode.ui;

import net.imagej.ops.features.FeatureSet;

import org.scijava.plugin.PluginInfo;

/**
 * Simple wrapper class to store a {@link PluginInfo} in a Combobox.
 * 
 * @author Daniel Seebacher, University of Konstanz
 *
 */
@SuppressWarnings("rawtypes")
public class PluginInfoComboboxItem {

	private final PluginInfo<FeatureSet> pluginInfo;

	/**
	 * Default constructor.
	 * 
	 * @param pluginInfo
	 */
	public PluginInfoComboboxItem(PluginInfo<FeatureSet> pluginInfo) {
		this.pluginInfo = pluginInfo;
	}

	public PluginInfo<FeatureSet> getPluginInfo() {
		return pluginInfo;
	}

	@Override
	public String toString() {
		return pluginInfo.getLabel();
	}
}