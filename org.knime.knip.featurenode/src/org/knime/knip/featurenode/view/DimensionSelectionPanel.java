package org.knime.knip.featurenode.view;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import net.miginfocom.layout.AC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import org.knime.knip.base.node.dialog.DialogComponentDimSelection;

public class DimensionSelectionPanel extends JPanel {

	/**
	 * serialVersionUID.
	 */
	private static final long serialVersionUID = -2266205228449833796L;

	public DimensionSelectionPanel(DialogComponentDimSelection dimSelection) {

		this.setBorder(BorderFactory.createTitledBorder("Dimension Selection:"));
		this.setLayout(new MigLayout(new LC().wrapAfter(1), new AC().fill()
				.grow()));

		this.add(dimSelection.getComponentPanel());
	}

}
