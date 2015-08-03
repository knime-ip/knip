package org.knime.knip.featurenode.view;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import net.miginfocom.layout.AC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;

public class ColumnSelectionPanel extends JPanel {

	/**
	 * serialVersionUID.
	 */
	private static final long serialVersionUID = -7299527416096266344L;

	public ColumnSelectionPanel(
			DialogComponentColumnNameSelection imgColumnComponent,
			DialogComponentColumnNameSelection labelingColumnComponent,
			DialogComponentStringSelection m_columnCreationModeComponent) {

		this.setBorder(BorderFactory.createTitledBorder("Column Selection:"));
		this.setLayout(new MigLayout(new LC().wrapAfter(3), new AC().fill()
				.grow()));

		this.add(imgColumnComponent.getComponentPanel());
		this.add(labelingColumnComponent.getComponentPanel());
		this.add(m_columnCreationModeComponent.getComponentPanel());
	}
}
