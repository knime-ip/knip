package org.knime.knip.featurenode.view2.featureset;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;

public class ColumnSelectionPanel extends JPanel {

	/**
	 * serialVersionUID.
	 */
	private static final long serialVersionUID = -7299527416096266344L;

	public ColumnSelectionPanel(
			DialogComponentColumnNameSelection imgColumnComponent,
			DialogComponentColumnNameSelection labelingColumnComponent) {

		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.setBorder(BorderFactory.createTitledBorder("Column Selection:"));
		this.add(Box.createHorizontalGlue());
		this.add(imgColumnComponent.getComponentPanel());
		this.add(Box.createRigidArea(new Dimension(75, 5)));
		this.add(labelingColumnComponent.getComponentPanel());
		this.add(Box.createHorizontalGlue());
	}
}
