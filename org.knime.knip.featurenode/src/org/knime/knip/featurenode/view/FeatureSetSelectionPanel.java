package org.knime.knip.featurenode.view;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import net.imagej.ops.features.FeatureSet;
import net.miginfocom.swing.MigLayout;

import org.knime.knip.featurenode.OpsGateway;
import org.scijava.plugin.PluginInfo;

@SuppressWarnings("rawtypes")
public class FeatureSetSelectionPanel extends JPanel {

	/**
	 * serialVersionUID.
	 */
	private static final long serialVersionUID = 1691899824989296852L;
	private JComboBox<PluginInfoComboboxItem> featureSetComboxBox;
	private JButton addButton;

	public FeatureSetSelectionPanel() {

		// create an array of plugininfos to put into a combobox
		final List<PluginInfo<FeatureSet>> fs = OpsGateway.getPluginService()
				.getPluginsOfType(FeatureSet.class);

		final List<PluginInfoComboboxItem> featureSetComboBoxItems = new ArrayList<PluginInfoComboboxItem>();
		for (int i = 0; i < fs.size(); i++) {
			featureSetComboBoxItems.add(new PluginInfoComboboxItem(fs.get(i)));
		}

		// create combobox and add button
		this.featureSetComboxBox = new JComboBox<PluginInfoComboboxItem>(
				featureSetComboBoxItems
						.toArray(new PluginInfoComboboxItem[featureSetComboBoxItems
								.size()]));
		this.addButton = new JButton("Add");

		// set sizes
		this.featureSetComboxBox.setMaximumSize(this.featureSetComboxBox
				.getPreferredSize());
		this.addButton.setMaximumSize(this.addButton.getPreferredSize());

		// add everything to this jpanel
		this.setBorder(BorderFactory.createTitledBorder("Select Feature Set:"));
		this.setLayout(new MigLayout("", "push[][]push", ""));
		this.add(this.featureSetComboxBox);
		this.add(this.addButton);
	}

	public JButton getAddButton() {
		return this.addButton;
	}

	public PluginInfo<FeatureSet> getCurrentlySelectedFeatureSet() {
		return this.featureSetComboxBox.getItemAt(
				this.featureSetComboxBox.getSelectedIndex()).getPluginInfo();
	}

	/**
	 * Simple wrapper class to store a {@link PluginInfo} in a Combobox.
	 *
	 * @author Daniel Seebacher, University of Konstanz
	 *
	 */
	private class PluginInfoComboboxItem {

		private final PluginInfo<FeatureSet> pluginInfo;

		/**
		 * Default constructor.
		 *
		 * @param pluginInfo
		 */
		public PluginInfoComboboxItem(final PluginInfo<FeatureSet> pluginInfo) {
			this.pluginInfo = pluginInfo;
		}

		public PluginInfo<FeatureSet> getPluginInfo() {
			return this.pluginInfo;
		}

		@Override
		public String toString() {
			return this.pluginInfo.getLabel();
		}
	}
}
