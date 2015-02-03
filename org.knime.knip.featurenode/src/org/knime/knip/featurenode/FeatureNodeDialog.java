package org.knime.knip.featurenode;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.imagej.ops.features.FeatureSet;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.knip.featurenode.model.FeatureSetInfo;
import org.knime.knip.featurenode.model.SettingsModelFeatureSet;
import org.knime.knip.featurenode.view.FeatureSetInfoJPanel;
import org.knime.knip.featurenode.view.FeatureSetPanel;
import org.scijava.InstantiableException;
import org.scijava.module.ModuleException;
import org.scijava.plugin.PluginInfo;

/**
 * <code>NodeDialog</code> for the "FeatureNode" Node.
 *
 * @author Daniel Seebacher
 * @author Tim-Oliver Buchholz
 */
public class FeatureNodeDialog extends NodeDialogPane {

	private static final String FEATURE_MODEL_SETTINGS = "feature_model_settings";

	/**
	 * The logger instance.
	 */
	private static final NodeLogger LOGGER = NodeLogger
			.getLogger(FeatureNodeModel.class);

	/**
	 * Dialog component to select the image column.
	 */
	private DialogComponentColumnNameSelection m_imgSelectionComponent;

	/**
	 * Dialog component to select the labeling column.
	 */
	private DialogComponentColumnNameSelection m_labelingSelectionComponent;

	/**
	 * Panel which holds all selected features with parameters.
	 */
	private FeatureSetPanel m_featureSetPanel;

	private final JPanel m_dialogPanel;

	public FeatureNodeDialog() {
		this.m_dialogPanel = new JPanel(new GridBagLayout());
		final GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.weighty = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridx = 0;
		c.gridy = 0;

		this.m_dialogPanel.add(createColumnSelectionPanel(), c);

		c.gridy++;
		this.m_dialogPanel.add(createFeatureSetSelectionPanel(), c);

		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.NORTH;
		c.weighty = 1;
		c.gridy++;
		this.m_dialogPanel.add(createSelectedFeaturePanel(), c);

		super.addTab("Configure", this.m_dialogPanel);

	}

	/**
	 * @return panel which holds all selected features
	 */
	private JPanel createSelectedFeaturePanel() {
		this.m_featureSetPanel = new FeatureSetPanel(getPanel());
		final JScrollPane sp = new JScrollPane(this.m_featureSetPanel);
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		sp.getVerticalScrollBar().setUnitIncrement(20);
		sp.setPreferredSize(new Dimension(200, 200));

		final JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(BorderFactory.createTitledBorder("Selected Features"));
		final GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.gridheight = 1;
		c.gridwidth = 1;
		p.add(sp, c);

		return p;
	}

