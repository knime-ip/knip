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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

/**
 * Just a container for multiple {@link FeatureSetPanel}s. Also add
 * {@link ActionListener}s to remove one {@link FeatureSetPanel} or to show the
 * {@link FeatureSetInfoDialog}.
 *
 * @author Daniel Seebacher
 */
public class FeatureSetCollectionPanel extends JPanel {

	/**
	 * serialVersionUID.
	 */
	private static final long serialVersionUID = 7093032660249377612L;
	private final List<FeatureSetPanel> featureSets = new ArrayList<FeatureSetPanel>();

	public FeatureSetCollectionPanel() {
		update();
	}

	/**
	 * The draw method, removes all components from this
	 * {@link FeatureSetCollectionPanel}, sets the layout and adds all currently
	 * added {@link FeatureSetPanel}s.
	 */
	private void update() {
		this.removeAll();

		this.setLayout(new MigLayout("", "[grow, fill]", ""));
		for (final FeatureSetPanel featureSetPanel : this.featureSets) {

			// remove old actionlisteners first
			removeActionListeners(featureSetPanel.getMinimizeButton());
			removeActionListeners(featureSetPanel.getInfoButton());
			removeActionListeners(featureSetPanel.getRemoveButton());
			removeActionListeners(featureSetPanel.getSelectAllCheckbox());

			// actionlistener for minimize button
			featureSetPanel.getMinimizeButton().addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					// toggle minimize/maximize
					featureSetPanel.toggleMinimizeMaximize();
					update();
				}
			});

			featureSetPanel.getInfoButton().addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					// toggle info dialog
					FeatureSetInfoDialog featureSetInfoDialog = new FeatureSetInfoDialog(
							featureSetPanel.getModule().getInfo().getName(),
							featureSetPanel.getModule().getInfo().getDescription());

					featureSetInfoDialog
							.setTitle(featureSetPanel.getModule().getInfo().getName() + " information dialog");
					featureSetInfoDialog.pack();
					featureSetInfoDialog.setLocationRelativeTo(null);
					featureSetInfoDialog.setVisible(true);
				}
			});

			featureSetPanel.getRemoveButton().addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					// remove feature set from this collection
					FeatureSetCollectionPanel.this.featureSets.remove(featureSetPanel);
					update();
				}
			});

			featureSetPanel.getSelectAllCheckbox().addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent e) {
					update();
				}
			});

			this.add(featureSetPanel, "wrap 25");
		}

		revalidate();
		repaint();
	}

	/**
	 * Removes all ActionListeners from this AbstractButton
	 *
	 * @param button
	 *            an AbstractButton
	 */
	private void removeActionListeners(final AbstractButton button) {
		// remove old actionlisteners first
		for (final ActionListener al : button.getActionListeners()) {
			button.removeActionListener(al);
		}
	}

	/**
	 * Adds one {@link FeatureSetPanel}
	 *
	 * @param fsp
	 *            a {@link FeatureSetPanel}
	 */
	public void addFeatureSetPanel(final FeatureSetPanel fsp) {
		this.featureSets.add(fsp);
		update();
	}

	/**
	 * @return All currently added {@link FeatureSetPanel}s.
	 */
	public List<FeatureSetPanel> getSelectedFeatureSets() {
		return this.featureSets;
	}

	/**
	 * Removes all {@link FeatureSetPanel}s.
	 */
	public void clear() {
		this.featureSets.clear();
		update();
	}
}
