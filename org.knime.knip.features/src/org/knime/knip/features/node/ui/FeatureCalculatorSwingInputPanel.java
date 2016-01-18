package org.knime.knip.features.node.ui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.scijava.widget.AbstractInputPanel;
import org.scijava.widget.InputWidget;
import org.scijava.widget.WidgetModel;

public class FeatureCalculatorSwingInputPanel extends AbstractInputPanel<JPanel, JPanel> {

	private JPanel uiComponent;

	private GridBagConstraints uiGBC;
	
	@Override
	public void addWidget(InputWidget<?, JPanel> widget) {
		super.addWidget(widget);
		final JPanel widgetPane = widget.getComponent();
		final WidgetModel model = widget.get();

		// add widget to panel
		final JPanel labeledWidget = new JPanel();
		labeledWidget.setLayout(new BorderLayout());
		if (widget.isLabeled()) {
			// widget is suffixed by a label
			final JLabel l = new JLabel(model.getWidgetLabel());
			final String desc = model.getItem().getDescription();
			if (desc != null && !desc.isEmpty())
				l.setToolTipText(desc);
			labeledWidget.add(widgetPane, BorderLayout.WEST);
			labeledWidget.add(l);
		} else {
			labeledWidget.add(widgetPane);
		}

		getComponent().add(labeledWidget, uiGBC);

		updateUIGBC();
	}

	private void updateUIGBC() {
		if (uiGBC.gridx % 3 == 2) {
			uiGBC.gridx = 0;
			uiGBC.gridy++;
		} else {
			uiGBC.gridx++;
		}
	}
	
	@Override
	public Class<JPanel> getWidgetComponentType() {
		return JPanel.class;
	}

	@Override
	public JPanel getComponent() {
		if (uiComponent == null) {
			uiComponent = new JPanel(new GridBagLayout());

			uiGBC = new GridBagConstraints();
			uiGBC.fill = GridBagConstraints.BOTH;
			uiGBC.anchor = GridBagConstraints.NORTH;
			uiGBC.gridx = 0;
			uiGBC.gridy = 0;
			uiGBC.gridheight = 1;
			uiGBC.gridwidth = 1;
			uiGBC.weightx = 1;
			uiGBC.insets = new Insets(2, 2, 2, 2);
		}
		return uiComponent;
	}
	
	@Override
	public Class<JPanel> getComponentType() {
		return JPanel.class;
	}
}
