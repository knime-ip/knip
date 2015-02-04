package org.knime.knip.featurenode.view2.featureset;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.knime.knip.featurenode.view.FeatureSetInfoDialog;

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

		JPanel featureSetsPanel = new JPanel();
		featureSetsPanel.setLayout(new BoxLayout(featureSetsPanel,
				BoxLayout.Y_AXIS));
		for (final FeatureSetPanel featureSetPanel : featureSets) {
			featureSetsPanel.add(featureSetPanel);

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
		featureSetsPanel.add(Box.createVerticalGlue());
		

		JScrollPane pane = new JScrollPane();
		pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		pane.setViewportView(featureSetsPanel);
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.add(pane, BorderLayout.CENTER);
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
