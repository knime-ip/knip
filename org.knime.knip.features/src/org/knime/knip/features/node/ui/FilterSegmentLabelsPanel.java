package org.knime.knip.features.node.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.knip.features.node.model.SettingsModelFeatureSet;

public class IncludeLabelsPanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1142576679032065287L;

	public IncludeLabelsPanel(final DialogComponent includeLabelsSelectionModel) {
		this.setBorder(BorderFactory.createTitledBorder("Segment Label Filter"));

		this.setLayout(new GridBagLayout());
		
		final GridBagConstraints gbc = SettingsModelFeatureSet.getNewDefaultGridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTH;
		this.add(includeLabelsSelectionModel.getComponentPanel(), gbc);
	}
}
