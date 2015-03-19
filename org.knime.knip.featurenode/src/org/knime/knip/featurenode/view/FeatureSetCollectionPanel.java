package org.knime.knip.featurenode.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

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
		for (final FeatureSetPanel featureSetPanel : featureSets) {

			// remove old actionlisteners first
			for (ActionListener al : featureSetPanel.getInfoButton()
					.getActionListeners()) {
				featureSetPanel.getInfoButton().removeActionListener(al);
			}

			for (ActionListener al : featureSetPanel.getRemoveButton()
					.getActionListeners()) {
				featureSetPanel.getRemoveButton().removeActionListener(al);
			}

			featureSetPanel.getInfoButton().addActionListener(
					new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							FeatureSetInfoDialog.openUserDialog(null,
									featureSetPanel.getPluginInfo().getLabel(),
									featureSetPanel.getPluginInfo()
											.getDescription());
						}
					});

			featureSetPanel.getRemoveButton().addActionListener(
					new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							featureSets.remove(featureSetPanel);
							update();
						}
					});

			this.add(featureSetPanel, "wrap 25");
		}
	}

	/**
	 * Adds one {@link FeatureSetPanel}
	 * 
	 * @param fsp
	 *            a {@link FeatureSetPanel}
	 */
	public void addFeatureSetPanel(FeatureSetPanel fsp) {
		this.featureSets.add(fsp);
		update();
	}

	/**
	 * @return All currently added {@link FeatureSetPanel}s.
	 */
	public List<FeatureSetPanel> getSelectedFeatureSets() {
		return featureSets;
	}

	/**
	 * Removes all {@link FeatureSetPanel}s.
	 */
	public void clear() {
		this.featureSets.clear();
		update();
	}
}
