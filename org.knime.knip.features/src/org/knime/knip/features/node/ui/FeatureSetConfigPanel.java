package org.knime.knip.features.node.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.knime.knip.features.node.model.SettingsModelFeatureSet;

/**
 * FeatureSet Configuration Panel which is placed in the Configure Tab of the
 * Node.
 * 
 * @author Tim-Oliver Buchholz, University of Konstanz
 *
 */
public class FeatureSetConfigPanel extends JPanel {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 2567291859653955359L;

	public FeatureSetConfigPanel(final ColumnSelectionPanel columnSelectionPanel,
			final DimensionSelectionPanel dimensionSelectionPanel,
			final FeatureSetSelectionPanel featureSetSelectionPanel,
			final FeatureSetCollectionPanel featureSetCollectionPanel) {

		final JScrollPane selectedFeatureSetsScrollPane = new JScrollPane();
		selectedFeatureSetsScrollPane.setBorder(BorderFactory.createTitledBorder("Selected Feature Sets:"));
		selectedFeatureSetsScrollPane.getVerticalScrollBar().setUnitIncrement(20);
		selectedFeatureSetsScrollPane.setViewportView(featureSetCollectionPanel);
		// set a size because on first open there is nothing to display here.
		selectedFeatureSetsScrollPane.setPreferredSize(new Dimension(500, 300));

		this.setLayout(new GridBagLayout());
		final GridBagConstraints gbc = SettingsModelFeatureSet.getNewDefaultGridBagConstraints();
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.anchor = GridBagConstraints.NORTH;
		this.add(columnSelectionPanel, gbc);

		gbc.gridy++;

		this.add(dimensionSelectionPanel, gbc);

		gbc.gridy++;

		this.add(featureSetSelectionPanel, gbc);

		gbc.gridy++;
		gbc.weighty = 1;

		this.add(selectedFeatureSetsScrollPane, gbc);
	}
}
