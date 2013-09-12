/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.knip.base.node.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.base.node.nodesettings.SettingsModelFilterSelection;
import org.knime.knip.core.ui.imgviewer.events.RulebasedLabelFilter;

/**
 * TODO Auto-generated
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class DialogComponentFilterSelection<L extends Comparable<L>> extends DialogComponent implements ChangeListener {

    private final JButton m_addButton;

    private final JComboBox m_operatorBox;

    private JButton m_removeButton;

    private final List<JButton> m_removeButtons;

    private final List<JTextField> m_textFields;

    private final JPanel m_textFieldsPanel;

    public DialogComponentFilterSelection(final SettingsModelFilterSelection<L> model) {
        super(model);

        model.addChangeListener(this);

        m_textFields = new ArrayList<JTextField>();
        m_removeButtons = new ArrayList<JButton>();

        final JPanel m_confirmationPanel = new JPanel();
        m_confirmationPanel.setLayout(new BoxLayout(m_confirmationPanel, BoxLayout.X_AXIS));

        m_textFieldsPanel = new JPanel();
        m_textFieldsPanel.setLayout(new BoxLayout(m_textFieldsPanel, BoxLayout.X_AXIS));

        m_addButton = new JButton("+");
        m_addButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                addTextField("");
            }
        });

        m_operatorBox = new JComboBox(RulebasedLabelFilter.Operator.values());
        m_operatorBox.setSize(new Dimension(43, 20));
        m_operatorBox.setMaximumSize(new Dimension(43, 20));

        m_confirmationPanel.add(m_addButton);
        m_confirmationPanel.add(m_operatorBox);

        getComponentPanel().setLayout(new BoxLayout(getComponentPanel(), BoxLayout.X_AXIS));

        getComponentPanel().add(m_textFieldsPanel);
        getComponentPanel().add(m_confirmationPanel);

        stateChanged(null);

    }

    protected void addTextField(final String initValue) {
        final JPanel oneFieldRow = new JPanel();
        oneFieldRow.add(new JLabel("Rule " + (m_textFields.size() + 1) + ":"));
        oneFieldRow.setLayout(new BoxLayout(oneFieldRow, BoxLayout.X_AXIS));

        final JTextField newField = new JTextField(initValue);
        newField.setSize(new Dimension(350, 20));
        newField.setMaximumSize(new Dimension(350, 20));
        newField.setColumns(10);
        oneFieldRow.add(newField);

        m_removeButton = new JButton("-");
        m_removeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {

                for (final Component p : m_removeButton.getParent().getComponents()) {

                    if (p instanceof JTextField) {
                        m_textFields.remove(p);
                    }

                }
                m_textFieldsPanel.remove(m_removeButton.getParent());
                getComponentPanel().updateUI();

            }
        });

        oneFieldRow.add(m_removeButton);
        m_textFields.add(newField);
        m_removeButtons.add(m_removeButton);
        m_textFieldsPanel.add(oneFieldRow);

        getComponentPanel().updateUI();

    }

    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // Nothing to do here
    }

    @Override
    protected void setEnabledComponents(final boolean enabled) {
        for (int t = 0; t < m_textFields.size(); t++) {
            m_textFields.get(t).setEnabled(true);
        }
    }

    @Override
    public void setToolTipText(final String text) {
        // Nothing to do here
    }

    // Change listener on model
    @Override
    public void stateChanged(final ChangeEvent e) {
        for (final JTextField field : m_textFields) {
            field.setEnabled(getModel().isEnabled());
        }
        for (final JButton button : m_removeButtons) {
            button.setEnabled(getModel().isEnabled());
        }
        m_addButton.setEnabled(getModel().isEnabled());
        m_operatorBox.setEnabled(getModel().isEnabled());
        m_textFieldsPanel.setEnabled(getModel().isEnabled());
        getComponentPanel().updateUI();
    }

    @Override
    protected void updateComponent() {
        @SuppressWarnings("unchecked")
        final SettingsModelFilterSelection<L> model = ((SettingsModelFilterSelection<L>)getModel());
        m_textFields.clear();
        m_textFieldsPanel.removeAll();

        final String[] rules = model.getRules();
        for (int r = 0; r < rules.length; r++) {
            addTextField(rules[r]);
        }

        m_operatorBox.setSelectedItem(model.getOperator());
        getComponentPanel().updateUI();
        setEnabledComponents(getModel().isEnabled());
    }

    private void updateModel() {
        @SuppressWarnings("unchecked")
        final SettingsModelFilterSelection<L> model = ((SettingsModelFilterSelection<L>)getModel());
        final String[] tmp = new String[m_textFields.size()];

        for (int t = 0; t < m_textFields.size(); t++) {
            tmp[t] = m_textFields.get(t).getText();
        }

        model.setRules(tmp);
        model.setOperator((RulebasedLabelFilter.Operator)m_operatorBox.getSelectedItem());

    }

    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        updateModel();
    }
}
