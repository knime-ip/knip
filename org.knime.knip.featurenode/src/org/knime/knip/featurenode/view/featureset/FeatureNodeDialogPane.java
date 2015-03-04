package org.knime.knip.featurenode.view.featureset;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

public class FeatureNodeDialogPane extends NodeDialogPane {

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
	 * Dialog component to select the column creation mode;
	 */
	private DialogComponentStringSelection m_columnCreationModeComponent;

	/**
	 * Feature Set GUI Elements
	 */
	private ColumnSelectionPanel columnSelectionPanel;
	private FeatureSetSelectionPanel featureSetSelectionPanel;
	private FeatureSetCollectionPanel featureSetCollectionPanel;
	private final SettingsModelFeatureSet smfs;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public FeatureNodeDialogPane() {

		// first create the column selection components
		this.m_imgSelectionComponent = new DialogComponentColumnNameSelection(
				FeatureNodeModel.createImgColumnModel(), "Image", 0, false,
				true, ImgPlusValue.class);
		this.m_labelingSelectionComponent = new DialogComponentColumnNameSelection(
				FeatureNodeModel.createLabelingColumnModel(), "Labeling", 0,
				false, true, LabelingValue.class);
		this.m_columnCreationModeComponent = new DialogComponentStringSelection(
				FeatureNodeModel.createColumnCreationModeModel(),
				"Column Creation Mode", new String[] { "Append", "New Table" });
		this.smfs = new SettingsModelFeatureSet(
				FeatureNodeModel.CFG_KEY_FEATURE_SETS);

		// create the three sup panels
		this.columnSelectionPanel = new ColumnSelectionPanel(
				this.m_imgSelectionComponent,
				this.m_labelingSelectionComponent,
				this.m_columnCreationModeComponent);
		this.featureSetSelectionPanel = new FeatureSetSelectionPanel();
		this.featureSetCollectionPanel = new FeatureSetCollectionPanel();

		// add action listeners
		this.featureSetSelectionPanel.getAddButton().addActionListener(
				new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						final PluginInfo<FeatureSet> currentlySelectedFeatureSet = FeatureNodeDialogPane.this.featureSetSelectionPanel
								.getCurrentlySelectedFeatureSet();
						try {
							FeatureNodeDialogPane.this.featureSetCollectionPanel
									.addFeatureSetPanel(new FeatureSetPanel(
											currentlySelectedFeatureSet));
							getTab("Configuration").revalidate();
						} catch (final InstantiableException e1) {
							LOGGER.error("Couldn't add feature set", e1);
						} catch (final ModuleException e1) {
							LOGGER.error("Couldn't add feature set", e1);
						}
					}
				});

		final JPanel configPanel = new JPanel(new GridBagLayout());
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		configPanel.add(this.columnSelectionPanel, gbc);

		gbc.gridy = 1;
		configPanel.add(this.featureSetSelectionPanel, gbc);

		gbc.gridy = 2;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		configPanel.add(this.featureSetCollectionPanel, gbc);

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

		this.smfs.clearFeatureSets();
		for (final FeatureSetPanel fsp : this.featureSetCollectionPanel
				.getSelectedFeatureSets()) {
			final FeatureSetInfo p = fsp.getSerializableInfos();
			this.smfs.addFeatureSet(p);
		}

		this.smfs.saveSettingsForDialog(settings);
		this.m_imgSelectionComponent.saveSettingsTo(settings);
		this.m_labelingSelectionComponent.saveSettingsTo(settings);
		this.m_columnCreationModeComponent.saveSettingsTo(settings);
	}

	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings,
			final DataTableSpec[] specs) throws NotConfigurableException {
		this.m_imgSelectionComponent.loadSettingsFrom(settings, specs);
		this.m_labelingSelectionComponent.loadSettingsFrom(settings, specs);
		this.m_columnCreationModeComponent.loadSettingsFrom(settings, specs);
		this.smfs.loadSettingsForDialog(settings, specs);

		// remove all content
		this.featureSetCollectionPanel.clear();

		for (final FeatureSetInfo p : this.smfs.getFeatureSets()) {
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
