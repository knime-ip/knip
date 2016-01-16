package org.knime.knip.features.node.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.knip.features.node.model.SettingsModelFeatureSet;

public class FilterSegmentLabelsPanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1142576679032065287L;

	public FilterSegmentLabelsPanel(final DialogComponent filterLabelsSelectionModel) {
		this.setBorder(BorderFactory.createTitledBorder("Filter on segment labels"));

		this.setLayout(new GridBagLayout());
		
		final GridBagConstraints gbc = SettingsModelFeatureSet.getNewDefaultGridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weighty = 0;
		this.add(filterLabelsSelectionModel.getComponentPanel(), gbc);
	}
}
