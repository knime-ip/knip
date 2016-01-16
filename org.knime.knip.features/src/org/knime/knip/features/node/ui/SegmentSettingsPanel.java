package org.knime.knip.features.node.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.knip.features.node.model.SettingsModelFeatureSet;

import net.miginfocom.layout.AC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

public class SegmentSettingsPanel extends JPanel {

	/**
	 * serial version uid
	 */
	private static final long serialVersionUID = 3993686239750347221L;

	public SegmentSettingsPanel(final DialogComponent appendOverlappingLabels,
			final DialogComponent intersectionComponent, final DialogComponent appendSegmentInformationComponent) {
		this.setBorder(BorderFactory.createTitledBorder("Settings"));
		this.setLayout(new GridBagLayout());

		final GridBagConstraints gbc = SettingsModelFeatureSet.getNewDefaultGridBagConstraints();
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		
		this.add(appendOverlappingLabels.getComponentPanel(), gbc);
		
		gbc.gridy++;
		
		this.add(intersectionComponent.getComponentPanel(), gbc);

		gbc.gridy++;
		
		this.add(appendSegmentInformationComponent.getComponentPanel(), gbc);
	}
}
