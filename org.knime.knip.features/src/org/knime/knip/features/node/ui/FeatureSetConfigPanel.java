/*
 * ------------------------------------------------------------------------
 *
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
  ---------------------------------------------------------------------
 *
 */

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
