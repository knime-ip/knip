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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.knime.knip.core.KNIPGateway;
import org.knime.knip.features.node.model.FeatureSetInfo;
import org.knime.knip.features.node.model.SettingsModelFeatureSet;
import org.knime.knip.features.sets.FeatureSet;
import org.scijava.command.CommandInfo;

@SuppressWarnings("rawtypes")
public class FeatureSetSelectionPanel extends JPanel {

	/**
	 * serialVersionUID.
	 */
	private static final long serialVersionUID = 1691899824989296852L;
	private final JComboBox<CommandInfo> featureSetComboxBox;
	private final JButton addButton;

	public FeatureSetSelectionPanel(final FeatureSetCollectionPanel featureSetCollectionPanel) {

		// get all available featuresets
		final List<CommandInfo> featureSetList = KNIPGateway.cs().getCommandsOfType(FeatureSet.class);

		this.featureSetComboxBox = new JComboBox<CommandInfo>(featureSetList.toArray(new CommandInfo[featureSetList.size()]));
		this.featureSetComboxBox.setRenderer(new ListCellRenderer<CommandInfo>() {

			@Override
			public Component getListCellRendererComponent(final JList<? extends CommandInfo> list, final CommandInfo value,
					final int index, final boolean isSelected, final boolean cellHasFocus) {

				final JLabel renderer = (JLabel) new DefaultListCellRenderer().getListCellRendererComponent(list, value,
						index, isSelected, cellHasFocus);

				if (!isSelected) {
					renderer.setForeground(list.getForeground());
				}

				renderer.setText(value.getLabel());
				
				return renderer;
			}
		});
		
		this.addButton = new JButton("Add");
		this.addButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					featureSetCollectionPanel.addFeatureSetPanel(
							new FeatureSetPanel(new FeatureSetInfo(getSelectedFeatureSetType(), null)));
				} catch (final Throwable e1) {
					KNIPGateway.log().error("Couldn't add feature set", e1);
				}
			}
		});

		this.setBorder(BorderFactory.createTitledBorder("Select Feature Set:"));
		this.setLayout(new GridBagLayout());
		
		final GridBagConstraints gbc = SettingsModelFeatureSet.getNewDefaultGridBagConstraints();
		gbc.weightx = 0;
		
		this.add(this.featureSetComboxBox, gbc);
		
		gbc.gridx++;
		this.add(this.addButton, gbc);
	}

	@SuppressWarnings("unchecked")
	public Class<? extends FeatureSet> getSelectedFeatureSetType() {
		try {
			return (Class<? extends FeatureSet>) Class.forName(this.featureSetComboxBox
					.getItemAt(this.featureSetComboxBox.getSelectedIndex()).getDelegateClassName());
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

}
