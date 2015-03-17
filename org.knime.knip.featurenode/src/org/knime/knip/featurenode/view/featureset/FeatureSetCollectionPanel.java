package org.knime.knip.featurenode.view.featureset;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;

import net.miginfocom.layout.AC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

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

		this.setLayout(new MigLayout(new LC().wrapAfter(1), new AC().grow()
				.fill()));

		for (final FeatureSetPanel featureSetPanel : featureSets) {

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

			this.add(featureSetPanel);
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
