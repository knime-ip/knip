/*
 * ------------------------------------------------------------------------
 *
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
  ---------------------------------------------------------------------
 *
 */

package org.knime.knip.features.node.ui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

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
