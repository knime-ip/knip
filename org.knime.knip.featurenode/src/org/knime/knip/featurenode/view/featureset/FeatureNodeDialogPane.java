package org.knime.knip.featurenode.view.featureset;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.imagej.ops.features.FeatureSet;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.featurenode.FeatureNodeModel;
import org.knime.knip.featurenode.model.FeatureSetInfo;
import org.knime.knip.featurenode.model.SettingsModelFeatureSet;
import org.scijava.InstantiableException;
import org.scijava.module.ModuleException;
import org.scijava.plugin.PluginInfo;

@SuppressWarnings("rawtypes")
public class FeatureNodeDialogPane extends NodeDialogPane {

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

	private ColumnSelectionPanel columnSelectionPanel;

	private FeatureSetSelectionPanel featureSetSelectionPanel;

	private FeatureSetCollectionPanel featureSetCollectionPanel;

	private DialogComponentStringSelection m_columnCreationModeComponent;

	@SuppressWarnings("unchecked")
	public FeatureNodeDialogPane() {
		// first create the two column selection components
		this.m_imgSelectionComponent = new DialogComponentColumnNameSelection(
				FeatureNodeModel.createImgColumnModel(), "Image", 0, false,
				true, ImgPlusValue.class);
		this.m_labelingSelectionComponent = new DialogComponentColumnNameSelection(
				FeatureNodeModel.createLabelingColumnModel(), "Labeling", 0,
				false, true, LabelingValue.class);
		this.m_columnCreationModeComponent = new DialogComponentStringSelection(
				FeatureNodeModel.createColumnCreationModeModel(),
				"Column Creation Mode", new String[] { "Append", "New Table" });

		// create the three sup panels
		columnSelectionPanel = new ColumnSelectionPanel(
				m_imgSelectionComponent, m_labelingSelectionComponent,
				m_columnCreationModeComponent);
		featureSetSelectionPanel = new FeatureSetSelectionPanel();
		featureSetCollectionPanel = new FeatureSetCollectionPanel();

		// add action listeners
		featureSetSelectionPanel.getAddButton().addActionListener(
				new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						PluginInfo<FeatureSet> currentlySelectedFeatureSet = featureSetSelectionPanel
								.getCurrentlySelectedFeatureSet();
						try {
							featureSetCollectionPanel
									.addFeatureSetPanel(new FeatureSetPanel(
											currentlySelectedFeatureSet));
							getTab("Configuration").revalidate();
						} catch (InstantiableException e1) {
							LOGGER.error("Couldn't add feature set", e1);
						} catch (ModuleException e1) {
							LOGGER.error("Couldn't add feature set", e1);
						}
					}
				});

		JPanel configPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		configPanel.add(columnSelectionPanel, gbc);

		gbc.gridy = 1;
		configPanel.add(featureSetSelectionPanel, gbc);

		gbc.gridy = 2;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		configPanel.add(featureSetCollectionPanel, gbc);

		configPanel.setPreferredSize(new Dimension(792, 500));
		this.addTab("Configuration", configPanel);
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
		this.m_columnCreationModeComponent.saveSettingsTo(settings);
		
		final SettingsModelFeatureSet featureSetSettings = new SettingsModelFeatureSet(
				FEATURE_MODEL_SETTINGS);

		for (FeatureSetPanel fsp : this.featureSetCollectionPanel
				.getSelectedFeatureSets()) {
			final FeatureSetInfo p = fsp.getSerializableInfos();
			featureSetSettings.addFeatureSet(p);
		}

		featureSetSettings.saveSettingsForModel(settings);
	}

	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings,
			final DataTableSpec[] specs) throws NotConfigurableException {
		this.m_imgSelectionComponent.loadSettingsFrom(settings, specs);
		this.m_labelingSelectionComponent.loadSettingsFrom(settings, specs);
		this.m_columnCreationModeComponent.loadSettingsFrom(settings, specs);
		
		final SettingsModelFeatureSet s = new SettingsModelFeatureSet(
				FEATURE_MODEL_SETTINGS);

		// remove all content
		this.featureSetCollectionPanel.clear();

		s.loadSettingsForDialog(settings, specs);
		final List<FeatureSetInfo> features = s.getFeatureSets();
		for (final FeatureSetInfo p : features) {
			try {
				this.featureSetCollectionPanel
						.addFeatureSetPanel(new FeatureSetPanel(p));
			} catch (InstantiableException | ModuleException e) {
				JOptionPane.showMessageDialog(getPanel(),
						"Could not add feature during load.",
						"Feature set error", JOptionPane.ERROR_MESSAGE);
				LOGGER.error(e.getMessage(), e);
			}
		}
	}
}
