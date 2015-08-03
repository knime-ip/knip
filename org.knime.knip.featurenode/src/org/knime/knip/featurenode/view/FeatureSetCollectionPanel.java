package org.knime.knip.featurenode.view;

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
					FeatureSetInfoDialog.openUserDialog(featureSetPanel.getPluginInfo().getLabel(),
							featureSetPanel.getPluginInfo().getDescription());
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
					featureSetPanel.toggleSelectDeselectAll();
					update();
				}
			});

			this.add(featureSetPanel, "wrap 25");
		}
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
