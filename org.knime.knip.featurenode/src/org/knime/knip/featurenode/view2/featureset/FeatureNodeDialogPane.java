package org.knime.knip.featurenode.view2.featureset;

import java.awt.BorderLayout;
import java.awt.GridLayout;
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
import org.knime.knip.featurenode.FeatureNodeModel;
import org.knime.knip.featurenode.model.FeatureSetInfo;
import org.knime.knip.featurenode.model.SettingsModelFeatureSet;
import org.knime.knip.featurenode.view.FeatureSetInfoJPanel;
import org.scijava.InstantiableException;
import org.scijava.module.ModuleException;
import org.scijava.plugin.PluginInfo;

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

	public FeatureNodeDialogPane() {
		// first create the two column selection components
		this.m_imgSelectionComponent = new DialogComponentColumnNameSelection(
				FeatureNodeModel.createImgSelectionModel(), "Image Column:", 0,
				false, true, FeatureNodeModel.imgPlusFilter());
		this.m_imgSelectionComponent
				.setToolTipText("Select the image column to compute the features on.");
		this.m_labelingSelectionComponent = new DialogComponentColumnNameSelection(
				FeatureNodeModel.createLabelingSelectionModel(),
				"Labeling Column:", 0, false, true,
				FeatureNodeModel.labelingFilter());
		this.m_labelingSelectionComponent
				.setToolTipText("Select the labeling column to compute the features on.");

		// create the three sup panels
		columnSelectionPanel = new ColumnSelectionPanel(
				m_imgSelectionComponent, m_labelingSelectionComponent);
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
							System.out.println("done");
						} catch (InstantiableException e1) {
							LOGGER.error("Couldn't add feature set", e1);
						} catch (ModuleException e1) {
							LOGGER.error("Couldn't add feature set", e1);
						}
					}
				});

		// add panels to dialog
		JPanel northPanel = new JPanel(new GridLayout(0, 1, 0, 20));
		northPanel.add(columnSelectionPanel);
		northPanel.add(featureSetSelectionPanel);

		JPanel superPanel = new JPanel();
		superPanel.setLayout(new BorderLayout(0, 20));
		superPanel.add(northPanel, BorderLayout.NORTH);
		superPanel.add(featureSetCollectionPanel, BorderLayout.CENTER);

		this.addTab("Configuration", superPanel);
	}

	//
	// @Override
	// protected void saveSettingsTo(NodeSettingsWO settings)
	// throws InvalidSettingsException {
	// // TODO Auto-generated method stub
	//
	// }

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
