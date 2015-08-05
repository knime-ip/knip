package org.knime.knip.featurenode.view;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import net.miginfocom.layout.AC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import org.knime.core.node.defaultnodesettings.DialogComponent;

public class LabelSettingsPanel extends JPanel {

	public LabelSettingsPanel(DialogComponent appendOverlappingLabels,
			DialogComponent intersectionComponent,

			DialogComponent appendSegmentInformationComponent,
			DialogComponent includeLabelsSelectionModel) {

		this.setBorder(BorderFactory.createTitledBorder("Segment Settings"));
		this.setLayout(new MigLayout(new LC().wrapAfter(1), new AC().fill()
				.grow()));

		this.add(appendOverlappingLabels.getComponentPanel());
		this.add(intersectionComponent.getComponentPanel());
		this.add(appendSegmentInformationComponent.getComponentPanel());

		JPanel includeLabelsPanel = new JPanel(new MigLayout(
				new LC().wrapAfter(1), new AC().fill().grow()));
		includeLabelsPanel.setBorder(BorderFactory
				.createTitledBorder("Segment Label Filter"));
		includeLabelsPanel.add(includeLabelsSelectionModel.getComponentPanel());

		this.add(includeLabelsPanel);
	}
}
