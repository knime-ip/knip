package org.knime.knip.featurenode.view.featureset;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class FeatureSetCollectionPanel extends JPanel {

	/**
	 * serialVersionUID.
	 */
	private static final long serialVersionUID = 7093032660249377612L;
	private final List<FeatureSetPanel> featureSets = new ArrayList<FeatureSetPanel>();

	public FeatureSetCollectionPanel() {
		update();
	}

	private void update() {
		this.removeAll();
		this.setBorder(BorderFactory
				.createTitledBorder("Selected Feature Sets:"));
		
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		for (final FeatureSetPanel featureSetPanel : featureSets) {
			this.add(featureSetPanel);

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
		}

		this.add(Box.createVerticalGlue());
	}

	public void addFeatureSetPanel(FeatureSetPanel fsp) {
		this.featureSets.add(fsp);
		update();
	}

	public List<FeatureSetPanel> getSelectedFeatureSets() {
		return featureSets;
	}

	public void clear() {
		this.featureSets.clear();
		update();
	}
}