	/**
	 * @return panel to select a feature which should be added to
	 *         {@link FeatureNodeDialog#m_featureSetPanel}.
	 */
	@SuppressWarnings("rawtypes")
	private JPanel createFeatureSetSelectionPanel() {
		final List<PluginInfo<FeatureSet>> fs = OpsGateway.getPluginService()
				.getPluginsOfType(FeatureSet.class);

		final PluginInfoComboboxItem[] plugins = new PluginInfoComboboxItem[fs
				.size()];
		for (int i = 0; i < fs.size(); i++) {
			plugins[i] = new PluginInfoComboboxItem(fs.get(i));
		}

		final JComboBox<PluginInfoComboboxItem> jComboBox = new JComboBox<PluginInfoComboboxItem>(
				plugins);

		final JLabel label = new JLabel("Select feature set: ");

		final JButton add = new JButton("Add");
		add.setToolTipText("Add selected feature set.");
		add.setPreferredSize(jComboBox.getPreferredSize());

		add.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				FeatureSetInfoJPanel f;
				try {

					f = new FeatureSetInfoJPanel(jComboBox.getItemAt(
							jComboBox.getSelectedIndex()).getPluginInfo());

					FeatureNodeDialog.this.m_featureSetPanel.addFeature(f);

					FeatureNodeDialog.this.m_featureSetPanel.revalidate();

				} catch (InstantiableException | ModuleException e1) {
					JOptionPane.showMessageDialog(getPanel(),
							"Could not add feature.", "Feature set error",
							JOptionPane.ERROR_MESSAGE);
					LOGGER.error(e1.getMessage(), e1);
				}

			}
		});

		final JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(BorderFactory.createTitledBorder("Feature selection: "));

		final GridBagConstraints c = new GridBagConstraints();

		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 4, 0, 4);
		c.gridheight = 1;
		c.gridwidth = 1;
		c.weightx = 1;
		c.weighty = 0;
		c.gridx = 0;
		c.gridy = 0;
		p.add(label, c);

		c.gridx++;
		p.add(jComboBox, c);

		c.gridx++;
		p.add(add, c);

		return p;
	}

	/**
	 * @return panel to select one or two input columns.
	 */
	private JPanel createColumnSelectionPanel() {
		this.m_imgSelectionComponent = new DialogComponentColumnNameSelection(
				FeatureNodeModel.createImgSelectionModel(),
				"Select image column: ", 0, false, true,
				FeatureNodeModel.imgPlusFilter());
		this.m_imgSelectionComponent
				.setToolTipText("Select the image column to compute the features on.");

		this.m_labelingSelectionComponent = new DialogComponentColumnNameSelection(
				FeatureNodeModel.createLabelingSelectionModel(),
				"Select labeling column: ", 0, false, true,
				FeatureNodeModel.labelingFilter());
		this.m_labelingSelectionComponent
				.setToolTipText("Select the labeling column to compute the features on.");

		final JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(BorderFactory.createTitledBorder("Column selection: "));

		final GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 0;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;

		p.add(this.m_imgSelectionComponent.getComponentPanel(), c);
		c.gridx++;
		p.add(this.m_labelingSelectionComponent.getComponentPanel(), c);

		return p;
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings)
			throws InvalidSettingsException {
		if ((this.m_imgSelectionComponent.getSelected() == null)
				&& (this.m_labelingSelectionComponent.getSelected() == null)) {
			throw new InvalidSettingsException(
					"Select at least one image column or one labeling column.");
		}

		this.m_imgSelectionComponent.saveSettingsTo(settings);
		this.m_labelingSelectionComponent.saveSettingsTo(settings);
		final List<FeatureSetInfoJPanel> features = this.m_featureSetPanel
				.getFeatureList();

		final SettingsModelFeatureSet featureSetSettings = new SettingsModelFeatureSet(
				FEATURE_MODEL_SETTINGS);

		for (int i = 0; i < features.size(); i++) {
			final FeatureSetInfo p = features.get(i).getSerializableInfos();
			featureSetSettings.addFeatureSet(p);
		}

		featureSetSettings.saveSettingsForModel(settings);
	}

	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings,
			final DataTableSpec[] specs) throws NotConfigurableException {
		this.m_imgSelectionComponent.loadSettingsFrom(settings, specs);
		this.m_labelingSelectionComponent.loadSettingsFrom(settings, specs);
		final SettingsModelFeatureSet s = new SettingsModelFeatureSet(
				FEATURE_MODEL_SETTINGS);

		// remove all content
		this.m_featureSetPanel.clear();

		s.loadSettingsForDialog(settings, specs);
		final List<FeatureSetInfo> features = s.getFeatureSets();
		for (final FeatureSetInfo p : features) {
			try {
				this.m_featureSetPanel.addFeature(new FeatureSetInfoJPanel(p));
			} catch (InstantiableException | ModuleException e) {
				JOptionPane.showMessageDialog(getPanel(),
						"Could not add feature during load.",
						"Feature set error", JOptionPane.ERROR_MESSAGE);
				LOGGER.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * Simple wrapper class to store a {@link PluginInfo} in a Combobox.
	 *
	 * @author Daniel Seebacher, University of Konstanz
	 *
	 */
	@SuppressWarnings("rawtypes")
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
