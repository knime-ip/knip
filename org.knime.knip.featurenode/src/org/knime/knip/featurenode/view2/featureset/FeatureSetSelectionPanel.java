package org.knime.knip.featurenode.view2.featureset;

import java.awt.Dimension;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import net.imagej.ops.features.FeatureSet;

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
		final PluginInfoComboboxItem[] featureSetComboBoxItems = new PluginInfoComboboxItem[fs
				.size()];
		for (int i = 0; i < fs.size(); i++) {
			featureSetComboBoxItems[i] = new PluginInfoComboboxItem(fs.get(i));
		}

		// create combobox and add button
		featureSetComboxBox = new JComboBox<PluginInfoComboboxItem>(
				featureSetComboBoxItems);
		addButton = new JButton("Add");

		// set sizes
		featureSetComboxBox.setMaximumSize(featureSetComboxBox.getPreferredSize());
		addButton.setMaximumSize(addButton.getPreferredSize());

		// add everything to this jpanel
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.setBorder(BorderFactory.createTitledBorder("Select Feature Set:"));
		this.add(Box.createHorizontalGlue());
		this.add(featureSetComboxBox);
		this.add(Box.createRigidArea(new Dimension(30, 5)));
		this.add(addButton);
		this.add(Box.createHorizontalGlue());
	}

	public JButton getAddButton() {
		return addButton;
	}

	public PluginInfo<FeatureSet> getCurrentlySelectedFeatureSet() {
		return featureSetComboxBox.getItemAt(
				featureSetComboxBox.getSelectedIndex()).getPluginInfo();
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
