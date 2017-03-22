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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
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
 * @param <L>
 */
public class DialogComponentFilterSelection<L extends Comparable<L>> extends DialogComponent implements ChangeListener {

    private final JButton m_addButton;

    @SuppressWarnings("rawtypes")
    private final JComboBox m_operatorBox;

    private final List<JButton> m_removeButtons;

    private final List<JTextField> m_textFields;

    private final JPanel m_textFieldsPanel;

    private final JCheckBox m_caseSensitiveMatch;

    private final JCheckBox m_containsWildCards;

    private final JCheckBox m_regularExpression;

    private GridBagConstraints m_textFieldsGBC;

    /**
     * @param model
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public DialogComponentFilterSelection(final SettingsModelFilterSelection<L> model) {
        super(model);

        model.addChangeListener(this);

        m_textFields = new ArrayList<JTextField>();
        m_removeButtons = new ArrayList<JButton>();

        m_textFieldsPanel = new JPanel();
        m_textFieldsPanel.setLayout(new GridBagLayout());
        m_textFieldsGBC = new GridBagConstraints();
        m_textFieldsGBC.anchor = GridBagConstraints.NORTH;
        m_textFieldsGBC.fill = GridBagConstraints.HORIZONTAL;
        m_textFieldsGBC.insets = new Insets(1, 2, 1, 2);
        m_textFieldsGBC.gridx = 0;
        m_textFieldsGBC.gridy = 0;
        m_textFieldsGBC.weightx = 1;
        m_textFieldsGBC.weighty = 0;

        m_addButton = new JButton("+");
        m_addButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                addTextField("");
            }
        });

        m_operatorBox = new JComboBox(RulebasedLabelFilter.Operator.values());

        m_caseSensitiveMatch = new JCheckBox("Case Sensitive Match");
        m_caseSensitiveMatch.setEnabled(false);

        m_containsWildCards = new JCheckBox("Contains Wild Cards");
        m_containsWildCards.setEnabled(false);
        m_containsWildCards.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                if ((e.getSource() == m_containsWildCards) && m_containsWildCards.isSelected()) {
                    m_regularExpression.setSelected(false);
                }
            }
        });

        m_regularExpression = new JCheckBox("Regular Expression");
        m_regularExpression.setEnabled(false);
        m_regularExpression.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                if ((e.getSource() == m_regularExpression) && m_regularExpression.isSelected()) {
                    m_containsWildCards.setSelected(false);
                }
            }
        });

        final JPanel ruleConfigurationPanel = new JPanel(new GridBagLayout());


        getComponentPanel().setLayout(new GridBagLayout());

        final GridBagConstraints dialogGBC = new GridBagConstraints();
        dialogGBC.fill = GridBagConstraints.BOTH;
        dialogGBC.weightx = 1;
        dialogGBC.weighty = 1;
        dialogGBC.gridx = 0;
        dialogGBC.gridy = 0;
        dialogGBC.anchor = GridBagConstraints.NORTH;

        ruleConfigurationPanel.add(m_caseSensitiveMatch, dialogGBC);
        dialogGBC.gridx++;
        ruleConfigurationPanel.add(m_regularExpression, dialogGBC);
        dialogGBC.gridx++;
        ruleConfigurationPanel.add(m_containsWildCards, dialogGBC);

        dialogGBC.gridx = 0;
        dialogGBC.insets = new Insets(2, 2, 2, 2);

        getComponentPanel().add(ruleConfigurationPanel, dialogGBC);
        dialogGBC.gridy++;

        getComponentPanel().add(m_textFieldsPanel, dialogGBC);
        dialogGBC.fill = GridBagConstraints.NONE;
        dialogGBC.anchor = GridBagConstraints.CENTER;
        dialogGBC.gridx++;
        dialogGBC.weightx = 0;
        dialogGBC.weighty = 1;
        getComponentPanel().add(m_operatorBox, dialogGBC);
        dialogGBC.gridwidth = 2;
        dialogGBC.gridx = 0;
        dialogGBC.gridy++;
        dialogGBC.weighty = 0;
        getComponentPanel().add(m_addButton, dialogGBC);

        stateChanged(null);
    }

    /**
     * @param initValue
     */
    protected void addTextField(final String initValue) {
        final JPanel oneFieldRow = new JPanel();
        oneFieldRow.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(1, 2, 1, 2);

        oneFieldRow.add(new JLabel("Rule " + (m_textFields.size() + 1) + ":"), gbc);

        final JTextField newField = new JTextField(initValue);
        newField.setColumns(10);
        gbc.weightx = 1;
        gbc.gridx++;
        oneFieldRow.add(newField, gbc);

        JButton removeButton = new JButton("-");
        // set size so it aligns with the textfield size
        removeButton.setPreferredSize(new Dimension(40, 18));
        removeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {

                for (final Component p : removeButton.getParent().getComponents()) {

                    if (p instanceof JTextField) {
                        m_textFields.remove(p);
                    }

                }
                m_textFieldsPanel.remove(removeButton.getParent());
                getComponentPanel().updateUI();
                m_textFieldsGBC.gridy--;

                if (m_textFieldsGBC.gridy == 0) {
                    m_caseSensitiveMatch.setEnabled(false);
                    m_containsWildCards.setEnabled(false);
                    m_regularExpression.setEnabled(false);
                }
            }
        });

        gbc.weightx = 0;
        gbc.gridx++;
        oneFieldRow.add(removeButton, gbc);
        m_textFields.add(newField);
        m_removeButtons.add(removeButton);
        m_textFieldsPanel.add(oneFieldRow, m_textFieldsGBC);
        m_textFieldsGBC.gridy++;

        m_caseSensitiveMatch.setEnabled(true);
        m_containsWildCards.setEnabled(true);
        m_regularExpression.setEnabled(true);

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

        m_caseSensitiveMatch.setSelected(model.getCaseSensitiveMatch());
        m_containsWildCards.setSelected(model.getContainsWildCards());
        m_regularExpression.setSelected(model.getRegularExpression());
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

        model.setCaseSensitiveMatch(m_caseSensitiveMatch.isSelected());
        model.setContainsWildCards(m_containsWildCards.isSelected());
        model.setRegularExpression(m_regularExpression.isSelected());
    }

    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        updateModel();
    }
}
