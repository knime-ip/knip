package org.knime.knip.features.node.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.knip.features.node.model.SettingsModelFeatureSet;

public class FilterOverlappingLabelsPanel extends JPanel {

	/**
	 * serial version uid
	 */
	private static final long serialVersionUID = 2927840146441921540L;
	
	
	public FilterOverlappingLabelsPanel(final DialogComponent filterOverlappingLabelsModel) {
		this.setBorder(BorderFactory.createTitledBorder("Filter overlapping segment labels"));

		this.setLayout(new GridBagLayout());
		
		final GridBagConstraints gbc = SettingsModelFeatureSet.getNewDefaultGridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weighty = 0;
		this.add(filterOverlappingLabelsModel.getComponentPanel(), gbc);
	}

}
